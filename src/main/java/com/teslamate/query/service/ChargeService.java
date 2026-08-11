package com.teslamate.query.service;

import com.teslamate.query.dao.ChargeDao;
import com.teslamate.query.db.condition.ChargeSearchCondition;
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
    private final QuerySupport support;

    public ChargeService(ChargeDao chargeDao, QuerySupport support) {
        this.chargeDao = chargeDao;
        this.support = support;
    }

    /**
     * List charge samples. Prefer {@code chargingProcessId}; otherwise require {@code from}+{@code to}.
     */
    public PageResponse<ChargeDto> list(Long chargingProcessId, String fromStr, String toStr,
                                        Integer page, Integer size) {
        Instant from = support.parseInstant(fromStr, "from");
        Instant to = support.parseInstant(toStr, "to");
        if (chargingProcessId == null && (from == null || to == null)) {
            throw new BadRequestException(
                    "charges list requires chargingProcessId, or from and to (ISO-8601)");
        }
        if (from != null && to != null && from.isAfter(to)) {
            throw new BadRequestException("from must be before to");
        }
        ChargeSearchCondition condition = ChargeSearchCondition.builder()
                .chargingProcessId(chargingProcessId)
                .dateFrom(from)
                .dateTo(to)
                .build();
        int p = support.page(page);
        int s = support.size(size);
        long total = chargeDao.count(condition);
        List<Long> ids = chargeDao.findIds(condition, s, support.offset(p, s));
        return PageResponse.of(EntityMapper.toChargeDtos(chargeDao.findByIdsOrdered(ids)), p, s, total);
    }

    public ChargeDto get(long id) {
        return chargeDao.findById(id)
                .map(EntityMapper::toChargeDto)
                .orElseThrow(() -> new NotFoundException("Charge not found: " + id));
    }
}
