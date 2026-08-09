# Grafana examples

完整调整策略见：**[docs/GRAFANA_ADJUSTMENT.md](../../docs/GRAFANA_ADJUSTMENT.md)**

## Quick setup

1. Install [Infinity](https://grafana.com/grafana/plugins/yesoreyeram-infinity-datasource/)
2. Datasource URL: `http://teslamate-query:8080`
3. Header: `X-API-Key: <key>`
4. Dashboard variables: `car_id` ← `GET /api/v1/cars`, time picker → `from`/`to`

## Endpoint map

| Old dashboard | New endpoint |
|---------------|--------------|
| drives.json | `GET /api/v1/drives` |
| charges.json | `GET /api/v1/charging-processes` |
| overview.json | `GET /api/v1/overview` |
| drive-stats.json | `GET /api/v1/stats/drives` |
| charging-stats.json | `GET /api/v1/stats/charging` |
| efficiency.json | `GET /api/v1/stats/efficiency` |
| statistics.json | `GET /api/v1/stats/period` |
| timeline.json | `GET /api/v1/timeline` |
| trip.json | `GET /api/v1/trips/summary` |
| vampire-drain.json | `GET /api/v1/stats/vampire-drain` |
| battery-health.json | `GET /api/v1/stats/battery-health` |
| projected-range.json | `GET /api/v1/stats/projected-range` |
| locations / visited | `GET /api/v1/stats/locations` |
| states.json | `GET /api/v1/states` |
| updates.json | `GET /api/v1/updates` |

## Sample panel snippets

- `infinity-drives.json` — table over `/drives`
- `infinity-overview.json` — notes for `/overview`

## Filter philosophy (short)

- **Keep:** car, time range, geofence id, min distance/duration, charge type, incomplete flag  
- **Drop from every panel:** length_unit / temp_unit / preferred_range SQL queries  
- **Units:** read `/settings` once; convert in Grafana field units  
- **Names (address/geofence):** second query + transform join, not forced SQL JOIN in API
