package com.teslamate.query.api.v1;

import com.teslamate.query.dto.GeofenceDto;
import com.teslamate.query.dto.PageResponse;
import com.teslamate.query.service.GeofenceService;
import com.teslamate.query.service.QuerySupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/geofences")
@Tag(name = "Geofences")
public class GeofenceController {

    private final GeofenceService geofenceService;
    private final QuerySupport support;

    public GeofenceController(GeofenceService geofenceService, QuerySupport support) {
        this.geofenceService = geofenceService;
        this.support = support;
    }

    @GetMapping
    @Operation(summary = "List geofences (radius uses elevation unit: m or ft)")
    public PageResponse<GeofenceDto> list(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String lengthUnit,
            @RequestParam(required = false) String tempUnit
    ) {
        return geofenceService.list(name, page, size, support.units(lengthUnit, tempUnit));
    }

    @GetMapping("/{geofenceId}")
    public GeofenceDto get(
            @PathVariable long geofenceId,
            @RequestParam(required = false) String lengthUnit,
            @RequestParam(required = false) String tempUnit
    ) {
        return geofenceService.get(geofenceId, support.units(lengthUnit, tempUnit));
    }
}
