package com.teslamate.query.entity;

import org.jdbi.v3.core.mapper.reflect.ColumnName;

import java.time.Instant;
import java.time.LocalDateTime;

/** Table: updates */
public record UpdateEntity(
        @ColumnName("id") Long id,
        @ColumnName("car_id") Long carId,
        @ColumnName("start_date") LocalDateTime startDate,
        @ColumnName("end_date") LocalDateTime endDate,
        @ColumnName("version") String version
) {}
