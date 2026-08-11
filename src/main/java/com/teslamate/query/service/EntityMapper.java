package com.teslamate.query.service;

import com.teslamate.query.dto.ChargeSampleDto;
import com.teslamate.query.dto.ChargingProcessDto;
import com.teslamate.query.dto.DriveDto;
import com.teslamate.query.dto.DrivePositionDto;
import com.teslamate.query.dto.PositionDto;
import com.teslamate.query.entity.ChargeEntity;
import com.teslamate.query.entity.ChargingProcessEntity;
import com.teslamate.query.entity.DriveEntity;
import com.teslamate.query.entity.PositionEntity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** Entity (table row) → API DTO. Keep API shape stable; Entity stays 1:1 with DB. */
public final class EntityMapper {

    private EntityMapper() {}

    public static DriveDto toDriveDto(DriveEntity e) {
        Double avgSpeed = null;
        if (e.distance() != null && e.durationMin() != null && e.durationMin() > 0) {
            avgSpeed = BigDecimal.valueOf(e.distance() / (e.durationMin() * 60.0) * 3600.0)
                    .setScale(4, RoundingMode.HALF_UP).doubleValue();
        }
        return new DriveDto(
                e.id(), e.carId(), e.startDate(), e.endDate(), e.durationMin(), e.distance(),
                e.startIdealRangeKm(), e.endIdealRangeKm(), e.startRatedRangeKm(), e.endRatedRangeKm(),
                e.outsideTempAvg() == null ? null : e.outsideTempAvg().doubleValue(),
                e.insideTempAvg() == null ? null : e.insideTempAvg().doubleValue(),
                avgSpeed, e.speedMax(), e.powerMax(), e.powerMin(), e.ascent(), e.descent(),
                e.startPositionId(), e.endPositionId(), e.startAddressId(), e.endAddressId(),
                e.startGeofenceId(), e.endGeofenceId()
        );
    }

    public static List<DriveDto> toDriveDtos(List<DriveEntity> list) {
        return list.stream().map(EntityMapper::toDriveDto).toList();
    }

    public static ChargingProcessDto toChargingProcessDto(ChargingProcessEntity e) {
        return new ChargingProcessDto(
                e.id(), e.carId(), e.startDate(), e.endDate(),
                e.chargeEnergyAdded(), e.chargeEnergyUsed(), e.durationMin(),
                e.startBatteryLevel(), e.endBatteryLevel(),
                e.startIdealRangeKm(), e.endIdealRangeKm(), e.startRatedRangeKm(), e.endRatedRangeKm(),
                e.outsideTempAvg(), e.cost(), e.positionId(), e.addressId(), e.geofenceId()
        );
    }

    public static List<ChargingProcessDto> toChargingProcessDtos(List<ChargingProcessEntity> list) {
        return list.stream().map(EntityMapper::toChargingProcessDto).toList();
    }

    public static DrivePositionDto toDrivePositionDto(PositionEntity e) {
        return new DrivePositionDto(
                e.id(), e.date(), e.latitude(), e.longitude(), e.elevation(), e.speed(), e.power(),
                e.odometer(), e.idealBatteryRangeKm(), e.ratedBatteryRangeKm(),
                e.batteryLevel(), e.usableBatteryLevel(), e.outsideTemp(), e.insideTemp()
        );
    }

    public static PositionDto toPositionDto(PositionEntity e) {
        return new PositionDto(
                e.id(), e.carId(), e.driveId(), e.date(), e.latitude(), e.longitude(), e.elevation(),
                e.speed(), e.power(), e.odometer(), e.idealBatteryRangeKm(), e.ratedBatteryRangeKm(),
                e.batteryLevel(), e.usableBatteryLevel(), e.outsideTemp(), e.insideTemp()
        );
    }

    public static ChargeSampleDto toChargeSampleDto(ChargeEntity e) {
        return new ChargeSampleDto(
                e.id(), e.date(), e.batteryLevel(), e.usableBatteryLevel(), e.chargeEnergyAdded(),
                e.chargerPower(), e.chargerVoltage(), e.chargerActualCurrent(), e.chargerPhases(),
                e.fastChargerPresent(), e.fastChargerType(), e.idealBatteryRangeKm(), e.ratedBatteryRangeKm(),
                e.outsideTemp(), e.batteryHeaterOn()
        );
    }
}
