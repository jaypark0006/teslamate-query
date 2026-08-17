package com.teslamate.query.entity;

import org.jdbi.v3.core.mapper.reflect.ColumnName;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

/** GPS + speed for commute overlay (not a full positions row). */
public record PositionCommutePoint(
        @ColumnName("drive_id") Long driveId,
        @ColumnName("date") LocalDateTime date,
        @ColumnName("longitude") BigDecimal longitude,
        @ColumnName("latitude") BigDecimal latitude,
        @ColumnName("speed") Integer speed,
        @ColumnName("odometer") Double odometer
) {}
