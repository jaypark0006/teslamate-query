package com.teslamate.query.api.v1;

import com.teslamate.query.dto.MapPointDto;
import com.teslamate.query.dto.MapTracksDto;
import com.teslamate.query.dto.PositionDto;
import com.teslamate.query.dto.TimelineItemDto;
import com.teslamate.query.service.MapTracksService;
import com.teslamate.query.service.QuerySupport;
import com.teslamate.query.service.TripMapService;
import com.teslamate.query.service.TripViewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
    private final TripMapService tripMapService;
    private final TripViewService tripViewService;
    private final QuerySupport support;

    public MapController(MapTracksService mapTracksService, TripMapService tripMapService,
                         TripViewService tripViewService, QuerySupport support) {
        this.mapTracksService = mapTracksService;
        this.tripMapService = tripMapService;
        this.tripViewService = tripViewService;
        this.support = support;
    }

    @GetMapping("/map/tracks")
    @Operation(summary = "Drive paths + charging-session points. from/to filter start_date (not overlap). Prefer /map/trip for Trip view.")
    public MapTracksDto tracks(
            @RequestParam long carId,
            @Parameter(description = "start_date >= from") @RequestParam String from,
            @Parameter(description = "start_date <= to") @RequestParam String to,
            @RequestParam(required = false) Integer maxDrives,
            @Parameter(description = "Max charging sessions (not sample points)")
            @RequestParam(required = false) Integer maxChargingProcesses,
            @RequestParam(required = false) String lengthUnit,
            @RequestParam(required = false) String tempUnit
    ) {
        return mapTracksService.tracks(carId, from, to, maxDrives, maxChargingProcesses,
                support.units(lengthUnit, tempUnit));
    }

    @GetMapping("/map/trip")
    @Operation(summary = "Trip GeoJSON: drives + charging sessions + parks. from/to is interval overlap.")
    public MapTracksDto trip(
            @RequestParam long carId,
            @Parameter(description = "Overlap: start <= to AND (end is null OR end >= from)")
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(required = false) Integer minParkMin,
            @RequestParam(required = false) Integer microDriveThresholdMin,
            @RequestParam(required = false) Integer maxDrives,
            @Parameter(description = "Max charging sessions (not sample points)")
            @RequestParam(required = false) Integer maxChargingProcesses,
            @RequestParam(required = false) String lengthUnit,
            @RequestParam(required = false) String tempUnit
    ) {
        return tripMapService.trip(carId, from, to, minParkMin, microDriveThresholdMin,
                maxDrives, maxChargingProcesses, support.units(lengthUnit, tempUnit));
    }

    @GetMapping("/map/points")
    @Operation(summary = "Flat trip points for Grafana Geomap (same data as /cars/{carId}/map/points)")
    public List<MapPointDto> points(
            @RequestParam long carId,
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(required = false) String minParkMin,
            @RequestParam(required = false) String kinds,
            @RequestParam(required = false) String lengthUnit,
            @RequestParam(required = false) String tempUnit
    ) {
        return tripViewService.points(carId, from, to, support.minParkMin(minParkMin), kinds,
                support.units(lengthUnit, tempUnit));
    }

    @GetMapping("/map/timeline")
    @Operation(summary = "DRIVE / CHARGE / PARK log (same data as /cars/{carId}/timeline)")
    public List<TimelineItemDto> timeline(
            @RequestParam long carId,
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(required = false) String minParkMin,
            @RequestParam(required = false) String timezone,
            @RequestParam(required = false) String lengthUnit,
            @RequestParam(required = false) String tempUnit
    ) {
        return tripViewService.timeline(carId, from, to, support.minParkMin(minParkMin),
                support.units(lengthUnit, tempUnit), support.zone(timezone));
    }

    @GetMapping("/series/battery")
    @Operation(summary = "SOC series from positions (id in each row is positionId)")
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
