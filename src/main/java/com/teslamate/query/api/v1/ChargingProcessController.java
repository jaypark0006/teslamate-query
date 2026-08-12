package com.teslamate.query.api.v1;

import com.teslamate.query.dto.ChargeSampleDto;
import com.teslamate.query.dto.ChargingProcessDto;
import com.teslamate.query.dto.PageResponse;
import com.teslamate.query.service.ChargingProcessService;
import com.teslamate.query.service.QuerySupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/charging-processes")
@Tag(name = "Charging Processes")
public class ChargingProcessController {

    private final ChargingProcessService service;
    private final QuerySupport support;

    public ChargingProcessController(ChargingProcessService service, QuerySupport support) {
        this.service = service;
        this.support = support;
    }

    @GetMapping
    @Operation(summary = "List charge sessions")
    public PageResponse<ChargingProcessDto> list(
            @RequestParam(required = false) Long carId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) Long geofenceId,
            @RequestParam(required = false) Boolean incompleteOnly,
            @RequestParam(required = false) Boolean excludeZeroEnergy,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String lengthUnit,
            @RequestParam(required = false) String tempUnit
    ) {
        return service.list(carId, from, to, geofenceId, incompleteOnly, excludeZeroEnergy, page, size,
                support.units(lengthUnit, tempUnit));
    }

    @GetMapping("/{id}")
    public ChargingProcessDto get(
            @PathVariable long id,
            @RequestParam(required = false) String lengthUnit,
            @RequestParam(required = false) String tempUnit
    ) {
        return service.get(id, support.units(lengthUnit, tempUnit));
    }

    @GetMapping("/{id}/samples")
    public List<ChargeSampleDto> samples(
            @PathVariable long id,
            @RequestParam(required = false) String lengthUnit,
            @RequestParam(required = false) String tempUnit
    ) {
        return service.samples(id, support.units(lengthUnit, tempUnit));
    }
}
