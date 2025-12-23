# Design Document: Grade Range Refactor

## Overview

本设计文档描述了 HG/LG（最高档位/最低档位）参数处理逻辑的重构方案。核心变更是将 HG/LG 的处理职责从 `AllocationAlgorithmSelector` 下沉到各算法服务内部，确保客户矩阵始终保持30列，算法服务负责根据 GradeRange 参数确定计算范围并返回完整的30列结果。

## Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                      CustomerMatrixBuilder                           │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │ 1. 从 region_customer_statistics 查询30列客户数据              │    │
│  │ 2. 构建 RegionCustomerMatrix（30列）                          │    │
│  │ 3. 调用 BiWeeklyVisitBoostService 执行两周一访上浮（可选）       │    │
│  │ 4. 返回完整30列客户矩阵                                        │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                              │                                       │
│                              ▼                                       │
│                    AllocationAlgorithmSelector                       │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │ 1. 接收完整30列客户矩阵（已含两周一访上浮）                       │    │
│  │ 2. 构建 GradeRange(maxGrade, minGrade)                       │    │
│  │ 3. 基于 GradeRange 验证零行（只检查范围内的列）                  │    │
│  │ 4. 选择算法并传递 GradeRange 参数                              │    │
│  │ 5. 直接返回算法的30列结果                                      │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                              │                                       │
│                              ▼                                       │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │              Algorithm Services (Domain Layer)               │    │
│  │  ┌─────────────┐ ┌─────────────┐ ┌─────────────────────┐    │    │
│  │  │SingleLevel  │ │ColumnWise  │ │ GroupSplitting      │    │    │
│  │  │Distribution │ │Adjustment  │ │ Distribution        │    │    │
│  │  │Service      │ │Service     │ │ Service             │    │    │
│  │  └─────────────┘ └─────────────┘ └─────────────────────┘    │    │
│  │                                                              │    │
│  │  输入: 30列客户矩阵 + GradeRange + 目标值                      │    │
│  │  处理: 只在 HG~LG 范围内计算，范围外置0                         │    │
│  │  输出: 30列分配矩阵                                           │    │
│  └─────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────┘
```

### 与两周一访上浮的关系

两周一访上浮（BiWeeklyVisitBoostService）与 HG/LG 处理是**串行独立**的：

1. **上浮阶段**：在 `CustomerMatrixBuilder.buildWithBoost()` 中执行，修改完整30列客户矩阵
2. **计算阶段**：算法服务接收上浮后的30列矩阵，根据 GradeRange 只在指定范围内计算

本次重构**不影响**两周一访上浮逻辑，因为：
- 上浮操作始终作用于完整30列矩阵
- 上浮后的矩阵仍然是30列，传给算法服务
- 算法服务根据 HG/LG 只在指定范围内计算，范围外的上浮数据不参与计算

## Components and Interfaces

### 1. GradeRange 值对象

**位置**: `src/main/java/org/example/domain/model/valueobject/GradeRange.java`

```java
package org.example.domain.model.valueobject;

import org.example.shared.util.GradeParser;

/**
 * 档位范围值对象。
 * <p>
 * 封装最高档位（HG）和最低档位（LG），提供索引转换和范围判断功能。
 * </p>
 */
public class GradeRange {
    
    private static final String DEFAULT_MAX_GRADE = "D30";
    private static final String DEFAULT_MIN_GRADE = "D1";
    
    private final String maxGrade;  // HG，如 "D30"
    private final String minGrade;  // LG，如 "D1"
    
    private GradeRange(String maxGrade, String minGrade) {
        this.maxGrade = normalizeGrade(maxGrade, DEFAULT_MAX_GRADE);
        this.minGrade = normalizeGrade(minGrade, DEFAULT_MIN_GRADE);
    }
    
    /** 创建默认范围（D30-D1） */
    public static GradeRange full() {
        return new GradeRange(DEFAULT_MAX_GRADE, DEFAULT_MIN_GRADE);
    }
    
    /** 创建指定范围 */
    public static GradeRange of(String maxGrade, String minGrade) {
        return new GradeRange(maxGrade, minGrade);
    }
    
    /** 获取 HG 索引（D30=0） */
    public int getMaxIndex() {
        return GradeParser.parseGradeToIndex(maxGrade);
    }
    
    /** 获取 LG 索引（D1=29） */
    public int getMinIndex() {
        return GradeParser.parseGradeToIndex(minGrade);
    }
    
    /** 判断索引是否在范围内 */
    public boolean contains(int gradeIndex) {
        return gradeIndex >= getMaxIndex() && gradeIndex <= getMinIndex();
    }
    
    public String getMaxGrade() { return maxGrade; }
    public String getMinGrade() { return minGrade; }
    
    private static String normalizeGrade(String grade, String defaultValue) {
        if (grade == null || grade.trim().isEmpty()) {
            return defaultValue;
        }
        return grade.trim().toUpperCase();
    }
}
```

### 2. 算法接口变更

**SingleLevelDistributionService**:
```java
BigDecimal[][] distribute(List<String> targetRegions,
                          BigDecimal[][] regionCustomerMatrix,
                          BigDecimal targetAmount,
                          GradeRange gradeRange);  // 新增参数
```

**ColumnWiseAdjustmentService**:
```java
BigDecimal[][] distribute(List<String> segments,
                          BigDecimal[][] customerMatrix,
                          BigDecimal targetAmount,
                          GradeRange gradeRange,  // 新增参数
                          Comparator<Integer> segmentComparator);
```

**GroupSplittingDistributionService**:
```java
BigDecimal[][] distribute(List<String> regions,
                          BigDecimal[][] customerMatrix,
                          BigDecimal targetAmount,
                          GradeRange gradeRange,  // 新增参数
                          Function<String, String> groupingFunction,
                          Map<String, BigDecimal> groupRatios);
```

### 3. 算法实现变更模式

各算法实现需要遵循以下模式：

```java
@Override
public BigDecimal[][] distribute(..., GradeRange gradeRange) {
    // 1. 处理 null 参数
    GradeRange range = gradeRange != null ? gradeRange : GradeRange.full();
    int maxIndex = range.getMaxIndex();
    int minIndex = range.getMinIndex();
    
    // 2. 初始化30列结果矩阵（全0）
    BigDecimal[][] result = initMatrix(regionCount, 30);
    
    // 3. 只在 maxIndex ~ minIndex 范围内进行计算
    // ... 算法核心逻辑，只操作 result[r][maxIndex] 到 result[r][minIndex]
    
    // 4. 返回完整30列矩阵（范围外已经是0）
    return result;
}
```

### 4. AllocationAlgorithmSelector 变更

移除压缩/扩展逻辑，直接传递参数：

```java
public AllocationResult execute(..., String maxGrade, String minGrade, ...) {
    // 构建 GradeRange
    GradeRange gradeRange = GradeRange.of(maxGrade, minGrade);
    
    // 构建完整30列矩阵（不压缩）
    BigDecimal[][] matrix = buildMatrix(rows);  // 30列
    
    // 基于 GradeRange 验证零行
    String zeroRowCheck = validateNoZeroRowsInRange(regions, matrix, gradeRange);
    
    // 调用算法（传递 GradeRange）
    BigDecimal[][] allocation = columnWiseService.distribute(
        regions, matrix, targetAmount, gradeRange, null);
    
    // 直接返回（不扩展）
    return AllocationResult.success(regions, matrix, allocation);
}
```

## Data Models

### GradeRange 值对象

| 字段 | 类型 | 说明 |
|------|------|------|
| maxGrade | String | 最高档位，如 "D30" |
| minGrade | String | 最低档位，如 "D1" |

### 矩阵维度约定

| 矩阵 | 维度 | 说明 |
|------|------|------|
| customerMatrix | [regionCount][30] | 客户矩阵，始终30列 |
| allocationMatrix | [regionCount][30] | 分配矩阵，始终30列 |

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: 档位字符串到索引转换正确性

*For any* 有效的档位字符串（D1-D30），`GradeParser.parseGradeToIndex()` 应返回正确的索引值，其中 D30=0, D29=1, ..., D1=29。

**Validates: Requirements 1.4, 1.5**

### Property 2: GradeRange.contains() 范围判断正确性

*For any* GradeRange 和任意整数索引，`contains(index)` 应返回 `true` 当且仅当 `maxIndex <= index <= minIndex`。

**Validates: Requirements 1.6**

### Property 3: 分配结果范围外列全为零

*For any* 客户矩阵、目标值和 GradeRange，算法返回的分配矩阵中，索引 < maxIndex 或索引 > minIndex 的所有列值应全为0。

**Validates: Requirements 3.2, 3.3, 3.4**

### Property 4: 分配结果矩阵维度正确

*For any* 有效输入，算法返回的分配矩阵应为 [regionCount][30] 维度。

**Validates: Requirements 3.5**

## Error Handling

| 场景 | 处理方式 |
|------|----------|
| GradeRange 为 null | 使用默认范围 GradeRange.full() |
| maxGrade/minGrade 为 null 或空 | 使用默认值 "D30"/"D1" |
| maxIndex > minIndex（无效范围） | 抛出 IllegalArgumentException |
| 范围内所有档位客户数全为0 | 返回 AllocationResult.failure() |

## Testing Strategy

### 单元测试

1. **GradeRange 值对象测试**
   - 测试 `full()` 返回默认范围
   - 测试 `of()` 创建指定范围
   - 测试 null/空字符串的默认值处理
   - 测试 `getMaxIndex()`/`getMinIndex()` 转换正确性
   - 测试 `contains()` 边界情况

2. **算法服务测试**
   - 测试 GradeRange 为 null 时使用默认范围
   - 测试非默认 GradeRange 时范围外列为0
   - 测试返回矩阵维度为30列

### 属性测试

使用 JUnit-Quickcheck 或类似框架：

1. **Property 1**: 生成随机档位字符串，验证索引转换
2. **Property 2**: 生成随机 GradeRange 和索引，验证 contains()
3. **Property 3**: 生成随机矩阵和 GradeRange，验证范围外列为0
4. **Property 4**: 生成随机输入，验证输出维度

### 集成测试

1. **Week3FullPipelineTest**: 验证重构后全链路测试通过
2. **回归测试**: 验证之前失败的卷烟（红金龙、利群）分配成功
