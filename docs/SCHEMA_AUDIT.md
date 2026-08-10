# Schema audit against live TeslaMate DB

Connected: PostgreSQL 17.10 via tunnel (`localhost:5432`).

## Verified data

| Table | Rows (approx) | Notes |
|-------|---------------|--------|
| positions | ~12.2M | Must downsample for maps |
| drives | 4487 | OK |
| charging_processes | 365 | `position_id` NOT NULL |
| cars | 1 | Model Y, LFP, efficiency 0.13826 |
| settings | 1 | preferred_range=**rated**, units km/C |
| addresses | 0 | empty — no reverse-geocode cache |
| geofences | 0 | empty |

## Bugs fixed from live check

1. **`settings.theme_mode` does not exist** on this install — removed from `SettingsDao` / `SettingsDto`.
2. Default preferred range fallback changed **ideal → rated** (matches this DB).

## Live HTTP smoke (after fixes)

- `/health` UP
- `/cars` OK
- `/drives?carId=1&size=2` lean OK (total 4487)
- `/charging-processes?carId=1&size=2` lean OK
- `/cars/1/latest` OK
- `/overview` OK (net ~158 Wh/km Jan 2025)
- `/settings` fixed after removing theme_mode

## Layering after simplification

| Layer | Role |
|-------|------|
| `dao/*` | SqlObject for simple fixed queries |
| `repository/*` | Dynamic SQL / analytics only |
| `service/*` | Params, paging, cache — no ResultSet |
| thin Dao wrappers | **removed** |

## Implications for map API

- Use `positions (drive_id, date)` index + downsample
- Charge markers: join `charging_processes.position_id → positions` for lat/lon
- No address/geofence labels available on this dataset
