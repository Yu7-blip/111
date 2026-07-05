package com.example.backend.common;

import com.example.backend.utils.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 熔断降级兜底服务 — 当后端服务不可用时返回降级数据
 *
 * 策略优先级：Redis缓存 > 静态兜底 > 服务繁忙提示
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CircuitBreakerFallback {

    private final RedisUtil redisUtil;

    /**
     * 店铺列表降级 — 优先返回Redis缓存，无缓存则返回空列表
     */
    @SuppressWarnings("unchecked")
    public Result<?> shopListFallback(Integer page, Integer pageSize, Double lat, Double lng,
                                      Double radius, String sort, Throwable t) {
        log.warn("Shop list circuit breaker triggered: {}", t.getMessage());

        // 尝试从Redis获取缓存数据（独立于正常缓存key，TTL更长）
        String cacheKey = "fallback:shops:list:" + page + ":" + pageSize;
        Object cached = redisUtil.get(cacheKey);
        if (cached != null && cached instanceof PageResult) {
            log.info("Shop list fallback: returning cached data");
            return Result.ok(cached);
        }

        // 无缓存时返回降级提示
        return Result.fail(ResultCode.INTERNAL_ERROR.getCode(), "附近商家查询繁忙，请下拉刷新重试");
    }

    /**
     * 订单创建降级
     */
    public Result<?> orderCreateFallback(Long userId, Object request, Throwable t) {
        log.warn("Order create circuit breaker triggered: {}", t.getMessage());
        return Result.fail(ResultCode.INTERNAL_ERROR.getCode(), "下单服务繁忙，请稍后重试");
    }

    /**
     * 订单支付降级
     */
    public Result<?> orderPayFallback(Long userId, Long orderId, Throwable t) {
        log.warn("Order pay circuit breaker triggered: orderId={}, msg={}", orderId, t.getMessage());
        return Result.fail(ResultCode.INTERNAL_ERROR.getCode(), "支付服务繁忙，订单已保存可稍后支付");
    }

    /**
     * 配送大厅降级
     */
    @SuppressWarnings("unchecked")
    public Result<?> deliveryLobbyFallback(Long userId, Double lat, Double lng, Throwable t) {
        log.warn("Delivery lobby circuit breaker triggered: {}", t.getMessage());

        String cacheKey = "fallback:delivery:lobby:" + userId;
        Object cached = redisUtil.get(cacheKey);
        if (cached != null && cached instanceof List) {
            log.info("Delivery lobby fallback: returning cached tasks");
            return Result.ok(cached);
        }

        return Result.ok(Collections.emptyList());
    }

    /**
     * 通用降级 — 返回服务繁忙
     */
    public Result<?> genericFallback(Throwable t) {
        log.warn("Generic circuit breaker triggered: {}", t.getMessage());
        return Result.fail(ResultCode.INTERNAL_ERROR.getCode(), "服务繁忙，请稍后重试");
    }
}
