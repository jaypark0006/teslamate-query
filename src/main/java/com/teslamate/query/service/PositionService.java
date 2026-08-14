package com.teslamate.query.service;

import com.teslamate.query.dao.PositionDao;
import com.teslamate.query.db.condition.PositionSearchCondition;
import com.teslamate.query.domain.units.DisplayUnits;
import com.teslamate.query.dto.PageResponse;
import com.teslamate.query.dto.PositionDto;
import com.teslamate.query.exception.BadRequestException;
import com.teslamate.query.exception.NotFoundException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class PositionService {

    private final PositionDao positionDao;
    private final QuerySupport support;

    public PositionService(PositionDao positionDao, QuerySupport support) {
        this.positionDao = positionDao;
        this.support = support;
    }

    public PageResponse<PositionDto> list(Long carId, Long driveId, String fromStr, String toStr,
                                          Boolean cleanOnly, Integer page, Integer size, DisplayUnits units) {
        DisplayUnits u = units == null ? DisplayUnits.METRIC : units;
        Instant from = support.parseInstant(fromStr, "from");
        Instant to = support.parseInstant(toStr, "to");
        if (driveId == null && (carId == null || from == null || to == null)) {
            throw new BadRequestException(
                    "positions list requires driveId, or carId with from and to (ISO-8601)");
        }
        if (from != null && to != null && from.isAfter(to)) {
            throw new BadRequestException("from must be before to");
        }
        PositionSearchCondition condition = PositionSearchCondition.builder()
                .carId(carId)
                .driveId(driveId)
                .dateFrom(from)
                .dateTo(to)
                .cleanOnly(cleanOnly)
                .build();
        int p = support.page(page);
        int s = support.size(size);
        long total = positionDao.count(condition);
        List<Long> ids = positionDao.findIds(condition, s, support.offset(p, s));
        return PageResponse.of(
                EntityMapper.toPositionDtos(positionDao.findByIdsOrdered(ids), u), p, s, total, u.toMeta());
    }

    public PositionDto get(long id, DisplayUnits units) {
        DisplayUnits u = units == null ? DisplayUnits.METRIC : units;
        return positionDao.findById(id)
                .map(e -> EntityMapper.toPositionDto(e, u))
                .orElseThrow(() -> new NotFoundException("Position not found: " + id));
    }
}
