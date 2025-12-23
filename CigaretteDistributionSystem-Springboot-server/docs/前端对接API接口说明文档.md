# 烟草分配系统 - 前端对接API接口说明文档

> **版本**: v2.0  
> **更新时间**: 2025-12-23  
> **作者**: Robin  
> **目标读者**: 前端开发团队

---

## 目录

- [一、文档概述](#一文档概述)
- [二、统一响应格式](#二统一响应格式)
- [三、分配计算接口](#三分配计算接口)
- [四、数据导入接口](#四数据导入接口)
- [五、预测数据查询与维护接口](#五预测数据查询与维护接口)
- [六、统计查询接口](#六统计查询接口)
- [七、错误码说明](#七错误码说明)
- [八、调用示例](#八调用示例)

---

## 一、文档概述

### 1.1 系统简介

烟草分配系统是一个基于 Spring Boot 的卷烟投放分配计算平台，主要功能包括:

- **数据导入**: 支持客户基础信息表和卷烟投放基础信息表的 Excel 导入
- **分配计算**: 基于多种算法(单层分配、多区域分配、价位段分配等)自动计算卷烟投放方案
- **策略调整**: 支持对单个卷烟进行投放策略的人工调整
- **数据查询**: 提供预测数据、价位段数据、统计数据的查询接口
- **数据维护**: 支持预测分配数据的增删改操作

### 1.2 接口基础信息

- **Base URL**: `http://{host}:{port}`
- **默认端口**: `8080` (可配置)
- **接口风格**: RESTful
- **数据格式**: JSON
- **字符编码**: UTF-8
- **跨域支持**: 已启用 CORS，允许所有来源 (`origins = "*"`)

### 1.3 通用请求头

```http
Content-Type: application/json
Accept: application/json
```

对于文件上传接口:
```http
Content-Type: multipart/form-data
```

---

## 二、统一响应格式

所有接口均采用统一的响应格式 `ApiResponseVo<T>`：

### 2.1 响应结构

```json
{
  "success": true,           // Boolean 是否成功
  "message": "操作成功",      // String 响应消息
  "errorCode": null,         // String 错误码(成功时为null)
  "data": {},                // T 响应数据(泛型类型)
  "timestamp": 1703308800000 // Long 响应时间戳(毫秒)
}
```

### 2.2 成功响应示例

```json
{
  "success": true,
  "message": "查询成功",
  "errorCode": null,
  "data": {
    "total": 100,
    "items": [...]
  },
  "timestamp": 1703308800000
}
```

### 2.3 失败响应示例

```json
{
  "success": false,
  "message": "卷烟代码不能为空",
  "errorCode": "VALIDATION_ERROR",
  "data": null,
  "timestamp": 1703308800000
}
```

---

## 三、分配计算接口

### 3.1 一键生成分配方案

**功能**: 全量重建指定分区（year/month/weekSeq）的卷烟分配记录，并写回预测表。该接口会同时处理标准分配（按档位投放、按档位扩展投放）和价位段分配（按价位段自选投放）。

**接口地址**: `POST /api/calculate/generate-distribution-plan`

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 | 约束 |
|--------|------|------|------|------|
| year | Integer | 是 | 年份 | 2020-2099 |
| month | Integer | 是 | 月份 | 1-12 |
| weekSeq | Integer | 是 | 周序号 | 1-5 |
| urbanRatio | BigDecimal | 否 | 城网比例 | 仅用于"档位+市场类型"扩展投放，需与 ruralRatio 同时提供 |
| ruralRatio | BigDecimal | 否 | 农网比例 | 仅用于"档位+市场类型"扩展投放，需与 urbanRatio 同时提供 |

**请求示例**:

```json
{
  "year": 2025,
  "month": 9,
  "weekSeq": 3,
  "urbanRatio": 0.6,
  "ruralRatio": 0.4
}
```

**响应数据结构** (`GenerateDistributionPlanResponseVo`):

| 字段名 | 类型 | 说明 |
|--------|------|------|
| success | Boolean | 是否成功 |
| message | String | 响应消息 |
| errorCode | String | 错误码(失败时) |
| year | Integer | 年份 |
| month | Integer | 月份 |
| weekSeq | Integer | 周序号 |
| deletedExistingData | Boolean | 是否删除了现有数据 |
| deletedRecords | Integer | 删除的记录数 |
| processedCount | Integer | 生成的总记录数 |
| processingTime | String | 处理耗时(如"1500ms") |
| totalCigarettes | Integer | 处理的卷烟总数 |
| successfulAllocations | Integer | 成功分配的卷烟数 |

**成功响应示例**:

```json
{
  "success": true,
  "message": "生成分配计划成功",
  "errorCode": null,
  "data": {
    "success": true,
    "message": "生成分配计划成功",
    "year": 2025,
    "month": 9,
    "weekSeq": 3,
    "deletedExistingData": true,
    "deletedRecords": 150,
    "processedCount": 200,
    "processingTime": "1500ms",
    "totalCigarettes": 50,
    "successfulAllocations": 48
  },
  "timestamp": 1703308800000
}
```

**失败响应示例**:

```json
{
  "success": false,
  "message": "生成分配计划失败: 未找到投放基础信息",
  "errorCode": "GENERATION_FAILED",
  "data": null,
  "timestamp": 1703308800000
}
```

**业务说明**:

1. **删除逻辑**: 执行前会删除指定分区内的现有分配数据，确保全量重建
2. **分流处理**: 
   - 标准分配: 按档位投放、按档位扩展投放 → 写入 `cigarette_distribution_prediction` 表
   - 价位段分配: 按价位段自选投放 → 写入 `cigarette_distribution_prediction_price` 表
3. **市场比例**: urbanRatio + ruralRatio 只在扩展类型为"档位+市场类型"时生效，用于拆分城网/农网的投放目标量
4. **幂等性**: 相同参数多次调用结果一致（会先删除旧数据）

---

### 3.2 计算总实际投放量

**功能**: 计算指定分区（year/month/weekSeq）内所有卷烟的总实际投放量，用于对账和看板汇总。

**接口地址**: `POST /api/calculate/total-actual-delivery`

**请求参数**: 通过 URL Query 参数传递

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| year | Integer | 是 | 年份 |
| month | Integer | 是 | 月份 |
| weekSeq | Integer | 是 | 周序号 |

**请求示例**:

```http
POST /api/calculate/total-actual-delivery?year=2025&month=9&weekSeq=3
```

**响应数据结构** (`TotalActualDeliveryResponseVo`):

| 字段名 | 类型 | 说明 |
|--------|------|------|
| success | Boolean | 是否成功 |
| message | String | 响应消息 |
| year | Integer | 年份 |
| month | Integer | 月份 |
| weekSeq | Integer | 周序号 |
| data | Map<String, BigDecimal> | 按卷烟分组的投放量数据 |
| totalRecords | Integer | 记录总数 |
| cigaretteCount | Integer | 卷烟种类数 |

**成功响应示例**:

```json
{
  "success": true,
  "message": "总实际投放量计算成功",
  "errorCode": null,
  "data": {
    "year": 2025,
    "month": 9,
    "weekSeq": 3,
    "data": {
      "001_红塔山": 1000.50,
      "002_云烟": 2000.00,
      "003_玉溪": 1500.75
    },
    "totalRecords": 150,
    "cigaretteCount": 3
  },
  "timestamp": 1703308800000
}
```

---

### 3.3 调整卷烟投放策略

**功能**: 对单个卷烟进行投放策略调整，包括修改投放方式、扩展类型、标签、投放量等，并重新计算分配方案。

**接口地址**: `POST /api/calculate/adjust-strategy`

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| year | Integer | 是 | 年份 |
| month | Integer | 是 | 月份 |
| weekSeq | Integer | 是 | 周序号 |
| cigCode | String | 是 | 卷烟代码 |
| cigName | String | 是 | 卷烟名称 |
| newDeliveryMethod | String | 是 | 新投放类型(如"按档位投放") |
| newDeliveryEtype | String | 否 | 新扩展投放类型(如"区县公司+市场类型") |
| newTag | String | 否 | 新标签(最多1个) |
| newTagFilterValue | String | 否 | 新标签过滤值(与newTag配套) |
| newAdvAmount | BigDecimal | 是 | 新建议投放量(必须>0) |

**请求示例**:

```json
{
  "year": 2025,
  "month": 9,
  "weekSeq": 3,
  "cigCode": "42010020",
  "cigName": "红金龙(硬神州腾龙)",
  "newDeliveryMethod": "按档位投放",
  "newDeliveryEtype": null,
  "newTag": "优质数据共享客户",
  "newTagFilterValue": "是",
  "newAdvAmount": 1000
}
```

**响应数据结构** (`AdjustCigaretteStrategyResponseVo`):

| 字段名 | 类型 | 说明 |
|--------|------|------|
| success | Boolean | 是否成功 |
| message | String | 响应消息 |
| cigCode | String | 卷烟代码 |
| cigName | String | 卷烟名称 |
| newRecordsCount | Integer | 新生成的分配记录数 |
| actualDelivery | BigDecimal | 实际投放量 |

**成功响应示例**:

```json
{
  "success": true,
  "message": "调整卷烟投放策略成功",
  "errorCode": null,
  "data": {
    "success": true,
    "message": "调整成功",
    "cigCode": "42010020",
    "cigName": "红金龙(硬神州腾龙)",
    "newRecordsCount": 15,
    "actualDelivery": 995.50
  },
  "timestamp": 1703308800000
}
```

**业务说明**:

1. 会先删除该卷烟的旧分配记录
2. 重新执行分配算法并写回数据库
3. 自动在 Info 表备注字段添加"已人工调整策略"标记

---

### 3.4 获取可用投放区域列表

**功能**: 根据投放类型和扩展类型，解析所有可能的投放区域，检查并构建区域客户统计数据，返回完整的可用区域列表。

**接口地址**: `POST /api/calculate/available-regions`

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| year | Integer | 是 | 年份 |
| month | Integer | 是 | 月份 |
| weekSeq | Integer | 是 | 周序号 |
| deliveryMethod | String | 是 | 投放类型(如"按档位投放") |
| deliveryEtype | String | 否 | 扩展类型(如"区县公司+市场类型") |

**请求示例**:

```json
{
  "year": 2025,
  "month": 9,
  "weekSeq": 3,
  "deliveryMethod": "按档位投放",
  "deliveryEtype": "区县公司+市场类型"
}
```

**响应数据结构** (`GetAvailableRegionsResponseVo`):

| 字段名 | 类型 | 说明 |
|--------|------|------|
| success | Boolean | 是否成功 |
| message | String | 响应消息 |
| regions | List<String> | 可用区域列表 |
| builtNewData | Boolean | 是否构建了新数据 |
| builtRegions | List<String> | 新构建的区域列表 |

**成功响应示例**:

```json
{
  "success": true,
  "message": "获取可用投放区域列表成功",
  "errorCode": null,
  "data": {
    "success": true,
    "regions": ["丹江+城网", "丹江+农网", "武当山+城网", "武当山+农网"],
    "builtNewData": true,
    "builtRegions": ["武当山+农网"],
    "message": "查询成功，已追加构建1个区域的客户数据"
  },
  "timestamp": 1703308800000
}
```

**业务说明**:

1. 如果某区域在 `region_customer_statistics` 表中不存在，会自动触发构建
2. 用于策略调整功能，为前端提供可选的区域下拉列表

---

## 四、数据导入接口

### 4.1 导入客户基础信息表

**功能**: 全量覆盖 `base_customer_info` 表，同步刷新诚信互助小组编码映射表，返回诚信互助小组映射信息。

**接口地址**: `POST /api/import/base-customer`

**请求参数**: `multipart/form-data`

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| file | MultipartFile | 是 | 客户基础信息表Excel文件 |

**文件要求**:
- 格式: `.xls` 或 `.xlsx`
- 大小: 最大 10MB
- 内容: 必须包含所需的客户基础信息列（客户编码、客户名称、区县公司、市场类型、档位、诚信互助小组等）

**请求示例** (使用 `curl`):

```bash
curl -X POST http://localhost:8080/api/import/base-customer \
  -F "file=@base_customer_info.xlsx"
```

**请求示例** (使用 JavaScript Fetch):

```javascript
const formData = new FormData();
formData.append('file', fileInput.files[0]);

fetch('http://localhost:8080/api/import/base-customer', {
  method: 'POST',
  body: formData
})
.then(response => response.json())
.then(data => console.log(data));
```

**响应数据结构** (`BaseCustomerInfoImportResponseVo`):

| 字段名 | 类型 | 说明 |
|--------|------|------|
| success | Boolean | 是否成功 |
| message | String | 响应消息 |
| insertedCount | Integer | 插入的记录数 |
| processedCount | Integer | 处理的记录数 |
| tableName | String | 表名 |
| integrityGroupMapping | Map<String, Object> | 诚信互助小组映射信息 |

**成功响应示例**:

```json
{
  "success": true,
  "message": "客户基础信息表导入成功",
  "errorCode": null,
  "data": {
    "success": true,
    "message": "导入成功，共处理 5000 条记录",
    "insertedCount": 5000,
    "processedCount": 5000,
    "tableName": "base_customer_info",
    "integrityGroupMapping": {
      "groupCount": 10,
      "groups": {
        "G001": { "name": "诚信互助小组1", "memberCount": 50 },
        "G002": { "name": "诚信互助小组2", "memberCount": 60 }
      }
    }
  },
  "timestamp": 1703308800000
}
```

**失败响应示例**:

```json
{
  "success": false,
  "message": "导入失败: Excel文件格式错误，缺少必填列",
  "errorCode": "IMPORT_FAILED",
  "data": null,
  "timestamp": 1703308800000
}
```

**业务说明**:

1. **全量覆盖**: 导入前会清空 `base_customer_info` 表
2. **诚信互助小组**: 自动刷新 `integrity_group_mapping` 映射表
3. **校验规则**: 
   - 客户编码、客户名称、区县公司、市场类型、档位为必填
   - 档位必须为 D1-D30 中的一个
   - Excel 列名不区分大小写

---

### 4.2 导入卷烟投放基础信息表

**功能**: 覆盖 `cigarette_distribution_info` 对应分区数据，执行业务合法性校验（全市占比、货源属性规则等）。

**接口地址**: `POST /api/import/cigarette`

**请求参数**: `multipart/form-data`

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| file | MultipartFile | 是 | 卷烟投放基础信息表Excel文件 |
| year | Integer | 是 | 年份 (2020-2099) |
| month | Integer | 是 | 月份 (1-12) |
| weekSeq | Integer | 是 | 周序号 (1-5) |

**文件要求**:
- 格式: `.xls` 或 `.xlsx`
- 大小: 最大 10MB
- 内容: 必须包含所需的卷烟投放信息列（卷烟代码、卷烟名称、建议投放量ADV、投放方式、扩展投放方式、投放区域、标签、货源属性等）

**请求示例** (使用 `curl`):

```bash
curl -X POST "http://localhost:8080/api/import/cigarette" \
  -F "file=@cigarette_distribution_info.xlsx" \
  -F "year=2025" \
  -F "month=9" \
  -F "weekSeq=3"
```

**请求示例** (使用 JavaScript Fetch):

```javascript
const formData = new FormData();
formData.append('file', fileInput.files[0]);
formData.append('year', 2025);
formData.append('month', 9);
formData.append('weekSeq', 3);

fetch('http://localhost:8080/api/import/cigarette', {
  method: 'POST',
  body: formData
})
.then(response => response.json())
.then(data => console.log(data));
```

**响应数据结构** (`CigaretteImportResponseVo`):

| 字段名 | 类型 | 说明 |
|--------|------|------|
| success | Boolean | 是否成功 |
| message | String | 响应消息 |
| insertedCount | Integer | 插入的记录数 |
| totalRows | Integer | Excel总行数 |

**成功响应示例**:

```json
{
  "success": true,
  "message": "卷烟投放基础信息表导入成功",
  "errorCode": null,
  "data": {
    "success": true,
    "message": "导入成功，共处理 50 条记录",
    "insertedCount": 50,
    "totalRows": 50
  },
  "timestamp": 1703308800000
}
```

**失败响应示例** (校验失败):

```json
{
  "success": false,
  "message": "导入校验失败: 卷烟'001-红塔山'的全市投放占比为30%，低于配置的50%阈值",
  "errorCode": "IMPORT_FAILED",
  "data": null,
  "timestamp": 1703308800000
}
```

**业务说明**:

1. **分区覆盖**: 导入前会删除指定分区（year/month/weekSeq）的现有数据
2. **业务校验**:
   - **全市占比校验**: 投放区域为"全市"的卷烟数量占比需达到配置阈值（默认50%）
   - **货源属性校验**: 货源属性、投放方式、标签的组合必须符合配置规则
   - 示例: 货源属性为"紧俏烟"时，投放方式只能为"按档位投放"，且不允许设置标签
3. **校验配置**: 规则定义在 `import-validation-rules.yml` 文件中
4. **失败回滚**: 任何校验失败都会导致整个导入操作回滚，不会写入数据库

---

## 五、预测数据查询与维护接口

### 5.1 按时间分区查询预测数据

**功能**: 查询指定分区（year/month/weekSeq）的标准预测分配数据（来自 `cigarette_distribution_prediction` 表）。

**接口地址**: `GET /api/prediction/list-by-time`

**请求参数**: URL Query 参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| year | Integer | 是 | 年份 |
| month | Integer | 是 | 月份 |
| weekSeq | Integer | 是 | 周序号 |

**请求示例**:

```http
GET /api/prediction/list-by-time?year=2025&month=9&weekSeq=3
```

**响应数据结构** (`PredictionQueryResponseVo`):

| 字段名 | 类型 | 说明 |
|--------|------|------|
| data | List<Map<String, Object>> | 预测数据列表 |
| total | Integer | 总记录数 |

每条记录包含以下字段:

| 字段名 | 类型 | 说明 |
|--------|------|------|
| cig_code | String | 卷烟代码 |
| cig_name | String | 卷烟名称 |
| deployinfo_code | String | 编码表达式 |
| delivery_method | String | 投放方式 |
| delivery_etype | String | 扩展投放方式 |
| delivery_area | String | 投放区域 |
| tag | String | 标签 |
| tag_filter_config | String | 标签过滤配置 |
| adv | BigDecimal | 建议投放量 |
| D30~D1 | BigDecimal | 30个档位的投放量(D30为最高档) |
| bz | String | 备注 |
| actual_delivery | BigDecimal | 实际投放量 |

**成功响应示例**:

```json
{
  "success": true,
  "message": "查询成功",
  "errorCode": null,
  "data": {
    "data": [
      {
        "cig_code": "001",
        "cig_name": "红塔山",
        "deployinfo_code": "丹江+城网(D10*1)",
        "delivery_method": "按档位投放",
        "delivery_etype": "区县公司+市场类型",
        "delivery_area": "丹江+城网",
        "tag": null,
        "tag_filter_config": null,
        "adv": 1000.00,
        "D30": 0, "D29": 0, ..., "D10": 1, ..., "D1": 0,
        "bz": null,
        "actual_delivery": 50.00
      }
    ],
    "total": 100
  },
  "timestamp": 1703308800000
}
```

---

### 5.2 按时间分区查询价位段预测数据

**功能**: 查询指定分区（year/month/weekSeq）的价位段预测数据（来自 `cigarette_distribution_prediction_price` 表）。

**接口地址**: `GET /api/prediction/list-price-by-time`

**请求参数**: URL Query 参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| year | Integer | 是 | 年份 |
| month | Integer | 是 | 月份 |
| weekSeq | Integer | 是 | 周序号 |

**请求示例**:

```http
GET /api/prediction/list-price-by-time?year=2025&month=9&weekSeq=3
```

**响应格式**: 同 5.1，数据结构相同，但来源表不同

---

### 5.3 懒加载查询聚合编码表达式

**功能**: 按批次 + 卷烟代码查询"多区域聚合编码表达式"，用于前端表格的懒加载展示，避免一次性加载全部区域数据。

**接口地址**: `GET /api/prediction/aggregated-encodings`

**请求参数**: URL Query 参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| year | Integer | 是 | 年份 |
| month | Integer | 是 | 月份 |
| weekSeq | Integer | 是 | 周序号 |
| cigCode | String | 是 | 卷烟代码 |

**请求示例**:

```http
GET /api/prediction/aggregated-encodings?year=2025&month=9&weekSeq=3&cigCode=001
```

**响应数据**: `List<String>` 聚合编码表达式列表

**成功响应示例**:

```json
{
  "success": true,
  "message": "查询成功",
  "errorCode": null,
  "data": [
    "丹江+城网(D10*1)",
    "丹江+农网(D10*1)",
    "武当山+城网(D15*1+D10*1)"
  ],
  "timestamp": 1703308800000
}
```

**业务说明**:

1. **聚合规则**: 只有当多个区域的档位分配结构完全一致时才会聚合
2. **表达式格式**: "区域名(档位*数量+档位*数量+...)"
3. **使用场景**: 前端表格展示时，点击"查看编码"按钮后调用

---

### 5.4 新增投放区域分配记录

**功能**: 为指定卷烟新增一条投放区域分配记录。

**接口地址**: `POST /api/prediction/add-region-allocation`

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| year | Integer | 是 | 年份 |
| month | Integer | 是 | 月份 |
| weekSeq | Integer | 是 | 周序号 |
| cigCode | String | 是 | 卷烟代码 |
| cigName | String | 是 | 卷烟名称 |
| primaryRegion | String | 是 | 主投放区域(单扩展时为完整区域名，双扩展时为主扩展区域) |
| secondaryRegion | String | 否 | 子投放区域(双扩展时必填，单扩展时传空) |
| grades | List<BigDecimal> | 是 | 30个档位的投放量(D30-D1，索引0对应D30，索引29对应D1) |
| remark | String | 否 | 备注 |

**请求示例**:

```json
{
  "year": 2025,
  "month": 9,
  "weekSeq": 3,
  "cigCode": "001",
  "cigName": "红塔山",
  "primaryRegion": "丹江",
  "secondaryRegion": "城网",
  "grades": [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0],
  "remark": "手动新增"
}
```

**响应**:

```json
{
  "success": true,
  "message": "新增投放区域分配记录成功",
  "errorCode": null,
  "data": null,
  "timestamp": 1703308800000
}
```

**业务说明**:

1. **档位数组**: grades 必须包含30个元素，索引0对应D30(最高档)，索引29对应D1(最低档)
2. **区域格式**: 
   - 单扩展: primaryRegion = "丹江"，secondaryRegion = null
   - 双扩展: primaryRegion = "丹江"，secondaryRegion = "城网"
3. **编码生成**: 系统会自动生成编码表达式(deployinfo_code)并计算实际投放量

---

### 5.5 删除指定卷烟的特定区域分配记录

**功能**: 删除指定卷烟在特定区域的分配记录。

**接口地址**: `DELETE /api/prediction/delete-region-allocation`

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| year | Integer | 是 | 年份 |
| month | Integer | 是 | 月份 |
| weekSeq | Integer | 是 | 周序号 |
| cigCode | String | 是 | 卷烟代码 |
| cigName | String | 是 | 卷烟名称 |
| primaryRegion | String | 是 | 主投放区域 |
| secondaryRegion | String | 否 | 子投放区域 |

**请求示例**:

```json
{
  "year": 2025,
  "month": 9,
  "weekSeq": 3,
  "cigCode": "001",
  "cigName": "红塔山",
  "primaryRegion": "丹江",
  "secondaryRegion": "城网"
}
```

**响应**:

```json
{
  "success": true,
  "message": "删除区域分配记录成功",
  "errorCode": null,
  "data": null,
  "timestamp": 1703308800000
}
```

---

### 5.6 删除指定卷烟的所有区域分配记录

**功能**: 删除指定卷烟在该分区内的所有区域分配记录。

**接口地址**: `DELETE /api/prediction/delete-cigarette`

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| year | Integer | 是 | 年份 |
| month | Integer | 是 | 月份 |
| weekSeq | Integer | 是 | 周序号 |
| cigCode | String | 是 | 卷烟代码 |
| cigName | String | 是 | 卷烟名称 |

**请求示例**:

```json
{
  "year": 2025,
  "month": 9,
  "weekSeq": 3,
  "cigCode": "001",
  "cigName": "红塔山"
}
```

**响应**:

```json
{
  "success": true,
  "message": "删除卷烟分配记录成功",
  "errorCode": null,
  "data": null,
  "timestamp": 1703308800000
}
```

---

### 5.7 修改指定卷烟特定区域的档位值

**功能**: 修改指定卷烟在特定区域的30个档位投放量。

**接口地址**: `PUT /api/prediction/update-region-grades`

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| year | Integer | 是 | 年份 |
| month | Integer | 是 | 月份 |
| weekSeq | Integer | 是 | 周序号 |
| cigCode | String | 是 | 卷烟代码 |
| cigName | String | 是 | 卷烟名称 |
| primaryRegion | String | 是 | 主投放区域 |
| secondaryRegion | String | 否 | 子投放区域 |
| grades | List<BigDecimal> | 是 | 30个档位的新投放量 |
| remark | String | 否 | 备注 |

**请求示例**:

```json
{
  "year": 2025,
  "month": 9,
  "weekSeq": 3,
  "cigCode": "001",
  "cigName": "红塔山",
  "primaryRegion": "丹江",
  "secondaryRegion": "城网",
  "grades": [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0],
  "remark": "手动调整档位"
}
```

**响应**:

```json
{
  "success": true,
  "message": "修改区域档位值成功",
  "errorCode": null,
  "data": null,
  "timestamp": 1703308800000
}
```

**业务说明**:

1. 系统会重新计算实际投放量和编码表达式
2. 修改后会自动在备注字段追加修改记录

---

## 六、统计查询接口

### 6.1 查询投放组合与区域映射

**功能**: 查询本次分配所包含的所有投放组合及其对应的区域全集。投放组合由投放方式、扩展类型、标签三部分组成。

**接口地址**: `GET /api/statistics/delivery-combination-regions`

**请求参数**: URL Query 参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| year | Integer | 是 | 年份 |
| month | Integer | 是 | 月份 |
| weekSeq | Integer | 是 | 周序号 |

**请求示例**:

```http
GET /api/statistics/delivery-combination-regions?year=2025&month=9&weekSeq=3
```

**响应数据结构**:

```json
{
  "success": true,
  "message": "查询成功",
  "errorCode": null,
  "data": {
    "combinations": [
      {
        "deliveryMethod": "按档位投放",
        "deliveryEtype": "区县公司+市场类型",
        "tag": null,
        "regions": ["丹江+城网", "丹江+农网", "武当山+城网", "武当山+农网"]
      },
      {
        "deliveryMethod": "按档位投放",
        "deliveryEtype": null,
        "tag": "优质数据共享客户",
        "regions": ["全市"]
      }
    ],
    "totalCombinations": 2
  },
  "timestamp": 1703308800000
}
```

**业务说明**:

1. 用于统计分析和数据校验
2. 可帮助前端生成投放组合的下拉选项

---

### 6.2 查询价位段订购量上限

**功能**: 根据按价位段自选投放的分配结果，计算每个价位段各档位的订购量上限。

**接口地址**: `GET /api/statistics/price-band-order-limits`

**请求参数**: URL Query 参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| year | Integer | 是 | 年份 |
| month | Integer | 是 | 月份 |
| weekSeq | Integer | 是 | 周序号 |

**请求示例**:

```http
GET /api/statistics/price-band-order-limits?year=2025&month=9&weekSeq=3
```

**响应数据结构**:

```json
{
  "success": true,
  "message": "查询成功",
  "errorCode": null,
  "data": {
    "priceBandLimits": [
      {
        "priceBand": 1,
        "gradeLimit": {
          "D30": 0, "D29": 0, ..., "D15": 50, "D14": 60, ..., "D1": 0
        },
        "totalLimit": 500
      },
      {
        "priceBand": 2,
        "gradeLimit": {
          "D30": 0, "D29": 0, ..., "D10": 80, ..., "D1": 0
        },
        "totalLimit": 800
      }
    ]
  },
  "timestamp": 1703308800000
}
```

**计算规则**:

订购量上限 = 价位段单档位投放量之和 / 设定阈值（向下取整）

**业务说明**:

1. 仅在执行过"按价位段自选投放"分配后才有数据
2. 用于控制客户在各价位段的订购上限

---

## 七、错误码说明

| 错误码 | 说明 | 常见原因 |
|--------|------|----------|
| VALIDATION_ERROR | 参数校验失败 | 必填参数缺失、参数格式错误、参数值超出约束范围 |
| BUSINESS_ERROR | 业务逻辑错误 | 不满足业务规则（如：区域不存在、记录已存在等） |
| GENERATION_FAILED | 生成分配计划失败 | 算法执行失败、数据异常、系统错误 |
| CALCULATION_FAILED | 计算失败 | 投放量计算失败、统计计算错误 |
| IMPORT_FAILED | 导入失败 | Excel格式错误、校验失败、数据写入失败 |
| ADJUST_FAILED | 策略调整失败 | 调整参数不合法、算法执行失败 |
| GET_REGIONS_FAILED | 获取可用区域失败 | 区域解析失败、数据构建失败 |
| DELETE_FAILED | 删除失败 | 记录不存在、删除操作失败 |
| INTERNAL_ERROR | 内部错误 | 系统异常、数据库异常、未预期的错误 |
| PARAM_INVALID | 参数非法 | 时间参数格式错误、参数值不合法 |
| QUERY_FAILED | 查询失败 | 数据库查询异常、查询条件错误 |
| BASE_CUSTOMER_FILE_EMPTY | 客户基础信息表文件为空 | 未选择文件 |
| BASE_CUSTOMER_FILE_TOO_LARGE | 客户基础信息表文件过大 | 文件大小超过10MB |
| CIGARETTE_FILE_EMPTY | 卷烟投放基础信息表文件为空 | 未选择文件 |
| CIGARETTE_FILE_TOO_LARGE | 卷烟投放基础信息表文件过大 | 文件大小超过10MB |

---

## 八、调用示例

### 8.1 完整调用流程示例

以下是一个完整的前端调用流程示例（使用 JavaScript + Fetch API）:

```javascript
// 1. 导入客户基础信息表
async function importBaseCustomerInfo(file) {
  const formData = new FormData();
  formData.append('file', file);
  
  const response = await fetch('http://localhost:8080/api/import/base-customer', {
    method: 'POST',
    body: formData
  });
  
  const result = await response.json();
  if (result.success) {
    console.log('客户基础信息导入成功:', result.data);
    return result.data;
  } else {
    throw new Error(result.message);
  }
}

// 2. 导入卷烟投放基础信息表
async function importCigaretteInfo(file, year, month, weekSeq) {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('year', year);
  formData.append('month', month);
  formData.append('weekSeq', weekSeq);
  
  const response = await fetch('http://localhost:8080/api/import/cigarette', {
    method: 'POST',
    body: formData
  });
  
  const result = await response.json();
  if (result.success) {
    console.log('卷烟投放基础信息导入成功:', result.data);
    return result.data;
  } else {
    throw new Error(result.message);
  }
}

// 3. 一键生成分配方案
async function generateDistributionPlan(year, month, weekSeq, urbanRatio = null, ruralRatio = null) {
  const requestData = {
    year,
    month,
    weekSeq
  };
  
  if (urbanRatio !== null && ruralRatio !== null) {
    requestData.urbanRatio = urbanRatio;
    requestData.ruralRatio = ruralRatio;
  }
  
  const response = await fetch('http://localhost:8080/api/calculate/generate-distribution-plan', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(requestData)
  });
  
  const result = await response.json();
  if (result.success) {
    console.log('分配方案生成成功:', result.data);
    return result.data;
  } else {
    throw new Error(result.message);
  }
}

// 4. 查询预测数据
async function queryPredictionData(year, month, weekSeq) {
  const response = await fetch(
    `http://localhost:8080/api/prediction/list-by-time?year=${year}&month=${month}&weekSeq=${weekSeq}`
  );
  
  const result = await response.json();
  if (result.success) {
    console.log('查询成功，共', result.data.total, '条记录');
    return result.data.data;
  } else {
    throw new Error(result.message);
  }
}

// 5. 调整单个卷烟的投放策略
async function adjustCigaretteStrategy(params) {
  const response = await fetch('http://localhost:8080/api/calculate/adjust-strategy', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(params)
  });
  
  const result = await response.json();
  if (result.success) {
    console.log('策略调整成功:', result.data);
    return result.data;
  } else {
    throw new Error(result.message);
  }
}

// 6. 完整流程示例
async function completeWorkflow() {
  try {
    // 步骤1: 导入客户基础信息
    const customerFile = document.getElementById('customerFile').files[0];
    await importBaseCustomerInfo(customerFile);
    
    // 步骤2: 导入卷烟投放基础信息
    const cigaretteFile = document.getElementById('cigaretteFile').files[0];
    await importCigaretteInfo(cigaretteFile, 2025, 9, 3);
    
    // 步骤3: 生成分配方案
    const planResult = await generateDistributionPlan(2025, 9, 3, 0.6, 0.4);
    console.log('生成了', planResult.processedCount, '条分配记录');
    
    // 步骤4: 查询结果
    const predictionData = await queryPredictionData(2025, 9, 3);
    console.log('查询到', predictionData.length, '条预测数据');
    
    // 步骤5: 调整某个卷烟的策略（可选）
    await adjustCigaretteStrategy({
      year: 2025,
      month: 9,
      weekSeq: 3,
      cigCode: '001',
      cigName: '红塔山',
      newDeliveryMethod: '按档位投放',
      newAdvAmount: 1000
    });
    
    console.log('完整流程执行成功!');
  } catch (error) {
    console.error('流程执行失败:', error.message);
  }
}
```

---

### 8.2 错误处理最佳实践

```javascript
// 统一错误处理函数
function handleApiError(error, context) {
  console.error(`${context} 失败:`, error);
  
  // 根据错误码进行不同的处理
  switch (error.errorCode) {
    case 'VALIDATION_ERROR':
      // 显示参数校验错误提示
      showErrorDialog('参数错误', error.message);
      break;
    case 'BUSINESS_ERROR':
      // 显示业务规则错误提示
      showWarningDialog('业务规则检查失败', error.message);
      break;
    case 'IMPORT_FAILED':
      // 显示导入失败详情
      showImportErrorDialog(error.message);
      break;
    case 'INTERNAL_ERROR':
      // 系统错误，建议联系管理员
      showErrorDialog('系统错误', '操作失败，请联系系统管理员');
      break;
    default:
      // 通用错误提示
      showErrorDialog('操作失败', error.message || '未知错误');
  }
}

// 带重试机制的API调用
async function callApiWithRetry(apiFunction, maxRetries = 3) {
  let lastError;
  
  for (let i = 0; i < maxRetries; i++) {
    try {
      return await apiFunction();
    } catch (error) {
      lastError = error;
      
      // 某些错误不需要重试
      if (error.errorCode === 'VALIDATION_ERROR' || error.errorCode === 'BUSINESS_ERROR') {
        throw error;
      }
      
      // 等待一段时间后重试
      if (i < maxRetries - 1) {
        await new Promise(resolve => setTimeout(resolve, 1000 * (i + 1)));
        console.log(`第 ${i + 1} 次重试...`);
      }
    }
  }
  
  throw lastError;
}
```

---

### 8.3 分页查询实现建议

虽然当前接口不直接支持分页，但前端可以通过以下方式实现分页:

```javascript
// 前端分页实现
function paginateData(dataList, pageSize = 20, currentPage = 1) {
  const startIndex = (currentPage - 1) * pageSize;
  const endIndex = startIndex + pageSize;
  
  return {
    data: dataList.slice(startIndex, endIndex),
    total: dataList.length,
    pageSize: pageSize,
    currentPage: currentPage,
    totalPages: Math.ceil(dataList.length / pageSize)
  };
}

// 使用示例
async function loadPredictionDataWithPagination(year, month, weekSeq, page = 1) {
  // 1. 获取全量数据（可缓存）
  const allData = await queryPredictionData(year, month, weekSeq);
  
  // 2. 前端分页
  const paginatedResult = paginateData(allData, 20, page);
  
  // 3. 渲染表格
  renderTable(paginatedResult.data);
  renderPagination(paginatedResult.totalPages, paginatedResult.currentPage);
}
```

---

## 附录

### A. 投放方式枚举说明

| 投放方式代码 | 投放方式名称 | 说明 |
|-------------|-------------|------|
| GRADE | 按档位投放 / 按档位统一投放 | 标准分配，单层区域或多区域无权重分配 |
| GRADE_EXTEND | 按档位扩展投放 | 标准分配，支持区域扩展（如区县+市场类型） |
| PRICE_SEGMENT | 按价位段自选投放 | 价位段分配，写入 prediction_price 表 |

### B. 扩展投放类型说明

| 扩展类型 | 说明 | 区域格式示例 |
|---------|------|-------------|
| 区县公司 | 单扩展，按区县分配 | "丹江"、"武当山" |
| 市场类型 | 单扩展，按市场类型分配 | "城网"、"农网" |
| 区县公司+市场类型 | 双扩展，按区县+市场类型分配 | "丹江+城网"、"武当山+农网" |
| 档位+市场类型 | 按档位分配并按市场类型拆分 | 需提供 urbanRatio 和 ruralRatio |

### C. 标签过滤配置格式

标签过滤配置 `TAG_FILTER_CONFIG` 为 JSON 字符串格式:

```json
{
  "优质数据共享客户": "是",
  "诚信互助小组": "G001"
}
```

- 键: 标签名称
- 值: 标签过滤值

### D. 档位说明

- **档位范围**: D1 ~ D30
- **档位顺序**: D30 为最高档，D1 为最低档
- **数组索引**: grades 数组中，索引0对应D30，索引29对应D1

### E. 时间分区说明

- **年份**: 2020-2099
- **月份**: 1-12
- **周序号**: 1-5 (每月最多5周)

---

## 联系与支持

如有疑问或需要技术支持，请联系:

- **开发负责人**: Robin
- **更新日期**: 2025-12-23

---

**文档版本**: v2.0  
**最后更新**: 2025-12-23

