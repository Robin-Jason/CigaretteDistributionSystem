# Service层结构分析与重构建议

## 一、当前结构问题分析

### 1.1 包结构混乱

**当前包结构：**
```
org.example.service/
├── impl/                          # 实现类（8个文件）
├── boost/                         # 辅助工具类（6个文件）
├── converter/                     # 数据转换器（1个文件）
├── dataManagement/                 # 数据管理（6个文件，但有很多空目录）
│   ├── read/                      # 查询服务（4个文件）
│   ├── update/                    # 更新服务（2个文件）
│   ├── create/                    # 空目录（.gitkeep）
│   ├── delete/                    # 空目录（.gitkeep）
│   ├── assembler/                 # 空目录（.gitkeep）
│   ├── helper/                   # 空目录（.gitkeep）
│   ├── usecase/                  # 空目录（.gitkeep）
│   └── validator/               # 空目录（.gitkeep）
├── delivery/                      # 投放相关（4个文件）
├── importer/                      # 导入相关（5个文件）
├── orchestrator/                  # 编排器（2个文件）
├── tag/                           # 标签相关（1个文件）
└── [接口文件]                     # 接口直接在根目录（7个文件）
```

### 1.2 主要问题

#### 问题1：接口与实现分离不清晰
- **现状**：接口在根目录，实现在 `impl/` 子包
- **问题**：不符合Java包命名规范，且查找不便
- **影响**：IDE导航困难，代码组织混乱

#### 问题2：工具类分散
- **现状**：`boost/`、`converter/`、`importer/` 都包含工具类
- **问题**：职责不清晰，命名不统一（`boost` 含义不明确）
- **影响**：开发者不知道应该把工具类放在哪里

#### 问题3：空目录过多
- **现状**：`dataManagement` 下有6个空目录
- **问题**：过度设计，预留了但未使用
- **影响**：增加理解成本，误导开发者

#### 问题4：职责边界不清
- **现状**：`orchestrator/` 和 `impl/` 中的服务职责有重叠
- **问题**：编排逻辑应该在哪里不明确
- **影响**：代码耦合度高，难以维护

#### 问题5：命名不一致
- **现状**：`boost`、`importer`、`converter` 命名风格不统一
- **问题**：`boost` 含义不明确，`importer` 应该是 `import`
- **影响**：代码可读性差

## 二、重构方案

### 2.1 推荐结构（按领域驱动设计）

```
org.example.service/
├── domain/                        # 领域服务（核心业务逻辑）
│   ├── allocation/                # 分配领域
│   │   ├── AllocationService.java
│   │   ├── AllocationServiceImpl.java
│   │   └── AllocationWriteBackService.java
│   ├── statistics/                # 统计领域
│   │   ├── RegionStatisticsService.java
│   │   └── RegionStatisticsServiceImpl.java
│   ├── import/                   # 导入领域
│   │   ├── ExcelImportService.java
│   │   └── ExcelImportServiceImpl.java
│   └── encoding/                 # 编码领域
│       ├── EncodeService.java
│       └── EncodeServiceImpl.java
│
├── application/                   # 应用服务（用例编排）
│   ├── DistributionCalculateService.java
│   ├── DistributionCalculateServiceImpl.java
│   └── orchestrator/             # 编排器
│       ├── DistributionAllocationOrchestrator.java
│       └── AllocationCalculationResult.java
│
├── infrastructure/                # 基础设施服务（技术实现）
│   ├── table/                    # 表管理
│   │   ├── TemporaryTableService.java
│   │   └── TemporaryTableServiceImpl.java
│   └── query/                    # 查询服务
│       ├── PartitionPredictionQueryService.java
│       └── PartitionPredictionQueryServiceImpl.java
│
├── shared/                        # 共享组件
│   ├── util/                     # 工具类（统一命名）
│   │   ├── DataConverter.java    # 合并 converter
│   │   ├── GradeParser.java      # 从 boost 移入
│   │   ├── MapValueExtractor.java
│   │   ├── RegionNameBuilder.java
│   │   └── RegionRecordBuilder.java
│   ├── helper/                   # 辅助类
│   │   ├── ExcelParseHelper.java # 从 importer 移入
│   │   ├── ImportValidationHelper.java
│   │   ├── BaseCustomerTableManager.java
│   │   └── CigaretteInfoWriter.java
│   └── model/                    # 领域模型/值对象
│       ├── delivery/             # 投放相关模型
│       │   ├── DeliveryMethodType.java
│       │   ├── DeliveryExtensionType.java
│       │   ├── DeliveryCombination.java
│       │   └── DeliveryCombinationParser.java
│       └── tag/                  # 标签相关模型
│           └── TagFilterRule.java
│
└── facade/                        # 门面服务（可选，用于简化外部调用）
    └── DistributionFacade.java
```

### 2.2 简化版结构（更实用）

如果不想采用DDD分层，可以采用更实用的结构：

```
org.example.service/
├── core/                          # 核心业务服务
│   ├── DistributionCalculateService.java
│   ├── DistributionCalculateServiceImpl.java
│   ├── RegionCustomerStatisticsBuildService.java
│   ├── RegionCustomerStatisticsBuildServiceImpl.java
│   ├── ExcelImportService.java
│   ├── ExcelImportServiceImpl.java
│   ├── EncodeService.java
│   └── EncodeServiceImpl.java
│
├── support/                       # 支撑服务
│   ├── TemporaryTableService.java
│   ├── TemporaryTableServiceImpl.java
│   ├── TagExtractionService.java
│   ├── TagExtractionServiceImpl.java
│   ├── BiWeeklyVisitBoostService.java
│   └── BiWeeklyVisitBoostServiceImpl.java
│
├── query/                         # 查询服务（合并 dataManagement/read）
│   ├── PartitionPredictionQueryService.java
│   ├── PartitionPredictionQueryServiceImpl.java
│   ├── PredictionQueryService.java
│   └── PredictionQueryServiceImpl.java
│
├── write/                         # 写回服务（合并 dataManagement/update）
│   ├── DistributionWriteBackService.java
│   ├── PredictionGradesUpdateService.java
│   └── PredictionGradesUpdateServiceImpl.java
│
├── util/                          # 工具类（统一命名）
│   ├── DataConverter.java         # 合并 converter/DistributionDataConverter
│   ├── GradeParser.java           # 从 boost 移入
│   ├── MapValueExtractor.java     # 从 boost 移入
│   ├── RegionNameBuilder.java    # 从 boost 移入
│   ├── RegionRecordBuilder.java  # 从 boost 移入
│   ├── OrderCycleMatrixCalculator.java
│   └── CombinationStrategyAnalyzer.java
│
├── helper/                        # 辅助类
│   ├── ExcelParseHelper.java      # 从 importer 移入
│   ├── ImportValidationHelper.java
│   ├── BaseCustomerTableManager.java
│   ├── CigaretteInfoWriter.java
│   └── IntegrityGroupMappingService.java
│
├── orchestrator/                  # 编排器
│   ├── DistributionAllocationOrchestrator.java
│   └── AllocationCalculationResult.java
│
└── model/                         # 领域模型/值对象
    ├── delivery/                  # 投放相关模型
    │   ├── DeliveryMethodType.java
    │   ├── DeliveryExtensionType.java
    │   ├── DeliveryCombination.java
    │   └── DeliveryCombinationParser.java
    └── tag/                       # 标签相关模型
        └── TagFilterRule.java
```

## 三、设计模式建议

### 3.1 服务层设计模式

#### 1. **Facade模式（门面模式）**
- **用途**：为复杂的子系统提供统一入口
- **应用**：`DistributionFacade` 封装分配计算、统计、导入等操作
- **优势**：简化Controller层调用，隐藏内部复杂性

```java
@Service
public class DistributionFacade {
    private final DistributionCalculateService calculateService;
    private final RegionCustomerStatisticsBuildService statisticsService;
    private final ExcelImportService importService;
    
    // 提供统一的业务入口
    public GenerateDistributionPlanResponseDto generatePlan(GenerateDistributionPlanRequestDto request) {
        // 1. 导入数据
        // 2. 构建统计
        // 3. 生成分配方案
    }
}
```

#### 2. **Strategy模式（策略模式）**
- **用途**：封装可替换的算法
- **应用**：分配算法（单区域、列调整、分组拆分）
- **优势**：算法可扩展，符合开闭原则

#### 3. **Template Method模式（模板方法模式）**
- **用途**：定义算法骨架，子类实现具体步骤
- **应用**：分配算法的粗调+微调流程
- **优势**：代码复用，流程统一

#### 4. **Builder模式（建造者模式）**
- **用途**：构建复杂对象
- **应用**：`RegionRecordBuilder`、`AllocationCalculationResult`
- **优势**：对象构建清晰，参数可选

#### 5. **Factory模式（工厂模式）**
- **用途**：创建对象，隐藏创建逻辑
- **应用**：`DistributionStrategyManager` 选择算法
- **优势**：解耦创建和使用

### 3.2 包组织原则

#### 1. **按职责分层**
- **Core**：核心业务逻辑
- **Support**：支撑服务
- **Query/Write**：数据访问（CQRS分离）
- **Util/Helper**：工具和辅助类
- **Model**：领域模型

#### 2. **按领域分组**
- 同一领域的服务放在同一包下
- 例如：`allocation/`、`statistics/`、`import/`

#### 3. **接口与实现分离**
- **方案A**：接口和实现在同一包（推荐）
  ```
  service/
    ├── DistributionCalculateService.java
    └── DistributionCalculateServiceImpl.java
  ```
- **方案B**：接口在 `api/`，实现在 `impl/`
  ```
  service/
    ├── api/
    │   └── DistributionCalculateService.java
    └── impl/
        └── DistributionCalculateServiceImpl.java
  ```

## 四、重构步骤建议

### 阶段1：清理空目录
1. 删除 `dataManagement` 下的空目录（`create/`、`delete/`、`assembler/`、`helper/`、`usecase/`、`validator/`）
2. 合并 `dataManagement/read` 和 `dataManagement/update` 到 `query/` 和 `write/`

### 阶段2：统一工具类命名
1. 将 `boost/` 重命名为 `util/`
2. 将 `converter/` 合并到 `util/`
3. 将 `importer/` 中的辅助类移到 `helper/`

### 阶段3：重组服务
1. 创建 `core/`、`support/`、`query/`、`write/` 包
2. 按职责移动服务类
3. 更新所有import语句

### 阶段4：优化接口组织
1. 将接口和实现放在同一包（推荐）
2. 或创建 `api/` 和 `impl/` 明确分离

## 五、最佳实践建议

### 5.1 命名规范
- **Service接口**：`XxxService`
- **Service实现**：`XxxServiceImpl`
- **工具类**：`XxxUtil` 或 `XxxHelper`
- **模型类**：`XxxModel` 或直接命名（如 `DeliveryMethodType`）

### 5.2 包命名规范
- **核心服务**：`core`
- **支撑服务**：`support`
- **查询服务**：`query`
- **写回服务**：`write`
- **工具类**：`util`
- **辅助类**：`helper`
- **模型**：`model`

### 5.3 依赖原则
- **Core** → **Support** → **Query/Write**
- **Util/Helper** 应该是无状态的，不依赖其他Service
- **Model** 应该是纯POJO，不依赖Service

## 六、总结

### 推荐方案：**简化版结构**
- ✅ 清晰的分层（core/support/query/write）
- ✅ 统一的命名（util/helper/model）
- ✅ 删除空目录，减少混乱
- ✅ 接口和实现在同一包，便于查找
- ✅ 符合Java包命名规范

### 重构优先级
1. **高优先级**：清理空目录、统一工具类命名
2. **中优先级**：重组服务包结构
3. **低优先级**：引入Facade模式（可选）

### 注意事项
- 重构时保持向后兼容（逐步迁移）
- 更新所有import语句
- 更新单元测试
- 更新文档

