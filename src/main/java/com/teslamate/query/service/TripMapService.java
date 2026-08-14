package com.teslamate.query.service;

import com.teslamate.query.dao.ChargeDao;
import com.teslamate.query.dao.ChargingProcessDao;
import com.teslamate.query.dao.DriveDao;
import com.teslamate.query.dao.PositionDao;
import com.teslamate.query.db.condition.ChargingProcessSearchCondition;
import com.teslamate.query.db.condition.DriveSearchCondition;
import com.teslamate.query.domain.units.DisplayUnits;
import com.teslamate.query.dto.MapTracksDto;
import com.teslamate.query.entity.ChargeEntity;
import com.teslamate.query.entity.ChargingProcessEntity;
import com.teslamate.query.entity.DriveEntity;
import com.teslamate.query.entity.PositionEntity;
import com.teslamate.query.entity.PositionPathPoint;
import com.teslamate.query.service.trip.ParkComposer;
import com.teslamate.query.service.trip.ParkGap;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Trip map: drive lines + charge points + derived park points.
 * Each piece is a single-table Dao; Park is computed in Java.
 */
@Service
public class TripMapService {

    private static final int DEFAULT_MIN_PARK_MIN = 15;
    private static final int DEFAULT_MICRO_DRIVE_MIN = 15;

    private final DriveDao driveDao;
    private final ChargingProcessDao chargingProcessDao;
    private final ChargeDao chargeDao;
    private final PositionDao positionDao;
    private final QuerySupport support;

    public TripMapService(DriveDao driveDao, ChargingProcessDao chargingProcessDao,
                          ChargeDao chargeDao, PositionDao positionDao, QuerySupport support) {
        this.driveDao = driveDao;
        this.chargingProcessDao = chargingProcessDao;
        this.chargeDao = chargeDao;
        this.positionDao = positionDao;
        this.support = support;
    }

    public MapTracksDto trip(long carId, String fromStr, String toStr,
                             Integer minParkMin, Integer microDriveThresholdMin,
                             Integer maxDrives, Integer maxChargingProcesses, DisplayUnits units) {
        DisplayUnits u = units == null ? DisplayUnits.METRIC : units;
        Instant[] range = support.requireRange(fromStr, toStr);
        Instant from = range[0];
        Instant to = range[1];
        Instant now = Instant.now();
        int driveLimit = maxDrives == null ? 80 : Math.min(Math.max(maxDrives, 1), 500);
        int chargeLimit = maxChargingProcesses == null ? 200 : Math.min(Math.max(maxChargingProcesses, 1), 1000);
        int minPark = minParkMin == null ? DEFAULT_MIN_PARK_MIN : Math.max(minParkMin, 0);
        int micro = microDriveThresholdMin == null ? DEFAULT_MICRO_DRIVE_MIN : Math.max(microDriveThresholdMin, 0);

        List<DriveEntity> windowDrives = loadOverlappingDrives(carId, from, to, driveLimit);
        List<DriveEntity> neighborBefore = loadNeighborBefore(carId, from);
        List<DriveEntity> neighborAfter = loadNeighborAfter(carId, to);
        List<DriveEntity> forParks = mergeUnique(neighborBefore, windowDrives, neighborAfter);

        List<ParkGap> parks = ParkComposer.compose(forParks, from, to, now, micro, minPark);

        List<ChargingProcessEntity> charges = loadOverlappingCharges(carId, from, to, chargeLimit);
        Map<Long, ChargeEntity> sampleByProcess = chargeDao.findLatestPerProcess(
                        charges.stream().map(ChargingProcessEntity::id).toList())
                .stream()
                .collect(Collectors.toMap(ChargeEntity::chargingProcessId, Function.identity(), (a, b) -> a));

        List<Long> posIds = Stream.concat(
                        charges.stream().map(ChargingProcessEntity::positionId),
                        parks.stream().map(ParkGap::endPositionId)
                )
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, PositionEntity> posById = positionDao.findByIds(posIds).stream()
                .collect(Collectors.toMap(PositionEntity::id, Function.identity(), (a, b) -> a));

        List<Long> pathDriveIds = windowDrives.stream().map(DriveEntity::id).toList();
        Map<Long, List<PositionPathPoint>> byDrive = (pathDriveIds.isEmpty()
                ? List.<PositionPathPoint>of()
                : positionDao.findPathPointsByDriveIds(pathDriveIds))
                .stream()
                .filter(p -> p.driveId() != null)
                .collect(Collectors.groupingBy(PositionPathPoint::driveId, LinkedHashMap::new, Collectors.toList()));

        List<MapTracksDto.Feature> features = new ArrayList<>();
        int totalPts = 0;
        for (DriveEntity d : windowDrives) {
            List<List<BigDecimal>> coords = new ArrayList<>();
            for (PositionPathPoint p : byDrive.getOrDefault(d.id(), List.of())) {
                coords.add(List.of(p.longitude(), p.latitude()));
            }
            if (coords.size() < 2) {
                continue;
            }
            totalPts += coords.size();
            Map<String, Object> props = new HashMap<>();
            props.put("startDate", iso(d.startDate()));
            props.put("endDate", iso(d.endDate()));
            props.put("distance", UnitConverter.length(d.distance(), u));
            props.put("durationMin", d.durationMin());
            props.put("incomplete", d.endDate() == null);
            props.put("units", u.toMeta());
            features.add(MapTracksDto.driveLine(d.id(), coords, props));
        }

        int parkIdx = 0;
        for (ParkGap park : parks) {
            PositionEntity p = park.endPositionId() == null ? null : posById.get(park.endPositionId());
            if (p == null || p.longitude() == null || p.latitude() == null) {
                continue;
            }
            Map<String, Object> props = new HashMap<>();
            props.put("startDate", iso(park.start()));
            props.put("endDate", iso(park.end()));
            props.put("durationMin", park.durationMin());
            props.put("afterDriveId", park.afterDriveId());
            features.add(MapTracksDto.parkPoint(parkIdx++, p.longitude(), p.latitude(), props));
        }

        for (ChargingProcessEntity c : charges) {
            PositionEntity p = c.positionId() == null ? null : posById.get(c.positionId());
            if (p == null || p.longitude() == null || p.latitude() == null) {
                continue;
            }
            ChargeEntity sample = sampleByProcess.get(c.id());
            Map<String, Object> props = new HashMap<>();
            props.put("startDate", iso(c.startDate()));
            props.put("endDate", iso(c.endDate()));
            props.put("durationMin", c.durationMin());
            props.put("chargeEnergyAdded", c.chargeEnergyAdded());
            props.put("cost", c.cost());
            props.put("chargeType", chargeType(sample));
            props.put("incomplete", c.endDate() == null);
            features.add(MapTracksDto.chargePoint(c.id(), p.longitude(), p.latitude(), props));
        }

        int parkCount = (int) features.stream().filter(f -> "park".equals(f.properties().get("kind"))).count();
        int chargeCount = (int) features.stream().filter(f -> "charge".equals(f.properties().get("kind"))).count();
        int driveCount = (int) features.stream().filter(f -> "drive".equals(f.properties().get("kind"))).count();
        return new MapTracksDto("FeatureCollection", features,
                new MapTracksDto.Meta(carId, from, to, driveCount, chargeCount, parkCount, totalPts));
    }

    private List<DriveEntity> loadOverlappingDrives(long carId, Instant from, Instant to, int limit) {
        DriveSearchCondition cond = DriveSearchCondition.builder()
                .carId(carId)
                .overlapping(from, to)
                .oldestFirst()
                .build();
        return driveDao.findByIdsOrdered(driveDao.findIds(cond, limit, 0));
    }

    private List<DriveEntity> loadNeighborBefore(long carId, Instant from) {
        DriveSearchCondition cond = DriveSearchCondition.builder()
                .carId(carId)
                .startDateTo(from)
                .build();
        List<Long> ids = driveDao.findIds(cond, 1, 0);
        return driveDao.findByIdsOrdered(ids);
    }

    private List<DriveEntity> loadNeighborAfter(long carId, Instant to) {
        DriveSearchCondition cond = DriveSearchCondition.builder()
                .carId(carId)
                .startDateFrom(to)
                .oldestFirst()
                .build();
        List<Long> ids = driveDao.findIds(cond, 1, 0);
        return driveDao.findByIdsOrdered(ids);
    }

    private List<ChargingProcessEntity> loadOverlappingCharges(long carId, Instant from, Instant to, int limit) {
        ChargingProcessSearchCondition cond = ChargingProcessSearchCondition.builder()
                .carId(carId)
                .overlapping(from, to)
                .build();
        return chargingProcessDao.findByIdsOrdered(chargingProcessDao.findIds(cond, limit, 0));
    }

    @SafeVarargs
    private static List<DriveEntity> mergeUnique(List<DriveEntity>... parts) {
        Map<Long, DriveEntity> byId = new LinkedHashMap<>();
        for (List<DriveEntity> part : parts) {
            for (DriveEntity d : part) {
                byId.putIfAbsent(d.id(), d);
            }
        }
        return List.copyOf(byId.values());
    }

    static String chargeType(ChargeEntity sample) {
        if (sample == null) {
            return null;
        }
        if (Boolean.TRUE.equals(sample.fastChargerPresent())) {
            return "dc";
        }
        if (sample.chargerPhases() == null || sample.chargerPhases() == 0) {
            return "dc";
        }
        return "ac";
    }

    private static String iso(Instant t) {
        return t == null ? null : t.toString();
    }
}
