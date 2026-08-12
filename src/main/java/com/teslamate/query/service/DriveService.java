package com.teslamate.query.service;

import com.teslamate.query.dao.DriveDao;
import com.teslamate.query.dao.PositionDao;
import com.teslamate.query.db.condition.DriveSearchCondition;
import com.teslamate.query.domain.units.DisplayUnits;
import com.teslamate.query.dto.DriveDto;
import com.teslamate.query.dto.DrivePositionDto;
import com.teslamate.query.dto.PageResponse;
import com.teslamate.query.entity.DriveEntity;
import com.teslamate.query.entity.PositionEntity;
import com.teslamate.query.exception.NotFoundException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class DriveService {

    private final DriveDao driveDao;
    private final PositionDao positionDao;
    private final QuerySupport support;

    public DriveService(DriveDao driveDao, PositionDao positionDao, QuerySupport support) {
        this.driveDao = driveDao;
        this.positionDao = positionDao;
        this.support = support;
    }

    public PageResponse<DriveDto> list(Long carId, String fromStr, String toStr, Double minDistance,
                                       Integer minDuration, Long geofenceId, Boolean incompleteOnly,
                                       Integer page, Integer size, DisplayUnits units) {
        DisplayUnits u = units == null ? DisplayUnits.METRIC : units;
        Double minDistanceKm = UnitConverter.toKm(minDistance, u);
        DriveSearchCondition condition = condition(carId, fromStr, toStr, minDistanceKm, minDuration,
                geofenceId, incompleteOnly);
        int p = support.page(page);
        int s = support.size(size);
        long total = driveDao.count(condition);
        List<Long> ids = driveDao.findIds(condition, s, support.offset(p, s));
        List<DriveEntity> rows = driveDao.findByIdsOrdered(ids);
        return PageResponse.of(EntityMapper.toDriveDtos(rows, u), p, s, total, u.toMeta());
    }

    public DriveDto get(long id, DisplayUnits units) {
        DisplayUnits u = units == null ? DisplayUnits.METRIC : units;
        return driveDao.findById(id)
                .map(e -> EntityMapper.toDriveDto(e, u))
                .orElseThrow(() -> new NotFoundException("Drive not found: " + id));
    }

    public List<DrivePositionDto> positions(long id, Integer downsampleSeconds, DisplayUnits units) {
        DisplayUnits u = units == null ? DisplayUnits.METRIC : units;
        get(id, DisplayUnits.METRIC);
        List<PositionEntity> all = positionDao.findByDriveId(id);
        List<DrivePositionDto> mapped = all.stream().map(e -> EntityMapper.toDrivePositionDto(e, u)).toList();
        if (downsampleSeconds == null || downsampleSeconds <= 0 || mapped.size() <= 2) {
            return mapped;
        }
        long bucketMs = downsampleSeconds * 1000L;
        ArrayList<DrivePositionDto> out = new ArrayList<>();
        Long lastBucket = null;
        for (DrivePositionDto pt : mapped) {
            if (pt.date() == null) {
                continue;
            }
            long b = pt.date().toEpochMilli() / bucketMs;
            if (lastBucket == null || b != lastBucket) {
                out.add(pt);
                lastBucket = b;
            }
        }
        if (!mapped.isEmpty() && (out.isEmpty() || !out.getLast().id().equals(mapped.getLast().id()))) {
            out.add(mapped.getLast());
        }
        return out;
    }

    private DriveSearchCondition condition(Long carId, String fromStr, String toStr, Double minDistanceKm,
                                           Integer minDuration, Long geofenceId, Boolean incompleteOnly) {
        Instant from = support.parseInstant(fromStr, "from");
        Instant to = support.parseInstant(toStr, "to");
        return DriveSearchCondition.builder()
                .carId(carId)
                .startDateFrom(from)
                .startDateTo(to)
                .minDistance(minDistanceKm)
                .minDuration(minDuration)
                .geofenceId(geofenceId)
                .incompleteOnly(incompleteOnly)
                .build();
    }
}
