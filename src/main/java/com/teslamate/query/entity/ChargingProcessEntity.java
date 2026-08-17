package com.teslamate.query.entity;

import org.jdbi.v3.core.mapper.reflect.ColumnName;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

/** Table: charging_processes */
public record ChargingProcessEntity(
        @ColumnName("id") Long id,
        @ColumnName("car_id") Long carId,
        @ColumnName("start_date") LocalDateTime startDate,
        @ColumnName("end_date") LocalDateTime endDate,
        @ColumnName("charge_energy_added") BigDecimal chargeEnergyAdded,
        @ColumnName("charge_energy_used") BigDecimal chargeEnergyUsed,
        @ColumnName("start_ideal_range_km") BigDecimal startIdealRangeKm,
        @ColumnName("end_ideal_range_km") BigDecimal endIdealRangeKm,
        @ColumnName("start_rated_range_km") BigDecimal startRatedRangeKm,
        @ColumnName("end_rated_range_km") BigDecimal endRatedRangeKm,
        @ColumnName("start_battery_level") Integer startBatteryLevel,
        @ColumnName("end_battery_level") Integer endBatteryLevel,
        @ColumnName("duration_min") Integer durationMin,
        @ColumnName("outside_temp_avg") BigDecimal outsideTempAvg,
        @ColumnName("cost") BigDecimal cost,
        @ColumnName("position_id") Long positionId,
        @ColumnName("address_id") Long addressId,
        @ColumnName("geofence_id") Long geofenceId
) {}
