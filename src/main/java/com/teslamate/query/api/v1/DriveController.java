package com.teslamate.query.api.v1;

import com.teslamate.query.dto.DriveDto;
import com.teslamate.query.dto.DrivePositionDto;
import com.teslamate.query.dto.PageResponse;
import com.teslamate.query.service.DriveService;
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

    public DriveController(DriveService driveService) {
        this.driveService = driveService;
    }

    @GetMapping
    @Operation(summary = "List drives with filters (replaces Drives dashboard SQL)")
    public PageResponse<DriveDto> list(
            @RequestParam(required = false) Long carId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) Double minDistance,
            @RequestParam(required = false) Integer minDuration,
            @RequestParam(required = false) Long geofenceId,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) Boolean incompleteOnly,
            @Parameter(description = "ideal or rated; default from settings")
            @RequestParam(required = false) String range,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        return driveService.list(carId, from, to, minDistance, minDuration, geofenceId, location,
                incompleteOnly, range, page, size);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Drive detail")
    public DriveDto get(@PathVariable long id,
                        @RequestParam(required = false) String range) {
        return driveService.get(id, range);
    }

    @GetMapping("/{id}/positions")
    @Operation(summary = "Drive path / telemetry series")
    public List<DrivePositionDto> positions(
            @PathVariable long id,
            @Parameter(description = "Optional downsample bucket in seconds")
            @RequestParam(required = false) Integer downsample
    ) {
        return driveService.positions(id, downsample);
    }
}
