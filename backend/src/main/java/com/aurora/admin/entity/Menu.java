package com.aurora.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@ToString(exclude = "children")
@TableName("t_menu")
public class Menu {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 父菜单ID，0为根 */
    private Long parentId;

    /** 菜单名称 */
    private String name;

    /** 路由路径 */
    private String path;

    /** 前端组件路径 */
    private String component;

    /** 1:目录 2:菜单 3:按钮 */
    private Integer menuType;

    /** 图标 */
    private String icon;

    /** 排序 */
    private Integer sortOrder;

    /** 权限标识，如: system:user:add */
    private String permission;

    /** 1:启用 0:禁用 */
    private Integer status;
    @TableLogic
    private Integer isDeleted;

    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private List<Menu> children;
}
