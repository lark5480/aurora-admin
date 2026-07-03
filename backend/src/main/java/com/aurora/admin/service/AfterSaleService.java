package com.aurora.admin.service;

import com.aurora.admin.dto.*;

public interface AfterSaleService {

    /** 用户提交售后申请，状态为 APPLIED，等待审核 */
    AfterSaleResponse createAfterSale(Long userId, CreateAfterSaleRequest request);

    /** 整单批量售后：对订单内所有未退款的明细行发起售后，返回创建的售后单数量 */
    int createAfterSaleBatch(Long userId, CreateAfterSaleBatchRequest request);

    /** 管理员审核通过，执行退款 + 恢复库存 */
    AfterSaleResponse approve(Long adminId, Long id, String remark);

    /** 管理员驳回，状态变为 REJECTED */
    AfterSaleResponse reject(Long adminId, Long id, String remark);

    /** 自动审核超过24小时的售后单 */
    int autoApproveExpired();

    /**
     * 分页查询售后记录。根据用户身份和查询条件分页获取售后列表，管理员可查全部，普通用户仅查自己的记录。
     *
     * @param userId 用户ID（传 null 表示管理员，不限制用户）
     * @param query  分页及筛选条件，支持按订单ID、状态、售后单号、订单号筛选
     * @return 售后记录分页结果
     */
    PageResult<AfterSaleResponse> getAfterSalePage(Long userId, AfterSaleQuery query);

    /**
     * 查询售后详情。按售后记录ID获取单条详情，管理员可查看全部，普通用户仅查看自己的记录。
     *
     * @param userId 用户ID（传 null 表示管理员，不限制用户）
     * @param id     售后记录ID
     * @return 售后记录详情
     * @throws NotFoundException  售后记录不存在
     * @throws ForbiddenException 非管理员且非本人记录时抛出
     */
    AfterSaleResponse getAfterSaleDetail(Long userId, Long id);
}
