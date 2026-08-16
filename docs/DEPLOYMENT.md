# Deployment

## Environment

| Variable | Default | Description |
|----------|---------|-------------|
| DB_HOST | localhost | TeslaMate Postgres host |
| DB_PORT | 5432 | Port |
| DB_NAME | teslamate | Database |
| DB_USER | teslamate | Prefer dedicated read-only user |
| DB_PASS | secret | Password |
| API_KEYS | dev-api-key | Comma-separated API keys |
| AUTH_ENABLED | false | Set true when exposing beyond compose/LAN |
| SERVER_PORT | 8080 | HTTP port |
| DB_POOL_SIZE | 4 | Hikari pool size (keep small on a 2 GB host) |
| JAVA_TOOL_OPTIONS | `-Xms64m -Xmx256m -XX:+UseSerialGC` | Heap cap. Override only if this process is the only JVM |
| HTTP_IDLE_TIMEOUT | 180s | Netty idle timeout (keep long Grafana map queries open) |

## Read-only DB user

```bash
psql -U postgres -d teslamate -f scripts/create-readonly-role.sql
```

Then set `DB_USER=teslamate_query` and the password you chose.

## Docker Compose (alongside TeslaMate)

```yaml
services:
  teslamate-query:
    image: teslamate-query:local
    build: .
    environment:
      DB_HOST: database
      DB_USER: teslamate_query
      DB_PASS: change-me
      API_KEYS: ${API_KEYS}
    ports:
      - "8080:8080"
    depends_on:
      - database
```

## Memory (2 GB host)

The API defaults to a **256 MB** heap and **4** DB connections. Map paths are sampled in SQL (never 1 Hz for a multi-drive window). If this JVM shares the box with Postgres and Grafana, do not raise `-Xmx` without leaving ~1 GB for the database.

## Security notes

- Do not expose the service publicly without TLS and strong API keys
- Never grant SELECT on `private.tokens`
- Prefer network isolation (Docker internal network / reverse proxy)

## Compatibility

Built against TeslaMate schema (cars, drives, positions, charging_processes, charges, states, updates, addresses, geofences, settings, car_settings). Schema changes in TeslaMate major versions may require query updates.
