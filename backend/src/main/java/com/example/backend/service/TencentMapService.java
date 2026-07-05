package com.example.backend.service;

import com.example.backend.config.TencentMapConfig;
import com.example.backend.utils.GeoUtil;
import com.example.backend.utils.RedisUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class TencentMapService {

    private final TencentMapConfig tencentMapConfig;
    private final RestTemplate restTemplate;
    private final RedisUtil redisUtil;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String DISTANCE_URL = "https://apis.map.qq.com/ws/distance/v1/";
    private static final String GEOCODE_URL = "https://apis.map.qq.com/ws/geocoder/v1/";
    private static final String BIKE_DIRECTION_URL = "https://apis.map.qq.com/ws/direction/v1/bicycling/";

    /**
     * Calculate real bike-route distance from one origin to multiple destinations.
     * Returns distances in km, keyed by "lng,lat" string.
     */
    public Map<String, Double> batchBikingDistance(double originLng, double originLat,
                                                    List<double[]> destinations) {
        Map<String, Double> result = new LinkedHashMap<>();
        if (destinations.isEmpty()) return result;

        String cachePrefix = "tmap:dist:" + originLng + "," + originLat + ":";

        List<double[]> uncached = new ArrayList<>();
        for (double[] dest : destinations) {
            String key = dest[0] + "," + dest[1];
            String cached = redisUtil.getString(cachePrefix + key);
            if (cached != null) {
                result.put(key, Double.parseDouble(cached));
            } else {
                uncached.add(dest);
            }
        }
        if (uncached.isEmpty()) return result;

        // Tencent Maps supports multiple "to" points separated by ";"
        for (int i = 0; i < uncached.size(); i += 10) {
            int end = Math.min(i + 10, uncached.size());
            List<double[]> batch = uncached.subList(i, end);
            Map<String, Double> batchResult = callDistanceApi(originLng, originLat, batch);
            for (Map.Entry<String, Double> entry : batchResult.entrySet()) {
                redisUtil.set(cachePrefix + entry.getKey(), entry.getValue().toString(), 30, TimeUnit.MINUTES);
            }
            result.putAll(batchResult);
        }

        return result;
    }

    private Map<String, Double> callDistanceApi(double originLng, double originLat,
                                                 List<double[]> destinations) {
        Map<String, Double> result = new LinkedHashMap<>();
        try {
            StringBuilder toStr = new StringBuilder();
            for (int i = 0; i < destinations.size(); i++) {
                if (i > 0) toStr.append(";");
                toStr.append(destinations.get(i)[1]).append(",").append(destinations.get(i)[0]);
            }

            String url = DISTANCE_URL + "?key=" + tencentMapConfig.getKey()
                    + "&mode=bicycling"
                    + "&from=" + originLat + "," + originLng
                    + "&to=" + toStr;

            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);

            int status = root.path("status").asInt();
            if (status == 0) {
                JsonNode elements = root.path("result").path("rows").get(0).path("elements");
                for (int i = 0; i < elements.size() && i < destinations.size(); i++) {
                    double distanceMeters = elements.get(i).path("distance").asDouble();
                    String destKey = destinations.get(i)[0] + "," + destinations.get(i)[1];
                    result.put(destKey, distanceMeters / 1000.0);
                }
            } else {
                String message = root.path("message").asText();
                log.warn("Tencent distance API failed: status={}, message={}. "
                        + "请检查腾讯地图API Key是否已开启WebserviceAPI功能", status, message);
            }
        } catch (Exception e) {
            log.error("Tencent distance API error", e);
        }

        // Haversine fallback for destinations not covered by API response
        for (int i = 0; i < destinations.size(); i++) {
            String destKey = destinations.get(i)[0] + "," + destinations.get(i)[1];
            if (!result.containsKey(destKey)) {
                double fallbackDist = GeoUtil.haversineDistance(originLat, originLng,
                        destinations.get(i)[1], destinations.get(i)[0]);
                result.put(destKey, fallbackDist);
                log.debug("Haversine fallback for ({},{}) → ({},{}): {}km",
                        originLat, originLng, destinations.get(i)[1], destinations.get(i)[0],
                        String.format("%.2f", fallbackDist));
            }
        }

        return result;
    }

    /**
     * Get biking distance between two points (km), with caching and Haversine fallback.
     */
    public double bikingDistance(double fromLng, double fromLat, double toLng, double toLat) {
        String cacheKey = "tmap:dist:" + fromLng + "," + fromLat + ":" + toLng + "," + toLat;
        String cached = redisUtil.getString(cacheKey);
        if (cached != null) return Double.parseDouble(cached);

        List<double[]> dests = List.of(new double[]{toLng, toLat});
        Map<String, Double> result = batchBikingDistance(fromLng, fromLat, dests);
        String key = toLng + "," + toLat;
        if (result.containsKey(key)) {
            double dist = result.get(key);
            redisUtil.set(cacheKey, String.valueOf(dist), 30, TimeUnit.MINUTES);
            return dist;
        }

        return GeoUtil.haversineDistance(fromLat, fromLng, toLat, toLng);
    }

    /**
     * Reverse geocode — coordinates to address. Returns address string or null.
     */
    public String reverseGeocode(double lat, double lng) {
        String cacheKey = "tmap:regeo:" + lat + "," + lng;
        String cached = redisUtil.getString(cacheKey);
        if (cached != null) {
            return cached;
        }

        try {
            String url = GEOCODE_URL + "?key=" + tencentMapConfig.getKey()
                    + "&location=" + lat + "," + lng;
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);

            if (root.path("status").asInt() == 0) {
                String address = root.path("result").path("address").asText();
                if (address != null && !address.isEmpty()) {
                    redisUtil.set(cacheKey, address, 24, TimeUnit.HOURS);
                    log.info("Reverse geocode success: ({}, {}) → '{}'", lat, lng, address);
                    return address;
                }
            } else {
                log.warn("Reverse geocode failed for ({}, {}): {}", lat, lng, root.path("message").asText());
            }
        } catch (Exception e) {
            log.error("Reverse geocode error for ({}, {}): {}", lat, lng, e.getMessage());
        }
        return null;
    }

    /**
     * Geocode address to coordinates. Returns [lng, lat] or null.
     */
    public double[] geocode(String address) {
        if (address == null || address.isEmpty()) return null;

        String cacheKey = "tmap:geo:" + address;
        String cached = redisUtil.getString(cacheKey);
        if (cached != null) {
            String[] parts = cached.split(",");
            return new double[]{Double.parseDouble(parts[0]), Double.parseDouble(parts[1])};
        }

        try {
            String url = GEOCODE_URL + "?key=" + tencentMapConfig.getKey()
                    + "&address=" + address;
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);

            int status = root.path("status").asInt();
            if (status == 0) {
                JsonNode location = root.path("result").path("location");
                double lat = location.path("lat").asDouble();
                double lng = location.path("lng").asDouble();
                String locStr = lng + "," + lat;
                redisUtil.set(cacheKey, locStr, 24, TimeUnit.HOURS);
                log.info("Geocode success: '{}' → ({}, {})", address, lat, lng);
                return new double[]{lng, lat};
            } else {
                String message = root.path("message").asText();
                log.warn("Tencent geocode failed for '{}': status={}, message={}. "
                        + "请检查腾讯地图API Key是否已开启WebserviceAPI功能: https://lbs.qq.com/console/mykey.html",
                        address, status, message);
            }
        } catch (Exception e) {
            log.error("Tencent geocode error for: {}", address, e);
        }
        return null;
    }

    /**
     * Estimate delivery time in minutes based on real biking distance.
     */
    public int estimateDeliveryMinutes(double fromLng, double fromLat, double toLng, double toLat) {
        double distKm = bikingDistance(fromLng, fromLat, toLng, toLat);
        return GeoUtil.estimateDeliveryMinutes(distKm);
    }

    /**
     * Get biking route with polyline points between two points.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getBikingRoute(double fromLng, double fromLat, double toLng, double toLat) {
        String cacheKey = "tmap:route:" + fromLng + "," + fromLat + ":" + toLng + "," + toLat;
        Object cached = redisUtil.get(cacheKey);
        if (cached instanceof Map) {
            return (Map<String, Object>) cached;
        }

        Map<String, Object> result = new LinkedHashMap<>();
        try {
            String url = BIKE_DIRECTION_URL + "?key=" + tencentMapConfig.getKey()
                    + "&from=" + fromLat + "," + fromLng
                    + "&to=" + toLat + "," + toLng;

            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);

            if (root.path("status").asInt() == 0) {
                JsonNode routes = root.path("result").path("routes");
                if (routes.size() > 0) {
                    JsonNode route = routes.get(0);
                    double distanceMeters = route.path("distance").asDouble();
                    double durationMinutes = route.path("duration").asDouble();

                    result.put("distance", distanceMeters / 1000.0);
                    result.put("duration", (int) Math.ceil(durationMinutes));

                    // Tencent polyline: flat array [lat, lng, lat, lng, ...]
                    List<double[]> polyline = new ArrayList<>();
                    JsonNode polyArr = route.path("polyline");
                    for (int i = 0; i + 1 < polyArr.size(); i += 2) {
                        polyline.add(new double[]{
                                polyArr.get(i).asDouble(),   // lat
                                polyArr.get(i + 1).asDouble() // lng
                        });
                    }
                    result.put("polyline", polyline);

                    // Parse turn-by-turn steps
                    List<Map<String, Object>> steps = new ArrayList<>();
                    JsonNode stepsArr = route.path("steps");
                    for (int j = 0; j < stepsArr.size(); j++) {
                        JsonNode step = stepsArr.get(j);
                        Map<String, Object> stepMap = new LinkedHashMap<>();
                        stepMap.put("instruction", step.path("instruction").asText(""));
                        stepMap.put("roadName", step.path("road_name").asText(""));
                        stepMap.put("distance", step.path("distance").asDouble(0));
                        stepMap.put("direction", step.path("dir_desc").asText(""));
                        // Parse polyline_idx to get step start coordinates
                        JsonNode idxArr = step.path("polyline_idx");
                        if (idxArr.size() >= 1) {
                            int startIdx = idxArr.get(0).asInt(0);
                            if (startIdx >= 0 && startIdx < polyline.size()) {
                                stepMap.put("startLat", polyline.get(startIdx)[0]);
                                stepMap.put("startLng", polyline.get(startIdx)[1]);
                            }
                        }
                        steps.add(stepMap);
                    }
                    result.put("steps", steps);

                    redisUtil.set(cacheKey, result, 30, TimeUnit.MINUTES);
                    return result;
                }
            }
        } catch (Exception e) {
            log.error("Tencent direction API error", e);
        }

        // Fallback: straight line
        result.put("distance", GeoUtil.haversineDistance(fromLat, fromLng, toLat, toLng));
        result.put("duration", GeoUtil.estimateDeliveryMinutes(
                GeoUtil.haversineDistance(fromLat, fromLng, toLat, toLng)));
        result.put("polyline", List.of(
                new double[]{fromLat, fromLng},
                new double[]{toLat, toLng}
        ));
        return result;
    }
}
