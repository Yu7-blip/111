package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.common.Result;
import com.example.backend.dto.response.StatisticsResponse;
import com.example.backend.entity.*;
import com.example.backend.mapper.*;
import com.example.backend.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final UserMapper userMapper;
    private final ShopMapper shopMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final DeliveryMapper deliveryMapper;
    private final GoodsMapper goodsMapper;
    private final GoodsCategoryMapper goodsCategoryMapper;

    @Override
    public Result<?> dashboardStats() {
        // Count users with role "user"
        LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(User::getRole, "user");
        Long userCount = userMapper.selectCount(userWrapper);

        // Count shops with status 1
        LambdaQueryWrapper<Shop> shopWrapper = new LambdaQueryWrapper<>();
        shopWrapper.eq(Shop::getStatus, 1);
        Long merchantCount = shopMapper.selectCount(shopWrapper);

        // Count orders with status 0 or 1 (approximate today)
        LambdaQueryWrapper<Order> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.in(Order::getStatus, 0, 1);
        Long todayOrderCount = orderMapper.selectCount(orderWrapper);

        // Count online deliveries (status 1)
        LambdaQueryWrapper<Delivery> deliveryWrapper = new LambdaQueryWrapper<>();
        deliveryWrapper.eq(Delivery::getStatus, 1);
        Long onlineDeliveryCount = deliveryMapper.selectCount(deliveryWrapper);

        // Today's revenue (已支付+配送中+已完成订单的实付金额)
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LambdaQueryWrapper<Order> revenueWrapper = new LambdaQueryWrapper<>();
        revenueWrapper.in(Order::getStatus, 1, 2, 3)
                .ge(Order::getCreateTime, todayStart);
        List<Order> todayPaidOrders = orderMapper.selectList(revenueWrapper);
        BigDecimal todayRevenue = todayPaidOrders.stream()
                .map(o -> o.getActualAmount() != null ? o.getActualAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        StatisticsResponse response = StatisticsResponse.builder()
                .userCount(userCount)
                .merchantCount(merchantCount)
                .todayOrderCount(todayOrderCount)
                .onlineDeliveryCount(onlineDeliveryCount)
                .todayRevenue(todayRevenue.toString())
                .build();

        return Result.ok(response);
    }

    @Override
    public Result<?> orderTrend() {
        List<Map<String, Object>> trendData = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("M月");
        for (int i = 5; i >= 0; i--) {
            LocalDate month = LocalDate.now().minusMonths(i);
            LocalDateTime monthStart = month.withDayOfMonth(1).atStartOfDay();
            LocalDateTime monthEnd = monthStart.plusMonths(1);
            LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
            wrapper.ge(Order::getCreateTime, monthStart)
                    .lt(Order::getCreateTime, monthEnd);
            long count = orderMapper.selectCount(wrapper);
            Map<String, Object> map = new HashMap<>();
            map.put("month", month.format(fmt));
            map.put("count", count);
            trendData.add(map);
        }
        return Result.ok(trendData);
    }

    @Override
    public Result<?> merchantDashboardStats(Long shopId) {
        Map<String, Object> result = new HashMap<>();

        // Total goods count
        LambdaQueryWrapper<Goods> goodsWrapper = new LambdaQueryWrapper<>();
        goodsWrapper.eq(Goods::getShopId, shopId);
        Long goodsCount = goodsMapper.selectCount(goodsWrapper);
        result.put("goodsCount", goodsCount);

        // Today's orders
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LambdaQueryWrapper<Order> todayWrapper = new LambdaQueryWrapper<>();
        todayWrapper.eq(Order::getShopId, shopId)
                .ge(Order::getCreateTime, todayStart);
        List<Order> todayOrders = orderMapper.selectList(todayWrapper);
        result.put("todayOrderCount", todayOrders.size());
        BigDecimal todayRevenue = todayOrders.stream()
                .map(o -> o.getActualAmount() != null ? o.getActualAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        result.put("todayRevenue", todayRevenue.toString());

        // Unique customer count for this shop
        LambdaQueryWrapper<Order> customerWrapper = new LambdaQueryWrapper<>();
        customerWrapper.eq(Order::getShopId, shopId);
        List<Order> shopOrders = orderMapper.selectList(customerWrapper);
        long customerCount = shopOrders.stream()
                .map(Order::getUserId)
                .distinct()
                .count();
        result.put("customerCount", customerCount);

        // Recent 7 days sales trend
        List<Map<String, Object>> salesTrend = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("M/d");
        for (int i = 6; i >= 0; i--) {
            LocalDate day = LocalDate.now().minusDays(i);
            LocalDateTime dayStart = day.atStartOfDay();
            LocalDateTime dayEnd = day.plusDays(1).atStartOfDay();
            LambdaQueryWrapper<Order> dayWrapper = new LambdaQueryWrapper<>();
            dayWrapper.eq(Order::getShopId, shopId)
                    .ge(Order::getCreateTime, dayStart)
                    .lt(Order::getCreateTime, dayEnd);
            List<Order> dayOrders = orderMapper.selectList(dayWrapper);
            BigDecimal dayRevenue = dayOrders.stream()
                    .map(o -> o.getActualAmount() != null ? o.getActualAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            Map<String, Object> point = new HashMap<>();
            point.put("date", day.format(fmt));
            point.put("amount", dayRevenue.doubleValue());
            point.put("count", dayOrders.size());
            salesTrend.add(point);
        }
        result.put("salesTrend", salesTrend);

        // Category distribution
        LambdaQueryWrapper<GoodsCategory> catWrapper = new LambdaQueryWrapper<>();
        catWrapper.eq(GoodsCategory::getShopId, shopId);
        List<GoodsCategory> categories = goodsCategoryMapper.selectList(catWrapper);

        List<Map<String, Object>> categoryPie = new ArrayList<>();
        for (GoodsCategory cat : categories) {
            LambdaQueryWrapper<Goods> gWrapper = new LambdaQueryWrapper<>();
            gWrapper.eq(Goods::getCategoryId, cat.getId());
            Long count = goodsMapper.selectCount(gWrapper);
            Map<String, Object> pie = new HashMap<>();
            pie.put("name", cat.getName());
            pie.put("value", count);
            categoryPie.add(pie);
        }
        result.put("categoryDistribution", categoryPie);

        // 7-day order stats
        List<Map<String, Object>> orderTrend = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate day = LocalDate.now().minusDays(i);
            LocalDateTime dayStart = day.atStartOfDay();
            LocalDateTime dayEnd = day.plusDays(1).atStartOfDay();
            LambdaQueryWrapper<Order> dayWrapper = new LambdaQueryWrapper<>();
            dayWrapper.eq(Order::getShopId, shopId)
                    .ge(Order::getCreateTime, dayStart)
                    .lt(Order::getCreateTime, dayEnd);
            List<Order> dayOrders = orderMapper.selectList(dayWrapper);
            long completed = dayOrders.stream().filter(o -> o.getStatus() == 3).count();
            Map<String, Object> point = new HashMap<>();
            point.put("date", day.format(fmt));
            point.put("total", dayOrders.size());
            point.put("completed", completed);
            orderTrend.add(point);
        }
        result.put("orderTrend", orderTrend);

        return Result.ok(result);
    }
}
