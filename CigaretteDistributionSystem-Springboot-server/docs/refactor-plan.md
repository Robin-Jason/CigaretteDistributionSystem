# 重构计划与任务清单

目的：清理职责越权、降低耦合，提升可维护性与可测试性。

## 分层职责准则
- Controller：请求绑定/最小校验/日志与响应包装，不做业务决策、不拼 SQL。
- Service：业务流程编排、事务控制、领域规则与校验、调用编排（算法/编码/DAO）。
- DAO：仅做持久化与表/分区操作（CRUD/DDL），无业务决策；返回实体或 DTO/RowMapper 结果。
- 通用组件：编码解码、参数校验、装配/转换、Excel 解析等应独立于 Service，供复用与测试。

## 现有主要越权/混杂点（初步）
- Controller：存在重复的参数校验与 Map 响应组装，可下沉/抽公共工具。
- Service：
  - `DataManagementServiceImpl` 既做流程又处理区域解析、编码生成、实体装配，职责偏重。
  - `ExcelImportServiceImpl` 集成文件解析、结构校验、DDL（建/删表）、批量导入，耦合度高。
- DAO：多数职责单一，但部分返回 Map，Service 需要手动取字段；DDL/分区操作集中在 DAO，可保持，但避免上层拼 SQL。

## 重构目标
1) 让 Controller 只“接/回”，Service 专注“算/管”，DAO 只管“存/取”。
2) 抽取可复用的校验/装配/解析组件，减少重复代码。
3) 明确事务边界，避免业务逻辑下沉到 DAO 或漂到 Controller。
4) 提升可测试性：拆分后为校验、解析、装配添加单元测试。

## 落地步骤与任务清单
### 第 1 步：分层约定与基线
- [ ] 输出/宣贯分层准则（本文件），在 MR 审查中作为检查项。

### 第 2 步：Controller 精简与统一响应
- [ ] 抽取公共校验（必填列表、文件大小、周期参数）到共享 Validator；Controller 仅调用。
- [ ] 抽取响应包装工具，减少各 Controller 内重复 Map 组装。
- [ ] 清点并移除多余的 try/catch，如业务异常可由全局异常处理。

### 第 3 步：Service 解耦
- `DataManagementServiceImpl`
  - [ ] 提炼“区域解析与一致性校验”（deliveryArea/deliveryEtype）到独立组件。
  - [ ] 提炼“卷烟代码清洗校验”到 Helper。
  - [ ] 提炼“编码表达式生成/解析适配”到 Adapter，Service 只编排。
- `ExcelImportServiceImpl`
  - [ ] 拆出 Excel 解析与结构校验 Parser（纯解析，不含 DB）。
  - [ ] 拆出表/分区管理（DDL/清表）到 DAO/Infra 类。
  - [ ] Service 仅做事务编排与导入统计。

### 第 4 步：DAO 规范化与 DTO 化
- [ ] 将返回 Map 的 DAO 查询改为实体/DTO + RowMapper，减少魔法字符串。
- [ ] 检查 DAO 方法命名与职责，确保无业务判断。

### 第 5 步：测试与回归
- [ ] 为新增的 Validator/Parser/Assembler/Adapter 编写单元测试（边界、非法参数）。
- [ ] 关键流程回归：一键分配、批量更新、导入三条主线。

## 优先级建议
1) Controller 公共校验与响应包装（改动小、收益快）。
2) Excel 导入拆分（风险集中、易测）。
3) DataManagement 解析/编码提炼（影响面广，但收益大）。
4) DAO DTO 化（逐步替换，减少一次性大改）。

## 注意事项
- 保持行为等价：拆分时先补测试，再重构。
- 事务边界留在 Service，DAO 不开启/控制事务。
- 拆分组件时避免引入循环依赖，保持单向调用：Controller → Service → (Adapter/Validator) → DAO。

