package com.teslamate.query.api.v1;

import com.teslamate.query.dto.ChargeDto;
import com.teslamate.query.dto.ChargingProcessDto;
import com.teslamate.query.dto.PageResponse;
import com.teslamate.query.service.ChargeService;
import com.teslamate.query.service.ChargingProcessService;
import com.teslamate.query.service.QuerySupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/charging-processes")
@Tag(name = "Charging Processes")
public class ChargingProcessController {

    private final ChargingProcessService service;
    private final ChargeService chargeService;
    private final QuerySupport support;

    public ChargingProcessController(ChargingProcessService service, ChargeService chargeService,
                                     QuerySupport support) {
        this.service = service;
        this.chargeService = chargeService;
        this.support = support;
    }

    @GetMapping
    @Operation(summary = "List charge sessions")
    public PageResponse<ChargingProcessDto> list(
            @RequestParam(required = false) Long carId,
            @Parameter(description = "start_date >= from (not interval overlap; see /map/trip)")
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

    @GetMapping("/{chargingProcessId}")
    @Operation(summary = "One charging session (not samples)")
    public ChargingProcessDto get(
            @PathVariable long chargingProcessId,
            @RequestParam(required = false) String lengthUnit,
            @RequestParam(required = false) String tempUnit
    ) {
        return service.get(chargingProcessId, support.units(lengthUnit, tempUnit));
    }

    @GetMapping("/{chargingProcessId}/charges")
    @Operation(summary = "All sample points in this charging session")
    public PageResponse<ChargeDto> charges(
            @PathVariable long chargingProcessId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String lengthUnit,
            @RequestParam(required = false) String tempUnit
    ) {
        return chargeService.list(chargingProcessId, null, null, null, page, size,
                support.units(lengthUnit, tempUnit));
    }
}
