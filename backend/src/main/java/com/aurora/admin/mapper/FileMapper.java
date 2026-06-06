package com.aurora.admin.mapper;

import com.aurora.admin.entity.FileRecord;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface FileMapper {

    @Select("<script>SELECT * FROM t_file WHERE upload_user_id = #{userId} AND is_deleted = 0" +
            "<if test='keyword != null and keyword != \"\"'> AND file_name LIKE CONCAT('%', #{keyword}, '%')</if>" +
            " ORDER BY create_time DESC LIMIT #{offset}, #{limit}</script>")
    List<FileRecord> findByUserId(@Param("userId") Long userId, @Param("offset") int offset, @Param("limit") int limit, @Param("keyword") String keyword);

    @Select("<script>SELECT COUNT(*) FROM t_file WHERE upload_user_id = #{userId} AND is_deleted = 0" +
            "<if test='keyword != null and keyword != \"\"'> AND file_name LIKE CONCAT('%', #{keyword}, '%')</if></script>")
    long countByUserId(@Param("userId") Long userId, @Param("keyword") String keyword);

    @Select("SELECT * FROM t_file WHERE id = #{id} AND is_deleted = 0")
    FileRecord findById(Long id);

    @Insert("INSERT INTO t_file(file_name, file_path, file_size, file_type, file_ext, upload_user_id, upload_username) " +
            "VALUES(#{fileName}, #{filePath}, #{fileSize}, #{fileType}, #{fileExt}, #{uploadUserId}, #{uploadUsername})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(FileRecord file);

    @Update("UPDATE t_file SET is_deleted = 1 WHERE id = #{id}")
    int softDelete(Long id);

    @Update("UPDATE t_file SET download_count = download_count + 1 WHERE id = #{id}")
    int incrementDownloadCount(Long id);

    @Select("<script>SELECT * FROM t_file WHERE is_deleted = 0" +
            "<if test='keyword != null and keyword != \"\"'> AND file_name LIKE CONCAT('%', #{keyword}, '%')</if>" +
            " ORDER BY create_time DESC LIMIT #{offset}, #{limit}</script>")
    List<FileRecord> findPage(@Param("offset") int offset, @Param("limit") int limit, @Param("keyword") String keyword);

    @Select("<script>SELECT COUNT(*) FROM t_file WHERE is_deleted = 0" +
            "<if test='keyword != null and keyword != \"\"'> AND file_name LIKE CONCAT('%', #{keyword}, '%')</if></script>")
    long count(@Param("keyword") String keyword);
}
