package com.teslamate.query.service;

import com.teslamate.query.domain.units.DisplayUnits;
import com.teslamate.query.dto.AddressDto;
import com.teslamate.query.dto.CarDto;
import com.teslamate.query.dto.CarSettingsDto;
import com.teslamate.query.dto.ChargeDto;
import com.teslamate.query.dto.ChargingProcessDto;
import com.teslamate.query.dto.DriveDto;
import com.teslamate.query.dto.DrivePositionDto;
import com.teslamate.query.dto.GeofenceDto;
import com.teslamate.query.dto.LatestSnapshotDto;
import com.teslamate.query.dto.PositionDto;
import com.teslamate.query.dto.SettingsDto;
import com.teslamate.query.dto.StateDto;
import com.teslamate.query.dto.UpdateDto;
import com.teslamate.query.entity.AddressEntity;
import com.teslamate.query.entity.CarEntity;
import com.teslamate.query.entity.CarSettingsEntity;
import com.teslamate.query.entity.ChargeEntity;
import com.teslamate.query.entity.ChargingProcessEntity;
import com.teslamate.query.entity.DriveEntity;
import com.teslamate.query.entity.GeofenceEntity;
import com.teslamate.query.entity.PositionEntity;
import com.teslamate.query.entity.SettingsEntity;
import com.teslamate.query.entity.StateEntity;
import com.teslamate.query.entity.UpdateEntity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.List;

/**
 * Entity (metric table row) → API DTO.
 * Length / temperature / speed / elevation are converted with {@link DisplayUnits}.
 * Field names keep historical *Km / *C suffixes; values follow response {@code units}.
 */
public final class EntityMapper {

    private EntityMapper() {}

    public static DriveDto toDriveDto(DriveEntity e, DisplayUnits u) {
        DisplayUnits units = u == null ? DisplayUnits.METRIC : u;
        Double avgSpeedKmh = null;
        if (e.distance() != null && e.durationMin() != null && e.durationMin() > 0) {
            avgSpeedKmh = BigDecimal.valueOf(e.distance() / (e.durationMin() * 60.0) * 3600.0)
                    .setScale(4, RoundingMode.HALF_UP).doubleValue();
        }
        return new DriveDto(
                e.id(), e.carId(), e.startDate(), e.endDate(), e.durationMin(),
                UnitConverter.length(e.distance(), units),
                UnitConverter.length(e.startIdealRangeKm(), units),
                UnitConverter.length(e.endIdealRangeKm(), units),
                UnitConverter.length(e.startRatedRangeKm(), units),
                UnitConverter.length(e.endRatedRangeKm(), units),
                UnitConverter.temp(e.outsideTempAvg() == null ? null : e.outsideTempAvg().doubleValue(), units),
                UnitConverter.temp(e.insideTempAvg() == null ? null : e.insideTempAvg().doubleValue(), units),
                UnitConverter.speed(avgSpeedKmh, units),
                UnitConverter.speed(e.speedMax(), units),
                e.powerMax(), e.powerMin(),
                UnitConverter.elevation(e.ascent(), units),
                UnitConverter.elevation(e.descent(), units),
                e.startPositionId(), e.endPositionId(), e.startAddressId(), e.endAddressId(),
                e.startGeofenceId(), e.endGeofenceId()
        );
    }

    public static List<DriveDto> toDriveDtos(List<DriveEntity> list, DisplayUnits u) {
        return list.stream().map(e -> toDriveDto(e, u)).toList();
    }

    public static ChargingProcessDto toChargingProcessDto(ChargingProcessEntity e, DisplayUnits u) {
        DisplayUnits units = u == null ? DisplayUnits.METRIC : u;
        return new ChargingProcessDto(
                e.id(), e.carId(), e.startDate(), e.endDate(),
                e.chargeEnergyAdded(), e.chargeEnergyUsed(), e.durationMin(),
                e.startBatteryLevel(), e.endBatteryLevel(),
                UnitConverter.length(e.startIdealRangeKm(), units),
                UnitConverter.length(e.endIdealRangeKm(), units),
                UnitConverter.length(e.startRatedRangeKm(), units),
                UnitConverter.length(e.endRatedRangeKm(), units),
                UnitConverter.temp(e.outsideTempAvg(), units),
                e.cost(), e.positionId(), e.addressId(), e.geofenceId()
        );
    }

    public static List<ChargingProcessDto> toChargingProcessDtos(List<ChargingProcessEntity> list, DisplayUnits u) {
        return list.stream().map(e -> toChargingProcessDto(e, u)).toList();
    }

    public static DrivePositionDto toDrivePositionDto(PositionEntity e, DisplayUnits u) {
        DisplayUnits units = u == null ? DisplayUnits.METRIC : u;
        return new DrivePositionDto(
                e.id(), e.date(), e.latitude(), e.longitude(),
                UnitConverter.elevation(e.elevation(), units),
                UnitConverter.speed(e.speed(), units),
                e.power(),
                UnitConverter.length(e.odometer(), units),
                UnitConverter.length(e.idealBatteryRangeKm(), units),
                UnitConverter.length(e.ratedBatteryRangeKm(), units),
                e.batteryLevel(), e.usableBatteryLevel(),
                UnitConverter.temp(e.outsideTemp(), units),
                UnitConverter.temp(e.insideTemp(), units)
        );
    }

    public static PositionDto toPositionDto(PositionEntity e, DisplayUnits u) {
        DisplayUnits units = u == null ? DisplayUnits.METRIC : u;
        return new PositionDto(
                e.id(), e.carId(), e.driveId(), e.date(), e.latitude(), e.longitude(),
                UnitConverter.elevation(e.elevation(), units),
                UnitConverter.speed(e.speed(), units),
                e.power(),
                UnitConverter.length(e.odometer(), units),
                UnitConverter.length(e.idealBatteryRangeKm(), units),
                UnitConverter.length(e.ratedBatteryRangeKm(), units),
                e.batteryLevel(), e.usableBatteryLevel(),
                UnitConverter.temp(e.outsideTemp(), units),
                UnitConverter.temp(e.insideTemp(), units)
        );
    }

    public static List<PositionDto> toPositionDtos(List<PositionEntity> list, DisplayUnits u) {
        return list.stream().map(e -> toPositionDto(e, u)).toList();
    }

    public static ChargeDto toChargeDto(ChargeEntity e, DisplayUnits u) {
        DisplayUnits units = u == null ? DisplayUnits.METRIC : u;
        return new ChargeDto(
                e.id(), e.chargingProcessId(), e.date(), e.batteryLevel(), e.usableBatteryLevel(),
                e.chargeEnergyAdded(), e.chargerPower(), e.chargerVoltage(), e.chargerActualCurrent(),
                e.chargerPhases(), e.fastChargerPresent(), e.fastChargerType(),
                UnitConverter.length(e.idealBatteryRangeKm(), units),
                UnitConverter.length(e.ratedBatteryRangeKm(), units),
                UnitConverter.temp(e.outsideTemp(), units),
                e.batteryHeaterOn()
        );
    }

    public static List<ChargeDto> toChargeDtos(List<ChargeEntity> list, DisplayUnits u) {
        return list.stream().map(e -> toChargeDto(e, u)).toList();
    }

    public static LatestSnapshotDto fromPosition(PositionEntity p) {
        return new LatestSnapshotDto(
                p.carId(), p.date(), "position",
                p.batteryLevel(), p.usableBatteryLevel(),
                p.idealBatteryRangeKm(), p.ratedBatteryRangeKm(),
                p.odometer(), p.latitude(), p.longitude(),
                p.outsideTemp(), p.insideTemp(),
                p.speed(), p.power(), null, null
        );
    }

    public static LatestSnapshotDto fromCharge(ChargeEntity c, Long carId, PositionEntity pos) {
        return new LatestSnapshotDto(
                carId, c.date(), "charge",
                c.batteryLevel(), c.usableBatteryLevel(),
                c.idealBatteryRangeKm(), c.ratedBatteryRangeKm(),
                pos == null ? null : pos.odometer(),
                pos == null ? null : pos.latitude(),
                pos == null ? null : pos.longitude(),
                c.outsideTemp(), null,
                null, null, c.chargerPower(), c.chargerVoltage()
        );
    }

    public static LatestSnapshotDto toLatestSnapshotDto(LatestSnapshotDto raw, DisplayUnits u) {
        if (raw == null) {
            return null;
        }
        DisplayUnits units = u == null ? DisplayUnits.METRIC : u;
        if (units.isMetric()) {
            return raw;
        }
        return new LatestSnapshotDto(
                raw.carId(), raw.date(), raw.source(),
                raw.batteryLevel(), raw.usableBatteryLevel(),
                UnitConverter.length(raw.idealBatteryRangeKm(), units),
                UnitConverter.length(raw.ratedBatteryRangeKm(), units),
                UnitConverter.length(raw.odometerKm(), units),
                raw.latitude(), raw.longitude(),
                UnitConverter.temp(raw.outsideTempC(), units),
                UnitConverter.temp(raw.insideTempC(), units),
                UnitConverter.speed(raw.speed(), units),
                raw.power(), raw.chargerPower(), raw.chargerVoltage()
        );
    }

    public static StateDto toStateDto(StateEntity e) {
        Long durationSeconds = null;
        if (e.startDate() != null && e.endDate() != null) {
            durationSeconds = Duration.between(e.startDate(), e.endDate()).getSeconds();
        }
        return new StateDto(e.id(), e.carId(), e.state(), e.startDate(), e.endDate(), durationSeconds);
    }

    public static List<StateDto> toStateDtos(List<StateEntity> list) {
        return list.stream().map(EntityMapper::toStateDto).toList();
    }

    public static UpdateDto toUpdateDto(UpdateEntity e) {
        return new UpdateDto(e.id(), e.carId(), e.startDate(), e.endDate(), e.version());
    }

    public static List<UpdateDto> toUpdateDtos(List<UpdateEntity> list) {
        return list.stream().map(EntityMapper::toUpdateDto).toList();
    }

    public static AddressDto toAddressDto(AddressEntity e) {
        return new AddressDto(
                e.id(), e.displayName(), e.name(), e.road(), e.houseNumber(), e.neighbourhood(),
                e.city(), e.county(), e.postcode(), e.state(), e.stateDistrict(), e.country(),
                e.latitude(), e.longitude(), e.osmId(), e.osmType()
        );
    }

    public static List<AddressDto> toAddressDtos(List<AddressEntity> list) {
        return list.stream().map(EntityMapper::toAddressDto).toList();
    }

    public static GeofenceDto toGeofenceDto(GeofenceEntity e, DisplayUnits u) {
        DisplayUnits units = u == null ? DisplayUnits.METRIC : u;
        return new GeofenceDto(
                e.id(), e.name(), e.latitude(), e.longitude(),
                UnitConverter.elevation(e.radius(), units),
                e.billingType(), e.costPerUnit(), e.sessionFee()
        );
    }

    public static List<GeofenceDto> toGeofenceDtos(List<GeofenceEntity> list, DisplayUnits u) {
        return list.stream().map(e -> toGeofenceDto(e, u)).toList();
    }

    public static SettingsDto toSettingsDto(SettingsEntity e) {
        return new SettingsDto(
                e.id(), e.unitOfLength(), e.unitOfTemperature(), e.unitOfPressure(),
                e.preferredRange(), e.baseUrl(), e.grafanaUrl(), e.language()
        );
    }

    public static CarSettingsDto toCarSettingsDto(CarSettingsEntity e) {
        return new CarSettingsDto(
                e.id(), e.suspendMin(), e.suspendAfterIdleMin(), e.reqNotUnlocked(),
                e.freeSupercharging(), e.useStreamingApi(), e.enabled(), e.lfpBattery()
        );
    }

    public static List<CarSettingsDto> toCarSettingsDtos(List<CarSettingsEntity> list) {
        return list.stream().map(EntityMapper::toCarSettingsDto).toList();
    }

    public static CarDto toCarDto(CarEntity car, CarSettingsEntity settings) {
        return new CarDto(
                car.id(), car.name(), car.vin(), car.model(), car.marketingName(), car.trimBadging(),
                car.efficiency(), car.displayPriority(), car.exteriorColor(), car.wheelType(),
                settings == null ? null : settings.lfpBattery(),
                settings == null ? null : settings.freeSupercharging(),
                settings == null ? null : settings.enabled()
        );
    }
}
