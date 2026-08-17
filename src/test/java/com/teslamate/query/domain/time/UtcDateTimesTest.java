package com.teslamate.query.domain.time;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class UtcDateTimesTest {

    @Test
    void convertsInstantToUtcDatabaseTimestampAndBack() {
        Instant instant = Instant.parse("2026-08-17T12:45:06.254Z");
        LocalDateTime database = LocalDateTime.parse("2026-08-17T12:45:06.254");

        assertEquals(database, UtcDateTimes.toDatabase(instant));
        assertEquals(instant, UtcDateTimes.fromDatabase(database));
    }

    @Test
    void preservesNulls() {
        assertNull(UtcDateTimes.toDatabase(null));
        assertNull(UtcDateTimes.fromDatabase(null));
    }
}
