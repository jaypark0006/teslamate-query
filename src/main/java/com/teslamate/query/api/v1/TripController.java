package com.teslamate.query.api.v1;

import com.teslamate.query.dto.TripSummaryDto;
import com.teslamate.query.service.AdvancedQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/trips")
@Tag(name = "Trips")
public class TripController {

    private final AdvancedQueryService service;

    public TripController(AdvancedQueryService service) {
        this.service = service;
    }

    @GetMapping("/summary")
    @Operation(summary = "Trip window summary (Trip dashboard)")
    public TripSummaryDto summary(
            @RequestParam long carId,
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(required = false) String range
    ) {
        return service.tripSummary(carId, from, to, range);
    }
}
