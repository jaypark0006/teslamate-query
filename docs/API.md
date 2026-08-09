# API reference (v1)

Base path: `/api/v1`  
Auth: header `X-API-Key` (or query `api_key`)

## Conventions

- Timestamps: ISO-8601 UTC (`2024-01-01T00:00:00Z`)
- Units: metric in field names (`distanceKm`, `*C`, ranges in km)
- `range=ideal|rated` overrides settings preferred range
- List responses: `{ "data": [...], "page", "size", "total" }`
- Errors: `{ "code", "message", "timestamp", "path" }`

## Core resources

### GET /cars
### GET /cars/{id}
### GET /cars/{id}/latest
Latest position **or** charge sample (`ideal_battery_range_km IS NOT NULL` for positions).

### GET /settings
Global units and preferred range.

### GET /geofences

### GET /drives
Query: `carId`, `from`, `to`, `minDistance`, `minDuration`, `geofenceId`, `location`, `incompleteOnly`, `range`, `page`, `size`

### GET /drives/{id}
### GET /drives/{id}/positions
Query: `downsample` (seconds)

### GET /charging-processes  (alias `/charges`)
Query: `carId`, `from`, `to`, `geofenceId`, `chargeType`, `incompleteOnly`, `range`, `page`, `size`

### GET /charging-processes/{id}
### GET /charging-processes/{id}/samples

## Aggregates

### GET /overview?carId&from&to&range
### GET /stats/drives?carId&from&to&groupBy&range
### GET /stats/charging?carId&from&to
### GET /stats/efficiency?carId&from&to&range
### GET /stats/period?carId&from&to&period&range
### GET /stats/mileage?carId&from&to
### GET /stats/vampire-drain?carId&from&to&range
### GET /stats/projected-range?carId&from&to&range
### GET /stats/battery-health?carId&from&to&range
### GET /stats/locations?carId&from&to

## Events / series

### GET /timeline?carId&from&to
### GET /trips/summary?carId&from&to&range
### GET /states?carId&from&to
### GET /updates?carId&from&to
### GET /positions?carId&from&to&cleanOnly&downsample&page&size

## Health

### GET /health  (no auth)
