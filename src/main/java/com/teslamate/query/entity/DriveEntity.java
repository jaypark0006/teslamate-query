package com.teslamate.query.entity;

import org.jdbi.v3.core.mapper.reflect.ColumnName;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

/** Table: drives */
public record DriveEntity(
        @ColumnName("id") Long id,
        @ColumnName("car_id") Long carId,
        @ColumnName("start_date") LocalDateTime startDate,
        @ColumnName("end_date") LocalDateTime endDate,
        @ColumnName("outside_temp_avg") BigDecimal outsideTempAvg,
        @ColumnName("inside_temp_avg") BigDecimal insideTempAvg,
        @ColumnName("speed_max") Integer speedMax,
        @ColumnName("power_max") Integer powerMax,
        @ColumnName("power_min") Integer powerMin,
        @ColumnName("start_ideal_range_km") BigDecimal startIdealRangeKm,
        @ColumnName("end_ideal_range_km") BigDecimal endIdealRangeKm,
        @ColumnName("start_rated_range_km") BigDecimal startRatedRangeKm,
        @ColumnName("end_rated_range_km") BigDecimal endRatedRangeKm,
        @ColumnName("start_km") Double startKm,
        @ColumnName("end_km") Double endKm,
        @ColumnName("distance") Double distance,
        @ColumnName("duration_min") Integer durationMin,
        @ColumnName("ascent") Integer ascent,
        @ColumnName("descent") Integer descent,
        @ColumnName("start_position_id") Long startPositionId,
        @ColumnName("end_position_id") Long endPositionId,
        @ColumnName("start_address_id") Long startAddressId,
        @ColumnName("end_address_id") Long endAddressId,
        @ColumnName("start_geofence_id") Long startGeofenceId,
        @ColumnName("end_geofence_id") Long endGeofenceId
) {}
