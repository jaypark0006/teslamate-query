package com.teslamate.query.dto;

import java.time.Instant;

/**
 * One cell on the day × hour-of-day grid.
 * {@code time} is local midnight (Grafana x-axis); {@code slot} is {@code HH:mm}
 * so a matrix transform sorts clock labels; {@code hour} is the clock hour.
 */
public record DayGridCellDto(
        Instant time,
        String day,
        double hour,
        String slot,
        int kindCode,
        String kind,
        Long sourceId,
        String label
) {
    public static final int PARK = 1;
    public static final int DRIVE = 2;
    public static final int CHARGE = 3;
    public static final int WEEKEND = 4;

    public static String cellLabel(int kindCode, Long sourceId) {
        return switch (kindCode) {
            case DRIVE -> sourceId == null ? "Drive" : "Drive #" + sourceId;
            case CHARGE -> sourceId == null ? "Charge" : "Charge #" + sourceId;
            case WEEKEND -> "Weekend";
            default -> "Park";
        };
    }
}
