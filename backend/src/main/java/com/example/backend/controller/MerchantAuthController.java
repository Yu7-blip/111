package com.example.backend.controller;

import com.example.backend.common.RateLimit;
import com.example.backend.common.Result;
import com.example.backend.dto.request.LoginRequest;
import com.example.backend.service.AuthService;
import com.example.backend.service.StatisticsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/merchant")
@RequiredArgsConstructor
public class MerchantAuthController {

    private final AuthService authService;
    private final StatisticsService statisticsService;

    @RateLimit(key = "merchant-login", maxCount = 5, seconds = 60)
    @PostMapping("/login")
    public Result<?> merchantLogin(@Valid @RequestBody LoginRequest request) {
        log.info("Merchant login: username={}", request.getUsername());
        return authService.merchantLogin(request);
    }

    @GetMapping("/userinfo")
    public Result<?> merchantUserInfo(@RequestAttribute("userId") Long userId) {
        log.info("Get merchant userinfo: shopId={}", userId);
        return authService.merchantUserInfo(userId);
    }

    @GetMapping("/dashboard")
    public Result<?> merchantDashboard(@RequestAttribute("userId") Long userId) {
        log.info("Merchant dashboard: shopId={}", userId);
        return statisticsService.merchantDashboardStats(userId);
    }

    @GetMapping("/evaluations")
    public Result<?> merchantEvaluations(@RequestAttribute("userId") Long shopId,
                                         @RequestParam(defaultValue = "1") Integer page,
                                         @RequestParam(defaultValue = "10") Integer pageSize) {
        return authService.merchantEvaluations(shopId, page, pageSize);
    }

    @PostMapping("/appeal")
    public Result<?> merchantAppeal(@RequestAttribute("userId") Long shopId,
                                     @RequestBody Map<String, Object> body) {
        return authService.merchantAppeal(shopId, body);
    }
}
