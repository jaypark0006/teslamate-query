package com.teslamate.query.api.v1;

import com.teslamate.query.dto.PageResponse;
import com.teslamate.query.dto.PositionDto;
import com.teslamate.query.service.PositionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/positions")
@Tag(name = "Positions")
public class PositionController {

    private final PositionService positionService;

    public PositionController(PositionService positionService) {
        this.positionService = positionService;
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
            @RequestParam(required = false) Integer size
    ) {
        return positionService.list(carId, driveId, from, to, cleanOnly, page, size);
    }

    @GetMapping("/{id}")
    public PositionDto get(@PathVariable long id) {
        return positionService.get(id);
    }
}
