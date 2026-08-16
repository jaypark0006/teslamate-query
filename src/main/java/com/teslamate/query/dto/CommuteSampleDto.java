package com.teslamate.query.dto;

import java.time.Instant;

/**
 * One sample along a daily commute, aligned by minutes since that day's start.
 * {@code kmFromStart} is how far you had gotten at that minute.
 */
public record CommuteSampleDto(
        String day,
        Long driveId,
        double elapsedMin,
        Instant elapsedAxis,
        Instant start,
        String startLocal,
        Double latitude,
        Double longitude,
        Integer speed,
        Double kmFromStart,
        String elapsedBin,
        String label
) {}
