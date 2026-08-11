package com.teslamate.query.service;

import com.teslamate.query.dao.UpdateDao;
import com.teslamate.query.db.condition.UpdateSearchCondition;
import com.teslamate.query.dto.PageResponse;
import com.teslamate.query.dto.UpdateDto;
import com.teslamate.query.exception.NotFoundException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class UpdateService {

    private final UpdateDao updateDao;
    private final QuerySupport support;

    public UpdateService(UpdateDao updateDao, QuerySupport support) {
        this.updateDao = updateDao;
        this.support = support;
    }

    public PageResponse<UpdateDto> list(Long carId, String fromStr, String toStr, Integer page, Integer size) {
        Instant from = support.parseInstant(fromStr, "from");
        Instant to = support.parseInstant(toStr, "to");
        UpdateSearchCondition condition = UpdateSearchCondition.builder()
                .carId(carId)
                .startDateFrom(from)
                .startDateTo(to)
                .build();
        int p = support.page(page);
        int s = support.size(size);
        long total = updateDao.count(condition);
        List<Long> ids = updateDao.findIds(condition, s, support.offset(p, s));
        return PageResponse.of(EntityMapper.toUpdateDtos(updateDao.findByIdsOrdered(ids)), p, s, total);
    }

    public UpdateDto get(long id) {
        return updateDao.findById(id)
                .map(EntityMapper::toUpdateDto)
                .orElseThrow(() -> new NotFoundException("Update not found: " + id));
    }
}
