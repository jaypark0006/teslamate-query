# API reference (v1)

Base path: `/api/v1`  
Auth: default **off** (`AUTH_ENABLED=false`). When on: header `X-API-Key` (or query `api_key`).

## Conventions

- Timestamps: ISO-8601 UTC (`2024-01-01T00:00:00Z`)
- Units are metric (`distance` km, temps °C, ranges km)
- Paged lists: `{ "data": [...], "page", "size", "total" }`
- Errors: `{ "code", "message", "timestamp", "path" }`
- **Entity 1:1 table**; multi-table only in Service (multi-Dao)

## Single-table resources

| Table | Endpoints |
|-------|-----------|
| cars | `GET /cars` · `/cars/{id}` |
| car_settings | `GET /car-settings` · `/car-settings/{id}` |
| settings | `GET /settings` |
| drives | `GET /drives?...` · `/drives/{id}` |
| charging_processes | `GET /charging-processes?...` · `/{id}` |
| positions | `GET /positions?...` · `/positions/{id}` |
| charges | `GET /charges?...` · `/charges/{id}` |
| states | `GET /states?...` · `/states/{id}` |
| updates | `GET /updates?...` · `/updates/{id}` |
| addresses | `GET /addresses?...` · `?ids=` · `/{id}` |
| geofences | `GET /geofences?...` · `/{id}` |

### Filter notes

- **positions**: require `driveId`, or `carId` + `from` + `to` (optional `cleanOnly`)
- **charges**: require `chargingProcessId`, or `from` + `to`
- **states**: optional `carId`, `from`/`to` (interval overlap)
- **updates**: optional `carId`, `from`/`to` on `start_date`
- **drives / charging-processes**: optional `carId`, time range, `geofenceId`, `incompleteOnly`, …

## Nested convenience (multi-Dao)

- `GET /drives/{id}/positions?downsample=`
- `GET /charging-processes/{id}/samples`
- `GET /cars/{id}/latest` — multi-table snapshot
- `GET /map/tracks?carId&from&to` — GeoJSON composition
- `GET /series/battery?carId&from&to` — SOC series from positions

## Health

`GET /health` (no auth)
