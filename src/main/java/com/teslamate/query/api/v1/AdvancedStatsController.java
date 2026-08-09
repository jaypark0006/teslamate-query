package com.teslamate.query.api.v1;

import com.teslamate.query.dto.*;
import com.teslamate.query.service.AdvancedQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/stats")
@Tag(name = "Statistics")
public class AdvancedStatsController {

    private final AdvancedQueryService service;

    public AdvancedStatsController(AdvancedQueryService service) {
        this.service = service;
    }

    @GetMapping("/vampire-drain")
    @Operation(summary = "Parked range loss between drives")
    public VampireDrainDto vampireDrain(
            @RequestParam long carId,
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(required = false) String range
    ) {
        return service.vampireDrain(carId, from, to, range);
    }

    @GetMapping("/projected-range")
    @Operation(summary = "Projected full battery range over time")
    public ProjectedRangeDto projectedRange(
            @RequestParam long carId,
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(required = false) String range
    ) {
        return service.projectedRange(carId, from, to, range);
    }

    @GetMapping("/battery-health")
    @Operation(summary = "Battery capacity estimates from charge sessions")
    public BatteryHealthDto batteryHealth(
            @RequestParam long carId,
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(required = false) String range
    ) {
        return service.batteryHealth(carId, from, to, range);
    }

    @GetMapping("/locations")
    @Operation(summary = "Visited locations / charge sites")
    public LocationStatsDto locations(
            @RequestParam long carId,
            @RequestParam String from,
            @RequestParam String to
    ) {
        return service.locations(carId, from, to);
    }
}
