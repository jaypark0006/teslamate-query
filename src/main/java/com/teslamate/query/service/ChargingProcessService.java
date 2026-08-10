package com.teslamate.query.service;

import com.teslamate.query.dto.ChargeSampleDto;
import com.teslamate.query.dto.ChargingProcessDto;
import com.teslamate.query.dto.ChargingProcessEnrichedDto;
import com.teslamate.query.dto.PageResponse;
import com.teslamate.query.exception.BadRequestException;
import com.teslamate.query.exception.NotFoundException;
import com.teslamate.query.repository.ChargingProcessRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class ChargingProcessService {

    private final ChargingProcessRepository repository;
    private final SettingsService settingsService;
    private final QuerySupport support;

    public ChargingProcessService(ChargingProcessRepository repository, SettingsService settingsService,
                                  QuerySupport support) {
        this.repository = repository;
        this.settingsService = settingsService;
        this.support = support;
    }

    public PageResponse<ChargingProcessDto> listLean(Long carId, String fromStr, String toStr, Long geofenceId,
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

    public PageResponse<ChargingProcessEnrichedDto> listEnriched(Long carId, String fromStr, String toStr,
                                                                 Long geofenceId, Boolean incompleteOnly,
                                                                 String range, Integer page, Integer size) {
        Instant from = support.parseInstant(fromStr, "from");
        Instant to = support.parseInstant(toStr, "to");
        int p = support.page(page);
        int s = support.size(size);
        String rangeMode = support.rangeMode(range, settingsService.preferredRangeOrDefault());
        long total = repository.count(carId, from, to, geofenceId, null, incompleteOnly);
        List<ChargingProcessEnrichedDto> data = repository.findEnriched(
                carId, from, to, geofenceId, incompleteOnly, rangeMode, s, support.offset(p, s));
        return PageResponse.of(data, p, s, total);
    }

    @Cacheable(value = "chargingProcess", key = "#id")
    public ChargingProcessDto getLean(long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Charging process not found: " + id));
    }

    public ChargingProcessEnrichedDto getEnriched(long id, String range) {
        String rangeMode = support.rangeMode(range, settingsService.preferredRangeOrDefault());
        return repository.findEnrichedById(id, rangeMode)
                .orElseThrow(() -> new NotFoundException("Charging process not found: " + id));
    }

    public List<ChargeSampleDto> samples(long id) {
        getLean(id);
        return repository.findSamples(id);
    }

    public static boolean isEnriched(String view) {
        return DriveService.isEnriched(view);
    }
}
