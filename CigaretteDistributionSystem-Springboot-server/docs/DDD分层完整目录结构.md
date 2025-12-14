# DDD分层完整目录结构

## 📋 项目概述

**项目名称**: CigaretteDistributionSystem-Springboot-server  
**架构模式**: DDD（领域驱动设计）分层架构  
**Java版本**: Java 8  
**框架**: Spring Boot 2.7.18

---

## 🏗️ 整体架构

```
org.example/
├── api/                    # 接口层（用户接口层）
├── application/            # 应用层（应用服务层）
├── domain/                 # 领域层（核心业务层）
├── infrastructure/         # 基础设施层（技术实现层）
└── shared/                 # 共享层（公共组件层）
```

### 依赖关系

```
api → application → domain ← infrastructure
      ↓           ↑
    shared ←──────┘
```

**依赖规则**：
- ✅ `api` 只能依赖 `application`
- ✅ `application` 可以依赖 `domain`、`shared`
- ✅ `domain` 只能依赖 `shared`，不能依赖其他层
- ✅ `infrastructure` 可以依赖 `domain`（实现接口）
- ✅ `shared` 不依赖任何层

---

## 📁 完整目录结构

### 1. API层（接口层）

**路径**: `org.example.api`  
**职责**: 处理HTTP请求，参数校验，响应格式化，VO转换

```
api/
└── web/
    ├── controller/                    # REST控制器
    │   ├── DistributionCalculateController.java      # 分配计算控制器
    │   ├── ExcelImportController.java                # Excel导入控制器
    │   └── PredictionQueryController.java            # 预测查询控制器
    │
    ├── converter/                     # VO-DTO转换器（MapStruct）
    │   ├── DistributionCalculateConverter.java      # 分配计算转换器
    │   ├── ExcelImportConverter.java                  # Excel导入转换器
    │   └── PredictionQueryConverter.java             # 预测查询转换器
    │
    └── vo/                            # View Object（视图对象）
        ├── request/                   # 请求VO
        │   ├── DataImportRequestVo.java               # 数据导入请求VO
        │   ├── GenerateDistributionPlanRequestVo.java # 生成分配方案请求VO
        │   └── PredictionQueryRequestVo.java         # 预测查询请求VO
        │
        └── response/                  # 响应VO
            ├── ApiResponseVo.java                     # 统一API响应VO
            ├── DataImportResponseVo.java              # 数据导入响应VO
            ├── GenerateDistributionPlanResponseVo.java # 生成分配方案响应VO
            ├── PredictionQueryResponseVo.java         # 预测查询响应VO
            └── TotalActualDeliveryResponseVo.java     # 总实际投放量响应VO
```

**文件统计**: 13个文件
- Controller: 3个
- Converter: 3个
- Request VO: 3个
- Response VO: 5个

---

### 2. Application层（应用层）

**路径**: `org.example.application`  
**职责**: 用例编排、事务管理、DTO转换、业务流程协调

```
application/
├── service/                           # 应用服务
│   ├── impl/                         # 服务实现
│   │   ├── BiWeeklyVisitBoostServiceImpl.java        # 两周一访上浮服务实现
│   │   ├── DistributionCalculateServiceImpl.java     # 分配计算服务实现
│   │   ├── DistributionWriteBackService.java         # 分配写回服务
│   │   ├── EncodeServiceImpl.java                    # 编码服务实现
│   │   ├── ExcelImportServiceImpl.java                # Excel导入服务实现
│   │   ├── RegionCustomerStatisticsBuildServiceImpl.java # 区域客户统计构建服务实现
│   │   └── TagExtractionServiceImpl.java             # 标签提取服务实现
│   │
│   ├── query/                        # 查询服务
│   │   ├── PartitionPredictionQueryService.java      # 分区预测查询服务接口
│   │   ├── PartitionPredictionQueryServiceImpl.java  # 分区预测查询服务实现
│   │   ├── PredictionQueryService.java                # 预测查询服务接口
│   │   └── PredictionQueryServiceImpl.java           # 预测查询服务实现
│   │
│   ├── BiWeeklyVisitBoostService.java                # 两周一访上浮服务接口
│   ├── DistributionCalculateService.java             # 分配计算服务接口
│   ├── EncodeService.java                            # 编码服务接口
│   ├── ExcelImportService.java                       # Excel导入服务接口
│   ├── RegionCustomerStatisticsBuildService.java     # 区域客户统计构建服务接口
│   └── TagExtractionService.java                     # 标签提取服务接口
│
├── orchestrator/                     # 编排器（用例编排、策略选择）
│   ├── provider/                     # 比例提供者
│   │   ├── impl/                     # 比例提供者实现
│   │   │   ├── IntegrityGroupRatioProvider.java       # 诚信自律小组比例提供者
│   │   │   └── MarketTypeRatioProvider.java          # 市场类型比例提供者
│   │   │
│   │   └── GroupRatioProvider.java                   # 比例提供者接口
│   │
│   ├── AllocationCalculationResult.java              # 分配计算结果
│   ├── DistributionAlgorithmEngine.java              # 分配算法引擎
│   ├── DistributionAllocationOrchestrator.java       # 分配算法编排器
│   ├── RegionCustomerMatrix.java                     # 区域客户矩阵
│   ├── StrategyContext.java                          # 策略上下文
│   ├── StrategyContextBuilder.java                   # 策略上下文构建器
│   ├── StrategyExecutionRequest.java                 # 策略执行请求
│   ├── StrategyExecutionResult.java                  # 策略执行结果
│   └── StrategyOrchestrator.java                     # 策略编排器
│
├── facade/                           # 门面（统一入口）
│   └── DistributionStrategyManager.java              # 分配策略管理器
│
├── converter/                        # 数据转换器
│   └── DistributionDataConverter.java                # 分配数据转换器
│
└── dto/                              # 应用层DTO（数据传输对象）
    ├── BaseCustomerInfoImportRequestDto.java         # 客户基础信息导入请求DTO
    ├── BatchUpdateFromExpressionsRequestDto.java     # 批量更新表达式请求DTO
    ├── CalRegionCustomerNumRequestDto.java           # 计算区域客户数请求DTO
    ├── CigaretteImportRequestDto.java                # 卷烟导入请求DTO
    ├── DataImportRequestDto.java                      # 数据导入请求DTO
    ├── DeleteAreasRequestDto.java                    # 删除区域请求DTO
    ├── DeleteDeliveryAreasResponseDto.java            # 删除投放区域响应DTO
    ├── GenerateDistributionPlanRequestDto.java        # 生成分配方案请求DTO
    ├── GenerateDistributionPlanResponseDto.java       # 生成分配方案响应DTO
    ├── QueryCigaretteDistributionRecordDto.java       # 查询卷烟投放记录DTO
    ├── QueryCigaretteDistributionResponseDto.java     # 查询卷烟投放响应DTO
    ├── QueryRequestDto.java                          # 查询请求DTO
    ├── RegionCustomerStatisticsRequestDto.java       # 区域客户统计请求DTO
    ├── RegionCustomerStatisticsResponseDto.java       # 区域客户统计响应DTO
    ├── TotalActualDeliveryResponseDto.java            # 总实际投放量响应DTO
    ├── UpdateCigaretteRequestDto.java                 # 更新卷烟请求DTO
    └── UpdatePredictionGradesRequestDto.java          # 更新预测档位请求DTO
```

**文件统计**: 约40个文件
- Service接口: 6个
- Service实现: 7个
- Query服务: 4个
- Orchestrator: 9个
- Facade: 1个
- Converter: 1个
- DTO: 17个

---

### 3. Domain层（领域层）

**路径**: `org.example.domain`  
**职责**: 核心业务逻辑、领域模型、领域服务、仓储接口

```
domain/
├── model/                             # 领域模型
│   ├── entity/                        # 实体（当前为空，使用PO作为实体）
│   │
│   ├── valueobject/                   # 值对象
│   │   ├── DeliveryCombination.java                  # 投放组合值对象
│   │   ├── DeliveryExtensionType.java                # 扩展投放类型值对象
│   │   └── DeliveryMethodType.java                    # 投放方法类型值对象
│   │
│   └── tag/                           # 标签模型
│       └── TagFilterRule.java                         # 标签过滤规则
│
├── repository/                        # 仓储接口（领域层定义接口）
│   ├── BaseCustomerInfoRepository.java                # 客户基础信息仓储接口
│   ├── CigaretteDistributionInfoRepository.java       # 卷烟投放信息仓储接口
│   ├── CigaretteDistributionPredictionPriceRepository.java # 预测价格仓储接口
│   ├── CigaretteDistributionPredictionRepository.java # 预测数据仓储接口
│   ├── IntegrityGroupMappingRepository.java           # 诚信自律小组映射仓储接口
│   ├── RegionCustomerStatisticsRepository.java        # 区域客户统计仓储接口
│   └── TemporaryCustomerTableRepository.java          # 临时客户表仓储接口
│
└── service/                           # 领域服务
    ├── algorithm/                     # 算法服务
    │   ├── impl/                      # 算法服务实现
    │   │   ├── ColumnWiseAdjustmentServiceImpl.java    # 列式调整服务实现
    │   │   ├── GroupSplittingDistributionServiceImpl.java # 分组拆分分配服务实现
    │   │   └── SingleLevelDistributionServiceImpl.java # 单层分配服务实现
    │   │
    │   ├── ColumnWiseAdjustmentService.java            # 列式调整服务接口
    │   ├── GroupSplittingDistributionService.java      # 分组拆分分配服务接口
    │   └── SingleLevelDistributionService.java        # 单层分配服务接口
    │
    ├── delivery/                      # 投放服务
    │   └── DeliveryCombinationParser.java             # 投放组合解析器
    │
    └── rule/                          # 规则服务
        ├── impl/                      # 规则服务实现
        │   ├── BiWeeklyVisitBoostRuleImpl.java        # 两周一访上浮规则实现
        │   ├── EncodingRuleImpl.java                   # 编码规则实现
        │   └── TagFilterRuleImpl.java                  # 标签过滤规则实现
        │
        ├── BiWeeklyVisitBoostRule.java                 # 两周一访上浮规则接口
        ├── EncodingRule.java                           # 编码规则接口
        └── TagFilterRule.java                          # 标签过滤规则接口
```

**文件统计**: 约25个文件
- Model: 4个
- Repository接口: 7个
- Service接口: 6个
- Service实现: 8个

---

### 4. Infrastructure层（基础设施层）

**路径**: `org.example.infrastructure`  
**职责**: 技术实现、持久化、外部服务、算法实现

```
infrastructure/
├── algorithm/                         # 算法实现
│   ├── impl/                          # 算法实现类
│   │   ├── DefaultColumnWiseAdjustmentAlgorithm.java  # 默认列式调整算法
│   │   ├── DefaultGroupSplittingDistributionAlgorithm.java # 默认分组拆分分配算法
│   │   └── DefaultSingleLevelDistributionAlgorithm.java # 默认单层分配算法
│   │
│   ├── ColumnWiseAdjustmentAlgorithm.java             # 列式调整算法接口
│   ├── GroupSplittingDistributionAlgorithm.java      # 分组拆分分配算法接口
│   └── SingleLevelDistributionAlgorithm.java         # 单层分配算法接口
│
├── config/                            # 配置
│   └── encoding/                      # 编码配置
│       ├── EncodingRuleProperties.java                # 编码规则属性
│       └── EncodingRuleRepository.java                # 编码规则仓储
│
├── monitor/                           # 监控
│   └── TransactionMonitor.java                        # 事务监控器
│
├── persistence/                       # 持久化
│   ├── mapper/                        # MyBatis Mapper接口
│   │   ├── BaseCustomerInfoMapper.java                # 客户基础信息Mapper
│   │   ├── CigaretteDistributionInfoMapper.java       # 卷烟投放信息Mapper
│   │   ├── CigaretteDistributionPredictionMapper.java # 预测数据Mapper
│   │   ├── CigaretteDistributionPredictionPriceMapper.java # 预测价格Mapper
│   │   ├── IntegrityGroupMappingMapper.java           # 诚信自律小组映射Mapper
│   │   ├── RegionCustomerStatisticsMapper.java        # 区域客户统计Mapper
│   │   └── TemporaryCustomerTableMapper.java          # 临时客户表Mapper
│   │
│   └── po/                            # Persistence Object（持久化对象）
│       ├── BaseCustomerInfoPO.java                     # 客户基础信息PO
│       ├── CigaretteDistributionInfoPO.java            # 卷烟投放信息PO
│       ├── CigaretteDistributionPredictionPO.java      # 预测数据PO
│       └── IntegrityGroupMappingPO.java                # 诚信自律小组映射PO
│
└── repository/                        # 仓储实现（实现领域层接口）
    └── impl/                          # 仓储实现类
        ├── BaseCustomerInfoRepositoryImpl.java         # 客户基础信息仓储实现
        ├── CigaretteDistributionInfoRepositoryImpl.java # 卷烟投放信息仓储实现
        ├── CigaretteDistributionPredictionPriceRepositoryImpl.java # 预测价格仓储实现
        ├── CigaretteDistributionPredictionRepositoryImpl.java # 预测数据仓储实现
        ├── IntegrityGroupMappingRepositoryImpl.java    # 诚信自律小组映射仓储实现
        ├── RegionCustomerStatisticsRepositoryImpl.java # 区域客户统计仓储实现
        └── TemporaryCustomerTableRepositoryImpl.java   # 临时客户表仓储实现
```

**文件统计**: 约28个文件
- Algorithm接口: 3个
- Algorithm实现: 3个
- Config: 2个
- Monitor: 1个
- Mapper: 7个
- PO: 4个
- Repository实现: 7个

---

### 5. Shared层（共享层）

**路径**: `org.example.shared`  
**职责**: 公共工具、常量、异常处理、辅助类

```
shared/
├── constants/                         # 常量
│   ├── BusinessConstants.java                         # 业务常量
│   ├── GradeConstants.java                            # 档位常量
│   └── TableConstants.java                            # 表常量
│
├── dto/                               # 共享DTO
│   └── RegionCustomerRecord.java                      # 区域客户记录DTO
│
├── exception/                         # 异常处理
│   └── GlobalExceptionHandler.java                    # 全局异常处理器
│
├── helper/                            # 辅助类
│   ├── BaseCustomerTableManager.java                  # 客户基础信息表管理器
│   ├── CigaretteInfoWriter.java                       # 卷烟信息写入器
│   ├── ExcelParseHelper.java                          # Excel解析辅助类
│   ├── ImportValidationHelper.java                    # 导入验证辅助类
│   └── IntegrityGroupMappingService.java              # 诚信自律小组映射服务
│
└── util/                              # 工具类
    ├── ApiResponses.java                              # API响应工具类
    ├── CombinationStrategyAnalyzer.java               # 组合策略分析器
    ├── GradeParser.java                               # 档位解析器
    ├── KmpMatcher.java                                # KMP匹配器
    ├── MapValueExtractor.java                         # Map值提取器
    ├── OrderCycleMatrixCalculator.java                # 订货周期矩阵计算器
    ├── PartitionTableManager.java                     # 分区表管理器
    ├── RegionNameBuilder.java                         # 区域名称构建器
    ├── RegionRecordBuilder.java                       # 区域记录构建器
    ├── RequestValidators.java                         # 请求验证器
    └── UploadValidators.java                          # 上传验证器
```

**文件统计**: 约20个文件
- Constants: 3个
- DTO: 1个
- Exception: 1个
- Helper: 5个
- Util: 10个

---

## 📊 统计汇总

| 层级 | 文件数量 | 主要职责 |
|------|---------|---------|
| **API层** | 13 | HTTP请求处理、VO转换 |
| **Application层** | ~40 | 用例编排、事务管理、DTO转换 |
| **Domain层** | ~25 | 核心业务逻辑、领域模型 |
| **Infrastructure层** | ~28 | 技术实现、持久化 |
| **Shared层** | ~20 | 公共工具、常量、异常处理 |
| **总计** | **~126** | - |

---

## 🔗 关键设计模式

### 1. 分层架构模式
- 清晰的层次划分和依赖关系
- 每层职责单一，边界明确

### 2. 仓储模式（Repository Pattern）
- Domain层定义接口
- Infrastructure层实现接口
- 实现领域层与持久化层的解耦

### 3. 策略模式（Strategy Pattern）
- `StrategyOrchestrator` 统一编排策略
- 多种分配算法可插拔

### 4. 编排器模式（Orchestrator Pattern）
- `DistributionAllocationOrchestrator` 编排分配流程
- `StrategyOrchestrator` 编排策略执行

### 5. 门面模式（Facade Pattern）
- `DistributionStrategyManager` 提供统一入口
- 简化客户端调用

### 6. 转换器模式（Converter Pattern）
- MapStruct实现VO-DTO转换
- 减少手动转换代码

---

## 📝 命名规范

### 包命名
- `api.web.controller` - 控制器
- `api.web.converter` - 转换器
- `api.web.vo` - 视图对象
- `application.service` - 应用服务
- `application.orchestrator` - 编排器
- `application.facade` - 门面
- `application.dto` - 数据传输对象
- `domain.model` - 领域模型
- `domain.repository` - 仓储接口
- `domain.service` - 领域服务
- `infrastructure.persistence` - 持久化
- `infrastructure.repository` - 仓储实现
- `shared.util` - 工具类
- `shared.helper` - 辅助类

### 类命名
- Controller: `*Controller`
- Service接口: `*Service`
- Service实现: `*ServiceImpl`
- Repository接口: `*Repository`
- Repository实现: `*RepositoryImpl`
- DTO: `*Dto`
- VO: `*Vo`
- PO: `*PO`
- Converter: `*Converter`
- Orchestrator: `*Orchestrator`

---

## 🎯 架构优势

1. **清晰的职责划分**: 每层职责明确，易于理解和维护
2. **高内聚低耦合**: 通过接口和DTO实现层间解耦
3. **易于测试**: 每层可独立测试
4. **易于扩展**: 新功能可在对应层添加，不影响其他层
5. **技术无关性**: Domain层不依赖具体技术实现
6. **可维护性**: 代码结构清晰，易于定位和修改

---

## 📅 文档版本

- **版本**: 1.0
- **创建日期**: 2025-12-14
- **最后更新**: 2025-12-14

