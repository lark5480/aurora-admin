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

    PageResult<AfterSaleResponse> getAfterSalePage(Long userId, AfterSaleQuery query);

    AfterSaleResponse getAfterSaleDetail(Long userId, Long id);
}
