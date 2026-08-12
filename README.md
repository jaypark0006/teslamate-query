# teslamate-query

只读 REST：把 TeslaMate Postgres 里 **行程 / 充电 / 轨迹 / 地图** 等核心数据提供给 Grafana（Infinity）或其它客户端。

```
Grafana  →  HTTP  →  teslamate-query  →  PostgreSQL (read-only)
```

**不做**：把官方全部 Grafana 统计 SQL 搬进服务。复杂 KPI 需要时再加。

## 跑起来

### A. Docker：query + 自建 Grafana（推荐）

见 **[docs/SELF_HOSTED_GRAFANA.md](docs/SELF_HOSTED_GRAFANA.md)**。

```bash
cp .env.example .env   # 填 TeslaMate Postgres
docker compose up -d --build
# Grafana  http://localhost:3000  (admin/admin)
# API      http://localhost:8080/swagger-ui.html
```

### B. 仅本地 Java

```bash
export DB_HOST=localhost DB_NAME=teslamate DB_USER=teslamate DB_PASS=...
export AUTH_ENABLED=false
./mvnw spring-boot:run
# Swagger: http://localhost:8080/swagger-ui.html
```

compose 内 Grafana 访问 API：`http://teslamate-query:8080`。公网暴露时再开 `AUTH_ENABLED=true`。

## GitHub CI

仓库已含 [`.github/workflows/ci.yml`](.github/workflows/ci.yml)：

| Job | 做什么 |
|-----|--------|
| **test** | JDK 21 + `mvn verify`（单元 / Web 切片测试，不连真实 DB） |
| **docker** | 构建镜像（不 push） |

触发：`push` / `PR` 到 `main` · `master` · `grok45`，或 Actions 页 **Run workflow**。

本地等价：

```bash
mvn -B verify
docker build -t teslamate-query:local .
```

以后若要 **push 到 GHCR**，在 workflow 里加 `docker/login-action` + `push: true`，并配置 `packages: write` 权限。

## 核心 API

见 [docs/GRAFANA_ENDPOINTS.md](docs/GRAFANA_ENDPOINTS.md)、[docs/ENTITY_MODEL.md](docs/ENTITY_MODEL.md)、[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)。

| 资源 | 路径 |
|------|------|
| cars / latest | `/api/v1/cars` |
| car_settings | `/api/v1/car-settings` |
| settings | `/api/v1/settings` |
| drives | `/api/v1/drives` |
| positions | `/api/v1/positions` |
| charging_processes | `/api/v1/charging-processes` |
| charges (samples) | `/api/v1/charges` |
| states / updates | `/api/v1/states` · `/updates` |
| addresses / geofences | `/api/v1/addresses` · `/geofences` |
| map GeoJSON | `/api/v1/map/tracks` |
| battery series | `/api/v1/series/battery` |

## 数据层约定

- **Entity** = 一表一行，`@ColumnName` 标列名  
- **Condition** = 单表 WHERE（无别名、无 join）  
- **多表** = Service 多 Dao（先 id 再 load）  
- 动态 WHERE **不用** StringTemplate（避免 `<>` 转义）

## 技术

Java 21 · Spring Boot **4.1** · JDBI 3 · PostgreSQL
