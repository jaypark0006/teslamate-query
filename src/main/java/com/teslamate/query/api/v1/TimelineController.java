package com.teslamate.query.api.v1;

import com.teslamate.query.dto.TimelineEventDto;
import com.teslamate.query.service.AdvancedQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/timeline")
@Tag(name = "Timeline")
public class TimelineController {

    private final AdvancedQueryService service;

    public TimelineController(AdvancedQueryService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Unified timeline of drives, charges, states, updates")
    public List<TimelineEventDto> list(
            @RequestParam long carId,
            @RequestParam String from,
            @RequestParam String to
    ) {
        return service.timeline(carId, from, to);
    }
}
