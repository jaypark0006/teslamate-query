package com.teslamate.query.domain.time;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Converts between API instants and TeslaMate's UTC-valued
 * {@code timestamp without time zone} columns.
 */
public final class UtcDateTimes {

    private UtcDateTimes() {}

    public static LocalDateTime toDatabase(Instant value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC).toLocalDateTime();
    }

    public static Instant fromDatabase(LocalDateTime value) {
        return value == null ? null : value.toInstant(ZoneOffset.UTC);
    }
}
