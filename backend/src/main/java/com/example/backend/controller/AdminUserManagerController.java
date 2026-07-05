package com.example.backend.controller;

import com.example.backend.common.Result;
import com.example.backend.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin/admins")
@RequiredArgsConstructor
public class AdminUserManagerController {

    private final AdminUserService adminUserService;

    @GetMapping
    public Result<?> list(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "10") int pageSize,
                          @RequestParam(required = false) String username,
                          @RequestParam(required = false) String role,
                          @RequestParam(required = false) Integer status) {
        return adminUserService.list(page, pageSize, username, role, status);
    }

    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable Long id) {
        return adminUserService.detail(id);
    }

    @PostMapping
    public Result<?> create(@RequestBody Map<String, Object> data) {
        return adminUserService.create(data);
    }

    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        return adminUserService.update(id, data);
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        return adminUserService.delete(id);
    }

    @PatchMapping("/{id}/status")
    public Result<?> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        return adminUserService.updateStatus(id, status);
    }
}
