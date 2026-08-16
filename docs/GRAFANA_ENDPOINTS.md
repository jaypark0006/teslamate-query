# Grafana → 核心 API

默认 **无鉴权**（`AUTH_ENABLED=false`，适合 compose 内网）。  
Infinity 数据源：`http://teslamate-query:8080`。

时间：`from=${__from:date:iso}&to=${__to:date:iso}`  
车：`carId=${car_id}` ← `GET /api/v1/cars`

Provisioning dashboards (`grafana/dashboards/`): Overview, Map, Trip, Drives, Drive.  
Drive uid `teslamate-query-drive` (`?var-drive_id=`). Trip uid `teslamate-query-trip` (picker window).

## 核心接口

| 用途 | Method / Path |
|------|----------------|
| Health | `GET /api/v1/health` |
| 车辆 | `GET /api/v1/cars` · `/cars/{carId}` · `/cars/{carId}/latest` |
| 车设置 | `GET /api/v1/car-settings` · `/car-settings/{carSettingsId}` |
| 全局设置 | `GET /api/v1/settings` |
| 围栏 | `GET /api/v1/geofences` · `/geofences/{geofenceId}` |
| 地址 | `GET /api/v1/addresses` · `?addressIds=` · `/{addressId}` |
| 行程 | `GET /api/v1/drives?...` · `/drives/{driveId}` · `/drives/{driveId}/positions` |
| 充电会话 | `GET /api/v1/charging-processes?...` · `/{chargingProcessId}` |
| 某次充电的全部采样 | `GET /api/v1/charging-processes/{chargingProcessId}/charges` 或 `GET /charges?chargingProcessId=` |
| 轨迹点 | `GET /api/v1/drives/{driveId}/positions` 或 `GET /positions?driveId=` |
| 状态 / 升级 | `GET /api/v1/states?...` · `/updates?...` |
| 多轨迹（start 落在窗内） | `GET /api/v1/map/tracks?carId&from&to` |
| Trip 地图 overlap + park | `GET /api/v1/map/trip?carId&from&to` 或 `GET /cars/{carId}/map` |
| Trip 扁平点（Geomap Route） | `GET /api/v1/cars/{carId}/map/points?from&to&kinds=drive\|charge\|park` |
| Trip 时间轴 | `GET /api/v1/cars/{carId}/timeline?from&to&minParkMin=` |
| Trip 日占用 | `GET /api/v1/cars/{carId}/timeline/daily?from&to&timezone=` |
| Trip 日×小时格子 | `GET /api/v1/cars/{carId}/timeline/grid?from&to&timezone=&dayStartHour=` |
| Trip 格子高亮 | `GET /api/v1/cars/{carId}/map/focus?day&slot&kind&from&to&id&timezone=&dayStartHour=` |
| SOC 曲线 | `GET /api/v1/series/battery?carId&from&to` |

列表 JSON 根路径多为 `data`（分页）。

## 超时（Map 路径容易超过默认）

真正掐断的是 **Grafana 等 API 的时间**，不是 Java 自己先关连接。

1. **Infinity 数据源** → Timeout = **180** 秒（插件默认 60）  
2. Grafana `grafana.ini`：`[dataproxy] timeout = 180`（或环境变量 `GF_DATAPROXY_TIMEOUT=180`，默认常为 30）

本仓库 compose / `infinity.yml` 已按 180 秒配好。已经手工建过的数据源要在 UI 里改一次并 Save。

## 刻意不做

- Overview / Drive Stats / Charging Stats / Efficiency / Vampire / Battery Health 等 **大段 Grafana 统计 SQL**
- 需要时再按业务加，或 Grafana 用核心数据自己聚合

## Auth

```yaml
AUTH_ENABLED: "false"   # 默认；仅 compose 内网
# AUTH_ENABLED: "true"
# API_KEYS: "secret"
```
