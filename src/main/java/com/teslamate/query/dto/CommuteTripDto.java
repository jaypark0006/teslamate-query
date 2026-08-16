package com.teslamate.query.dto;

import java.time.Instant;

/** One outing the commute picker kept. Times are UTC; {@code startLocal}/{@code endLocal} are Asia/Shanghai text. */
public record CommuteTripDto(
        String day,
        Long driveId,
        Instant start,
        Instant end,
        String startLocal,
        String endLocal,
        Double distanceKm,
        Integer durationMin
) {}
