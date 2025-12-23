# Requirements Document

## Introduction

本需求文档描述了卷烟分配系统中 HG/LG（最高档位/最低档位）参数处理逻辑的重构。当前实现在 `AllocationAlgorithmSelector` 中对客户矩阵进行压缩/扩展处理，导致算法服务收到的矩阵列数不是30，引发验证失败。重构后，客户矩阵始终保持30列，HG/LG 作为参数传入算法服务，由算法内部处理计算范围。

## Glossary

- **HG (Highest Grade)**：最高档位，如 "D30"，对应索引0
- **LG (Lowest Grade)**：最低档位，如 "D1"，对应索引29
- **GradeRange**：档位范围值对象，封装 HG 和 LG
- **Customer_Matrix**：客户矩阵，维度为 [区域数][30]，存储各区域30个档位的客户数
- **Allocation_Matrix**：分配矩阵，维度为 [区域数][30]，存储各区域30个档位的分配值
- **Algorithm_Service**：分配算法领域服务，包括 SingleLevelDistributionService、ColumnWiseAdjustmentService、GroupSplittingDistributionService
- **AllocationAlgorithmSelector**：分配算法选择器，负责选择并调用具体算法服务

## Requirements

### Requirement 1: GradeRange 值对象

**User Story:** As a 开发者, I want to 使用 GradeRange 值对象封装 HG/LG 参数, so that 代码语义更清晰且便于扩展。

#### Acceptance Criteria

1. THE GradeRange SHALL 包含 maxGrade（HG）和 minGrade（LG）两个字符串字段
2. THE GradeRange SHALL 提供静态工厂方法 `full()` 返回默认范围（D30-D1）
3. THE GradeRange SHALL 提供静态工厂方法 `of(String maxGrade, String minGrade)` 创建指定范围
4. THE GradeRange SHALL 提供 `getMaxIndex()` 方法将 maxGrade 转换为索引（D30=0）
5. THE GradeRange SHALL 提供 `getMinIndex()` 方法将 minGrade 转换为索引（D1=29）
6. THE GradeRange SHALL 提供 `contains(int gradeIndex)` 方法判断索引是否在范围内
7. WHEN maxGrade 为 null 或空 THEN GradeRange SHALL 默认使用 "D30"
8. WHEN minGrade 为 null 或空 THEN GradeRange SHALL 默认使用 "D1"

### Requirement 2: 算法接口签名变更

**User Story:** As a 开发者, I want to 在算法接口中新增 GradeRange 参数, so that 算法可以根据档位范围进行计算。

#### Acceptance Criteria

1. THE SingleLevelDistributionService.distribute() SHALL 新增 GradeRange 参数
2. THE ColumnWiseAdjustmentService.distribute() SHALL 新增 GradeRange 参数
3. THE GroupSplittingDistributionService.distribute() SHALL 新增 GradeRange 参数
4. WHEN GradeRange 参数为 null THEN Algorithm_Service SHALL 使用默认范围（D30-D1）

### Requirement 3: 算法内部处理 HG/LG 范围

**User Story:** As a 开发者, I want to 算法内部根据 GradeRange 确定计算范围, so that 只在指定档位范围内进行分配计算。

#### Acceptance Criteria

1. THE Algorithm_Service SHALL 接收完整的30列客户矩阵
2. THE Algorithm_Service SHALL 只在 HG 到 LG 范围内的档位列进行分配计算
3. THE Algorithm_Service SHALL 将 HG 之前（索引 < maxIndex）的档位列分配值置为0
4. THE Algorithm_Service SHALL 将 LG 之后（索引 > minIndex）的档位列分配值置为0
5. THE Algorithm_Service SHALL 返回完整的30列分配矩阵

### Requirement 4: AllocationAlgorithmSelector 调整

**User Story:** As a 开发者, I want to AllocationAlgorithmSelector 不再压缩/扩展矩阵, so that 职责更清晰且避免矩阵列数不一致问题。

#### Acceptance Criteria

1. THE AllocationAlgorithmSelector SHALL 将完整30列客户矩阵传递给算法服务
2. THE AllocationAlgorithmSelector SHALL 将 GradeRange 参数传递给算法服务
3. THE AllocationAlgorithmSelector SHALL NOT 调用 compactToRange 方法压缩客户矩阵
4. THE AllocationAlgorithmSelector SHALL NOT 调用 expandFromRange 方法扩展结果矩阵
5. THE AllocationAlgorithmSelector SHALL 直接使用算法返回的30列结果矩阵

### Requirement 5: 删除废弃方法

**User Story:** As a 开发者, I want to 删除不再使用的压缩/扩展方法, so that 代码库保持整洁。

#### Acceptance Criteria

1. THE AllocationMatrixUtils SHALL NOT 包含 compactToRange 方法
2. THE AllocationMatrixUtils SHALL NOT 包含 expandFromRange 方法
3. WHEN 删除方法后 THEN 所有编译错误 SHALL 被修复

### Requirement 6: 零行验证调整

**User Story:** As a 开发者, I want to 零行验证基于 HG/LG 范围进行, so that 只检查参与计算的档位列。

#### Acceptance Criteria

1. THE AllocationAlgorithmSelector SHALL 在调用算法前验证客户矩阵
2. WHEN 某区域在 HG 到 LG 范围内的所有档位客户数全为0 THEN AllocationAlgorithmSelector SHALL 返回失败
3. THE 验证逻辑 SHALL 忽略 HG 之前和 LG 之后的档位列

### Requirement 7: 测试验证

**User Story:** As a 开发者, I want to 现有测试通过, so that 重构不引入回归问题。

#### Acceptance Criteria

1. WHEN 执行 Week3FullPipelineTest THEN 测试 SHALL 通过
2. WHEN 卷烟设置了非默认 HG/LG THEN 分配 SHALL 成功完成
3. THE 之前因"客户矩阵列数必须为30"失败的卷烟 SHALL 分配成功
