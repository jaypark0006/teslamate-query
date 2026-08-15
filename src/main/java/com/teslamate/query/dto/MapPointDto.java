package com.teslamate.query.dto;

import java.time.Instant;

/**
 * Flat lat/lon row for Grafana Geomap.
 * Drive rows form the route (arrows follow {@code time} order).
 * Charge / park rows are stop markers.
 */
public record MapPointDto(
        Instant time,
        Double latitude,
        Double longitude,
        String kind,
        Long id,
        Integer seq,
        Double heading,
        Double elapsedMin,
        String color,
        String label,
        String durationLabel,
        Double durationMin,
        Double energyKwh
) {}
