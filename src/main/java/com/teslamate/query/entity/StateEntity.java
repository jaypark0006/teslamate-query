package com.teslamate.query.entity;

import org.jdbi.v3.core.mapper.reflect.ColumnName;

import java.time.Instant;

/** Table: states */
public record StateEntity(
        @ColumnName("id") Long id,
        @ColumnName("car_id") Long carId,
        @ColumnName("state") String state,
        @ColumnName("start_date") Instant startDate,
        @ColumnName("end_date") Instant endDate
) {}
