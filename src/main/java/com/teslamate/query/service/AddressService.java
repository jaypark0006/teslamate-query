package com.teslamate.query.service;

import com.teslamate.query.dao.AddressDao;
import com.teslamate.query.db.condition.AddressSearchCondition;
import com.teslamate.query.dto.AddressDto;
import com.teslamate.query.dto.PageResponse;
import com.teslamate.query.exception.BadRequestException;
import com.teslamate.query.exception.NotFoundException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AddressService {

    private final AddressDao addressDao;
    private final QuerySupport support;

    public AddressService(AddressDao addressDao, QuerySupport support) {
        this.addressDao = addressDao;
        this.support = support;
    }

    @Cacheable(value = "addresses", key = "#id")
    public AddressDto get(long id) {
        return addressDao.findById(id)
                .map(EntityMapper::toAddressDto)
                .orElseThrow(() -> new NotFoundException("Address not found: " + id));
    }

    /** Paged list with optional city/country filters. */
    public PageResponse<AddressDto> list(String city, String country, Integer page, Integer size) {
        AddressSearchCondition condition = AddressSearchCondition.builder()
                .city(city)
                .country(country)
                .build();
        int p = support.page(page);
        int s = support.size(size);
        long total = addressDao.count(condition);
        List<Long> ids = addressDao.findIds(condition, s, support.offset(p, s));
        return PageResponse.of(EntityMapper.toAddressDtos(addressDao.findByIdsOrdered(ids)), p, s, total);
    }

    /** Batch get by comma-separated ids (Grafana join transforms). */
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
        if (ids.isEmpty()) {
            throw new BadRequestException("ids is required");
        }
        if (ids.size() > 200) {
            throw new BadRequestException("at most 200 address ids per request");
        }
        return EntityMapper.toAddressDtos(addressDao.findByIdsOrdered(ids));
    }
}
