package com.teslamate.query.service;

import com.teslamate.query.dao.ChargingProcessDao;
import com.teslamate.query.db.condition.ChargingProcessSearchCondition;
import com.teslamate.query.domain.units.DisplayUnits;
import com.teslamate.query.dto.ChargingProcessDto;
import com.teslamate.query.dto.PageResponse;
import com.teslamate.query.exception.NotFoundException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class ChargingProcessService {

    private final ChargingProcessDao chargingProcessDao;
    private final QuerySupport support;

    public ChargingProcessService(ChargingProcessDao chargingProcessDao, QuerySupport support) {
        this.chargingProcessDao = chargingProcessDao;
        this.support = support;
    }

    public PageResponse<ChargingProcessDto> list(Long carId, String fromStr, String toStr, Long geofenceId,
                                                 Boolean incompleteOnly, Boolean excludeZeroEnergy,
                                                 Integer page, Integer size, DisplayUnits units) {
        DisplayUnits u = units == null ? DisplayUnits.METRIC : units;
        ChargingProcessSearchCondition condition =
                condition(carId, fromStr, toStr, geofenceId, incompleteOnly, excludeZeroEnergy);
        int p = support.page(page);
        int s = support.size(size);
        long total = chargingProcessDao.count(condition);
        List<Long> ids = chargingProcessDao.findIds(condition, s, support.offset(p, s));
        return PageResponse.of(
                EntityMapper.toChargingProcessDtos(chargingProcessDao.findByIdsOrdered(ids), u),
                p, s, total, u.toMeta());
    }

    public ChargingProcessDto get(long id, DisplayUnits units) {
        DisplayUnits u = units == null ? DisplayUnits.METRIC : units;
        return chargingProcessDao.findById(id)
                .map(e -> EntityMapper.toChargingProcessDto(e, u))
                .orElseThrow(() -> new NotFoundException("Charging process not found: " + id));
    }

    private ChargingProcessSearchCondition condition(Long carId, String fromStr, String toStr,
                                                     Long geofenceId, Boolean incompleteOnly,
                                                     Boolean excludeZeroEnergy) {
        Instant from = support.parseInstant(fromStr, "from");
        Instant to = support.parseInstant(toStr, "to");
        return ChargingProcessSearchCondition.builder()
                .carId(carId)
                .startDateFrom(from)
                .startDateTo(to)
                .geofenceId(geofenceId)
                .incompleteOnly(incompleteOnly)
                .excludeZeroEnergy(excludeZeroEnergy)
                .build();
    }
}
