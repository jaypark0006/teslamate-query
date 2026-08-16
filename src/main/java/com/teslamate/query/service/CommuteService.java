package com.teslamate.query.service;

import com.teslamate.query.dao.CarDao;
import com.teslamate.query.dao.DriveDao;
import com.teslamate.query.dao.PositionDao;
import com.teslamate.query.db.condition.DriveSearchCondition;
import com.teslamate.query.dto.CommuteSampleDto;
import com.teslamate.query.entity.DriveEntity;
import com.teslamate.query.entity.PositionCommutePoint;
import com.teslamate.query.exception.NotFoundException;
import com.teslamate.query.service.trip.CommuteCompare;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CommuteService {

    private static final Logger log = LoggerFactory.getLogger(CommuteService.class);
    static final int MIN_DURATION_MIN = 8;
    static final double MIN_KM = 2.0;
    static final int DRIVE_SCAN = 400;

    private final CarDao carDao;
    private final DriveDao driveDao;
    private final PositionDao positionDao;

    public CommuteService(CarDao carDao, DriveDao driveDao, PositionDao positionDao) {
        this.carDao = carDao;
        this.driveDao = driveDao;
        this.positionDao = positionDao;
    }

    public List<CommuteSampleDto> compare(
            long carId, Instant from, Instant to, int startAfterMin, int startBeforeMin,
            int stepSec, Integer elapsedMin, ZoneId zone) {
        carDao.findById(carId).orElseThrow(() -> new NotFoundException("Car not found: " + carId));
        ZoneId z = zone == null ? ZoneId.of("Asia/Shanghai") : zone;
        int step = Math.min(Math.max(stepSec, 15), 180);
        DriveSearchCondition cond = DriveSearchCondition.builder()
                .carId(carId)
                .startDateFrom(from)
                .startDateTo(to)
                .completedOnly(true)
                .minDuration(MIN_DURATION_MIN)
                .minDistance(MIN_KM)
                .oldestFirst()
                .build();
        List<DriveEntity> scanned = driveDao.findByIdsOrdered(driveDao.findIds(cond, DRIVE_SCAN, 0));
        List<DriveEntity> picked = CommuteCompare.pickOnePerDay(scanned, startAfterMin, startBeforeMin, z);
        if (picked.isEmpty()) {
            return List.of();
        }
        List<Long> ids = picked.stream().map(DriveEntity::id).toList();
        List<PositionCommutePoint> rows = positionDao.findCommutePointsByDriveIds(ids, step);
        Map<Long, List<PositionCommutePoint>> byDrive = new LinkedHashMap<>();
        for (PositionCommutePoint p : rows) {
            if (p.driveId() == null) {
                continue;
            }
            byDrive.computeIfAbsent(p.driveId(), id -> new ArrayList<>()).add(p);
        }
        List<CommuteSampleDto> out = new ArrayList<>();
        for (DriveEntity d : picked) {
            out.addAll(CommuteCompare.resample(
                    d, byDrive.getOrDefault(d.id(), List.of()), step, elapsedMin, z));
        }
        out.sort(CommuteCompare.byDayThenElapsed());
        log.info("commute car={} days={} clock={}-{} elapsed={} step={}s samples={}",
                carId, picked.size(), startAfterMin, startBeforeMin, elapsedMin, step, out.size());
        return out;
    }
}
