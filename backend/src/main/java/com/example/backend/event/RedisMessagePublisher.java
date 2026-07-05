package com.example.backend.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Redis Pub/Sub 消息发布者 — 将同步操作解耦为异步事件
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisMessagePublisher {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    /** Redis Pub/Sub 频道名 */
    public static final String CHANNEL_ORDER_EVENTS = "delivery:order-events";
    public static final String CHANNEL_EVALUATION_EVENTS = "delivery:evaluation-events";
    public static final String CHANNEL_DELIVERY_EVENTS = "delivery:delivery-events";

    /**
     * 发布订单事件
     */
    public void publishOrderEvent(EventMessage event) {
        publish(CHANNEL_ORDER_EVENTS, event);
    }

    /**
     * 发布评价事件
     */
    public void publishEvaluationEvent(EventMessage event) {
        publish(CHANNEL_EVALUATION_EVENTS, event);
    }

    /**
     * 发布配送事件
     */
    public void publishDeliveryEvent(EventMessage event) {
        publish(CHANNEL_DELIVERY_EVENTS, event);
    }

    private void publish(String channel, EventMessage event) {
        try {
            byte[] rawMsg = objectMapper.writeValueAsBytes(event);
            // 使用底层连接直接发布 raw bytes，避免 RedisTemplate 的序列化器二次序列化
            redisTemplate.execute((org.springframework.data.redis.core.RedisCallback<Object>) connection -> {
                connection.publish(channel.getBytes(StandardCharsets.UTF_8), rawMsg);
                return null;
            });
            log.info("Published event [{}] to channel [{}]", event.getType(), channel);
        } catch (Exception e) {
            log.error("Failed to publish event [{}]: {}", event.getType(), e.getMessage());
        }
    }
}
