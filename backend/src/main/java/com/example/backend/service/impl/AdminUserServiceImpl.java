package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.common.PageResult;
import com.example.backend.common.Result;
import com.example.backend.entity.Admin;
import com.example.backend.mapper.AdminMapper;
import com.example.backend.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import cn.hutool.crypto.digest.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final AdminMapper adminMapper;

    @Override
    public Result<?> list(Integer page, Integer pageSize, String username, String role, Integer status) {
        LambdaQueryWrapper<Admin> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(username)) {
            wrapper.like(Admin::getUsername, username);
        }
        if (StringUtils.hasText(role)) {
            wrapper.eq(Admin::getRole, role);
        }
        if (status != null) {
            wrapper.eq(Admin::getStatus, status);
        }
        wrapper.orderByAsc(Admin::getId);

        Page<Admin> mpPage = new Page<>(page, pageSize);
        adminMapper.selectPage(mpPage, wrapper);

        // 不返回密码
        List<Map<String, Object>> records = new ArrayList<>();
        for (Admin admin : mpPage.getRecords()) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", admin.getId());
            map.put("username", admin.getUsername());
            map.put("name", admin.getName());
            map.put("role", admin.getRole());
            map.put("phone", admin.getPhone());
            map.put("status", admin.getStatus());
            map.put("createTime", admin.getCreateTime());
            records.add(map);
        }

        return Result.ok(PageResult.of(records, mpPage.getTotal(), mpPage.getCurrent(), mpPage.getSize()));
    }

    @Override
    public Result<?> detail(Long id) {
        Admin admin = adminMapper.selectById(id);
        if (admin == null) return Result.fail("管理员不存在");
        admin.setPassword(null); // 不泄露密码
        return Result.ok(admin);
    }

    @Override
    public Result<?> create(Map<String, Object> data) {
        String username = (String) data.get("username");
        String password = (String) data.get("password");
        String name = (String) data.get("name");
        String role = (String) data.getOrDefault("role", "operator");
        String phone = (String) data.get("phone");

        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            return Result.fail("用户名和密码不能为空");
        }

        // 检查用户名唯一
        Long count = adminMapper.selectCount(
                new LambdaQueryWrapper<Admin>().eq(Admin::getUsername, username));
        if (count != null && count > 0) {
            return Result.fail("用户名已存在");
        }

        Admin admin = new Admin();
        admin.setUsername(username.trim());
        admin.setPassword(BCrypt.hashpw(password, BCrypt.gensalt()));
        admin.setName(StringUtils.hasText(name) ? name : username);
        admin.setRole("admin".equals(role) ? "admin" : "operator");
        admin.setPhone(phone);
        admin.setStatus(1);
        admin.setCreateTime(LocalDateTime.now());
        admin.setUpdateTime(LocalDateTime.now());
        adminMapper.insert(admin);

        admin.setPassword(null);
        return Result.ok(admin);
    }

    @Override
    public Result<?> update(Long id, Map<String, Object> data) {
        Admin admin = adminMapper.selectById(id);
        if (admin == null) return Result.fail("管理员不存在");

        if (data.containsKey("name")) {
            admin.setName((String) data.get("name"));
        }
        if (data.containsKey("phone")) {
            admin.setPhone((String) data.get("phone"));
        }
        if (data.containsKey("role")) {
            String role = (String) data.get("role");
            admin.setRole("admin".equals(role) ? "admin" : "operator");
        }
        if (data.containsKey("password")) {
            String newPwd = (String) data.get("password");
            if (StringUtils.hasText(newPwd)) {
                admin.setPassword(BCrypt.hashpw(newPwd, BCrypt.gensalt()));
            }
        }
        admin.setUpdateTime(LocalDateTime.now());
        adminMapper.updateById(admin);

        return Result.ok("更新成功");
    }

    @Override
    public Result<?> delete(Long id) {
        Admin admin = adminMapper.selectById(id);
        if (admin == null) return Result.fail("管理员不存在");

        // 不允许删除最后一个 admin 角色用户
        if ("admin".equals(admin.getRole())) {
            Long adminCount = adminMapper.selectCount(
                    new LambdaQueryWrapper<Admin>().eq(Admin::getRole, "admin").eq(Admin::getStatus, 1));
            if (adminCount != null && adminCount <= 1) {
                return Result.fail("不能删除最后一个超级管理员");
            }
        }

        adminMapper.deleteById(id);
        return Result.ok("删除成功");
    }

    @Override
    public Result<?> updateStatus(Long id, Integer status) {
        Admin admin = adminMapper.selectById(id);
        if (admin == null) return Result.fail("管理员不存在");

        if (status == 0 && "admin".equals(admin.getRole())) {
            Long activeAdminCount = adminMapper.selectCount(
                    new LambdaQueryWrapper<Admin>().eq(Admin::getRole, "admin").eq(Admin::getStatus, 1));
            if (activeAdminCount != null && activeAdminCount <= 1) {
                return Result.fail("不能禁用最后一个超级管理员");
            }
        }

        admin.setStatus(status);
        admin.setUpdateTime(LocalDateTime.now());
        adminMapper.updateById(admin);
        return Result.ok(status == 1 ? "已启用" : "已禁用");
    }
}
