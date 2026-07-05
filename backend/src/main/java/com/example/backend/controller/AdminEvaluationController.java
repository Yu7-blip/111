package com.example.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.common.PageResult;
import com.example.backend.common.Result;
import com.example.backend.entity.Evaluation;
import com.example.backend.entity.Order;
import com.example.backend.entity.User;
import com.example.backend.entity.Delivery;
import com.example.backend.mapper.EvaluationMapper;
import com.example.backend.mapper.OrderMapper;
import com.example.backend.mapper.UserMapper;
import com.example.backend.mapper.DeliveryMapper;
import com.example.backend.service.ShopService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/admin/evaluations")
@RequiredArgsConstructor
public class AdminEvaluationController {

    private final EvaluationMapper evaluationMapper;
    private final OrderMapper orderMapper;
    private final UserMapper userMapper;
    private final DeliveryMapper deliveryMapper;
    private final ShopService shopService;

    @GetMapping
    public Result<?> adminList(@RequestParam(defaultValue = "1") Integer page,
                               @RequestParam(defaultValue = "10") Integer pageSize,
                               @RequestParam(required = false) String orderNo) {
        LambdaQueryWrapper<Evaluation> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(orderNo)) {
            // Find order by orderNo
            LambdaQueryWrapper<Order> orderWrapper = new LambdaQueryWrapper<>();
            orderWrapper.eq(Order::getOrderNo, orderNo);
            Order order = orderMapper.selectOne(orderWrapper);
            if (order != null) {
                wrapper.eq(Evaluation::getOrderId, order.getId());
            } else {
                return Result.ok(PageResult.of(Collections.emptyList(), 0, page, pageSize));
            }
        }
        wrapper.orderByDesc(Evaluation::getCreateTime);

        Page<Evaluation> mpPage = new Page<>(page, pageSize);
        evaluationMapper.selectPage(mpPage, wrapper);

        List<Map<String, Object>> records = mpPage.getRecords().stream().map(eval -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", eval.getId());
            map.put("orderId", eval.getOrderId());
            // Order info
            Order order = orderMapper.selectById(eval.getOrderId());
            if (order != null) {
                map.put("orderNo", order.getOrderNo());
            }
            // User info
            User user = userMapper.selectById(eval.getUserId());
            map.put("userId", eval.getUserId());
            map.put("userName", user != null ? user.getNickname() : "");
            // Delivery info
            if (eval.getDeliveryId() != null) {
                Delivery delivery = deliveryMapper.selectById(eval.getDeliveryId());
                map.put("deliveryId", eval.getDeliveryId());
                map.put("deliveryName", delivery != null ? delivery.getName() : "");
            }
            map.put("rating", eval.getRating());
            map.put("content", eval.getContent());
            map.put("content", eval.getContent());
            map.put("status", eval.getStatus() != null ? eval.getStatus() : 0);
            map.put("createTime", eval.getCreateTime());
            return map;
        }).toList();

        return Result.ok(PageResult.of(records, mpPage.getTotal(), mpPage.getCurrent(), mpPage.getSize()));
    }

    @PutMapping("/{id}/revoke")
    public Result<?> revoke(@PathVariable Long id) {
        Evaluation eval = evaluationMapper.selectById(id);
        if (eval == null) return Result.fail("评价不存在");
        eval.setStatus(1);
        evaluationMapper.updateById(eval);

        // 撤销评价后重新计算店铺评分和骑手好评率
        Order order = orderMapper.selectById(eval.getOrderId());
        if (order != null) {
            shopService.recalculateShopRating(order.getShopId());
        }
        if (eval.getDeliveryId() != null) {
            recalculateDeliveryPraiseRate(eval.getDeliveryId());
        }

        return Result.ok("评价已撤销");
    }

    /**
     * 同步重算骑手好评率（撤销评价后触发）
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
            java.math.BigDecimal avgPct = java.math.BigDecimal.valueOf(totalRating)
                    .multiply(java.math.BigDecimal.valueOf(100))
                    .divide(java.math.BigDecimal.valueOf(riderEvals.size() * 5L), 1, java.math.RoundingMode.HALF_UP);
            delivery.setPraiseRate(avgPct);

            int total = delivery.getTotalDeliveries() != null ? delivery.getTotalDeliveries() : 0;
            int level;
            if (total >= 200 && avgPct.compareTo(java.math.BigDecimal.valueOf(95)) >= 0) {
                level = 2;
            } else if (total >= 50 && avgPct.compareTo(java.math.BigDecimal.valueOf(90)) >= 0) {
                level = 1;
            } else {
                level = 0;
            }
            delivery.setLevel(level);
            delivery.setUpdateTime(java.time.LocalDateTime.now());
            deliveryMapper.updateById(delivery);
        }
    }
}
