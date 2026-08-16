package com.teslamate.query.service.trip;

import com.teslamate.query.dto.DayGridCellDto;
import com.teslamate.query.dto.TimelineItemDto;
import com.teslamate.query.dto.TimelineKind;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Paint each activity onto hourly slots of local day-blocks.
 * Overnight parks fill every block they cover (grid only; the log stays one row).
 * {@code dayStartHour} shifts the civil day (4 → 04:00–04:00) using the caller's zone.
 */
public final class DayGrid {

    static final int SLOT_MIN = 60;
    static final int SLOTS_PER_DAY = 24 * 60 / SLOT_MIN;
    /** Sorts after {@code 00 04:00}…{@code 23 03:00} even if Grafana strips colons. */
    static final String WEEKEND_SLOT = "24 ★";

    private DayGrid() {}

    public static List<DayGridCellDto> paintFromTimeline(List<TimelineItemDto> items, ZoneId zone) {
        return paintFromTimeline(items, zone, 0);
    }

    public static List<DayGridCellDto> paintFromTimeline(List<TimelineItemDto> items, ZoneId zone, int dayStartHour) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        List<ActivitySpan> spans = new ArrayList<>(items.size());
        for (TimelineItemDto item : items) {
            if (item == null || item.start() == null || item.end() == null) {
                continue;
            }
            double min = item.durationMin() == null ? 0 : item.durationMin();
            spans.add(new ActivitySpan(item.kind(), item.id(), item.start(), item.end(), min, null, null));
        }
        return paint(spans, zone, dayStartHour);
    }

    public static List<DayGridCellDto> paint(List<ActivitySpan> spans, ZoneId zone) {
        return paint(spans, zone, 0);
    }

    public static List<DayGridCellDto> paint(List<ActivitySpan> spans, ZoneId zone, int dayStartHour) {
        if (spans == null || spans.isEmpty()) {
            return List.of();
        }
        ZoneId z = zone == null ? ZoneId.of("UTC") : zone;
        int startH = Math.floorMod(dayStartHour, 24);
        Map<String, Painted> cells = new LinkedHashMap<>();
        Set<LocalDate> days = new LinkedHashSet<>();
        for (ActivitySpan span : spans) {
            if (span == null || span.start() == null || span.end() == null || !span.end().isAfter(span.start())) {
                continue;
            }
            int code = code(span.kind());
            Instant cursor = span.start();
            Instant end = span.end();
            int guard = 0;
            while (cursor.isBefore(end) && guard++ < 20_000) {
                ZonedDateTime local = cursor.atZone(z);
                LocalDate day = local.minusHours(startH).toLocalDate();
                days.add(day);
                ZonedDateTime blockStart = day.atStartOfDay(z).plusHours(startH);
                long minInto = Duration.between(blockStart, local).toMinutes();
                if (minInto < 0) {
                    minInto = 0;
                }
                int slot = (int) Math.min(minInto / SLOT_MIN, SLOTS_PER_DAY - 1);
                Instant slotEnd = blockStart.plusMinutes((long) (slot + 1) * SLOT_MIN).toInstant();
                if (slotEnd.isAfter(end)) {
                    slotEnd = end;
                }
                if (!slotEnd.isAfter(cursor)) {
                    slotEnd = cursor.plusSeconds(60);
                }
                String key = day + "#" + slot;
                Painted cell = cells.get(key);
                if (cell == null || preferred(code, cell.code)) {
                    cells.put(key, new Painted(code, day, slot, span.sourceId()));
                }
                cursor = slotEnd;
            }
        }
        List<DayGridCellDto> out = new ArrayList<>(cells.size() + days.size());
        for (Painted c : cells.values()) {
            Instant midnight = c.day.atStartOfDay(z).toInstant();
            String slot = slotKey(startH, c.slot);
            int clockMin = (startH * 60 + c.slot * SLOT_MIN) % (24 * 60);
            double hour = clockMin / 60.0;
            String kind = switch (c.code) {
                case DayGridCellDto.DRIVE -> "DRIVE";
                case DayGridCellDto.CHARGE -> "CHARGE";
                default -> "PARK";
            };
            out.add(new DayGridCellDto(midnight, c.day.toString(), hour, slot, c.code, kind,
                    c.sourceId, DayGridCellDto.cellLabel(c.code, c.sourceId)));
        }
        for (LocalDate day : days) {
            DayOfWeek w = day.getDayOfWeek();
            if (w != DayOfWeek.SATURDAY && w != DayOfWeek.SUNDAY) {
                continue;
            }
            out.add(new DayGridCellDto(
                    day.atStartOfDay(z).toInstant(),
                    day.toString(),
                    24.0,
                    WEEKEND_SLOT,
                    DayGridCellDto.WEEKEND,
                    "WEEKEND",
                    null,
                    DayGridCellDto.cellLabel(DayGridCellDto.WEEKEND, null)));
        }
        out.sort(Comparator.comparing(DayGridCellDto::day).thenComparing(DayGridCellDto::slot));
        return out;
    }

    /** {@code 00 04:00} … {@code 23 03:00} so 04:00 stays first after a string sort. */
    static String slotKey(int dayStartHour, int slotIndex) {
        int startH = Math.floorMod(dayStartHour, 24);
        int idx = Math.floorMod(slotIndex, SLOTS_PER_DAY);
        int clockMin = (startH * 60 + idx * SLOT_MIN) % (24 * 60);
        return String.format("%02d %02d:%02d", idx, clockMin / 60, clockMin % 60);
    }

    /** Clock hour from {@code 22:00}, {@code 06 10:00}, or {@code ·00:00}. */
    public static Long parseSourceId(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        int i = raw.length();
        while (i > 0 && Character.isDigit(raw.charAt(i - 1))) {
            i--;
        }
        if (i == raw.length()) {
            return null;
        }
        try {
            return Long.parseLong(raw.substring(i));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static Integer parseClockHour(String slot) {
        if (slot == null || slot.isBlank() || slot.contains("★")) {
            return null;
        }
        String s = slot.trim();
        int colon = s.lastIndexOf(':');
        if (colon < 2) {
            return null;
        }
        String hh = s.substring(colon - 2, colon).replace("·", "").trim();
        try {
            int h = Integer.parseInt(hh);
            return (h >= 0 && h <= 23) ? h : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static int code(TimelineKind kind) {
        if (kind == TimelineKind.DRIVE) {
            return DayGridCellDto.DRIVE;
        }
        if (kind == TimelineKind.CHARGE) {
            return DayGridCellDto.CHARGE;
        }
        return DayGridCellDto.PARK;
    }

    /** Charge covers drive covers park when two things share an hour slot. */
    private static boolean preferred(int incoming, int existing) {
        return incoming > existing;
    }

    private record Painted(int code, LocalDate day, int slot, Long sourceId) {}
}
