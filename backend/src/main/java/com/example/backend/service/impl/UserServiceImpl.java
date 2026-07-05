package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.common.PageResult;
import com.example.backend.common.Result;
import com.example.backend.common.ResultCode;
import com.example.backend.entity.User;
import com.example.backend.mapper.UserMapper;
import com.example.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    @Override
    public Result<?> list(Integer page, Integer pageSize, String username, String phone, Integer status) {
        Page<User> pageObj = new Page<>(page, pageSize);

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getRole, "user");  // 默认只查普通用户，也可通过 status 等参数查全部

        if (StringUtils.hasText(username)) {
            wrapper.like(User::getNickname, username);
        }
        if (StringUtils.hasText(phone)) {
            wrapper.like(User::getPhone, phone);
        }
        if (status != null) {
            wrapper.eq(User::getStatus, status);
        }
        wrapper.orderByDesc(User::getCreateTime);

        userMapper.selectPage(pageObj, wrapper);

        List<Map<String, Object>> records = pageObj.getRecords().stream().map(user -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", user.getId());
            map.put("username", user.getNickname());
            map.put("nickname", user.getNickname());
            map.put("phone", user.getPhone());
            map.put("role", user.getRole());
            map.put("avatar", user.getAvatar());
            map.put("status", user.getStatus());
            map.put("createTime", user.getCreateTime());
            return map;
        }).collect(Collectors.toList());

        PageResult<Map<String, Object>> pageResult = PageResult.of(
                records,
                pageObj.getTotal(),
                pageObj.getCurrent(),
                pageObj.getSize()
        );

        return Result.ok(pageResult);
    }

    @Override
    public Result<?> create(Map<String, Object> data) {
        User user = new User();
        user.setPhone((String) data.get("phone"));
        user.setNickname((String) data.get("nickname"));
        user.setRole("user");
        user.setStatus(data.get("status") != null ? ((Number) data.get("status")).intValue() : 1);
        user.setCreateTime(LocalDateTime.now());
        userMapper.insert(user);

        Map<String, String> result = new HashMap<>();
        result.put("message", "创建成功");
        return Result.ok(result);
    }

    @Override
    public Result<?> update(Long id, Map<String, Object> data) {
        User user = userMapper.selectById(id);
        if (user == null) {
            return Result.fail(ResultCode.NOT_FOUND);
        }

        if (data.containsKey("nickname")) {
            user.setNickname((String) data.get("nickname"));
        }
        if (data.containsKey("phone")) {
            user.setPhone((String) data.get("phone"));
        }
        if (data.containsKey("avatar")) {
            user.setAvatar((String) data.get("avatar"));
        }
        if (data.containsKey("status")) {
            user.setStatus(((Number) data.get("status")).intValue());
        }
        if (data.containsKey("role")) {
            user.setRole((String) data.get("role"));
        }
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);

        Map<String, String> result = new HashMap<>();
        result.put("message", "更新成功");
        return Result.ok(result);
    }

    @Override
    public Result<?> delete(Long id) {
        userMapper.deleteById(id);
        Map<String, String> result = new HashMap<>();
        result.put("message", "删除成功");
        return Result.ok(result);
    }

    @Override
    public Result<?> updateStatus(Long id, Integer status) {
        User user = userMapper.selectById(id);
        if (user == null) {
            return Result.fail(ResultCode.NOT_FOUND);
        }
        user.setStatus(status);
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);

        Map<String, String> result = new HashMap<>();
        result.put("message", "操作成功");
        return Result.ok(result);
    }
}
