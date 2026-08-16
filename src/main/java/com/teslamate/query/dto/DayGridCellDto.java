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
    public static final int HIGHLIGHT_PARK = 11;
    public static final int HIGHLIGHT_DRIVE = 12;
    public static final int HIGHLIGHT_CHARGE = 13;

    public DayGridCellDto withKindCode(int code) {
        return new DayGridCellDto(time, day, hour, slot, code, kind, sourceId, label);
    }

    public static String cellLabel(int kindCode, Long sourceId) {
        return cellLabel(kindCode, kindCode == DRIVE ? sourceId : null, kindCode == CHARGE ? sourceId : null);
    }

    public static String cellLabel(int kindCode, Long driveId, Long chargeId) {
        if (kindCode == WEEKEND) {
            return "Weekend";
        }
        boolean charge = chargeId != null;
        boolean drive = driveId != null;
        if (charge && drive) {
            return "Charge #" + chargeId + " · Drive #" + driveId;
        }
        if (charge) {
            return "Charge #" + chargeId;
        }
        if (drive) {
            return "Drive #" + driveId;
        }
        return kindCode == PARK ? "Park" : null;
    }
}
