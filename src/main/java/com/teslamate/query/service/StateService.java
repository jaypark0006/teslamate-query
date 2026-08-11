package com.teslamate.query.service;

import com.teslamate.query.dao.StateDao;
import com.teslamate.query.db.condition.StateSearchCondition;
import com.teslamate.query.dto.PageResponse;
import com.teslamate.query.dto.StateDto;
import com.teslamate.query.exception.NotFoundException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class StateService {

    private final StateDao stateDao;
    private final QuerySupport support;

    public StateService(StateDao stateDao, QuerySupport support) {
        this.stateDao = stateDao;
        this.support = support;
    }

    public PageResponse<StateDto> list(Long carId, String fromStr, String toStr, Integer page, Integer size) {
        Instant from = support.parseInstant(fromStr, "from");
        Instant to = support.parseInstant(toStr, "to");
        StateSearchCondition condition = StateSearchCondition.builder()
                .carId(carId)
                .overlapping(from, to)
                .build();
        int p = support.page(page);
        int s = support.size(size);
        long total = stateDao.count(condition);
        List<Long> ids = stateDao.findIds(condition, s, support.offset(p, s));
        return PageResponse.of(EntityMapper.toStateDtos(stateDao.findByIdsOrdered(ids)), p, s, total);
    }

    public StateDto get(long id) {
        return stateDao.findById(id)
                .map(EntityMapper::toStateDto)
                .orElseThrow(() -> new NotFoundException("State not found: " + id));
    }
}
