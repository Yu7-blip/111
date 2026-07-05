package com.example.backend.controller;

import com.example.backend.common.Result;
import com.example.backend.service.DeliveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin/delivery")
@RequiredArgsConstructor
public class AdminDeliveryController {

    private final DeliveryService deliveryService;

    @GetMapping
    public Result<?> adminList(@RequestParam(defaultValue = "1") int page,
                               @RequestParam(defaultValue = "10") int pageSize,
                               @RequestParam(required = false) String name,
                               @RequestParam(required = false) String phone,
                               @RequestParam(required = false) Integer status) {
        return deliveryService.adminList(page, pageSize, name, phone, status);
    }

    @GetMapping("/{id}")
    public Result<?> adminDetail(@PathVariable Long id) {
        return deliveryService.adminDetail(id);
    }

    @PatchMapping("/{id}/status")
    public Result<?> adminUpdateStatus(@PathVariable Long id, @RequestParam Integer status) {
        return deliveryService.adminUpdateStatus(id, status);
    }

    /**
     * 骑手实名认证审核（通过/拒绝）
     */
    @PostMapping("/{id}/verify-review")
    public Result<?> reviewVerification(@PathVariable Long id,
                                        @RequestBody Map<String, Object> body) {
        Integer verifyStatus = body.get("verifyStatus") != null
                ? ((Number) body.get("verifyStatus")).intValue() : null;
        String remark = body.get("remark") != null ? body.get("remark").toString() : null;
        return deliveryService.reviewVerification(id, verifyStatus, remark);
    }
}
