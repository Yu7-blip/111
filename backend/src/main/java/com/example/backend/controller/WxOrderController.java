package com.example.backend.controller;

import com.example.backend.common.RateLimit;
import com.example.backend.common.Result;
import com.example.backend.common.ResultCode;
import com.example.backend.dto.request.CreateOrderRequest;
import com.example.backend.dto.request.PayRequest;
import com.example.backend.entity.FullReduceActivity;
import com.example.backend.service.MarketingService;
import com.example.backend.service.OrderService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wx/orders")
@RequiredArgsConstructor
@Slf4j
public class WxOrderController {

    private final OrderService orderService;
    private final MarketingService marketingService;

    @RateLimit(key = "create-order", maxCount = 10, seconds = 60)
    @PostMapping
    @CircuitBreaker(name = "orderService", fallbackMethod = "orderCreateFallback")
    public Result<?> wxCreate(@RequestAttribute("userId") Long userId,
                              @Valid @RequestBody CreateOrderRequest request) {
        log.info("wx order create: userId={}, shopId={}", userId, request.getShopId());
        return orderService.wxCreate(userId, request);
    }

    @GetMapping
    public Result<?> wxList(@RequestAttribute("userId") Long userId,
                            @RequestParam(defaultValue = "0") Integer tab,
                            @RequestParam(defaultValue = "1") Integer page,
                            @RequestParam(defaultValue = "10") Integer pageSize) {
        log.info("wx order list: userId={}, tab={}, page={}, pageSize={}", userId, tab, page, pageSize);
        return orderService.wxList(userId, tab, page, pageSize);
    }

    @GetMapping("/{id}")
    public Result<?> wxDetail(@PathVariable Long id) {
        log.info("wx order detail: id={}", id);
        return orderService.wxDetail(id);
    }

    @RateLimit(key = "pay", maxCount = 5, seconds = 60)
    @PostMapping("/{id}/pay")
    @CircuitBreaker(name = "orderService", fallbackMethod = "orderPayFallback")
    public Result<?> wxPay(@RequestAttribute("userId") Long userId,
                           @PathVariable Long id,
                           @Valid @RequestBody PayRequest request) {
        log.info("wx order pay: userId={}, orderId={}, payMethod={}", userId, id, request.getPayMethod());
        return orderService.wxPay(userId, id, request.getPayMethod());
    }

    @PostMapping("/{id}/cancel")
    public Result<?> wxCancle(@RequestAttribute("userId") Long userId,
                              @PathVariable Long id) {
        log.info("wx order cancel: userId={}, orderId={}", userId, id);
        return orderService.wxCancle(userId, id);
    }

    @RateLimit(key = "refund", maxCount = 3, seconds = 60)
    @PostMapping("/{id}/refund")
    public Result<?> wxRefund(@RequestAttribute("userId") Long userId,
                              @PathVariable Long id,
                              @RequestParam String reason) {
        log.info("wx order refund: userId={}, orderId={}, reason={}", userId, id, reason);
        return orderService.wxRefund(userId, id, reason);
    }

    /**
     * 预览满减活动（下单前展示可用优惠）
     */
    @GetMapping("/active-activity")
    public Result<?> previewActivity(@RequestParam Long shopId,
                                     @RequestParam java.math.BigDecimal amount) {
        FullReduceActivity activity = marketingService.getBestActivity(shopId, amount);
        if (activity != null) {
            return Result.ok(java.util.Map.of(
                    "name", activity.getName(),
                    "conditionAmount", activity.getConditionAmount(),
                    "reduceAmount", activity.getReduceAmount()
            ));
        }
        return Result.ok(null);
    }

    // ==================== 熔断降级 ====================

    public Result<?> orderCreateFallback(Long userId, CreateOrderRequest request, Throwable t) {
        log.warn("Order create circuit breaker OPEN: {}", t.getMessage());
        return Result.fail(ResultCode.INTERNAL_ERROR.getCode(), "下单服务繁忙，请稍后重试");
    }

    public Result<?> orderPayFallback(Long userId, Long orderId, Throwable t) {
        log.warn("Order pay circuit breaker OPEN: orderId={}, msg={}", orderId, t.getMessage());
        return Result.fail(ResultCode.INTERNAL_ERROR.getCode(), "支付服务繁忙，订单已保存可稍后支付");
    }
}
