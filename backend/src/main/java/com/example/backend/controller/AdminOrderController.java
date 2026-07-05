package com.example.backend.controller;

import com.example.backend.common.Result;
import com.example.backend.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;

    @GetMapping
    public Result<?> adminList(@RequestParam(defaultValue = "1") int page,
                               @RequestParam(defaultValue = "10") int pageSize,
                               @RequestParam(required = false) String orderNo,
                               @RequestParam(required = false) String username,
                               @RequestParam(required = false) String merchantName,
                               @RequestParam(required = false) Integer status) {
        return orderService.adminList(page, pageSize, orderNo, username, merchantName, status);
    }

    @GetMapping("/{id}")
    public Result<?> adminDetail(@PathVariable Long id) {
        return orderService.adminDetail(id);
    }

    @PostMapping("/{id}/cancel")
    public Result<?> adminCancel(@PathVariable Long id) {
        return orderService.adminCancel(id);
    }

    @GetMapping("/refunds")
    public Result<?> adminRefundList(@RequestParam(defaultValue = "1") int page,
                                     @RequestParam(defaultValue = "10") int pageSize) {
        return orderService.adminRefundList(page, pageSize);
    }

    @PostMapping("/{id}/refund/approve")
    public Result<?> adminRefundApprove(@PathVariable Long id) {
        return orderService.adminRefundApprove(id);
    }

    @PostMapping("/{id}/refund/reject")
    public Result<?> adminRefundReject(@PathVariable Long id) {
        return orderService.adminRefundReject(id);
    }

    @GetMapping("/{id}/children")
    public Result<?> adminChildren(@PathVariable Long id) {
        return orderService.getChildren(id);
    }
}
