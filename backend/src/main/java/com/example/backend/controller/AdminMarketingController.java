package com.example.backend.controller;

import com.example.backend.common.Result;
import com.example.backend.service.MarketingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin/activities")
@RequiredArgsConstructor
public class AdminMarketingController {

    private final MarketingService marketingService;

    @GetMapping
    public Result<?> activityList(@RequestParam(defaultValue = "1") int page,
                                  @RequestParam(defaultValue = "10") int pageSize,
                                  @RequestParam(required = false) String name,
                                  @RequestParam(required = false) Integer status) {
        return marketingService.activityList(page, pageSize, name, status);
    }

    @PostMapping
    public Result<?> activityCreate(@RequestBody Map<String, Object> data) {
        return marketingService.activityCreate(data);
    }

    @PutMapping("/{id}")
    public Result<?> activityUpdate(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        return marketingService.activityUpdate(id, data);
    }

    @DeleteMapping("/{id}")
    public Result<?> activityDelete(@PathVariable Long id) {
        return marketingService.activityDelete(id);
    }

    @PatchMapping("/{id}/status")
    public Result<?> activityUpdateStatus(@PathVariable Long id, @RequestParam Integer status) {
        return marketingService.activityUpdateStatus(id, status);
    }
}
