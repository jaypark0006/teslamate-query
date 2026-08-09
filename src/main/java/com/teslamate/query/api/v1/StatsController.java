package com.teslamate.query.api.v1;

import com.teslamate.query.dto.*;
import com.teslamate.query.service.StatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/stats")
@Tag(name = "Statistics")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/drives")
    @Operation(summary = "Drive statistics with time buckets")
    public DriveStatsDto drives(
            @RequestParam long carId,
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(required = false, defaultValue = "day") String groupBy,
            @RequestParam(required = false) String range
    ) {
        return statsService.driveStats(carId, from, to, groupBy, range);
    }

    @GetMapping("/charging")
    @Operation(summary = "Charging statistics, AC/DC breakdown, top stations")
    public ChargingStatsDto charging(
            @RequestParam long carId,
            @RequestParam String from,
            @RequestParam String to
    ) {
        return statsService.chargingStats(carId, from, to);
    }

    @GetMapping("/efficiency")
    @Operation(summary = "Net/gross efficiency and temp buckets")
    public EfficiencyStatsDto efficiency(
            @RequestParam long carId,
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(required = false) String range
    ) {
        return statsService.efficiency(carId, from, to, range);
    }

    @GetMapping("/period")
    @Operation(summary = "Period rollup table (Statistics dashboard)")
    public PeriodStatsDto period(
            @RequestParam long carId,
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(required = false, defaultValue = "month") String period,
            @RequestParam(required = false) String range
    ) {
        return statsService.period(carId, from, to, period, range);
    }

    @GetMapping("/mileage")
    @Operation(summary = "Odometer series from drives")
    public List<MileagePointDto> mileage(
            @RequestParam long carId,
            @RequestParam String from,
            @RequestParam String to
    ) {
        return statsService.mileage(carId, from, to);
    }
}
