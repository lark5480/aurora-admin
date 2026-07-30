package com.aurora.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("t_order")
public class Order {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String orderNo;

    private Long userId;

    /** 用户名（查询时 JOIN t_user 填充，非数据库字段） */
    @TableField(exist = false)
    private String username;

    private BigDecimal totalAmount;

    private String status;

    private String receiverName;

    private String receiverPhone;

    private String receiverAddress;

    private String remark;

    private String trackingNumber;

    private LocalDateTime payTime;

    @TableLogic
    private Integer isDeleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
