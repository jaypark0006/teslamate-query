package com.teslamate.query.dto;

import java.time.Instant;

/** One chronological DRIVE / CHARGE / PARK row for the map timeline log. */
public record TimelineItemDto(
        int seq,
        TimelineKind kind,
        Long id,
        Instant start,
        Instant end,
        Double durationMin,
        String title,
        String detail,
        String color,
        Double latitude,
        Double longitude,
        Double distanceKm,
        Integer startSocPercent,
        Integer endSocPercent,
        Double energyKwh,
        String chargeType,
        String day,
        Integer dayBand,
        Instant clockStart,
        Instant clockEnd
) {}
