package com.teslamate.query.api.v1;

import com.teslamate.query.dto.CommuteSampleDto;
import com.teslamate.query.dto.CommuteTripDto;
import com.teslamate.query.service.CommuteService;
import com.teslamate.query.service.QuerySupport;
import com.teslamate.query.service.trip.CommuteCompare;
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
@RequestMapping("/api/v1/cars/{carId}/commute")
@Tag(name = "Commute compare")
public class CommuteController {

    private final CommuteService commuteService;
    private final QuerySupport support;

    public CommuteController(CommuteService commuteService, QuerySupport support) {
        this.commuteService = commuteService;
        this.support = support;
    }

    @GetMapping("/trips")
    @Operation(summary = "Drives this page will compare (one per local day in the clock window)")
    public List<CommuteTripDto> trips(
            @PathVariable String carId,
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(required = false) String startAfter,
            @RequestParam(required = false) String startBefore,
            @RequestParam(required = false) String timezone
    ) {
        var range = support.requireRange(from, to);
        return commuteService.trips(carId, range[0], range[1],
                support.clockMinutes(startAfter, 5 * 60 + 30),
                support.clockMinutes(startBefore, 7 * 60),
                support.zone(timezone));
    }

    @GetMapping
    @Operation(summary = "Same clock-window outing each local day, sampled by elapsed minutes from that day's start")
    public List<CommuteSampleDto> compare(
            @PathVariable String carId,
            @RequestParam String from,
            @RequestParam String to,
            @Parameter(description = "Local start clock, inclusive (HH:mm). Default 05:30")
            @RequestParam(required = false) String startAfter,
            @Parameter(description = "Local start clock, inclusive (HH:mm). Default 07:00")
            @RequestParam(required = false) String startBefore,
            @Parameter(description = "Sample every N seconds of elapsed time (15–180). Default 60")
            @RequestParam(required = false) Integer stepSec,
            @Parameter(description = "If set, one pin per day at this elapsed minute (map overlay)")
            @RequestParam(required = false) Integer elapsedMin,
            @RequestParam(required = false) String timezone
    ) {
        var range = support.requireRange(from, to);
        int after = support.clockMinutes(startAfter, 5 * 60 + 30);
        int before = support.clockMinutes(startBefore, 7 * 60);
        int step = stepSec == null ? CommuteCompare.DEFAULT_STEP_SEC : stepSec;
        Integer at = elapsedMin == null || elapsedMin < 0 ? null : elapsedMin;
        return commuteService.compare(carId, range[0], range[1], after, before, step, at,
                support.zone(timezone));
    }
}
