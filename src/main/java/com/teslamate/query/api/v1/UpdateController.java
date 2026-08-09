package com.teslamate.query.api.v1;

import com.teslamate.query.dto.UpdateDto;
import com.teslamate.query.service.AdvancedQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/updates")
@Tag(name = "Updates")
public class UpdateController {

    private final AdvancedQueryService service;

    public UpdateController(AdvancedQueryService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Software update history")
    public List<UpdateDto> list(
            @RequestParam long carId,
            @RequestParam String from,
            @RequestParam String to
    ) {
        return service.updates(carId, from, to);
    }
}
