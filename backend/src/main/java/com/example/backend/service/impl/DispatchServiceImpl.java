package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.common.Result;
import com.example.backend.entity.*;
import com.example.backend.mapper.*;
import com.example.backend.service.DispatchService;
import com.example.backend.utils.GeoUtil;
import com.example.backend.utils.RedisUtil;
import com.example.backend.websocket.NotificationService;
import com.example.backend.websocket.OrderNotificationEndpoint;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DispatchServiceImpl implements DispatchService {

    // 权重配置
    private static final double W_DISTANCE = 0.40;
    private static final double W_QUALITY = 0.25;
    private static final double W_LOAD = 0.20;
    private static final double W_ROUTE = 0.15;

    // 派单策略阈值
    private static final double SCORE_AUTO_ASSIGN = 0.60;   // 高于此分数自动派单
    private static final double SCORE_PUSH_NOTIFY = 0.35;    // 高于此分数推送通知
    private static final double MAX_DISTANCE_KM = 5.0;        // 骑手最大接单距离
    private static final int MAX_LOAD = 2;                    // 骑手最大同时接单数
    private static final double ROUTE_SAME_DEST_KM = 1.0;    // 同方向判定距离
    private static final double ROUTE_SAME_BEARING_DEG = 30; // 同方向判定角度

    private final DeliveryMapper deliveryMapper;
    private final DeliveryRecordMapper deliveryRecordMapper;
    private final OrderMapper orderMapper;
    private final ShopMapper shopMapper;
    private final OrderItemMapper orderItemMapper;
    private final GoodsMapper goodsMapper;
    private final NotificationService notificationService;
    private final RedisUtil redisUtil;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public Result<?> dispatch(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || order.getStatus() != 1 || order.getDeliveryId() != null) {
            return Result.fail("订单状态不允许派单");
        }

        List<Map<String, Object>> ranked = scoreAllRiders(orderId);
        if (ranked.isEmpty()) {
            log.info("Dispatch: no suitable riders for order {}", orderId);
            return Result.fail("暂无合适骑手，进入抢单池");
        }

        Map<String, Object> top = ranked.get(0);
        double topScore = (Double) top.get("score");
        Long topDeliveryId = (Long) top.get("deliveryId");

        if (topScore >= SCORE_AUTO_ASSIGN) {
            // 自动派单给最高分骑手
            Delivery delivery = deliveryMapper.selectById(topDeliveryId);
            assignOrder(order, delivery);
            log.info("Dispatch: auto-assigned order {} to delivery {} (score={})", orderId, topDeliveryId, String.format("%.3f", topScore));
            Map<String, Object> result = new HashMap<>();
            result.put("method", "auto");
            result.put("deliveryId", topDeliveryId);
            result.put("deliveryName", delivery != null ? delivery.getName() : "");
            result.put("score", topScore);
            return Result.ok(result);
        }

        if (topScore >= SCORE_PUSH_NOTIFY) {
            // 推送给前3名骑手
            int pushCount = Math.min(3, ranked.size());
            for (int i = 0; i < pushCount; i++) {
                Map<String, Object> entry = ranked.get(i);
                Long dId = (Long) entry.get("deliveryId");
                Double score = (Double) entry.get("score");
                Delivery delivery = deliveryMapper.selectById(dId);
                if (delivery != null) {
                    sendDispatchNotification(delivery, order, score);
                }
            }
            log.info("Dispatch: pushed order {} to {} riders, topScore={}", orderId, pushCount, String.format("%.3f", topScore));
            Map<String, Object> result = new HashMap<>();
            result.put("method", "push");
            result.put("pushedCount", pushCount);
            result.put("topScore", topScore);
            return Result.ok(result);
        }

        log.info("Dispatch: order {} entering manual grab pool (topScore={})", orderId, String.format("%.3f", topScore));
        return Result.fail("暂无高匹配度骑手，进入抢单池");
    }

    @Override
    public Double scoreRiderForOrder(Long deliveryUserId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) return null;

        LambdaQueryWrapper<Delivery> dw = new LambdaQueryWrapper<>();
        dw.eq(Delivery::getUserId, deliveryUserId);
        Delivery delivery = deliveryMapper.selectOne(dw);
        if (delivery == null || delivery.getStatus() != 1) return null;

        return computeScore(delivery, order);
    }

    @Override
    public List<Map<String, Object>> scoreAllRiders(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) return Collections.emptyList();

        // 获取订单所属店铺（一次性查询）
        Shop shop = shopMapper.selectById(order.getShopId());
        double shopLat = shop != null && shop.getLatitude() != null ? shop.getLatitude() : 0;
        double shopLng = shop != null && shop.getLongitude() != null ? shop.getLongitude() : 0;

        // 获取所有在线骑手
        LambdaQueryWrapper<Delivery> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Delivery::getStatus, 1);
        List<Delivery> riders = deliveryMapper.selectList(wrapper);

        List<Map<String, Object>> ranked = new ArrayList<>();
        for (Delivery rider : riders) {
            Double score = computeScore(rider, order);
            if (score != null) {
                Map<String, Object> entry = new HashMap<>();
                entry.put("deliveryId", rider.getId());
                entry.put("deliveryName", rider.getName());
                entry.put("score", score);
                entry.put("onTimeRate", rider.getOnTimeRate());
                entry.put("praiseRate", rider.getPraiseRate());
                entry.put("level", rider.getLevel());
                entry.put("distanceKm", getRiderDistanceKm(rider, shopLat, shopLng));
                entry.put("activeOrders", countActiveOrders(rider.getId()));
                ranked.add(entry);
            }
        }

        ranked.sort((a, b) -> Double.compare((Double) b.get("score"), (Double) a.get("score")));
        return ranked;
    }

    // ==================== 核心评分算法 ====================

    /**
     * Score = W_DISTANCE * distanceScore + W_QUALITY * qualityScore
     *       + W_LOAD * loadScore + W_ROUTE * routeScore
     */
    private Double computeScore(Delivery rider, Order order) {
        // 获取骑手最后已知位置
        double[] riderPos = getRiderPosition(rider.getId());
        if (riderPos == null) return null; // 位置过期，跳过

        // 已达接单上限，不参与评分
        int activeOrders = countActiveOrders(rider.getId());
        if (activeOrders >= MAX_LOAD) return null;

        Shop shop = shopMapper.selectById(order.getShopId());
        if (shop == null || shop.getLatitude() == null || shop.getLongitude() == null) return null;

        double riderLat = riderPos[0], riderLng = riderPos[1];

        // 1. 距离分 (40%)
        double distToShop = GeoUtil.haversineDistance(riderLat, riderLng, shop.getLatitude(), shop.getLongitude());
        if (distToShop > MAX_DISTANCE_KM) return null; // 太远，不适合
        double distanceScore = Math.max(0, 1.0 - distToShop / MAX_DISTANCE_KM);

        // 2. 质量分 (25%)
        double onTimeRate = rider.getOnTimeRate() != null ? rider.getOnTimeRate().doubleValue() : 100.0;
        double praiseRate = rider.getPraiseRate() != null ? rider.getPraiseRate().doubleValue() : 100.0;
        int level = rider.getLevel() != null ? rider.getLevel() : 0;
        double qualityScore = onTimeRate / 100.0 * 0.5 + praiseRate / 100.0 * 0.3 + level / 2.0 * 0.2;

        // 3. 负载分 (20%)
        double loadScore = Math.max(0, 1.0 - (double) activeOrders / MAX_LOAD);

        // 4. 顺路分 (15%)
        double routeScore = computeRouteSimilarity(rider.getId(), order, shop);

        return W_DISTANCE * distanceScore + W_QUALITY * qualityScore
             + W_LOAD * loadScore + W_ROUTE * routeScore;
    }

    /**
     * 计算顺路相似度
     * 检查骑手当前配送中的订单，是否与新订单目的地接近或同方向
     */
    private double computeRouteSimilarity(Long deliveryId, Order newOrder, Shop shop) {
        if (newOrder.getAddressLat() == null || newOrder.getAddressLng() == null) return 0;

        // 获取骑手所有配送中的订单
        LambdaQueryWrapper<DeliveryRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DeliveryRecord::getDeliveryId, deliveryId)
               .eq(DeliveryRecord::getStatus, "delivering");
        List<DeliveryRecord> records = deliveryRecordMapper.selectList(wrapper);

        double bonus = 0;
        for (DeliveryRecord record : records) {
            Order existingOrder = orderMapper.selectById(record.getOrderId());
            if (existingOrder == null || existingOrder.getAddressLat() == null || existingOrder.getAddressLng() == null)
                continue;

            // 检查两个目的地是否接近（1km内）
            double distBetweenDests = GeoUtil.haversineDistance(
                    newOrder.getAddressLat(), newOrder.getAddressLng(),
                    existingOrder.getAddressLat(), existingOrder.getAddressLng());
            if (distBetweenDests < ROUTE_SAME_DEST_KM) {
                bonus += 0.3;
            }

            // 检查方向相似度
            double bearing1 = bearing(shop.getLatitude(), shop.getLongitude(),
                    newOrder.getAddressLat(), newOrder.getAddressLng());
            double bearing2 = bearing(shop.getLatitude(), shop.getLongitude(),
                    existingOrder.getAddressLat(), existingOrder.getAddressLng());
            double bearingDiff = Math.abs(bearing1 - bearing2);
            if (bearingDiff > 180) bearingDiff = 360 - bearingDiff;
            if (bearingDiff < ROUTE_SAME_BEARING_DEG) {
                bonus += 0.2;
            }
        }

        return Math.min(1.0, bonus);
    }

    // ==================== 辅助方法 ====================

    /**
     * 从Redis获取骑手最后位置
     */
    private double[] getRiderPosition(Long deliveryId) {
        String key = "rider:location:" + deliveryId;
        @SuppressWarnings("unchecked")
        Map<String, Object> loc = (Map<String, Object>) redisUtil.get(key);
        if (loc == null) return null;
        Double lat = (Double) loc.get("lat");
        Double lng = (Double) loc.get("lng");
        if (lat == null || lng == null) return null;
        return new double[]{lat, lng};
    }

    /**
     * 骑手到店铺距离 (Haversine, km)
     */
    private double getRiderDistanceKm(Delivery rider, double shopLat, double shopLng) {
        double[] pos = getRiderPosition(rider.getId());
        if (pos == null || shopLat == 0 || shopLng == 0) return Double.MAX_VALUE;
        return GeoUtil.haversineDistance(pos[0], pos[1], shopLat, shopLng);
    }

    /**
     * 统计骑手当前活跃订单数
     */
    private int countActiveOrders(Long deliveryId) {
        LambdaQueryWrapper<DeliveryRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DeliveryRecord::getDeliveryId, deliveryId)
               .in(DeliveryRecord::getStatus, "pickup", "delivering");
        Long count = deliveryRecordMapper.selectCount(wrapper);
        return count != null ? count.intValue() : 0;
    }

    /**
     * 方位角计算
     */
    private double bearing(double lat1, double lng1, double lat2, double lng2) {
        double dLng = Math.toRadians(lng2 - lng1);
        double rLat1 = Math.toRadians(lat1);
        double rLat2 = Math.toRadians(lat2);
        double y = Math.sin(dLng) * Math.cos(rLat2);
        double x = Math.cos(rLat1) * Math.sin(rLat2) - Math.sin(rLat1) * Math.cos(rLat2) * Math.cos(dLng);
        double brng = Math.toDegrees(Math.atan2(y, x));
        return (brng + 360) % 360;
    }

    // ==================== 执行派单 ====================

    @Transactional
    public void assignOrder(Order order, Delivery delivery) {
        // 兜底：派单前再次检查骑手是否已满单
        LambdaQueryWrapper<DeliveryRecord> drWrapper = new LambdaQueryWrapper<>();
        drWrapper.eq(DeliveryRecord::getDeliveryId, delivery.getId())
                 .in(DeliveryRecord::getStatus, "pickup", "delivering");
        long activeCount = deliveryRecordMapper.selectCount(drWrapper);
        if (activeCount >= MAX_LOAD) {
            log.warn("assignOrder blocked: rider {} already has {} active orders", delivery.getId(), activeCount);
            return;
        }

        order.setDeliveryId(delivery.getId());
        order.setStatus(2);
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);

        delivery.setStatus(2);
        delivery.setUpdateTime(LocalDateTime.now());
        deliveryMapper.updateById(delivery);

        Shop shop = shopMapper.selectById(order.getShopId());
        BigDecimal fee = (shop != null && shop.getDeliveryFee() != null) ? shop.getDeliveryFee() : BigDecimal.valueOf(3);

        DeliveryRecord record = new DeliveryRecord();
        record.setOrderId(order.getId());
        record.setDeliveryId(delivery.getId());
        record.setFee(fee);
        record.setStatus("pickup");
        record.setCreateTime(LocalDateTime.now());
        deliveryRecordMapper.insert(record);

        notificationService.notifyOrderStatusChange(order, "骑手已接单（智能派单），正在前往商家取餐");
    }

    /**
     * 推送派单通知给骑手
     */
    private void sendDispatchNotification(Delivery delivery, Order order, Double score) {
        Shop shop = shopMapper.selectById(order.getShopId());
        Map<String, Object> msg = new LinkedHashMap<>();
        msg.put("type", "new_order_dispatch");
        msg.put("orderId", order.getId());
        msg.put("orderNo", order.getOrderNo());
        msg.put("shopName", shop != null ? shop.getName() : "");
        msg.put("deliveryAddress", order.getAddressInfo());
        msg.put("fee", shop != null ? shop.getDeliveryFee() : BigDecimal.ZERO);
        msg.put("score", score);
        msg.put("timestamp", System.currentTimeMillis());

        try {
            User user = userMapper.selectById(delivery.getUserId());
            if (user != null) {
                String json = objectMapper.writeValueAsString(msg);
                OrderNotificationEndpoint.sendToUser(String.valueOf(user.getId()), json);
            }
        } catch (Exception e) {
            log.warn("Dispatch notification failed: {}", e.getMessage());
        }
    }

    // 补充注入
    private final UserMapper userMapper;
    private final EvaluationMapper evaluationMapper;
}
