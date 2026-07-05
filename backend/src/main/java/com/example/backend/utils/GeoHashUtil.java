package com.example.backend.utils;

import java.util.*;

/**
 * GeoHash 编码/解码工具 — 用于附近店铺空间索引搜索
 * Base32 字符集（排除 a/i/l/o 避免混淆）
 */
public class GeoHashUtil {

    private static final char[] BASE32 = {
        '0','1','2','3','4','5','6','7','8','9',
        'b','c','d','e','f','g','h','j','k','m',
        'n','p','q','r','s','t','u','v','w','x','y','z'
    };

    // 精度 → 大致覆盖半径（km）
    private static final double[] PRECISION_RADIUS = {
        5000,   // 1
        5000,   // 2
        1250,   // 3
        630,    // 4
        156,    // 5
        78,     // 6
        20,     // 7
        5,      // 8
        2.5,    // 9
        0.6,    // 10
        0.3,    // 11
        0.08    // 12
    };

    /**
     * 将 (lat, lng) 编码为 GeoHash 字符串
     */
    public static String encode(double lat, double lng, int precision) {
        if (precision < 1 || precision > 12) {
            throw new IllegalArgumentException("precision must be 1-12");
        }
        double latMin = -90.0, latMax = 90.0;
        double lngMin = -180.0, lngMax = 180.0;
        long bits = 0;
        int bitCount = 0;
        StringBuilder sb = new StringBuilder(precision);

        while (sb.length() < precision) {
            if (bitCount % 2 == 0) {
                double mid = (lngMin + lngMax) / 2;
                if (lng >= mid) { bits = (bits << 1) | 1; lngMin = mid; }
                else { bits = (bits << 1); lngMax = mid; }
            } else {
                double mid = (latMin + latMax) / 2;
                if (lat >= mid) { bits = (bits << 1) | 1; latMin = mid; }
                else { bits = (bits << 1); latMax = mid; }
            }
            bitCount++;
            if (bitCount == 5) {
                sb.append(BASE32[(int) (bits & 0x1F)]);
                bits = 0;
                bitCount = 0;
            }
        }
        return sb.toString();
    }

    /**
     * 解码 GeoHash 返回 [lat_center, lng_center]
     */
    public static double[] decode(String geohash) {
        double latMin = -90.0, latMax = 90.0;
        double lngMin = -180.0, lngMax = 180.0;
        boolean isLng = true;

        for (int i = 0; i < geohash.length(); i++) {
            char c = geohash.charAt(i);
            int val = 0;
            for (int j = 0; j < BASE32.length; j++) {
                if (BASE32[j] == c) { val = j; break; }
            }
            for (int bit = 4; bit >= 0; bit--) {
                int b = (val >> bit) & 1;
                if (isLng) {
                    double mid = (lngMin + lngMax) / 2;
                    if (b == 1) lngMin = mid; else lngMax = mid;
                } else {
                    double mid = (latMin + latMax) / 2;
                    if (b == 1) latMin = mid; else latMax = mid;
                }
                isLng = !isLng;
            }
        }
        return new double[]{
            (latMin + latMax) / 2,
            (lngMin + lngMax) / 2
        };
    }

    /**
     * 获取指定 GeoHash 的 8 个相邻单元格
     * 使用"解码→偏移→重新编码"方法，简单可靠
     */
    public static List<String> getNeighbors(String geohash) {
        double[] center = decode(geohash);
        double lat = center[0];
        double lng = center[1];

        // 估算该精度下单元格的宽高（度数）
        int bits = geohash.length() * 5;
        double latHeight = 180.0 / Math.pow(2, bits / 2.0);
        double lngWidth  = 360.0 / Math.pow(2, (bits + 1) / 2.0);

        // 用 2倍偏移确保跨越到相邻格
        double dLat = latHeight * 2.0;
        double dLng = lngWidth  * 2.0;

        double[][] offsets = {
            { dLat,   0    },  // N
            { dLat,   dLng },  // NE
            { 0,      dLng },  // E
            {-dLat,   dLng },  // SE
            {-dLat,   0    },  // S
            {-dLat,  -dLng },  // SW
            { 0,     -dLng },  // W
            { dLat,  -dLng },  // NW
        };

        Set<String> seen = new LinkedHashSet<>();
        seen.add(geohash);

        for (double[] offset : offsets) {
            double nl = lat + offset[0];
            double el = lng + offset[1];
            nl = Math.max(-90.0, Math.min(90.0, nl));
            el = Math.max(-180.0, Math.min(180.0, el));
            String nHash = encode(nl, el, geohash.length());
            if (!seen.contains(nHash)) {
                seen.add(nHash);
            }
        }

        List<String> neighbors = new ArrayList<>(seen);
        neighbors.remove(geohash); // 去掉自身
        return neighbors;
    }

    /**
     * 根据搜索半径(km)返回最佳 GeoHash 精度等级。
     * 选择恰好覆盖该半径的最小精度（即最高精度）。
     */
    public static int precisionForRadius(double radiusKm) {
        for (int i = PRECISION_RADIUS.length - 1; i >= 1; i--) {
            if (radiusKm >= PRECISION_RADIUS[i]) {
                return i;
            }
        }
        return 7; // default
    }
}
