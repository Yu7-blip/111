package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.common.PageResult;
import com.example.backend.common.Result;
import com.example.backend.common.ResultCode;
import com.example.backend.entity.FullReduceActivity;
import com.example.backend.mapper.FullReduceActivityMapper;
import com.example.backend.service.MarketingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MarketingServiceImpl implements MarketingService {

    private final FullReduceActivityMapper fullReduceActivityMapper;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public Result<?> activityList(Integer page, Integer pageSize, String name, Integer status) {
        LambdaQueryWrapper<FullReduceActivity> wrapper = new LambdaQueryWrapper<>();
        if (name != null && !name.isEmpty()) {
            wrapper.like(FullReduceActivity::getName, name);
        }
        if (status != null) {
            wrapper.eq(FullReduceActivity::getStatus, status);
        }
        wrapper.orderByDesc(FullReduceActivity::getCreateTime);

        Page<FullReduceActivity> activityPage = new Page<>(page, pageSize);
        fullReduceActivityMapper.selectPage(activityPage, wrapper);

        List<Map<String, Object>> records = activityPage.getRecords().stream().map(activity -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", activity.getId());
            map.put("name", activity.getName());
            map.put("conditionAmount", activity.getConditionAmount());
            map.put("reduceAmount", activity.getReduceAmount());
            map.put("startTime", activity.getStartTime());
            map.put("endTime", activity.getEndTime());
            map.put("status", activity.getStatus());
            map.put("createTime", activity.getCreateTime());
            return map;
        }).collect(Collectors.toList());

        return Result.ok(PageResult.of(records, activityPage.getTotal(), page, pageSize));
    }

    @Override
    @Transactional
    public Result<?> activityCreate(Map<String, Object> data) {
        FullReduceActivity activity = new FullReduceActivity();

        if (data.containsKey("name") && data.get("name") != null) {
            activity.setName(data.get("name").toString());
        }
        if (data.containsKey("conditionAmount") && data.get("conditionAmount") != null) {
            activity.setConditionAmount(new BigDecimal(data.get("conditionAmount").toString()));
        }
        if (data.containsKey("reduceAmount") && data.get("reduceAmount") != null) {
            activity.setReduceAmount(new BigDecimal(data.get("reduceAmount").toString()));
        }
        if (data.containsKey("startTime") && data.get("startTime") != null) {
            String startStr = data.get("startTime").toString();
            if (startStr.contains("T")) {
                startStr = startStr.replace("T", " ");
            }
            activity.setStartTime(LocalDateTime.parse(startStr, DATE_TIME_FORMATTER));
        }
        if (data.containsKey("endTime") && data.get("endTime") != null) {
            String endStr = data.get("endTime").toString();
            if (endStr.contains("T")) {
                endStr = endStr.replace("T", " ");
            }
            activity.setEndTime(LocalDateTime.parse(endStr, DATE_TIME_FORMATTER));
        }
        if (data.containsKey("status") && data.get("status") != null) {
            activity.setStatus(Integer.parseInt(data.get("status").toString()));
        } else {
            activity.setStatus(1);
        }

        activity.setCreateTime(LocalDateTime.now());
        activity.setUpdateTime(LocalDateTime.now());
        fullReduceActivityMapper.insert(activity);
        return Result.ok("创建成功");
    }

    @Override
    @Transactional
    public Result<?> activityUpdate(Long id, Map<String, Object> data) {
        FullReduceActivity activity = fullReduceActivityMapper.selectById(id);
        if (activity == null) {
            return Result.fail(ResultCode.NOT_FOUND);
        }

        if (data.containsKey("name") && data.get("name") != null) {
            activity.setName(data.get("name").toString());
        }
        if (data.containsKey("conditionAmount") && data.get("conditionAmount") != null) {
            activity.setConditionAmount(new BigDecimal(data.get("conditionAmount").toString()));
        }
        if (data.containsKey("reduceAmount") && data.get("reduceAmount") != null) {
            activity.setReduceAmount(new BigDecimal(data.get("reduceAmount").toString()));
        }
        if (data.containsKey("startTime") && data.get("startTime") != null) {
            String startStr = data.get("startTime").toString();
            if (startStr.contains("T")) {
                startStr = startStr.replace("T", " ");
            }
            activity.setStartTime(LocalDateTime.parse(startStr, DATE_TIME_FORMATTER));
        }
        if (data.containsKey("endTime") && data.get("endTime") != null) {
            String endStr = data.get("endTime").toString();
            if (endStr.contains("T")) {
                endStr = endStr.replace("T", " ");
            }
            activity.setEndTime(LocalDateTime.parse(endStr, DATE_TIME_FORMATTER));
        }
        if (data.containsKey("status") && data.get("status") != null) {
            activity.setStatus(Integer.parseInt(data.get("status").toString()));
        }

        activity.setUpdateTime(LocalDateTime.now());
        fullReduceActivityMapper.updateById(activity);
        return Result.ok("更新成功");
    }

    @Override
    @Transactional
    public Result<?> activityDelete(Long id) {
        FullReduceActivity activity = fullReduceActivityMapper.selectById(id);
        if (activity == null) {
            return Result.fail(ResultCode.NOT_FOUND);
        }
        fullReduceActivityMapper.deleteById(id);
        return Result.ok("删除成功");
    }

    @Override
    @Transactional
    public Result<?> activityUpdateStatus(Long id, Integer status) {
        FullReduceActivity activity = fullReduceActivityMapper.selectById(id);
        if (activity == null) {
            return Result.fail(ResultCode.NOT_FOUND);
        }
        activity.setStatus(status);
        activity.setUpdateTime(LocalDateTime.now());
        fullReduceActivityMapper.updateById(activity);
        return Result.ok("操作成功");
    }

    @Override
    public FullReduceActivity getBestActivity(Long shopId, BigDecimal orderAmount) {
        LocalDateTime now = LocalDateTime.now();

        // 查询进行中且在有效期内的活动：全平台活动(shop_id IS NULL) 或 本店活动(shop_id = ?)
        LambdaQueryWrapper<FullReduceActivity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FullReduceActivity::getStatus, 1)                    // 进行中
                .le(FullReduceActivity::getStartTime, now)              // 已开始
                .ge(FullReduceActivity::getEndTime, now)                // 未结束
                .le(FullReduceActivity::getConditionAmount, orderAmount) // 满足门槛
                .and(w -> w.isNull(FullReduceActivity::getShopId)
                         .or()
                         .eq(FullReduceActivity::getShopId, shopId))
                .orderByDesc(FullReduceActivity::getReduceAmount)       // 取减免金额最大的
                .last("LIMIT 1");

        return fullReduceActivityMapper.selectOne(wrapper);
    }
}
