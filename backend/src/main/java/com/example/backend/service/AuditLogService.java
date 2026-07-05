package com.example.backend.service;

import com.example.backend.common.Result;

public interface AuditLogService {
    void log(Long adminId, String adminName, String action, String targetType, Long targetId, String detail, String ip);
    Result<?> list(Integer page, Integer pageSize, String action, String targetType, Long adminId);
}
