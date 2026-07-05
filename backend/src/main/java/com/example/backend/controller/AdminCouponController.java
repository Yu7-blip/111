package com.example.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.common.Result;
import com.example.backend.entity.Coupon;
import com.example.backend.mapper.CouponMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/coupons")
@RequiredArgsConstructor
public class AdminCouponController {

    private final CouponMapper couponMapper;

    @GetMapping
    public Result<?> list() {
        LambdaQueryWrapper<Coupon> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNull(Coupon::getShopId);
        wrapper.orderByDesc(Coupon::getCreateTime);
        return Result.ok(couponMapper.selectList(wrapper));
    }

    @PostMapping
    public Result<?> create(@RequestBody Map<String, Object> data) {
        Coupon coupon = new Coupon();
        coupon.setName((String) data.get("name"));
        coupon.setConditionAmount(new BigDecimal(data.get("conditionAmount").toString()));
        coupon.setReduceAmount(new BigDecimal(data.get("reduceAmount").toString()));
        coupon.setTotalCount(data.get("totalCount") != null ? ((Number) data.get("totalCount")).intValue() : 500);
        coupon.setRemainCount(data.get("remainCount") != null ? ((Number) data.get("remainCount")).intValue() : 500);
        coupon.setStatus(data.get("status") != null ? ((Number) data.get("status")).intValue() : 1);
        if (data.get("startTime") != null && !data.get("startTime").toString().isEmpty()) {
            coupon.setStartTime(LocalDateTime.parse(data.get("startTime").toString().replace("T", " ").substring(0, 19).replace(" ", "T")));
        }
        if (data.get("endTime") != null && !data.get("endTime").toString().isEmpty()) {
            coupon.setEndTime(LocalDateTime.parse(data.get("endTime").toString().replace("T", " ").substring(0, 19).replace(" ", "T")));
        }
        coupon.setCreateTime(LocalDateTime.now());
        couponMapper.insert(coupon);
        return Result.ok("创建成功");
    }

    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        Coupon coupon = couponMapper.selectById(id);
        if (coupon == null) return Result.fail("优惠券不存在");
        if (data.containsKey("name")) coupon.setName((String) data.get("name"));
        if (data.containsKey("conditionAmount")) coupon.setConditionAmount(new BigDecimal(data.get("conditionAmount").toString()));
        if (data.containsKey("reduceAmount")) coupon.setReduceAmount(new BigDecimal(data.get("reduceAmount").toString()));
        if (data.containsKey("totalCount")) coupon.setTotalCount(((Number) data.get("totalCount")).intValue());
        if (data.containsKey("remainCount")) coupon.setRemainCount(((Number) data.get("remainCount")).intValue());
        if (data.containsKey("status")) coupon.setStatus(((Number) data.get("status")).intValue());
        if (data.containsKey("startTime") && data.get("startTime") != null
                && !data.get("startTime").toString().isEmpty()) {
            coupon.setStartTime(LocalDateTime.parse(data.get("startTime").toString().replace("T", " ").substring(0, 19).replace(" ", "T")));
        }
        if (data.containsKey("endTime") && data.get("endTime") != null
                && !data.get("endTime").toString().isEmpty()) {
            coupon.setEndTime(LocalDateTime.parse(data.get("endTime").toString().replace("T", " ").substring(0, 19).replace(" ", "T")));
        }
        couponMapper.updateById(coupon);
        return Result.ok("更新成功");
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        couponMapper.deleteById(id);
        return Result.ok("删除成功");
    }
}
