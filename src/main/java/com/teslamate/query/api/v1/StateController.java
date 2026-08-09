package com.teslamate.query.api.v1;

import com.teslamate.query.dto.StateDto;
import com.teslamate.query.service.AdvancedQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/states")
@Tag(name = "States")
public class StateController {

    private final AdvancedQueryService service;

    public StateController(AdvancedQueryService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Vehicle connectivity states (online/offline/asleep)")
    public List<StateDto> list(
            @RequestParam long carId,
            @RequestParam String from,
            @RequestParam String to
    ) {
        return service.states(carId, from, to);
    }
}
