package com.teslamate.query.service;

import com.teslamate.query.dao.ChargeDao;
import com.teslamate.query.dao.ChargingProcessDao;
import com.teslamate.query.db.condition.ChargeSearchCondition;
import com.teslamate.query.db.condition.ChargingProcessSearchCondition;
import com.teslamate.query.domain.units.DisplayUnits;
import com.teslamate.query.dto.ChargeDto;
import com.teslamate.query.dto.PageResponse;
import com.teslamate.query.exception.BadRequestException;
import com.teslamate.query.exception.NotFoundException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class ChargeService {

    private final ChargeDao chargeDao;
    private final ChargingProcessDao chargingProcessDao;
    private final QuerySupport support;

    public ChargeService(ChargeDao chargeDao, ChargingProcessDao chargingProcessDao, QuerySupport support) {
        this.chargeDao = chargeDao;
        this.chargingProcessDao = chargingProcessDao;
        this.support = support;
    }

    public PageResponse<ChargeDto> list(Long chargingProcessId, Long carId, String fromStr, String toStr,
                                        Integer page, Integer size, DisplayUnits units) {
        DisplayUnits u = units == null ? DisplayUnits.METRIC : units;
        Instant from = support.parseInstant(fromStr, "from");
        Instant to = support.parseInstant(toStr, "to");
        if (chargingProcessId == null && (carId == null || from == null || to == null)) {
            throw new BadRequestException(
                    "charges list requires chargingProcessId, or carId with from and to (ISO-8601)");
        }
        if (from != null && to != null && from.isAfter(to)) {
            throw new BadRequestException("from must be before to");
        }
        ChargeSearchCondition.Builder builder = ChargeSearchCondition.builder()
                .chargingProcessId(chargingProcessId)
                .dateFrom(from)
                .dateTo(to);
        if (chargingProcessId == null) {
            List<Long> processIds = chargingProcessDao.findIds(
                    ChargingProcessSearchCondition.builder()
                            .carId(carId)
                            .startDateFrom(from)
                            .startDateTo(to)
                            .build(),
                    2000, 0);
            if (processIds.isEmpty()) {
                int p = support.page(page);
                int s = support.size(size);
                return PageResponse.of(List.of(), p, s, 0, u.toMeta());
            }
            builder.chargingProcessIds(processIds);
        }
        ChargeSearchCondition condition = builder.build();
        int p = support.page(page);
        int s = support.size(size);
        long total = chargeDao.count(condition);
        List<Long> ids = chargeDao.findIds(condition, s, support.offset(p, s));
        return PageResponse.of(
                EntityMapper.toChargeDtos(chargeDao.findByIdsOrdered(ids), u), p, s, total, u.toMeta());
    }

    public ChargeDto get(long id, DisplayUnits units) {
        DisplayUnits u = units == null ? DisplayUnits.METRIC : units;
        return chargeDao.findById(id)
                .map(e -> EntityMapper.toChargeDto(e, u))
                .orElseThrow(() -> new NotFoundException("Charge not found: " + id));
    }
}
