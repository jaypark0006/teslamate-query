package com.teslamate.query.service;

import com.teslamate.query.dao.CarDao;
import com.teslamate.query.dao.ChargeDao;
import com.teslamate.query.dao.ChargingProcessDao;
import com.teslamate.query.dao.DriveDao;
import com.teslamate.query.dao.PositionDao;
import com.teslamate.query.dao.SettingsDao;
import com.teslamate.query.db.condition.ChargingProcessSearchCondition;
import com.teslamate.query.db.condition.DriveSearchCondition;
import com.teslamate.query.dto.ChargeType;
import com.teslamate.query.dto.RecentChargeDto;
import com.teslamate.query.dto.RecentDriveDto;
import com.teslamate.query.entity.CarEntity;
import com.teslamate.query.entity.ChargeEntity;
import com.teslamate.query.entity.ChargingProcessEntity;
import com.teslamate.query.entity.DriveEntity;
import com.teslamate.query.entity.PositionEntity;
import com.teslamate.query.entity.SettingsEntity;
import com.teslamate.query.exception.NotFoundException;
import com.teslamate.query.service.trip.DriveOutingComposer;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class RecentActivityService {

    static final int MERGE_FETCH_CAP = 30;

    private final CarDao carDao;
    private final DriveDao driveDao;
    private final ChargingProcessDao chargingProcessDao;
    private final ChargeDao chargeDao;
    private final PositionDao positionDao;
    private final SettingsDao settingsDao;
    private final QuerySupport support;

    public RecentActivityService(
            CarDao carDao,
            DriveDao driveDao,
            ChargingProcessDao chargingProcessDao,
            ChargeDao chargeDao,
            PositionDao positionDao,
            SettingsDao settingsDao,
            QuerySupport support
    ) {
        this.carDao = carDao;
        this.driveDao = driveDao;
        this.chargingProcessDao = chargingProcessDao;
        this.chargeDao = chargeDao;
        this.positionDao = positionDao;
        this.settingsDao = settingsDao;
        this.support = support;
    }

    public List<RecentDriveDto> recentDrives(long carId, Integer limit, Integer mergeGapMin) {
        return recentDrives(carId, limit, mergeGapMin == null ? null : String.valueOf(mergeGapMin));
    }

    public List<RecentDriveDto> recentDrives(long carId, Integer limit, String mergeGapMin) {
        CarEntity car = requireCar(carId);
        int n = support.recentLimit(limit);
        int gap = support.mergeGapMin(mergeGapMin);
        int fetch = gap > 0 ? MERGE_FETCH_CAP : n;
        List<Long> ids = driveDao.findIds(
                DriveSearchCondition.builder().carId(carId).completedOnly(true).build(),
                fetch, 0);
        List<DriveEntity> rows = driveDao.findByIdsOrdered(ids);
        String preferred = preferredRangeMode();
        Double efficiency = car.efficiency();
        List<List<DriveEntity>> outings = DriveOutingComposer.newestOutings(rows, gap, n);
        Map<Long, PositionEntity> positions = loadEndpointPositions(outings);
        return outings.stream()
                .map(group -> toDriveDto(group, preferred, efficiency, positions))
                .toList();
    }

    public List<RecentChargeDto> recentCharges(long carId, Integer limit) {
        requireCar(carId);
        int n = support.recentLimit(limit);
        List<Long> ids = chargingProcessDao.findIds(
                ChargingProcessSearchCondition.builder().carId(carId).completedOnly(true).build(),
                n, 0);
        List<ChargingProcessEntity> rows = chargingProcessDao.findByIdsOrdered(ids);
        Map<Long, ChargeEntity> sampleByProcess = chargeDao.findLatestPerProcess(ids).stream()
                .collect(Collectors.toMap(ChargeEntity::chargingProcessId, s -> s, (a, b) -> a));
        Map<Long, List<ChargeEntity>> samplesByProcess = chargeDao.findByProcessIds(ids, 10_000).stream()
                .collect(Collectors.groupingBy(ChargeEntity::chargingProcessId));
        String preferred = preferredRangeMode();
        return rows.stream()
                .map(p -> toChargeDto(
                        p, sampleByProcess.get(p.id()), samplesByProcess.getOrDefault(p.id(), List.of()), preferred))
                .toList();
    }

    private RecentDriveDto toDriveDto(
            List<DriveEntity> group,
            String preferred,
            Double efficiency,
            Map<Long, PositionEntity> positions
    ) {
        DriveEntity first = group.getFirst();
        DriveEntity last = group.getLast();
        List<Long> driveIds = new ArrayList<>();
        double distance = 0;
        int duration = 0;
        for (DriveEntity d : group) {
            driveIds.add(d.id());
            if (d.distance() != null) {
                distance += d.distance();
            }
            if (d.durationMin() != null) {
                duration += d.durationMin();
            }
        }
        BigDecimal startRange = pickRange(first.startIdealRangeKm(), first.startRatedRangeKm(), preferred);
        BigDecimal endRange = pickRange(last.endIdealRangeKm(), last.endRatedRangeKm(), preferred);
        Double rangeUsed = null;
        if (startRange != null && endRange != null) {
            rangeUsed = startRange.subtract(endRange).doubleValue();
        }
        Double energy = null;
        if (rangeUsed != null && efficiency != null) {
            energy = BigDecimal.valueOf(rangeUsed * efficiency).setScale(2, RoundingMode.HALF_UP).doubleValue();
        }
        Double avgSpeed = null;
        if (duration > 0) {
            avgSpeed = BigDecimal.valueOf(distance / (duration * 60.0) * 3600.0)
                    .setScale(1, RoundingMode.HALF_UP).doubleValue();
        }
        Double whKm = null;
        if (energy != null && distance > 0.2) {
            whKm = BigDecimal.valueOf(energy / distance * 1000.0)
                    .setScale(0, RoundingMode.HALF_UP)
                    .doubleValue();
        }
        PositionEntity startPos = first.startPositionId() == null ? null : positions.get(first.startPositionId());
        PositionEntity endPos = last.endPositionId() == null ? null : positions.get(last.endPositionId());
        return new RecentDriveDto(
                first.id(),
                driveIds,
                first.startDate(),
                last.endDate(),
                duration,
                round1(distance),
                energy,
                rangeUsed == null ? null : round1(rangeUsed),
                startRange == null ? null : round1(startRange.doubleValue()),
                endRange == null ? null : round1(endRange.doubleValue()),
                startPos == null ? null : startPos.batteryLevel(),
                endPos == null ? null : endPos.batteryLevel(),
                avgSpeed,
                whKm
        );
    }

    private Map<Long, PositionEntity> loadEndpointPositions(List<List<DriveEntity>> outings) {
        List<Long> ids = outings.stream()
                .flatMap(List::stream)
                .flatMap(d -> Stream.of(d.startPositionId(), d.endPositionId()))
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return positionDao.findByIds(ids).stream()
                .collect(Collectors.toMap(PositionEntity::id, p -> p, (a, b) -> a));
    }

    private RecentChargeDto toChargeDto(
            ChargingProcessEntity p,
            ChargeEntity sample,
            List<ChargeEntity> samples,
            String preferred
    ) {
        ChargeType type = sample == null
                ? null
                : ActivityClassifier.chargeType(
                        sample.fastChargerPresent(), sample.fastChargerType(), sample.connChargeCable());
        var band2080 = ChargeSessionMetrics.band20to80(samples);
        var band80end = ChargeSessionMetrics.band80toEnd(samples);
        return new RecentChargeDto(
                p.id(),
                p.startDate(),
                p.endDate(),
                p.durationMin(),
                p.chargeEnergyAdded(),
                type,
                pickRange(p.startIdealRangeKm(), p.startRatedRangeKm(), preferred),
                pickRange(p.endIdealRangeKm(), p.endRatedRangeKm(), preferred),
                p.startBatteryLevel(),
                p.endBatteryLevel(),
                ChargeSessionMetrics.efficiencyPercent(p),
                ChargeSessionMetrics.avgPowerKw(p),
                band2080 == null ? null : band2080.kw(),
                band80end == null ? null : band80end.kw(),
                band2080 == null ? null : band2080.label(),
                band80end == null ? null : band80end.label()
        );
    }

    private CarEntity requireCar(long carId) {
        return carDao.findById(carId)
                .orElseThrow(() -> new NotFoundException("Car not found: " + carId));
    }

    private String preferredRangeMode() {
        return settingsDao.find()
                .map(SettingsEntity::preferredRange)
                .orElse("rated");
    }

    private static BigDecimal pickRange(BigDecimal ideal, BigDecimal rated, String preferred) {
        boolean wantIdeal = preferred != null && preferred.equalsIgnoreCase("ideal");
        if (wantIdeal && ideal != null) {
            return ideal;
        }
        return rated != null ? rated : ideal;
    }

    private static Double round1(double v) {
        return BigDecimal.valueOf(v).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
}
