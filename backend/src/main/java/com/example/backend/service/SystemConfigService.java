package com.example.backend.service;

import com.example.backend.common.Result;

import java.util.Map;

public interface SystemConfigService {
    Result<?> list(Integer page, Integer pageSize, String key);
    Result<?> getByKey(String key);
    Result<?> update(Long id, Map<String, Object> data);
    Result<?> create(Map<String, Object> data);
    Result<?> delete(Long id);
    String getConfigValue(String key);
}
