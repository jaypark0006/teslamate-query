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
    static final String WEEKEND_SLOT = "★";

    private DayGrid() {}

    public static List<DayGridCellDto> paintFromTimeline(List<TimelineItemDto> items, ZoneId zone) {
        return paintFromTimeline(items, zone, 0);
    }

    public static List<DayGridCellDto> paintFromTimeline(List<TimelineItemDto> items, ZoneId zone, int dayStartHour) {
        return paintFromTimeline(items, zone, dayStartHour, null, null);
    }

    public static List<DayGridCellDto> paintFromTimeline(List<TimelineItemDto> items, ZoneId zone, int dayStartHour,
                                                         Instant from, Instant to) {
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
        return paint(spans, zone, dayStartHour, from, to);
    }

    public static List<DayGridCellDto> paint(List<ActivitySpan> spans, ZoneId zone) {
        return paint(spans, zone, 0);
    }

    public static List<DayGridCellDto> paint(List<ActivitySpan> spans, ZoneId zone, int dayStartHour) {
        return paint(spans, zone, dayStartHour, null, null);
    }

    public static List<DayGridCellDto> paint(List<ActivitySpan> spans, ZoneId zone, int dayStartHour,
                                             Instant from, Instant to) {
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
                cells.put(key, Painted.merge(cell, code, day, slot, span.sourceId()));
                cursor = slotEnd;
            }
        }
        fillEmptySlots(cells, days, startH, z, from, to);
        List<DayGridCellDto> out = new ArrayList<>(cells.size() + days.size());
        for (Painted c : cells.values()) {
            Instant x = columnTime(c.day, startH, z, from, to);
            String slot = slotKey(startH, c.slot);
            int clockMin = (startH * 60 + c.slot * SLOT_MIN) % (24 * 60);
            double hour = clockMin / 60.0;
            String kind = switch (c.code) {
                case DayGridCellDto.DRIVE -> "DRIVE";
                case DayGridCellDto.CHARGE -> "CHARGE";
                case DayGridCellDto.PARK -> "PARK";
                default -> "";
            };
            String label = c.code == 0 ? null : DayGridCellDto.cellLabel(c.code, c.driveId, c.chargeId);
            Long sourceId = c.chargeId != null ? c.chargeId : c.driveId;
            out.add(new DayGridCellDto(x, c.day.toString(), hour, slot, c.code, kind,
                    sourceId, label));
        }
        for (LocalDate day : days) {
            DayOfWeek w = day.getDayOfWeek();
            if (w != DayOfWeek.SATURDAY && w != DayOfWeek.SUNDAY) {
                continue;
            }
            out.add(new DayGridCellDto(
                    columnTime(day, startH, z, from, to),
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

    /**
     * Clock hour labels that stay in 04:00-first order even after a string sort:
     * {@code 04}…{@code 23} then {@code ·00}…{@code ·03}. No {@code HH:mm} so Grafana
     * does not rotate the Y-axis to the dashboard's start hour.
     */
    static String slotKey(int dayStartHour, int slotIndex) {
        int startH = Math.floorMod(dayStartHour, 24);
        int idx = Math.floorMod(slotIndex, SLOTS_PER_DAY);
        int clockH = (startH * 60 + idx * SLOT_MIN) / 60 % 24;
        if (startH != 0 && clockH < startH) {
            return String.format("·%02d", clockH);
        }
        return String.format("%02d", clockH);
    }

    static Instant columnTime(LocalDate day, int startH, ZoneId z, Instant from, Instant to) {
        Instant x = day.atStartOfDay(z).plusHours(startH).toInstant();
        if (from != null && x.isBefore(from)) {
            x = from;
        }
        if (to != null && !x.isBefore(to)) {
            x = to.minusSeconds(1);
        }
        return x;
    }

    private static void fillEmptySlots(Map<String, Painted> cells, Set<LocalDate> days,
                                       int startH, ZoneId z, Instant from, Instant to) {
        if (from == null || to == null || !to.isAfter(from)) {
            return;
        }
        LocalDate first = from.atZone(z).minusHours(startH).toLocalDate();
        LocalDate last = to.minusNanos(1).atZone(z).minusHours(startH).toLocalDate();
        for (LocalDate day = first; !day.isAfter(last); day = day.plusDays(1)) {
            days.add(day);
            for (int slot = 0; slot < SLOTS_PER_DAY; slot++) {
                cells.putIfAbsent(day + "#" + slot, new Painted(0, day, slot, null, null));
            }
        }
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
        if (colon >= 2) {
            return parseHour(s.substring(colon - 2, colon).replace("·", "").trim());
        }
        s = s.replace("·", "").trim();
        if (s.length() > 2) {
            s = s.substring(s.length() - 2);
        }
        return parseHour(s);
    }

    private static Integer parseHour(String hh) {
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

    /**
     * Display color: charge if any charge in the hour, else drive, else park.
     * Drive and charge ids are both kept so click can highlight each.
     */
    private record Painted(int code, LocalDate day, int slot, Long driveId, Long chargeId) {
        static Painted merge(Painted existing, int incoming, LocalDate day, int slot, Long sourceId) {
            Long driveId = existing == null ? null : existing.driveId;
            Long chargeId = existing == null ? null : existing.chargeId;
            if (incoming == DayGridCellDto.DRIVE && sourceId != null) {
                driveId = sourceId;
            }
            if (incoming == DayGridCellDto.CHARGE && sourceId != null) {
                chargeId = sourceId;
            }
            int code;
            if (chargeId != null) {
                code = DayGridCellDto.CHARGE;
            } else if (driveId != null) {
                code = DayGridCellDto.DRIVE;
            } else if (existing == null) {
                code = incoming;
            } else {
                code = Math.max(existing.code, incoming);
            }
            return new Painted(code, day, slot, driveId, chargeId);
        }
    }
}
