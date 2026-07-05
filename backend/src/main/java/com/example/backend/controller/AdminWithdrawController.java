package com.example.backend.controller;

import com.example.backend.common.Result;
import com.example.backend.service.WithdrawService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin/withdraw")
@RequiredArgsConstructor
public class AdminWithdrawController {

    private final WithdrawService withdrawService;

    @GetMapping
    public Result<?> adminList(@RequestParam(defaultValue = "1") Integer page,
                                @RequestParam(defaultValue = "10") Integer pageSize,
                                @RequestParam(required = false) Integer status) {
        return withdrawService.adminList(page, pageSize, status);
    }

    @PutMapping("/{id}/process")
    public Result<?> adminProcess(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        Integer status = ((Number) data.get("status")).intValue();
        String remark = (String) data.getOrDefault("remark", "");
        return withdrawService.adminProcess(id, status, remark);
    }
}
