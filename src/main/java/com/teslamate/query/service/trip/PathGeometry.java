package com.teslamate.query.service.trip;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** Heading and small GeoJSON chevrons. */
public final class PathGeometry {

    private static final double EARTH_M = 6_378_137.0;

    private PathGeometry() {}

    /** Compass degrees 0–360, north = 0, clockwise. Null if the segment is degenerate. */
    public static Double bearingDeg(BigDecimal lon1, BigDecimal lat1, BigDecimal lon2, BigDecimal lat2) {
        if (lon1 == null || lat1 == null || lon2 == null || lat2 == null) {
            return null;
        }
        return bearingDeg(lon1.doubleValue(), lat1.doubleValue(), lon2.doubleValue(), lat2.doubleValue());
    }

    public static Double bearingDeg(double lon1, double lat1, double lon2, double lat2) {
        if (lon1 == lon2 && lat1 == lat2) {
            return null;
        }
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double dLambda = Math.toRadians(lon2 - lon1);
        double y = Math.sin(dLambda) * Math.cos(phi2);
        double x = Math.cos(phi1) * Math.sin(phi2) - Math.sin(phi1) * Math.cos(phi2) * Math.cos(dLambda);
        double deg = Math.toDegrees(Math.atan2(y, x));
        return (deg + 360.0) % 360.0;
    }

    /** LineString [left, tip, right] pointing along {@code bearingDeg}. */
    public static List<List<BigDecimal>> chevron(
            BigDecimal tipLon, BigDecimal tipLat, double bearingDeg, double armMeters
    ) {
        if (tipLon == null || tipLat == null || armMeters <= 0) {
            return List.of();
        }
        double lon = tipLon.doubleValue();
        double lat = tipLat.doubleValue();
        double back = (bearingDeg + 180.0) % 360.0;
        double[] left = offset(lon, lat, back + 28.0, armMeters);
        double[] right = offset(lon, lat, back - 28.0, armMeters);
        return List.of(coord(left[0], left[1]), coord(lon, lat), coord(right[0], right[1]));
    }

    private static double[] offset(double lon, double lat, double bearingDeg, double meters) {
        double ang = meters / EARTH_M;
        double theta = Math.toRadians(bearingDeg);
        double phi1 = Math.toRadians(lat);
        double lambda1 = Math.toRadians(lon);
        double sinPhi2 = Math.sin(phi1) * Math.cos(ang) + Math.cos(phi1) * Math.sin(ang) * Math.cos(theta);
        double phi2 = Math.asin(Math.max(-1.0, Math.min(1.0, sinPhi2)));
        double y = Math.sin(theta) * Math.sin(ang) * Math.cos(phi1);
        double x = Math.cos(ang) - Math.sin(phi1) * Math.sin(phi2);
        double lambda2 = lambda1 + Math.atan2(y, x);
        return new double[]{Math.toDegrees(lambda2), Math.toDegrees(phi2)};
    }

    private static List<BigDecimal> coord(double lon, double lat) {
        return List.of(scale(lon), scale(lat));
    }

    private static BigDecimal scale(double v) {
        return BigDecimal.valueOf(v).setScale(7, RoundingMode.HALF_UP);
    }
}
