package com.teslamate.query.api.v1;

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
    @Operation(summary = "List drives. Default lean; view=enriched for wide Grafana rows.")
    public PageResponse<?> list(
            @RequestParam(required = false) Long carId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) Double minDistance,
            @RequestParam(required = false) Integer minDuration,
            @RequestParam(required = false) Long geofenceId,
            @RequestParam(required = false) Boolean incompleteOnly,
            @Parameter(description = "lean (default) | enriched")
            @RequestParam(required = false) String view,
            @RequestParam(required = false) String range,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        if (DriveService.isEnriched(view)) {
            return driveService.listEnriched(carId, from, to, minDistance, minDuration, geofenceId,
                    incompleteOnly, range, page, size);
        }
        return driveService.listLean(carId, from, to, minDistance, minDuration, geofenceId,
                incompleteOnly, page, size);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Drive detail (lean or enriched)")
    public Object get(@PathVariable long id,
                      @RequestParam(required = false) String view,
                      @RequestParam(required = false) String range) {
        if (DriveService.isEnriched(view)) {
            return driveService.getEnriched(id, range);
        }
        return driveService.getLean(id);
    }

    @GetMapping("/{id}/positions")
    @Operation(summary = "Drive path / telemetry (use downsample for maps)")
    public List<DrivePositionDto> positions(
            @PathVariable long id,
            @RequestParam(required = false) Integer downsample
    ) {
        return driveService.positions(id, downsample);
    }
}
