# Grafana examples

Implemented endpoints: **[docs/GRAFANA_ENDPOINTS.md](../../docs/GRAFANA_ENDPOINTS.md)**  
(Older notes in `docs/GRAFANA_ADJUSTMENT.md` describe stats/overview that this service does **not** implement.)

## Quick setup

1. Install [Infinity](https://grafana.com/grafana/plugins/yesoreyeram-infinity-datasource/)
2. Datasource URL: `http://teslamate-query:8080`
3. Auth off by default; if `AUTH_ENABLED=true`, header `X-API-Key`
4. Variables: `car_id` ← `GET /api/v1/cars`; time picker → `from`/`to`; optional `lengthUnit` / `tempUnit`

## Endpoint map (what exists)

| Use | Endpoint |
|-----|----------|
| drives.json | `GET /api/v1/drives` |
| charges.json | `GET /api/v1/charging-processes` |
| charge samples | `GET /api/v1/charges?chargingProcessId=` or `carId&from&to` |
| states.json | `GET /api/v1/states` |
| updates.json | `GET /api/v1/updates` |
| map / visited | `GET /api/v1/map/tracks` |
| charge level | `GET /api/v1/series/battery` |
| latest | `GET /api/v1/cars/{id}/latest` |

There is no `/overview`, `/stats/*`, `/timeline`, or `view=enriched`.

## Sample panel snippets

- `infinity-drives.json` — table over `/drives`

## Filter philosophy (short)

- **Keep:** car, time range, geofence id, min distance/duration, incomplete flag, `lengthUnit`/`tempUnit`
- **Units:** API converts when those params are set; `/settings` is the stored preference only
- **Names (address/geofence):** second query + transform join
