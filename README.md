# teslamate-query

只读 REST：把 TeslaMate Postgres 里 **行程 / 充电 / 轨迹 / 地图** 等核心数据提供给 Grafana（Infinity）或其它客户端。

```
Grafana  →  HTTP  →  teslamate-query  →  PostgreSQL (read-only)
```

**不做**：把官方全部 Grafana 统计 SQL 搬进服务。复杂 KPI 需要时再加。

## 跑起来

```bash
export DB_HOST=database   # 或 localhost（隧道）
export DB_NAME=teslamate DB_USER=teslamate DB_PASS=...
# 默认关闭 API Key（compose 内网）
export AUTH_ENABLED=false

mvn spring-boot:run
# Swagger: http://localhost:8080/swagger-ui.html
```

生产 compose 内与 Grafana 同网：`http://teslamate-query:8080`，**不要**映射 8080 到公网。若要对公网暴露，再设 `AUTH_ENABLED=true` 与 `API_KEYS`。

## 核心 API

见 [docs/GRAFANA_ENDPOINTS.md](docs/GRAFANA_ENDPOINTS.md)、[docs/ENTITY_MODEL.md](docs/ENTITY_MODEL.md)、[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)。

| 资源 | 路径 |
|------|------|
| cars / latest | `/api/v1/cars` |
| settings | `/api/v1/settings` |
| drives + positions | `/api/v1/drives` |
| charging + samples | `/api/v1/charging-processes` |
| map GeoJSON | `/api/v1/map/tracks` |
| battery series | `/api/v1/series/battery` |

## 数据层约定

- **Entity** = 一表一行，`@ColumnName` 标列名  
- **Condition** = 单表 WHERE（无别名、无 join）  
- **多表** = Service 多 Dao（先 id 再 load）  
- 动态 WHERE **不用** StringTemplate（避免 `<>` 转义）

## 技术

Java 21 · Spring Boot 3 · JDBI 3 · PostgreSQL
