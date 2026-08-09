package com.teslamate.query.service;

import com.teslamate.query.dto.ChargeSampleDto;
import com.teslamate.query.dto.ChargingProcessDto;
import com.teslamate.query.dto.ChargingProcessEnrichedDto;
import com.teslamate.query.dto.PageResponse;
import com.teslamate.query.dto.SettingsDto;
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

    public PageResponse<?> list(Long carId, String fromStr, String toStr, Long geofenceId,
                                String chargeType, Boolean incompleteOnly, String view, String range,
                                Integer page, Integer size) {
        Instant from = support.parseInstant(fromStr, "from");
        Instant to = support.parseInstant(toStr, "to");
        int p = support.page(page);
        int s = support.size(size);

        if (isEnriched(view)) {
            if (chargeType != null && !chargeType.isBlank()) {
                throw new BadRequestException("chargeType filter is only for lean view; use stats/charging for AC/DC breakdown");
            }
            String rangeMode = resolveRange(range);
            long total = repository.count(carId, from, to, geofenceId, null, incompleteOnly);
            List<ChargingProcessEnrichedDto> data = repository.findEnriched(
                    carId, from, to, geofenceId, incompleteOnly, rangeMode, s, support.offset(p, s));
            return PageResponse.of(data, p, s, total);
        }

        long total = repository.count(carId, from, to, geofenceId, chargeType, incompleteOnly);
        List<ChargingProcessDto> data = repository.find(carId, from, to, geofenceId, chargeType,
                incompleteOnly, s, support.offset(p, s));
        return PageResponse.of(data, p, s, total);
    }

    public Object get(long id, String view, String range) {
        if (isEnriched(view)) {
            return repository.findEnrichedById(id, resolveRange(range))
                    .orElseThrow(() -> new NotFoundException("Charging process not found: " + id));
        }
        return getLean(id);
    }

    @Cacheable(value = "chargingProcess", key = "#id")
    public ChargingProcessDto getLean(long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Charging process not found: " + id));
    }

    public List<ChargeSampleDto> samples(long id) {
        getLean(id);
        return repository.findSamples(id);
    }

    private boolean isEnriched(String view) {
        if (view == null || view.isBlank() || "lean".equalsIgnoreCase(view)) {
            return false;
        }
        if ("enriched".equalsIgnoreCase(view)) {
            return true;
        }
        throw new BadRequestException("view must be lean or enriched");
    }

    private String resolveRange(String range) {
        String preferred = "ideal";
        try {
            preferred = settingsService.get().preferredRange();
        } catch (Exception ignored) {
        }
        return support.rangeMode(range, preferred);
    }
}
