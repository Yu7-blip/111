package com.example.backend.controller;

import com.example.backend.common.RateLimit;
import com.example.backend.common.Result;
import com.example.backend.dto.request.RegisterRequest;
import com.example.backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wx")
@RequiredArgsConstructor
@Slf4j
public class WxAuthController {

    private final AuthService authService;

    @RateLimit(key = "login", maxCount = 5, seconds = 60)
    @PostMapping("/login")
    public Result<?> wxLogin(@Valid @RequestBody RegisterRequest request) {
        log.info("wx login: phone={}", request.getPhone());
        return authService.wxLogin(request);
    }

    @RateLimit(key = "send-code", maxCount = 1, seconds = 60)
    @PostMapping("/send-code")
    public Result<?> wxSendCode(@RequestParam String phone) {
        log.info("wx send code: phone={}", phone);
        return authService.wxSendCode(phone);
    }
}
