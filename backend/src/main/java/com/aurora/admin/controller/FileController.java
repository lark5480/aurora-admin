package com.aurora.admin.controller;

import com.aurora.admin.annotation.RateLimit;
import com.aurora.admin.annotation.RateLimit.KeyType;
import com.aurora.admin.dto.ApiResponse;
import com.aurora.admin.dto.PageResult;
import com.aurora.admin.entity.FileRecord;
import com.aurora.admin.exception.ForbiddenException;
import com.aurora.admin.exception.NotFoundException;
import com.aurora.admin.service.FileService;
import com.aurora.admin.util.SecurityUtils;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/**
 * 文件管理控制器。提供文件的上传、下载、预览、删除以及文件列表查询等 REST API。
 * 管理员可查看和操作所有文件，普通用户仅能操作自己上传的文件。
 * 映射路径：/api/files
 */
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private static final Logger log = LoggerFactory.getLogger(FileController.class);

    private final FileService fileService;

    @Value("${file.upload-dir}")
    private String uploadDir;

    /**
     * 获取文件列表（分页）。管理员返回所有用户文件，普通用户仅返回其本人的文件。
     * 支持按文件名模糊搜索。
     *
     * @param page    页码，从 1 开始，默认 1
     * @param size    每页条数，默认 10
     * @param keyword 文件名搜索关键字，可选，为空时返回全部
     * @return 分页文件列表
     */
    @GetMapping
    public ApiResponse getFiles(@RequestParam(defaultValue = "1") int page,
                                @RequestParam(defaultValue = "10") int size,
                                @RequestParam(defaultValue = "") String keyword) {
        Long userId = SecurityUtils.getCurrentUserId();
        List<FileRecord> files;
        long total;

        if (SecurityUtils.isCurrentUserAdmin()) {
            files = fileService.getAllFiles(page, size, keyword);
            total = fileService.getAllFileCount(keyword);
        } else {
            files = fileService.getUserFiles(userId, page, size, keyword);
            total = fileService.getUserFileCount(userId, keyword);
        }

        return ApiResponse.success(PageResult.of(files, total, page, size));
    }

    /**
     * 上传文件。限流：每 IP 每 60 秒最多 5 次。
     * 文件大小和类型受系统配置（upload.max_size / upload.allowed_types）限制。
     *
     * @param file 待上传的文件（Multipart）
     * @return 上传成功后的文件记录
     * @throws IllegalArgumentException 文件为空或超出大小/类型限制时抛出
     */
    @RateLimit(key = KeyType.IP, limit = 5, duration = 60, message = "上传过于频繁，请稍后再试")
    @PostMapping("/upload")
    public ApiResponse uploadFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        Long userId = SecurityUtils.getCurrentUserId();
        String username = SecurityUtils.getCurrentUsername();

        try {
            FileRecord record = fileService.uploadFile(file, userId, username);
            return ApiResponse.success(record);
        } catch (IOException e) {
            throw new RuntimeException("上传失败: " + e.getMessage(), e);
        }
    }

    /**
     * 删除文件（逻辑删除）。仅文件上传者或管理员可执行。
     *
     * @param id 文件 ID
     * @return 删除成功提示
     * @throws NotFoundException 文件不存在时抛出
     */
    @DeleteMapping("/{id}")
    public ApiResponse deleteFile(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        boolean success = fileService.deleteFile(id, userId);
        if (!success) {
            throw new NotFoundException("文件", id);
        }
        return ApiResponse.success("删除成功");
    }

    /**
     * 预览文件（内联展示）。将文件内容直接写入响应流，Content-Type 取文件原始类型。
     * 权限：仅文件上传者或管理员可访问。
     * 客户端断连时自动检测并记录 warn 日志。
     *
     * @param id       文件 ID
     * @param response HTTP 响应
     * @throws IOException 文件读写异常
     */
    @GetMapping("/{id}/view")
    public void viewFile(@PathVariable Long id, HttpServletResponse response) throws IOException {
        FileRecord record = fileService.getFileById(id);
        if (record == null || record.getFilePath().contains("..")) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        if (!SecurityUtils.isCurrentUserAdmin() && !record.getUploadUserId().equals(SecurityUtils.getCurrentUserId())) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        File file = new File(uploadDir, record.getFilePath());
        if (!file.exists()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        response.setContentType(record.getFileType() != null ? record.getFileType() : "application/octet-stream");
        response.setContentLengthLong(file.length());

        try (FileInputStream fis = new FileInputStream(file);
             OutputStream os = response.getOutputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            if (isClientDisconnect(e)) {
                log.warn("客户端断开连接，文件查看中断: fileId={}", id);
            } else {
                log.error("文件查看失败: fileId={}", id, e);
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
    }

    /**
     * 下载文件（附件形式）。触发下载计数 +1，设置 Content-Disposition: attachment 响应头。
     * 权限：仅文件上传者或管理员可访问。
     * 客户端断连时自动检测并记录 warn 日志。
     *
     * @param id       文件 ID
     * @param response HTTP 响应
     * @throws IOException 文件读写异常
     */
    @GetMapping("/{id}/download")
    public void downloadFile(@PathVariable Long id, HttpServletResponse response) throws IOException {
        Long userId = SecurityUtils.getCurrentUserId();
        File file = fileService.downloadFile(id, userId);
        if (file == null) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        FileRecord record = fileService.getFileById(id);
        if (record != null) {
            fileService.incrementDownloadCount(id);
            response.setContentType(record.getFileType() != null ? record.getFileType() : "application/octet-stream");
            String encodedFilename = java.net.URLEncoder.encode(record.getFileName(), "UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + encodedFilename + "\"");
        }

        try (FileInputStream fis = new FileInputStream(file);
             OutputStream os = response.getOutputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            if (isClientDisconnect(e)) {
                log.warn("客户端断开连接，文件下载中断: fileId={}", id);
            } else {
                log.error("文件下载失败: fileId={}", id, e);
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
    }

    /**
     * 判断是否为客户端主动断开连接导致的 IOException
     * 包括：Connection reset by peer / Broken pipe / 连接被对端重置
     */
    private boolean isClientDisconnect(IOException e) {
        String msg = e.getMessage();
        if (msg != null) {
            String lower = msg.toLowerCase();
            return lower.contains("connection reset")
                    || lower.contains("broken pipe")
                    || lower.contains("断开")
                    || lower.contains("被对端重置");
        }
        // 检查 cause 链
        Throwable cause = e.getCause();
        while (cause != null) {
            String causeMsg = cause.getMessage();
            if (causeMsg != null) {
                String lower = causeMsg.toLowerCase();
                if (lower.contains("connection reset") || lower.contains("broken pipe")) {
                    return true;
                }
            }
            cause = cause.getCause();
        }
        return false;
    }

    /**
     * 获取单个文件详情。仅文件上传者或管理员可访问。
     *
     * @param id 文件 ID
     * @return 文件记录
     * @throws NotFoundException  文件不存在时抛出
     * @throws ForbiddenException 无权限时抛出
     */
    @GetMapping("/{id}")
    public ApiResponse getFile(@PathVariable Long id) {
        FileRecord file = fileService.getFileById(id);
        if (file == null) {
            throw new NotFoundException("文件", id);
        }
        if (!SecurityUtils.isCurrentUserAdmin() && !file.getUploadUserId().equals(SecurityUtils.getCurrentUserId())) {
            throw new ForbiddenException("无权限访问此文件");
        }
        return ApiResponse.success(file);
    }
}
