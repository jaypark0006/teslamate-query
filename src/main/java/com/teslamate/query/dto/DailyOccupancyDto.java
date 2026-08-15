package com.teslamate.query.dto;

/** Hours spent driving / charging / parked on one local day. */
public record DailyOccupancyDto(
        String day,
        double driveHours,
        double chargeHours,
        double parkHours
) {}
