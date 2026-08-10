# JDBI Condition 模式（对齐 ASRS）

参考 `asrs` 的 `JdbiCondition` / `JdbiUpdate` + SqlObject `@Define` / `@BindMap`。

## 用法

```java
var condition = DriveSearchCondition.builder()
    .carId(1L)
    .startDateFrom(from)
    .startDateTo(to)
    .minDistance(5.0)
    .build();

long total = driveDao.count(condition);
List<Long> ids = driveDao.findIds(condition, limit, offset);
List<DriveDto> rows = driveDao.findByIdsOrdered(ids);
```

生成 SQL 片段：

- `condition.whereClause()` → `""` 或 `WHERE d.car_id = :carId AND ...`
- `condition.params()` → `@BindMap`
- `condition.sortClause()` → `ORDER BY d.start_date DESC`

Dao：

```java
@SqlQuery("SELECT d.id FROM drives d <whereClause> <sortClause> LIMIT :limit OFFSET :offset")
@UseStringTemplateEngine
List<Long> findIds(@Define("whereClause") String whereClause,
                   @Define("sortClause") String sortClause,
                   @BindMap Map<String, Object> params,
                   @Bind int limit, @Bind int offset);
```

## Spring DI

`JdbiConfig`：

- `SqlObjectPlugin` + `PostgresPlugin` + **全局 `StringTemplateEngine`**
- `SnakeCaseColumnNameMatcher`（一次）
- `jdbi.onDemand(DriveDao.class)` 等注册为 Bean
- Service 只注入 Dao + 拼 Condition，不写拼接 SQL

## 与 ASRS 的对应

| ASRS | teslamate-query |
|------|-----------------|
| `JdbiCondition` | `com.teslamate.query.db.JdbiCondition` |
| `JdbiUpdate` | `com.teslamate.query.db.JdbiUpdate` |
| `*SearchCondition` | `db.condition.DriveSearchCondition` 等 |
| `@Define whereClause` + `@BindMap` | 相同 |
| 代码生成 entity condition | 手写少量 condition（够用） |

读路径主推 Condition；写路径预留 Update（当前几乎只读）。
