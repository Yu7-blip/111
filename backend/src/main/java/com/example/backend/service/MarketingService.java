package com.example.backend.service;

import com.example.backend.common.Result;
import com.example.backend.entity.FullReduceActivity;

import java.util.Map;

public interface MarketingService {
    Result<?> activityList(Integer page, Integer pageSize, String name, Integer status);
    Result<?> activityCreate(Map<String, Object> data);
    Result<?> activityUpdate(Long id, Map<String, Object> data);
    Result<?> activityDelete(Long id);
    Result<?> activityUpdateStatus(Long id, Integer status);

    /**
     * 查询适用于当前订单的最佳满减活动
     * @param shopId 店铺ID
     * @param orderAmount 订单商品总金额
     * @return 最佳匹配活动，无匹配返回null
     */
    FullReduceActivity getBestActivity(Long shopId, java.math.BigDecimal orderAmount);
}
