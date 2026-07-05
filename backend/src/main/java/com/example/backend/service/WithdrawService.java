package com.example.backend.service;

import com.example.backend.common.Result;

public interface WithdrawService {
    Result<?> apply(Long userId);
    Result<?> list(Long userId, Integer page, Integer pageSize);
    Result<?> adminList(Integer page, Integer pageSize, Integer status);
    Result<?> adminProcess(Long id, Integer status, String remark);
}
