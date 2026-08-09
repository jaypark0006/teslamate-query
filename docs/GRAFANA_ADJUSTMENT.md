# Grafana 如何配合 REST API 调整

核心前提：

1. **每个页面的请求发起人是 Grafana**（时间窗、变量、面板刷新都由它驱动）
2. **现有 Grafana 筛选并不理想**（变量多、SQL 宏散、单位/续航模式每面板重复查）
3. REST 资源应 **领域独立、列表偏瘦**；Grafana 负责编排与展示，而不是逼 API 复刻宽表 SQL

目标结构：

```
Grafana 变量 / 时间选择器 / 少量筛选
        │  稳定、少量 HTTP
        ▼
teslamate-query  REST（瘦资源 + 明确 stats）
        │
        ▼
PostgreSQL（只读）
```

---

## 1. 先改「筛选心智」，再改面板

### 1.1 现在 Grafana 筛选的问题

| 问题 | 表现 | 后果 |
|------|------|------|
| 变量过载 | 几乎每页都有 `length_unit` / `temp_unit` / `preferred_range` / `base_url` | 每个面板重复 SQL；单位逻辑散落 |
| 筛选语义弱 | `location` 文本框、`exclude` 自由文本 | 无法稳定索引；API 也难做干净契约 |
| 时间宏绑死 SQL | `$__timeFilter(col)` | 换 REST 后必须统一成 `from`/`to` |
| 面板各自为政 | Overview 17 条 SQL、Charges 里又拼 AC/DC | 无法共享缓存与鉴权 |
| 宽表 join | 一行展示「地址名+能耗+AC/DC」 | 逼 API 做 Grafana 专用 join |

### 1.2 REST 时代 Grafana 该保留的筛选

**全局（Dashboard 级，尽量少）**

| 变量 | 来源 | 说明 |
|------|------|------|
| `car_id` | `GET /api/v1/cars` | 保留；用 Infinity 拉列表，label=name，value=id |
| 时间范围 | Grafana time picker | **唯一时间筛选**；映射为 `from=${__from:date:iso}&to=${__to:date:iso}` |
| `range` | 一次 `GET /settings` 的 `preferredRange`，允许用户覆盖 | **不要每面板查 settings** |
| `api_base` / datasource | 数据源配置 | `base_url` 仅用于链接回 TeslaMate UI，不必当 SQL 变量 |

**单位：不要再当「每面板 SQL 变量」**

- 一次读取 `GET /settings` → `unitOfLength` / `unitOfTemperature`
- 用 Grafana **Transform / Field override / 单位** 做 km↔mi、°C↔°F
- API 固定 metric（`distanceKm`、`*C`）——契约稳定

**页面级筛选（只留可索引、可契约化的）**

| 场景 | 建议保留 | 建议弱化/删除 |
|------|----------|----------------|
| Drives | `minDistance`, `minDuration`, `geofenceId`, `incompleteOnly` | 自由文本 `location`（改为选 geofence 或后续 `/addresses` 搜索） |
| Charges | `geofenceId`, `chargeType`, `incompleteOnly`, `minDuration` | `cost` 文本框等非结构化条件 |
| Stats | `groupBy` / `period` | 各面板私有 exclude 字符串 |
| Details | `driveId` / `chargingProcessId`（从列表 drill-down） | 详情页再查一遍 car 单位变量 |

### 1.3 筛选应由谁实现？

| 能力 | Grafana | API |
|------|---------|-----|
| 选车、时间窗 | ✅ 变量 + time picker | 接收 `carId, from, to` |
| 分页 | 可用，但列表优先服务端 page/size | ✅ `page/size` |
| geofence 多选 | 变量 multi | ✅ `geofenceId`（可扩展为 ids） |
| 单位换算 | ✅ Transform | ❌ 不要再 `$length_unit` 拼 SQL |
| 地址展示名 | 可用 Lookup/Join transform 拼 | 列表只给 `*Id`；或 opt-in enrich |
| 复杂 KPI | 少做前端拼 | ✅ `/stats/*`、`/overview` |

**原则：Grafana 负责「人话筛选」；API 负责「可缓存、可索引的查询参数」。**

---

## 2. 数据源与变量：一次性基建

### 2.1 数据源

1. 安装 **Infinity**（或 JSON API）插件  
2. Datasource URL：`http://teslamate-query:8080`  
3. 默认 Header：`X-API-Key: <key>`  
4. **保留** 原 Postgres DS 仅作双轨迁移；新面板不要再写 `rawSql`

### 2.2 推荐变量清单（全局 Dashboard 模板）

```text
car_id          Infinity → GET /api/v1/cars
                root: $  (数组)
                text: name   value: id

range           Custom: ideal,rated
                默认: 从 settings 读一次，或写死 ideal

# 可选只读展示（不进每个 query）
settings        Infinity → GET /api/v1/settings  （隐藏变量 / 仅用于单位显示）
```

**删除或降级的旧变量**

| 旧变量 | 处理 |
|--------|------|
| `length_unit` / `temp_unit` / `pressure_unit` / `speed_unit` | 删；用 settings + panel unit |
| `preferred_range` SQL 查询 | 改为 Custom 或 settings 一次 |
| `base_url` SQL | 配置成常量 / settings.baseUrl 一次 |
| `geofence` 大 SQL（含 placeholder） | `GET /geofences`；value=id, text=name；允许 All |
| 各种 `*_query` 隐藏变量（aux、exclude_formatted…） | 尽量消灭；逻辑进 API |

### 2.3 时间参数统一写法（Infinity）

```text
from=${__from:date:iso}
to=${__to:date:iso}
carId=${car_id}
range=${range}
```

所有列表/统计面板复用，不再出现 `$__timeFilter(start_date)`。

---

## 3. 面板怎么改：三种模式

Grafana 从「一条 SQL 搞定」改成「按资源取数 + 组合」。

### 模式 A — 单资源表（列表页）

适用于 Drives / Charges 主表。

```
GET /api/v1/drives?carId=&from=&to=&minDistance=&page=1&size=200
```

- Panel type: **Table**
- Infinity: type=JSON, root `data`
- 列直接绑 DTO 字段（瘦资源：`distanceKm`、`startGeofenceId`…）
- **不要** 期望一行里已有「起终点中文地址」——见模式 C

### 模式 B — 统计/KPI（Stats 页、Overview）

```
GET /api/v1/overview?carId=&from=&to=&range=
GET /api/v1/stats/drives?carId=&from=&to=&groupBy=day
```

- Stat / Time series / Bar chart 直接吃聚合 JSON
- **禁止** 为了一个 KPI 再跑 positions 全表 SQL

### 模式 C — 组合展示（需要名字时）

瘦资源只有 ID 时，Grafana 两种做法：

1. **推荐**：二次请求 + Transformation  
   - Query A: drives  
   - Query B: geofences（可 dashboard 级缓存/变量）  
   - Transform: **Outer join** / **Lookup fields from queries** 用 `startGeofenceId = id`  
2. **可选**：API `?view=enriched`（仅报表页开启，默认关闭）

地址同理：`addresses` 字典 join，而不是 drives SQL 里 `LEFT JOIN addresses`。

### 模式 D — 详情 drill-down

列表 → Data link：

```text
/d/drive-details?var-drive_id=${__data.fields.id}
```

详情页：

```
GET /drives/${drive_id}
GET /drives/${drive_id}/positions?downsample=5
```

Charge details 同理 → `/charging-processes/{id}` + `/samples`。

---

## 4. 按 Dashboard 的调整清单

### 4.1 Overview

| 现状 | 调整 |
|------|------|
| 十余条独立 SQL（battery、odometer、firmware…） | **1 次** `GET /overview` 填大部分 Stat |
| 另要 SOC 曲线 | `GET /positions?…` 或后续 `/series/battery`（可二期） |
| 每条 SQL 都带 car_id + timeFilter | 统一变量 |

筛选：只留 **car + time + range**。删掉面板内重复 settings 查询。

### 4.2 Drives

| 现状 | 调整 |
|------|------|
| 巨型 CTE join 地址/围栏/position/efficiency | 主表 → `GET /drives`（瘦） |
| 变量：min_dist, min_speed, location, geofence, efficiency 模式 | **保留** minDistance/minDuration/geofenceId；**location 文本降级**；efficiency 模式若要做放 API 可选参数，默认不算 slope |
| Incomplete 面板 | `incompleteOnly=true` 同一 endpoint |
| 地址列 | 模式 C join geofences；或 enriched 仅此 dashboard |

筛选优化建议：

- `geofence`：多选 ID，来自 `/geofences`  
- 去掉「任意 location 字符串」或做成显式「搜索」按钮式变量（少触发）  
- `min_speed`：若 API 暂无，先 Grafana filter 客户端过滤，或 API 补 `minSpeed`

### 4.3 Drive Details（internal）

| 现状 | 调整 |
|------|------|
| 16 条 SQL 绑 `drive_id` | `GET /drives/{id}` + `GET /drives/{id}/positions` |
| 单位变量一堆 | settings 一次 + field unit |
| 地图 | positions 的 lat/lon |

筛选：详情页 **几乎不要筛选**，只有 `drive_id`（从列表点进来）。

### 4.4 Charges

| 现状 | 调整 |
|------|------|
| join charges 算 AC/DC、max V、地址 | 列表瘦资源；`chargeType` 作 **可选 filter**（API 仅在过滤时聚合） |
| geofence / charge_type / min_duration / cost | 保留 geofence、type、minDuration；cost 过滤可二期 |
| Incomplete | `incompleteOnly=true` |

Grafana：AC/DC 饼图不要扫列表拼，改用 `GET /stats/charging` 的 `byType`。

### 4.5 Charge Details

同 Drive Details：id + samples 曲线。`determine_phases` 隐藏 SQL 变量删除。

### 4.6 Drive Stats / Charging Stats / Efficiency / Statistics / Mileage

| 现状 | 调整 |
|------|------|
| 各面板复制 CTE | 全部改为对应 `/stats/*` |
| 私有 exclude 文本 | 尽量去掉；需要「排除某 geofence」做成正式 API 参数 |
| period | Statistics 的 `period` 变量 → `GET /stats/period?period=` |

筛选：car + time + range + period/groupBy。其它自定义文本筛选默认不进 Grafana。

### 4.7 Timeline / States / Updates

| 面板 | API |
|------|-----|
| Timeline | `GET /timeline`（可加 type filter 二期） |
| States | `GET /states` |
| Updates | `GET /updates` |

旧的 `action_filter` / `text_filter`：能删则删；要留就变成 API 的 `types=drive,charge`。

### 4.8 Trip

| 现状 | 调整 |
|------|------|
| 16 面板 + 内部 from 变量 | `GET /trips/summary` 出 KPI；drives/charges 子表再调列表 API（**不要** 一个 JSON 塞全部宽行也可） |
| 地图 | positions 按时间窗 |

Trip 页筛选 = **就是时间窗 + car**（旅行定义=时间片），不要再叠一层弱筛选。

### 4.9 Vampire / Projected Range / Battery Health / Locations / Visited

全部走 `/stats/*` 或 locations。  
Visited 地图：`/positions` 降采样，而不是 lifetime 无界 SQL。

### 4.10 Database Info / Dutch tax

- Database Info：**不要** 进 teslamate-query 产品 API；保留 Postgres 或丢掉  
- Dutch tax：`GET /drives` export 列 + 时间窗即可  

---

## 5. 筛选 UX 优化（比旧 Grafana 更好的地方）

因为请求从 Grafana 发起，我们可以 **重新设计变量**，不必兼容旧 SQL 变量名。

### 5.1 推荐 Dashboard 变量布局

```
[ Car ▼ ]  [ Time range ]  [ Range: ideal|rated ]
[ Geofence ▼ multi ]  [ Min distance ]  [ Incomplete only ☑ ]
```

而不是：

```
length_unit, temp_unit, preferred_range, base_url, location text,
efficiency mode, exclude string, cost text, ...
```

### 5.2 级联与默认值

1. 打开 Dashboard → 隐式请求 `/settings` → 设置 range 默认、面板单位  
2. `/cars` → 默认第一辆或用户上次选择（Grafana 可记住）  
3. `/geofences` → 筛选下拉（可空 = 全部）  
4. 时间变化 → 所有 Infinity 面板自动带新 `from/to` 刷新  

### 5.3 减少请求风暴

| 做法 | 说明 |
|------|------|
| Overview 合并 | 1 个 overview 替代 N 个 Stat SQL |
| Geofences/Cars 变量级缓存 | 变量 query 刷新频率设「dashboard load」而非 on time change |
| Positions 强制 downsample | Grafana 不拉原始 1Hz |
| 列表 size 上限 | size=200；更多用分页或「load more」不适合 Grafana 时就导出 CSV |

### 5.4 单位与显示

- API：metric only  
- Grafana：根据 settings 设 Field unit（length: kilometre / mile）  
- 一处配置，全 Dashboard 继承  

---

## 6. 与「瘦 REST 资源」如何配合

```
┌─────────────────────────────────────────────────────────┐
│ Grafana Drives 页                                       │
│  Query A: GET /drives          → 表数据（ID + 指标）      │
│  Query B: GET /geofences       → 字典（变量或隐藏 query）  │
│  Transform: join on geofenceId → 显示名称                 │
│  （可选）Query C: GET /cars/$id → efficiency，算辅助列     │
└─────────────────────────────────────────────────────────┘
```

这样：

- API **不用** 为 Grafana 特化 join  
- Grafana **仍然** 能画出和以前类似的表  
- 筛选参数干净，后端可缓存、可索引  

若某页实在要「一枪宽表」（运营报表）：

```
GET /api/v1/reports/drives-table?...
```

**报表 ≠ 领域资源**，单独开，避免污染 `/drives`。

---

## 7. 迁移步骤（建议执行顺序）

### Phase G0 — 基建（0.5 天）

- [ ] Infinity DS + API Key  
- [ ] 全局变量模板：`car_id`, `range`  
- [ ] 文档化 `from/to` 写法  
- [ ] settings 单位只读一次  

### Phase G1 — 列表页替换（价值最高）

- [ ] Drives 主表 + Incomplete → `/drives`  
- [ ] Charges 主表 + Incomplete → `/charging-processes`  
- [ ] 详情页 → id + positions/samples  
- [ ] 旧 Postgres 面板隐藏，不删以便回滚  

### Phase G2 — Overview + Stats

- [ ] Overview 合并  
- [ ] Drive/Charging/Efficiency/Period/Mileage stats  

### Phase G3 — 其余

- [ ] Timeline / Trip / Vampire / Battery / Locations  
- [ ] 删 Postgres DS（或仅 ops 保留 database-info）  

### Phase G4 — 筛选打磨

- [ ] 去掉无用变量  
- [ ] geofence 多选体验  
- [ ] 列表分页或 size 策略  
- [ ] 按需加 `view=enriched` 或 reports  

---

## 8. 面板 URL 速查（Infinity）

```text
# 变量
GET /api/v1/cars
GET /api/v1/geofences
GET /api/v1/settings

# 列表
GET /api/v1/drives?carId=${car_id}&from=${__from:date:iso}&to=${__to:date:iso}&minDistance=${min_dist}&size=200
GET /api/v1/charging-processes?carId=${car_id}&from=...&to=...&chargeType=${charge_type}&size=200

# 详情
GET /api/v1/drives/${drive_id}
GET /api/v1/drives/${drive_id}/positions?downsample=5
GET /api/v1/charging-processes/${charging_process_id}/samples

# 统计
GET /api/v1/overview?carId=${car_id}&from=...&to=...&range=${range}
GET /api/v1/stats/drives?carId=${car_id}&from=...&to=...&groupBy=day&range=${range}
GET /api/v1/stats/charging?carId=${car_id}&from=...&to=...
GET /api/v1/stats/period?carId=${car_id}&from=...&to=...&period=${period}
```

---

## 9. 结论

| 问题 | 答案 |
|------|------|
| Grafana 要怎么调？ | 换 DS、砍变量、时间统一、`from/to` 调 REST；单位展示上移到 Grafana |
| 筛选怎么变好？ | 只留 car/time/geofence/数值阈值；删文本魔法与每面板 settings SQL |
| 和瘦 API 冲突吗？ | 不冲突：Grafana 用多 query + join transform，或可选 report 宽表 |
| 谁发起请求？ | 仍是 Grafana；但发起的是 **少而稳的 HTTP**，不是 150+ 裸 SQL |

**一句话：Grafana 从「SQL IDE」降级为「带时间窗的 REST 客户端」；筛选变少、变硬、变可缓存；宽表 join 要么在 Grafana 组合，要么进独立 report，不再绑架领域 API。**
