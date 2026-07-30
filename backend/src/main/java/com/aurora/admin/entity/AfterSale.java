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
@TableName("t_after_sale")
public class AfterSale {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String afterSaleNo;

    private Long orderId;

    private Long orderItemId;

    private Long userId;

    private String type;

    private String reason;

    private BigDecimal refundAmount;

    private String status;

    private String originalOrderStatus;

    @TableLogic
    private Integer isDeleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private String reviewRemark;

    private LocalDateTime reviewTime;

    private Long reviewerId;

    /** 订单号（查询时 JOIN t_order 填充，非数据库字段） */
    @TableField(exist = false)
    private String orderNo;
}
