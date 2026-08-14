# 自建 Grafana + teslamate-query（Docker）

目标架构（**不再**用官方 TeslaMate 面板直连 Postgres SQL）：

```
Browser → Grafana :3000
              │  Infinity 插件 (HTTP JSON)
              ▼
         teslamate-query :8080
              │  JDBC read-only
              ▼
         TeslaMate PostgreSQL
```

## 前置

1. **Docker + Docker Compose**（Docker Desktop / OrbStack / 服务器上的 docker）
2. **可连的 TeslaMate Postgres**（本机、隧道 `localhost:5432`，或 compose 里的 `database` 服务）

## 一键启动

```bash
cd teslamate-query-grok45
cp .env.example .env
# 编辑 .env：填真实 DB_HOST / DB_USER / DB_PASS

docker compose up -d --build
```

| 服务 | URL |
|------|-----|
| Grafana | http://localhost:3000 （默认 `admin` / `admin`） |
| Query API | http://localhost:8080/api/v1/health |
| Swagger | http://localhost:8080/swagger-ui.html |

Dashboard 文件夹：**TeslaMate Query → Drives (REST)**（自动 provisioning）。

## DB 地址怎么填

| 场景 | `DB_HOST` |
|------|-----------|
| Postgres 在宿主机 / SSH 隧道映射到本机 5432 | `host.docker.internal`（compose 已配 `extra_hosts`） |
| 与官方 TeslaMate compose **同一 docker 网络** | 官方库服务名，一般是 `database`，并 `external: true` 挂到同一 network |
| 远端 IP | 直接写 IP/hostname，确保容器能路由到 |

compose 内 Grafana 访问 API **固定用服务名**：

```text
http://teslamate-query:8080
```

不要在 Grafana 容器里写 `localhost:8080`（那是 Grafana 自己，不是 query）。

## 鉴权

- 默认 **`AUTH_ENABLED=false`**：compose 内网 Grafana → query 无需 API Key  
- 若把 query 暴露到公网：`.env` 设 `AUTH_ENABLED=true` + 强 `API_KEYS`，并在 Infinity 数据源加 Header `X-API-Key`

## 本地只跑 Java（不启 Docker）

Postgres 先可达 `localhost:5432`：

```bash
export DB_HOST=localhost DB_NAME=teslamate DB_USER=teslamate DB_PASS=...
export AUTH_ENABLED=false
./mvnw spring-boot:run
# 或: java -jar target/teslamate-query-*.jar
```

Grafana 仍建议 Docker：

```bash
docker run --rm -p 3000:3000 \
  -e GF_INSTALL_PLUGINS=yesoreyeram-infinity-datasource \
  -e GF_SECURITY_ADMIN_PASSWORD=admin \
  grafana/grafana:11.5.2
```

本机开发时 Infinity 的 base URL 改为 `http://host.docker.internal:8080`（Grafana 在容器、API 在宿主机）。

## 面板怎么接 REST（Infinity）

1. Type: **JSON**，Source: **URL**
2. URL 示例：

```text
http://teslamate-query:8080/api/v1/drives?carId=${car_id}&from=${__from:date:iso}&to=${__to:date:iso}&size=200
```

3. Parser: **Backend** / root: `data`（分页列表）
4. 变量 `car_id`：URL = `http://teslamate-query:8080/api/v1/cars`，label=`name`，value=`id`

其它端点见 [GRAFANA_ENDPOINTS.md](./GRAFANA_ENDPOINTS.md)。

## 与「官方 TeslaMate Grafana」的关系

| | 官方 TeslaMate Grafana | 本栈 |
|--|------------------------|------|
| 数据源 | Postgres 插件 + 大量 SQL | Infinity + 我们的 REST |
| 面板 | 官方仓库 dashboards | 自己维护 `grafana/dashboards/*.json` |
| 依赖 | 直接读库 | 只依赖 query 服务契约 |

可以两套 Grafana 并存；本栈**不**挂载官方 SQL 面板。

## 常见问题

**query 起不来 / health DOWN**  
→ 容器打不到 Postgres。在宿主机 `psql`/`nc -z localhost 5432`，确认 `.env` 的 `DB_*`，看 `docker compose logs teslamate-query`。

**Grafana 面板空数据**  
→ 在容器网络测：`docker compose exec grafana wget -qO- http://teslamate-query:8080/api/v1/health`  
→ 确认变量 `car_id` 有值；时间范围是否过窄。

**插件 Infinity 未安装**  
→ 看 Grafana 日志；镜像启动时 `GF_INSTALL_PLUGINS=yesoreyeram-infinity-datasource` 需能访问 Grafana 插件站（离线环境需预装插件 volume）。
