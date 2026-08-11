package com.teslamate.query.api.v1;

import com.teslamate.query.dto.GeofenceDto;
import com.teslamate.query.dto.PageResponse;
import com.teslamate.query.service.GeofenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/geofences")
@Tag(name = "Geofences")
public class GeofenceController {

    private final GeofenceService geofenceService;

    public GeofenceController(GeofenceService geofenceService) {
        this.geofenceService = geofenceService;
    }

    @GetMapping
    @Operation(summary = "List geofences (Condition → ids → rows)")
    public PageResponse<GeofenceDto> list(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        return geofenceService.list(name, page, size);
    }

    @GetMapping("/{id}")
    public GeofenceDto get(@PathVariable long id) {
        return geofenceService.get(id);
    }
}
