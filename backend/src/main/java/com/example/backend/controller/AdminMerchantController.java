package com.example.backend.controller;

import com.example.backend.common.Result;
import com.example.backend.service.ShopService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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
@RequestMapping("/api/admin/merchants")
@RequiredArgsConstructor
public class AdminMerchantController {

    private final ShopService shopService;

    @GetMapping
    public Result<?> list(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "10") int pageSize,
                          @RequestParam(required = false) String name,
                          @RequestParam(required = false) Integer status) {
        return shopService.list(page, pageSize, name, status);
    }

    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable Long id) {
        return shopService.detail(id);
    }

    @PostMapping("/{id}/audit")
    public Result<?> audit(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        Integer status = data.get("status") != null ? ((Number) data.get("status")).intValue() : null;
        String remark = data.get("remark") != null ? data.get("remark").toString() : null;
        return shopService.audit(id, status, remark);
    }

    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        return shopService.adminUpdate(id, data);
    }

    @PostMapping
    public Result<?> create(@RequestBody Map<String, Object> data) {
        return shopService.adminCreate(data);
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        return shopService.adminDelete(id);
    }
}
