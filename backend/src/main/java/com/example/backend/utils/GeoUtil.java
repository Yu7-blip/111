package com.example.backend.utils;

public class GeoUtil {

    private static final double EARTH_RADIUS = 6371.0;

    public static double haversineDistance(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS * c;
    }

    /**
     * Estimate delivery time in minutes based on biking distance.
     * Uses a piecewise speed model:
     *   - &lt; 1km: 12 km/h average (urban, traffic lights, parking)
     *   - 1-3km: 18 km/h average
     *   - &gt; 3km: 22 km/h average (arterial roads, less stopping)
     * Plus a configurable preparation time.
     */
    public static int estimateDeliveryMinutes(double distanceKm) {
        if (distanceKm <= 0) return 15;
        double speedKmh;
        if (distanceKm < 1.0) {
            speedKmh = 12.0;
        } else if (distanceKm < 3.0) {
            speedKmh = 18.0;
        } else {
            speedKmh = 22.0;
        }
        int bikeMinutes = (int) Math.ceil(distanceKm / speedKmh * 60);
        int prepMinutes = 15;
        return prepMinutes + bikeMinutes;
    }
}
