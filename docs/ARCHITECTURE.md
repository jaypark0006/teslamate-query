# Architecture (post-refactor)

```
Controller
  → Service (params, paging, multi-Dao orchestration)
      → SqlObject Dao + SearchCondition (list/filter)
      → StatsDao / AnalyticsDao (fixed aggregate SQL for Grafana KPIs)
```

## Patterns

1. **Entity = 1 table**: `entity/*Entity` + nested `Table.NAME` / `Table.COL_*` / `Table.COLUMNS`
2. **Condition = single-table only**: column names from `Table.*`, **no alias, no join**
3. **Multi-table**: Service multi-Dao (ids then load) — **not** Condition
4. **SqlObject Dao**: `@Define whereClause` + `@BindMap`; maps to **Entity**
5. **API DTO**: `EntityMapper` entity → dto for HTTP
6. **Analytics**: `StatsDao` / `AnalyticsDao` fixed SQL (optional Grafana KPIs)

See also `ENTITY_MODEL.md`.

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
