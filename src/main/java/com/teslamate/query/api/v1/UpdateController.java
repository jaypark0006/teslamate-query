package com.teslamate.query.api.v1;

import com.teslamate.query.dto.PageResponse;
import com.teslamate.query.dto.UpdateDto;
import com.teslamate.query.service.UpdateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/updates")
@Tag(name = "Updates")
public class UpdateController {

    private final UpdateService updateService;

    public UpdateController(UpdateService updateService) {
        this.updateService = updateService;
    }

    @GetMapping
    @Operation(summary = "List software updates (Condition → ids → rows)")
    public PageResponse<UpdateDto> list(
            @RequestParam(required = false) Long carId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        return updateService.list(carId, from, to, page, size);
    }

    @GetMapping("/{updateId}")
    public UpdateDto get(@PathVariable long updateId) {
        return updateService.get(updateId);
    }
}
