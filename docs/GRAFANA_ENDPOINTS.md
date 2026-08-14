# Grafana → 核心 API

默认 **无鉴权**（`AUTH_ENABLED=false`，适合 compose 内网）。  
Infinity 数据源：`http://teslamate-query:8080`。

时间：`from=${__from:date:iso}&to=${__to:date:iso}`  
车：`carId=${car_id}` ← `GET /api/v1/cars`

## 核心接口

| 用途 | Method / Path |
|------|----------------|
| Health | `GET /api/v1/health` |
| 车辆 | `GET /api/v1/cars` · `/cars/{carId}` · `/cars/{carId}/latest` |
| 车设置 | `GET /api/v1/car-settings` · `/car-settings/{carSettingsId}` |
| 全局设置 | `GET /api/v1/settings` |
| 围栏 | `GET /api/v1/geofences` · `/geofences/{geofenceId}` |
| 地址 | `GET /api/v1/addresses` · `?ids=` · `/{addressId}` |
| 行程 | `GET /api/v1/drives?...` · `/drives/{driveId}` · `/drives/{driveId}/positions` |
| 充电会话 | `GET /api/v1/charging-processes?...` · `/{chargingProcessId}` · `/{chargingProcessId}/samples` |
| 充电采样 | `GET /api/v1/charges/{chargingProcessId}` 或 `?chargingProcessId=` · `/{chargingProcessId}/samples` |
| 轨迹点 | `GET /api/v1/positions?driveId=` 或 `carId&from&to` · `/{positionId}` |
| 状态 / 升级 | `GET /api/v1/states?...` · `/updates?...` |
| 多轨迹地图 | `GET /api/v1/map/tracks?carId&from&to` |
| Trip 地图（驾/充/停） | `GET /api/v1/map/trip?carId&from&to` |
| SOC 曲线 | `GET /api/v1/series/battery?carId&from&to` |

列表 JSON 根路径多为 `data`（分页）。

## 刻意不做

- Overview / Drive Stats / Charging Stats / Efficiency / Vampire / Battery Health 等 **大段 Grafana 统计 SQL**
- 需要时再按业务加，或 Grafana 用核心数据自己聚合

## Auth

```yaml
AUTH_ENABLED: "false"   # 默认；仅 compose 内网
# AUTH_ENABLED: "true"
# API_KEYS: "secret"
```
