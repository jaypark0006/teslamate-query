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
import com.teslamate.query.dto.ChargeDto;
import com.teslamate.query.dto.RecentChargeDto;
import com.teslamate.query.dto.RecentDriveDto;
import com.teslamate.query.entity.CarEntity;
import com.teslamate.query.entity.ChargeEntity;
import com.teslamate.query.entity.ChargingProcessEntity;
import com.teslamate.query.entity.DriveEntity;
import com.teslamate.query.entity.PositionEntity;
import com.teslamate.query.entity.SettingsEntity;
import com.teslamate.query.exception.NotFoundException;
import com.teslamate.query.domain.time.UtcDateTimes;
import com.teslamate.query.domain.units.DisplayUnits;
import com.teslamate.query.service.trip.ChargeSessionComposer;
import com.teslamate.query.service.trip.DriveOutingComposer;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class RecentActivityService {

    static final int MERGE_FETCH_CAP = 30;
    static final int SESSION_DETAIL_NEIGHBORS = 50;

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
        return recentCharges(carId, limit, null, null);
    }

    public List<RecentChargeDto> recentCharges(
            long carId,
            Integer limit,
            Integer mergeGapMin,
            Integer mergeDistanceM
    ) {
        requireCar(carId);
        int n = support.recentLimit(limit);
        int gap = support.chargeMergeGapMin(mergeGapMin);
        int distance = support.mergeDistanceM(mergeDistanceM);
        List<Long> ids = chargingProcessDao.findIds(
                ChargingProcessSearchCondition.builder().carId(carId).completedOnly(true).build(),
                gap > 0 ? MERGE_FETCH_CAP : n, 0);
        List<ChargingProcessEntity> rows = chargingProcessDao.findByIdsOrdered(ids);
        List<List<ChargingProcessEntity>> sessions = composeChargeSessions(rows, gap, distance, n);
        List<Long> sessionIds = sessions.stream().flatMap(List::stream)
                .map(ChargingProcessEntity::id).distinct().toList();
        Map<Long, ChargeEntity> sampleByProcess = chargeDao.findLatestPerProcess(sessionIds).stream()
                .collect(Collectors.toMap(ChargeEntity::chargingProcessId, s -> s, (a, b) -> a));
        Map<Long, List<ChargeEntity>> samplesByProcess = chargeDao.findByProcessIds(sessionIds, 10_000).stream()
                .collect(Collectors.groupingBy(ChargeEntity::chargingProcessId));
        String preferred = preferredRangeMode();
        return sessions.stream()
                .map(group -> toChargeDto(group, sampleByProcess, samplesByProcess, preferred))
                .toList();
    }

    public RecentChargeDto chargingSession(long chargingProcessId, Integer mergeGapMin, Integer mergeDistanceM) {
        List<ChargingProcessEntity> group = resolveChargingSession(
                chargingProcessId,
                support.chargeMergeGapMin(mergeGapMin),
                support.mergeDistanceM(mergeDistanceM));
        List<Long> ids = group.stream().map(ChargingProcessEntity::id).toList();
        Map<Long, ChargeEntity> latest = chargeDao.findLatestPerProcess(ids).stream()
                .collect(Collectors.toMap(ChargeEntity::chargingProcessId, s -> s, (a, b) -> a));
        Map<Long, List<ChargeEntity>> samples = chargeDao.findByProcessIds(ids, 10_000).stream()
                .collect(Collectors.groupingBy(ChargeEntity::chargingProcessId));
        return toChargeDto(group, latest, samples, preferredRangeMode());
    }

    public List<ChargeDto> chargingSessionCharges(
            long chargingProcessId,
            Integer mergeGapMin,
            Integer mergeDistanceM,
            DisplayUnits units
    ) {
        List<Long> ids = resolveChargingSession(
                chargingProcessId,
                support.chargeMergeGapMin(mergeGapMin),
                support.mergeDistanceM(mergeDistanceM)).stream()
                .map(ChargingProcessEntity::id)
                .toList();
        return chargeDao.findByProcessIds(ids).stream()
                .sorted(java.util.Comparator.comparing(ChargeEntity::date))
                .map(sample -> EntityMapper.toChargeDto(sample, units))
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
                first.startDate().toInstant(ZoneOffset.UTC),
                last.endDate().toInstant(ZoneOffset.UTC),
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
            List<ChargingProcessEntity> group,
            Map<Long, ChargeEntity> sampleByProcess,
            Map<Long, List<ChargeEntity>> samplesByProcess,
            String preferred
    ) {
        ChargingProcessEntity first = group.getFirst();
        ChargingProcessEntity last = group.getLast();
        List<Long> ids = group.stream().map(ChargingProcessEntity::id).toList();
        ChargeEntity sample = sampleByProcess.get(last.id());
        List<ChargeEntity> samples = ids.stream()
                .flatMap(id -> samplesByProcess.getOrDefault(id, List.of()).stream())
                .sorted(java.util.Comparator.comparing(ChargeEntity::date))
                .toList();
        ChargeType type = sample == null
                ? null
                : ActivityClassifier.chargeType(
                        sample.fastChargerPresent(), sample.fastChargerType(), sample.connChargeCable());
        var band2080 = group.size() == 1 ? ChargeSessionMetrics.band20to80(samples) : null;
        var band80end = group.size() == 1 ? ChargeSessionMetrics.band80toEnd(samples) : null;
        BigDecimal energyAdded = sum(group.stream().map(ChargingProcessEntity::chargeEnergyAdded).toList());
        BigDecimal energyUsed = sum(group.stream().map(ChargingProcessEntity::chargeEnergyUsed).toList());
        BigDecimal cost = sum(group.stream().map(ChargingProcessEntity::cost).toList());
        int duration = elapsedMinutes(group);
        return new RecentChargeDto(
                last.id(),
                ids,
                group.size(),
                UtcDateTimes.fromDatabase(first.startDate()),
                UtcDateTimes.fromDatabase(last.endDate()),
                duration,
                energyAdded,
                type,
                pickRange(first.startIdealRangeKm(), first.startRatedRangeKm(), preferred),
                pickRange(last.endIdealRangeKm(), last.endRatedRangeKm(), preferred),
                first.startBatteryLevel(),
                last.endBatteryLevel(),
                ChargeSessionMetrics.efficiencyPercent(energyAdded, energyUsed),
                ChargeSessionMetrics.avgPowerKw(energyAdded, duration),
                band2080 == null ? null : band2080.kw(),
                band80end == null ? null : band80end.kw(),
                band2080 == null ? null : band2080.label(),
                band80end == null ? null : band80end.label(),
                energyUsed,
                cost,
                last.positionId(),
                last.addressId(),
                last.geofenceId()
        );
    }

    private List<ChargingProcessEntity> resolveChargingSession(long id, int gap, int distance) {
        ChargingProcessEntity seed = chargingProcessDao.findById(id)
                .orElseThrow(() -> new NotFoundException("Charging process not found: " + id));
        List<Long> allIds = chargingProcessDao.findIds(
                ChargingProcessSearchCondition.builder().carId(seed.carId()).completedOnly(true).build(),
                10_000, 0);
        int index = allIds.indexOf(id);
        if (index < 0) {
            return List.of(seed);
        }
        int from = Math.max(0, index - SESSION_DETAIL_NEIGHBORS);
        int to = Math.min(allIds.size(), index + SESSION_DETAIL_NEIGHBORS + 1);
        List<ChargingProcessEntity> candidates = chargingProcessDao.findByIdsOrdered(allIds.subList(from, to));
        return composeChargeSessions(candidates, gap, distance, candidates.size()).stream()
                .filter(group -> group.stream().anyMatch(p -> p.id().equals(id)))
                .findFirst()
                .orElse(List.of(seed));
    }

    private List<List<ChargingProcessEntity>> composeChargeSessions(
            List<ChargingProcessEntity> rows,
            int gap,
            int distance,
            int limit
    ) {
        if (rows.isEmpty()) {
            return List.of();
        }
        List<Long> positionIds = rows.stream().map(ChargingProcessEntity::positionId)
                .filter(Objects::nonNull).distinct().toList();
        Map<Long, PositionEntity> positions = positionIds.isEmpty()
                ? Map.of()
                : positionDao.findByIds(positionIds).stream()
                        .collect(Collectors.toMap(PositionEntity::id, p -> p, (a, b) -> a));
        var oldest = rows.stream().map(ChargingProcessEntity::startDate)
                .filter(Objects::nonNull).min(java.time.LocalDateTime::compareTo).orElse(null);
        var newest = rows.stream().map(ChargingProcessEntity::endDate)
                .filter(Objects::nonNull).max(java.time.LocalDateTime::compareTo).orElse(null);
        List<DriveEntity> drives = List.of();
        if (oldest != null && newest != null) {
            var condition = DriveSearchCondition.builder().carId(rows.getFirst().carId())
                    .overlapping(UtcDateTimes.fromDatabase(oldest), UtcDateTimes.fromDatabase(newest)).build();
            List<Long> driveIds = driveDao.findIds(condition, 2_000, 0);
            if (!driveIds.isEmpty()) {
                drives = driveDao.findByIdsOrdered(driveIds);
            }
        }
        return ChargeSessionComposer.newestSessions(rows, positions, drives, gap, distance, limit);
    }

    private static int elapsedMinutes(List<ChargingProcessEntity> group) {
        ChargingProcessEntity first = group.getFirst();
        ChargingProcessEntity last = group.getLast();
        if (group.size() == 1 && first.durationMin() != null) {
            return first.durationMin();
        }
        if (first.startDate() != null && last.endDate() != null) {
            return Math.toIntExact(Duration.between(first.startDate(), last.endDate()).toMinutes());
        }
        return group.stream().map(ChargingProcessEntity::durationMin)
                .filter(Objects::nonNull).mapToInt(Integer::intValue).sum();
    }

    private static BigDecimal sum(List<BigDecimal> values) {
        BigDecimal total = null;
        for (BigDecimal value : values) {
            if (value != null) {
                total = total == null ? value : total.add(value);
            }
        }
        return total;
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
