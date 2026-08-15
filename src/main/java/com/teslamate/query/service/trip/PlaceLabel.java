package com.teslamate.query.service.trip;

import com.teslamate.query.entity.AddressEntity;
import com.teslamate.query.entity.GeofenceEntity;

/** Short map/timeline place text: geofence name, else road+city, else first two address parts. */
public final class PlaceLabel {

    private PlaceLabel() {}

    public static String of(GeofenceEntity geofence, AddressEntity address) {
        if (geofence != null && notBlank(geofence.name())) {
            return geofence.name().trim();
        }
        if (address == null) {
            return null;
        }
        if (notBlank(address.name())) {
            return address.name().trim();
        }
        if (notBlank(address.road())) {
            String road = address.road().trim();
            return notBlank(address.city()) ? road + ", " + address.city().trim() : road;
        }
        if (!notBlank(address.displayName())) {
            return null;
        }
        String[] parts = address.displayName().split(",");
        if (parts.length >= 2) {
            return parts[0].trim() + ", " + parts[1].trim();
        }
        return address.displayName().trim();
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
