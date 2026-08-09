package com.teslamate.query.api.v1;

import com.teslamate.query.dto.CarDto;
import com.teslamate.query.dto.LatestSnapshotDto;
import com.teslamate.query.service.CarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cars")
@Tag(name = "Cars")
public class CarController {

    private final CarService carService;

    public CarController(CarService carService) {
        this.carService = carService;
    }

    @GetMapping
    @Operation(summary = "List cars (Grafana $car_id variable)")
    public List<CarDto> list() {
        return carService.list();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get car by id")
    public CarDto get(@PathVariable long id) {
        return carService.get(id);
    }

    @GetMapping("/{id}/latest")
    @Operation(summary = "Latest telemetry snapshot (position or charge sample)")
    public LatestSnapshotDto latest(@PathVariable long id) {
        return carService.latest(id);
    }
}
