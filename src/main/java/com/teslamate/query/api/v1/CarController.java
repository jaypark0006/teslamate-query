package com.teslamate.query.api.v1;

import com.teslamate.query.dto.CarDto;
import com.teslamate.query.dto.LatestSnapshotDto;
import com.teslamate.query.service.CarService;
import com.teslamate.query.service.QuerySupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cars")
@Tag(name = "Cars")
public class CarController {

    private final CarService carService;
    private final QuerySupport support;

    public CarController(CarService carService, QuerySupport support) {
        this.carService = carService;
        this.support = support;
    }

    @GetMapping
    @Operation(summary = "List cars (Grafana $car_id variable)")
    public List<CarDto> list() {
        return carService.list();
    }

    @GetMapping("/{carId}")
    @Operation(summary = "Get car by carId")
    public CarDto get(@PathVariable long carId) {
        return carService.get(carId);
    }

    @GetMapping("/{carId}/latest")
    @Operation(summary = "Latest telemetry snapshot (position or charge sample)")
    public LatestSnapshotDto latest(
            @PathVariable long carId,
            @RequestParam(required = false) String lengthUnit,
            @RequestParam(required = false) String tempUnit
    ) {
        return carService.latest(carId, support.units(lengthUnit, tempUnit));
    }
}
