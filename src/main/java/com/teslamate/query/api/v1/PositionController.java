package com.teslamate.query.api.v1;

import com.teslamate.query.dto.PageResponse;
import com.teslamate.query.dto.PositionDto;
import com.teslamate.query.service.PositionService;
import com.teslamate.query.service.QuerySupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/positions")
@Tag(name = "Positions")
public class PositionController {

    private final PositionService positionService;
    private final QuerySupport support;

    public PositionController(PositionService positionService, QuerySupport support) {
        this.positionService = positionService;
        this.support = support;
    }

    @GetMapping
    @Operation(summary = "List positions (requires driveId, or carId+from+to)")
    public PageResponse<PositionDto> list(
            @RequestParam(required = false) Long carId,
            @RequestParam(required = false) Long driveId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) Boolean cleanOnly,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String lengthUnit,
            @RequestParam(required = false) String tempUnit
    ) {
        return positionService.list(carId, driveId, from, to, cleanOnly, page, size,
                support.units(lengthUnit, tempUnit));
    }

    @GetMapping("/{positionId}")
    public PositionDto get(
            @PathVariable long positionId,
            @RequestParam(required = false) String lengthUnit,
            @RequestParam(required = false) String tempUnit
    ) {
        return positionService.get(positionId, support.units(lengthUnit, tempUnit));
    }
}
