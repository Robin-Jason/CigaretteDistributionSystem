# Implementation Tasks

## Overview

本文档定义了 Grade Range 重构的实施任务清单。重构目标是将 HG/LG 处理职责从 `AllocationAlgorithmSelector` 下沉到算法服务内部，确保客户矩阵始终保持30列，算法服务根据 `GradeRange` 参数确定计算范围并返回完整30列结果。

## Task Breakdown

### Task 1: 创建 GradeRange 值对象

**Priority**: High  
**Estimated Effort**: 30 minutes  
**Dependencies**: None

**Description**:  
创建 `GradeRange` 值对象，封装 HG/LG 参数，提供索引转换和范围判断功能。

**Acceptance Criteria**:
- [ ] 创建 `src/main/java/org/example/domain/model/valueobject/GradeRange.java`
- [ ] 实现 `full()` 静态工厂方法（返回 D30-D1）
- [ ] 实现 `of(String maxGrade, String minGrade)` 静态工厂方法
- [ ] 实现 `getMaxIndex()` 方法（D30=0, D29=1, ..., D1=29）
- [ ] 实现 `getMinIndex()` 方法
- [ ] 实现 `contains(int gradeIndex)` 方法
- [ ] null/空字符串默认值处理（maxGrade默认"D30"，minGrade默认"D1"）
- [ ] 添加 getter 方法：`getMaxGrade()`, `getMinGrade()`

**Implementation Notes**:
- 使用 `GradeParser.parseGradeToIndex()` 进行档位字符串到索引的转换
- 私有构造函数，只通过静态工厂方法创建
- 使用 `normalizeGrade()` 私有方法处理 null/空字符串

**Files to Create**:
- `src/main/java/org/example/domain/model/valueobject/GradeRange.java`

---

### Task 2: 更新算法接口签名

**Priority**: High  
**Estimated Effort**: 15 minutes  
**Dependencies**: Task 1

**Description**:  
在三个算法服务接口中新增 `GradeRange` 参数。

**Acceptance Criteria**:
- [ ] `SingleLevelDistributionService.distribute()` 新增 `GradeRange gradeRange` 参数
- [ ] `ColumnWiseAdjustmentService.distribute()` 新增 `GradeRange gradeRange` 参数
- [ ] `GroupSplittingDistributionService.distribute()` 新增 `GradeRange gradeRange` 参数
- [ ] 更新接口 Javadoc，说明 GradeRange 参数用途

**Implementation Notes**:
- 参数位置：放在 `targetAmount` 之后，其他参数之前
- Javadoc 说明：`@param gradeRange 档位范围，指定计算范围（HG到LG），为null时使用默认范围（D30-D1）`

**Files to Modify**:
- `src/main/java/org/example/domain/service/algorithm/SingleLevelDistributionService.java`
- `src/main/java/org/example/domain/service/algorithm/ColumnWiseAdjustmentService.java`
- `src/main/java/org/example/domain/service/algorithm/GroupSplittingDistributionService.java`

---

### Task 3: 更新 SingleLevelDistributionService 实现

**Priority**: High  
**Estimated Effort**: 45 minutes  
**Dependencies**: Task 2

**Description**:  
更新 `SingleLevelDistributionServiceImpl`，根据 `GradeRange` 只在指定范围内计算，范围外置0。

**Acceptance Criteria**:
- [ ] 更新 `distribute()` 方法签名，新增 `GradeRange gradeRange` 参数
- [ ] 处理 null 参数：`GradeRange range = gradeRange != null ? gradeRange : GradeRange.full();`
- [ ] 获取 `maxIndex` 和 `minIndex`
- [ ] 初始化30列结果矩阵（全0）
- [ ] 只在 `maxIndex ~ minIndex` 范围内进行分配计算
- [ ] 返回完整30列矩阵（范围外已经是0）
- [ ] 更新方法 Javadoc

**Implementation Notes**:
- 算法核心逻辑只操作 `result[r][maxIndex]` 到 `result[r][minIndex]`
- 范围外的列保持初始值0，无需额外处理
- 验证矩阵维度时仍然要求30列

**Files to Modify**:
- `src/main/java/org/example/domain/service/algorithm/impl/SingleLevelDistributionServiceImpl.java`

---

### Task 4: 更新 ColumnWiseAdjustmentService 实现

**Priority**: High  
**Estimated Effort**: 45 minutes  
**Dependencies**: Task 2

**Description**:  
更新 `ColumnWiseAdjustmentServiceImpl`，根据 `GradeRange` 只在指定范围内计算，范围外置0。

**Acceptance Criteria**:
- [ ] 更新 `distribute()` 方法签名，新增 `GradeRange gradeRange` 参数
- [ ] 处理 null 参数：`GradeRange range = gradeRange != null ? gradeRange : GradeRange.full();`
- [ ] 获取 `maxIndex` 和 `minIndex`
- [ ] 初始化30列结果矩阵（全0）
- [ ] 粗调阶段：只在 `maxIndex ~ minIndex` 范围内进行整列+1
- [ ] 微调阶段：只在 `maxIndex ~ minIndex` 范围内进行调整
- [ ] 返回完整30列矩阵（范围外已经是0）
- [ ] 更新零行验证：只检查范围内的列
- [ ] 更新方法 Javadoc

**Implementation Notes**:
- `coarseAdjustment()` 方法：循环范围改为 `for (int grade = maxIndex; grade <= minIndex; grade++)`
- `rollbackLastColumnIncrement()` 方法：查找范围改为 `for (int grade = minIndex; grade >= maxIndex; grade--)`
- `runColumnFillIteration()` 方法：循环范围改为 `for (int grade = maxIndex; grade <= minIndex; grade++)`
- 零行验证：检查某区域在 `maxIndex ~ minIndex` 范围内是否全为0

**Files to Modify**:
- `src/main/java/org/example/domain/service/algorithm/impl/ColumnWiseAdjustmentServiceImpl.java`

---

### Task 5: 更新 GroupSplittingDistributionService 实现

**Priority**: High  
**Estimated Effort**: 45 minutes  
**Dependencies**: Task 2

**Description**:  
更新 `GroupSplittingDistributionServiceImpl`，根据 `GradeRange` 只在指定范围内计算，范围外置0。

**Acceptance Criteria**:
- [ ] 更新 `distribute()` 方法签名，新增 `GradeRange gradeRange` 参数
- [ ] 处理 null 参数：`GradeRange range = gradeRange != null ? gradeRange : GradeRange.full();`
- [ ] 获取 `maxIndex` 和 `minIndex`
- [ ] 将 `GradeRange` 传递给内部调用的 `ColumnWiseAdjustmentService`
- [ ] 返回完整30列矩阵（范围外已经是0）
- [ ] 更新方法 Javadoc

**Implementation Notes**:
- `GroupSplittingDistributionService` 内部调用 `ColumnWiseAdjustmentService`，需要传递 `GradeRange` 参数
- 分组聚合和结果拆分逻辑不变，只是传递参数

**Files to Modify**:
- `src/main/java/org/example/domain/service/algorithm/impl/GroupSplittingDistributionServiceImpl.java`

---

### Task 6: 更新 AllocationAlgorithmSelector

**Priority**: High  
**Estimated Effort**: 30 minutes  
**Dependencies**: Task 3, Task 4, Task 5

**Description**:  
更新 `AllocationAlgorithmSelector`，移除压缩/扩展逻辑，直接传递完整30列矩阵和 `GradeRange` 参数给算法服务。

**Acceptance Criteria**:
- [ ] 构建 `GradeRange` 对象：`GradeRange gradeRange = GradeRange.of(maxGrade, minGrade);`
- [ ] 移除 `compactToRange()` 调用，直接使用完整30列 `matrix`
- [ ] 更新零行验证：调用新方法 `validateNoZeroRowsInRange(regions, matrix, gradeRange)`
- [ ] 调用算法时传递 `GradeRange` 参数：
  - `singleLevelService.distribute(regions, matrix, roundedTarget, gradeRange)`
  - `columnWiseService.distribute(regions, matrix, roundedTarget, gradeRange, null)`
  - `groupSplittingService.distribute(regions, matrix, roundedTarget, gradeRange, ...)`
- [ ] 移除 `expandFromRange()` 调用，直接使用算法返回的30列结果
- [ ] 更新方法 Javadoc

**Implementation Notes**:
- 零行验证需要新增方法 `validateNoZeroRowsInRange()`，只检查 `GradeRange` 范围内的列
- 移除 `maxIndex`/`minIndex` 的本地计算逻辑，改用 `GradeRange`
- 移除 `compactMatrix` 变量，所有地方改用 `matrix`
- 移除 `allocationCompact` 变量，改用 `finalAllocation`

**Files to Modify**:
- `src/main/java/org/example/application/service/coordinator/AllocationAlgorithmSelector.java`

---

### Task 7: 新增零行验证方法（基于 GradeRange）

**Priority**: High  
**Estimated Effort**: 20 minutes  
**Dependencies**: Task 1

**Description**:  
在 `AllocationMatrixUtils` 中新增 `validateNoZeroRowsInRange()` 方法，只检查 `GradeRange` 范围内的列。

**Acceptance Criteria**:
- [ ] 新增 `validateNoZeroRowsInRange(List<String> regions, BigDecimal[][] customerMatrix, GradeRange gradeRange)` 方法
- [ ] 只检查 `gradeRange.getMaxIndex()` 到 `gradeRange.getMinIndex()` 范围内的列
- [ ] 如果某区域在范围内所有列全为0，返回错误信息
- [ ] 如果验证通过，返回 null
- [ ] 添加方法 Javadoc

**Implementation Notes**:
- 复用现有 `validateNoZeroRows()` 的逻辑，但只检查指定范围的列
- 错误信息格式：`"以下区域在档位范围 [%s-%s] 内客户数全为0，无法进行分配: %s"`

**Files to Modify**:
- `src/main/java/org/example/shared/util/AllocationMatrixUtils.java`

---

### Task 8: 删除废弃方法

**Priority**: Medium  
**Estimated Effort**: 10 minutes  
**Dependencies**: Task 6

**Description**:  
从 `AllocationMatrixUtils` 中删除不再使用的 `compactToRange()` 和 `expandFromRange()` 方法。

**Acceptance Criteria**:
- [ ] 删除 `compactToRange()` 方法
- [ ] 删除 `expandFromRange()` 方法
- [ ] 确认没有其他地方调用这两个方法（编译通过）

**Implementation Notes**:
- 删除前先确认 Task 6 已完成，`AllocationAlgorithmSelector` 不再调用这两个方法
- 如果有其他地方调用，需要一并修改

**Files to Modify**:
- `src/main/java/org/example/shared/util/AllocationMatrixUtils.java`

---

### Task 9: 运行测试验证

**Priority**: High  
**Estimated Effort**: 15 minutes  
**Dependencies**: Task 8

**Description**:  
运行 `Week3FullPipelineTest` 验证重构后功能正常。

**Acceptance Criteria**:
- [ ] 执行 `mvn test -Dtest=Week3FullPipelineTest -q`
- [ ] 测试通过，无失败用例
- [ ] 之前因"客户矩阵列数必须为30"失败的卷烟（红金龙(硬神州腾龙)、利群(长嘴)）分配成功
- [ ] 检查日志，确认算法正确处理了 HG/LG 范围

**Implementation Notes**:
- 如果测试失败，检查算法实现是否正确处理了 `GradeRange`
- 检查零行验证是否只检查了范围内的列
- 检查返回的矩阵是否为30列

**Test Command**:
```bash
mvn test -Dtest=Week3FullPipelineTest -q
```

---

## Implementation Order

建议按以下顺序实施任务：

1. **Task 1**: 创建 GradeRange 值对象（基础设施）
2. **Task 2**: 更新算法接口签名（接口定义）
3. **Task 3**: 更新 SingleLevelDistributionService 实现
4. **Task 4**: 更新 ColumnWiseAdjustmentService 实现
5. **Task 5**: 更新 GroupSplittingDistributionService 实现
6. **Task 7**: 新增零行验证方法（为 Task 6 准备）
7. **Task 6**: 更新 AllocationAlgorithmSelector（集成层）
8. **Task 8**: 删除废弃方法（清理）
9. **Task 9**: 运行测试验证（验收）

## Risk Assessment

| Risk | Impact | Mitigation |
|------|--------|------------|
| 算法实现遗漏范围外列置0 | High | 初始化矩阵时全部置0，只在范围内赋值 |
| 零行验证逻辑错误 | Medium | 单独测试验证方法，确保只检查范围内列 |
| 其他代码调用废弃方法 | Low | 删除前全局搜索调用点 |
| 测试失败 | High | 逐个任务验证，及时发现问题 |

## Rollback Plan

如果重构后测试失败且无法快速修复：

1. 回退所有代码变更（使用 Git）
2. 保留 `GradeRange` 值对象和新增的验证方法（不影响现有逻辑）
3. 重新评估设计方案

## Success Criteria

- [ ] 所有任务完成
- [ ] `Week3FullPipelineTest` 测试通过
- [ ] 之前失败的卷烟分配成功
- [ ] 代码编译无错误
- [ ] 无废弃方法残留
