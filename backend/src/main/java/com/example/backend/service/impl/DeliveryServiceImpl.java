package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.common.PageResult;
import com.example.backend.common.Result;
import com.example.backend.common.ResultCode;
import com.example.backend.entity.*;
import com.example.backend.event.EventMessage;
import com.example.backend.event.RedisMessagePublisher;
import com.example.backend.mapper.*;
import com.example.backend.utils.GeoUtil;
import com.example.backend.service.DispatchService;
import com.example.backend.service.TencentMapService;
import com.example.backend.service.DeliveryService;
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
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryServiceImpl implements DeliveryService {

    /** 骑手最大同时接单数 */
    private static final int MAX_ACTIVE_ORDERS = 2;

    private final DeliveryMapper deliveryMapper;
    private final DeliveryRecordMapper deliveryRecordMapper;
    private final DeliveryTrackMapper deliveryTrackMapper;
    private final OrderMapper orderMapper;
    private final UserMapper userMapper;
    private final ShopMapper shopMapper;
    private final NotificationService notificationService;
    private final TencentMapService tencentMapService;
    private final RedisUtil redisUtil;
    private final ObjectMapper objectMapper;
    private final DispatchService dispatchService;
    private final RedisMessagePublisher redisMessagePublisher;

    private void calculateLevel(Delivery delivery) {
        int total = delivery.getTotalDeliveries() != null ? delivery.getTotalDeliveries() : 0;
        BigDecimal praise = delivery.getPraiseRate() != null ? delivery.getPraiseRate() : BigDecimal.valueOf(100);
        int level;
        if (total >= 200 && praise.compareTo(BigDecimal.valueOf(95)) >= 0) {
            level = 2;
        } else if (total >= 50 && praise.compareTo(BigDecimal.valueOf(90)) >= 0) {
            level = 1;
        } else {
            level = 0;
        }
        delivery.setLevel(level);
    }

    /**
     * Get destination coordinates, preferring stored order address lat/lng, falling back to geocoding.
     */
    private double[] getDestCoords(Order order) {
        if (order.getAddressLat() != null && order.getAddressLng() != null) {
            return new double[]{order.getAddressLng(), order.getAddressLat()};
        }
        if (order.getAddressInfo() == null || order.getAddressInfo().isEmpty()) {
            return null;
        }
        return tencentMapService.geocode(order.getAddressInfo());
    }

    /**
     * Calculate biking distance from rider to shop.
     */
    private double getRiderToShopDistance(double riderLng, double riderLat, Shop shop) {
        if (shop == null || shop.getLatitude() == null || shop.getLongitude() == null) {
            log.warn("getRiderToShopDistance: shop or coords missing, shop={}", shop != null ? shop.getName() : "null");
            return -1.0;
        }
        double dist = tencentMapService.bikingDistance(
                riderLng, riderLat,
                shop.getLongitude(), shop.getLatitude());
        log.info("getRiderToShopDistance: rider({},{}) → shop({},{}) = {}km",
                riderLat, riderLng, shop.getLatitude(), shop.getLongitude(), String.format("%.3f", dist));
        return dist;
    }

    /**
     * Calculate real biking distance from shop to delivery address.
     */
    private Double getDeliveryDistance(Shop shop, Order order) {
        if (shop == null || shop.getLatitude() == null || shop.getLongitude() == null) {
            log.warn("getDeliveryDistance: shop coords missing, shop={}", shop != null ? shop.getName() : "null");
            return -1.0;
        }
        double[] destCoords = getDestCoords(order);
        if (destCoords == null) {
            log.warn("getDeliveryDistance: destCoords null for order={}, addressInfo={}",
                    order.getOrderNo(), order.getAddressInfo());
            return -1.0;
        }
        double dist = tencentMapService.bikingDistance(
                shop.getLongitude(), shop.getLatitude(),
                destCoords[0], destCoords[1]);
        log.info("getDeliveryDistance: shop({},{}) → dest({},{}) = {}km",
                shop.getLatitude(), shop.getLongitude(), destCoords[1], destCoords[0], String.format("%.3f", dist));
        return dist;
    }

    /**
     * Build distance/coordinate map for a task response.
     */
    private void attachDeliveryInfo(Map<String, Object> map, Shop shop, Order order,
                                    Double riderLng, Double riderLat) {
        double deliveryDist = getDeliveryDistance(shop, order);
        int deliveryMin = getDeliveryEstimateMinutes(shop, order);
        if (deliveryDist < 0) {
            map.put("deliveryDistance", "未知");
        } else {
            map.put("deliveryDistance", String.format("%.1fkm", deliveryDist));
        }

        if (riderLng != null && riderLat != null) {
            double toShop = getRiderToShopDistance(riderLng, riderLat, shop);
            if (toShop < 0) {
                map.put("distanceToShop", "未知");
            } else {
                map.put("distanceToShop", String.format("%.1fkm", toShop));
            }
            map.put("estimateTime", toShop >= 0 && deliveryMin >= 0 ? (int) Math.ceil(toShop * 5) + deliveryMin : -1);
        } else {
            if (deliveryDist < 0) {
                map.put("distanceToShop", "未知");
            } else {
                map.put("distanceToShop", String.format("%.1fkm", deliveryDist));
            }
            map.put("estimateTime", deliveryMin >= 0 ? deliveryMin : -1);
        }

        double[] deliveryCoords = getDestCoords(order);
        if (deliveryCoords != null) {
            map.put("deliveryLat", deliveryCoords[1]);
            map.put("deliveryLng", deliveryCoords[0]);
            map.put("userLat", deliveryCoords[1]);
            map.put("userLng", deliveryCoords[0]);
        }
    }

    private int getDeliveryEstimateMinutes(Shop shop, Order order) {
        if (shop == null || shop.getLatitude() == null || shop.getLongitude() == null) {
            return -1;
        }
        double[] destCoords = getDestCoords(order);
        if (destCoords == null) {
            return -1;
        }
        return tencentMapService.estimateDeliveryMinutes(
                shop.getLongitude(), shop.getLatitude(),
                destCoords[0], destCoords[1]);
    }

    // ==================== Admin Methods ====================

    @Override
    public Result<?> adminList(Integer page, Integer pageSize, String name, String phone, Integer status) {
        LambdaQueryWrapper<Delivery> wrapper = new LambdaQueryWrapper<>();
        if (name != null && !name.isEmpty()) {
            wrapper.like(Delivery::getName, name);
        }
        if (phone != null && !phone.isEmpty()) {
            wrapper.like(Delivery::getPhone, phone);
        }
        if (status != null) {
            wrapper.eq(Delivery::getStatus, status);
        } else {
            wrapper.in(Delivery::getStatus, 0, 1, 2);
        }
        wrapper.orderByDesc(Delivery::getCreateTime);

        Page<Delivery> deliveryPage = new Page<>(page, pageSize);
        deliveryMapper.selectPage(deliveryPage, wrapper);

        List<Map<String, Object>> records = deliveryPage.getRecords().stream().map(delivery -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", delivery.getId());
            map.put("name", delivery.getName());
            map.put("phone", delivery.getPhone());
            map.put("idCard", delivery.getIdCard());
            map.put("vehicle", delivery.getVehicle());
            map.put("status", delivery.getStatus());
            map.put("balance", delivery.getBalance());
            map.put("onTimeRate", delivery.getOnTimeRate());
            map.put("praiseRate", delivery.getPraiseRate());
            map.put("totalDeliveries", delivery.getTotalDeliveries());
            map.put("level", delivery.getLevel());
            map.put("verifyStatus", delivery.getVerifyStatus());
            map.put("realName", delivery.getRealName());
            map.put("verifyRemark", delivery.getVerifyRemark());
            map.put("createTime", delivery.getCreateTime());
            map.put("updateTime", delivery.getUpdateTime());
            return map;
        }).collect(Collectors.toList());

        return Result.ok(PageResult.of(records, deliveryPage.getTotal(), page, pageSize));
    }

    @Override
    public Result<?> adminDetail(Long id) {
        Delivery delivery = deliveryMapper.selectById(id);
        if (delivery == null) {
            return Result.fail(ResultCode.NOT_FOUND);
        }
        return Result.ok(delivery);
    }

    @Override
    @Transactional
    public Result<?> adminUpdateStatus(Long id, Integer status) {
        Delivery delivery = deliveryMapper.selectById(id);
        if (delivery == null) {
            return Result.fail(ResultCode.NOT_FOUND);
        }
        delivery.setStatus(status);
        delivery.setUpdateTime(LocalDateTime.now());
        deliveryMapper.updateById(delivery);
        return Result.ok("状态更新成功");
    }

    // ==================== WeChat Methods ====================

    @Override
    public Result<?> wxLobby(Long userId, Double lat, Double lng) {
        LambdaQueryWrapper<Delivery> deliveryWrapper = new LambdaQueryWrapper<>();
        deliveryWrapper.eq(Delivery::getUserId, userId);
        Delivery delivery = deliveryMapper.selectOne(deliveryWrapper);
        if (delivery == null) {
            return Result.fail(ResultCode.NOT_FOUND);
        }
        if (delivery.getStatus() != 1) {
            return Result.fail(ResultCode.DELIVERY_NOT_AVAILABLE);
        }

        LambdaQueryWrapper<Order> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.eq(Order::getStatus, 1);
        orderWrapper.isNull(Order::getDeliveryId);
        orderWrapper.orderByDesc(Order::getCreateTime);
        List<Order> orders = orderMapper.selectList(orderWrapper);

        List<Map<String, Object>> tasks = orders.stream().map(order -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", order.getId());
            map.put("orderNo", order.getOrderNo());
            Shop shop = shopMapper.selectById(order.getShopId());
            map.put("fee", shop != null ? shop.getDeliveryFee() : BigDecimal.ZERO);
            map.put("status", "pickup");
            map.put("shopName", shop != null ? shop.getName() : "");
            map.put("shopPhone", shop != null ? shop.getPhone() : "");
            map.put("shopAddress", shop != null ? shop.getAddress() : "");
            if (shop != null) {
                map.put("shopLat", shop.getLatitude());
                map.put("shopLng", shop.getLongitude());
                // 商家坐标缺失时尝试用地址地理编码补位，并回写数据库
                if (shop.getLatitude() == null || shop.getLongitude() == null) {
                    double[] coords = tencentMapService.geocode(shop.getAddress());
                    if (coords != null) {
                        map.put("shopLat", coords[1]);
                        map.put("shopLng", coords[0]);
                        shop.setLatitude(coords[1]);
                        shop.setLongitude(coords[0]);
                        shopMapper.updateById(shop);
                        log.info("Auto-filled shop coords via geocode: shop={}, lat={}, lng={}",
                                shop.getName(), coords[1], coords[0]);
                    }
                }
            }
            map.put("deliveryAddress", order.getAddressInfo());
            attachDeliveryInfo(map, shop, order, lng, lat);
            map.put("requireTime", order.getCreateTime());
            map.put("isTimeout", false);
            User user = userMapper.selectById(order.getUserId());
            map.put("userName", user != null ? user.getNickname() : "");
            map.put("userPhone", user != null ? user.getPhone() : "");
            // 智能派单：计算该骑手对此订单的匹配分数
            try {
                Double score = dispatchService.scoreRiderForOrder(userId, order.getId());
                map.put("dispatchScore", score);
                map.put("isRecommended", score != null && score > 0.55);
            } catch (Exception e) {
                map.put("dispatchScore", null);
                map.put("isRecommended", false);
            }
            return map;
        }).collect(Collectors.toList());

        // 按派单评分降序排序（高分订单优先展示）
        tasks.sort((a, b) -> {
            Double sa = (Double) a.getOrDefault("dispatchScore", 0.0);
            Double sb = (Double) b.getOrDefault("dispatchScore", 0.0);
            if (sa == null) sa = 0.0;
            if (sb == null) sb = 0.0;
            return Double.compare(sb, sa);
        });

        return Result.ok(tasks);
    }

    @Override
    @Transactional
    public Result<?> wxGrab(Long userId, Long orderId) {
        // Get delivery by userId
        LambdaQueryWrapper<Delivery> deliveryWrapper = new LambdaQueryWrapper<>();
        deliveryWrapper.eq(Delivery::getUserId, userId);
        Delivery delivery = deliveryMapper.selectOne(deliveryWrapper);
        if (delivery == null) {
            return Result.fail(ResultCode.NOT_FOUND);
        }
        if (delivery.getStatus() != 1) {
            return Result.fail(ResultCode.DELIVERY_NOT_AVAILABLE);
        }

        // 检查骑手当前活跃订单数
        int activeCount = countActiveOrdersByDeliveryId(delivery.getId());
        if (activeCount >= MAX_ACTIVE_ORDERS) {
            return Result.fail("已达最大同时接单数（" + MAX_ACTIVE_ORDERS + "单），请先完成当前订单再接新单");
        }

        // Get order
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            return Result.fail(ResultCode.NOT_FOUND);
        }
        // 防止竞态：订单可能已被自动派单或其他骑手抢走
        if (order.getDeliveryId() != null) {
            return Result.fail("该订单已被其他骑手接单");
        }
        if (order.getStatus() != 1) {
            return Result.fail("该订单状态已变更，无法接单");
        }

        // Set delivery status to busy
        delivery.setStatus(2);
        delivery.setUpdateTime(LocalDateTime.now());
        deliveryMapper.updateById(delivery);

        // Update order: set deliveryId, status=2 (配送中)
        order.setDeliveryId(delivery.getId());
        order.setStatus(2);
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);

        // Create DeliveryRecord
        Shop shop = shopMapper.selectById(order.getShopId());
        BigDecimal fee = (shop != null && shop.getDeliveryFee() != null) ? shop.getDeliveryFee() : new BigDecimal("3.00");

        DeliveryRecord record = new DeliveryRecord();
        record.setOrderId(orderId);
        record.setDeliveryId(delivery.getId());
        record.setFee(fee);
        record.setStatus("pickup");
        record.setCreateTime(LocalDateTime.now());
        deliveryRecordMapper.insert(record);

        notificationService.notifyOrderStatusChange(order, "骑手已接单，正在前往商家取餐");

        return Result.ok("抢单成功");
    }

    @Override
    public Result<?> wxTasks(Long userId, Double lat, Double lng) {
        LambdaQueryWrapper<Delivery> deliveryWrapper = new LambdaQueryWrapper<>();
        deliveryWrapper.eq(Delivery::getUserId, userId);
        Delivery delivery = deliveryMapper.selectOne(deliveryWrapper);
        if (delivery == null) {
            return Result.fail(ResultCode.NOT_FOUND);
        }

        LambdaQueryWrapper<DeliveryRecord> recordWrapper = new LambdaQueryWrapper<>();
        recordWrapper.eq(DeliveryRecord::getDeliveryId, delivery.getId());
        recordWrapper.in(DeliveryRecord::getStatus, "pickup", "delivering");
        recordWrapper.orderByDesc(DeliveryRecord::getCreateTime);
        List<DeliveryRecord> records = deliveryRecordMapper.selectList(recordWrapper);

        List<Map<String, Object>> pickupTasks = new ArrayList<>();
        List<Map<String, Object>> deliveringTasks = new ArrayList<>();

        for (DeliveryRecord record : records) {
            Order order = orderMapper.selectById(record.getOrderId());
            if (order == null) continue;

            Shop shop = shopMapper.selectById(order.getShopId());
            User user = userMapper.selectById(order.getUserId());

            Map<String, Object> task = new HashMap<>();
            task.put("id", record.getId());
            task.put("orderNo", order.getOrderNo());
            task.put("fee", record.getFee());
            task.put("status", record.getStatus());
            task.put("shopName", shop != null ? shop.getName() : "");
            task.put("shopPhone", shop != null ? shop.getPhone() : "");
            task.put("shopAddress", shop != null ? shop.getAddress() : "");
            if (shop != null) {
                task.put("shopLat", shop.getLatitude());
                task.put("shopLng", shop.getLongitude());
                // 商家坐标缺失时尝试用地址地理编码补位
                if (shop.getLatitude() == null || shop.getLongitude() == null) {
                    double[] coords = tencentMapService.geocode(shop.getAddress());
                    if (coords != null) {
                        task.put("shopLat", coords[1]);
                        task.put("shopLng", coords[0]);
                        shop.setLatitude(coords[1]);
                        shop.setLongitude(coords[0]);
                        shopMapper.updateById(shop);
                        log.info("Auto-filled shop coords via geocode: shop={}, lat={}, lng={}",
                                shop.getName(), coords[1], coords[0]);
                    }
                }
            }
            task.put("deliveryAddress", order.getAddressInfo());
            attachDeliveryInfo(task, shop, order, lng, lat);
            task.put("requireTime", order.getCreateTime());
            task.put("isTimeout", false);
            task.put("userName", user != null ? user.getNickname() : "");
            task.put("userPhone", user != null ? user.getPhone() : "");

            if ("pickup".equals(record.getStatus())) {
                pickupTasks.add(task);
            } else {
                deliveringTasks.add(task);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("pickupTasks", pickupTasks);
        result.put("deliveringTasks", deliveringTasks);
        return Result.ok(result);
    }

    @Override
    @Transactional
    public Result<?> wxUpdateTaskStatus(Long userId, Long recordId, String status) {
        DeliveryRecord record = deliveryRecordMapper.selectById(recordId);
        if (record == null) {
            return Result.fail(ResultCode.NOT_FOUND);
        }

        // Get delivery
        LambdaQueryWrapper<Delivery> deliveryWrapper = new LambdaQueryWrapper<>();
        deliveryWrapper.eq(Delivery::getUserId, userId);
        Delivery delivery = deliveryMapper.selectOne(deliveryWrapper);
        if (delivery == null) {
            return Result.fail(ResultCode.NOT_FOUND);
        }

        if ("delivering".equals(status) && "pickup".equals(record.getStatus())) {
            // [演示模式] 跳过地理位置校验，允许远程确认取餐
            Order pickupOrder = orderMapper.selectById(record.getOrderId());
            // Pickup: pickupTime=now, update record status
            record.setPickupTime(LocalDateTime.now());
            record.setStatus("delivering");
            deliveryRecordMapper.updateById(record);

            if (pickupOrder != null) {
                notificationService.notifyOrderStatusChange(pickupOrder, "骑手已取餐，正在配送中");
            }
        } else if ("completed".equals(status) && "delivering".equals(record.getStatus())) {
            // [演示模式] 跳过地理位置校验，允许远程确认送达
            // 核心事务：更新配送记录 + 订单状态 + 骑手状态 + 余额
            record.setDeliverTime(LocalDateTime.now());
            record.setStatus("completed");
            deliveryRecordMapper.updateById(record);

            Order order = orderMapper.selectById(record.getOrderId());
            if (order != null) {
                order.setStatus(3);
                order.setUpdateTime(LocalDateTime.now());
                orderMapper.updateById(order);
            }

            // 只有当前没有其他活跃订单时才切回在线
            int remainingActive = countActiveOrdersByDeliveryId(delivery.getId());
            if (remainingActive <= 0) {
                delivery.setStatus(1);
            }
            // 更新余额和总配送数
            BigDecimal currentBalance = delivery.getBalance() != null ? delivery.getBalance() : BigDecimal.ZERO;
            delivery.setBalance(currentBalance.add(record.getFee()));
            delivery.setTotalDeliveries((delivery.getTotalDeliveries() != null ? delivery.getTotalDeliveries() : 0) + 1);
            calculateLevel(delivery);
            delivery.setUpdateTime(LocalDateTime.now());
            deliveryMapper.updateById(delivery);

            // 异步：通知推送 + 骑手统计更新
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("deliveryId", delivery.getId());
            eventData.put("orderId", record.getOrderId());
            eventData.put("fee", record.getFee().toString());

            EventMessage event = EventMessage.builder()
                    .type(EventMessage.DELIVERY_COMPLETED)
                    .data(eventData)
                    .timestamp(LocalDateTime.now())
                    .build();
            redisMessagePublisher.publishDeliveryEvent(event);

            if (order != null) {
                notificationService.notifyOrderStatusChange(order, "订单已送达，请确认并评价");
            }
        } else {
            return Result.fail(ResultCode.ORDER_STATUS_ERROR);
        }

        return Result.ok();
    }

    @Override
    public Result<?> wxUpdateStatus(Long userId, Integer status) {
        LambdaQueryWrapper<Delivery> deliveryWrapper = new LambdaQueryWrapper<>();
        deliveryWrapper.eq(Delivery::getUserId, userId);
        Delivery delivery = deliveryMapper.selectOne(deliveryWrapper);
        if (delivery == null) {
            return Result.fail(ResultCode.NOT_FOUND);
        }
        // 如果要上线，检查是否有未完成的订单（防止通过切换状态绕过接单限制）
        if (status == 1) {
            int activeCount = countActiveOrdersByDeliveryId(delivery.getId());
            if (activeCount > 0) {
                return Result.fail("当前还有" + activeCount + "单未完成配送，无法上线接单");
            }
        }
        delivery.setStatus(status);
        delivery.setUpdateTime(LocalDateTime.now());
        deliveryMapper.updateById(delivery);
        return Result.ok();
    }

    @Override
    public Result<?> wxIncome(Long userId) {
        // Get delivery by userId
        LambdaQueryWrapper<Delivery> deliveryWrapper = new LambdaQueryWrapper<>();
        deliveryWrapper.eq(Delivery::getUserId, userId);
        Delivery delivery = deliveryMapper.selectOne(deliveryWrapper);
        if (delivery == null) {
            return Result.fail(ResultCode.NOT_FOUND);
        }

        // Query DeliveryRecord by deliveryId where status="completed"
        LambdaQueryWrapper<DeliveryRecord> recordWrapper = new LambdaQueryWrapper<>();
        recordWrapper.eq(DeliveryRecord::getDeliveryId, delivery.getId());
        recordWrapper.eq(DeliveryRecord::getStatus, "completed");
        recordWrapper.orderByDesc(DeliveryRecord::getDeliverTime);
        List<DeliveryRecord> records = deliveryRecordMapper.selectList(recordWrapper);

        // Build income records
        BigDecimal afterBalance = delivery.getBalance() != null ? delivery.getBalance() : BigDecimal.ZERO;
        List<Map<String, Object>> incomeRecords = new ArrayList<>();

        // Build records in chronological order
        BigDecimal runningBalance = BigDecimal.ZERO;
        for (int i = records.size() - 1; i >= 0; i--) {
            DeliveryRecord record = records.get(i);
            Order order = orderMapper.selectById(record.getOrderId());
            runningBalance = runningBalance.add(record.getFee());

            Map<String, Object> incomeMap = new HashMap<>();
            incomeMap.put("id", record.getId());
            incomeMap.put("type", "配送收入-订单 #" + (order != null ? order.getOrderNo() : record.getOrderId()));
            incomeMap.put("time", record.getDeliverTime());
            incomeMap.put("amount", "+" + record.getFee().toString());
            incomeMap.put("afterBalance", runningBalance.toString());
            incomeRecords.add(incomeMap);
        }

        // Calculate today stats
        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        BigDecimal todayIncome = records.stream()
                .filter(r -> r.getDeliverTime() != null && r.getDeliverTime().isAfter(todayStart))
                .map(DeliveryRecord::getFee)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int todayOrders = (int) records.stream()
                .filter(r -> r.getDeliverTime() != null && r.getDeliverTime().isAfter(todayStart))
                .count();

        Map<String, Object> result = new HashMap<>();
        result.put("balance", afterBalance.toString());
        result.put("todayIncome", todayIncome.toString());
        result.put("todayOrders", todayOrders);
        result.put("records", incomeRecords);

        return Result.ok(result);
    }

    @Override
    public Result<?> wxProfile(Long userId) {
        LambdaQueryWrapper<Delivery> deliveryWrapper = new LambdaQueryWrapper<>();
        deliveryWrapper.eq(Delivery::getUserId, userId);
        Delivery delivery = deliveryMapper.selectOne(deliveryWrapper);
        if (delivery == null) {
            return Result.fail("骑手信息不存在");
        }
        User user = userMapper.selectById(userId);

        Map<String, Object> profile = new HashMap<>();
        profile.put("name", delivery.getName());
        profile.put("phone", delivery.getPhone());
        profile.put("vehicle", delivery.getVehicle());
        profile.put("onTimeRate", delivery.getOnTimeRate());
        profile.put("praiseRate", delivery.getPraiseRate());
        profile.put("totalDeliveries", delivery.getTotalDeliveries());
        profile.put("balance", delivery.getBalance());
        profile.put("level", delivery.getLevel() != null ? delivery.getLevel() : 0);
        String levelName = "铜牌骑手";
        if (delivery.getLevel() != null && delivery.getLevel() == 1) levelName = "银牌骑手";
        if (delivery.getLevel() != null && delivery.getLevel() == 2) levelName = "金牌骑手";
        profile.put("levelName", levelName);
        profile.put("avatar", user != null ? user.getAvatar() : null);
        profile.put("nickname", user != null ? user.getNickname() : "");
        profile.put("verifyStatus", delivery.getVerifyStatus() != null ? delivery.getVerifyStatus() : 0);
        profile.put("realName", delivery.getRealName());
        profile.put("id", delivery.getId());

        return Result.ok(profile);
    }

    // ==================== Location Tracking ====================

    @Override
    public Result<?> reportLocation(Long userId, Double lat, Double lng) {
        LambdaQueryWrapper<Delivery> deliveryWrapper = new LambdaQueryWrapper<>();
        deliveryWrapper.eq(Delivery::getUserId, userId);
        Delivery delivery = deliveryMapper.selectOne(deliveryWrapper);
        if (delivery == null) {
            return Result.fail(ResultCode.NOT_FOUND);
        }

        // Store in Redis with 3-minute TTL
        Map<String, Object> loc = new LinkedHashMap<>();
        loc.put("deliveryId", delivery.getId());
        loc.put("riderName", delivery.getName());
        loc.put("lat", lat);
        loc.put("lng", lng);
        loc.put("timestamp", System.currentTimeMillis());

        String key = "rider:location:" + delivery.getId();
        redisUtil.set(key, loc, 15, TimeUnit.MINUTES);

        // 异步持久化轨迹到DB（找到当前活跃订单关联）
        try {
            LambdaQueryWrapper<DeliveryRecord> drWrapper = new LambdaQueryWrapper<>();
            drWrapper.eq(DeliveryRecord::getDeliveryId, delivery.getId())
                     .in(DeliveryRecord::getStatus, "pickup", "delivering")
                     .orderByDesc(DeliveryRecord::getCreateTime)
                     .last("LIMIT 1");
            DeliveryRecord activeRecord = deliveryRecordMapper.selectList(drWrapper)
                    .stream().findFirst().orElse(null);

            if (activeRecord != null) {
                DeliveryTrack track = new DeliveryTrack();
                track.setDeliveryId(delivery.getId());
                track.setOrderId(activeRecord.getOrderId());
                track.setLat(lat);
                track.setLng(lng);
                track.setReportTime(LocalDateTime.now());
                track.setCreateTime(LocalDateTime.now());
                deliveryTrackMapper.insert(track);
            }
        } catch (Exception e) {
            // 轨迹落库失败不影响主流程
        }

        // Push location to customers who have active deliveries with this rider
        pushRiderLocationToCustomers(delivery.getId(), lat, lng);

        return Result.ok();
    }

    @Override
    public Result<?> getRiderLocation(Long deliveryId) {
        String key = "rider:location:" + deliveryId;
        @SuppressWarnings("unchecked")
        Map<String, Object> loc = (Map<String, Object>) redisUtil.get(key);
        if (loc == null) {
            return Result.fail("骑手位置已过期或未上报");
        }
        return Result.ok(loc);
    }

    @Override
    public Result<?> getRiderLocationByOrder(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            return Result.fail(ResultCode.NOT_FOUND);
        }
        if (order.getDeliveryId() == null) {
            return Result.fail("该订单暂未分配骑手");
        }
        return getRiderLocation(order.getDeliveryId());
    }

    /**
     * Push rider's real-time location to all customers with active deliveries from this rider
     */
    private void pushRiderLocationToCustomers(Long deliveryId, Double lat, Double lng) {
        LambdaQueryWrapper<DeliveryRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DeliveryRecord::getDeliveryId, deliveryId)
               .eq(DeliveryRecord::getStatus, "delivering");
        List<DeliveryRecord> records = deliveryRecordMapper.selectList(wrapper);

        for (DeliveryRecord record : records) {
            Order order = orderMapper.selectById(record.getOrderId());
            if (order != null) {
                Map<String, Object> msg = new LinkedHashMap<>();
                msg.put("type", "rider_location");
                msg.put("orderId", order.getId());
                msg.put("orderNo", order.getOrderNo());
                msg.put("lat", lat);
                msg.put("lng", lng);
                msg.put("timestamp", System.currentTimeMillis());
                try {
                    String json = objectMapper.writeValueAsString(msg);
                    OrderNotificationEndpoint.sendToUser(String.valueOf(order.getUserId()), json);
                } catch (Exception e) {
                    // Silently fail
                }
            }
        }
    }

    // ==================== Track Query ====================

    @Override
    public Result<?> getTrackPoints(Long orderId) {
        LambdaQueryWrapper<DeliveryTrack> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DeliveryTrack::getOrderId, orderId)
               .orderByAsc(DeliveryTrack::getReportTime);
        List<DeliveryTrack> tracks = deliveryTrackMapper.selectList(wrapper);

        List<Map<String, Object>> points = tracks.stream().map(t -> {
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("lat", t.getLat());
            p.put("lng", t.getLng());
            p.put("time", t.getReportTime().toString());
            return p;
        }).collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("orderId", orderId);
        result.put("count", points.size());
        result.put("points", points);
        return Result.ok(result);
    }

    // ==================== Verification ====================

    @Override
    public Result<?> applyVerification(Long userId, String realName, String idCard) {
        LambdaQueryWrapper<Delivery> deliveryWrapper = new LambdaQueryWrapper<>();
        deliveryWrapper.eq(Delivery::getUserId, userId);
        Delivery delivery = deliveryMapper.selectOne(deliveryWrapper);
        if (delivery == null) {
            return Result.fail(ResultCode.NOT_FOUND);
        }
        if (delivery.getVerifyStatus() != null && delivery.getVerifyStatus() == 2) {
            return Result.fail("已通过实名认证，无需重复申请");
        }
        delivery.setRealName(realName);
        delivery.setIdCard(idCard);
        delivery.setVerifyStatus(1); // 审核中
        delivery.setUpdateTime(LocalDateTime.now());
        deliveryMapper.updateById(delivery);
        return Result.ok("实名认证申请已提交，等待审核");
    }

    @Override
    public Result<?> getVerificationStatus(Long userId) {
        LambdaQueryWrapper<Delivery> deliveryWrapper = new LambdaQueryWrapper<>();
        deliveryWrapper.eq(Delivery::getUserId, userId);
        Delivery delivery = deliveryMapper.selectOne(deliveryWrapper);
        if (delivery == null) {
            return Result.fail(ResultCode.NOT_FOUND);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("verifyStatus", delivery.getVerifyStatus() != null ? delivery.getVerifyStatus() : 0);
        result.put("realName", delivery.getRealName());
        result.put("verifyRemark", delivery.getVerifyRemark());
        return Result.ok(result);
    }

    @Override
    public Result<?> reviewVerification(Long deliveryId, Integer verifyStatus, String remark) {
        Delivery delivery = deliveryMapper.selectById(deliveryId);
        if (delivery == null) {
            return Result.fail(ResultCode.NOT_FOUND);
        }
        if (delivery.getVerifyStatus() == null || delivery.getVerifyStatus() != 1) {
            return Result.fail("该骑手未提交认证申请或已审核");
        }
        if (verifyStatus != 2 && verifyStatus != 0) {
            return Result.fail("审核状态无效，仅支持通过(2)或拒绝(0)");
        }
        delivery.setVerifyStatus(verifyStatus);
        delivery.setVerifyRemark(remark != null ? remark : "");
        delivery.setUpdateTime(LocalDateTime.now());
        deliveryMapper.updateById(delivery);

        // 审核通过后自动通知骑手（可通过 WebSocket 推送）
        String msg = verifyStatus == 2 ? "实名认证已通过" : "实名认证未通过: " + (remark != null ? remark : "");
        log.info("Delivery verification reviewed: deliveryId={}, status={}, remark={}", deliveryId, verifyStatus, remark);
        return Result.ok(msg);
    }

    @Override
    public Result<?> updateVehicle(Long userId, String vehicle) {
        LambdaQueryWrapper<Delivery> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Delivery::getUserId, userId);
        Delivery delivery = deliveryMapper.selectOne(wrapper);
        if (delivery == null) {
            return Result.fail("请先完成骑手认证");
        }
        delivery.setVehicle(vehicle);
        delivery.setUpdateTime(LocalDateTime.now());
        deliveryMapper.updateById(delivery);
        return Result.ok("车辆信息已更新");
    }

    /**
     * 统计骑手当前活跃订单数（取餐中 + 配送中）
     */
    private int countActiveOrdersByDeliveryId(Long deliveryId) {
        LambdaQueryWrapper<DeliveryRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DeliveryRecord::getDeliveryId, deliveryId)
               .in(DeliveryRecord::getStatus, "pickup", "delivering");
        Long count = deliveryRecordMapper.selectCount(wrapper);
        return count != null ? count.intValue() : 0;
    }
}
