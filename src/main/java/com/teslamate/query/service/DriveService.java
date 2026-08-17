package com.teslamate.query.service;

import com.teslamate.query.dao.DriveDao;
import com.teslamate.query.dao.PositionDao;
import com.teslamate.query.db.condition.DriveSearchCondition;
import com.teslamate.query.db.condition.PositionSearchCondition;
import com.teslamate.query.domain.time.UtcDateTimes;
import com.teslamate.query.domain.units.DisplayUnits;
import com.teslamate.query.dto.DriveDto;
import com.teslamate.query.dto.DrivePositionDto;
import com.teslamate.query.dto.PageResponse;
import com.teslamate.query.dto.TirePressureDto;
import com.teslamate.query.dto.TirePressureSampleDto;
import com.teslamate.query.entity.DriveEntity;
import com.teslamate.query.exception.NotFoundException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class DriveService {

    private static final int POSITION_CAP = 50_000;

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
        PositionSearchCondition cond = PositionSearchCondition.builder().driveId(id).build();
        List<Long> ids;
        if (downsampleSeconds != null && downsampleSeconds > 0) {
            ids = positionDao.findIdsBucketed(cond, downsampleSeconds, POSITION_CAP);
        } else {
            ids = positionDao.findIds(cond, POSITION_CAP, 0);
        }
        return positionDao.findByIdsOrdered(ids).stream()
                .map(e -> EntityMapper.toDrivePositionDto(e, u))
                .toList();
    }

    public List<TirePressureSampleDto> tirePressures(long id, Integer downsampleSeconds) {
        get(id, DisplayUnits.METRIC);
        int bucket = downsampleSeconds == null ? 0 : Math.max(downsampleSeconds, 0);
        return positionDao.findTirePressuresByDriveId(id, bucket, POSITION_CAP).stream()
                .map(p -> new TirePressureSampleDto(
                        UtcDateTimes.fromDatabase(p.date()),
                        new TirePressureDto(p.fl(), p.fr(), p.rl(), p.rr())))
                .toList();
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
