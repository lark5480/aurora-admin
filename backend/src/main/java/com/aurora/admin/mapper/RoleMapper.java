package com.aurora.admin.mapper;

import com.aurora.admin.entity.Role;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface RoleMapper {

    @Select("SELECT * FROM t_role WHERE is_deleted = 0 ORDER BY id")
    List<Role> findAll();

    @Select("<script>" +
            "SELECT * FROM t_role WHERE is_deleted = 0 " +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (name LIKE CONCAT('%', #{keyword}, '%') OR code LIKE CONCAT('%', #{keyword}, '%')) " +
            "</if>" +
            "ORDER BY sort_order ASC, id ASC LIMIT #{offset}, #{size}" +
            "</script>")
    List<Role> findByKeyword(@Param("keyword") String keyword, @Param("offset") int offset, @Param("size") int size);

    @Select("<script>" +
            "SELECT COUNT(*) FROM t_role WHERE is_deleted = 0 " +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (name LIKE CONCAT('%', #{keyword}, '%') OR code LIKE CONCAT('%', #{keyword}, '%')) " +
            "</if>" +
            "</script>")
    long countByKeyword(@Param("keyword") String keyword);

    @Select("SELECT * FROM t_role WHERE id = #{id} AND is_deleted = 0")
    Role findById(Long id);

    @Select("SELECT * FROM t_role WHERE code = #{code} AND is_deleted = 0")
    Role findByCode(String code);

    @Insert("INSERT INTO t_role(code, name, description, data_scope, is_system, status, sort_order) " +
            "VALUES(#{code}, #{name}, #{description}, #{dataScope}, #{isSystem}, #{status}, #{sortOrder})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Role role);

    @Update("UPDATE t_role SET name = #{name}, description = #{description}, " +
            "data_scope = #{dataScope}, status = #{status}, sort_order = #{sortOrder} WHERE id = #{id}")
    int update(Role role);

    @Update("UPDATE t_role SET is_deleted = 1 WHERE id = #{id}")
    int delete(Long id);

    @Select("SELECT menu_id FROM t_role_menu WHERE role_id = #{roleId}")
    List<Long> findMenuIdsByRoleId(Long roleId);

    @Delete("DELETE FROM t_role_menu WHERE role_id = #{roleId}")
    int deleteRoleMenus(Long roleId);

    @Insert("<script>" +
            "INSERT INTO t_role_menu(role_id, menu_id) VALUES " +
            "<foreach collection='menuIds' item='menuId' separator=','>(#{roleId}, #{menuId})</foreach>" +
            "</script>")
    int insertRoleMenus(@Param("roleId") Long roleId, @Param("menuIds") List<Long> menuIds);

    @Insert("INSERT IGNORE INTO t_user_role(user_id, role_id) VALUES(#{userId}, #{roleId})")
    int insertUserRole(@Param("userId") Long userId, @Param("roleId") Long roleId);

    @Select("SELECT COUNT(*) FROM t_user_role WHERE user_id = #{userId} AND role_id = #{roleId}")
    int countUserRole(@Param("userId") Long userId, @Param("roleId") Long roleId);

    @Select("<script>SELECT id FROM t_role WHERE code IN " +
            "<foreach collection='codes' item='code' open='(' separator=',' close=')'>#{code}</foreach>" +
            " AND is_deleted = 0" +
            "</script>")
    List<Long> findIdsByCodes(@Param("codes") List<String> codes);

    @Select("SELECT r.code FROM t_role r INNER JOIN t_user_role ur ON r.id = ur.role_id WHERE ur.user_id = #{userId} AND r.is_deleted = 0")
    List<String> findCodesByUserId(@Param("userId") Long userId);

    @Select("SELECT r.* FROM t_role r INNER JOIN t_user_role ur ON r.id = ur.role_id WHERE ur.user_id = #{userId} AND r.is_deleted = 0")
    List<Role> findByUserId(@Param("userId") Long userId);

    @Delete("DELETE FROM t_user_role WHERE user_id = #{userId}")
    int deleteUserRoles(@Param("userId") Long userId);

    @Delete("DELETE FROM t_user_role WHERE user_id = #{userId} AND role_id = #{roleId}")
    int deleteUserRole(@Param("userId") Long userId, @Param("roleId") Long roleId);
}
