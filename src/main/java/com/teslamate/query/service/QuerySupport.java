package com.teslamate.query.service;

import com.teslamate.query.config.QueryProperties;
import com.teslamate.query.domain.units.DisplayUnits;
import com.teslamate.query.dto.TimelineKind;
import com.teslamate.query.dto.TripFocus;
import com.teslamate.query.exception.BadRequestException;
import com.teslamate.query.service.trip.DayGrid;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Set;

@Component
public class QuerySupport {

    private final QueryProperties properties;

    public QuerySupport(QueryProperties properties) {
        this.properties = properties;
    }

    /**
     * Parse display units from query params. Defaults: {@code km}, {@code C}.
     * Filter inputs such as {@code minDistance} are interpreted in {@code lengthUnit}
     * and converted to km for SQL.
     */
    public DisplayUnits units(String lengthUnit, String tempUnit) {
        return DisplayUnits.of(lengthUnit, tempUnit);
    }

    public int page(Integer page) {
        if (page == null || page < 1) {
            return 1;
        }
        return Math.min(page, 1_000_000);
    }

    public int recentLimit(Integer limit) {
        int n = limit == null ? 5 : limit;
        if (n < 1) {
            n = 5;
        }
        return Math.min(n, 10);
    }

    public int mergeGapMin(Integer mergeGapMin) {
        if (mergeGapMin == null || mergeGapMin < 0) {
            return 0;
        }
        return Math.min(mergeGapMin, 180);
    }

    public int chargeMergeGapMin(Integer value) {
        return value == null ? 15 : Math.min(Math.max(value, 0), 180);
    }

    public int mergeDistanceM(Integer value) {
        return value == null ? 100 : Math.min(Math.max(value, 0), 1_000);
    }

    /** Grafana custom var: 10, "10 min", or unsubstituted ${min_park}. */
    public int minParkMin(String raw) {
        if (raw == null || raw.isBlank() || raw.contains("${") || raw.contains("%24")) {
            return 10;
        }
        String s = raw.trim();
        int i = 0;
        while (i < s.length() && Character.isDigit(s.charAt(i))) {
            i++;
        }
        if (i == 0) {
            return 10;
        }
        return Math.min(Math.max(Integer.parseInt(s.substring(0, i)), 0), 24 * 60);
    }

    /** Grafana may send 0, "Off", "5 min", or an unsubstituted ${merge_gap}. */
    public int mergeGapMin(String raw) {
        if (raw == null || raw.isBlank() || raw.contains("${") || raw.contains("%24")) {
            return 0;
        }
        String s = raw.trim();
        if (s.equalsIgnoreCase("off") || s.equalsIgnoreCase("none")) {
            return 0;
        }
        int i = 0;
        while (i < s.length() && Character.isDigit(s.charAt(i))) {
            i++;
        }
        if (i == 0) {
            return 0;
        }
        return mergeGapMin(Integer.parseInt(s.substring(0, i)));
    }

    public int size(Integer size) {
        int s = size == null ? properties.getDefaultPageSize() : size;
        if (s < 1) {
            s = properties.getDefaultPageSize();
        }
        return Math.min(s, properties.getMaxPageSize());
    }

    public int offset(int page, int size) {
        long off = (long) (page - 1) * size;
        if (off > Integer.MAX_VALUE) {
            throw new BadRequestException("page too large");
        }
        return (int) off;
    }

    public boolean unset(String raw) {
        return raw == null || raw.isBlank() || raw.contains("${") || raw.contains("%24")
                || raw.equalsIgnoreCase("all") || raw.equalsIgnoreCase("none") || raw.equals("-");
    }

    /**
     * Local clock as minutes from midnight. Accepts {@code 5:40}, {@code 05:40}, {@code 540}.
     */
    public int clockMinutes(String raw, int fallback) {
        if (raw == null || raw.isBlank() || raw.contains("${") || raw.equals("-")) {
            return fallback;
        }
        String s = raw.trim();
        int colon = s.indexOf(':');
        try {
            if (colon >= 0) {
                int h = Integer.parseInt(s.substring(0, colon));
                String rest = s.substring(colon + 1);
                int m = rest.isEmpty() ? 0 : Integer.parseInt(rest.replaceAll("[^0-9].*", ""));
                return Math.floorMod(h, 24) * 60 + Math.min(Math.max(m, 0), 59);
            }
            String digits = s.replaceAll("[^0-9]", "");
            if (digits.isEmpty()) {
                return fallback;
            }
            int n = Integer.parseInt(digits);
            if (n <= 24) {
                return Math.floorMod(n, 24) * 60;
            }
            int h = n / 100;
            int m = n % 100;
            return Math.floorMod(h, 24) * 60 + Math.min(m, 59);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /** Local hour the day-block starts (0–23). Grafana sends 4 for 04:00–04:00. */
    public int dayStartHour(String raw) {
        if (raw == null || raw.isBlank() || raw.contains("${") || raw.contains("%24")) {
            return 0;
        }
        String s = raw.trim();
        int i = 0;
        if (s.charAt(0) == '-') {
            i = 1;
        }
        while (i < s.length() && Character.isDigit(s.charAt(i))) {
            i++;
        }
        if (i == 0 || (i == 1 && s.charAt(0) == '-')) {
            return 0;
        }
        return Math.floorMod(Integer.parseInt(s.substring(0, i)), 24);
    }

    /** Grafana may send {@code browser}, an IANA zone, or an unsubstituted ${__timezone}. */
    public ZoneId zone(String raw) {
        if (raw == null || raw.isBlank() || raw.contains("${") || raw.equalsIgnoreCase("browser")) {
            return ZoneId.of("Asia/Shanghai");
        }
        try {
            return ZoneId.of(raw.trim());
        } catch (Exception e) {
            return ZoneId.of("Asia/Shanghai");
        }
    }

    public Instant parseInstant(String value, String name) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (Exception e) {
            try {
                return OffsetDateTime.parse(value).toInstant();
            } catch (Exception e2) {
                throw new BadRequestException(name + " must be ISO-8601 datetime, got: " + value);
            }
        }
    }

    public TripFocus tripFocus(String day, String slot, String kind, String from, String to, String id,
                               ZoneId zone) {
        ZoneId z = zone == null ? ZoneId.of("Asia/Shanghai") : zone;
        Instant windowFrom = optionalInstant(from);
        Instant windowTo = optionalInstant(to);
        if (windowFrom != null && !plausibleTrip(windowFrom)) {
            windowFrom = null;
        }
        if (windowTo != null && !plausibleTrip(windowTo)) {
            windowTo = null;
        }
        return new TripFocus(
                TimelineKind.parse(kind).orElse(null),
                optionalLong(id),
                windowFrom,
                windowTo,
                optionalDay(day, z),
                unset(slot) ? null : DayGrid.parseClockHour(slot));
    }

    public Set<TimelineKind> layers(String kinds) {
        return TimelineKind.parseLayers(kinds);
    }

    public Long optionalLong(String raw) {
        if (unset(raw)) {
            return null;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            return DayGrid.parseSourceId(raw);
        }
    }

    public Instant optionalInstant(String raw) {
        if (unset(raw)) {
            return null;
        }
        String s = raw.trim();
        if (s.matches(".*T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)? \\d{2}:\\d{2}(:\\d{2})?$")) {
            s = s.replaceFirst(" (\\d{2}:\\d{2}(?::\\d{2})?)$", "+$1");
        }
        if (s.chars().skip(s.startsWith("-") ? 1 : 0).allMatch(Character::isDigit)) {
            try {
                long n = Long.parseLong(s);
                Instant t = Math.abs(n) < 1_000_000_000_000L
                        ? Instant.ofEpochSecond(n)
                        : Instant.ofEpochMilli(n);
                return plausibleTrip(t) ? t : null;
            } catch (NumberFormatException e) {
                return null;
            }
        }
        try {
            Instant t = Instant.parse(s);
            return plausibleTrip(t) ? t : null;
        } catch (RuntimeException e) {
            try {
                Instant t = OffsetDateTime.parse(s).toInstant();
                return plausibleTrip(t) ? t : null;
            } catch (RuntimeException e2) {
                return null;
            }
        }
    }

    public LocalDate optionalDay(String raw, ZoneId zone) {
        if (unset(raw)) {
            return null;
        }
        try {
            return LocalDate.parse(raw.trim());
        } catch (RuntimeException e) {
            Instant t = optionalInstant(raw);
            if (t == null) {
                return null;
            }
            ZoneId z = zone == null ? ZoneId.of("Asia/Shanghai") : zone;
            return t.atZone(z).toLocalDate();
        }
    }

    static final Instant MIN_PLAUSIBLE_TRIP = Instant.parse("2012-01-01T00:00:00Z");

    static boolean plausibleTrip(Instant t) {
        return t != null && !t.isBefore(MIN_PLAUSIBLE_TRIP);
    }

    public Instant[] requireRange(String fromStr, String toStr) {
        Instant from = parseInstant(fromStr, "from");
        Instant to = parseInstant(toStr, "to");
        if (from == null || to == null) {
            throw new BadRequestException("from and to are required (ISO-8601)");
        }
        if (from.isAfter(to)) {
            throw new BadRequestException("from must be before to");
        }
        return new Instant[]{from, to};
    }
}
