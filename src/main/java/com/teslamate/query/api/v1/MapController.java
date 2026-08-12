package com.teslamate.query.api.v1;

import com.teslamate.query.dto.MapTracksDto;
import com.teslamate.query.dto.PositionDto;
import com.teslamate.query.service.MapTracksService;
import com.teslamate.query.service.QuerySupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Map / Series (Grafana)")
public class MapController {

    private final MapTracksService mapTracksService;
    private final QuerySupport support;

    public MapController(MapTracksService mapTracksService, QuerySupport support) {
        this.mapTracksService = mapTracksService;
        this.support = support;
    }

    @GetMapping("/map/tracks")
    @Operation(summary = "Multi-drive paths + charge points as GeoJSON FeatureCollection")
    public MapTracksDto tracks(
            @RequestParam long carId,
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(required = false) Integer maxDrives,
            @RequestParam(required = false) Integer maxCharges,
            @RequestParam(required = false) String lengthUnit,
            @RequestParam(required = false) String tempUnit
    ) {
        return mapTracksService.tracks(carId, from, to, maxDrives, maxCharges,
                support.units(lengthUnit, tempUnit));
    }

    @GetMapping("/series/battery")
    @Operation(summary = "SOC / battery series for Charge Level dashboard")
    public List<PositionDto> battery(
            @RequestParam long carId,
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String lengthUnit,
            @RequestParam(required = false) String tempUnit
    ) {
        return mapTracksService.batterySeries(carId, from, to, limit,
                support.units(lengthUnit, tempUnit));
    }
}
