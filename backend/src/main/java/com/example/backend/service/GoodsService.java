package com.example.backend.service;

import com.example.backend.common.Result;
import com.example.backend.dto.request.GoodsSaveRequest;

public interface GoodsService {
    Result<?> merchantList(Long shopId, Integer page, Integer pageSize, String name, Integer status, String category);
    Result<?> merchantDetail(Long id);
    Result<?> merchantCreate(Long shopId, GoodsSaveRequest request);
    Result<?> merchantUpdate(Long id, GoodsSaveRequest request);
    Result<?> merchantDelete(Long id);
    Result<?> merchantToggleStatus(Long id, Integer status);
    Result<?> wxShopGoods(Long shopId);
    Result<?> merchantCategories(Long shopId);
}
