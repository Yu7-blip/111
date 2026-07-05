package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.common.PageResult;
import com.example.backend.common.Result;
import com.example.backend.common.ResultCode;
import com.example.backend.dto.request.LoginRequest;
import com.example.backend.dto.request.RegisterRequest;
import com.example.backend.dto.response.LoginResponse;
import com.example.backend.entity.*;
import com.example.backend.mapper.*;
import com.example.backend.service.AuthService;
import com.example.backend.utils.JwtUtil;
import com.example.backend.utils.RandomUtil;
import com.example.backend.utils.RedisUtil;
import cn.hutool.crypto.digest.BCrypt;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AdminMapper adminMapper;
    private final ShopMapper shopMapper;
    private final UserMapper userMapper;
    private final DeliveryMapper deliveryMapper;
    private final JwtUtil jwtUtil;
    private final RedisUtil redisUtil;
    private final EvaluationMapper evaluationMapper;
    private final OrderMapper orderMapper;
    private final FeedbackMapper feedbackMapper;

    @Override
    public Result<?> adminLogin(LoginRequest request) {
        LambdaQueryWrapper<Admin> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Admin::getUsername, request.getUsername());
        Admin admin = adminMapper.selectOne(wrapper);

        if (admin == null) {
            return Result.fail(ResultCode.LOGIN_FAILED);
        }

        // BCrypt 密码校验 + 明文密码自动升级
        boolean passwordOk;
        try {
            passwordOk = BCrypt.checkpw(request.getPassword(), admin.getPassword());
            if (!passwordOk && request.getPassword().equals(admin.getPassword())) {
                // 明文密码匹配 → 自动升级为 BCrypt
                admin.setPassword(BCrypt.hashpw(request.getPassword(), BCrypt.gensalt()));
                adminMapper.updateById(admin);
                passwordOk = true;
                log.info("Upgraded admin {} password to BCrypt", admin.getUsername());
            }
        } catch (Exception e) {
            passwordOk = false;
        }
        if (!passwordOk) {
            return Result.fail(ResultCode.LOGIN_FAILED);
        }

        if (admin.getStatus() != 1) {
            return Result.fail(ResultCode.ACCOUNT_DISABLED);
        }

        String token = jwtUtil.generateToken(admin.getId(), admin.getRole());

        Map<String, Object> user = new HashMap<>();
        user.put("id", admin.getId());
        user.put("username", admin.getUsername());
        user.put("name", admin.getName());
        user.put("role", admin.getRole());
        user.put("avatar", admin.getAvatar());
        user.put("phone", admin.getPhone());

        LoginResponse response = LoginResponse.builder()
                .token(token)
                .user(user)
                .build();

        return Result.ok(response);
    }

    @Override
    public Result<?> merchantLogin(LoginRequest request) {
        // 先按 username 查，查不到再按店铺名称查（支持店名登录）
        LambdaQueryWrapper<Shop> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(Shop::getUsername, request.getUsername())
                         .or()
                         .eq(Shop::getName, request.getUsername()));
        wrapper.ne(Shop::getStatus, 2);
        wrapper.last("LIMIT 1");  // 确保只返回一条，避免username和name各自都匹配时出问题
        Shop shop = shopMapper.selectOne(wrapper);

        if (shop == null) {
            return Result.fail(ResultCode.LOGIN_FAILED);
        }

        // BCrypt 密码校验 + 明文密码自动升级
        boolean passwordOk;
        if (shop.getPassword() != null && shop.getPassword().startsWith("$2a$")) {
            passwordOk = BCrypt.checkpw(request.getPassword(), shop.getPassword());
        } else {
            // 明文密码直接对比，匹配后自动升级为 BCrypt
            passwordOk = request.getPassword().equals(shop.getPassword());
            if (passwordOk) {
                shop.setPassword(BCrypt.hashpw(request.getPassword(), BCrypt.gensalt()));
                shopMapper.updateById(shop);
                log.info("Upgraded merchant {} password to BCrypt", shop.getUsername());
            }
        }
        if (!passwordOk) {
            return Result.fail(ResultCode.LOGIN_FAILED);
        }

        if (shop.getStatus() != 1) {
            return Result.fail(ResultCode.MERCHANT_NOT_APPROVED);
        }

        String token = jwtUtil.generateToken(shop.getId(), "merchant");

        Map<String, Object> user = new HashMap<>();
        user.put("id", shop.getId());
        user.put("username", shop.getUsername());
        user.put("shopName", shop.getName());
        user.put("phone", shop.getPhone());
        user.put("email", shop.getEmail());

        LoginResponse response = LoginResponse.builder()
                .token(token)
                .user(user)
                .build();

        return Result.ok(response);
    }

    @Override
    public Result<?> wxLogin(RegisterRequest request) {
        String reqRole = request.getRole() != null ? request.getRole() : "user";

        // Find user by phone AND role (same phone can have user + delivery accounts)
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getPhone, request.getPhone());
        wrapper.eq(User::getRole, reqRole);
        User user = userMapper.selectOne(wrapper);

        // Password login mode
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            if (user == null) {
                return Result.fail(ResultCode.LOGIN_FAILED);
            }
            if (!request.getPassword().equals(user.getPassword())) {
                if (user.getPassword() == null) {
                    user.setPassword(request.getPassword());
                    userMapper.updateById(user);
                } else {
                    return Result.fail(ResultCode.LOGIN_FAILED);
                }
            }
        } else {
            // SMS code login mode
            String cachedCode = redisUtil.getString("sms:" + request.getPhone());
            if (cachedCode == null || !cachedCode.equals(request.getCode())) {
                return Result.fail(ResultCode.PHONE_CODE_ERROR);
            }
            redisUtil.delete("sms:" + request.getPhone());

            // If not exists for this role, auto-create
            if (user == null) {
                user = new User();
                user.setPhone(request.getPhone());
                user.setNickname(request.getNickname() != null ? request.getNickname() : request.getPhone());
                user.setPassword("123456");
                user.setRole(reqRole);
                user.setStatus(1);
                user.setCreateTime(LocalDateTime.now());
                userMapper.insert(user);
            }
        }

        // If user registered as delivery, ensure Delivery record exists
        if ("delivery".equals(user.getRole())) {
            LambdaQueryWrapper<Delivery> deliveryWrapper = new LambdaQueryWrapper<>();
            deliveryWrapper.eq(Delivery::getUserId, user.getId());
            if (deliveryMapper.selectCount(deliveryWrapper) == 0) {
                Delivery delivery = new Delivery();
                delivery.setUserId(user.getId());
                delivery.setName(user.getNickname() != null ? user.getNickname() : user.getPhone());
                delivery.setPhone(user.getPhone());
                delivery.setStatus(0);
                delivery.setBalance(new java.math.BigDecimal("0.00"));
                delivery.setOnTimeRate(new java.math.BigDecimal("100.00"));
                delivery.setPraiseRate(new java.math.BigDecimal("100.00"));
                delivery.setTotalDeliveries(0);
                delivery.setCreateTime(LocalDateTime.now());
                delivery.setUpdateTime(LocalDateTime.now());
                deliveryMapper.insert(delivery);
            }
        }

        // Generate JWT token
        String token = jwtUtil.generateToken(user.getId(), user.getRole());

        // Build LoginResponse
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", user.getId());
        userMap.put("phone", user.getPhone());
        userMap.put("nickname", user.getNickname());
        userMap.put("role", user.getRole());
        userMap.put("avatar", user.getAvatar());

        LoginResponse response = LoginResponse.builder()
                .token(token)
                .user(userMap)
                .build();

        return Result.ok(response);
    }

    @Override
    public Result<?> wxSendCode(String phone) {
        String code = RandomUtil.generateCode(6);
        redisUtil.set("sms:" + phone, code, 5, TimeUnit.MINUTES);
        log.info("=== 验证码已发送: phone={}, code={} ===", phone, code);
        return Result.ok(code);
    }

    @Override
    public Result<?> merchantUserInfo(Long shopId) {
        Shop shop = shopMapper.selectById(shopId);
        if (shop == null) {
            return Result.fail(ResultCode.NOT_FOUND);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("id", shop.getId());
        data.put("username", shop.getUsername());
        data.put("shopName", shop.getName());
        data.put("phone", shop.getPhone());
        data.put("email", shop.getEmail());
        data.put("createTime", shop.getCreateTime());

        return Result.ok(data);
    }

    @Override
    public Result<?> merchantEvaluations(Long shopId, Integer page, Integer pageSize) {
        // Get all order IDs for this shop
        java.util.List<Long> orderIds = new java.util.ArrayList<>();
        LambdaQueryWrapper<Order> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.eq(Order::getShopId, shopId);
        java.util.List<Order> orders = orderMapper.selectList(orderWrapper);
        for (Order o : orders) orderIds.add(o.getId());

        if (orderIds.isEmpty()) return Result.ok(PageResult.of(java.util.Collections.emptyList(), 0, page, pageSize));

        LambdaQueryWrapper<Evaluation> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Evaluation::getOrderId, orderIds)
               .orderByDesc(Evaluation::getCreateTime);

        Page<Evaluation> mpPage = new Page<>(page, pageSize);
        evaluationMapper.selectPage(mpPage, wrapper);

        java.util.List<Map<String, Object>> records = mpPage.getRecords().stream().map(e -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", e.getId());
            m.put("orderId", e.getOrderId());
            m.put("rating", e.getRating());
            m.put("content", e.getContent());
            m.put("status", e.getStatus() != null ? e.getStatus() : 0);
            m.put("createTime", e.getCreateTime());
            Order o = orderMapper.selectById(e.getOrderId());
            m.put("orderNo", o != null ? o.getOrderNo() : "");
            User u = userMapper.selectById(e.getUserId());
            m.put("userName", u != null ? u.getNickname() : "");
            return m;
        }).collect(java.util.stream.Collectors.toList());

        return Result.ok(PageResult.of(records, mpPage.getTotal(), mpPage.getCurrent(), mpPage.getSize()));
    }

    @Override
    public Result<?> merchantAppeal(Long shopId, Map<String, Object> body) {
        Long evaluationId = Long.valueOf(body.get("evaluationId").toString());
        String reason = (String) body.get("reason");
        if (reason == null || reason.trim().length() < 5) {
            return Result.fail("申诉理由至少5个字符");
        }
        Evaluation eval = evaluationMapper.selectById(evaluationId);
        if (eval == null) return Result.fail("评价不存在");

        Feedback fb = new Feedback();
        fb.setUserId(shopId);
        fb.setRole("merchant");
        fb.setType("appeal");
        fb.setContent("申诉评价#" + evaluationId + "：" + reason.trim());
        fb.setStatus(0);
        fb.setCreateTime(LocalDateTime.now());
        feedbackMapper.insert(fb);
        return Result.ok("申诉已提交，平台会审核处理");
    }
}
