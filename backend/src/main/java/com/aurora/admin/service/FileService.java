package com.aurora.admin.service;

import com.aurora.admin.entity.FileRecord;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;

public interface FileService {
    /**
     * 上传文件。将 MultipartFile 写入磁盘并创建文件记录。
     * 文件大小和类型受系统配置限制，超出时抛出 IllegalArgumentException。
     *
     * @param file     上传的文件
     * @param userId   上传者用户 ID
     * @param username 上传者用户名
     * @return 写入数据库后的文件记录（含自增 ID）
     * @throws IOException 磁盘写入失败时抛出
     */
    FileRecord uploadFile(MultipartFile file, Long userId, String username) throws IOException;
    /**
     * 查询指定用户的文件列表（分页，按上传时间倒序）。
     *
     * @param userId  用户 ID
     * @param page    页码，从 1 开始
     * @param size    每页条数
     * @param keyword 文件名搜索关键字，可选
     * @return 文件记录列表
     */
    List<FileRecord> getUserFiles(Long userId, int page, int size, String keyword);
    /**
     * 查询指定用户的文件总数（配合分页）。
     *
     * @param userId  用户 ID
     * @param keyword 文件名搜索关键字，可选
     * @return 文件总数
     */
    long getUserFileCount(Long userId, String keyword);
    /**
     * 查询所有用户的文件列表（分页，管理员专用）。
     *
     * @param page    页码，从 1 开始
     * @param size    每页条数
     * @param keyword 文件名搜索关键字，可选
     * @return 文件记录列表
     */
    List<FileRecord> getAllFiles(int page, int size, String keyword);
    /**
     * 查询所有用户的文件总数（配合分页，管理员专用）。
     *
     * @param keyword 文件名搜索关键字，可选
     * @return 文件总数
     */
    long getAllFileCount(String keyword);
    /**
     * 根据 ID 获取文件记录（未删除的文件）。
     *
     * @param id 文件 ID
     * @return 文件记录，不存在时返回 null
     */
    FileRecord getFileById(Long id);
    /**
     * 逻辑删除文件。仅文件上传者或管理员可执行，非所有者调用返回 false。
     *
     * @param id     文件 ID
     * @param userId 当前操作用户 ID（用于权限校验）
     * @return true 删除成功，false 文件不存在或无权限
     */
    boolean deleteFile(Long id, Long userId);
    /**
     * 获取文件对应的磁盘 File 对象（用于下载/预览）。
     * 进行路径穿越防护（canonical path 校验）和权限校验。
     *
     * @param id     文件 ID
     * @param userId 当前操作用户 ID（用于权限校验）
     * @return 文件磁盘对象，无权限或文件不存在时返回 null
     */
    File downloadFile(Long id, Long userId);
    /**
     * 文件下载计数 +1（原子更新）。
     *
     * @param id 文件 ID
     */
    void incrementDownloadCount(Long id);
}
