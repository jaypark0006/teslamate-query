package com.teslamate.query.service;

import com.teslamate.query.dao.ChargingProcessDao;
import com.teslamate.query.dao.DriveDao;
import com.teslamate.query.dao.PositionDao;
import com.teslamate.query.db.condition.ChargingProcessSearchCondition;
import com.teslamate.query.db.condition.DriveSearchCondition;
import com.teslamate.query.dto.MapTracksDto;
import com.teslamate.query.dto.PositionDto;
import com.teslamate.query.entity.ChargingProcessEntity;
import com.teslamate.query.entity.DriveEntity;
import com.teslamate.query.entity.PositionEntity;
import com.teslamate.query.exception.BadRequestException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class MapTracksService {

    private final DriveDao driveDao;
    private final ChargingProcessDao chargingProcessDao;
    private final PositionDao positionDao;
    private final QuerySupport support;

    public MapTracksService(DriveDao driveDao, ChargingProcessDao chargingProcessDao,
                            PositionDao positionDao, QuerySupport support) {
        this.driveDao = driveDao;
        this.chargingProcessDao = chargingProcessDao;
        this.positionDao = positionDao;
        this.support = support;
    }

    public MapTracksDto tracks(long carId, String fromStr, String toStr, Integer maxDrives, Integer maxCharges) {
        Instant[] range = support.requireRange(fromStr, toStr);
        Instant from = range[0];
        Instant to = range[1];
        int driveLimit = maxDrives == null ? 80 : Math.min(Math.max(maxDrives, 1), 500);
        int chargeLimit = maxCharges == null ? 200 : Math.min(Math.max(maxCharges, 1), 1000);

        var driveCond = DriveSearchCondition.builder().carId(carId).startDateFrom(from).startDateTo(to).build();
        List<Long> driveIds = driveDao.findIds(driveCond, driveLimit, 0);
        List<DriveEntity> drives = driveDao.findByIdsOrdered(driveIds);

        List<PositionEntity> pathPoints = driveIds.isEmpty() ? List.of() : positionDao.findByDriveIds(driveIds);
        Map<Long, List<PositionEntity>> byDrive = pathPoints.stream()
                .filter(p -> p.driveId() != null)
                .collect(Collectors.groupingBy(PositionEntity::driveId, LinkedHashMap::new, Collectors.toList()));

        var chargeCond = ChargingProcessSearchCondition.builder().carId(carId).startDateFrom(from).startDateTo(to).build();
        List<Long> chargeIds = chargingProcessDao.findIds(chargeCond, chargeLimit, 0);
        List<ChargingProcessEntity> charges = chargingProcessDao.findByIdsOrdered(chargeIds);

        List<Long> posIds = charges.stream().map(ChargingProcessEntity::positionId)
                .filter(Objects::nonNull).distinct().toList();
        Map<Long, PositionEntity> posById = (posIds.isEmpty() ? List.<PositionEntity>of() : positionDao.findByIds(posIds))
                .stream().collect(Collectors.toMap(PositionEntity::id, p -> p, (a, b) -> a));

        List<MapTracksDto.Feature> features = new ArrayList<>();
        int totalPts = 0;
        for (DriveEntity d : drives) {
            List<PositionEntity> pts = byDrive.getOrDefault(d.id(), List.of());
            List<List<BigDecimal>> coords = new ArrayList<>();
            for (PositionEntity p : pts) {
                if (p.longitude() != null && p.latitude() != null) {
                    coords.add(List.of(p.longitude(), p.latitude()));
                }
            }
            if (coords.size() < 2) {
                continue;
            }
            totalPts += coords.size();
            Map<String, Object> props = new HashMap<>();
            props.put("startDate", d.startDate() != null ? d.startDate().toString() : null);
            props.put("endDate", d.endDate() != null ? d.endDate().toString() : null);
            props.put("distanceKm", d.distance());
            props.put("durationMin", d.durationMin());
            features.add(MapTracksDto.lineString(d.id(), coords, props));
        }
        for (ChargingProcessEntity c : charges) {
            PositionEntity p = c.positionId() == null ? null : posById.get(c.positionId());
            if (p == null || p.longitude() == null || p.latitude() == null) {
                continue;
            }
            Map<String, Object> props = new HashMap<>();
            props.put("startDate", c.startDate() != null ? c.startDate().toString() : null);
            props.put("endDate", c.endDate() != null ? c.endDate().toString() : null);
            props.put("chargeEnergyAdded", c.chargeEnergyAdded());
            props.put("durationMin", c.durationMin());
            props.put("cost", c.cost());
            features.add(MapTracksDto.point(c.id(), p.longitude(), p.latitude(), props));
        }
        return new MapTracksDto("FeatureCollection", features,
                new MapTracksDto.Meta(carId, from, to, drives.size(), charges.size(), totalPts));
    }

    public List<PositionDto> batterySeries(long carId, String fromStr, String toStr, Integer limit) {
        Instant[] range = support.requireRange(fromStr, toStr);
        int lim = limit == null ? 5000 : Math.min(Math.max(limit, 1), 50_000);
        if (lim > 20_000) {
            throw new BadRequestException("limit max 20000 for battery series; narrow time range");
        }
        return positionDao.findCleanForCarInRange(carId, range[0], range[1], lim).stream()
                .map(EntityMapper::toPositionDto)
                .toList();
    }
}
