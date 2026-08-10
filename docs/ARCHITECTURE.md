# Architecture (post-refactor)

```
Controller
  → Service (params, paging, multi-Dao orchestration)
      → SqlObject Dao + SearchCondition (list/filter)
      → StatsDao / AnalyticsDao (fixed aggregate SQL for Grafana KPIs)
```

## Patterns

1. **JdbiCondition** (ASRS): `whereClause()` + `params()` + optional `sortClause()`
2. **SqlObject Dao**: `@Define whereClause` + `@BindMap` + `@RegisterConstructorMapper`
3. **Two-step load**: `findIds(condition)` → `findByIdsOrdered(ids)`
4. **Spring DI**: `jdbi.onDemand(XDao.class)` as `@Bean`
5. **No repository package** for resources; analytics live in `StatsDao` / `AnalyticsDao`

## Package map

| Package | Role |
|---------|------|
| `dao` | All data access |
| `db` | JdbiCondition, JdbiUpdate, IdOrder, JdbiRepository base |
| `db.condition` | Drive/ChargingProcess search builders |
| `service` | Business orchestration only |
| `api.v1` | REST |

## Grafana

See `GRAFANA_ENDPOINTS.md`. Lean resources only (enriched join path removed).
