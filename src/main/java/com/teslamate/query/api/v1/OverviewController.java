package com.teslamate.query.api.v1;

import com.teslamate.query.dto.OverviewDto;
import com.teslamate.query.service.OverviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/overview")
@Tag(name = "Overview")
public class OverviewController {

    private final OverviewService overviewService;

    public OverviewController(OverviewService overviewService) {
        this.overviewService = overviewService;
    }

    @GetMapping
    @Operation(summary = "Overview KPIs for a car and time range")
    public OverviewDto get(
            @RequestParam long carId,
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(required = false) String range
    ) {
        return overviewService.get(carId, from, to, range);
    }
}
