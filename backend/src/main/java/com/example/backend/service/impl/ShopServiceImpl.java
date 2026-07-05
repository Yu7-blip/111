package com.example.backend.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.common.PageResult;
import com.example.backend.common.Result;
import com.example.backend.dto.request.ShopRegisterRequest;
import com.example.backend.entity.Evaluation;
import com.example.backend.entity.Order;
import com.example.backend.entity.Shop;
import com.example.backend.entity.GoodsCategory;
import com.example.backend.mapper.EvaluationMapper;
import com.example.backend.mapper.GoodsCategoryMapper;
import com.example.backend.mapper.OrderMapper;
import com.example.backend.mapper.ShopMapper;
import com.example.backend.service.TencentMapService;
import com.example.backend.service.ShopService;
import com.example.backend.utils.GeoHashUtil;
import com.example.backend.utils.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShopServiceImpl implements ShopService {

    private final ShopMapper shopMapper;
    private final TencentMapService tencentMapService;
    private final GoodsCategoryMapper goodsCategoryMapper;
    private final OrderMapper orderMapper;
    private final EvaluationMapper evaluationMapper;
    private final RedisUtil redisUtil;

    @Override
    public Result<?> list(Integer page, Integer pageSize, String name, Integer status) {
        LambdaQueryWrapper<Shop> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(name)) {
            wrapper.like(Shop::getName, name);
        }
        if (status != null) {
            wrapper.eq(Shop::getStatus, status);
        }
        wrapper.orderByDesc(Shop::getCreateTime);

        Page<Shop> mpPage = new Page<>(page, pageSize);
        Page<Shop> resultPage = shopMapper.selectPage(mpPage, wrapper);

        List<Map<String, Object>> records = new ArrayList<>();
        for (Shop shop : resultPage.getRecords()) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", shop.getId());
            map.put("shopNo", shop.getShopNo());
            map.put("name", shop.getName());
            map.put("contact", shop.getName());
            map.put("phone", shop.getPhone());
            map.put("email", shop.getEmail());
            map.put("address", shop.getAddress());
            map.put("description", shop.getDescription());
            map.put("logo", shop.getLogo());
            map.put("openTime", shop.getOpenTime());
            map.put("closeTime", shop.getCloseTime());
            map.put("minPrice", shop.getMinPrice());
            map.put("deliveryFee", shop.getDeliveryFee());
            map.put("rating", shop.getRating());
            map.put("sales", shop.getSales());
            map.put("notice", shop.getNotice());
            map.put("businessStatus", shop.getBusinessStatus());
            map.put("status", shop.getStatus());
            map.put("auditRemark", shop.getAuditRemark());
            map.put("username", shop.getUsername());
            map.put("createTime", shop.getCreateTime());
            records.add(map);
        }

        return Result.ok(PageResult.of(records, resultPage.getTotal(), resultPage.getCurrent(), resultPage.getSize()));
    }

    @Override
    public Result<?> detail(Long id) {
        Shop shop = shopMapper.selectById(id);
        if (shop == null) {
            return Result.fail("店铺不存在");
        }
        Map<String, Object> map = new HashMap<>();
        map.put("id", shop.getId());
        map.put("shopNo", shop.getShopNo());
        map.put("name", shop.getName());
        map.put("contact", shop.getName());
        map.put("phone", shop.getPhone());
        map.put("email", shop.getEmail());
        map.put("address", shop.getAddress());
        map.put("description", shop.getDescription());
        map.put("logo", shop.getLogo());
        map.put("openTime", shop.getOpenTime());
        map.put("closeTime", shop.getCloseTime());
        map.put("minPrice", shop.getMinPrice());
        map.put("deliveryFee", shop.getDeliveryFee());
        map.put("rating", shop.getRating());
        map.put("sales", shop.getSales());
        map.put("notice", shop.getNotice());
        map.put("businessStatus", shop.getBusinessStatus());
        map.put("status", shop.getStatus());
        map.put("auditRemark", shop.getAuditRemark());
        map.put("username", shop.getUsername());
        map.put("password", shop.getPassword());
        map.put("createTime", shop.getCreateTime());
        return Result.ok(map);
    }

    @Override
    public Result<?> audit(Long id, Integer status, String remark) {
        Shop shop = shopMapper.selectById(id);
        if (shop == null) {
            return Result.fail("店铺不存在");
        }
        shop.setStatus(status);
        shop.setAuditRemark(remark);
        shopMapper.updateById(shop);
        String message = status == 1 ? "审核通过" : "审核拒绝";
        return Result.ok(message);
    }

    @Override
    public Result<?> register(ShopRegisterRequest request) {
        Shop shop = new Shop();
        shop.setName(request.getName());
        shop.setPhone(request.getPhone());
        shop.setEmail(request.getEmail());
        shop.setAddress(request.getAddress());
        shop.setDescription(request.getDescription());
        shop.setUsername(request.getUsername());
        shop.setPassword(BCrypt.hashpw(request.getPassword(), BCrypt.gensalt()));
        shop.setStatus(0);
        shop.setRating(BigDecimal.ZERO);
        geocodeShop(shop);
        shopMapper.insert(shop);
        // 自动生成 shopNo: SHOP + id
        shop.setShopNo("SHOP" + shop.getId());
        shopMapper.updateById(shop);
        initDefaultCategories(shop.getId());
        return Result.ok("注册成功，请等待审核");
    }

    @Override
    public Result<?> getMerchantShop(Long shopId) {
        Shop shop = shopMapper.selectById(shopId);
        if (shop == null) {
            return Result.fail("店铺不存在");
        }
        // 不泄露密码给商家端，但保留用户名和编号供展示
        shop.setPassword(null);
        return Result.ok(shop);
    }

    @Override
    public Result<?> updateMerchantShop(Long shopId, Map<String, Object> data) {
        Shop shop = shopMapper.selectById(shopId);
        if (shop == null) {
            return Result.fail("店铺不存在");
        }
        boolean needGeocode = false;
        boolean hasManualCoords = false;
        if (data.containsKey("name")) {
            shop.setName((String) data.get("name"));
        }
        if (data.containsKey("phone")) {
            shop.setPhone((String) data.get("phone"));
        }
        if (data.containsKey("email")) {
            shop.setEmail((String) data.get("email"));
        }
        if (data.containsKey("address")) {
            shop.setAddress((String) data.get("address"));
            needGeocode = true;
        }
        if (data.containsKey("description")) {
            shop.setDescription((String) data.get("description"));
        }
        if (data.containsKey("openTime")) {
            shop.setOpenTime((String) data.get("openTime"));
        }
        if (data.containsKey("closeTime")) {
            shop.setCloseTime((String) data.get("closeTime"));
        }
        if (data.containsKey("logo")) {
            shop.setLogo((String) data.get("logo"));
        }
        if (data.containsKey("minPrice")) {
            shop.setMinPrice(new java.math.BigDecimal(data.get("minPrice").toString()));
        }
        if (data.containsKey("deliveryFee")) {
            shop.setDeliveryFee(new java.math.BigDecimal(data.get("deliveryFee").toString()));
        }
        if (data.containsKey("notice")) {
            shop.setNotice((String) data.get("notice"));
        }
        // 商家可修改自己的登录用户名和密码
        if (data.containsKey("username")) {
            String newUsername = (String) data.get("username");
            if (newUsername != null && !newUsername.trim().isEmpty()) {
                shop.setUsername(newUsername.trim());
            }
        }
        if (data.containsKey("password")) {
            String newPassword = (String) data.get("password");
            if (newPassword != null && !newPassword.trim().isEmpty()) {
                shop.setPassword(BCrypt.hashpw(newPassword, BCrypt.gensalt()));
            }
        }
        // 手动填写坐标时直接使用，不再 geocode
        if (data.containsKey("latitude") && data.get("latitude") != null) {
            shop.setLatitude(parseDouble(data.get("latitude")));
            hasManualCoords = true;
            needGeocode = false;
        }
        if (data.containsKey("longitude") && data.get("longitude") != null) {
            shop.setLongitude(parseDouble(data.get("longitude")));
            hasManualCoords = true;
            needGeocode = false;
        }
        if (needGeocode) {
            geocodeShop(shop);
        }
        shopMapper.updateById(shop);
        // Update GeoHash when coordinates changed
        if (hasManualCoords && shop.getLatitude() != null && shop.getLongitude() != null) {
            shop.setGeohash(GeoHashUtil.encode(shop.getLatitude(), shop.getLongitude(), 7));
            shopMapper.updateById(shop);
            log.info("Updated GeoHash for shop {} after manual coordinate change: {}", shopId, shop.getGeohash());
        }
        return Result.ok("保存成功");
    }

    @Override
    public Result<?> adminUpdate(Long id, Map<String, Object> data) {
        Shop shop = shopMapper.selectById(id);
        if (shop == null) {
            return Result.fail("店铺不存在");
        }
        boolean needGeocode = false;
        if (data.containsKey("name")) {
            shop.setName((String) data.get("name"));
        }
        if (data.containsKey("phone")) {
            shop.setPhone((String) data.get("phone"));
        }
        if (data.containsKey("email")) {
            shop.setEmail((String) data.get("email"));
        }
        if (data.containsKey("address")) {
            shop.setAddress((String) data.get("address"));
            needGeocode = true;
        }
        if (data.containsKey("description")) {
            shop.setDescription((String) data.get("description"));
        }
        if (data.containsKey("openTime")) {
            shop.setOpenTime((String) data.get("openTime"));
        }
        if (data.containsKey("closeTime")) {
            shop.setCloseTime((String) data.get("closeTime"));
        }
        if (data.containsKey("logo")) {
            shop.setLogo((String) data.get("logo"));
        }
        if (data.containsKey("notice")) {
            shop.setNotice((String) data.get("notice"));
        }
        if (data.containsKey("businessStatus")) {
            shop.setBusinessStatus(((Number) data.get("businessStatus")).intValue());
        }
        // 管理员可修改商家登录用户名和密码
        if (data.containsKey("username")) {
            String newUsername = (String) data.get("username");
            if (newUsername != null && !newUsername.trim().isEmpty()) {
                shop.setUsername(newUsername.trim());
            }
        }
        if (data.containsKey("password")) {
            String newPassword = (String) data.get("password");
            if (newPassword != null && !newPassword.trim().isEmpty()) {
                shop.setPassword(BCrypt.hashpw(newPassword, BCrypt.gensalt()));
            }
        }
        // 手动填写坐标时直接使用
        if (data.containsKey("latitude") && data.get("latitude") != null) {
            shop.setLatitude(parseDouble(data.get("latitude")));
            needGeocode = false;
        }
        if (data.containsKey("longitude") && data.get("longitude") != null) {
            shop.setLongitude(parseDouble(data.get("longitude")));
            needGeocode = false;
        }
        if (data.containsKey("status")) {
            shop.setStatus(((Number) data.get("status")).intValue());
        }
        if (data.containsKey("minPrice")) {
            shop.setMinPrice(new java.math.BigDecimal(data.get("minPrice").toString()));
        }
        if (data.containsKey("deliveryFee")) {
            shop.setDeliveryFee(new java.math.BigDecimal(data.get("deliveryFee").toString()));
        }
        if (needGeocode) {
            geocodeShop(shop);
        }
        // Recalculate GeoHash if coordinates exist
        if (shop.getLatitude() != null && shop.getLongitude() != null) {
            shop.setGeohash(GeoHashUtil.encode(shop.getLatitude(), shop.getLongitude(), 7));
        }
        shopMapper.updateById(shop);
        return Result.ok("更新成功");
    }

    @Override
    public Result<?> toggleBusinessStatus(Long shopId, Integer businessStatus) {
        Shop shop = shopMapper.selectById(shopId);
        if (shop == null) {
            return Result.fail("店铺不存在");
        }
        shop.setBusinessStatus(businessStatus);
        shopMapper.updateById(shop);
        return Result.ok(businessStatus == 1 ? "已切换为营业中" : "已切换为休息中");
    }

    @Override
    public Result<?> adminCreate(Map<String, Object> data) {
        Shop shop = new Shop();
        shop.setName((String) data.get("name"));
        shop.setPhone((String) data.get("phone"));
        shop.setEmail((String) data.getOrDefault("email", ""));
        shop.setAddress((String) data.getOrDefault("address", ""));
        shop.setDescription((String) data.getOrDefault("description", ""));

        // 商家登录凭证（必填，审核通过后商家可用此账号登录）
        String username = (String) data.get("username");
        String password = (String) data.get("password");
        if (username == null || username.trim().isEmpty()) {
            return Result.fail("商家登录用户名不能为空");
        }
        if (password == null || password.trim().isEmpty()) {
            return Result.fail("商家登录密码不能为空");
        }
        shop.setUsername(username.trim());
        shop.setPassword(BCrypt.hashpw(password, BCrypt.gensalt()));

        // 创建时默认状态为"待审核"，管理员需到审核页面审批
        shop.setStatus(data.containsKey("status") ? ((Number) data.get("status")).intValue() : 0);
        shop.setRating(BigDecimal.ZERO);
        if (data.containsKey("minPrice")) {
            shop.setMinPrice(new java.math.BigDecimal(data.get("minPrice").toString()));
        }
        if (data.containsKey("deliveryFee")) {
            shop.setDeliveryFee(new java.math.BigDecimal(data.get("deliveryFee").toString()));
        }
        // 手动传入的坐标优先，否则地理编码
        if (data.containsKey("latitude") && data.get("latitude") != null) {
            shop.setLatitude(parseDouble(data.get("latitude")));
        }
        if (data.containsKey("longitude") && data.get("longitude") != null) {
            shop.setLongitude(parseDouble(data.get("longitude")));
        }
        if (shop.getLatitude() == null || shop.getLongitude() == null) {
            geocodeShop(shop);
        }
        shopMapper.insert(shop);
        // 自动生成 shopNo: SHOP + id
        shop.setShopNo("SHOP" + shop.getId());
        shopMapper.updateById(shop);
        initDefaultCategories(shop.getId());
        return Result.ok("创建成功，请到审核页面审批通过后商家即可登录");
    }

    @Override
    public Result<?> adminDelete(Long id) {
        Shop shop = shopMapper.selectById(id);
        if (shop == null) {
            return Result.fail("店铺不存在");
        }
        shopMapper.deleteById(id);
        return Result.ok("删除成功");
    }

    private void initDefaultCategories(Long shopId) {
        String[][] defaults = {
            {"热销推荐", "1"},
            {"主食", "2"},
            {"小食", "3"},
            {"饮品", "4"}
        };
        for (String[] cat : defaults) {
            GoodsCategory category = new GoodsCategory();
            category.setShopId(shopId);
            category.setName(cat[0]);
            category.setSort(Integer.parseInt(cat[1]));
            category.setCreateTime(LocalDateTime.now());
            goodsCategoryMapper.insert(category);
        }
        log.info("Initialized default categories for shop {}", shopId);
    }

    private Double parseDouble(Object val) {
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).doubleValue();
        try { return Double.parseDouble(val.toString()); } catch (NumberFormatException e) { return null; }
    }

    private void geocodeShop(Shop shop) {
        String address = shop.getAddress();
        if (address == null || address.isEmpty()) return;
        try {
            double[] coords = tencentMapService.geocode(address);
            if (coords != null) {
                shop.setLongitude(coords[0]);
                shop.setLatitude(coords[1]);
            }
        } catch (Exception e) {
            log.warn("Geocode failed for shop id={}: {}", shop.getId(), e.getMessage());
        }
        // 地理编码后同步更新 GeoHash
        if (shop.getLatitude() != null && shop.getLongitude() != null) {
            shop.setGeohash(GeoHashUtil.encode(shop.getLatitude(), shop.getLongitude(), 7));
        }
    }

    @Override
    public void updateGeohash(Long shopId) {
        Shop shop = shopMapper.selectById(shopId);
        if (shop != null && shop.getLatitude() != null && shop.getLongitude() != null) {
            shop.setGeohash(GeoHashUtil.encode(shop.getLatitude(), shop.getLongitude(), 7));
            shopMapper.updateById(shop);
            log.debug("Updated GeoHash for shop {}: {}", shopId, shop.getGeohash());
        }
    }

    @Override
    public void recalculateShopRating(Long shopId) {
        Shop shop = shopMapper.selectById(shopId);
        if (shop == null) {
            log.warn("recalculateShopRating: shop not found, shopId={}", shopId);
            return;
        }

        // 查询该店铺的所有订单
        List<Order> shopOrders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getShopId, shopId)
                        .select(Order::getId));
        if (shopOrders.isEmpty()) {
            // 无订单 → 评分清零
            shop.setRating(BigDecimal.ZERO);
            shop.setUpdateTime(LocalDateTime.now());
            shopMapper.updateById(shop);
            log.info("Shop {} has no orders, rating set to 0", shopId);
            return;
        }

        List<Long> orderIds = shopOrders.stream().map(Order::getId).toList();
        // 查询所有有效评价（排除已撤销 status=1）
        List<Evaluation> shopEvals = evaluationMapper.selectList(
                new LambdaQueryWrapper<Evaluation>()
                        .in(Evaluation::getOrderId, orderIds)
                        .ne(Evaluation::getStatus, 1)
                        .isNotNull(Evaluation::getRating));

        if (shopEvals.isEmpty()) {
            shop.setRating(BigDecimal.ZERO);
        } else {
            int totalRating = shopEvals.stream().mapToInt(Evaluation::getRating).sum();
            BigDecimal avgRating = BigDecimal.valueOf(totalRating)
                    .divide(BigDecimal.valueOf(shopEvals.size()), 1, RoundingMode.HALF_UP);
            shop.setRating(avgRating);
        }
        shop.setUpdateTime(LocalDateTime.now());
        shopMapper.updateById(shop);

        // 清除店铺列表缓存，让新评分立即生效
        redisUtil.deleteByPattern("cache:shops:list:*");
        redisUtil.deleteByPattern("fallback:shops:list:*");

        log.info("Recalculated rating for shop {}: {} ({} evaluations)", shopId, shop.getRating(),
                shopEvals.isEmpty() ? 0 : shopEvals.size());
    }
}
