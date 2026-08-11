package com.teslamate.query.api.v1;

import com.teslamate.query.dto.ChargeDto;
import com.teslamate.query.dto.PageResponse;
import com.teslamate.query.service.ChargeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/charges")
@Tag(name = "Charges")
public class ChargeController {

    private final ChargeService chargeService;

    public ChargeController(ChargeService chargeService) {
        this.chargeService = chargeService;
    }

    @GetMapping
    @Operation(summary = "List charge samples (requires chargingProcessId, or from+to)")
    public PageResponse<ChargeDto> list(
            @RequestParam(required = false) Long chargingProcessId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        return chargeService.list(chargingProcessId, from, to, page, size);
    }

    @GetMapping("/{id}")
    public ChargeDto get(@PathVariable long id) {
        return chargeService.get(id);
    }
}
