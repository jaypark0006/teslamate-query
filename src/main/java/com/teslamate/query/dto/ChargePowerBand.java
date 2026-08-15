package com.teslamate.query.dto;

/** Average power over a SOC window. Incomplete windows are marked in {@link #label()}. */
public record ChargePowerBand(
        Double kw,
        int fromSoc,
        int toSoc,
        boolean complete
) {
    public String label() {
        if (kw == null) {
            return null;
        }
        String n = kw == Math.rint(kw) ? String.valueOf(kw.intValue()) : String.format("%.1f", kw);
        if (complete) {
            return n;
        }
        return "~" + n + " (" + fromSoc + "–" + toSoc + "%)";
    }
}
