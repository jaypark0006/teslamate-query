# Grafana migration examples

Use the [Infinity](https://grafana.com/grafana/plugins/yesoreyeram-infinity-datasource/) plugin (or JSON API) as the Grafana datasource.

## Datasource setup

1. Install Infinity plugin
2. Create datasource pointing at `http://teslamate-query:8080` (or host port)
3. Add header `X-API-Key: <your key>`
4. Replace Postgres `rawSql` panels gradually:

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
| locations.json / visited.json | `GET /api/v1/stats/locations` |
| states.json | `GET /api/v1/states` |
| updates.json | `GET /api/v1/updates` |

## Migration stages

1. **Dual-run**: keep Postgres DS, add Infinity DS
2. Replace Drives/Charges panels first
3. Replace Overview/Stats
4. Remove Postgres datasource from Grafana when parity is enough
