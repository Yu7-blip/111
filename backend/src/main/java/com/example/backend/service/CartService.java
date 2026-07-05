package com.example.backend.service;

import com.example.backend.common.Result;
import com.example.backend.dto.request.AddCartRequest;

public interface CartService {
    Result<?> list(Long userId);
    Result<?> add(Long userId, AddCartRequest request);
    Result<?> update(Long userId, Long cartId, Integer count);
    Result<?> delete(Long userId, Long cartId);
    Result<?> clear(Long userId);
}
