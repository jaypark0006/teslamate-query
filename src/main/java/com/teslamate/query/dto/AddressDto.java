package com.teslamate.query.dto;

import java.math.BigDecimal;

public record AddressDto(
        Long addressId,
        String displayName,
        String name,
        String road,
        String houseNumber,
        String neighbourhood,
        String city,
        String county,
        String postcode,
        String state,
        String stateDistrict,
        String country,
        BigDecimal latitude,
        BigDecimal longitude,
        Long osmId,
        String osmType
) {
}
