package com.example.backend.controller;

import com.example.backend.common.Result;
import com.example.backend.service.StatisticsService;
import com.example.backend.service.TencentMapService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final StatisticsService statisticsService;
    private final TencentMapService tencentMapService;

    @GetMapping("/stats")
    public Result<?> dashboardStats() {
        return statisticsService.dashboardStats();
    }

    @GetMapping("/order-trend")
    public Result<?> orderTrend() {
        return statisticsService.orderTrend();
    }

    /**
     * 地理编码 — 地址 → 坐标
     */
    @GetMapping("/geocode")
    public Result<?> geocode(@RequestParam String address) {
        double[] coords = tencentMapService.geocode(address);
        if (coords != null) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("address", address);
            result.put("lat", coords[1]);
            result.put("lng", coords[0]);
            return Result.ok(result);
        }
        return Result.fail("地理编码失败，请检查地址是否正确");
    }

    /**
     * 逆地理编码 — 坐标 → 地址（地图选点后用）
     */
    @GetMapping("/reverse-geocode")
    public Result<?> reverseGeocode(@RequestParam Double lat, @RequestParam Double lng) {
        String address = tencentMapService.reverseGeocode(lat, lng);
        if (address != null) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("address", address);
            result.put("lat", lat);
            result.put("lng", lng);
            return Result.ok(result);
        }
        return Result.fail("逆地理编码失败");
    }

    /**
     * 检查腾讯地图API Key是否正常
     */
    @GetMapping("/check-map-api")
    public Result<?> checkMapApi() {
        Map<String, Object> result = new LinkedHashMap<>();
        // 用贵阳市一个已知地址测试geocode
        double[] coords = tencentMapService.geocode("贵阳市观山湖区");
        if (coords != null) {
            result.put("status", "OK");
            result.put("message", "腾讯地图API Key正常工作");
            result.put("testCoords", "lng=" + coords[0] + ", lat=" + coords[1]);
        } else {
            result.put("status", "FAIL");
            result.put("message", "腾讯地图API Key未生效，请前往 https://lbs.qq.com/console/mykey.html 开启WebserviceAPI功能");
            result.put("tip", "在腾讯地图控制台 → 我的Key → 开启WebServiceAPI即可");
        }
        return Result.ok(result);
    }
}
