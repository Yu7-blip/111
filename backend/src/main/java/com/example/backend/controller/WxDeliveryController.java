package com.example.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.common.Result;
import com.example.backend.common.ResultCode;
import com.example.backend.dto.request.RiderLocationRequest;
import com.example.backend.entity.*;
import com.example.backend.mapper.*;
import com.example.backend.service.TencentMapService;
import com.example.backend.service.DeliveryService;
import com.example.backend.service.WithdrawService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDateTime;
import java.util.*;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wx/delivery")
@RequiredArgsConstructor
@Slf4j
public class WxDeliveryController {

    private final DeliveryService deliveryService;
    private final WithdrawService withdrawService;
    private final TencentMapService tencentMapService;
    private final EvaluationMapper evaluationMapper;
    private final DeliveryMapper deliveryMapper;
    private final OrderMapper orderMapper;
    private final UserMapper userMapper;
    private final FeedbackMapper feedbackMapper;

    @GetMapping("/lobby")
    @CircuitBreaker(name = "deliveryService", fallbackMethod = "deliveryLobbyFallback")
    public Result<?> wxLobby(@RequestAttribute("userId") Long userId,
                             @RequestParam(required = false) Double lat,
                             @RequestParam(required = false) Double lng) {
        log.info("wx delivery lobby: userId={}, lat={}, lng={}", userId, lat, lng);
        return deliveryService.wxLobby(userId, lat, lng);
    }

    @PostMapping("/grab/{orderId}")
    public Result<?> wxGrab(@RequestAttribute("userId") Long userId,
                            @PathVariable Long orderId) {
        log.info("wx delivery grab: userId={}, orderId={}", userId, orderId);
        return deliveryService.wxGrab(userId, orderId);
    }

    @GetMapping("/tasks")
    public Result<?> wxTasks(@RequestAttribute("userId") Long userId,
                             @RequestParam(required = false) Double lat,
                             @RequestParam(required = false) Double lng) {
        log.info("wx delivery tasks: userId={}, lat={}, lng={}", userId, lat, lng);
        return deliveryService.wxTasks(userId, lat, lng);
    }

    @PutMapping("/tasks/{id}/status")
    public Result<?> wxUpdateTaskStatus(@RequestAttribute("userId") Long userId,
                                        @PathVariable Long id,
                                        @RequestParam String status) {
        log.info("wx delivery update task status: userId={}, recordId={}, status={}", userId, id, status);
        return deliveryService.wxUpdateTaskStatus(userId, id, status);
    }

    @GetMapping("/income")
    public Result<?> wxIncome(@RequestAttribute("userId") Long userId) {
        log.info("wx delivery income: userId={}", userId);
        return deliveryService.wxIncome(userId);
    }

    @PutMapping("/status")
    public Result<?> wxUpdateStatus(@RequestAttribute("userId") Long userId,
                                     @RequestParam Integer status) {
        log.info("wx delivery update status: userId={}, status={}", userId, status);
        return deliveryService.wxUpdateStatus(userId, status);
    }

    @GetMapping("/profile")
    public Result<?> wxProfile(@RequestAttribute("userId") Long userId) {
        log.info("wx delivery profile: userId={}", userId);
        return deliveryService.wxProfile(userId);
    }

    @PostMapping("/withdraw")
    public Result<?> wxWithdraw(@RequestAttribute("userId") Long userId) {
        log.info("wx delivery withdraw: userId={}", userId);
        return withdrawService.apply(userId);
    }

    @GetMapping("/withdraw/list")
    public Result<?> wxWithdrawList(@RequestAttribute("userId") Long userId,
                                     @RequestParam(defaultValue = "1") Integer page,
                                     @RequestParam(defaultValue = "10") Integer pageSize) {
        log.info("wx delivery withdraw list: userId={}", userId);
        return withdrawService.list(userId, page, pageSize);
    }

    @GetMapping("/route")
    public Result<?> getRoute(@RequestParam Double fromLng, @RequestParam Double fromLat,
                              @RequestParam Double toLng, @RequestParam Double toLat) {
        return Result.ok(tencentMapService.getBikingRoute(fromLng, fromLat, toLng, toLat));
    }

    @GetMapping("/route/enhanced")
    public Result<?> getEnhancedRoute(@RequestParam Double fromLng, @RequestParam Double fromLat,
                                       @RequestParam Double toLng, @RequestParam Double toLat) {
        Map<String, Object> route = tencentMapService.getBikingRoute(fromLng, fromLat, toLng, toLat);
        Map<String, Object> resp = new java.util.LinkedHashMap<>();
        resp.put("distance", route.get("distance"));
        resp.put("duration", route.get("duration"));
        resp.put("polyline", route.get("polyline"));
        resp.put("steps", route.get("steps"));
        // 第一条导航指令
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> steps = (List<Map<String, Object>>) route.get("steps");
        if (steps != null && !steps.isEmpty()) {
            resp.put("nextInstruction", steps.get(0).get("instruction"));
        }
        resp.put("currentLat", fromLat);
        resp.put("currentLng", fromLng);
        return Result.ok(resp);
    }

    // ==================== Location Tracking ====================

    @PostMapping("/location")
    public Result<?> reportLocation(@RequestAttribute("userId") Long userId,
                                    @RequestBody @Valid RiderLocationRequest req) {
        return deliveryService.reportLocation(userId, req.getLat(), req.getLng());
    }

    @GetMapping("/location/{deliveryId}")
    public Result<?> getRiderLocation(@PathVariable Long deliveryId) {
        return deliveryService.getRiderLocation(deliveryId);
    }

    @GetMapping("/location/by-order/{orderId}")
    public Result<?> getRiderLocationByOrder(@PathVariable Long orderId) {
        return deliveryService.getRiderLocationByOrder(orderId);
    }

    // ==================== Track & Verification ====================

    @GetMapping("/track/{orderId}")
    public Result<?> getTrackPoints(@PathVariable Long orderId) {
        return deliveryService.getTrackPoints(orderId);
    }

    @PostMapping("/verify")
    public Result<?> applyVerification(@RequestAttribute("userId") Long userId,
                                       @RequestBody Map<String, String> body) {
        String realName = body.get("realName");
        String idCard = body.get("idCard");
        if (realName == null || realName.trim().isEmpty()) {
            return Result.fail("请输入真实姓名");
        }
        if (idCard == null || idCard.trim().length() < 5) {
            return Result.fail("证件号至少5个字符");
        }
        return deliveryService.applyVerification(userId, realName.trim(), idCard.trim());
    }

    @GetMapping("/verify/status")
    public Result<?> getVerificationStatus(@RequestAttribute("userId") Long userId) {
        return deliveryService.getVerificationStatus(userId);
    }

    @PutMapping("/vehicle")
    public Result<?> updateVehicle(@RequestAttribute("userId") Long userId,
                                    @RequestBody Map<String, String> body) {
        String vehicle = body.get("vehicle");
        if (vehicle == null || vehicle.trim().isEmpty()) {
            return Result.fail("请输入交通工具");
        }
        return deliveryService.updateVehicle(userId, vehicle.trim());
    }

    // ==================== 骑手反馈 & 申诉记录 ====================

    @GetMapping("/my-feedback")
    public Result<?> myFeedback(@RequestAttribute("userId") Long userId) {
        List<Feedback> list = feedbackMapper.selectList(
                new LambdaQueryWrapper<Feedback>()
                        .eq(Feedback::getUserId, userId)
                        .orderByDesc(Feedback::getCreateTime));
        List<Map<String, Object>> records = new ArrayList<>();
        for (Feedback fb : list) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", fb.getId());
            m.put("type", fb.getType());
            m.put("content", fb.getContent());
            m.put("status", fb.getStatus());
            m.put("reply", fb.getReply());
            m.put("createTime", fb.getCreateTime());
            records.add(m);
        }
        return Result.ok(records);
    }

    // ==================== 骑手评价 & 申诉 ====================

    @GetMapping("/evaluations")
    public Result<?> myEvaluations(@RequestAttribute("userId") Long userId) {
        // Find delivery record for this user
        Delivery delivery = deliveryMapper.selectOne(
                new LambdaQueryWrapper<Delivery>().eq(Delivery::getUserId, userId));
        if (delivery == null) return Result.ok(java.util.Collections.emptyList());

        List<Evaluation> list = evaluationMapper.selectList(
                new LambdaQueryWrapper<Evaluation>()
                        .eq(Evaluation::getDeliveryId, delivery.getId())
                        .orderByDesc(Evaluation::getCreateTime));

        java.util.List<Map<String, Object>> records = new java.util.ArrayList<>();
        for (Evaluation e : list) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", e.getId());
            m.put("rating", e.getRating());
            m.put("content", e.getContent());
            m.put("status", e.getStatus() != null ? e.getStatus() : 0);
            m.put("createTime", e.getCreateTime());
            Order o = orderMapper.selectById(e.getOrderId());
            m.put("orderNo", o != null ? o.getOrderNo() : "");
            User u = userMapper.selectById(e.getUserId());
            m.put("userName", u != null ? u.getNickname() : "");
            records.add(m);
        }
        return Result.ok(records);
    }

    @PostMapping("/appeal")
    public Result<?> appealEvaluation(@RequestAttribute("userId") Long userId,
                                       @RequestBody Map<String, Object> body) {
        Long evaluationId = Long.valueOf(body.get("evaluationId").toString());
        String reason = (String) body.get("reason");
        if (reason == null || reason.trim().length() < 5) {
            return Result.fail("申诉理由至少5个字符");
        }
        Evaluation eval = evaluationMapper.selectById(evaluationId);
        if (eval == null) return Result.fail("评价不存在");

        Feedback fb = new Feedback();
        fb.setUserId(userId);
        fb.setRole("delivery");
        fb.setType("appeal");
        fb.setContent("申诉评价#" + evaluationId + "（订单" + (orderMapper.selectById(eval.getOrderId()) != null ? orderMapper.selectById(eval.getOrderId()).getOrderNo() : "") + "）：" + reason.trim());
        fb.setStatus(0);
        fb.setCreateTime(LocalDateTime.now());
        feedbackMapper.insert(fb);
        return Result.ok("申诉已提交，平台会审核处理");
    }

    // ==================== 骑手客服反馈 ====================

    @GetMapping("/ping")
    public Result<?> ping(@RequestAttribute("userId") Long userId) {
        return Result.ok("pong, userId=" + userId);
    }

    @PostMapping("/feedback")
    public Result<?> submitFeedback(@RequestAttribute("userId") Long userId,
                                     @RequestBody(required = false) Map<String, Object> body) {
        try {
            log.info("=== Delivery feedback START: userId={}, body={} ===", userId, body);
            if (body == null) {
                log.warn("Delivery feedback: body is NULL!");
                return Result.fail("请求数据为空");
            }
            String content = (String) body.get("content");
            if (content == null || content.trim().isEmpty()) {
                return Result.fail("请输入内容");
            }
            String type = (String) body.getOrDefault("type", "support");
            Feedback fb = new Feedback();
            fb.setUserId(userId);
            fb.setRole("delivery");
            fb.setType(type);
            fb.setContent(content.trim());
            fb.setStatus(0);
            fb.setCreateTime(LocalDateTime.now());
            feedbackMapper.insert(fb);
            log.info("=== Delivery feedback SUCCESS: id={} ===", fb.getId());
            return Result.ok("已提交，平台会尽快处理");
        } catch (Exception e) {
            log.error("Delivery feedback ERROR: ", e);
            return Result.fail("提交失败: " + e.getMessage());
        }
    }

    // ==================== 熔断降级 ====================

    @SuppressWarnings("unchecked")
    public Result<?> deliveryLobbyFallback(Long userId, Double lat, Double lng, Throwable t) {
        log.warn("Delivery lobby circuit breaker OPEN, using fallback: {}", t.getMessage());
        return Result.ok(Collections.emptyList());
    }
}
