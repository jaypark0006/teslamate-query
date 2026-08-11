package com.teslamate.query.service;

import com.teslamate.query.dao.ChargeDao;
import com.teslamate.query.dao.ChargingProcessDao;
import com.teslamate.query.db.condition.ChargingProcessSearchCondition;
import com.teslamate.query.dto.ChargeSampleDto;
import com.teslamate.query.dto.ChargingProcessDto;
import com.teslamate.query.dto.PageResponse;
import com.teslamate.query.exception.NotFoundException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class ChargingProcessService {

    private final ChargingProcessDao chargingProcessDao;
    private final ChargeDao chargeDao;
    private final QuerySupport support;

    public ChargingProcessService(ChargingProcessDao chargingProcessDao, ChargeDao chargeDao, QuerySupport support) {
        this.chargingProcessDao = chargingProcessDao;
        this.chargeDao = chargeDao;
        this.support = support;
    }

    public PageResponse<ChargingProcessDto> list(Long carId, String fromStr, String toStr, Long geofenceId,
                                                 Boolean incompleteOnly, Integer page, Integer size) {
        ChargingProcessSearchCondition condition = condition(carId, fromStr, toStr, geofenceId, incompleteOnly);
        int p = support.page(page);
        int s = support.size(size);
        long total = chargingProcessDao.count(condition);
        List<Long> ids = chargingProcessDao.findIds(condition, s, support.offset(p, s));
        return PageResponse.of(
                EntityMapper.toChargingProcessDtos(chargingProcessDao.findByIdsOrdered(ids)), p, s, total);
    }

    @Cacheable(value = "chargingProcess", key = "#id")
    public ChargingProcessDto get(long id) {
        return chargingProcessDao.findById(id)
                .map(EntityMapper::toChargingProcessDto)
                .orElseThrow(() -> new NotFoundException("Charging process not found: " + id));
    }

    public List<ChargeSampleDto> samples(long id) {
        get(id);
        return chargeDao.findByProcessId(id).stream().map(EntityMapper::toChargeSampleDto).toList();
    }

    private ChargingProcessSearchCondition condition(Long carId, String fromStr, String toStr,
                                                     Long geofenceId, Boolean incompleteOnly) {
        Instant from = support.parseInstant(fromStr, "from");
        Instant to = support.parseInstant(toStr, "to");
        return ChargingProcessSearchCondition.builder()
                .carId(carId)
                .startDateFrom(from)
                .startDateTo(to)
                .geofenceId(geofenceId)
                .incompleteOnly(incompleteOnly)
                .build();
    }
}
