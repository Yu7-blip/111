package com.example.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.common.PageResult;
import com.example.backend.common.Result;
import com.example.backend.entity.*;
import com.example.backend.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackMapper feedbackMapper;
    private final UserMapper userMapper;
    private final DeliveryMapper deliveryMapper;
    private final ShopMapper shopMapper;

    // 用户提交投诉/反馈
    @PostMapping("/wx/feedback")
    public Result<?> userFeedback(@RequestAttribute("userId") Long userId,
                                   @RequestBody Map<String, Object> body) {
        String content = (String) body.get("content");
        if (content == null || content.trim().isEmpty()) {
            return Result.fail("请输入内容");
        }
        String type = (String) body.getOrDefault("type", "complaint");
        Feedback fb = new Feedback();
        fb.setUserId(userId);
        fb.setRole("user");
        fb.setType(type);
        fb.setContent(content.trim());
        fb.setStatus(0);
        fb.setCreateTime(LocalDateTime.now());
        feedbackMapper.insert(fb);
        return Result.ok("已提交，平台会尽快处理");
    }

    // Admin 查看反馈列表
    @GetMapping("/admin/feedback")
    public Result<?> adminList(@RequestParam(defaultValue = "1") Integer page,
                               @RequestParam(defaultValue = "10") Integer pageSize,
                               @RequestParam(required = false) String role,
                               @RequestParam(required = false) String type,
                               @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<Feedback> wrapper = new LambdaQueryWrapper<>();
        if (role != null && !role.isEmpty()) wrapper.eq(Feedback::getRole, role);
        if (type != null && !type.isEmpty()) wrapper.eq(Feedback::getType, type);
        if (status != null) wrapper.eq(Feedback::getStatus, status);
        wrapper.orderByDesc(Feedback::getCreateTime);

        Page<Feedback> mpPage = new Page<>(page, pageSize);
        feedbackMapper.selectPage(mpPage, wrapper);

        // 解析提交者名称
        List<Map<String, Object>> records = new ArrayList<>();
        for (Feedback fb : mpPage.getRecords()) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", fb.getId());
            map.put("userId", fb.getUserId());
            map.put("role", fb.getRole());
            map.put("type", fb.getType());
            map.put("content", fb.getContent());
            map.put("status", fb.getStatus());
            map.put("reply", fb.getReply());
            map.put("createTime", fb.getCreateTime());

            // 解析提交者名称
            String userName = "";
            if ("user".equals(fb.getRole())) {
                User user = userMapper.selectById(fb.getUserId());
                userName = user != null ? user.getNickname() : "";
            } else if ("delivery".equals(fb.getRole())) {
                Delivery delivery = deliveryMapper.selectById(fb.getUserId());
                userName = delivery != null ? delivery.getName() : "";
            } else if ("merchant".equals(fb.getRole())) {
                Shop shop = shopMapper.selectById(fb.getUserId());
                userName = shop != null ? shop.getName() : "";
            }
            map.put("userName", userName);
            records.add(map);
        }

        return Result.ok(PageResult.of(records, mpPage.getTotal(), mpPage.getCurrent(), mpPage.getSize()));
    }

    // Admin 回复反馈
    @PutMapping("/admin/feedback/{id}/reply")
    public Result<?> adminReply(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Feedback fb = feedbackMapper.selectById(id);
        if (fb == null) return Result.fail("反馈不存在");
        fb.setReply(body.get("reply"));
        fb.setStatus(1);
        feedbackMapper.updateById(fb);
        return Result.ok("已回复");
    }
}
