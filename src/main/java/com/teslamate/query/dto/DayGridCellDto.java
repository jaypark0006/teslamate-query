package com.teslamate.query.dto;

import java.time.Instant;

/**
 * One cell on the day × hour-of-day grid.
 * {@code time} is local midnight (Grafana x-axis); {@code slot} is {@code HH:mm}
 * so a matrix transform sorts 00:00 … 23:45; {@code hour} is 0–24.
 */
public record DayGridCellDto(
        Instant time,
        String day,
        double hour,
        String slot,
        int kindCode,
        String kind
) {
    public static final int PARK = 1;
    public static final int DRIVE = 2;
    public static final int CHARGE = 3;
}
