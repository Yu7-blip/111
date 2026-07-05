package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.common.PageResult;
import com.example.backend.common.Result;
import com.example.backend.entity.Admin;
import com.example.backend.entity.AuditLog;
import com.example.backend.mapper.AdminMapper;
import com.example.backend.mapper.AuditLogMapper;
import com.example.backend.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogMapper auditLogMapper;
    private final AdminMapper adminMapper;

    @Override
    public void log(Long adminId, String adminName, String action, String targetType, Long targetId, String detail, String ip) {
        try {
            // 如果未传 adminName，从 DB 查
            if (!StringUtils.hasText(adminName) && adminId != null) {
                Admin admin = adminMapper.selectById(adminId);
                adminName = admin != null ? admin.getName() : "未知";
            }
            AuditLog log = new AuditLog();
            log.setAdminId(adminId);
            log.setAdminName(adminName);
            log.setAction(action);
            log.setTargetType(targetType);
            log.setTargetId(targetId);
            log.setDetail(detail);
            log.setIp(ip);
            log.setCreateTime(LocalDateTime.now());
            auditLogMapper.insert(log);
        } catch (Exception e) {
            log.error("Failed to write audit log: {}", e.getMessage());
        }
    }

    @Override
    public Result<?> list(Integer page, Integer pageSize, String action, String targetType, Long adminId) {
        LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(action)) {
            wrapper.eq(AuditLog::getAction, action);
        }
        if (StringUtils.hasText(targetType)) {
            wrapper.eq(AuditLog::getTargetType, targetType);
        }
        if (adminId != null) {
            wrapper.eq(AuditLog::getAdminId, adminId);
        }
        wrapper.orderByDesc(AuditLog::getCreateTime);

        Page<AuditLog> mpPage = new Page<>(page, pageSize);
        auditLogMapper.selectPage(mpPage, wrapper);

        return Result.ok(PageResult.of(mpPage.getRecords(), mpPage.getTotal(), mpPage.getCurrent(), mpPage.getSize()));
    }
}
