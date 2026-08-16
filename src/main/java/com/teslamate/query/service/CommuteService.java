package com.teslamate.query.service;

import com.teslamate.query.dao.CarDao;
import com.teslamate.query.dao.DriveDao;
import com.teslamate.query.dao.PositionDao;
import com.teslamate.query.db.condition.DriveSearchCondition;
import com.teslamate.query.dto.CommuteSampleDto;
import com.teslamate.query.dto.CommuteTripDto;
import com.teslamate.query.entity.DriveEntity;
import com.teslamate.query.entity.PositionCommutePoint;
import com.teslamate.query.exception.BadRequestException;
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

    public List<CommuteTripDto> trips(
            String carKey, Instant from, Instant to, int startAfterMin, int startBeforeMin, ZoneId zone) {
        ZoneId z = zone == null ? ZoneId.of("Asia/Shanghai") : zone;
        return pick(carKey, from, to, startAfterMin, startBeforeMin, z).stream()
                .map(d -> CommuteCompare.toTrip(d, z))
                .toList();
    }

    public List<CommuteSampleDto> compare(
            String carKey, Instant from, Instant to, int startAfterMin, int startBeforeMin,
            double stepKm, ZoneId zone) {
        ZoneId z = zone == null ? ZoneId.of("Asia/Shanghai") : zone;
        double step = Math.min(Math.max(stepKm, 0.2), 2.0);
        List<DriveEntity> picked = pick(carKey, from, to, startAfterMin, startBeforeMin, z);
        if (picked.isEmpty()) {
            return List.of();
        }
        List<Long> ids = picked.stream().map(DriveEntity::id).toList();
        List<PositionCommutePoint> rows = positionDao.findCommutePointsByDriveIds(ids, 20);
        Map<Long, List<PositionCommutePoint>> byDrive = new LinkedHashMap<>();
        for (PositionCommutePoint p : rows) {
            if (p.driveId() == null) {
                continue;
            }
            byDrive.computeIfAbsent(p.driveId(), id -> new ArrayList<>()).add(p);
        }
        List<CommuteSampleDto> out = new ArrayList<>();
        for (DriveEntity d : picked) {
            out.addAll(CommuteCompare.resampleByKm(
                    d, byDrive.getOrDefault(d.id(), List.of()), step, z));
        }
        out.sort(CommuteCompare.byDayThenKm());
        log.info("commute car={} days={} clock={}-{} stepKm={} samples={}",
                picked.getFirst().carId(), picked.size(), startAfterMin, startBeforeMin, step, out.size());
        return out;
    }

    private List<DriveEntity> pick(
            String carKey, Instant from, Instant to, int startAfterMin, int startBeforeMin, ZoneId zone) {
        long carId = requireCarId(carKey);
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
        return CommuteCompare.pickOnePerDay(scanned, startAfterMin, startBeforeMin, zone);
    }

    /** Grafana may send the numeric id or the VIN as {@code car_id}. */
    long requireCarId(String raw) {
        if (raw == null || raw.isBlank() || raw.contains("${")) {
            throw new BadRequestException("carId is required");
        }
        String s = raw.trim();
        if (s.chars().allMatch(Character::isDigit)) {
            long id = Long.parseLong(s);
            return carDao.findById(id)
                    .orElseThrow(() -> new NotFoundException("Car not found: " + id))
                    .id();
        }
        return carDao.findByVin(s)
                .orElseThrow(() -> new NotFoundException("Car not found: " + s))
                .id();
    }
}
