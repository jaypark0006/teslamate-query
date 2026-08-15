package com.teslamate.query.service.trip;

import com.teslamate.query.entity.AddressEntity;
import com.teslamate.query.entity.GeofenceEntity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlaceLabelTest {

    @Test
    void geofenceWins() {
        GeofenceEntity g = new GeofenceEntity(1L, "Home", BigDecimal.ONE, BigDecimal.ONE, 50, null, null, null);
        AddressEntity a = address("ignored", "Road", "City", "Road, City, CN");
        assertEquals("Home", PlaceLabel.of(g, a));
    }

    @Test
    void roadAndCity() {
        assertEquals("Yangjiaping Rd, Chongqing", PlaceLabel.of(null,
                address(null, "Yangjiaping Rd", "Chongqing", "long display")));
    }

    @Test
    void displayNameFallback() {
        assertEquals("A, B", PlaceLabel.of(null, address(null, null, null, "A, B, C, D")));
    }

    private static AddressEntity address(String name, String road, String city, String display) {
        return new AddressEntity(1L, display, name, null, road, null, city, null, null, null, null, "CN",
                null, null, null, null);
    }
}
