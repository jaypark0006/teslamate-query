package com.teslamate.query.service.trip;

import java.time.Instant;

public record ParkGap(
        Instant start,
        Instant end,
        double durationMin,
        Long endPositionId,
        Long afterDriveId
) {}
