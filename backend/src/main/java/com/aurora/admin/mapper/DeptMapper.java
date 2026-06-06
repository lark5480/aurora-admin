package com.aurora.admin.mapper;

import com.aurora.admin.entity.Dept;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface DeptMapper {

    @Select("SELECT * FROM t_dept WHERE is_deleted = 0 ORDER BY sort_order")
    List<Dept> findAll();

    @Select("SELECT * FROM t_dept WHERE parent_id = #{parentId} AND is_deleted = 0 ORDER BY sort_order")
    List<Dept> findByParentId(Long parentId);

    @Select("SELECT * FROM t_dept WHERE id = #{id} AND is_deleted = 0")
    Dept findById(Long id);

    @Select("SELECT * FROM t_dept WHERE code = #{code} AND is_deleted = 0")
    Dept findByCode(String code);

    @Insert("INSERT INTO t_dept(parent_id, name, code, sort_order, leader_user_id, leader_name, phone, email, status) " +
            "VALUES(#{parentId}, #{name}, #{code}, #{sortOrder}, #{leaderUserId}, #{leaderName}, #{phone}, #{email}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Dept dept);

    @Update("UPDATE t_dept SET parent_id = #{parentId}, name = #{name}, code = #{code}, " +
            "sort_order = #{sortOrder}, leader_user_id = #{leaderUserId}, leader_name = #{leaderName}, " +
            "phone = #{phone}, email = #{email}, status = #{status} WHERE id = #{id}")
    int update(Dept dept);

    @Update("UPDATE t_dept SET is_deleted = 1 WHERE id = #{id}")
    int delete(Long id);

    @Select("SELECT COUNT(*) FROM t_dept WHERE parent_id = #{parentId} AND is_deleted = 0")
    int countByParentId(Long parentId);
}
