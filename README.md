# teslamate-query

REST API facade over TeslaMate PostgreSQL for Grafana and other clients.

## Phase 2 APIs

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/overview` | Overview KPIs (`carId`, `from`, `to`) |
| GET | `/api/v1/stats/drives` | Drive stats + buckets |
| GET | `/api/v1/stats/charging` | Charging stats, AC/DC, top stations |
| GET | `/api/v1/stats/efficiency` | Net/gross consumption + temp buckets |
| GET | `/api/v1/stats/period` | Day/week/month/year rollup |
| GET | `/api/v1/stats/mileage` | Odometer series |


## Phase 3 APIs

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/timeline` | Unified drive/charge/state/update events |
| GET | `/api/v1/trips/summary` | Trip window summary |
| GET | `/api/v1/states` | Connectivity states |
| GET | `/api/v1/updates` | Firmware updates |
| GET | `/api/v1/positions` | Positions (time range required) |
| GET | `/api/v1/stats/vampire-drain` | Parked range loss |
| GET | `/api/v1/stats/projected-range` | Projected full range series |
| GET | `/api/v1/stats/battery-health` | Capacity estimates from charges |
| GET | `/api/v1/stats/locations` | Visited places |
