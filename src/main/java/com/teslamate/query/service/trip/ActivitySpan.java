package com.teslamate.query.service.trip;

import com.teslamate.query.dto.TimelineKind;

import java.time.Instant;

/** One clipped DRIVE / CHARGE / PARK interval inside a map window. */
public record ActivitySpan(
        TimelineKind kind,
        Long sourceId,
        Instant start,
        Instant end,
        double durationMin,
        Long locationPositionId,
        Long afterDriveId,
        String detail
) {
    public ActivitySpan(
            TimelineKind kind,
            Long sourceId,
            Instant start,
            Instant end,
            double durationMin,
            Long locationPositionId,
            Long afterDriveId
    ) {
        this(kind, sourceId, start, end, durationMin, locationPositionId, afterDriveId, null);
    }
}
