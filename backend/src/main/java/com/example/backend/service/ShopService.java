package com.example.backend.service;

import com.example.backend.common.Result;
import com.example.backend.dto.request.ShopRegisterRequest;

import java.util.Map;

public interface ShopService {
    Result<?> list(Integer page, Integer pageSize, String name, Integer status);
    Result<?> detail(Long id);
    Result<?> audit(Long id, Integer status, String remark);
    Result<?> register(ShopRegisterRequest request);
    Result<?> getMerchantShop(Long shopId);
    Result<?> updateMerchantShop(Long shopId, Map<String, Object> data);
    Result<?> adminCreate(Map<String, Object> data);
    Result<?> adminUpdate(Long id, Map<String, Object> data);
    Result<?> adminDelete(Long id);
    Result<?> toggleBusinessStatus(Long shopId, Integer businessStatus);
    void updateGeohash(Long shopId);

    /**
     * 重新计算店铺评分（排除已撤销的评价）
     */
    void recalculateShopRating(Long shopId);
}
