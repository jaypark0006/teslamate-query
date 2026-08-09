package com.teslamate.query.service;

import com.teslamate.query.dto.AddressDto;
import com.teslamate.query.exception.BadRequestException;
import com.teslamate.query.exception.NotFoundException;
import com.teslamate.query.repository.AddressRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AddressService {

    private final AddressRepository addressRepository;

    public AddressService(AddressRepository addressRepository) {
        this.addressRepository = addressRepository;
    }

    @Cacheable(value = "addresses", key = "#id")
    public AddressDto get(long id) {
        return addressRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Address not found: " + id));
    }

    public List<AddressDto> listByIds(String idsParam) {
        if (idsParam == null || idsParam.isBlank()) {
            throw new BadRequestException("ids is required (comma-separated address ids)");
        }
        Set<Long> ids = Arrays.stream(idsParam.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> {
                    try {
                        return Long.parseLong(s);
                    } catch (NumberFormatException e) {
                        throw new BadRequestException("invalid address id: " + s);
                    }
                })
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (ids.size() > 200) {
            throw new BadRequestException("at most 200 address ids per request");
        }
        return addressRepository.findByIds(ids);
    }
}
