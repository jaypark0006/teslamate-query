package com.teslamate.query.api.v1;

import com.teslamate.query.dto.ChargeSampleDto;
import com.teslamate.query.dto.ChargingProcessDto;
import com.teslamate.query.dto.PageResponse;
import com.teslamate.query.service.ChargingProcessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/charging-processes")
@Tag(name = "Charging Processes")
public class ChargingProcessController {

    private final ChargingProcessService service;

    public ChargingProcessController(ChargingProcessService service) {
        this.service = service;
    }

    @GetMapping
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

    @GetMapping("/{id}")
    public ChargingProcessDto get(@PathVariable long id) {
        return service.get(id);
    }

    @GetMapping("/{id}/samples")
    @Operation(summary = "Charge samples for a session (nested convenience)")
    public List<ChargeSampleDto> samples(@PathVariable long id) {
        return service.samples(id);
    }
}
