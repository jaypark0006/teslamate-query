package com.teslamate.query.entity;

import org.jdbi.v3.core.mapper.reflect.ColumnName;

import java.math.BigDecimal;
import java.time.Instant;

/** Lean projection for map paths (not a full positions row). */
public record PositionPathPoint(
        @ColumnName("drive_id") Long driveId,
        @ColumnName("date") Instant date,
        @ColumnName("longitude") BigDecimal longitude,
        @ColumnName("latitude") BigDecimal latitude
) {}
