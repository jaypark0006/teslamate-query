package com.teslamate.query.service;

import com.teslamate.query.dto.AddressDto;
import com.teslamate.query.dto.CarDto;
import com.teslamate.query.dto.CarSettingsDto;
import com.teslamate.query.dto.ChargeDto;
import com.teslamate.query.dto.ChargeSampleDto;
import com.teslamate.query.dto.ChargingProcessDto;
import com.teslamate.query.dto.DriveDto;
import com.teslamate.query.dto.DrivePositionDto;
import com.teslamate.query.dto.GeofenceDto;
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

    public static List<PositionDto> toPositionDtos(List<PositionEntity> list) {
        return list.stream().map(EntityMapper::toPositionDto).toList();
    }

    public static ChargeDto toChargeDto(ChargeEntity e) {
        return new ChargeDto(
                e.id(), e.chargingProcessId(), e.date(), e.batteryLevel(), e.usableBatteryLevel(),
                e.chargeEnergyAdded(), e.chargerPower(), e.chargerVoltage(), e.chargerActualCurrent(),
                e.chargerPhases(), e.fastChargerPresent(), e.fastChargerType(),
                e.idealBatteryRangeKm(), e.ratedBatteryRangeKm(), e.outsideTemp(), e.batteryHeaterOn()
        );
    }

    public static List<ChargeDto> toChargeDtos(List<ChargeEntity> list) {
        return list.stream().map(EntityMapper::toChargeDto).toList();
    }

    /** Nested under charging-process; omits chargingProcessId for compact charts. */
    public static ChargeSampleDto toChargeSampleDto(ChargeEntity e) {
        return new ChargeSampleDto(
                e.id(), e.date(), e.batteryLevel(), e.usableBatteryLevel(), e.chargeEnergyAdded(),
                e.chargerPower(), e.chargerVoltage(), e.chargerActualCurrent(), e.chargerPhases(),
                e.fastChargerPresent(), e.fastChargerType(), e.idealBatteryRangeKm(), e.ratedBatteryRangeKm(),
                e.outsideTemp(), e.batteryHeaterOn()
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

    public static GeofenceDto toGeofenceDto(GeofenceEntity e) {
        return new GeofenceDto(
                e.id(), e.name(), e.latitude(), e.longitude(), e.radius(),
                e.billingType(), e.costPerUnit(), e.sessionFee()
        );
    }

    public static List<GeofenceDto> toGeofenceDtos(List<GeofenceEntity> list) {
        return list.stream().map(EntityMapper::toGeofenceDto).toList();
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

    /** cars + optional car_settings (multi-DAO composition). */
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
