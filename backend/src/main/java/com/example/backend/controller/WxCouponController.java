package com.example.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.common.Result;
import com.example.backend.entity.Coupon;
import com.example.backend.entity.UserCoupon;
import com.example.backend.mapper.CouponMapper;
import com.example.backend.mapper.UserCouponMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/wx/coupons")
@RequiredArgsConstructor
public class WxCouponController {

    private final CouponMapper couponMapper;
    private final UserCouponMapper userCouponMapper;

    /**
     * List user's available (unused) coupons with coupon details.
     */
    @GetMapping
    public Result<?> list(@RequestAttribute("userId") Long userId,
                          @RequestParam(required = false) BigDecimal orderAmount,
                          @RequestParam(required = false) Long shopId) {
        LambdaQueryWrapper<UserCoupon> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserCoupon::getUserId, userId);
        wrapper.eq(UserCoupon::getStatus, 0);
        List<UserCoupon> userCoupons = userCouponMapper.selectList(wrapper);

        List<Map<String, Object>> result = new ArrayList<>();
        for (UserCoupon uc : userCoupons) {
            Coupon coupon = couponMapper.selectById(uc.getCouponId());
            if (coupon == null || coupon.getStatus() != 1) continue;
            if (coupon.getStartTime() != null && coupon.getStartTime().isAfter(LocalDateTime.now())) continue;
            if (coupon.getEndTime() != null && coupon.getEndTime().isBefore(LocalDateTime.now())) continue;
            // Filter: only platform coupons or coupons belonging to this shop
            if (shopId != null && coupon.getShopId() != null && !coupon.getShopId().equals(shopId)) continue;

            Map<String, Object> map = new HashMap<>();
            map.put("id", uc.getId());
            map.put("couponId", coupon.getId());
            map.put("name", coupon.getName());
            map.put("conditionAmount", coupon.getConditionAmount());
            map.put("reduceAmount", coupon.getReduceAmount());
            map.put("endTime", coupon.getEndTime());

            boolean usable = orderAmount == null ||
                    orderAmount.compareTo(coupon.getConditionAmount()) >= 0;
            map.put("usable", usable);
            if (!usable) {
                map.put("tip", "满" + coupon.getConditionAmount() + "元可用");
            }
            result.add(map);
        }
        return Result.ok(result);
    }

    /**
     * List available coupons the user can claim (platform-wide + shop-specific).
     */
    @GetMapping("/available")
    public Result<?> available(@RequestAttribute("userId") Long userId,
                               @RequestParam(required = false) Long shopId) {
        LambdaQueryWrapper<Coupon> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Coupon::getStatus, 1);
        // shop page: platform coupons OR this shop's coupons
        // 领券中心 (no shopId): only platform-wide coupons
        if (shopId != null) {
            wrapper.and(w -> w.isNull(Coupon::getShopId).or().eq(Coupon::getShopId, shopId));
        } else {
            wrapper.isNull(Coupon::getShopId);
        }
        wrapper.orderByDesc(Coupon::getCreateTime);
        List<Coupon> coupons = couponMapper.selectList(wrapper);

        // Filter out already-claimed ones
        LambdaQueryWrapper<UserCoupon> ucWrapper = new LambdaQueryWrapper<>();
        ucWrapper.eq(UserCoupon::getUserId, userId);
        List<UserCoupon> claimed = userCouponMapper.selectList(ucWrapper);
        Set<Long> claimedIds = claimed.stream().map(UserCoupon::getCouponId).collect(Collectors.toSet());

        List<Map<String, Object>> result = new ArrayList<>();
        for (Coupon c : coupons) {
            if (claimedIds.contains(c.getId())) continue;
            if (c.getRemainCount() != null && c.getRemainCount() <= 0) continue;
            if (c.getStartTime() != null && c.getStartTime().isAfter(LocalDateTime.now())) continue;
            if (c.getEndTime() != null && c.getEndTime().isBefore(LocalDateTime.now())) continue;

            Map<String, Object> map = new HashMap<>();
            map.put("id", c.getId());
            map.put("name", c.getName());
            map.put("conditionAmount", c.getConditionAmount());
            map.put("reduceAmount", c.getReduceAmount());
            map.put("endTime", c.getEndTime());
            map.put("shopId", c.getShopId());
            map.put("shopType", c.getShopId() == null ? "平台券" : "商家券");
            result.add(map);
        }
        return Result.ok(result);
    }

    /**
     * Claim a coupon.
     */
    @PostMapping("/{couponId}/receive")
    public Result<?> receive(@RequestAttribute("userId") Long userId,
                             @PathVariable Long couponId) {
        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null || coupon.getStatus() != 1) {
            return Result.fail("优惠券不可用");
        }
        if (coupon.getRemainCount() != null && coupon.getRemainCount() <= 0) {
            return Result.fail("优惠券已领完");
        }

        LambdaQueryWrapper<UserCoupon> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserCoupon::getUserId, userId);
        wrapper.eq(UserCoupon::getCouponId, couponId);
        if (userCouponMapper.selectCount(wrapper) > 0) {
            return Result.fail("已领取过该优惠券");
        }

        if (coupon.getRemainCount() != null) {
            coupon.setRemainCount(coupon.getRemainCount() - 1);
            couponMapper.updateById(coupon);
        }

        UserCoupon uc = new UserCoupon();
        uc.setUserId(userId);
        uc.setCouponId(couponId);
        uc.setStatus(0);
        uc.setCreateTime(LocalDateTime.now());
        userCouponMapper.insert(uc);

        return Result.ok("领取成功");
    }
}
