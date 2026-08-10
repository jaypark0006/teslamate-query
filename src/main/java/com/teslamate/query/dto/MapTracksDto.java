package com.teslamate.query.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Grafana Geomap-friendly payload: GeoJSON FeatureCollection + optional meta.
 * Features: LineString (drives) and Point (charges).
 */
public record MapTracksDto(
        String type,
        List<Feature> features,
        Meta meta
) {
    public record Meta(
            long carId,
            Instant from,
            Instant to,
            int driveCount,
            int chargeCount,
            int totalPathPoints
    ) {}

    public record Feature(
            String type,
            Geometry geometry,
            Map<String, Object> properties
    ) {}

    public record Geometry(
            String type,
            Object coordinates
    ) {}

    public static Feature lineString(long driveId, List<List<BigDecimal>> lonLat, Map<String, Object> props) {
        props.put("kind", "drive");
        props.put("id", driveId);
        return new Feature("Feature", new Geometry("LineString", lonLat), props);
    }

    public static Feature point(long chargeId, BigDecimal lon, BigDecimal lat, Map<String, Object> props) {
        props.put("kind", "charge");
        props.put("id", chargeId);
        return new Feature("Feature", new Geometry("Point", List.of(lon, lat)), props);
    }
}
