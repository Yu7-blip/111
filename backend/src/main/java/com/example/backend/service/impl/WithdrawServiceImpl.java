package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.common.PageResult;
import com.example.backend.common.Result;
import com.example.backend.entity.Delivery;
import com.example.backend.entity.Withdraw;
import com.example.backend.mapper.DeliveryMapper;
import com.example.backend.mapper.WithdrawMapper;
import com.example.backend.service.WithdrawService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WithdrawServiceImpl implements WithdrawService {

    private final WithdrawMapper withdrawMapper;
    private final DeliveryMapper deliveryMapper;

    private Delivery getDeliveryByUserId(Long userId) {
        LambdaQueryWrapper<Delivery> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Delivery::getUserId, userId);
        return deliveryMapper.selectOne(wrapper);
    }

    @Override
    @Transactional
    public Result<?> apply(Long userId) {
        Delivery delivery = getDeliveryByUserId(userId);
        if (delivery == null) {
            return Result.fail("骑手信息异常");
        }
        BigDecimal balance = delivery.getBalance() != null ? delivery.getBalance() : BigDecimal.ZERO;
        if (balance.compareTo(BigDecimal.ZERO) <= 0) {
            return Result.fail("余额不足，无法提现");
        }

        LambdaQueryWrapper<Withdraw> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Withdraw::getDeliveryId, delivery.getId());
        wrapper.eq(Withdraw::getStatus, 0);
        if (withdrawMapper.selectCount(wrapper) > 0) {
            return Result.fail("您有一笔提现正在处理中，请等待处理完成后再申请");
        }

        delivery.setBalance(BigDecimal.ZERO);
        delivery.setUpdateTime(LocalDateTime.now());
        deliveryMapper.updateById(delivery);

        Withdraw withdraw = new Withdraw();
        withdraw.setDeliveryId(delivery.getId());
        withdraw.setUserId(userId);
        withdraw.setAmount(balance);
        withdraw.setStatus(0);
        withdraw.setCreateTime(LocalDateTime.now());
        withdraw.setUpdateTime(LocalDateTime.now());
        withdrawMapper.insert(withdraw);

        Map<String, Object> result = new HashMap<>();
        result.put("id", withdraw.getId());
        result.put("amount", balance.toString());
        return Result.ok(result);
    }

    @Override
    public Result<?> list(Long userId, Integer page, Integer pageSize) {
        Delivery delivery = getDeliveryByUserId(userId);
        if (delivery == null) {
            return Result.ok(PageResult.of(Collections.emptyList(), 0, page, pageSize));
        }

        LambdaQueryWrapper<Withdraw> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Withdraw::getDeliveryId, delivery.getId());
        wrapper.orderByDesc(Withdraw::getCreateTime);

        Page<Withdraw> mpPage = new Page<>(page, pageSize);
        withdrawMapper.selectPage(mpPage, wrapper);

        List<Map<String, Object>> records = mpPage.getRecords().stream().map(w -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", w.getId());
            map.put("amount", w.getAmount().toString());
            map.put("status", w.getStatus());
            map.put("statusText", w.getStatus() == 0 ? "处理中" : w.getStatus() == 1 ? "已到账" : "已拒绝");
            map.put("remark", w.getRemark());
            map.put("createTime", w.getCreateTime());
            return map;
        }).collect(Collectors.toList());

        return Result.ok(PageResult.of(records, mpPage.getTotal(), page, pageSize));
    }

    @Override
    public Result<?> adminList(Integer page, Integer pageSize, Integer status) {
        LambdaQueryWrapper<Withdraw> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(Withdraw::getStatus, status);
        }
        wrapper.orderByDesc(Withdraw::getCreateTime);

        Page<Withdraw> mpPage = new Page<>(page, pageSize);
        withdrawMapper.selectPage(mpPage, wrapper);

        List<Map<String, Object>> records = mpPage.getRecords().stream().map(w -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", w.getId());
            Delivery delivery = deliveryMapper.selectById(w.getDeliveryId());
            map.put("deliveryName", delivery != null ? delivery.getName() : "");
            map.put("deliveryPhone", delivery != null ? delivery.getPhone() : "");
            map.put("amount", w.getAmount().toString());
            map.put("status", w.getStatus());
            map.put("remark", w.getRemark());
            map.put("createTime", w.getCreateTime());
            return map;
        }).collect(Collectors.toList());

        return Result.ok(PageResult.of(records, mpPage.getTotal(), page, pageSize));
    }

    @Override
    @Transactional
    public Result<?> adminProcess(Long id, Integer status, String remark) {
        Withdraw withdraw = withdrawMapper.selectById(id);
        if (withdraw == null) {
            return Result.fail("提现记录不存在");
        }
        if (withdraw.getStatus() != 0) {
            return Result.fail("该提现申请已处理");
        }

        withdraw.setStatus(status);
        withdraw.setRemark(remark);
        withdraw.setUpdateTime(LocalDateTime.now());
        withdrawMapper.updateById(withdraw);

        if (status == 2) {
            Delivery delivery = deliveryMapper.selectById(withdraw.getDeliveryId());
            if (delivery != null) {
                BigDecimal currentBalance = delivery.getBalance() != null ? delivery.getBalance() : BigDecimal.ZERO;
                delivery.setBalance(currentBalance.add(withdraw.getAmount()));
                delivery.setUpdateTime(LocalDateTime.now());
                deliveryMapper.updateById(delivery);
            }
        }

        return Result.ok(status == 1 ? "已通过提现申请" : "已拒绝提现申请，余额已退回");
    }
}
