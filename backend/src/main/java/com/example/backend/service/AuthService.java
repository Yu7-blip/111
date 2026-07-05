package com.example.backend.service;

import com.example.backend.common.Result;
import com.example.backend.dto.request.LoginRequest;
import com.example.backend.dto.request.RegisterRequest;

import java.util.Map;

public interface AuthService {
    Result<?> adminLogin(LoginRequest request);
    Result<?> merchantLogin(LoginRequest request);
    Result<?> wxLogin(RegisterRequest request);
    Result<?> wxSendCode(String phone);
    Result<?> merchantUserInfo(Long shopId);
    Result<?> merchantEvaluations(Long shopId, Integer page, Integer pageSize);
    Result<?> merchantAppeal(Long shopId, Map<String, Object> body);
}
