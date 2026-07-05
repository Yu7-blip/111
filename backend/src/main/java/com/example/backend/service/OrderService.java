package com.example.backend.service;

import com.example.backend.common.Result;
import com.example.backend.dto.request.CreateOrderRequest;

public interface OrderService {
    Result<?> adminList(Integer page, Integer pageSize, String orderNo, String username, String merchantName, Integer status);
    Result<?> adminDetail(Long id);
    Result<?> adminCancel(Long id);
    Result<?> merchantList(Long shopId, Integer page, Integer pageSize, String keyword, Integer status);
    Result<?> merchantUpdateStatus(Long id, Integer status);
    Result<?> wxCreate(Long userId, CreateOrderRequest request);
    Result<?> wxList(Long userId, Integer tab, Integer page, Integer pageSize);
    Result<?> wxDetail(Long id);
    Result<?> wxPay(Long userId, Long id, String payMethod);
    Result<?> getChildren(Long parentOrderId);
    Result<?> wxCancle(Long userId, Long id);
    Result<?> wxRefund(Long userId, Long id, String reason);
    Result<?> merchantRefundList(Long shopId, Integer page, Integer pageSize);
    Result<?> merchantRefundApprove(Long shopId, Long id);
    Result<?> merchantRefundReject(Long shopId, Long id);
    Result<?> adminRefundList(Integer page, Integer pageSize);
    Result<?> adminRefundApprove(Long id);
    Result<?> adminRefundReject(Long id);
    Result<?> splitLargeOrder(Long orderId);
}
