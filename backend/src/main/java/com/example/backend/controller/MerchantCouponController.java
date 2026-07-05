package com.example.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.common.Result;
import com.example.backend.entity.Coupon;
import com.example.backend.mapper.CouponMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/merchant/coupons")
@RequiredArgsConstructor
public class MerchantCouponController {

    private final CouponMapper couponMapper;

    @GetMapping
    public Result<?> list(@RequestAttribute("userId") Long shopId) {
        LambdaQueryWrapper<Coupon> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Coupon::getShopId, shopId);
        wrapper.orderByDesc(Coupon::getCreateTime);
        return Result.ok(couponMapper.selectList(wrapper));
    }

    @PostMapping
    public Result<?> create(@RequestAttribute("userId") Long shopId,
                            @RequestBody Map<String, Object> data) {
        Coupon coupon = new Coupon();
        coupon.setName((String) data.get("name"));
        coupon.setConditionAmount(new BigDecimal(data.get("conditionAmount").toString()));
        coupon.setReduceAmount(new BigDecimal(data.get("reduceAmount").toString()));
        coupon.setTotalCount(data.get("totalCount") != null ? ((Number) data.get("totalCount")).intValue() : 100);
        coupon.setRemainCount(data.get("remainCount") != null ? ((Number) data.get("remainCount")).intValue() : 100);
        String startTimeStr = data.get("startTime") != null ? data.get("startTime").toString() : "";
        String endTimeStr = data.get("endTime") != null ? data.get("endTime").toString() : "";
        if (!startTimeStr.isEmpty()) {
            coupon.setStartTime(parseDateTime(startTimeStr));
        }
        if (!endTimeStr.isEmpty()) {
            coupon.setEndTime(parseDateTime(endTimeStr));
        }
        coupon.setStatus(data.get("status") != null ? ((Number) data.get("status")).intValue() : 1);
        coupon.setShopId(shopId);
        coupon.setCreateTime(LocalDateTime.now());
        couponMapper.insert(coupon);
        return Result.ok("创建成功");
    }

    private LocalDateTime parseDateTime(String s) {
        try {
            String clean = s.replace("T", " ").trim();
            if (clean.length() >= 19) {
                return LocalDateTime.parse(clean.substring(0, 19).replace(" ", "T"));
            }
            return LocalDateTime.parse(clean.replace(" ", "T"));
        } catch (Exception e) {
            return null;
        }
    }

    @PutMapping("/{id}")
    public Result<?> update(@RequestAttribute("userId") Long shopId,
                            @PathVariable Long id,
                            @RequestBody Map<String, Object> data) {
        Coupon coupon = couponMapper.selectById(id);
        if (coupon == null || !coupon.getShopId().equals(shopId)) {
            return Result.fail("优惠券不存在");
        }
        if (data.containsKey("name")) coupon.setName((String) data.get("name"));
        if (data.containsKey("conditionAmount")) coupon.setConditionAmount(new BigDecimal(data.get("conditionAmount").toString()));
        if (data.containsKey("reduceAmount")) coupon.setReduceAmount(new BigDecimal(data.get("reduceAmount").toString()));
        if (data.containsKey("totalCount")) coupon.setTotalCount(((Number) data.get("totalCount")).intValue());
        if (data.containsKey("remainCount")) coupon.setRemainCount(((Number) data.get("remainCount")).intValue());
        if (data.containsKey("status")) coupon.setStatus(((Number) data.get("status")).intValue());
        if (data.containsKey("startTime") && data.get("startTime") != null
                && !data.get("startTime").toString().isEmpty()) {
            coupon.setStartTime(parseDateTime(data.get("startTime").toString()));
        }
        if (data.containsKey("endTime") && data.get("endTime") != null
                && !data.get("endTime").toString().isEmpty()) {
            coupon.setEndTime(parseDateTime(data.get("endTime").toString()));
        }
        couponMapper.updateById(coupon);
        return Result.ok("更新成功");
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@RequestAttribute("userId") Long shopId,
                            @PathVariable Long id) {
        Coupon coupon = couponMapper.selectById(id);
        if (coupon == null || !coupon.getShopId().equals(shopId)) {
            return Result.fail("优惠券不存在");
        }
        couponMapper.deleteById(id);
        return Result.ok("删除成功");
    }
}
