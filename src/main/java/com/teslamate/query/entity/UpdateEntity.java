package com.teslamate.query.entity;

import org.jdbi.v3.core.mapper.reflect.ColumnName;

import java.time.Instant;

/** Table: updates */
public record UpdateEntity(
        @ColumnName("id") Long id,
        @ColumnName("car_id") Long carId,
        @ColumnName("start_date") Instant startDate,
        @ColumnName("end_date") Instant endDate,
        @ColumnName("version") String version
) {}
