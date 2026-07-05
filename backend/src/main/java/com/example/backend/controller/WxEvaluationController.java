package com.example.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.common.BusinessException;
import com.example.backend.common.Result;
import com.example.backend.entity.Delivery;
import com.example.backend.entity.Evaluation;
import com.example.backend.entity.Order;
import com.example.backend.entity.Shop;
import com.example.backend.event.EventMessage;
import com.example.backend.event.RedisMessagePublisher;
import com.example.backend.mapper.DeliveryMapper;
import com.example.backend.mapper.EvaluationMapper;
import com.example.backend.mapper.OrderMapper;
import com.example.backend.mapper.ShopMapper;
import com.example.backend.service.ShopService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/wx/evaluation")
@RequiredArgsConstructor
@Slf4j
public class WxEvaluationController {

    private final EvaluationMapper evaluationMapper;
    private final OrderMapper orderMapper;
    private final DeliveryMapper deliveryMapper;
    private final ShopMapper shopMapper;
    private final RedisMessagePublisher redisMessagePublisher;
    private final ShopService shopService;

    /**
     * 检查订单是否可以评价
     */
    @GetMapping("/check/{orderId}")
    public Result<?> checkEvaluable(@RequestAttribute("userId") Long userId,
                                     @PathVariable Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) return Result.fail("订单不存在");
        if (!order.getUserId().equals(userId)) return Result.fail("该订单不属于你");
        if (order.getStatus() != 3) return Result.fail("订单未完成，当前状态:" + order.getStatus());

        Long cnt = evaluationMapper.selectCount(
                new LambdaQueryWrapper<Evaluation>().eq(Evaluation::getOrderId, orderId));
        if (cnt != null && cnt > 0) return Result.fail("已评价过");

        return Result.ok(Map.of("ok", true, "message", "可以评价"));
    }

    @PostMapping
    @Transactional
    public Result<?> create(@RequestAttribute("userId") Long userId,
                            @RequestBody Map<String, Object> data) {
        Long orderId = Long.valueOf(data.get("orderId").toString());
        Integer rating = Integer.valueOf(data.get("rating").toString());
        String content = (String) data.get("content");

        log.info("wx evaluation create: userId={}, orderId={}, rating={}", userId, orderId, rating);

        // Check if already evaluated
        Long count = evaluationMapper.selectCount(new LambdaQueryWrapper<Evaluation>()
                .eq(Evaluation::getOrderId, orderId));
        if (count != null && count > 0) {
            throw new BusinessException("该订单已评价");
        }

        // Verify order exists and belongs to user
        Order order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException("订单不存在");
        }

        // Create evaluation
        Evaluation evaluation = new Evaluation();
        evaluation.setOrderId(orderId);
        evaluation.setUserId(userId);
        evaluation.setDeliveryId(order.getDeliveryId());
        evaluation.setRating(rating);
        evaluation.setContent(content);
        evaluation.setCreateTime(LocalDateTime.now());
        evaluationMapper.insert(evaluation);

        // 异步更新骑手好评率 + 等级 和 店铺评分（通过 Redis Pub/Sub 解耦）
        // Redis 不可用时退化到同步计算，确保评分不丢失
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("orderId", orderId);
        eventData.put("deliveryId", order.getDeliveryId());
        eventData.put("shopId", order.getShopId());

        EventMessage event = EventMessage.builder()
                .type(EventMessage.EVALUATION_SUBMITTED)
                .data(eventData)
                .timestamp(LocalDateTime.now())
                .build();

        try {
            redisMessagePublisher.publishEvaluationEvent(event);
        } catch (Exception e) {
            log.error("Redis publish failed, falling back to synchronous rating update: {}", e.getMessage());
            // Redis 不可用 → 同步更新店铺评分
            try {
                shopService.recalculateShopRating(order.getShopId());
            } catch (Exception ex) {
                log.error("Fallback shop rating recalculate also failed: {}", ex.getMessage());
            }
            // 同步更新骑手好评率
            if (order.getDeliveryId() != null) {
                try {
                    recalculateDeliveryPraiseRate(order.getDeliveryId());
                } catch (Exception ex) {
                    log.error("Fallback delivery praise rate recalculate also failed: {}", ex.getMessage());
                }
            }
        }

        return Result.ok(evaluation);
    }

    /**
     * 同步重算骑手好评率（Redis 不可用时的降级方案）
     */
    private void recalculateDeliveryPraiseRate(Long deliveryId) {
        Delivery delivery = deliveryMapper.selectById(deliveryId);
        if (delivery == null) return;

        List<Evaluation> riderEvals = evaluationMapper.selectList(
                new LambdaQueryWrapper<Evaluation>()
                        .eq(Evaluation::getDeliveryId, deliveryId)
                        .ne(Evaluation::getStatus, 1)
                        .isNotNull(Evaluation::getRating));
        if (!riderEvals.isEmpty()) {
            int totalRating = riderEvals.stream().mapToInt(Evaluation::getRating).sum();
            BigDecimal avgPct = BigDecimal.valueOf(totalRating)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(riderEvals.size() * 5L), 1, RoundingMode.HALF_UP);
            delivery.setPraiseRate(avgPct);

            int total = delivery.getTotalDeliveries() != null ? delivery.getTotalDeliveries() : 0;
            int level;
            if (total >= 200 && avgPct.compareTo(BigDecimal.valueOf(95)) >= 0) {
                level = 2;
            } else if (total >= 50 && avgPct.compareTo(BigDecimal.valueOf(90)) >= 0) {
                level = 1;
            } else {
                level = 0;
            }
            delivery.setLevel(level);
            delivery.setUpdateTime(LocalDateTime.now());
            deliveryMapper.updateById(delivery);
        }
    }
}
