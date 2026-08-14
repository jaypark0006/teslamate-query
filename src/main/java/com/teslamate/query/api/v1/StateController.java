package com.teslamate.query.api.v1;

import com.teslamate.query.dto.PageResponse;
import com.teslamate.query.dto.StateDto;
import com.teslamate.query.service.StateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/states")
@Tag(name = "States")
public class StateController {

    private final StateService stateService;

    public StateController(StateService stateService) {
        this.stateService = stateService;
    }

    @GetMapping
    @Operation(summary = "List connectivity intervals (Condition → ids → rows)")
    public PageResponse<StateDto> list(
            @RequestParam(required = false) Long carId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        return stateService.list(carId, from, to, page, size);
    }

    @GetMapping("/{stateId}")
    public StateDto get(@PathVariable long stateId) {
        return stateService.get(stateId);
    }
}
