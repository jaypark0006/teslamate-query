# JdbiCondition（单表动态 WHERE）

## 用法

```java
var c = DriveSearchCondition.builder()
    .carId(1L)
    .startDateFrom(from)
    .minDistance(1.0)   // 生成 distance >= :minDistance，含 > 也安全
    .build();

// 字符串拼接，不用 StringTemplate
"SELECT id FROM drives " + c.whereClause() + " " + c.sortClause() + " LIMIT :limit"
```

## 为什么不用 StringTemplate / `@Define("<whereClause>")`

StringTemplate 把 `<` `>` 当模板语法。Condition 里常见：

```sql
charge_energy_added > 0
distance >= :min
```

若塞进 `<whereClause>` 会解析失败或要写成 `\>` 转义，又臭又长。

**本项目约定：动态片段用 Java 字符串拼接 + named bind；固定 SQL 才用 SqlObject 注解。**

JDBI 的 `IN (<ids>)` + `bindList` 是 JDBI 自己的占位语法，**不是** StringTemplate，可继续用。

## 范围

- Condition：仅 **单表** 过滤，列名无别名  
- 多表：Service 多 Dao，不把 join 写进 Condition  
