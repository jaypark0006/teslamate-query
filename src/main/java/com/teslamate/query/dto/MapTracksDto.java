package com.teslamate.query.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** GeoJSON FeatureCollection. Feature ids are named by kind (driveId / chargingProcessId / parkIndex). */
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
            int chargingProcessCount,
            int parkCount,
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

    public static Feature driveLine(long driveId, List<List<BigDecimal>> lonLat, Map<String, Object> props) {
        props.put("kind", "drive");
        props.put("driveId", driveId);
        return new Feature("Feature", new Geometry("LineString", lonLat), props);
    }

    public static Feature chargePoint(long chargingProcessId, BigDecimal lon, BigDecimal lat,
                                      Map<String, Object> props) {
        props.put("kind", "charge");
        props.put("chargingProcessId", chargingProcessId);
        return new Feature("Feature", new Geometry("Point", List.of(lon, lat)), props);
    }

    public static Feature parkPoint(int parkIndex, BigDecimal lon, BigDecimal lat, Map<String, Object> props) {
        props.put("kind", "park");
        props.put("parkIndex", parkIndex);
        return new Feature("Feature", new Geometry("Point", List.of(lon, lat)), props);
    }

    public static Feature arrowLine(int arrowIndex, List<List<BigDecimal>> lonLat, Map<String, Object> props) {
        props.put("kind", "arrow");
        props.put("arrowIndex", arrowIndex);
        return new Feature("Feature", new Geometry("LineString", lonLat), props);
    }
}
