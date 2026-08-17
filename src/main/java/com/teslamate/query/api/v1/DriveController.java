package com.teslamate.query.api.v1;

import com.teslamate.query.dto.DriveDto;
import com.teslamate.query.dto.DrivePositionDto;
import com.teslamate.query.dto.PageResponse;
import com.teslamate.query.dto.TirePressureSampleDto;
import com.teslamate.query.service.DriveService;
import com.teslamate.query.service.QuerySupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/drives")
@Tag(name = "Drives")
public class DriveController {

    private final DriveService driveService;
    private final QuerySupport support;

    public DriveController(DriveService driveService, QuerySupport support) {
        this.driveService = driveService;
        this.support = support;
    }

    @GetMapping
    @Operation(summary = "List drives (Condition → ids → rows). DB is metric; pass lengthUnit/tempUnit to convert.")
    public PageResponse<DriveDto> list(
            @RequestParam(required = false) Long carId,
            @Parameter(description = "start_date >= from (not interval overlap; see /map/trip)")
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @Parameter(description = "Min distance in lengthUnit (default km); converted to km for SQL")
            @RequestParam(required = false) Double minDistance,
            @RequestParam(required = false) Integer minDuration,
            @RequestParam(required = false) Long geofenceId,
            @RequestParam(required = false) Boolean incompleteOnly,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String lengthUnit,
            @RequestParam(required = false) String tempUnit
    ) {
        return driveService.list(carId, from, to, minDistance, minDuration, geofenceId,
                incompleteOnly, page, size, support.units(lengthUnit, tempUnit));
    }

    @GetMapping("/{driveId}")
    public DriveDto get(
            @PathVariable long driveId,
            @RequestParam(required = false) String lengthUnit,
            @RequestParam(required = false) String tempUnit
    ) {
        return driveService.get(driveId, support.units(lengthUnit, tempUnit));
    }

    @GetMapping("/{driveId}/positions")
    @Operation(summary = "All GPS points of this drive (not GET /positions/{driveId})")
    public List<DrivePositionDto> positions(
            @PathVariable long driveId,
            @RequestParam(required = false) Integer downsample,
            @RequestParam(required = false) String lengthUnit,
            @RequestParam(required = false) String tempUnit
    ) {
        return driveService.positions(driveId, downsample, support.units(lengthUnit, tempUnit));
    }

    @GetMapping("/{driveId}/tire-pressures")
    @Operation(summary = "Tire-pressure samples for one drive")
    public List<TirePressureSampleDto> tirePressures(
            @PathVariable long driveId,
            @RequestParam(required = false) Integer downsample
    ) {
        return driveService.tirePressures(driveId, downsample);
    }
}
