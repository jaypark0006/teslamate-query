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
        String label,
        String detail,
        long hoverCode,
        String color
) {
    public static final int PARK = 1;
    public static final int DRIVE = 2;
    public static final int CHARGE = 3;
    public static final int WEEKEND = 4;
    public static final int HIGHLIGHT_PARK = 11;
    public static final int HIGHLIGHT_DRIVE = 12;
    public static final int HIGHLIGHT_CHARGE = 13;

    public static final long HOVER_TAIL = 10_000_000L;

    public DayGridCellDto withKindCode(int code) {
        long tail = Math.floorMod(hoverCode, HOVER_TAIL);
        return new DayGridCellDto(time, day, hour, slot, code, kind, sourceId, label, detail,
                hoverCode(code, tail), colorFor(code));
    }

    public static long hoverCode(int kindCode, long tail) {
        if (kindCode <= 0) {
            return 0;
        }
        return kindCode * HOVER_TAIL + Math.floorMod(tail, HOVER_TAIL);
    }

    public static long hoverTail(Long sourceId, String day, String slot) {
        if (sourceId != null) {
            return Math.floorMod(sourceId, HOVER_TAIL);
        }
        int h = (String.valueOf(day) + "#" + slot).hashCode();
        return Math.floorMod(h, HOVER_TAIL);
    }

    public static String colorFor(int kindCode) {
        return switch (kindCode) {
            case PARK -> "#64748b";
            case DRIVE -> "#3b82f6";
            case CHARGE -> "#22c55e";
            case WEEKEND -> "#d97706";
            case HIGHLIGHT_PARK, HIGHLIGHT_DRIVE, HIGHLIGHT_CHARGE -> "#facc15";
            default -> null;
        };
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
