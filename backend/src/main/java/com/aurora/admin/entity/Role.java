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
@ToString(exclude = "menuIds")
@TableName("t_role")
public class Role {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String code;

    private String name;

    private String description;

    /** 数据权限范围: 1:全部 2:本部门及下级 3:本部门 4:仅本人 */
    private Integer dataScope;

    /** 是否系统内置角色（不可删除） */
    private Integer isSystem;

    private Integer status;
    /** 排序 */
    private Integer sortOrder;
    @TableLogic
    private Integer isDeleted;

    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private List<Long> menuIds;
}
