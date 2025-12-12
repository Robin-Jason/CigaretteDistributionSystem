## 后端接口说明（供前端参考）

> 版本：2025-11-27  
> 基础路径：`/api`（除非特别说明）  
> 所有接口默认支持跨域，返回 JSON，对错误会给出 `success=false` 或 HTTP 4xx/5xx。

---

### 1. 通用规范
- **鉴权**：当前环境未接入登录鉴权，生产环境按需补充。
- **日期字段**：所有年月周字段均为必填整数，格式 `(year: 4 位, month: 1-12, weekSeq: 1-5)`。
- **分页**：查询接口暂不分页，如需分页由前端处理或提需求。
- **编码表达式**：使用 `EncodeDecodeService` 生成/解析，接口已在返回体中附带。

---

### 2. 健康检查
| 接口 | 方法 | URL | 描述 | 请求体 | 返回体要点 |
| --- | --- | --- | --- | --- | --- |
| 服务存活检查 | GET | `/api/common/health` | 系统级心跳 | 无 | `status`, `message`, `timestamp` |
| 区域客户数服务心跳 | GET | `/api/cal-region-customer-num/health` | 区域客户统计模块心跳 | 无 | `status`, `service`, `message`, `timestamp` |

---

### 3. Excel 导入模块 (`ExcelImportController`)
| 场景 | 方法 | URL | 请求体 | 返回字段 | 说明 |
| --- | --- | --- | --- | --- | --- |
| 导入卷烟投放基础信息 | POST | `/api/import/cigarette-info` | `CigaretteImportRequestDto`（multipart form，包含 `year/month/weekSeq` 与上传文件 `file`） | `success`, `tableName`, `insertedCount`, `warnings[]`, `message` | 文件 ≤10 MB；服务会自动建/改 `cigarette_distribution_info_{y}_{m}_{w}` 表，并解析投放组合 |
| 导入客户基础信息 | POST | `/api/import/base-customer-info` | `BaseCustomerInfoImportRequestDto`（multipart form，包含 Excel 文件及 optional 覆盖策略） | `success`, `insertedCount`, `updatedCount`, `processedCount`, `message` | 自动创建/扩展 `base_customer_info`，并对缺失列做增量补齐 |

> 前端只需用 `multipart/form-data` 直接提交文件；其余参数按照 DTO 字段同名提交即可。

**请求示例**
```
POST /api/import/cigarette-info
Content-Type: multipart/form-data

file=@cigarette.xlsx
year=2025
month=9
weekSeq=3
overwrite=true
```

---

### 4. 区域客户数计算 (`CalRegionCustomerNumController`)
| 场景 | 方法 | URL | 请求体 | 返回字段 | 说明 |
| --- | --- | --- | --- | --- | --- |
| 计算区域客户数矩阵 | POST | `/api/cal-region-customer-num/calculate` | `CalRegionCustomerNumRequestDto`<br>```json {"year":2025,"month":10,"weekSeq":3,"customerTypes":["单周客户","正常客户"],"workdays":["周一","周三"]}``` | `success`, `message`, `createdTables[]`, `tableCount`, `filteredCustomerCount`, `customerTypes`, `workdays` | 根据客户类型/工作日过滤 `base_customer_info`，生成 5 张 `region_customerNum_*` 表，供旧算法使用 |

---

### 5. 分配计算与写回 (`DistributionCalculateController`)
| 场景 | 方法 | URL | 请求参数 / 体 | 返回字段 | 说明 |
| --- | --- | --- | --- | --- | --- |
| 执行策略并写回预测表 | POST | `/api/calculate/write-back` | Query 参数：`year`, `month`, `weekSeq`, 可选 `urbanRatio`, `ruralRatio`（仅档位+市场类型） | `success`, `message`, `totalCount`, `successCount`, `results[]`, `writeBackResults[]` | 根据 `cigarette_distribution_info_{y}_{m}_{w}` 逐条执行策略：含 TAG 或 “按价位段自选投放” 组合走新 orchestrator，其余走旧策略；成功后写入 `cigarette_distribution_prediction_{y}_{m}_{w}` |
| 一键生成分配方案 | POST | `/api/calculate/generate-distribution-plan` | 同上 | `success`, `operation`, `deletedExistingData`, `deletedRecords`, `allocationResult`（同上）、`processedCount`, `processingTime` | 先检测并清空已有预测表，再调用 `write-back` 逻辑 |
| 统计总实际投放量 | POST | `/api/calculate/total-actual-delivery` | Query 参数：`year`, `month`, `weekSeq` | `success`, `message`, `data`（Map: `cigCode_cigName`→`actualDelivery`), `totalRecords`, `cigaretteCount` | 基于预测表输出每支卷烟覆盖区域的实际投放总和 |

**请求示例**
```
POST /api/calculate/write-back?year=2025&month=9&weekSeq=3&urbanRatio=0.6&ruralRatio=0.4
```

**标准响应**
```json
{
  "success": true,
  "message": "OK",
  "totalCount": 12,
  "successCount": 11,
  "results": [
    {
      "cigCode": "12345678",
      "cigName": "某品牌",
      "algorithm": "StrategyOrchestrator",
      "writeBackStatus": "成功",
      "writeBackMessage": "写回完成"
    }
  ],
  "writeBackResults": [...]
}
```

---

### 6. 数据管理 (`DataManageController`)
| 场景 | 方法 | URL | 请求体 | 返回字段 | 说明 |
| --- | --- | --- | --- | --- | --- |
| 查询预测表+组合信息 | POST | `/api/data/query` | `QueryRequestDto`：`year/month/weekSeq` | `success`, `data[]`, `total`；每条包含档位、adv、actual、编码表达式等 | 聚合 `cigarette_distribution_prediction` + `cigarette_distribution_info` 数据，附带编码表达式 |
| 更新卷烟分配记录 | POST | `/api/data/update-cigarette` | `UpdateCigaretteRequestDto`（包含目标卷烟、年月周、投放类型、区域、各档位、备注等） | `success`, `message`, 具体操作统计 | 支持修改某支卷烟的分配类型与矩阵，内部自动校验投放组合合法性 |
| 删除指定区域 | POST | `/api/data/delete-delivery-areas` | `DeleteAreasRequestDto`（包含卷烟及要删除区域列表） | `success`, `deletedCount`, `remainingAreas`, `message` | 删除前会校验至少保留一个区域 |
| 重建区域客户统计 | POST | `/api/data/region-customer-statistics/rebuild` | `RegionCustomerStatisticsRequestDto`：`year/month/weekSeq`，可选 `overwriteExisting` | `success`, `targetTable`, `processedCombinationCount`, `combinationDetails[]`, `warnings[]` | 触发 Step2 区域矩阵重建，生成 `region_customer_statistics_{y}_{m}_{w}` |
| 编码表达式批量更新 | POST | `/api/data/batch-update-from-expressions` | `BatchUpdateFromExpressionsRequestDto`：卷烟标识 + `encodedExpressions[]` + 可选 `bz` | `success`, `operation`（投放类型变更/增量更新）、`deletedRecords` / `newAreas` 等 | 解析表达式后批量更新预测表，支持整单重建或增量 |

> **辅助服务**：`DataManageController` 内部还调用 `EncodeDecodeService` 与 `DistributionCalculateService`，前端无需额外请求即可获得总投放/编码信息。

**查询接口响应结构（节选）**
```json
{
  "success": true,
  "total": 24,
  "data": [
    {
      "cigCode": "12345678",
      "deliveryMethod": "按档位投放",
      "deliveryEtype": null,
      "deliveryArea": "全市",
      "advAmount": 1000,
      "actualDelivery": 980,
      "encodedExpression": "...",
      "decodedExpression": "...",
      "d30": "0.50",
      "...": "..."
    }
  ]
}
```

---

### 7. 区域客户统计中台 (`DataManageController` / `RegionCustomerStatisticsService`)
前端通常只需调用 `/api/data/region-customer-statistics/rebuild`。但若需要更细信息，可关注 `RegionCustomerStatisticsResponseDto`：
- `combinationDetails[]`：每个组合的执行状态、区域数量、失败原因；
- `warnings[]`：非法组合或未实现策略的提示（如 `按需投放` 占位）；
- `targetTable`：本次成功生成的矩阵表名，可用于后续分析。

---

### 8. 其他支撑服务
| 模块 | 说明 | 前端是否直接调用 |
| --- | --- | --- |
| `EncodeDecodeService` | 编码/解码投放表达式，已经在查询/批量更新接口中封装 | 否 |
| `StrategyOrchestrator` | 新策略编排入口，通过 `DistributionCalculateController` 间接触发 | 否 |
| `RegionMatrixProvider` | 自动构建/缓存 `region_customer_statistics_*` 矩阵 | 否 |

---

### 9. 前端调用建议
1. **导入顺序**：先导客户基础信息，再导卷烟投放信息，最后调用统计/写回接口。
2. **错误处理**：统一解析 `success` 和 `message` 字段；当 HTTP 状态为 500 且返回 `error` 字段时，可直接弹出 `message`。
3. **长耗时操作**：`/calculate/write-back`、`/calculate/generate-distribution-plan`、`/data/region-customer-statistics/rebuild` 均可能耗时数十秒，前端需做 loading 与轮询。
4. **占位策略监控**：当组合解析出“按需投放”“选点投放”时，响应中会出现 `warnings` 或 `error`，前端应将其提示给业务用户。
5. **编码表达式**：在查询界面可直接展示 `encodedExpression/decodedExpression`，并允许用户复制后提交到批量更新接口。

---

### 10. DTO 字段速查表

| DTO | 主要字段 | 说明 |
| --- | --- | --- |
| `CigaretteImportRequestDto` | `file`, `year`, `month`, `weekSeq`, `overwrite` | `file` 必填；`overwrite` 控制是否覆盖同名表。 |
| `BaseCustomerInfoImportRequestDto` | `file`, `sheetIndex`, `skipHeaderRows`, `overwriteMode` | `overwriteMode` 支持 `APPEND` / `REPLACE`。 |
| `CalRegionCustomerNumRequestDto` | `year`, `month`, `weekSeq`, `customerTypes[]`, `workdays[]` | `customerTypes` / `workdays` 可空，空则表示全选。 |
| `StrategyExecutionRequest`（内部） | `deliveryMethod`, `deliveryEtype`, `tag`, `targetAmount`, `year`, `month`, `weekSeq`, `extraInfo` | `extraInfo` 用于传递 `groupRatios`、`regionGroupMapping`、`segmentOrder` 等算法参数。 |
| `BatchUpdateFromExpressionsRequestDto` | `cigCode`, `cigName`, `year`, `month`, `weekSeq`, `encodedExpressions[]`, `bz` | 提供多条编码表达式，后端解析后自动更新。 |

---

### 11. 投放组合信息

| 维度 | 说明 | 来源 | 示例 |
| --- | --- | --- | --- |
| `DELIVERY_METHOD` | 投放方法（按档位 / 按档位扩展 / 按价位段自选 / 按需 / 选点） | `cigarette_distribution_info_{y}_{m}_{w}` 导入的 Excel 字段 | `"按档位扩展投放"` |
| `DELIVERY_ETYPE` | 扩展类型（可多个，用 `+` 分隔） | 同上 | `"区县+市场类型"`, `"区县"` |
| `TAG` | 标签（可多个，用 `+` 分隔） | 同上或编码表达式 | `"优质数据共享客户"` |
| `DeliveryCombination` | 解析后的结构体，包含枚举化的 `methodType`、`extensionTypes[]`、`tags[]` | 由 `DeliveryCombinationParser` 自动生成 | `method=GRADE_EXTEND, extensions=[COUNTY, MARKET_TYPE], tags=["优质数据共享客户"]` |

**获取/展示方式**
- `/api/data/query` 返回的数据里包含 `deliveryMethod`、`deliveryEtype`、`tag` 字段，前端可直接显示。
- `RegionCustomerStatisticsResponseDto.combinationDetails[]` 提供每次统计所涉及的组合及执行状态。
- `DistributionCalculateController` 的 `results[]` 中附带 `algorithm`/`targetType`，可用于定位组合走的是新引擎还是 Legacy。

**组合与算法映射规则（由后端自动处理）**
1. **单区域**（解析后只有一条 region，如“全市”） → `SingleLevelDistributionAlgorithm`。
2. **多区域，无比例/权重**（如“区县+业态”） → `ColumnWiseAdjustmentAlgorithm`。
3. **多区域 + 比例/权重**（如“区县+市场类型”且传了城/农网比例、或标签有单独配额） → `GroupSplittingDistributionAlgorithm`。
   - 前端若需要指定比例，可在 `/api/calculate/write-back` 请求中附带 `urbanRatio`/`ruralRatio`。
4. **特殊组合 / 占位**（按需/选点） → 后端会返回 `success=false` 且 `message` 提示“策略未实现”。

前端只需保证在导入/编辑时把 `DELIVERY_METHOD`、`DELIVERY_ETYPE`、`TAG` 字段填好，其余解析、矩阵构建与算法选择均由后端自动完成。

#### 11.1 投放方法（Delivery Method）

| 枚举值 | 文案 | 说明 |
| --- | --- | --- |
| `按档位投放` (`GRADE`) | 全市统一投放 | 只选择“全市/城网/农网”等单层区域。 |
| `按档位扩展投放` (`GRADE_EXTEND`) | 区县/市场类型等扩展投放 | 与扩展类型、标签组合形成笛卡尔积，支持多维度。 |
| `按价位段自选投放` (`PRICE_SEGMENT`) | 价位段自选投放 | 默认等同于“全市”；后续可叠加扩展/标签。 |
| `按需投放` (`ON_DEMAND`) | 按需投放（占位） | 目前未实现，后端会返回“策略未实现”。 |
| `选点投放` (`POINT_SELECTION`) | 选点投放（占位） | 目前未实现，同上。 |

#### 11.2 扩展投放类型（Delivery Extension Type）

| Field | 文案 | 说明 |
| --- | --- | --- |
| `区县` (`COUNTY`) | 区县 | 以 `COMPANY_DISTRICT` 为 key；常与市场类型组合。 |
| `市场类型` (`MARKET_TYPE`) | 城网/农网 | 支持配比（`urbanRatio`/`ruralRatio`）。 |
| `城乡分类代码` (`URBAN_RURAL_CODE`) | 城乡分类 | 依赖客户表 `CLASSIFICATION_CODE`。 |
| `业态` (`BUSINESS_FORMAT`) | 业态类型 | 结合 `CUST_FORMAT` 等字段；支持模糊匹配（KMP）。 |
| `市场部` (`MARKET_DEPARTMENT`) | 市场部 | 未配置列时会提示警告。 |
| `商圈类型` (`BUSINESS_DISTRICT_TYPE`) | 商圈类型 | 同上。 |
| `诚信自律小组` (`GROUP_NAME`) | 诚信组织 | 可与标签/其他扩展组合。 |
| `信用等级` (`CREDIT_LEVEL`) | 信用等级 | 用于高低信用筛选。 |

> 扩展类型可双选，并以 `+` 连接（如 `区县+市场类型`）。系统会自动生成笛卡尔积并检查所需列是否存在。

#### 11.3 标签集合（Tag）

当前内置标签：

| 标签 | 说明 | 默认提取规则 |
| --- | --- | --- |
| `优质客户` | 直接使用字段值（“是/否”等）。 | `ColumnValueTagValueExtractor` |
| `优质数据共享客户` | 字段非空时固定拼接“优质数据共享客户”。 | `ConstantTagValueExtractor` |
| `重点时段+重点地段` | 结合 `CRITICAL_TIME_AREA` 字段。 | 可根据值映射/扩展器调整 |

> 若后续有更多标签，可通过 `RegionCustomerStatisticsServiceImpl.registerTagWithExtractor` 注册，或在编码表达式中附带 TAG 字段。

---

如需进一步拆分 DTO 字段或新增分页/过滤条件，请在需求中说明，后端将同步更新本说明。***

