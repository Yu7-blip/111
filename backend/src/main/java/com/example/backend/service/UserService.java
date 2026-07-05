package com.example.backend.service;

import com.example.backend.common.Result;

import java.util.Map;

public interface UserService {
    Result<?> list(Integer page, Integer pageSize, String username, String phone, Integer status);
    Result<?> create(Map<String, Object> data);
    Result<?> update(Long id, Map<String, Object> data);
    Result<?> delete(Long id);
    Result<?> updateStatus(Long id, Integer status);
}
