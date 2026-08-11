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

## 约定

| 层 | 做什么 |
|----|--------|
| Entity | 表行；列名只在 `@ColumnName` 写一次 |
| Dao | `SELECT * FROM drives` / 条件用 Condition；映射到 Entity |
| Condition | 单表 WHERE，字面量列名（`car_id`），无别名、无 join |
| 多表 | Service 多 Dao，不用 Condition 联查 |
| API DTO | `EntityMapper` 转换，可加派生字段 |

**不要**再抽 `Entity.Table` 常量类：和 `@ColumnName` 重复，改列要改两处。

Dao 用 `SELECT *` 即可，列增删时主要维护 Entity。
