package com.aurora.admin.service.impl;

import com.aurora.admin.config.ConfigCache;
import com.aurora.admin.entity.FileRecord;
import com.aurora.admin.mapper.FileMapper;
import com.aurora.admin.service.FileService;
import com.aurora.admin.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private static final String PARENT_DIR = "..";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final FileMapper fileMapper;
    private final ConfigCache configCache;

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Override
    public FileRecord uploadFile(MultipartFile file, Long userId, String username) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String fileExt = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            fileExt = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        // 文件大小校验
        int maxSizeMB = configCache.getInt("upload.max_size", 10);
        long maxSizeBytes = (long) maxSizeMB * 1024 * 1024;
        if (file.getSize() > maxSizeBytes) {
            throw new IllegalArgumentException("文件大小超过限制，最大允许 " + maxSizeMB + "MB");
        }

        // 文件类型校验
        String allowedTypes = configCache.getString("upload.allowed_types", "");
        if (allowedTypes != null && !allowedTypes.isEmpty()) {
            String[] allowed = allowedTypes.toLowerCase().split(",");
            boolean matched = false;
            for (String ext : allowed) {
                String trimmed = ext.trim();
                if (!trimmed.isEmpty() && trimmed.equals(fileExt.toLowerCase())) {
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                throw new IllegalArgumentException("不支持的文件类型，仅允许: " + allowedTypes);
            }
        }

        String datePath = LocalDate.now().format(DATE_FORMATTER);
        String newFilename = UUID.randomUUID().toString() + fileExt;
        String relativePath = datePath + "/" + newFilename;
        if (relativePath.contains(PARENT_DIR)) {
            throw new IOException("Invalid file path");
        }

        Path uploadPath = Paths.get(uploadDir, datePath);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        File destFile = uploadPath.resolve(newFilename).toFile();
        file.transferTo(destFile);

        FileRecord record = new FileRecord();
        record.setFileName(originalFilename);
        record.setFilePath(relativePath);
        record.setFileSize(file.getSize());
        record.setFileType(file.getContentType());
        record.setFileExt(fileExt);
        record.setUploadUserId(userId);
        record.setUploadUsername(username);
        record.setDownloadCount(0);
        record.setIsDeleted(0);

        try {
            fileMapper.insert(record);
            return record;
        } catch (Exception e) {
            // DB 写入失败，清理已落盘的孤儿文件
            Files.deleteIfExists(destFile.toPath());
            throw e;
        }
    }

    @Override
    public List<FileRecord> getUserFiles(Long userId, int page, int size, String keyword) {
        int offset = (page - 1) * size;
        return fileMapper.findByUserId(userId, offset, size, keyword);
    }

    @Override
    public long getUserFileCount(Long userId, String keyword) {
        return fileMapper.countByUserId(userId, keyword);
    }

    @Override
    public List<FileRecord> getAllFiles(int page, int size, String keyword) {
        int offset = (page - 1) * size;
        return fileMapper.findPage(offset, size, keyword);
    }

    @Override
    public long getAllFileCount(String keyword) {
        return fileMapper.count(keyword);
    }

    @Override
    public FileRecord getFileById(Long id) {
        return fileMapper.findById(id);
    }

    @Override
    @Transactional
    public boolean deleteFile(Long id, Long userId) {
        FileRecord file = fileMapper.findById(id);
        if (file == null) {
            return false;
        }
        if (!SecurityUtils.isCurrentUserAdmin() && !file.getUploadUserId().equals(userId)) {
            return false;
        }
        return fileMapper.softDelete(id) > 0;
    }

    @Override
    public File downloadFile(Long id, Long userId) {
        FileRecord file = fileMapper.findById(id);
        if (file == null || file.getFilePath().contains(PARENT_DIR)) {
            return null;
        }

        boolean isAdmin = SecurityUtils.isCurrentUserAdmin();
        if (!isAdmin && !file.getUploadUserId().equals(userId)) {
            return null;
        }

        File targetFile = new File(uploadDir, file.getFilePath());
        String canonicalPath;
        try {
            canonicalPath = targetFile.getCanonicalPath();
            String uploadDirCanonical = Paths.get(uploadDir).toFile().getCanonicalPath();
            if (!canonicalPath.startsWith(uploadDirCanonical + File.separator)) {
                return null;
            }
        } catch (IOException e) {
            return null;
        }
        return targetFile.exists() ? targetFile : null;
    }

    @Override
    public void incrementDownloadCount(Long id) {
        fileMapper.incrementDownloadCount(id);
    }
}
