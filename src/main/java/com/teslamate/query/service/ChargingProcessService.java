package com.teslamate.query.service;

import com.teslamate.query.dto.ChargeSampleDto;
import com.teslamate.query.dto.ChargingProcessDto;
import com.teslamate.query.dto.PageResponse;
import com.teslamate.query.exception.NotFoundException;
import com.teslamate.query.repository.ChargingProcessRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class ChargingProcessService {

    private final ChargingProcessRepository repository;
    private final QuerySupport support;

    public ChargingProcessService(ChargingProcessRepository repository, QuerySupport support) {
        this.repository = repository;
        this.support = support;
    }

    public PageResponse<ChargingProcessDto> list(Long carId, String fromStr, String toStr, Long geofenceId,
                                                 String chargeType, Boolean incompleteOnly,
                                                 Integer page, Integer size) {
        Instant from = support.parseInstant(fromStr, "from");
        Instant to = support.parseInstant(toStr, "to");
        int p = support.page(page);
        int s = support.size(size);

        long total = repository.count(carId, from, to, geofenceId, chargeType, incompleteOnly);
        List<ChargingProcessDto> data = repository.find(carId, from, to, geofenceId, chargeType,
                incompleteOnly, s, support.offset(p, s));
        return PageResponse.of(data, p, s, total);
    }

    @Cacheable(value = "chargingProcess", key = "#id")
    public ChargingProcessDto get(long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Charging process not found: " + id));
    }

    public List<ChargeSampleDto> samples(long id) {
        get(id);
        return repository.findSamples(id);
    }
}
