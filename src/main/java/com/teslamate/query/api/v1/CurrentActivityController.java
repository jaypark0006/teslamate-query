package com.teslamate.query.api.v1;

import com.teslamate.query.dto.CurrentChargingDto;
import com.teslamate.query.dto.CurrentDriveDto;
import com.teslamate.query.dto.CurrentParkingDto;
import com.teslamate.query.dto.CurrentStatusDto;
import com.teslamate.query.dto.RecentChargeDto;
import com.teslamate.query.dto.RecentDriveDto;
import com.teslamate.query.service.CurrentActivityService;
import com.teslamate.query.service.RecentActivityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cars/{carId}")
@Tag(name = "Current Activity")
public class CurrentActivityController {

    private final CurrentActivityService service;
    private final RecentActivityService recent;

    public CurrentActivityController(CurrentActivityService service, RecentActivityService recent) {
        this.service = service;
        this.recent = recent;
    }

    @GetMapping("/current")
    @Operation(summary = "Current vehicle snapshot for the homepage (PARKING / DRIVING / CHARGING)")
    public CurrentStatusDto status(@PathVariable long carId) {
        return service.status(carId);
    }

    @GetMapping("/current/charging")
    @Operation(summary = "In-progress charging session; 404 if not charging")
    public CurrentChargingDto charging(@PathVariable long carId) {
        return service.charging(carId);
    }

    @GetMapping("/current/drive")
    @Operation(summary = "In-progress drive; 404 if not driving")
    public CurrentDriveDto drive(@PathVariable long carId) {
        return service.drive(carId);
    }

    @GetMapping("/current/parking")
    @Operation(summary = "In-progress parking; 404 if driving or charging")
    public CurrentParkingDto parking(@PathVariable long carId) {
        return service.parking(carId);
    }

    @GetMapping("/recent/drives")
    @Operation(summary = "Last completed drives; mergeGapMin>0 joins outings closer than that many minutes")
    public List<RecentDriveDto> recentDrives(
            @PathVariable long carId,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String mergeGapMin
    ) {
        return recent.recentDrives(carId, limit, mergeGapMin);
    }

    @GetMapping("/recent/charges")
    @Operation(summary = "Last completed charging sessions")
    public List<RecentChargeDto> recentCharges(
            @PathVariable long carId,
            @RequestParam(required = false) Integer limit
    ) {
        return recent.recentCharges(carId, limit);
    }
}
