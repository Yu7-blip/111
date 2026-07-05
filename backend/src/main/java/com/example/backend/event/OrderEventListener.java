package com.example.backend.event;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.entity.*;
import com.example.backend.mapper.*;
import com.example.backend.service.ShopService;
import com.example.backend.websocket.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 订单事件异步监听器 — 处理 ORDER_PAID / ORDER_CREATED / REFUND_PROCESSED 等事件
 * 将非核心同步操作（通知推送、统计更新）转为异步处理，显著降低接口响应时间
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventListener implements MessageListener {

    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final GoodsMapper goodsMapper;
    private final ShopMapper shopMapper;
    private final DeliveryMapper deliveryMapper;
    private final DeliveryRecordMapper deliveryRecordMapper;
    private final EvaluationMapper evaluationMapper;
    private final ShopService shopService;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String body = new String(message.getBody());
            EventMessage event = objectMapper.readValue(body, EventMessage.class);
            log.info("Received event [{}] on channel [{}]", event.getType(), new String(message.getChannel()));

            switch (event.getType()) {
                case EventMessage.ORDER_PAID -> handleOrderPaid(event);
                case EventMessage.ORDER_CREATED -> handleOrderCreated(event);
                case EventMessage.REFUND_PROCESSED -> handleRefundProcessed(event);
                case EventMessage.EVALUATION_SUBMITTED -> handleEvaluationSubmitted(event);
                case EventMessage.DELIVERY_COMPLETED -> handleDeliveryCompleted(event);
                default -> log.warn("Unknown event type: {}", event.getType());
            }
        } catch (Exception e) {
            log.error("Failed to process event: {}", e.getMessage(), e);
        }
    }

    /**
     * 订单支付成功 → 异步通知 + 更新销量统计
     */
    private void handleOrderPaid(EventMessage event) {
        Map<String, Object> data = event.getData();
        Long orderId = Long.valueOf(data.get("orderId").toString());
        Order order = orderMapper.selectById(orderId);
        if (order == null) return;

        // 1. 推送通知给用户
        notificationService.notifyOrderStatusChange(order, "已支付，商家正在备餐");

        // 2. 更新商品销量和库存
        LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(OrderItem::getOrderId, orderId);
        List<OrderItem> items = orderItemMapper.selectList(itemWrapper);
        for (OrderItem item : items) {
            Goods goods = goodsMapper.selectById(item.getGoodsId());
            if (goods != null) {
                goods.setSales((goods.getSales() != null ? goods.getSales() : 0) + item.getCount());
                goods.setStock(goods.getStock() - item.getCount());
                goods.setUpdateTime(LocalDateTime.now());
                goodsMapper.updateById(goods);
            }
        }

        // 3. 更新店铺月销量
        Shop shop = shopMapper.selectById(order.getShopId());
        if (shop != null) {
            shop.setSales((shop.getSales() != null ? shop.getSales() : 0) + 1);
            shop.setUpdateTime(LocalDateTime.now());
            shopMapper.updateById(shop);
        }

        log.info("Async: order paid processing done, orderId={}", orderId);
    }

    /**
     * 订单创建 → 异步通知商家（可扩展）
     */
    private void handleOrderCreated(EventMessage event) {
        Map<String, Object> data = event.getData();
        Long orderId = Long.valueOf(data.get("orderId").toString());
        Order order = orderMapper.selectById(orderId);
        if (order == null) return;

        notificationService.notifyOrderStatusChange(order, "新订单已生成，等待支付");
        log.info("Async: order created notification sent, orderId={}", orderId);
    }

    /**
     * 退款处理 → 异步恢复库存 + 通知
     */
    private void handleRefundProcessed(EventMessage event) {
        Map<String, Object> data = event.getData();
        Long orderId = Long.valueOf(data.get("orderId").toString());
        String notifyMsg = (String) data.getOrDefault("notifyMsg", "退款已处理");
        Order order = orderMapper.selectById(orderId);
        if (order == null) return;

        // 恢复库存
        LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(OrderItem::getOrderId, orderId);
        List<OrderItem> items = orderItemMapper.selectList(itemWrapper);
        for (OrderItem item : items) {
            Goods goods = goodsMapper.selectById(item.getGoodsId());
            if (goods != null) {
                goods.setStock(goods.getStock() + item.getCount());
                goods.setSales(Math.max(0, (goods.getSales() != null ? goods.getSales() : 0) - item.getCount()));
                goods.setUpdateTime(LocalDateTime.now());
                goodsMapper.updateById(goods);
            }
        }

        notificationService.notifyOrderStatusChange(order, notifyMsg);
        log.info("Async: refund processed, orderId={}", orderId);
    }

    /**
     * 评价提交 → 异步更新骑手好评率和店铺评分（排除已撤销的评价）
     */
    private void handleEvaluationSubmitted(EventMessage event) {
        Map<String, Object> data = event.getData();
        Long orderId = Long.valueOf(data.get("orderId").toString());
        Order order = orderMapper.selectById(orderId);
        if (order == null) return;

        // 更新骑手好评率（排除已撤销的评价）
        if (order.getDeliveryId() != null) {
            Delivery delivery = deliveryMapper.selectById(order.getDeliveryId());
            if (delivery != null) {
                List<Evaluation> riderEvals = evaluationMapper.selectList(
                        new LambdaQueryWrapper<Evaluation>()
                                .eq(Evaluation::getDeliveryId, order.getDeliveryId())
                                .ne(Evaluation::getStatus, 1)
                                .isNotNull(Evaluation::getRating));
                if (!riderEvals.isEmpty()) {
                    int totalRating = riderEvals.stream().mapToInt(Evaluation::getRating).sum();
                    BigDecimal avgPct = BigDecimal.valueOf(totalRating)
                            .multiply(BigDecimal.valueOf(100))
                            .divide(BigDecimal.valueOf(riderEvals.size() * 5L), 1, RoundingMode.HALF_UP);
                    delivery.setPraiseRate(avgPct);

                    // 重新计算等级
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

        // 更新店铺评分（使用统一的 recalculate 方法，自动排除已撤销评价 + 清除缓存）
        shopService.recalculateShopRating(order.getShopId());

        log.info("Async: evaluation stats updated, orderId={}", orderId);
    }

    /**
     * 配送完成 → 异步更新骑手统计
     */
    private void handleDeliveryCompleted(EventMessage event) {
        Map<String, Object> data = event.getData();
        Long deliveryId = Long.valueOf(data.get("deliveryId").toString());
        Delivery delivery = deliveryMapper.selectById(deliveryId);
        if (delivery == null) return;

        // 统计已完成配送数
        Long completedCount = deliveryRecordMapper.selectCount(
                new LambdaQueryWrapper<DeliveryRecord>()
                        .eq(DeliveryRecord::getDeliveryId, deliveryId)
                        .eq(DeliveryRecord::getStatus, "completed"));
        delivery.setTotalDeliveries(completedCount != null ? completedCount.intValue() : 0);
        delivery.setUpdateTime(LocalDateTime.now());
        deliveryMapper.updateById(delivery);

        log.info("Async: delivery stats updated, deliveryId={}, totalDeliveries={}", deliveryId, delivery.getTotalDeliveries());
    }
}
