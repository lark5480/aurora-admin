package com.aurora.admin.mapper;

import com.aurora.admin.entity.Menu;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface MenuMapper {

    @Select("SELECT * FROM t_menu WHERE is_deleted = 0 ORDER BY sort_order")
    List<Menu> findAll();

    @Select("SELECT * FROM t_menu WHERE id = #{id} AND is_deleted = 0")
    Menu findById(Long id);

    @Select("SELECT * FROM t_menu WHERE parent_id = #{parentId} AND is_deleted = 0 ORDER BY sort_order")
    List<Menu> findByParentId(Long parentId);

    @Select("SELECT * FROM t_menu WHERE id IN (#{ids}) AND is_deleted = 0 ORDER BY sort_order")
    List<Menu> findByIds(@Param("ids") List<Long> ids);

    @Select("SELECT * FROM t_menu WHERE is_deleted = 0 AND id IN (SELECT menu_id FROM t_role_menu WHERE role_id = #{roleId}) ORDER BY sort_order")
    List<Menu> findByRoleId(Long roleId);

    @Select("<script>SELECT DISTINCT m.* FROM t_menu m " +
            "INNER JOIN t_role_menu rm ON m.id = rm.menu_id " +
            "WHERE m.is_deleted = 0 AND rm.role_id IN " +
            "<foreach collection='roleIds' item='roleId' open='(' separator=',' close=')'>#{roleId}</foreach>" +
            "ORDER BY m.sort_order</script>")
    List<Menu> findByRoleIds(@Param("roleIds") List<Long> roleIds);

    @Insert("INSERT INTO t_menu(parent_id, name, path, component, menu_type, icon, sort_order, permission, status) " +
            "VALUES(#{parentId}, #{name}, #{path}, #{component}, #{menuType}, #{icon}, #{sortOrder}, #{permission}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Menu menu);

    @Update("UPDATE t_menu SET parent_id = #{parentId}, name = #{name}, path = #{path}, " +
            "component = #{component}, menu_type = #{menuType}, icon = #{icon}, " +
            "sort_order = #{sortOrder}, permission = #{permission}, status = #{status} WHERE id = #{id}")
    int update(Menu menu);

    @Update("UPDATE t_menu SET is_deleted = 1 WHERE id = #{id}")
    int delete(Long id);

    @Select("SELECT COUNT(*) FROM t_menu WHERE parent_id = #{parentId} AND is_deleted = 0")
    int countByParentId(Long parentId);
}
