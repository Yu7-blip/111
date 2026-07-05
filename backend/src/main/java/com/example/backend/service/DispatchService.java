package com.example.backend.service;

import com.example.backend.common.Result;

import java.util.List;
import java.util.Map;

public interface DispatchService {
    /**
     * 核心派单：为订单找到最佳骑手并分配
     * 返回分配结果，或 null（进入抢单池）
     */
    Result<?> dispatch(Long orderId);

    /**
     * 为指定骑手计算对某订单的匹配分数
     * @return 0-1 之间的分数，null 表示不适合
     */
    Double scoreRiderForOrder(Long deliveryUserId, Long orderId);

    /**
     * 计算所有在线骑手对某订单的分数排名
     * @return 按分数降序排列的骑手列表
     */
    List<Map<String, Object>> scoreAllRiders(Long orderId);
}
