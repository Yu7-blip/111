package com.example.backend.controller;

import com.example.backend.common.Result;
import com.example.backend.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/merchant/orders")
@RequiredArgsConstructor
public class MerchantOrderController {

    private final OrderService orderService;

    @GetMapping
    public Result<?> merchantList(@RequestAttribute("userId") Long userId,
                                  @RequestParam(defaultValue = "1") Integer page,
                                  @RequestParam(defaultValue = "10") Integer pageSize,
                                  @RequestParam(required = false) String keyword,
                                  @RequestParam(required = false) Integer status) {
        log.info("Merchant order list: shopId={}, page={}, pageSize={}", userId, page, pageSize);
        return orderService.merchantList(userId, page, pageSize, keyword, status);
    }

    @PatchMapping("/{id}/status")
    public Result<?> merchantUpdateStatus(@PathVariable Long id,
                                          @RequestParam Integer status) {
        log.info("Merchant order update status: id={}, status={}", id, status);
        return orderService.merchantUpdateStatus(id, status);
    }

    @GetMapping("/refunds")
    public Result<?> merchantRefundList(@RequestAttribute("userId") Long shopId,
                                        @RequestParam(defaultValue = "1") Integer page,
                                        @RequestParam(defaultValue = "10") Integer pageSize) {
        log.info("Merchant refund list: shopId={}", shopId);
        return orderService.merchantRefundList(shopId, page, pageSize);
    }

    @PostMapping("/{id}/refund/approve")
    public Result<?> merchantRefundApprove(@RequestAttribute("userId") Long shopId,
                                           @PathVariable Long id) {
        log.info("Merchant refund approve: shopId={}, orderId={}", shopId, id);
        return orderService.merchantRefundApprove(shopId, id);
    }

    @PostMapping("/{id}/refund/reject")
    public Result<?> merchantRefundReject(@RequestAttribute("userId") Long shopId,
                                          @PathVariable Long id) {
        log.info("Merchant refund reject: shopId={}, orderId={}", shopId, id);
        return orderService.merchantRefundReject(shopId, id);
    }

    @PostMapping("/{id}/refund/reject-and-escalate")
    public Result<?> merchantRefundRejectEscalate(@RequestAttribute("userId") Long shopId,
                                                   @PathVariable Long id) {
        log.info("Merchant refund reject and escalate: shopId={}, orderId={}", shopId, id);
        return orderService.merchantRefundReject(shopId, id);
    }

    @PostMapping("/{id}/split")
    public Result<?> splitLargeOrder(@RequestAttribute("userId") Long shopId,
                                      @PathVariable Long id) {
        log.info("Split large order: shopId={}, orderId={}", shopId, id);
        return orderService.splitLargeOrder(id);
    }

    @GetMapping("/{id}/children")
    public Result<?> merchantChildren(@PathVariable Long id) {
        return orderService.getChildren(id);
    }
}
