# API reference (v1)

Base path: `/api/v1`  
Auth: default **off** (`AUTH_ENABLED=false`). When on: header `X-API-Key` (or query `api_key`).

## Conventions

- Timestamps: ISO-8601 UTC (`2024-01-01T00:00:00Z`)
- **DB is always metric** (km, °C, km/h, m). Conversion is **not** done in SQL.
- Display units via query params (optional):
  - `lengthUnit=km|mi` (default `km`)
  - `tempUnit=C|F` (default `C`)
- After load, Service converts length / temp / speed / elevation for the response.
- Paged lists: `{ "data", "page", "size", "total", "units": { "length", "temperature", "speed", "elevation" } }`
- Field names may still say `*Km` / `*C` for compatibility; **values follow `units`**.
- Filter `minDistance` is in **`lengthUnit`**, then converted to km for SQL.
- Errors: `{ "code", "message", "timestamp", "path" }`
- **Entity 1:1 table**; multi-table only in Service (multi-Dao)

### Example

```http
GET /api/v1/drives?carId=1&lengthUnit=mi&tempUnit=F&minDistance=10
```

→ `minDistance` means 10 **miles** in the filter; JSON distances/temps are mi / °F; `units.length` is `"mi"`.

## IDs (read this first)

| Name | Table | Meaning |
|------|--------|---------|
| `carId` | cars | Vehicle |
| `driveId` | drives | One trip |
| **`chargingProcessId`** | **charging_processes** | **One charging session** (plug-in to unplug) |
| `charge` row `id` | charges | One telemetry sample *inside* a session |
| `positionId` | positions | One GPS/telemetry point |
| `addressId` / `geofenceId` | addresses / geofences | Place labels |

JSON `"id"` on a resource is **that resource’s own primary key**.  
A session’s samples are **not** `/charges/{chargingProcessId}`.  
A drive’s GPS points are **not** `/positions/{driveId}` — use `/drives/{driveId}/positions`.

**Time `from`/`to`:** `/drives` and `/charging-processes` filter **`start_date` in range**. `/states` and `/map/trip` use **interval overlap**. `/map/tracks` uses start_date (prefer `/map/trip` for Trip).

**Map features:** `kind=drive` → `driveId`; `kind=charge` → `chargingProcessId`; `kind=park` → `parkIndex`.

### How to get all samples for one session

```http
GET /api/v1/charging-processes/{chargingProcessId}/charges
```

Same data, query style:

```http
GET /api/v1/charges?chargingProcessId={chargingProcessId}
```

Example: session `364` (from `GET /charging-processes?carId=1`):

```http
GET /api/v1/charging-processes/364/charges
GET /api/v1/charges?chargingProcessId=364
```

## Single-table resources

| Table | Endpoints |
|-------|-----------|
| cars | `GET /cars` · `/cars/{carId}` |
| car_settings | `GET /car-settings` · `/car-settings/{carSettingsId}` |
| settings | `GET /settings` |
| drives | `GET /drives?...` · `/drives/{driveId}` |
| charging_processes | `GET /charging-processes?...` · `/{chargingProcessId}` · `/{chargingProcessId}/charges` |
| positions | `GET /positions?driveId=` or `carId&from&to` (no `/{id}` — use `/drives/{driveId}/positions`) |
| charges | `GET /charges?chargingProcessId=` or `carId&from&to` |
| states | `GET /states?...` · `/states/{stateId}` |
| updates | `GET /updates?...` · `/updates/{updateId}` |
| addresses | `GET /addresses?...` · `?addressIds=` · `/{addressId}` |
| geofences | `GET /geofences?...` · `/{geofenceId}` |

### Filter notes

- **positions**: require `driveId`, or `carId` + `from` + `to` (optional `cleanOnly`)
- **charges**: require `chargingProcessId`, or `carId` + `from` + `to`
- **states**: optional `carId`, `from`/`to` (interval overlap)
- **updates**: optional `carId`, `from`/`to` on `start_date`
- **drives / charging-processes**: optional `carId`, time range, `geofenceId`, `incompleteOnly`; charging-processes also `excludeZeroEnergy`

## Nested convenience (multi-Dao)

- `GET /drives/{driveId}/positions?downsample=`
- `GET /charging-processes/{chargingProcessId}/charges`
- `GET /cars/{carId}/latest` — multi-table snapshot
- `GET /map/tracks?carId&from&to` — GeoJSON composition
- `GET /map/trip?carId&from&to` — drive lines + charge/park points + direction chevrons (overlap window)
- `GET /cars/{carId}/map` — same GeoJSON, car-scoped
- `GET /cars/{carId}/map/points?from&to&kinds=` — flat lat/lon rows for Grafana Geomap Route (arrows) + Markers
- `GET /cars/{carId}/timeline?from&to&minParkMin=` — chronological DRIVE / CHARGE / PARK log
- `GET /cars/{carId}/timeline/daily?from&to` — hours of drive/charge/park per local day
- `GET /cars/{carId}/timeline/grid?from&to&timezone=` — day × 15-min cells (`kindCode` 1=park 2=drive 3=charge); overnight parks fill every local day they cover
- `GET /series/battery?carId&from&to` — SOC series from positions

## Health

`GET /health` (no auth)
