# teslamate-query

REST API facade over [TeslaMate](https://github.com/teslamate-org/teslamate) PostgreSQL.
Moves Grafana (and other clients) off raw SQL onto versioned, cacheable, authenticated HTTP APIs.

```
Grafana / clients
      │  HTTP JSON + X-API-Key
      ▼
teslamate-query
  ├─ Auth (API Key)
  ├─ Cache (Caffeine)
  └─ JDBC (read-only) ──► TeslaMate PostgreSQL
```

## Stack

- Java 21, Spring Boot 3.4
- JDBI 3 (SqlObject + ConstructorMapper) over Hikari read-only pool
- Caffeine cache
- springdoc OpenAPI (`/swagger-ui.html`)
- Docker / Compose

## Quick start

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export DB_HOST=localhost DB_PORT=5432 DB_NAME=teslamate
export DB_USER=teslamate DB_PASS=secret
export API_KEYS=dev-api-key

mvn spring-boot:run
```

- Swagger: http://localhost:8080/swagger-ui.html  
- Health: http://localhost:8080/api/v1/health  

```bash
curl -H "X-API-Key: dev-api-key" "http://localhost:8080/api/v1/cars"
```

## API surface

### Phase 0–1 (core)

| Method | Path |
|--------|------|
| GET | `/api/v1/health` |
| GET | `/api/v1/cars`, `/cars/{id}`, `/cars/{id}/latest` |
| GET | `/api/v1/settings` |
| GET | `/api/v1/geofences` |
| GET | `/api/v1/drives`, `/drives/{id}`, `/drives/{id}/positions` |
| GET | `/api/v1/charging-processes` (alias `/charges`) + detail + `/samples` |

### Phase 2 (stats)

| Method | Path |
|--------|------|
| GET | `/api/v1/overview` |
| GET | `/api/v1/stats/drives` |
| GET | `/api/v1/stats/charging` |
| GET | `/api/v1/stats/efficiency` |
| GET | `/api/v1/stats/period` |
| GET | `/api/v1/stats/mileage` |

### Phase 3 (advanced)

| Method | Path |
|--------|------|
| GET | `/api/v1/timeline` |
| GET | `/api/v1/trips/summary` |
| GET | `/api/v1/states` |
| GET | `/api/v1/updates` |
| GET | `/api/v1/positions` |
| GET | `/api/v1/stats/vampire-drain` |
| GET | `/api/v1/stats/projected-range` |
| GET | `/api/v1/stats/battery-health` |
| GET | `/api/v1/stats/locations` |

Full parameter docs: [docs/API.md](docs/API.md)

## Configuration

| Env | Default | Description |
|-----|---------|-------------|
| `DB_HOST` / `DB_PORT` / `DB_NAME` | localhost / 5432 / teslamate | Database |
| `DB_USER` / `DB_PASS` | teslamate / secret | Prefer read-only role |
| `API_KEYS` | dev-api-key | Comma-separated |
| `AUTH_ENABLED` | true | Local debug only: false |
| `SERVER_PORT` | 8080 | HTTP port |

Create a read-only role: [scripts/create-readonly-role.sql](scripts/create-readonly-role.sql)

## Grafana

See [docs/GRAFANA_ADJUSTMENT.md](docs/GRAFANA_ADJUSTMENT.md) for how to redesign Grafana variables/filters/panels, and [grafana/examples/README.md](grafana/examples/README.md) for Infinity snippets.

## Design notes

- **Sessions vs samples**: Grafana “Charges” → table `charging_processes`; curve points → `charges` via `/samples`
- **Units**: responses use metric field names; convert in Grafana if needed
- **Range mode**: `range=ideal|rated` (default from `settings.preferred_range`)
- **Gross consumption**: centralized in `StatsRepository` (shared Overview / Efficiency / etc.)
- **Positions**: time range required; optional `downsample` and `cleanOnly` (`ideal_battery_range_km IS NOT NULL`)
- **Security**: no access path to `private.tokens`

## Build & test

```bash
mvn -DskipTests package
mvn test
```

## Deploy

See [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md)

## License

Same usage model as your TeslaMate deployment; this service only reads the existing database.


## Data access layout

| Layer | Role |
|-------|------|
| `dao/*Dao` | JDBI SqlObject for stable queries (cars, settings, geofences, health) |
| `repository/*` | Extends `JdbiRepository`; dynamic filters via `SqlQueryBuilder` |
| `db/JdbiRepository` | `queryList` / `queryOne` / `mapTo(Dto.class)` — no manual ResultSet mapping |
| `config/JdbiConfig` | Plugins + snake_case → camelCase constructor mappers |

SQL column aliases are snake_case; DTOs stay camelCase. Computed fields (consumption, AC/DC, …) are projected in SQL, not in Java mappers.
