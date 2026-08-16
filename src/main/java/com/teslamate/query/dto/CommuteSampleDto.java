package com.teslamate.query.dto;

import java.time.Instant;

/**
 * One sample along a daily commute, aligned by elapsed time from that day's start.
 * {@code elapsedMs} is a dummy epoch so Grafana can plot elapsed minutes on a time axis.
 */
public record CommuteSampleDto(
        String day,
        Long driveId,
        double elapsedMin,
        Instant elapsedMs,
        Instant start,
        String startLocal,
        Double latitude,
        Double longitude,
        Integer speed,
        Double kmFromStart,
        String label
) {}
