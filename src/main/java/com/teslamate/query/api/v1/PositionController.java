package com.teslamate.query.api.v1;

import com.teslamate.query.dto.PageResponse;
import com.teslamate.query.dto.PositionDto;
import com.teslamate.query.service.AdvancedQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/positions")
@Tag(name = "Positions")
public class PositionController {

    private final AdvancedQueryService service;

    public PositionController(AdvancedQueryService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Positions in time range (requires from/to; supports downsample)")
    public PageResponse<PositionDto> list(
            @RequestParam long carId,
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(required = false, defaultValue = "true") Boolean cleanOnly,
            @RequestParam(required = false) Integer downsample,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        return service.positions(carId, from, to, cleanOnly, downsample, page, size);
    }
}
