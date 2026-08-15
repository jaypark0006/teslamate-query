# Architecture（核心优先）

```
Controller → Service → Dao (Entity)
                 ↘ MapTracksService 多 Dao 编排
```

## 范围内

- 车辆 / 设置 / 围栏 / 地址  
- 行程、充电会话、采样、按 drive 的 positions  
- states / updates（简单表）  
- map/tracks、map/trip、cars/{id}/map/points、cars/{id}/timeline、cars/{id}/timeline/grid、series/battery  

## 范围外

- 从 Grafana 搬运的 gross consumption CTE、vampire、period rollup 等统计 SQL  
- 需要时再单独加接口，不堆进核心  

## 模式

1. Entity + `@ColumnName`（无嵌套 Table 类）  
2. 单表 Condition + 字符串拼接 WHERE  
3. `findIds` → `findByIds`  
4. 默认 `AUTH_ENABLED=false`（依赖 Docker 网络隔离）  
