package com.teslamate.query.api.v1;

import com.teslamate.query.dto.GeofenceDto;
import com.teslamate.query.service.GeofenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/geofences")
@Tag(name = "Geofences")
public class GeofenceController {

    private final GeofenceService geofenceService;

    public GeofenceController(GeofenceService geofenceService) {
        this.geofenceService = geofenceService;
    }

    @GetMapping
    @Operation(summary = "List geofences")
    public List<GeofenceDto> list() {
        return geofenceService.list();
    }
}
