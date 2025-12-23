# 统一分配流程重构需求

## 背景
当前系统有两种分配类型：
- **标准分配**：按档位投放、按档位扩展投放
- **价位段分配**：按价位段自选投放

目前的问题是分流逻辑分散在多个层级，且 `PriceBandAllocationService` 内部重新查询 info 表，而不是接收已过滤的数据。

## 用户故事

### US-1: 统一入口分流
**作为** 系统调用方  
**我希望** 调用单一 API 端点 `/api/calculate/generate-distribution-plan`  
**以便** 自动处理所有投放类型，无需关心内部分流逻辑

**验收标准：**
- [ ] 单一 API 端点处理所有投放类型
- [ ] 标准分配写入 `prediction` 表
- [ ] 价位段分配写入 `prediction_price` 表

### US-2: Orchestrator 层分流
**作为** 开发者  
**我希望** 在 Orchestrator 层面完成 Standard 和 PriceBand 的分流  
**以便** 各服务只处理自己负责的数据，职责清晰

**验收标准：**
- [ ] 在 `StandardAllocationServiceImpl.executeAllocation()` 中查询 info 表一次
- [ ] 根据 `delivery_method` 分成两个集合
- [ ] Standard 集合传给标准分配流程
- [ ] PriceBand 集合传给价位段分配流程

### US-3: PriceBandAllocationService 接收数据
**作为** 开发者  
**我希望** `PriceBandAllocationService` 接收已过滤的卷烟列表  
**以便** 避免重复查询 info 表，提高效率

**验收标准：**
- [ ] 新增方法 `allocateForPriceBand(List<Map<String, Object>> candidates, Integer year, Integer month, Integer weekSeq)`
- [ ] 移除内部对 `priceBandCandidateQueryService` 的调用
- [ ] 保留原有方法作为兼容（可选）

## 技术设计

### 当前流程
```
StandardAllocationServiceImpl.executeAllocation()
    │
    ├── queryDistributionInfo() → 查询 info 表
    ├── filterNonPriceBandData() → 过滤出标准分配数据
    ├── processStandardAllocations() → 处理标准分配
    │
    └── strategyOrchestrator.executePriceBandBatch()
            │
            └── priceBandAllocationService.allocateForPriceBand()
                    │
                    └── priceBandCandidateQueryService.listOrderedPriceBandCandidates() → 再次查询
```

### 目标流程
```
StandardAllocationServiceImpl.executeAllocation()
    │
    ├── queryDistributionInfo() → 查询 info 表（一次）
    │
    ├── 分流：
    │   ├── standardList = filter(delivery_method != "按价位段自选投放")
    │   └── priceBandList = filter(delivery_method == "按价位段自选投放")
    │
    ├── processStandardAllocations(standardList) → 处理标准分配
    │
    └── priceBandAllocationService.allocateForPriceBand(priceBandList, year, month, weekSeq)
            │
            └── 直接使用传入的 priceBandList，不再查询
```

## 涉及文件
- `src/main/java/org/example/application/service/calculate/impl/StandardAllocationServiceImpl.java`
- `src/main/java/org/example/application/service/calculate/impl/PriceBandAllocationServiceImpl.java`
- `src/main/java/org/example/application/service/calculate/PriceBandAllocationService.java`
- `src/main/java/org/example/application/orchestrator/strategy/StrategyOrchestrator.java`

## 测试验证
- `mvn test -Dtest=Week3FullPipelineTest -q` (标准分配)
- `mvn test -Dtest=Week4FullPipelineTest -q` (价位段分配)
