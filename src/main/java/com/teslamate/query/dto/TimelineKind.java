package com.teslamate.query.dto;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/** One row on the trip timeline (not TeslaMate {@code states}). */
public enum TimelineKind {
    PARK(1, "park"),
    DRIVE(2, "drive"),
    CHARGE(3, "charge");

    private final int code;
    private final String layer;

    TimelineKind(int code, String layer) {
        this.code = code;
        this.layer = layer;
    }

    public int code() {
        return code;
    }

    /** Grafana Geomap / {@code MapPointDto.kind} value. */
    public String layer() {
        return layer;
    }

    public int highlightCode() {
        return code + 10;
    }

    public static Optional<TimelineKind> fromCode(long raw) {
        long n = raw;
        if (n >= DayGridCellDto.HOVER_TAIL) {
            n = n / DayGridCellDto.HOVER_TAIL;
        }
        if (n >= 11 && n <= 13) {
            n = n - 10;
        }
        return switch ((int) n) {
            case 1 -> Optional.of(PARK);
            case 2 -> Optional.of(DRIVE);
            case 3 -> Optional.of(CHARGE);
            default -> Optional.empty();
        };
    }

    /**
     * Grafana may send {@code DRIVE}, {@code Drive #9}, {@code 2}, highlight 12,
     * or a hoverCode such as {@code 20005155}. Blank / {@code ${var}} means unset.
     */
    public static Optional<TimelineKind> parse(String raw) {
        if (raw == null || raw.isBlank() || raw.contains("${")) {
            return Optional.empty();
        }
        String s = raw.trim();
        String u = s.toUpperCase(Locale.ROOT);
        if (u.startsWith("PARK")) {
            return Optional.of(PARK);
        }
        if (u.startsWith("DRIVE")) {
            return Optional.of(DRIVE);
        }
        if (u.startsWith("CHARGE")) {
            return Optional.of(CHARGE);
        }
        try {
            return fromCode(Long.parseLong(s));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    public static Set<TimelineKind> parseLayers(String raw) {
        if (raw == null || raw.isBlank() || raw.contains("${") || raw.equals("-")) {
            return EnumSet.allOf(TimelineKind.class);
        }
        EnumSet<TimelineKind> out = EnumSet.noneOf(TimelineKind.class);
        for (String part : raw.split(",")) {
            parse(part).ifPresent(out::add);
        }
        return out.isEmpty() ? EnumSet.allOf(TimelineKind.class) : out;
    }
}
