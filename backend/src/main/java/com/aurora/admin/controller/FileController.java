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

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private static final Logger log = LoggerFactory.getLogger(FileController.class);

    private final FileService fileService;

    @Value("${file.upload-dir}")
    private String uploadDir;

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

    @DeleteMapping("/{id}")
    public ApiResponse deleteFile(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        boolean success = fileService.deleteFile(id, userId);
        if (!success) {
            throw new NotFoundException("文件", id);
        }
        return ApiResponse.success("删除成功");
    }

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
