# 按价位段自选投放分配问题分析报告

## 问题现象
测试卷烟 `TEST_01643`（测试卷烟_按价位段自选投放）在价位段1中经过截断和微调后所有档位分配量均为0，导致分配失败。

## 测试环境数据
- **时间分区**: 2099/9/1
- **价位段1卷烟数**: 39支
- **问题卷烟**: TEST_01643
  - ADV: 959.88
  - 批发价: 825.00元
  - 价位段: 1

## 问题根本原因

### 1. 单层分配阶段
单层分配算法 `SingleLevelDistributionService` 会尽可能将分配集中在**高档位**（D30、D29、D28...），以最小化与ADV的误差。

对于ADV较小的卷烟（如959.88），可能只在D30或D30+D29这样少数高档位有分配值。

### 2. 价位段截断逻辑的问题
当前截断逻辑（`truncateAndAdjustBands`方法）：
1. 从 LG（D1）向 HG（D30）扫描
2. 找到首个"**至少有两支卷烟**分配量 > 0"的档位作为 cutoffIndex
3. 清零所有低于 cutoffIndex 的档位

**问题**：
- 如果某支小ADV卷烟只在极少数高档位有分配（如仅D30、D29）
- 而其他大ADV卷烟的分配分布更广（覆盖D30-D20）
- 系统会找到一个公共的cutoffIndex（如D20）
- 所有低于D20的档位被清零
- **但这支小ADV卷烟原本没有在D20以下的分配，其所有分配都在D30-D28范围**
- 截断后，如果该卷烟在D20这个档位上没有或只有少量分配，后续的全0检测会失败

### 3. 误差微调阶段的局限
`adjustErrorsForBand` 方法只能在 HG 到 cutoffIndex 的范围内微调。如果截断过于激进，微调无法恢复被清零的高档位分配。

### 4. 全0检测直接抛出异常
`checkZeroAllocation` 方法检测到全0后会直接抛出异常，中断整个分配流程。

## 具体执行流程示例

### 价位段1中的卷烟分配情况（简化示例）：

```
卷烟         ADV      单层分配结果（D30-D1）
TEST_01643   959.88   [2, 1, 0, 0, 0, ...] (仅D30、D29有值)
TEST_01644   1919.51  [5, 4, 3, 2, 1, 0, ...] (D30-D24有值)
TEST_01645   1719.45  [4, 4, 3, 2, 1, 0, ...] (D30-D24有值)
...其他36支卷烟...
```

### 截断扫描过程：
1. 从D1向D30扫描
2. 在D24档位发现有2支卷烟有分配（TEST_01644, TEST_01645）
3. 设置 cutoffIndex = 24（对应数组索引5）
4. 清零所有 > 24的档位（D23-D1）

### 截断后的结果：
```
卷烟         截断后分配结果
TEST_01643   [2, 1, 0, 0, 0, ...] (未被影响，因为它只在D30-D29)
TEST_01644   [5, 4, 3, 2, 1, 0, 0, ...] (D23以下被清零)
TEST_01645   [4, 4, 3, 2, 1, 0, 0, ...] (D23以下被清零)
```

**但是**，如果cutoffIndex更激进（如找到的是D28），那么：
```
TEST_01643   [0, 0, 0, ...] (所有档位被清零！)
```

## 解决方案

### 方案1：修改截断逻辑（推荐）
不使用"至少2支卷烟"的规则，而是为每支卷烟保留其有效分配区间：

```java
// 为每支卷烟找到其最低有效档位（从HG向LG扫描，找到最后一个非0值）
Map<String, Integer> cigaretteLowestGrades = new HashMap<>();
for (Map<String, Object> row : group) {
    BigDecimal[] grades = (BigDecimal[]) row.get("GRADES");
    int lowestGrade = findLowestNonZeroGrade(grades);
    String cigCode = row.get("CIG_CODE").toString();
    cigaretteLowestGrades.put(cigCode, lowestGrade);
}

// 为每支卷烟单独截断（只清零其自身的无效档位）
for (Map<String, Object> row : group) {
    String cigCode = row.get("CIG_CODE").toString();
    int lowestGrade = cigaretteLowestGrades.get(cigCode);
    BigDecimal[] grades = (BigDecimal[]) row.get("GRADES");
    
    // 只清零该卷烟自己的低档位（保护其他卷烟的分配）
    for (int col = lowestGrade + 1; col <= lowestIndex && col < grades.length; col++) {
        grades[col] = BigDecimal.ZERO;
    }
}
```

### 方案2：放宽全0检测（作为补充）
不直接抛出异常，而是恢复截断前的分配并记录警告：

```java
if (allZero) {
    // 恢复截断前的结果
    if (i < preTruncationGrades.size()) {
        row.put("GRADES", preTruncationGrades.get(i));
    }
    
    // 记录警告但不抛出异常
    String cigCode = row.get("CIG_CODE") != null ? row.get("CIG_CODE").toString() : "未知";
    String cigName = row.get("CIG_NAME") != null ? row.get("CIG_NAME").toString() : "未知";
    log.warn("价位段 {} 中的卷烟 {}（{}）经过截断后全0，已恢复截断前的分配结果",
             band, cigCode, cigName);
    
    // 继续处理，不中断流程
    continue;
}
```

### 方案3：智能分组截断
将价位段内的卷烟按ADV大小分组，对每组使用不同的截断策略：

```java
// 按ADV四分位数分组
List<Map<String, Object>> smallAdvGroup = new ArrayList<>();  // ADV < Q1
List<Map<String, Object>> mediumAdvGroup = new ArrayList<>(); // Q1 <= ADV < Q3
List<Map<String, Object>> largeAdvGroup = new ArrayList<>();  // ADV >= Q3

// 对每组使用不同的截断阈值
// 小ADV组：不截断或只截断极低档位
// 中ADV组：使用温和截断
// 大ADV组：使用当前的截断逻辑
```

## 建议的优先级

1. **立即实施**：方案2（放宽全0检测），作为临时解决方案，确保系统不会因个别卷烟失败而中断
2. **短期优化**：方案1（修改截断逻辑），从根本上解决截断过于激进的问题
3. **长期优化**：方案3（智能分组截断），进一步优化不同ADV规模卷烟的分配效果

## 测试验证建议

1. 对价位段1中ADV最小的10支卷烟进行单独测试
2. 对价位段1中ADV分布在不同四分位的卷烟进行对比测试
3. 验证修复后所有84支候选卷烟都能成功写回

## 相关代码位置

- `PriceBandAllocationServiceImpl.truncateAndAdjustBands()` - 第148行
- `PriceBandAllocationServiceImpl.checkZeroAllocation()` - 第361行
- `PriceBandAllocationServiceImpl.adjustErrorsForBand()` - 第235行

