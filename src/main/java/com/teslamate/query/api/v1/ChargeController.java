package com.teslamate.query.api.v1;

import com.teslamate.query.dto.ChargeDto;
import com.teslamate.query.dto.PageResponse;
import com.teslamate.query.service.ChargeService;
import com.teslamate.query.service.QuerySupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/charges")
@Tag(name = "Charges")
public class ChargeController {

    private final ChargeService chargeService;
    private final QuerySupport support;

    public ChargeController(ChargeService chargeService, QuerySupport support) {
        this.chargeService = chargeService;
        this.support = support;
    }

    @GetMapping
    @Operation(summary = "Query sample points; prefer chargingProcessId. Path /charges/{id} is not a session.")
    public PageResponse<ChargeDto> list(
            @RequestParam(required = false) Long chargingProcessId,
            @RequestParam(required = false) Long carId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String lengthUnit,
            @RequestParam(required = false) String tempUnit
    ) {
        return chargeService.list(chargingProcessId, carId, from, to, page, size,
                support.units(lengthUnit, tempUnit));
    }
}
