package com.example.backend.interceptor;

import com.example.backend.common.RateLimit;
import com.example.backend.utils.RedisUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RedisUtil redisUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RateLimit rateLimit = handlerMethod.getMethodAnnotation(RateLimit.class);
        if (rateLimit == null) {
            return true;
        }

        String key = buildKey(request, rateLimit);
        int current = incrementAndGet(key, rateLimit.seconds());

        if (current > rateLimit.maxCount()) {
            log.warn("Rate limit exceeded: key={}, count={}, max={}", key, current, rateLimit.maxCount());
            writeLimitResponse(response);
            return false;
        }

        return true;
    }

    private String buildKey(HttpServletRequest request, RateLimit rateLimit) {
        String prefix = rateLimit.key().isEmpty() ? request.getRequestURI() : rateLimit.key();
        String identifier;
        Object userId = request.getAttribute("userId");
        if (userId != null) {
            identifier = "user:" + userId;
        } else {
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isEmpty()) {
                ip = request.getRemoteAddr();
            }
            identifier = "ip:" + ip;
        }
        return "rate_limit:" + prefix + ":" + identifier;
    }

    private int incrementAndGet(String key, int seconds) {
        String current = redisUtil.getString(key);
        if (current == null) {
            redisUtil.set(key, "1", seconds, TimeUnit.SECONDS);
            return 1;
        }
        int count = Integer.parseInt(current) + 1;
        long ttl = redisUtil.getExpire(key);
        redisUtil.set(key, String.valueOf(count), ttl > 0 ? ttl : seconds, TimeUnit.SECONDS);
        return count;
    }

    private void writeLimitResponse(HttpServletResponse response) throws IOException {
        response.setStatus(429);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":429,\"msg\":\"请求过于频繁，请稍后再试\",\"data\":null}");
    }
}
