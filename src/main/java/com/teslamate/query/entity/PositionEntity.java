package com.teslamate.query.entity;

import org.jdbi.v3.core.mapper.reflect.ColumnName;

import java.math.BigDecimal;
import java.time.Instant;

/** Table: positions */
public record PositionEntity(
        @ColumnName("id") Long id,
        @ColumnName("car_id") Long carId,
        @ColumnName("drive_id") Long driveId,
        @ColumnName("date") Instant date,
        @ColumnName("latitude") BigDecimal latitude,
        @ColumnName("longitude") BigDecimal longitude,
        @ColumnName("elevation") Integer elevation,
        @ColumnName("speed") Integer speed,
        @ColumnName("power") Integer power,
        @ColumnName("odometer") Double odometer,
        @ColumnName("ideal_battery_range_km") BigDecimal idealBatteryRangeKm,
        @ColumnName("est_battery_range_km") BigDecimal estBatteryRangeKm,
        @ColumnName("rated_battery_range_km") BigDecimal ratedBatteryRangeKm,
        @ColumnName("battery_level") Integer batteryLevel,
        @ColumnName("usable_battery_level") Integer usableBatteryLevel,
        @ColumnName("outside_temp") BigDecimal outsideTemp,
        @ColumnName("inside_temp") BigDecimal insideTemp
) {}
