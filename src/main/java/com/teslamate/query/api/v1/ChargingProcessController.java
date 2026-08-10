package com.teslamate.query.api.v1;

import com.teslamate.query.dto.ChargeSampleDto;
import com.teslamate.query.dto.ChargingProcessDto;
import com.teslamate.query.dto.PageResponse;
import com.teslamate.query.service.ChargingProcessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Charging Processes")
public class ChargingProcessController {

    private final ChargingProcessService service;

    public ChargingProcessController(ChargingProcessService service) {
        this.service = service;
    }

    @GetMapping({"/charging-processes", "/charges"})
    @Operation(summary = "List charge sessions (Condition → ids → rows)")
    public PageResponse<ChargingProcessDto> list(
            @RequestParam(required = false) Long carId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) Long geofenceId,
            @RequestParam(required = false) Boolean incompleteOnly,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        return service.list(carId, from, to, geofenceId, incompleteOnly, page, size);
    }

    @GetMapping({"/charging-processes/{id}", "/charges/{id}"})
    public ChargingProcessDto get(@PathVariable long id) {
        return service.get(id);
    }

    @GetMapping({"/charging-processes/{id}/samples", "/charges/{id}/samples"})
    public List<ChargeSampleDto> samples(@PathVariable long id) {
        return service.samples(id);
    }
}
