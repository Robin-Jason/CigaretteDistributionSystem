# DDD 分层实施方案与任务清单（可执行版）

## 目标与收益
- 用例/编排与业务规则分离，控制器不再依赖 Mapper，领域不再被技术细节污染。
- 领域模型沉淀业务概念，算法/规则可单测；应用服务便于复用（REST/任务/消息）。
- 基础设施细节（MyBatis-Plus、分区表、动态表名）下沉，未来替换存储/外部依赖成本低。

## 目标分层（命名可微调）
```
org.example
├── interface/web/            # Controller, VO/Response
├── application/service/      # 用例编排、事务、DTO/Command/Query、Assembler
├── domain/
│   ├── model/                # 聚合根/实体/值对象（纯业务，不含持久化注解）
│   ├── service/              # 领域服务（算法/规则）
│   └── repository/           # 仓储接口（面向领域）
├── infrastructure/
│   ├── persistence/          # mapper/xml/po（MyBatis-Plus 实体）
│   ├── repository/           # 仓储实现（依赖 mapper）
│   └── config/client/gateway # 技术配置、外部适配
└── shared/
    ├── util/                 # 通用工具
    └── helper/               # 解析/校验等辅助
```

## 分阶段实施（每步可编译、可回滚）
### 阶段0 基线
- 建立分支；开启 lint/格式化；约束新增代码不得 Controller→Mapper 直连。

### 阶段1 目录雏形（不改依赖，仅搬迁公共类）
- 创建目标目录。
- 将 `util/helper` 移至 `shared/util`、`shared/helper`，调整 package，确保编译。
- 将 `model/delivery`、`model/tag` 移至 `domain/model`，调整引用。

### 阶段2 实体/PO 分离
- 将 MyBatis-Plus 注解实体移至 `infrastructure/persistence/po`。
- 在 `domain/model` 创建对应领域对象（初期可 1:1），应用层用转换器过渡。

### 阶段3 仓储接口/实现拆分
- 在 `domain/repository` 定义接口：如 `CigaretteDistributionRepository`、`RegionCustomerStatsRepository`、`PredictionRepository` 等。
- 在 `infrastructure/repository` 实现，复用现有 Mapper/XML；应用层改依赖仓储接口。

### 阶段4 应用服务落位
- 将现有 `core` 编排类迁至 `application/service`（如 `DistributionApplicationService`、`ImportApplicationService`、`WriteBackApplicationService`）。
- Controller 仅依赖应用服务 DTO/VO；事务边界放应用服务。

### 阶段5 领域服务提炼
- 将算法/上浮/标签规则等纯业务逻辑从应用/支撑层抽到 `domain/service`。
- 应用服务只做编排、事务、调用领域服务与仓储。

### 阶段6 接口层整理
- Controller 迁至 `interface/web`，放请求/响应 VO；移除对 Mapper/Helper 的直接依赖。

### 阶段7 共享层收敛
- 统一 `shared/util`、`shared/helper`；常量/异常放 `shared/constants|exception`。

### 阶段8 收尾与治理
- 全局搜索并移除旧包引用；删除废弃文件。
- 更新文档、依赖方向约束；补充测试（领域单测、应用用例测、关键接口集成测）。

## 任务清单（可拆 Issue）
1. 目录初始化：新增 `interface/web`, `application/service`, `domain/*`, `infrastructure/*`, `shared/*`。
2. 工具/辅助迁移：`util/helper` → `shared`，修正 package 与引用。
3. 模型迁移：`model/delivery`, `model/tag` → `domain/model`，修正引用。
4. PO 下沉：实体类 → `infrastructure/persistence/po`；Mapper/XML 依赖更新。
5. 仓储接口：在 `domain/repository` 定义主要仓储；实现放 `infrastructure/repository`；应用层改用接口。
6. 应用服务重定位：`core` 编排类 → `application/service`；Controller 改调应用服务。
7. 领域服务提炼：算法/上浮/标签规则 → `domain/service`，应用层保留编排。
8. 接口层归位：Controller → `interface/web`；VO 放同层；去掉对 Mapper 的直依赖。
9. 清理与编译：全局替换旧包，删除旧文件，保证编译通过。
10. 文档与测试：更新分层说明；补充领域服务单测、应用用例测、关键接口集成测。

## 命名/依赖约束
- 领域层：禁用 Spring/MyBatis 注解；只能依赖 `domain/*`、`shared/*`。
- 应用层：可依赖领域、仓储接口、shared；控制事务与编排。
- 接口层：只依赖应用服务与 DTO/VO，不依赖 mapper/infrastructure。
- 基础设施层：实现仓储、mapper、外部适配；不得反向依赖应用/接口。

## 优先改造的类（示例映射）
- 应用服务：`DistributionCalculateServiceImpl`、`ExcelImportServiceImpl`、`RegionCustomerStatisticsBuildServiceImpl` → `application/service/*ApplicationService`
- 领域服务：分配算法、上浮规则、标签规则相关逻辑 → `domain/service`
- 仓储接口：预测/分配/统计/基础客户相关的持久化访问 → `domain/repository`
- PO：现有 MyBatis-Plus 实体 → `infrastructure/persistence/po`

## 里程碑建议
- M1：目录雏形 + 工具/模型迁移（编译通过）
- M2：PO 下沉 + 仓储接口/实现拆分（应用层依赖接口）
- M3：应用服务/控制器重定位（跑通关键用例）
- M4：领域服务提炼 + 测试补齐（单测/用例测）
- M5：清理/文档/约束固化

## 测试策略
- 领域服务：纯单测（无 Spring/DB）。
- 应用服务：Mock 仓储接口的用例测试；关键用例做集成测试（含事务）。
- 接口层：REST 集成测试，覆盖一键分配/导入/查询主流程。
- 回归：关键算法路径、上浮规则、写回路径。

## 升级路径建议
- 先做最小侵入（目录+package+依赖替换），确保编译；再分批抽象领域服务与仓储接口。
- 控制每次改动影响面，保留旧实现到完成替换后再删除。
