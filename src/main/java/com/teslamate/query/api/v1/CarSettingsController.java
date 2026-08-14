package com.teslamate.query.api.v1;

import com.teslamate.query.dto.CarSettingsDto;
import com.teslamate.query.service.CarSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/car-settings")
@Tag(name = "Car Settings")
public class CarSettingsController {

    private final CarSettingsService carSettingsService;

    public CarSettingsController(CarSettingsService carSettingsService) {
        this.carSettingsService = carSettingsService;
    }

    @GetMapping
    @Operation(summary = "List car_settings rows")
    public List<CarSettingsDto> list() {
        return carSettingsService.list();
    }

    @GetMapping("/{carSettingsId}")
    @Operation(summary = "One car_settings row. This is cars.settingsId, not carId.")
    public CarSettingsDto get(@PathVariable long carSettingsId) {
        return carSettingsService.get(carSettingsId);
    }
}
