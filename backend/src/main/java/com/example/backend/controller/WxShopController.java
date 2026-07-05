package com.example.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.common.PageResult;
import com.example.backend.common.Result;
import com.example.backend.common.ResultCode;
import com.example.backend.entity.Shop;
import com.example.backend.mapper.ShopMapper;
import com.example.backend.service.TencentMapService;
import com.example.backend.service.GoodsService;
import com.example.backend.utils.CacheUtil;
import com.example.backend.utils.GeoHashUtil;
import com.example.backend.utils.RedisUtil;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.utils.GeoUtil;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/wx/shops")
@RequiredArgsConstructor
@Slf4j
public class WxShopController {

    private final ShopMapper shopMapper;
    private final GoodsService goodsService;
    private final RedisUtil redisUtil;
    private final CacheUtil cacheUtil;
    private final TencentMapService tencentMapService;

    @GetMapping
    @CircuitBreaker(name = "shopService", fallbackMethod = "shopListFallback")
    public Result<?> list(@RequestParam(defaultValue = "1") Integer page,
                          @RequestParam(defaultValue = "10") Integer pageSize,
                          @RequestParam(required = false) Double lat,
                          @RequestParam(required = false) Double lng,
                          @RequestParam(defaultValue = "10.0") Double radius,
                          @RequestParam(defaultValue = "rating") String sort) {
        log.info("wx shop list: page={}, pageSize={}, lat={}, lng={}, radius={}, sort={}", page, pageSize, lat, lng, radius, sort);

        // 无位置信息时可缓存（按页缓存 5分钟）
        boolean canCache = lat == null && page <= 2;
        if (canCache) {
            String cacheKey = "cache:shops:list:" + page + ":" + pageSize;
            @SuppressWarnings("unchecked")
            PageResult<Map<String, Object>> cached = (PageResult<Map<String, Object>>) redisUtil.get(cacheKey);
            if (cached != null) {
                return Result.ok(cached);
            }
        }

        LambdaQueryWrapper<Shop> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Shop::getStatus, 1)
               .eq(Shop::getBusinessStatus, 1);

        // ============ GeoHash 空间过滤（数据库层面缩小范围）============
        if (lat != null && lng != null) {
            int precision = GeoHashUtil.precisionForRadius(radius);
            int prefixLen = Math.max(3, precision);
            String userHash = GeoHashUtil.encode(lat, lng, 7);
            String prefix = userHash.substring(0, Math.min(prefixLen, userHash.length()));
            List<String> neighbors = GeoHashUtil.getNeighbors(prefix);

            // 构建 WHERE (geohash LIKE 'prefix%' OR geohash LIKE 'neighbor1%' OR ... OR geohash IS NULL)
            // NULL 兜底：老店铺尚未生成 geohash 也纳入结果
            wrapper.and(w -> {
                w.likeRight(Shop::getGeohash, prefix);
                for (String n : neighbors) {
                    w.or().likeRight(Shop::getGeohash, n);
                }
                w.or().isNull(Shop::getGeohash);
            });
        }

        if ("sales".equals(sort)) {
            wrapper.orderByDesc(Shop::getSales);
        } else {
            wrapper.orderByDesc(Shop::getRating);
        }

        // DB 查询 → 已经过 GeoHash 粗筛，数据量大幅减少
        List<Shop> candidateShops = shopMapper.selectList(wrapper);
        log.debug("GeoHash filtered: {} candidate shops", candidateShops.size());

        // ============ Haversine 精确距离过滤 ============
        if (lat != null && lng != null && candidateShops.size() > 1) {
            // 计算直线距离，过滤超出半径的店铺
            List<Shop> withinRadius = new ArrayList<>();
            for (Shop shop : candidateShops) {
                if (shop.getLatitude() != null && shop.getLongitude() != null) {
                    double dist = GeoUtil.haversineDistance(lat, lng, shop.getLatitude(), shop.getLongitude());
                    shop.setDistance(dist);
                    if (dist <= radius) {
                        withinRadius.add(shop);
                    }
                } else {
                    withinRadius.add(shop); // 无坐标的不排除
                }
            }
            candidateShops = withinRadius;
            log.debug("Haversine filtered: {} shops within {}km", candidateShops.size(), radius);
        }

        // ============ 若过滤后没有有坐标的店铺，回退显示所有营业店铺 ============
        boolean hasNoShopsWithCoords = candidateShops.stream()
                .noneMatch(s -> s.getLatitude() != null && s.getLongitude() != null);
        if (hasNoShopsWithCoords && lat != null && lng != null) {
            log.info("No shops with coords near ({},{}), falling back to all open shops", lat, lng);
            LambdaQueryWrapper<Shop> fallbackWrapper = new LambdaQueryWrapper<>();
            fallbackWrapper.eq(Shop::getStatus, 1)
                           .eq(Shop::getBusinessStatus, 1)
                           .orderByDesc(Shop::getRating)
                           .last("LIMIT 50");
            candidateShops = shopMapper.selectList(fallbackWrapper);
        }

        // ============ 排序 ============
        if ("distance".equals(sort)) {
            candidateShops.sort((a, b) -> Double.compare(
                    a.getDistance() != null ? a.getDistance() : Double.MAX_VALUE,
                    b.getDistance() != null ? b.getDistance() : Double.MAX_VALUE));
        } else if ("sales".equals(sort)) {
            candidateShops.sort((a, b) -> Integer.compare(
                    b.getSales() != null ? b.getSales() : 0,
                    a.getSales() != null ? a.getSales() : 0));
        } else {
            candidateShops.sort((a, b) -> Double.compare(
                    b.getRating() != null ? b.getRating().doubleValue() : 0,
                    a.getRating() != null ? a.getRating().doubleValue() : 0));
        }

        // ============ 分页 ============
        int total = candidateShops.size();
        int fromIndex = (page - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);
        List<Shop> paged = fromIndex < total ? candidateShops.subList(fromIndex, toIndex) : Collections.emptyList();

        // ============ 仅对当前页调用腾讯地图获取真实骑行距离 ============
        if (lat != null && lng != null && !paged.isEmpty()) {
            // 收集需要真实距离的店铺（Haversine距离 < 3km 的才调用 API，远的用直线距离即可）
            List<double[]> apiDests = new ArrayList<>();
            List<Shop> apiShops = new ArrayList<>();
            for (Shop shop : paged) {
                if (shop.getLatitude() != null && shop.getLongitude() != null
                        && (shop.getDistance() == null || shop.getDistance() < 3.0)) {
                    apiDests.add(new double[]{shop.getLongitude(), shop.getLatitude()});
                    apiShops.add(shop);
                }
            }
            if (!apiDests.isEmpty()) {
                Map<String, Double> realDistances = tencentMapService.batchBikingDistance(lng, lat, apiDests);
                for (Shop shop : apiShops) {
                    String key = shop.getLongitude() + "," + shop.getLatitude();
                    Double realDist = realDistances.get(key);
                    if (realDist != null) {
                        shop.setDistance(realDist);
                    }
                }
            }

            // Re-sort paged items by real biking distance
            if ("distance".equals(sort)) {
                paged.sort((a, b) -> Double.compare(
                        a.getDistance() != null ? a.getDistance() : Double.MAX_VALUE,
                        b.getDistance() != null ? b.getDistance() : Double.MAX_VALUE));
            }
        }

        // ============ 构建响应 ============
        List<Map<String, Object>> records = paged.stream().map(shop -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", shop.getId());
            map.put("name", shop.getName());
            map.put("logo", shop.getLogo());
            map.put("description", shop.getDescription());
            map.put("address", shop.getAddress());
            map.put("minPrice", shop.getMinPrice());
            map.put("deliveryFee", shop.getDeliveryFee());
            map.put("rating", shop.getRating());
            map.put("sales", shop.getSales());
            map.put("notice", shop.getNotice());
            map.put("openTime", shop.getOpenTime());
            map.put("closeTime", shop.getCloseTime());
            map.put("businessStatus", shop.getBusinessStatus());
            map.put("isOpen", shop.getBusinessStatus() != null && shop.getBusinessStatus() == 1);
            if (shop.getDistance() != null) {
                map.put("distance", String.format("%.1f", shop.getDistance()));
            }
            return map;
        }).collect(Collectors.toList());

        PageResult<Map<String, Object>> result = PageResult.of(records, total, page, pageSize);

        if (canCache) {
            redisUtil.set("cache:shops:list:" + page + ":" + pageSize, result, 5, TimeUnit.MINUTES);
        }
        // 保存长TTL兜底缓存（熔断降级用）
        redisUtil.set("fallback:shops:list:" + page + ":" + pageSize, result, 30, TimeUnit.MINUTES);
        return Result.ok(result);
    }

    @GetMapping("/{id}/goods")
    public Result<?> shopGoods(@PathVariable Long id) {
        log.info("wx shop goods: shopId={}", id);
        return goodsService.wxShopGoods(id);
    }

    // ==================== 熔断降级 ====================

    @SuppressWarnings("unchecked")
    public Result<?> shopListFallback(Integer page, Integer pageSize, Double lat, Double lng,
                                      Double radius, String sort, Throwable t) {
        log.warn("Shop list circuit breaker OPEN, using fallback: {}", t.getMessage());
        String cacheKey = "fallback:shops:list:" + page + ":" + pageSize;
        Object cached = redisUtil.get(cacheKey);
        if (cached != null) {
            // 正常缓存也存一份长TTL的兜底
            return Result.ok(cached);
        }
        return Result.fail(ResultCode.INTERNAL_ERROR.getCode(), "附近商家查询繁忙，请下拉刷新重试");
    }
}
