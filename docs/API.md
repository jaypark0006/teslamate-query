# API reference (v1)

Base path: `/api/v1`  
Auth: header `X-API-Key` (or query `api_key`)

## Conventions

- Timestamps: ISO-8601 UTC (`2024-01-01T00:00:00Z`)
- **Units are metric** in field names (`distanceKm`, temps in °C, ranges in km)
- List responses: `{ "data": [...], "page", "size", "total" }`
- Errors: `{ "code", "message", "timestamp", "path" }`
- **Domain resources are lean by default** (table columns + foreign keys)
- Grafana-wide rows: `view=enriched` (optional; joins allowed only here)

## Core resources (lean)

### GET /cars
### GET /cars/{id}
### GET /cars/{id}/latest

### GET /settings
Global units / preferred range (read once per Grafana dashboard).

### GET /geofences
### GET /addresses/{id}
### GET /addresses?ids=1,2,3
Batch resolve address FKs (max 200) for client/Grafana joins.

### GET /drives
Query: `carId`, `from`, `to`, `minDistance`, `minDuration`, `geofenceId`, `incompleteOnly`, `page`, `size`  
Optional: `view=enriched`, `range=ideal|rated` (enriched only)

Lean fields include both ideal/rated ranges and FK ids:
`startAddressId`, `endAddressId`, `startGeofenceId`, `endGeofenceId`, `startPositionId`, `endPositionId`.

### GET /drives/{id}
### GET /drives/{id}/positions?downsample=

### GET /charging-processes  (alias `/charges`)
Query: `carId`, `from`, `to`, `geofenceId`, `incompleteOnly`, `page`, `size`  
Optional: `chargeType=AC|DC` (**lean only**, joins `charges` only when set)  
Optional: `view=enriched`, `range=`

Lean fields: energy, SOC, cost, both range pairs, `positionId`, `addressId`, `geofenceId`.

### GET /charging-processes/{id}
### GET /charging-processes/{id}/samples

## Enriched view (Grafana tables)

```
GET /drives?view=enriched&range=ideal&...
GET /charging-processes?view=enriched&range=ideal&...
```

Adds display labels, optional consumption, AC/DC, lat/lon, etc.  
Prefer lean + `/geofences` + `/addresses?ids=` composition when possible.

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
