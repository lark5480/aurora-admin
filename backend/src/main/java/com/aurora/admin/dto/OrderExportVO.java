package com.aurora.admin.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.ContentStyle;
import com.alibaba.excel.annotation.write.style.HeadRowHeight;
import com.alibaba.excel.annotation.write.style.HeadStyle;
import com.alibaba.excel.enums.poi.BorderStyleEnum;
import com.alibaba.excel.enums.poi.FillPatternTypeEnum;

import java.math.BigDecimal;

/**
 * 订单导出 VO（EasyExcel 映射）
 */
@HeadRowHeight(22)
@HeadStyle(fillPatternType = FillPatternTypeEnum.SOLID_FOREGROUND,
        fillForegroundColor = 44,
        borderBottom = BorderStyleEnum.THIN,
        borderLeft = BorderStyleEnum.THIN,
        borderRight = BorderStyleEnum.THIN,
        borderTop = BorderStyleEnum.THIN)
@ContentStyle(borderBottom = BorderStyleEnum.THIN,
        borderLeft = BorderStyleEnum.THIN,
        borderRight = BorderStyleEnum.THIN,
        borderTop = BorderStyleEnum.THIN)
public class OrderExportVO {

    @ExcelProperty("订单号")
    @ColumnWidth(22)
    private String orderNo;

    @ExcelProperty("用户名")
    @ColumnWidth(14)
    private String username;

    @ExcelProperty("金额")
    @ColumnWidth(12)
    private BigDecimal totalAmount;

    @ExcelProperty("订单状态")
    @ColumnWidth(12)
    private String status;

    @ExcelProperty("收货人")
    @ColumnWidth(12)
    private String receiverName;

    @ExcelProperty("联系电话")
    @ColumnWidth(16)
    private String receiverPhone;

    @ExcelProperty("收货地址")
    @ColumnWidth(40)
    private String receiverAddress;

    @ExcelProperty("备注")
    @ColumnWidth(30)
    private String remark;

    @ExcelProperty("下单时间")
    @ColumnWidth(22)
    private String createTime;

    public static OrderExportVO from(com.aurora.admin.entity.Order order) {
        OrderExportVO vo = new OrderExportVO();
        vo.orderNo = order.getOrderNo();
        vo.username = order.getUsername();
        vo.totalAmount = order.getTotalAmount();
        vo.status = statusLabel(order.getStatus());
        vo.receiverName = order.getReceiverName();
        vo.receiverPhone = order.getReceiverPhone();
        vo.receiverAddress = order.getReceiverAddress();
        vo.remark = order.getRemark();
        vo.createTime = order.getCreateTime() != null ? order.getCreateTime().toString() : "";
        return vo;
    }

    private static String statusLabel(String status) {
        if (status == null) return "";
        return switch (status) {
            case "PENDING" -> "待支付";
            case "PAID" -> "已支付";
            case "SHIPPED" -> "已发货";
            case "COMPLETED" -> "已完成";
            case "CANCELLED" -> "已取消";
            default -> status;
        };
    }

    // Getters and setters required by EasyExcel
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getReceiverName() { return receiverName; }
    public void setReceiverName(String receiverName) { this.receiverName = receiverName; }
    public String getReceiverPhone() { return receiverPhone; }
    public void setReceiverPhone(String receiverPhone) { this.receiverPhone = receiverPhone; }
    public String getReceiverAddress() { return receiverAddress; }
    public void setReceiverAddress(String receiverAddress) { this.receiverAddress = receiverAddress; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getCreateTime() { return createTime; }
    public void setCreateTime(String createTime) { this.createTime = createTime; }
}
