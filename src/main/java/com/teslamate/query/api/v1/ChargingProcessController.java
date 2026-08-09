package com.teslamate.query.api.v1;

import com.teslamate.query.dto.ChargeSampleDto;
import com.teslamate.query.dto.PageResponse;
import com.teslamate.query.service.ChargingProcessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
    @Operation(summary = "List sessions. Default lean; view=enriched for wide Grafana rows.")
    public PageResponse<?> list(
            @RequestParam(required = false) Long carId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) Long geofenceId,
            @Parameter(description = "AC|DC filter (lean view only)")
            @RequestParam(required = false) String chargeType,
            @RequestParam(required = false) Boolean incompleteOnly,
            @Parameter(description = "lean (default) | enriched")
            @RequestParam(required = false) String view,
            @RequestParam(required = false) String range,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        return service.list(carId, from, to, geofenceId, chargeType, incompleteOnly, view, range, page, size);
    }

    @GetMapping({"/charging-processes/{id}", "/charges/{id}"})
    @Operation(summary = "Session detail (lean or enriched)")
    public Object get(@PathVariable long id,
                      @RequestParam(required = false) String view,
                      @RequestParam(required = false) String range) {
        return service.get(id, view, range);
    }

    @GetMapping({"/charging-processes/{id}/samples", "/charges/{id}/samples"})
    @Operation(summary = "Charge curve samples")
    public List<ChargeSampleDto> samples(@PathVariable long id) {
        return service.samples(id);
    }
}
