package com.teslamate.query.api.v1;

import com.teslamate.query.dto.AddressDto;
import com.teslamate.query.dto.PageResponse;
import com.teslamate.query.service.AddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/addresses")
@Tag(name = "Addresses")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping(params = "ids")
    @Operation(summary = "Batch get addresses by comma-separated ids")
    public List<AddressDto> listByIds(
            @Parameter(description = "Comma-separated ids, max 200")
            @RequestParam String ids
    ) {
        return addressService.listByIds(ids);
    }

    @GetMapping
    @Operation(summary = "List addresses (paged)")
    public PageResponse<AddressDto> list(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        return addressService.list(city, country, page, size);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get address by id")
    public AddressDto get(@PathVariable long id) {
        return addressService.get(id);
    }
}
