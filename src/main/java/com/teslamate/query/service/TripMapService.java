package com.teslamate.query.service;

import com.teslamate.query.domain.units.DisplayUnits;
import com.teslamate.query.dto.MapTracksDto;
import com.teslamate.query.entity.ChargeEntity;
import org.springframework.stereotype.Service;

/**
 * Trip GeoJSON facade. Composition lives in {@link TripViewService}.
 */
@Service
public class TripMapService {

    private final TripViewService tripViewService;
    private final QuerySupport support;

    public TripMapService(TripViewService tripViewService, QuerySupport support) {
        this.tripViewService = tripViewService;
        this.support = support;
    }

    public MapTracksDto trip(long carId, String fromStr, String toStr,
                             Integer minParkMin, Integer microDriveThresholdMin,
                             Integer maxDrives, Integer maxChargingProcesses, DisplayUnits units) {
        var range = support.requireRange(fromStr, toStr);
        return tripViewService.geoJson(carId, range[0], range[1], minParkMin, units);
    }

    static String chargeType(ChargeEntity sample) {
        if (sample == null) {
            return null;
        }
        if (Boolean.TRUE.equals(sample.fastChargerPresent())) {
            return "dc";
        }
        if (sample.chargerPhases() == null || sample.chargerPhases() == 0) {
            return "dc";
        }
        return "ac";
    }
}
