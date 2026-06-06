package com.aurora.admin.service;

import com.aurora.admin.dto.CreateOrderRequest;
import com.aurora.admin.dto.OrderQuery;
import com.aurora.admin.dto.OrderResponse;
import com.aurora.admin.dto.PageResult;

public interface OrderService {

    /**
     * 创建订单（从购物车下单）
     */
    OrderResponse createOrder(Long userId, CreateOrderRequest request);

    /**
     * 分页查询用户订单
     */
    PageResult<OrderResponse> getOrderPage(Long userId, OrderQuery query);

    /**
     * 查询订单详情（含明细）
     */
    OrderResponse getOrderDetail(Long userId, Long orderId);

    /**
     * 取消订单（PENDING → CANCELLED）
     */
    void cancelOrder(Long userId, Long orderId);

    /**
     * 发货（PAID → SHIPPED），管理员操作
     */
    void shipOrder(Long orderId);

    /**
     * 确认收货（SHIPPED → COMPLETED）
     */
    void confirmOrder(Long userId, Long orderId);

    /**
     * 批量删除已取消的订单（逻辑删除，仅 CANCELLED 状态允许）
     * 普通用户只能删除自己的订单（userId != null 时校验归属）
     * @param ids 订单 ID 列表
     * @param userId 当前用户 ID，null 表示管理员（不校验归属）
     * @return 实际删除数量
     */
    int batchDeleteCancelled(java.util.List<Long> ids, Long userId);

    /**
     * 导出订单为 CSV（UTF-8 BOM + 逗号分隔，Excel 兼容）
     * @param userId 当前用户 ID，null 表示管理员
     * @param query  查询条件（忽略分页，最大导出 100000 条）
     * @return CSV 文件字节数组
     */
    byte[] exportOrders(Long userId, OrderQuery query);
}
