package com.teslamate.query.service;

import com.teslamate.query.dao.DriveDao;
import com.teslamate.query.dao.PositionDao;
import com.teslamate.query.db.condition.DriveSearchCondition;
import com.teslamate.query.dto.DriveDto;
import com.teslamate.query.dto.DrivePositionDto;
import com.teslamate.query.dto.PageResponse;
import com.teslamate.query.exception.NotFoundException;
import org.springframework.cache.annotation.Cacheable;
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
                                       Integer page, Integer size) {
        DriveSearchCondition condition = condition(carId, fromStr, toStr, minDistance, minDuration,
                geofenceId, incompleteOnly);
        int p = support.page(page);
        int s = support.size(size);
        long total = driveDao.count(condition);
        List<Long> ids = driveDao.findIds(condition, s, support.offset(p, s));
        return PageResponse.of(driveDao.findByIdsOrdered(ids), p, s, total);
    }

    @Cacheable(value = "drive", key = "#id")
    public DriveDto get(long id) {
        return driveDao.findById(id).orElseThrow(() -> new NotFoundException("Drive not found: " + id));
    }

    public List<DrivePositionDto> positions(long id, Integer downsampleSeconds) {
        get(id);
        List<DrivePositionDto> all = positionDao.findByDriveId(id);
        if (downsampleSeconds == null || downsampleSeconds <= 0 || all.size() <= 2) {
            return all;
        }
        long bucketMs = downsampleSeconds * 1000L;
        ArrayList<DrivePositionDto> out = new ArrayList<>();
        Long lastBucket = null;
        for (DrivePositionDto pt : all) {
            if (pt.date() == null) {
                continue;
            }
            long b = pt.date().toEpochMilli() / bucketMs;
            if (lastBucket == null || b != lastBucket) {
                out.add(pt);
                lastBucket = b;
            }
        }
        if (!all.isEmpty() && (out.isEmpty() || !out.getLast().id().equals(all.getLast().id()))) {
            out.add(all.getLast());
        }
        return out;
    }

    private DriveSearchCondition condition(Long carId, String fromStr, String toStr, Double minDistance,
                                           Integer minDuration, Long geofenceId, Boolean incompleteOnly) {
        Instant from = support.parseInstant(fromStr, "from");
        Instant to = support.parseInstant(toStr, "to");
        return DriveSearchCondition.builder()
                .carId(carId)
                .startDateFrom(from)
                .startDateTo(to)
                .minDistance(minDistance)
                .minDuration(minDuration)
                .geofenceId(geofenceId)
                .incompleteOnly(incompleteOnly)
                .build();
    }
}
