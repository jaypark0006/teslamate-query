# Grafana → API 对照（当前实现）

数据源：Infinity，Base URL `http://teslamate-query:8080`，Header `X-API-Key`.

时间统一：`from=${__from:date:iso}&to=${__to:date:iso}`  
车：`carId=${car_id}`（变量来自 `GET /api/v1/cars`）

## 变量

| Grafana 变量 | API |
|--------------|-----|
| car_id | `GET /api/v1/cars` text=name/vin value=id |
| units / range 默认 | `GET /api/v1/settings`（一次） |
| geofence | `GET /api/v1/geofences`（本库可能为空） |

## 仪表盘

| Dashboard | 用哪个 API | 说明 |
|-----------|------------|------|
| **Drives** | `GET /api/v1/drives?carId&from&to&minDistance&size=` | lean：先 id 再 load |
| incomplete | 同上 `&incompleteOnly=true` | |
| **Drive Details** | `GET /drives/{id}` + `GET /drives/{id}/positions` | 轨迹 |
| **Charges** | `GET /api/v1/charging-processes?carId&from&to` | lean 两步 |
| **Charge Details** | `GET /charging-processes/{id}` + `/samples` | |
| **Overview** | `GET /api/v1/overview?carId&from&to&range=` | KPI |
| **Drive Stats** | `GET /api/v1/stats/drives?...&groupBy=day` | |
| **Charging Stats** | `GET /api/v1/stats/charging?...` | |
| **Efficiency** | `GET /api/v1/stats/efficiency?...` | |
| **Statistics** | `GET /api/v1/stats/period?period=month` | |
| **Mileage** | `GET /api/v1/stats/mileage?...` | |
| **Timeline** | `GET /api/v1/timeline?...` | |
| **States** | `GET /api/v1/states?...` | |
| **Updates** | `GET /api/v1/updates?...` | |
| **Trip** | `GET /api/v1/trips/summary?...` | |
| **Vampire / Battery / Projected / Locations** | `/api/v1/stats/*` | |
| **Charge Level** | `GET /api/v1/series/battery?carId&from&to&limit=` | SOC 序列 |
| **多轨迹 + 充电点 Geomap** | `GET /api/v1/map/tracks?carId&from&to` | **GeoJSON FeatureCollection** |

## Geomap 配置要点

1. Infinity 请求 `/api/v1/map/tracks?...`
2. 解析根对象（即 FeatureCollection）
3. Geomap **GeoJSON** 层；或按 `properties.kind` 过滤 drive/charge  
4. LineString = 轨迹，Point = 充电  

可选宽表：`view=enriched`（仍走 join SQL，非主路径）。

## 多 Dao 编排（map/tracks）

```
DriveDao.findIds → DriveDao.findByIds
PositionDao.findByDriveIds
ChargingProcessDao.findIds → findByIds
PositionDao.findByIds(charge.positionId)
→ 组装 GeoJSON
```
