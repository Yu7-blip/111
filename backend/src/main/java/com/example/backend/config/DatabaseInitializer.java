package com.example.backend.config;

import com.example.backend.service.TencentMapService;
import com.example.backend.utils.GeoHashUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * 数据库初始化辅助：修复缺坐标的店铺、回填 GeoHash 等。
 * 表结构和种子数据已由 Spring Boot sql.init (schema.sql + data.sql) 自动管理。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseInitializer {

    private final DataSource dataSource;
    private final TencentMapService tencentMapService;

    @PostConstruct
    public void init() {
        log.info("=== DatabaseInitializer: running post-startup tasks ===");
        fixShopsWithoutCoords();
        backfillGeohash();
        log.info("=== DatabaseInitializer: done ===");
    }

    /**
     * 给缺少经纬度的店铺用地址文本做地理编码，自动补上坐标
     */
    private void fixShopsWithoutCoords() {
        try (Connection conn = dataSource.getConnection()) {
            // 查询有地址但没有经纬度的店铺
            String sql = "SELECT id, name, address FROM shop WHERE (latitude IS NULL OR longitude IS NULL) AND address IS NOT NULL AND address != ''";
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                String updateSql = "UPDATE shop SET latitude = ?, longitude = ?, geohash = ? WHERE id = ?";
                try (PreparedStatement ups = conn.prepareStatement(updateSql)) {
                    int fixed = 0, failed = 0;
                    while (rs.next()) {
                        long id = rs.getLong("id");
                        String name = rs.getString("name");
                        String address = rs.getString("address");
                        double[] coords = tencentMapService.geocode(address);
                        if (coords != null) {
                            double lat = coords[1];
                            double lng = coords[0];
                            String geohash = GeoHashUtil.encode(lat, lng, 7);
                            ups.setDouble(1, lat);
                            ups.setDouble(2, lng);
                            ups.setString(3, geohash);
                            ups.setLong(4, id);
                            ups.executeUpdate();
                            fixed++;
                            log.info("Auto-fixed shop [{}] {}: geocoded '{}' → ({}, {})", id, name, address, lat, lng);
                        } else {
                            failed++;
                            log.warn("Cannot geocode shop [{}] {}: address='{}'. "
                                    + "请在腾讯地图控制台开启WebserviceAPI: https://lbs.qq.com/console/mykey.html",
                                    id, name, address);
                        }
                    }
                    log.info("fixShopsWithoutCoords: fixed {} shops, {} failed (need API key)", fixed, failed);
                }
            }
        } catch (Exception e) {
            log.warn("fixShopsWithoutCoords skipped: {}", e.getMessage());
        }
    }

    /**
     * 回填店铺的 GeoHash（对于已有经纬度但 geohash 为空的店铺）
     */
    private void backfillGeohash() {
        try (Connection conn = dataSource.getConnection()) {
            try {
                conn.createStatement().execute("SELECT geohash FROM shop LIMIT 0");
            } catch (Exception e) {
                log.info("GeoHash column not yet available, skipping backfill");
                return;
            }

            String selectSql = "SELECT id, latitude, longitude FROM shop WHERE geohash IS NULL AND latitude IS NOT NULL AND longitude IS NOT NULL";
            try (PreparedStatement ps = conn.prepareStatement(selectSql);
                 ResultSet rs = ps.executeQuery()) {

                String updateSql = "UPDATE shop SET geohash = ? WHERE id = ?";
                try (PreparedStatement ups = conn.prepareStatement(updateSql)) {
                    int count = 0;
                    while (rs.next()) {
                        long id = rs.getLong("id");
                        double lat = rs.getDouble("latitude");
                        double lng = rs.getDouble("longitude");
                        String geohash = GeoHashUtil.encode(lat, lng, 7);
                        ups.setString(1, geohash);
                        ups.setLong(2, id);
                        ups.executeUpdate();
                        count++;
                    }
                    if (count > 0) {
                        log.info("GeoHash backfill: updated {} shops", count);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("GeoHash backfill skipped: {}", e.getMessage());
        }
    }
}
