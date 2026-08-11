# Entity 模型（一表一实体）

每个 Entity **只对应一张表**，列名与库一致，写在 `Entity.Table` 常量里。

```
entity/DriveEntity.java
  DriveEntity.Table.NAME     = "drives"
  DriveEntity.Table.CAR_ID   = "car_id"
  DriveEntity.Table.COLUMNS  = "id, car_id, ..."
```

## 规则

1. **Entity** = 表行；`@ColumnName` 与 `Table.*` 常量一致  
2. **单表筛选** = Condition，列名来自 `Table.XXX`，**无表别名、不写 join**  
3. **多表** = Service 调多个 Dao（先 id 再 load），**不用 Condition 拼联查**  
4. **API DTO** = 对外 JSON；Service 用 `EntityMapper` 转换（可加派生字段如 avgSpeed）  

## 已建模的表

| Entity | Table |
|--------|-------|
| DriveEntity | drives |
| ChargingProcessEntity | charging_processes |
| PositionEntity | positions |
| ChargeEntity | charges |
| CarEntity | cars |
| SettingsEntity | settings |

其余表（states/updates/addresses…）可同样补 `*Entity.Table`。

## 示例

```java
// 列名确定
eq(DriveEntity.Table.CAR_ID, "carId", 1L);

// SQL 无别名
SELECT id, car_id, ... FROM drives WHERE car_id = :carId
```
