package com.aurora.admin.service;

import com.aurora.admin.entity.FileRecord;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;

public interface FileService {
    FileRecord uploadFile(MultipartFile file, Long userId, String username) throws IOException;
    List<FileRecord> getUserFiles(Long userId, int page, int size, String keyword);
    long getUserFileCount(Long userId, String keyword);
    List<FileRecord> getAllFiles(int page, int size, String keyword);
    long getAllFileCount(String keyword);
    FileRecord getFileById(Long id);
    boolean deleteFile(Long id, Long userId);
    File downloadFile(Long id, Long userId);
    void incrementDownloadCount(Long id);
}
