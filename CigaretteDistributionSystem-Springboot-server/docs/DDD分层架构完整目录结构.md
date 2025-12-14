# DDD 分层架构完整目录结构

## 📁 目标架构（完整版）

```
org.example
├── api/                               # 接口层（用户接口，使用api替代interface因为interface是Java关键字）
│   └── web/                           # Web接口
│       ├── controller/                # REST控制器
│       │   ├── DistributionCalculateController.java
│       │   ├── ExcelImportController.java
│       │   ├── PredictionQueryController.java
│       │   └── PredictionMutationController.java
│       ├── vo/                        # 视图对象（View Object）
│       │   ├── request/               # 请求VO
│       │   │   ├── GenerateDistributionPlanRequestVo.java
│       │   │   └── DataImportRequestVo.java
│       │   └── response/              # 响应VO
│       │       ├── GenerateDistributionPlanResponseVo.java
│       │       └── DataImportResponseVo.java
│       └── dto/                       # 数据传输对象（DTO，用于接口层）
│           └── ...                   # 接口层专用的DTO
│
├── application/                       # 应用层（用例编排）
│   ├── service/                       # 应用服务
│   │   ├── DistributionCalculateService.java
│   │   ├── DistributionCalculateServiceImpl.java
│   │   ├── ExcelImportService.java
│   │   ├── ExcelImportServiceImpl.java
│   │   ├── RegionCustomerStatisticsBuildService.java
│   │   ├── RegionCustomerStatisticsBuildServiceImpl.java
│   │   ├── BiWeeklyVisitBoostService.java
│   │   ├── BiWeeklyVisitBoostServiceImpl.java
│   │   ├── TagExtractionService.java
│   │   ├── TagExtractionServiceImpl.java
│   │   ├── EncodeService.java
│   │   ├── EncodeServiceImpl.java
│   │   ├── DistributionWriteBackService.java
│   │   ├── PartitionPredictionQueryService.java
│   │   ├── PartitionPredictionQueryServiceImpl.java
│   │   ├── PredictionQueryService.java
│   │   └── PredictionQueryServiceImpl.java
│   ├── orchestrator/                  # 编排器（用例编排、策略选择）
│   │   ├── DistributionAllocationOrchestrator.java
│   │   ├── AllocationCalculationResult.java
│   │   ├── StrategyOrchestrator.java              # 策略编排器（原strategy层）
│   │   ├── DistributionAlgorithmEngine.java      # 算法选择引擎（原strategy层）
│   │   ├── StrategyContext.java                  # 策略上下文（原strategy层）
│   │   ├── StrategyContextBuilder.java           # 策略上下文构建器（原strategy层）
│   │   ├── StrategyExecutionRequest.java         # 策略执行请求（原strategy层）
│   │   ├── StrategyExecutionResult.java           # 策略执行结果（原strategy层）
│   │   ├── RegionCustomerMatrix.java             # 区域客户矩阵（原strategy层）
│   │   └── provider/                              # 比例提供者（原strategy层）
│   │       ├── GroupRatioProvider.java
│   │       └── impl/
│   │           ├── IntegrityGroupRatioProvider.java
│   │           └── MarketTypeRatioProvider.java
│   ├── converter/                     # 转换器（可选）
│   │   └── DistributionDataConverter.java
│   ├── dto/                          # 应用层DTO
│   │   ├── GenerateDistributionPlanRequestDto.java
│   │   ├── GenerateDistributionPlanResponseDto.java
│   │   └── DataImportRequestDto.java
│   └── facade/                      # 门面（可选，统一入口）
│       └── DistributionStrategyManager.java      # 分配策略管理器（原strategy层）
│
├── domain/                            # 领域层（核心业务逻辑）
│   ├── model/                         # 领域模型
│   │   ├── entity/                    # 实体（聚合根）
│   │   │   ├── Cigarette.java
│   │   │   ├── Region.java
│   │   │   └── Customer.java
│   │   ├── valueobject/               # 值对象
│   │   │   ├── DeliveryMethod.java
│   │   │   ├── DeliveryCombination.java
│   │   │   └── RegionCustomerMatrix.java
│   │   └── tag/                       # 标签相关模型
│   │       └── TagFilterRule.java
│   ├── service/                       # 领域服务（业务规则/算法）
│   │   ├── algorithm/                 # 算法服务
│   │   │   ├── SingleLevelDistributionService.java
│   │   │   ├── ColumnWiseAdjustmentService.java
│   │   │   ├── GroupSplittingDistributionService.java
│   │   │   └── impl/                  # 算法实现
│   │   │       ├── SingleLevelDistributionServiceImpl.java
│   │   │       ├── ColumnWiseAdjustmentServiceImpl.java
│   │   │       └── GroupSplittingDistributionServiceImpl.java
│   │   └── rule/                      # 规则服务（预留）
│   │       ├── BiWeeklyVisitBoostRule.java      # 双周访销上浮规则（待提炼）
│   │       ├── TagFilterRule.java               # 标签过滤规则（待提炼）
│   │       └── EncodingRule.java                 # 编码规则（待提炼）
│   └── repository/                    # 仓储接口（面向领域）
│       ├── CigaretteDistributionInfoRepository.java
│       ├── CigaretteDistributionPredictionRepository.java
│       ├── CigaretteDistributionPredictionPriceRepository.java
│       ├── RegionCustomerStatisticsRepository.java
│       ├── BaseCustomerInfoRepository.java
│       ├── IntegrityGroupMappingRepository.java
│       └── TemporaryCustomerTableRepository.java
│
├── infrastructure/                    # 基础设施层（技术实现）
│   ├── persistence/                    # 持久化
│   │   ├── po/                        # 持久化对象（PO）
│   │   │   ├── BaseCustomerInfoPO.java
│   │   │   ├── CigaretteDistributionInfoPO.java
│   │   │   ├── CigaretteDistributionPredictionPO.java
│   │   │   ├── CigaretteDistributionPredictionPricePO.java
│   │   │   ├── RegionCustomerStatisticsPO.java
│   │   │   └── IntegrityGroupMappingPO.java
│   │   └── mapper/                    # MyBatis Mapper
│   │       ├── BaseCustomerInfoMapper.java
│   │       ├── BaseCustomerInfoMapper.xml
│   │       ├── CigaretteDistributionInfoMapper.java
│   │       ├── CigaretteDistributionInfoMapper.xml
│   │       ├── CigaretteDistributionPredictionMapper.java
│   │       ├── CigaretteDistributionPredictionMapper.xml
│   │       ├── CigaretteDistributionPredictionPriceMapper.java
│   │       ├── CigaretteDistributionPredictionPriceMapper.xml
│   │       ├── RegionCustomerStatisticsMapper.java
│   │       ├── RegionCustomerStatisticsMapper.xml
│   │       ├── IntegrityGroupMappingMapper.java
│   │       ├── IntegrityGroupMappingMapper.xml
│   │       ├── TemporaryCustomerTableMapper.java
│   │       └── TemporaryCustomerTableMapper.xml
│   ├── repository/                    # 仓储实现
│   │   └── impl/
│   │       ├── CigaretteDistributionInfoRepositoryImpl.java
│   │       ├── CigaretteDistributionPredictionRepositoryImpl.java
│   │       ├── CigaretteDistributionPredictionPriceRepositoryImpl.java
│   │       ├── RegionCustomerStatisticsRepositoryImpl.java
│   │       ├── BaseCustomerInfoRepositoryImpl.java
│   │       ├── IntegrityGroupMappingRepositoryImpl.java
│   │       └── TemporaryCustomerTableRepositoryImpl.java
│   ├── algorithm/                     # 算法适配器（Spring适配器）
│   │   ├── SingleLevelDistributionAlgorithm.java
│   │   ├── ColumnWiseAdjustmentAlgorithm.java
│   │   ├── GroupSplittingDistributionAlgorithm.java
│   │   └── impl/
│   │       ├── DefaultSingleLevelDistributionAlgorithm.java
│   │       ├── DefaultColumnWiseAdjustmentAlgorithm.java
│   │       └── DefaultGroupSplittingDistributionAlgorithm.java
│   ├── config/                        # 配置
│   │   ├── MyBatisConfig.java
│   │   ├── PartitionTableConfig.java
│   │   └── ...
│   ├── client/                        # 外部客户端（可选）
│   │   └── ...
│   └── gateway/                       # 网关（可选）
│       └── ...
│
├── shared/                            # 共享层（通用组件）
│   ├── util/                          # 工具类
│   │   ├── ExcelParseHelper.java
│   │   ├── ImportValidationHelper.java
│   │   └── ...
│   ├── helper/                        # 辅助类
│   │   ├── MapValueExtractor.java
│   │   ├── GradeParser.java
│   │   ├── RegionNameBuilder.java
│   │   ├── CombinationStrategyAnalyzer.java
│   │   ├── OrderCycleMatrixCalculator.java
│   │   └── RegionRecordBuilder.java
│   ├── constants/                     # 常量
│   │   └── ...
│   ├── exception/                     # 异常
│   │   └── ...
│   └── dto/                          # 共享DTO（跨层使用）
│       └── RegionCustomerRecord.java
│
```

## 📊 当前状态 vs 目标状态

### ✅ 已完成
- ✅ `domain/repository/` - 仓储接口
- ✅ `infrastructure/repository/impl/` - 仓储实现
- ✅ `infrastructure/persistence/po/` - PO对象
- ✅ `infrastructure/persistence/mapper/` - Mapper
- ✅ `domain/service/algorithm/` - 算法领域服务
- ✅ `domain/service/rule/` - 规则领域服务（目录已创建）
- ✅ `application/service/` - 应用服务

### ⏳ 待完成
- ✅ `api/web/controller/` - 接口层整理（Controller迁移完成，使用`api`替代`interface`因为`interface`是Java关键字）
- ⏳ `domain/model/` - 领域模型（实体/值对象）
- ⏳ `shared/util/` - 工具类整理
- ⏳ `shared/helper/` - 辅助类整理
- ⏳ `shared/constants/` - 常量整理
- ⏳ `shared/exception/` - 异常整理

## 🎯 各层职责说明

### 1. **api/web** - 接口层（使用`api`替代`interface`，因为`interface`是Java关键字）
- **职责**：处理HTTP请求，参数校验，响应格式化
- **依赖**：只能依赖 `application/service`
- **禁止**：不能直接依赖 `infrastructure`、`domain/repository`

### 2. **application/service** - 应用层
- **职责**：用例编排、事务管理、DTO转换
- **依赖**：可以依赖 `domain/service`、`domain/repository`、`shared`
- **禁止**：不能直接依赖 `infrastructure/persistence/mapper`

#### 2.1 **application/orchestrator** - 编排器
- **职责**：用例编排、策略选择、算法调度
- **包含**：
  - `StrategyOrchestrator` - 策略编排器（原strategy层）
  - `DistributionAlgorithmEngine` - 算法选择引擎（原strategy层）
  - `GroupRatioProvider` - 分组比例提供者（原strategy层）
- **特点**：协调多个领域服务完成复杂用例

#### 2.2 **application/facade** - 门面（可选）
- **职责**：为复杂子系统提供统一入口
- **包含**：
  - `DistributionStrategyManager` - 分配策略管理器（原strategy层）
- **特点**：简化Controller层调用，隐藏内部复杂性

### 3. **domain/** - 领域层
- **职责**：核心业务逻辑、业务规则、领域模型
- **依赖**：只能依赖 `shared`（工具类、常量）
- **禁止**：不能依赖 `infrastructure`、`application`、`api`

#### 3.1 **domain/model** - 领域模型
- **entity**：聚合根、实体（有唯一标识）
- **valueobject**：值对象（无唯一标识，不可变）

#### 3.2 **domain/service** - 领域服务
- 不属于任何实体的业务逻辑
- 需要多个领域对象协作的操作
- 算法、业务规则

##### 3.2.1 **domain/service/algorithm** - 算法服务
- 分配算法实现（单区域、列调整、分组拆分）
- 纯业务逻辑，无Spring依赖
- 可独立测试

##### 3.2.2 **domain/service/rule** - 规则服务
- 业务规则实现（双周访销上浮、标签过滤、编码规则）
- 纯业务逻辑，无Spring依赖
- 可独立测试

#### 3.3 **domain/repository** - 仓储接口
- 定义数据访问接口（面向领域）
- 不涉及具体技术实现

### 4. **infrastructure/** - 基础设施层
- **职责**：技术实现、框架集成、外部系统适配
- **依赖**：可以依赖 `domain`（实现领域接口）
- **禁止**：不能依赖 `application`、`api`

#### 4.1 **infrastructure/persistence** - 持久化
- **po**：数据库实体（带MyBatis-Plus注解）
- **mapper**：MyBatis Mapper接口和XML

#### 4.2 **infrastructure/repository** - 仓储实现
- 实现 `domain/repository` 接口
- 调用 `infrastructure/persistence/mapper`

#### 4.3 **infrastructure/algorithm** - 算法适配器
- Spring适配器，调用 `domain/service`
- 保持向后兼容

### 5. **shared/** - 共享层
- **职责**：通用工具、常量、异常、跨层DTO
- **依赖**：不依赖其他层
- **特点**：无状态、可复用

## 🔄 依赖方向规则

```
api → application → domain ← infrastructure
      ↓           ↑
    shared ←──────┘
```

**规则**：
1. ✅ `api` 只能依赖 `application`
2. ✅ `application` 可以依赖 `domain`、`shared`
3. ✅ `domain` 只能依赖 `shared`，不能依赖其他层
4. ✅ `infrastructure` 可以依赖 `domain`（实现接口）
5. ✅ `shared` 不依赖任何层

## 📝 命名规范

### 包命名
- **接口层**：`api/web`（使用`api`替代`interface`，因为`interface`是Java关键字）
- **应用层**：`application/service`
- **领域层**：`domain/model`、`domain/service`、`domain/repository`
- **基础设施层**：`infrastructure/persistence`、`infrastructure/repository`
- **共享层**：`shared/util`、`shared/helper`

### 类命名
- **接口**：`*Service`、`*Repository`
- **实现**：`*ServiceImpl`、`*RepositoryImpl`
- **实体**：`*Entity`（领域模型）、`*PO`（持久化对象）
- **值对象**：`*ValueObject`、`*VO`（注意与View Object区分）
- **DTO**：`*Dto`、`*DTO`
- **VO**：`*Vo`、`*VO`（View Object）

## 🎯 实施优先级

### 阶段1：基础结构（已完成）✅
- Repository迁移
- PO下沉
- 领域服务提炼（算法）

### 阶段2：接口层整理（待完成）⏳
- Controller迁移到 `api/web/controller`（✅ 已完成）
- 创建VO对象

### 阶段3：领域模型（待完成）⏳
- 创建领域实体和值对象
- 替换PO在应用层的使用

### 阶段4：共享层整理（待完成）⏳
- 工具类迁移到 `shared/util`
- 辅助类迁移到 `shared/helper`
- 常量整理到 `shared/constants`
- 异常整理到 `shared/exception`

## 🔍 Strategy 层迁移说明

### 原 Strategy 层位置
```
strategy/
├── DistributionStrategyManager.java
└── orchestrator/
    ├── StrategyOrchestrator.java
    ├── DistributionAlgorithmEngine.java
    ├── GroupRatioProvider.java
    ├── StrategyContext.java
    ├── StrategyContextBuilder.java
    ├── StrategyExecutionRequest.java
    ├── StrategyExecutionResult.java
    ├── RegionCustomerMatrix.java
    └── impl/
        ├── IntegrityGroupRatioProvider.java
        └── MarketTypeRatioProvider.java
```

### 迁移目标位置

根据 DDD 分层原则，Strategy 层应迁移到以下位置：

#### 1. **application/orchestrator/** - 编排器（主要位置）
- ✅ `StrategyOrchestrator` - 策略编排器（用例编排）
- ✅ `DistributionAlgorithmEngine` - 算法选择引擎（策略选择）
- ✅ `StrategyContext` - 策略上下文（编排上下文）
- ✅ `StrategyContextBuilder` - 策略上下文构建器（构建器模式）
- ✅ `StrategyExecutionRequest` - 策略执行请求（DTO）
- ✅ `StrategyExecutionResult` - 策略执行结果（DTO）
- ✅ `RegionCustomerMatrix` - 区域客户矩阵（值对象）
- ✅ `GroupRatioProvider` - 分组比例提供者接口
- ✅ `IntegrityGroupRatioProvider` - 诚信分组比例提供者
- ✅ `MarketTypeRatioProvider` - 市场类型比例提供者

**理由**：
- 这些组件负责**用例编排**和**策略选择**，属于应用层职责
- 协调多个领域服务完成复杂业务用例
- 包含Spring依赖，需要依赖注入

#### 2. **application/facade/** - 门面（可选）
- ✅ `DistributionStrategyManager` - 分配策略管理器

**理由**：
- 作为门面模式，为复杂子系统提供统一入口
- 简化Controller层调用
- 属于应用层职责

### 迁移步骤

1. **创建目标目录**
   ```bash
   mkdir -p src/main/java/org/example/application/orchestrator/provider/impl
   mkdir -p src/main/java/org/example/application/facade
   ```

2. **移动文件**
   - `strategy/orchestrator/*` → `application/orchestrator/`
   - `strategy/orchestrator/impl/*` → `application/orchestrator/provider/impl/`
   - `strategy/DistributionStrategyManager.java` → `application/facade/`

3. **更新Package声明**
   - `package org.example.strategy.orchestrator;` → `package org.example.application.orchestrator;`
   - `package org.example.strategy;` → `package org.example.application.facade;`

4. **更新引用**
   - 更新所有import语句
   - 更新Spring配置（如有）

5. **删除原目录**
   ```bash
   rm -rf src/main/java/org/example/strategy
   ```

### 架构优势

- ✅ **职责清晰**：编排逻辑集中在应用层
- ✅ **依赖正确**：应用层可以依赖领域服务
- ✅ **易于测试**：编排器可以mock领域服务
- ✅ **符合DDD**：遵循分层架构原则

## 📚 参考文档

- `docs/DDD分层实施方案.md` - 详细实施方案
- `docs/工作清单-最新版.md` - 当前工作进度
- `docs/领域服务命名说明.md` - 命名规范说明
- `docs/domain-service目录重构总结.md` - 领域服务目录重构总结

