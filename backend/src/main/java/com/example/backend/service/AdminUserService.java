package com.example.backend.service;

import com.example.backend.common.Result;

import java.util.Map;

public interface AdminUserService {
    Result<?> list(Integer page, Integer pageSize, String username, String role, Integer status);
    Result<?> detail(Long id);
    Result<?> create(Map<String, Object> data);
    Result<?> update(Long id, Map<String, Object> data);
    Result<?> delete(Long id);
    Result<?> updateStatus(Long id, Integer status);
}
