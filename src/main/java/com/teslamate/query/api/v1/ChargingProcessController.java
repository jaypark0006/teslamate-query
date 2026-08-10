package com.teslamate.query.api.v1;

import com.teslamate.query.dto.ChargeSampleDto;
import com.teslamate.query.dto.PageResponse;
import com.teslamate.query.exception.BadRequestException;
import com.teslamate.query.service.ChargingProcessService;
import com.teslamate.query.service.DriveService;
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
    @Operation(summary = "List sessions. Default lean; view=enriched for wide rows.")
    public PageResponse<?> list(
            @RequestParam(required = false) Long carId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) Long geofenceId,
            @Parameter(description = "AC|DC filter (lean only)")
            @RequestParam(required = false) String chargeType,
            @RequestParam(required = false) Boolean incompleteOnly,
            @RequestParam(required = false) String view,
            @RequestParam(required = false) String range,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        if (DriveService.isEnriched(view)) {
            if (chargeType != null && !chargeType.isBlank()) {
                throw new BadRequestException("chargeType is only for lean view; use /stats/charging for AC/DC");
            }
            return service.listEnriched(carId, from, to, geofenceId, incompleteOnly, range, page, size);
        }
        return service.listLean(carId, from, to, geofenceId, chargeType, incompleteOnly, page, size);
    }

    @GetMapping({"/charging-processes/{id}", "/charges/{id}"})
    public Object get(@PathVariable long id,
                      @RequestParam(required = false) String view,
                      @RequestParam(required = false) String range) {
        if (DriveService.isEnriched(view)) {
            return service.getEnriched(id, range);
        }
        return service.getLean(id);
    }

    @GetMapping({"/charging-processes/{id}/samples", "/charges/{id}/samples"})
    public List<ChargeSampleDto> samples(@PathVariable long id) {
        return service.samples(id);
    }
}
