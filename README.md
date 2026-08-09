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
