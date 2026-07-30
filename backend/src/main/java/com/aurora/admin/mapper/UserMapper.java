package com.aurora.admin.mapper;

import com.aurora.admin.annotation.DataScope;
import com.aurora.admin.entity.User;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface UserMapper {

    @Select("SELECT * FROM t_user WHERE username = #{username} AND is_deleted = 0")
    User findByUsername(String username);

    @Insert("INSERT INTO t_user(username, password, email, nickname, avatar, role, dept_id, dept_name, status) VALUES(#{username}, #{password}, #{email}, #{nickname}, #{avatar}, #{role}, #{deptId}, #{deptName}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    @Select("SELECT * FROM t_user WHERE id = #{id} AND is_deleted = 0")
    User findById(Long id);

    @Select("SELECT * FROM t_user WHERE email = #{email} AND is_deleted = 0")
    User findByEmail(String email);

    @Select("SELECT * FROM t_user WHERE nickname = #{nickname} AND is_deleted = 0")
    User findByNickname(String nickname);

    @Select("SELECT * FROM t_user WHERE username = #{username}")
    User findByUsernameIncludeDeleted(String username);

    @Select("SELECT * FROM t_user WHERE (username LIKE CONCAT('%', #{keyword}, '%') OR email LIKE CONCAT('%', #{keyword}, '%')) AND is_deleted = 0 LIMIT #{offset}, #{size}")
    @DataScope(userColumn = "id")
    List<User> findAll(@Param("offset") int offset, @Param("size") int size, @Param("keyword") String keyword);

    @Select("SELECT COUNT(*) FROM t_user WHERE (username LIKE CONCAT('%', #{keyword}, '%') OR email LIKE CONCAT('%', #{keyword}, '%')) AND is_deleted = 0")
    @DataScope(userColumn = "id")
    int count(@Param("keyword") String keyword);

    @Update("UPDATE t_user SET username = #{username}, email = #{email}, nickname = #{nickname}, avatar = #{avatar}, " +
        "role = #{role}, dept_id = #{deptId}, dept_name = #{deptName}, status = #{status} WHERE id = #{id}")
    int update(User user);

    @Update("UPDATE t_user SET status = #{status} WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") int status);

    @Update("UPDATE t_user SET password = #{password} WHERE id = #{id}")
    int updatePassword(@Param("id") Long id, @Param("password") String password);

    @Update("UPDATE t_user SET dept_id = #{deptId} WHERE id = #{id}")
    int updateDeptId(@Param("id") Long id, @Param("deptId") Long deptId);

    @Update("UPDATE t_user SET nickname = #{nickname}, email = #{email}, avatar = #{avatar} WHERE id = #{id}")
    int updateProfile(@Param("id") Long id, @Param("nickname") String nickname, @Param("email") String email, @Param("avatar") String avatar);

    @Update("UPDATE t_user SET is_deleted = 1 WHERE id = #{id}")
    int delete(Long id);
}
