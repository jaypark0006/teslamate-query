package com.teslamate.query.api.v1;

import com.teslamate.query.dto.DailyOccupancyDto;
import com.teslamate.query.dto.DayGridCellDto;
import com.teslamate.query.dto.MapPointDto;
import com.teslamate.query.dto.MapTracksDto;
import com.teslamate.query.dto.TimelineItemDto;
import com.teslamate.query.service.QuerySupport;
import com.teslamate.query.service.TripViewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cars/{carId}")
@Tag(name = "Trip map / timeline")
public class TripViewController {

    private final TripViewService tripViewService;
    private final QuerySupport support;

    public TripViewController(TripViewService tripViewService, QuerySupport support) {
        this.tripViewService = tripViewService;
        this.support = support;
    }

    @GetMapping("/timeline")
    @Operation(summary = "Chronological DRIVE / CHARGE / PARK log for the Grafana time range")
    public List<TimelineItemDto> timeline(
            @PathVariable long carId,
            @Parameter(description = "Window start (ISO-8601 UTC)") @RequestParam String from,
            @Parameter(description = "Window end (ISO-8601 UTC)") @RequestParam String to,
            @Parameter(description = "Omit parks shorter than this many minutes")
            @RequestParam(required = false) String minParkMin,
            @RequestParam(required = false) String timezone,
            @RequestParam(required = false) String lengthUnit,
            @RequestParam(required = false) String tempUnit
    ) {
        return tripViewService.timeline(carId, from, to, support.minParkMin(minParkMin),
                support.units(lengthUnit, tempUnit), support.zone(timezone));
    }

    @GetMapping("/timeline/daily")
    @Operation(summary = "Hours per local day of DRIVE / CHARGE / PARK (same window as /timeline)")
    public List<DailyOccupancyDto> daily(
            @PathVariable long carId,
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(required = false) String minParkMin,
            @RequestParam(required = false) String timezone,
            @RequestParam(required = false) String lengthUnit,
            @RequestParam(required = false) String tempUnit
    ) {
        return tripViewService.dailyOccupancy(carId, from, to, support.minParkMin(minParkMin),
                support.units(lengthUnit, tempUnit), support.zone(timezone));
    }

    @GetMapping("/timeline/grid")
    @Operation(summary = "Day × hour slots of DRIVE / CHARGE / PARK (day starts at dayStartHour in timezone)")
    public List<DayGridCellDto> grid(
            @PathVariable long carId,
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(required = false) String minParkMin,
            @RequestParam(required = false) String timezone,
            @RequestParam(required = false) String dayStartHour,
            @RequestParam(required = false) String lengthUnit,
            @RequestParam(required = false) String tempUnit
    ) {
        return tripViewService.grid(carId, from, to, support.minParkMin(minParkMin),
                support.units(lengthUnit, tempUnit), support.zone(timezone),
                support.dayStartHour(dayStartHour));
    }

    @GetMapping("/map")
    @Operation(summary = "Trip GeoJSON: drive LineStrings, charge/park points, direction chevrons")
    public MapTracksDto map(
            @PathVariable long carId,
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(required = false) String minParkMin,
            @RequestParam(required = false) String lengthUnit,
            @RequestParam(required = false) String tempUnit
    ) {
        return tripViewService.geoJson(carId, from, to, support.minParkMin(minParkMin),
                support.units(lengthUnit, tempUnit));
    }

    @GetMapping("/map/points")
    @Operation(summary = "Flat lat/lon rows for Grafana Geomap Route + Markers")
    public List<MapPointDto> points(
            @PathVariable long carId,
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(required = false) String minParkMin,
            @Parameter(description = "Comma list: drive,charge,park")
            @RequestParam(required = false) String kinds,
            @RequestParam(required = false) String lengthUnit,
            @RequestParam(required = false) String tempUnit
    ) {
        return tripViewService.points(carId, from, to, support.minParkMin(minParkMin), kinds,
                support.units(lengthUnit, tempUnit));
    }

    @GetMapping("/map/focus")
    @Operation(summary = "Map points for a focused trip (day+slot from the grid, or from/to from the timeline)")
    public List<MapPointDto> focus(
            @PathVariable long carId,
            @RequestParam(required = false) String day,
            @RequestParam(required = false) String slot,
            @RequestParam(required = false) String kind,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String minParkMin,
            @RequestParam(required = false) String timezone,
            @RequestParam(required = false) String dayStartHour,
            @RequestParam(required = false) String lengthUnit,
            @RequestParam(required = false) String tempUnit
    ) {
        return tripViewService.focus(carId, day, slot, kind, from, to, support.minParkMin(minParkMin),
                support.units(lengthUnit, tempUnit), support.zone(timezone),
                support.dayStartHour(dayStartHour));
    }
}
