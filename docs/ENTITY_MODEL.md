# Entity 模型

**一表一 Entity**，字段用 `@ColumnName("物理列名")` 标注即可。

```java
/** Table: drives */
public record DriveEntity(
    @ColumnName("id") Long id,
    @ColumnName("car_id") Long carId,
    ...
) {}
```

## 业务表覆盖

| Table | Entity | Condition | list/byId API |
|-------|--------|-----------|---------------|
| cars | CarEntity | — (small) | `/cars` |
| car_settings | CarSettingsEntity | — | `/car-settings` |
| settings | SettingsEntity | — | `/settings` |
| drives | DriveEntity | DriveSearchCondition | `/drives` |
| charging_processes | ChargingProcessEntity | ChargingProcessSearchCondition | `/charging-processes` |
| positions | PositionEntity | PositionSearchCondition | `/positions` (scoped) |
| charges | ChargeEntity | ChargeSearchCondition | `/charges` (scoped) |
| states | StateEntity | StateSearchCondition | `/states` |
| updates | UpdateEntity | UpdateSearchCondition | `/updates` |
| addresses | AddressEntity | AddressSearchCondition | `/addresses` |
| geofences | GeofenceEntity | GeofenceSearchCondition | `/geofences` |

`positions` / `charges` 必须带范围条件（driveId 或 carId+时间 / processId 或时间），防止全表扫。

## 约定

| 层 | 做什么 |
|----|--------|
| Entity | 表行；列名只在 `@ColumnName` 写一次 |
| Dao | `count` / `findIds` / `findByIds` / `findById`；映射到 Entity |
| Condition | 单表 WHERE，字面量列名（`car_id`），无别名、无 join |
| 多表 | Service 多 Dao，不用 Condition 联查（如 cars + car_settings → CarDto） |
| API DTO | `EntityMapper` + `DisplayUnits`：DB 公制 → 响应按 `lengthUnit`/`tempUnit` 换算 |
| 单位 | 不在 SQL 里 `convert_km`；`UnitConverter` 在出库后处理 |

**不要**再抽 `Entity.Table` 常量类：和 `@ColumnName` 重复，改列要改两处。

Dao 用 `SELECT *` 即可（`states.state` 为 PG enum 时 `state::text`）。

## Dynamic WHERE

Condition fragments are concatenated in Java (see `JDBI_CONDITION.md`). No StringTemplate escaping of `<`/`>`.
