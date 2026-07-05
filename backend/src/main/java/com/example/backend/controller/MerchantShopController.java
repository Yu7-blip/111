package com.example.backend.controller;

import com.example.backend.common.Result;
import com.example.backend.service.ShopService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/merchant/shop")
@RequiredArgsConstructor
public class MerchantShopController {

    private final ShopService shopService;

    @GetMapping
    public Result<?> getMerchantShop(@RequestAttribute("userId") Long userId) {
        log.info("Get merchant shop: shopId={}", userId);
        return shopService.getMerchantShop(userId);
    }

    @PutMapping
    public Result<?> updateMerchantShop(@RequestAttribute("userId") Long userId,
                                        @RequestBody Map<String, Object> data) {
        log.info("Update merchant shop: shopId={}", userId);
        return shopService.updateMerchantShop(userId, data);
    }

    @PutMapping("/business-status")
    public Result<?> toggleBusinessStatus(@RequestAttribute("userId") Long userId,
                                          @RequestBody Map<String, Object> data) {
        log.info("Toggle business status: shopId={}", userId);
        Integer businessStatus = ((Number) data.get("businessStatus")).intValue();
        return shopService.toggleBusinessStatus(userId, businessStatus);
    }
}
