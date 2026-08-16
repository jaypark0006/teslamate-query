package com.teslamate.query.dto;

import java.time.Instant;

/**
 * One sample along a daily commute, aligned by kilometres from that day's start
 * so the same stretch of road is the same X / heatmap column every day.
 */
public record CommuteSampleDto(
        String day,
        Long driveId,
        double elapsedMin,
        Instant start,
        String startLocal,
        Double latitude,
        Double longitude,
        Integer speed,
        Double kmFromStart,
        String kmBin,
        String label
) {}
