package com.example.backend.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.entity.*;
import com.example.backend.mapper.*;
import com.example.backend.service.impl.EventLogService;
import com.example.backend.websocket.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderScheduledTask {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final GoodsMapper goodsMapper;
    private final DeliveryMapper deliveryMapper;
    private final DeliveryRecordMapper deliveryRecordMapper;
    private final NotificationService notificationService;
    private final EventLogService eventLogService;
    private final CouponMapper couponMapper;
    private final FullReduceActivityMapper activityMapper;

    @Scheduled(fixedRate = 60000)
    public void autoCancelUnpaidOrders() {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(15);
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getStatus, 0)
               .lt(Order::getCreateTime, deadline);
        List<Order> orders = orderMapper.selectList(wrapper);

        if (orders.isEmpty()) return;

        for (Order order : orders) {
            order.setStatus(4);
            order.setOrderNo(order.getOrderNo());
            order.setUpdateTime(LocalDateTime.now());
            orderMapper.updateById(order);

            LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
            itemWrapper.eq(OrderItem::getOrderId, order.getId());
            List<OrderItem> items = orderItemMapper.selectList(itemWrapper);
            for (OrderItem item : items) {
                Goods goods = goodsMapper.selectById(item.getGoodsId());
                if (goods != null) {
                    goods.setStock(goods.getStock() + item.getCount());
                    goods.setUpdateTime(LocalDateTime.now());
                    goodsMapper.updateById(goods);
                }
            }
            log.info("auto cancelled unpaid order: id={}, orderNo={}", order.getId(), order.getOrderNo());
        }
    }

    @Scheduled(fixedRate = 120000)
    public void checkDeliveryTimeout() {
        // Orders in delivering status (2) for over 60 minutes = timeout
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(60);
        LambdaQueryWrapper<DeliveryRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DeliveryRecord::getStatus, "delivering")
               .lt(DeliveryRecord::getPickupTime, deadline)
               .isNotNull(DeliveryRecord::getPickupTime);
        List<DeliveryRecord> records = deliveryRecordMapper.selectList(wrapper);

        for (DeliveryRecord record : records) {
            // Already processed
            if (record.getDeliverTime() != null) continue;

            // Mark as timeout: complete the record but penalize rider
            record.setDeliverTime(LocalDateTime.now());
            record.setStatus("completed");
            deliveryRecordMapper.updateById(record);

            // Complete order
            Order order = orderMapper.selectById(record.getOrderId());
            if (order != null) {
                order.setStatus(3);
                order.setUpdateTime(LocalDateTime.now());
                orderMapper.updateById(order);
                notificationService.notifyOrderStatusChange(order, "订单已自动完成（超时），如有问题请联系客服");
            }

            // Penalize rider: reduce onTimeRate
            Delivery delivery = deliveryMapper.selectById(record.getDeliveryId());
            if (delivery != null) {
                int total = delivery.getTotalDeliveries() != null ? delivery.getTotalDeliveries() : 0;
                BigDecimal currentRate = delivery.getOnTimeRate() != null ? delivery.getOnTimeRate() : BigDecimal.valueOf(100);
                if (total > 0) {
                    // onTimeRate = (onTimeDeliveries / totalDeliveries) * 100
                    // Each timeout reduces the rate proportionally
                    int onTime = Math.max(0, (int) Math.round(currentRate.doubleValue() / 100.0 * total) - 1);
                    BigDecimal newRate = BigDecimal.valueOf(onTime)
                            .multiply(BigDecimal.valueOf(100))
                            .divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP);
                    delivery.setOnTimeRate(newRate.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : newRate);
                } else {
                    delivery.setOnTimeRate(BigDecimal.valueOf(100));
                }
                delivery.setUpdateTime(LocalDateTime.now());
                deliveryMapper.updateById(delivery);
            }

            log.info("delivery timeout: recordId={}, orderId={}", record.getId(), record.getOrderId());
        }
    }

    /**
     * 优惠券过期自动失效 — 每2分钟扫描一次
     */
    @Scheduled(fixedRate = 120000)
    public void autoExpireCoupons() {
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<Coupon> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Coupon::getStatus, 1)
               .lt(Coupon::getEndTime, now);
        List<Coupon> coupons = couponMapper.selectList(wrapper);

        if (coupons.isEmpty()) return;
        for (Coupon coupon : coupons) {
            coupon.setStatus(0);
            couponMapper.updateById(coupon);
            log.info("auto expired coupon: id={}, name={}", coupon.getId(), coupon.getName());
        }
        log.info("auto expired {} coupons", coupons.size());
    }

    /**
     * 营销活动自动启停 — 每2分钟扫描一次
     * - 到达开始时间且未过期：自动开启
     * - 超过结束时间：自动关闭
     */
    @Scheduled(fixedRate = 120000)
    public void autoUpdateActivityStatus() {
        LocalDateTime now = LocalDateTime.now();

        // 1. 自动开启：还未开始但已到开始时间的活动
        LambdaQueryWrapper<FullReduceActivity> startWrapper = new LambdaQueryWrapper<>();
        startWrapper.eq(FullReduceActivity::getStatus, 0)
                    .le(FullReduceActivity::getStartTime, now)
                    .gt(FullReduceActivity::getEndTime, now);
        List<FullReduceActivity> toStart = activityMapper.selectList(startWrapper);
        for (FullReduceActivity activity : toStart) {
            activity.setStatus(1);
            activity.setUpdateTime(now);
            activityMapper.updateById(activity);
            log.info("auto started activity: id={}, name={}", activity.getId(), activity.getName());
        }

        // 2. 自动关闭：已过结束时间的活动
        LambdaQueryWrapper<FullReduceActivity> endWrapper = new LambdaQueryWrapper<>();
        endWrapper.eq(FullReduceActivity::getStatus, 1)
                  .lt(FullReduceActivity::getEndTime, now);
        List<FullReduceActivity> toEnd = activityMapper.selectList(endWrapper);
        for (FullReduceActivity activity : toEnd) {
            activity.setStatus(0);
            activity.setUpdateTime(now);
            activityMapper.updateById(activity);
            log.info("auto ended activity: id={}, name={}", activity.getId(), activity.getName());
        }

        if (!toStart.isEmpty() || !toEnd.isEmpty()) {
            log.info("auto updated activities: started={}, ended={}", toStart.size(), toEnd.size());
        }
    }

    /**
     * 本地事件表兜底扫描 — 每分钟处理未消费的事件
     * 这是 Transaction Outbox 模式的安全网：
     * - 正常路径：事务提交后 Redis Pub/Sub 实时处理
     * - 兜底路径：Redis 失败后，此定时任务将在60秒后重试
     */
    @Scheduled(fixedRate = 60000)
    public void processEventLog() {
        try {
            int count = eventLogService.processStaleEvents();
            if (count > 0) {
                log.info("EventLog retry processed: {} events", count);
            }
        } catch (Exception e) {
            log.error("EventLog scheduled task error: {}", e.getMessage());
        }
    }
}
