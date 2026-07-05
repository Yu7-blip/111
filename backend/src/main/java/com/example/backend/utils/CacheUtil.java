package com.example.backend.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 缓存工具 — 防止缓存穿透/击穿/雪崩
 *
 * 穿透: 查一个不存在的数据 → 布隆过滤器 + 空值缓存
 * 击穿: 热点key过期瞬间大量请求 → 互斥锁(mutex)重建
 * 雪崩: 大量key同时过期 → 随机TTL
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CacheUtil {

    private final RedisUtil redisUtil;

    // ==================== 空值缓存（防穿透） ====================

    private static final String NULL_PLACEHOLDER = "__NULL__";
    private static final long NULL_TTL_MINUTES = 5;

    /**
     * 带空值缓存的查询：查不到的数据缓存一个占位符，下次直接返回 null
     * 避免恶意请求不存在的 ID 穿透到数据库
     */
    @SuppressWarnings("unchecked")
    public <T> T getOrLoadWithNullGuard(String key, Class<T> type,
                                         Supplier<T> loader, long ttl, TimeUnit unit) {
        Object cached = redisUtil.get(key);
        if (cached != null) {
            if (NULL_PLACEHOLDER.equals(cached)) {
                return null; // 缓存了空值，避免穿透DB
            }
            return (T) cached;
        }

        // 缓存未命中，从DB加载
        T value = loader.get();
        if (value == null) {
            // 存空占位符，防止穿透
            redisUtil.set(key, NULL_PLACEHOLDER, NULL_TTL_MINUTES, TimeUnit.MINUTES);
            return null;
        }
        redisUtil.set(key, value, ttl, unit);
        return value;
    }

    // ==================== 互斥锁（防击穿） ====================

    private final ConcurrentHashMap<String, Object> mutexLocks = new ConcurrentHashMap<>();

    /**
     * 互斥加载：热点key过期时，只有第一个请求去重建缓存，其他请求等待
     * 防止大量请求同时打到数据库
     */
    @SuppressWarnings("unchecked")
    public <T> T getOrLoadWithMutex(String key, Class<T> type,
                                     Supplier<T> loader, long ttl, TimeUnit unit) {
        Object cached = redisUtil.get(key);
        if (cached != null) {
            if (NULL_PLACEHOLDER.equals(cached)) return null;
            return (T) cached;
        }

        // 获取互斥锁
        String lockKey = "mutex:" + key;
        Object lock = mutexLocks.computeIfAbsent(lockKey, k -> new Object());

        synchronized (lock) {
            try {
                // Double-check: 可能其他线程已经重建了
                Object doubleCheck = redisUtil.get(key);
                if (doubleCheck != null) {
                    if (NULL_PLACEHOLDER.equals(doubleCheck)) return null;
                    return (T) doubleCheck;
                }

                T value = loader.get();
                if (value == null) {
                    redisUtil.set(key, NULL_PLACEHOLDER, NULL_TTL_MINUTES, TimeUnit.MINUTES);
                    return null;
                }
                redisUtil.set(key, value, ttl, unit);
                return value;
            } finally {
                mutexLocks.remove(lockKey);
            }
        }
    }

    // ==================== 随机TTL（防雪崩） ====================

    /**
     * 写入缓存时给 TTL 加随机偏移（±30%），防止大量 key 同时过期
     */
    public void setWithJitter(String key, Object value, long baseTtl, TimeUnit unit) {
        long jitter = (long) (baseTtl * 0.3 * Math.random());
        long ttl = baseTtl + (Math.random() > 0.5 ? jitter : -jitter);
        if (ttl <= 0) ttl = baseTtl;
        redisUtil.set(key, value, ttl, unit);
    }

    // ==================== 布隆过滤器（防穿透 — 简易版） ====================

    private static final String BLOOM_PREFIX = "bloom:";
    private static final int BLOOM_SIZE = 1000000;

    /**
     * 添加到布隆过滤器（基于Redis Bitmap）
     * 适用于判断"某个ID是否可能存在"
     */
    public void bloomAdd(String namespace, String value) {
        String key = BLOOM_PREFIX + namespace;
        int[] offsets = bloomHash(value, BLOOM_SIZE);
        for (int offset : offsets) {
            redisUtil.setBit(key, offset, true);
        }
    }

    /**
     * 布隆过滤器判断：返回 false 则一定不存在，返回 true 则可能存在
     */
    public boolean bloomMightContain(String namespace, String value) {
        String key = BLOOM_PREFIX + namespace;
        int[] offsets = bloomHash(value, BLOOM_SIZE);
        for (int offset : offsets) {
            Boolean bit = redisUtil.getBit(key, offset);
            if (bit == null || !bit) return false;
        }
        return true;
    }

    /**
     * 带布隆过滤器的查询：先查布隆过滤器，不存在直接返回 null
     */
    @SuppressWarnings("unchecked")
    public <T> T getOrLoadWithBloom(String namespace, String bloomValue,
                                     String cacheKey, Class<T> type,
                                     Supplier<T> loader, long ttl, TimeUnit unit) {
        // 布隆过滤器判断
        if (!bloomMightContain(namespace, bloomValue)) {
            return null; // 一定不存在，直接返回，不查DB
        }

        // 查缓存
        Object cached = redisUtil.get(cacheKey);
        if (cached != null) {
            if (NULL_PLACEHOLDER.equals(cached)) return null;
            return (T) cached;
        }

        // 查DB + 缓存
        T value = loader.get();
        if (value == null) {
            redisUtil.set(cacheKey, NULL_PLACEHOLDER, NULL_TTL_MINUTES, TimeUnit.MINUTES);
            return null;
        }
        bloomAdd(namespace, bloomValue); // 确认存在，加到布隆过滤器
        setWithJitter(cacheKey, value, ttl, unit); // 用随机TTL防雪崩
        return value;
    }

    // ---- 内部 ----

    private int[] bloomHash(String value, int size) {
        int h1 = value.hashCode();
        int h2 = h1 >>> 16;
        return new int[]{
            Math.abs(h1 % size),
            Math.abs((h1 ^ h2) % size),
            Math.abs((h1 + h2 * 31) % size)
        };
    }
}
