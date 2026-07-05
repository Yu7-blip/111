package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.entity.EventLog;
import com.example.backend.mapper.EventLogMapper;
import com.example.backend.event.EventMessage;
import com.example.backend.event.RedisMessagePublisher;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 本地事件表服务 — Transaction Outbox 模式
 *
 * 核心原理：
 * 1. 业务操作 + event_log 写入在同一数据库事务中，保证原子性
 * 2. 事务提交后立即通过 Redis Pub/Sub 尝试实时处理（快速路径）
 * 3. 定时任务兜底扫描未处理事件，保证最终一致性
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventLogService {

    private final EventLogMapper eventLogMapper;
    private final RedisMessagePublisher redisMessagePublisher;
    private final ObjectMapper objectMapper;

    // 最大重试次数
    private static final int MAX_RETRY = 5;

    /**
     * 写入事件（在业务事务中调用）
     * 返回写入的事件ID，供后续实时推送使用
     */
    public Long saveEvent(String eventType, Map<String, Object> payload) {
        EventLog eventLog = new EventLog();
        eventLog.setEventType(eventType);
        try {
            eventLog.setPayload(objectMapper.writeValueAsString(payload));
        } catch (Exception e) {
            log.error("Failed to serialize event payload", e);
            eventLog.setPayload("{}");
        }
        eventLog.setStatus(0);
        eventLog.setRetryCount(0);
        eventLog.setCreateTime(LocalDateTime.now());
        eventLogMapper.insert(eventLog);
        return eventLog.getId();
    }

    /**
     * 事务提交后 — 尝试实时处理（通过 Redis Pub/Sub）
     * 注意：在事务外调用，此时 event_log 已持久化
     */
    public void tryPublishAfterCommit(Long eventId) {
        try {
            EventLog eventLog = eventLogMapper.selectById(eventId);
            if (eventLog == null) return;

            Map<String, Object> payload = objectMapper.readValue(
                    eventLog.getPayload(),
                    new TypeReference<Map<String, Object>>() {});

            EventMessage msg = EventMessage.builder()
                    .type(eventLog.getEventType())
                    .data(payload)
                    .timestamp(LocalDateTime.now())
                    .build();

            // 根据事件类型发送到对应频道
            switch (eventLog.getEventType()) {
                case EventMessage.ORDER_PAID, EventMessage.ORDER_CREATED, EventMessage.REFUND_PROCESSED ->
                        redisMessagePublisher.publishOrderEvent(msg);
                case EventMessage.EVALUATION_SUBMITTED ->
                        redisMessagePublisher.publishEvaluationEvent(msg);
                case EventMessage.DELIVERY_COMPLETED ->
                        redisMessagePublisher.publishDeliveryEvent(msg);
            }

            // 标记为已处理（Redis 发送成功即视为处理中，监听器会最终处理）
            eventLog.setStatus(1);
            eventLog.setUpdateTime(LocalDateTime.now());
            eventLogMapper.updateById(eventLog);

        } catch (Exception e) {
            log.warn("Failed to publish event {} via Redis, will retry via scheduler: {}",
                    eventId, e.getMessage());
        }
    }

    /**
     * 兜底扫描 — 定时任务调用
     * 处理所有 status=0 且超过60秒未处理的事件（Redis 路径失败后的补偿）
     */
    public int processStaleEvents() {
        LocalDateTime deadline = LocalDateTime.now().minusSeconds(60);

        LambdaQueryWrapper<EventLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EventLog::getStatus, 0)
                .lt(EventLog::getCreateTime, deadline)
                .orderByAsc(EventLog::getCreateTime)
                .last("LIMIT 50");  // 每次最多处理50条，避免单次扫描过重

        List<EventLog> staleEvents = eventLogMapper.selectList(wrapper);
        int processed = 0;

        for (EventLog eventLog : staleEvents) {
            boolean success = processEvent(eventLog);
            if (success) {
                processed++;
            }
        }

        if (processed > 0) {
            log.info("Scheduled retry: processed {} stale events", processed);
        }
        return processed;
    }

    /**
     * 处理单个事件 — 直接执行业务逻辑（兜底路径，不经过 Redis）
     * 事件数据驱动：根据 event_type 调用对应的业务处理
     */
    private boolean processEvent(EventLog eventLog) {
        try {
            // 超过最大重试次数，标记为失败
            if (eventLog.getRetryCount() != null && eventLog.getRetryCount() >= MAX_RETRY) {
                eventLog.setStatus(2);
                eventLog.setErrorMsg("超过最大重试次数(" + MAX_RETRY + ")");
                eventLog.setUpdateTime(LocalDateTime.now());
                eventLogMapper.updateById(eventLog);
                log.warn("Event {} permanently failed after {} retries", eventLog.getId(), MAX_RETRY);
                return false;
            }

            // 重新尝试通过 Redis 发布
            Map<String, Object> payload = objectMapper.readValue(
                    eventLog.getPayload(),
                    new TypeReference<Map<String, Object>>() {});

            EventMessage msg = EventMessage.builder()
                    .type(eventLog.getEventType())
                    .data(payload)
                    .timestamp(LocalDateTime.now())
                    .build();

            switch (eventLog.getEventType()) {
                case EventMessage.ORDER_PAID, EventMessage.ORDER_CREATED, EventMessage.REFUND_PROCESSED ->
                        redisMessagePublisher.publishOrderEvent(msg);
                case EventMessage.EVALUATION_SUBMITTED ->
                        redisMessagePublisher.publishEvaluationEvent(msg);
                case EventMessage.DELIVERY_COMPLETED ->
                        redisMessagePublisher.publishDeliveryEvent(msg);
            }

            eventLog.setStatus(1);
            eventLog.setUpdateTime(LocalDateTime.now());
            eventLogMapper.updateById(eventLog);
            return true;

        } catch (Exception e) {
            eventLog.setRetryCount((eventLog.getRetryCount() != null ? eventLog.getRetryCount() : 0) + 1);
            String errMsg = e.getMessage();
            if (errMsg != null && errMsg.length() > 450) errMsg = errMsg.substring(0, 450);
            eventLog.setErrorMsg(errMsg);
            eventLog.setUpdateTime(LocalDateTime.now());

            if (eventLog.getRetryCount() >= MAX_RETRY) {
                eventLog.setStatus(2);
                log.error("Event {} permanently failed: {}", eventLog.getId(), e.getMessage());
            }

            eventLogMapper.updateById(eventLog);
            return false;
        }
    }
}
