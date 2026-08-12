# Schema audit against live TeslaMate DB

Connected: PostgreSQL 17.10 via tunnel (`localhost:5432`).

## Verified data

| Table | Rows (approx) | Notes |
|-------|---------------|--------|
| positions | ~12.2M | Map paths select lon/lat only |
| drives | 4487 | OK |
| charging_processes | 365 | `position_id` NOT NULL |
| cars | 1 | Model Y, LFP, efficiency 0.13826 |
| settings | 1 | preferred_range=**rated**, units km/C |
| addresses | 0 | empty — no reverse-geocode cache |
| geofences | 0 | empty |

## Bugs fixed from live check

1. **`settings.theme_mode` does not exist** on this install — not mapped on `SettingsEntity`.
2. Preferred range on this DB is **rated**.

## Layering

| Layer | Role |
|-------|------|
| Entity | 1:1 table row, `@ColumnName` |
| Dao | `count` / `findIds` / `findByIds` / `findById` |
| Condition | single-table WHERE |
| Service | params, paging, units, multi-Dao composition |

## Map API

- `/map/tracks` loads `drive_id, date, longitude, latitude` only
- Charge markers: `charging_processes.position_id` → `positions` for lat/lon
