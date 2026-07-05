package com.example.backend.controller;

import com.example.backend.common.Result;
import com.example.backend.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;



@Slf4j
@RestController
@RequestMapping("/api/admin/configs")
@RequiredArgsConstructor
public class SystemConfigController {

    private final SystemConfigService systemConfigService;

    @GetMapping
    public Result<?> list(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "10") int pageSize,
                          @RequestParam(required = false) String key) {
        return systemConfigService.list(page, pageSize, key);
    }

    @GetMapping("/{key}")
    public Result<?> getByKey(@PathVariable String key) {
        return systemConfigService.getByKey(key);
    }

    @PostMapping
    public Result<?> create(@RequestBody Map<String, Object> data) {
        return systemConfigService.create(data);
    }

    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        return systemConfigService.update(id, data);
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        return systemConfigService.delete(id);
    }
}
