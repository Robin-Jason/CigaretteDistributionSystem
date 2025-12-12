# 多区域带权重分配算法（GROUP_SPLITTING）详细说明

## 一、算法概述

多区域带权重分配算法（GROUP_SPLITTING）用于处理**多区域**且需要**按权重比例分配**的卷烟分配问题，通过"权重拆分 + 分组独立分配"的策略，将总预投放量按权重比例拆分到各分组，然后对每个分组独立运行分配算法。

**典型应用场景**：
- **市场类型**：城网/农网比例分配（默认4:6）
- **诚信自律小组**：按客户数占比分配
- **其他需要权重比例的扩展类型**

---

## 二、数学模型

### 2.1 问题定义

设共有 $R$ 个投放区域和 $B$ 个档位（通常 $B = 30$，从 D30 到 D1，索引从 0 到 29，其中索引 0 对应 D30，索引 29 对应 D1）。

对于区域 $i$（$i = 0, 1, \ldots, R-1$）和档位 $j$（$j = 0, 1, \ldots, B-1$）：
- 令 $c_{ij}$ 表示区域 $i$ 档位 $j$ 的客户数（从给定的客户数表中获取）
- 令 $x_{ij}$ 表示分配给区域 $i$ 档位 $j$ 的卷烟数量，其中 $x_{ij}$ 为非负整数

### 2.2 分组定义

将 $R$ 个区域划分为 $G$ 个分组，每个分组 $g$（$g = 1, 2, \ldots, G$）：
- 包含 $R_g$ 个区域
- 具有权重 $w_g$（$\sum_{g=1}^{G} w_g > 0$）
- 分组目标量：$T_g = T \times \frac{w_g}{\sum_{g=1}^{G} w_g}$，其中 $T$ 为总预投放量

### 2.3 约束条件

**单调性约束**（非递增约束）：
对于每个区域 $i$，必须满足：
$$x_{i0} \geq x_{i1} \geq \cdots \geq x_{i(B-1)}$$

即每个区域内，高档位分配值不小于低档位分配值（D30 ≥ D29 ≥ ... ≥ D1）。

**约束重要性**：单调性约束是第一位的，必须严格满足。如果某个操作违反约束，必须强制调整或跳过该操作。

### 2.4 目标函数

**实际投放量** $S$ 计算公式为：
$$S = \sum_{i=0}^{R-1} \sum_{j=0}^{B-1} x_{ij} \cdot c_{ij}$$

**分组目标**：对于每个分组 $g$，目标是最小化误差 $|S_g - T_g|$，其中 $S_g$ 为分组 $g$ 的实际投放量，$T_g$ 为分组 $g$ 的目标预投放量。

---

## 三、算法详细步骤

### 3.1 阶段0：算法选择（DistributionAlgorithmEngine）

在进入GROUP_SPLITTING算法之前，需要先进行算法选择判断：

#### 3.1.1 检查待投放区域数量

**步骤1**：检查待投放区域数量
- 如果只有1个区域 → 选择 `SINGLE_LEVEL` 算法，**不进入GROUP_SPLITTING**
- 如果有多个区域 → 继续判断

#### 3.1.2 检查是否存在带权重扩展类型

**步骤2**：通过 `GroupRatioProvider` 计算分组比例
- 如果 `groupRatios` 为空 → 选择 `COLUMN_WISE` 算法，**不进入GROUP_SPLITTING**
- 如果 `groupRatios` 不为空 → 继续判断

#### 3.1.3 检查带权重扩展类型的区域种类

**步骤3**：检查带权重扩展类型的区域种类数
- 统计不同的分组ID数量（即带权重扩展类型的区域种类数）
- 如果只有1种（如只有城网） → 选择 `COLUMN_WISE` 算法（**退化**），**不进入GROUP_SPLITTING**
- 如果有多种 → 继续判断

#### 3.1.4 检查带权重扩展类型的笛卡尔积前区域数

**步骤4**：检查带权重扩展类型的笛卡尔积前区域数
- 统计每个分组对应的区域数量
- 如果所有分组都只有1个区域 → 选择 `GROUP_SPLITTING`（内部会选择SINGLE_LEVEL）
- 如果至少有一个分组有多个区域 → 选择 `GROUP_SPLITTING`

**只有满足以下条件才会选择GROUP_SPLITTING算法**：
1. 待投放区域数量 > 1
2. 存在带权重扩展类型（`groupRatios` 不为空）
3. 带权重扩展类型的区域种类数 > 1
4. 至少有一个分组有多个区域（或所有分组都只有1个区域，但为了保持一致性仍使用GROUP_SPLITTING）

---

### 3.2 阶段1：构建分组（buildGroups）

#### 3.2.1 算法描述

根据区域到分组的映射函数，将区域分组。

#### 3.2.2 具体步骤

1. **初始化分组映射**：
   ```java
   Map<String, GroupContext> groups = new HashMap<>();
   ```

2. **遍历区域列表**：
   ```java
   for (int i = 0; i < regions.size(); i++) {
       String region = regions.get(i);
       String groupId = groupingFunction.apply(region);
       // 如果分组ID为空，使用"UNSPECIFIED"
       if (groupId == null || groupId.trim().isEmpty()) {
           groupId = "UNSPECIFIED";
       }
       // 将区域索引添加到对应分组
       groups.computeIfAbsent(groupId, key -> new GroupContext())
              .indices().add(i);
   }
   ```

3. **输出**：分组映射（key=分组ID，value=分组上下文，包含区域索引列表）

---

### 3.3 阶段2：计算分组权重（computeGroupWeights）

#### 3.3.1 算法描述

从 `groupRatios` 中获取各分组的权重，如果某个分组没有权重，默认为1.0。

#### 3.3.2 具体步骤

1. **遍历所有分组ID**：
   ```java
   for (String groupId : groupIds) {
       BigDecimal ratio = groupRatios != null ? groupRatios.get(groupId) : null;
       if (ratio == null || ratio.compareTo(BigDecimal.ZERO) <= 0) {
           ratio = BigDecimal.ONE;  // 默认权重为1.0
       }
       weights.put(groupId, ratio);
   }
   ```

2. **计算总权重**：
   $$W_{\text{total}} = \sum_{g=1}^{G} w_g$$

3. **输出**：分组权重映射（key=分组ID，value=权重）

---

### 3.4 阶段3：按权重拆分预投放量

#### 3.4.1 算法描述

根据分组权重，将总预投放量拆分到各分组。

#### 3.4.2 具体步骤

对于每个分组 $g$：

1. **计算分组目标量**：
   $$T_g = T \times \frac{w_g}{W_{\text{total}}}$$

   其中：
   - $T$ 为总预投放量
   - $w_g$ 为分组 $g$ 的权重
   - $W_{\text{total}}$ 为总权重

2. **提取分组子矩阵**：
   ```java
   BigDecimal[][] groupMatrix = extractSubMatrix(groupIndices, customerMatrix);
   ```
   - 从完整客户矩阵中提取该分组的区域数据

3. **执行分组独立分配**：
   ```java
   BigDecimal[][] groupAllocation = runStandaloneAlgorithm(groupRegions, groupMatrix, groupTarget);
   ```

4. **合并结果**：
   ```java
   copyGroupResult(groupAllocation, groupIndices, finalMatrix);
   ```
   - 将分组分配结果复制回最终矩阵的对应位置

---

### 3.5 阶段4：分组独立分配（runStandaloneAlgorithm）

#### 3.5.1 算法描述

对每个分组独立运行分配算法，根据分组内区域数选择算法：
- 如果分组内只有1个区域 → 使用 `SINGLE_LEVEL` 算法
- 如果分组内有多个区域 → 使用 `COLUMN_WISE` 算法

#### 3.5.2 算法选择逻辑

```java
if (regionCount <= 1) {
    // 使用SINGLE_LEVEL算法
    return singleLevelAlgorithm.distribute(groupRegions, customerMatrix, targetAmount);
} else {
    // 使用COLUMN_WISE算法
    return columnWiseAlgorithm.distribute(groupRegions, customerMatrix, targetAmount, null);
}
```

#### 3.5.3 SINGLE_LEVEL算法

如果分组内只有1个区域，使用单区域分配算法（详见《单区域分配算法（SINGLE_LEVEL）详细说明.md》）。

**算法流程**：
1. 粗调阶段：从HG到LG逐档位+1，直到刚好超出目标
2. 高档位微调阶段：生成4个候选方案
3. 方案选择：选择误差最小的方案

#### 3.5.4 COLUMN_WISE算法

如果分组内有多个区域，使用多区域无权重分配算法（详见《多区域无权重分配算法（COLUMN_WISE）详细说明.md》）。

**算法流程**：
1. 粗调阶段：从HG到LG逐档位列+1（整列+1），直到刚好超出目标
2. 高档位微调阶段：生成4个候选方案
3. 方案选择：选择误差最小的方案

---

### 3.6 阶段5：单调性约束强制执行

#### 3.6.1 算法描述

在最终返回前，**强制执行单调性约束**作为双重保险。

#### 3.6.2 具体步骤

```java
for (BigDecimal[] row : matrix) {
    for (int grade = 1; grade < GRADE_COUNT; grade++) {
        if (row[grade].compareTo(row[grade - 1]) > 0) {
            row[grade] = row[grade - 1];  // 强制调整
        }
    }
}
```

---

## 四、算法流程图

```
开始
  ↓
【DistributionAlgorithmEngine - 算法选择】
  ↓
检查待投放区域数量
  ├─ 如果只有1个 → SINGLE_LEVEL ✓
  └─ 如果有多个 → 继续
  ↓
检查是否存在带权重扩展类型
  ├─ 如果不存在 → COLUMN_WISE ✓
  └─ 如果存在 → 继续
  ↓
检查带权重扩展类型的区域种类
  ├─ 如果只有1种 → COLUMN_WISE（退化）✓
  └─ 如果有多种 → 继续
  ↓
检查带权重扩展类型的笛卡尔积前区域数
  ├─ 如果所有分组都只有1个区域 → GROUP_SPLITTING（内部会选择SINGLE_LEVEL）
  └─ 如果至少有一个分组有多个区域 → GROUP_SPLITTING ✓
  ↓
【DefaultGroupSplittingDistributionAlgorithm - 权重拆分】
  ↓
构建分组（根据区域→分组ID映射）
  ↓
计算分组权重（从groupRatios获取，默认1.0）
  ↓
按权重拆分预投放量
  ├─ groupTarget = totalTarget × (weight / totalWeight)
  └─ 对每个分组：
      ├─ 提取分组子矩阵
      ├─ 执行分组独立分配
      │   ├─ 如果分组内只有1个区域 → SINGLE_LEVEL
      │   └─ 如果分组内有多个区域 → COLUMN_WISE
      └─ 合并结果到最终矩阵
  ↓
强制执行单调性约束
  ↓
返回最终分配矩阵
  ↓
结束
```

---

## 五、关键实现细节

### 5.1 可扩展的分组比例提供者（GroupRatioProvider）

支持多种扩展类型的分组比例计算：

#### 5.1.1 MarketTypeRatioProvider（市场类型比例提供者）

**规则**：
1. 默认比例：城网:农网 = 4:6（用户未传入比例参数时使用）
2. 如果用户传入了比例参数，使用用户参数
3. 比例参数生效条件：当前区域既包括城网也包括农网
4. 如果为单扩展单区域，退化为单区域分配算法（不设置比例）
5. 如果为双扩展且市场类型扩展类型的区域仅包含一种，退化为无比例参数的算法（不设置比例）

**实现**：
```java
@Override
public Map<String, BigDecimal> calculateGroupRatios(...) {
    // 检查区域中是否同时包含城网和农网
    boolean hasUrban = regions.contains("城网");
    boolean hasRural = regions.contains("农网");
    
    // 如果区域仅包含一种市场类型，不设置比例（退化为无比例参数算法）
    if (!hasUrban || !hasRural) {
        return Collections.emptyMap();
    }
    
    // 如果为单扩展单区域（只有一个区域），不设置比例（退化为单区域分配算法）
    if (regions.size() == 1) {
        return Collections.emptyMap();
    }
    
    // 使用默认比例或用户传入的比例
    // ...
}
```

#### 5.1.2 IntegrityGroupRatioProvider（诚信自律小组比例提供者）

**规则**：
1. 根据各小组的客户数占比计算权重
2. 权重 = 小组客户数 / 总客户数

**实现**：
```java
@Override
public Map<String, BigDecimal> calculateGroupRatios(...) {
    // 计算每个诚信自律小组的总客户数
    Map<String, BigDecimal> groupCustomerCounts = new HashMap<>();
    BigDecimal totalCustomers = BigDecimal.ZERO;
    
    for (int i = 0; i < regions.size(); i++) {
        String region = regions.get(i);
        String groupId = extractIntegrityGroup(region);
        BigDecimal customerCount = calculateTotalCustomers(customerMatrix[i]);
        groupCustomerCounts.put(groupId, 
            groupCustomerCounts.getOrDefault(groupId, BigDecimal.ZERO).add(customerCount));
        totalCustomers = totalCustomers.add(customerCount);
    }
    
    // 计算比例
    Map<String, BigDecimal> ratios = new HashMap<>();
    for (Map.Entry<String, BigDecimal> entry : groupCustomerCounts.entrySet()) {
        BigDecimal ratio = entry.getValue().divide(totalCustomers, MATH_CONTEXT);
        ratios.put(entry.getKey(), ratio);
    }
    
    return ratios;
}
```

---

### 5.2 主扩展和子扩展的处理

#### 5.2.1 定义说明

- **主扩展**：为了说明方便，如果扩展类型存在区县，区县作为主扩展（写在括号外面）
- **子扩展**：其他扩展类型作为子扩展（写在括号里面）
- **实际处理**：主扩展和子扩展可以互换，只是为了表达习惯方便

#### 5.2.2 分配计算时的处理顺序

**重要**：分配计算时，**先按带权重扩展类型拆分预投放量，再在拆分后的区域分组**。

**示例**：档位+区县+市场类型
- 区域列表：["丹江（城网）", "丹江（农网）", "郧西（城网）", "郧西（农网）"]
- **第一步**：按市场类型拆分预投放量
  - 城网组：["丹江（城网）", "郧西（城网）"]，目标量 = 总预投放量 × 0.4
  - 农网组：["丹江（农网）", "郧西（农网）"]，目标量 = 总预投放量 × 0.6
- **第二步**：每个组内按区县分组（如果需要）
  - 城网组 → 丹江（城网）：1个区域 → SINGLE_LEVEL
  - 城网组 → 郧西（城网）：1个区域 → SINGLE_LEVEL
  - 农网组 → 丹江（农网）：1个区域 → SINGLE_LEVEL
  - 农网组 → 郧西（农网）：1个区域 → SINGLE_LEVEL

#### 5.2.3 写回数据库时的处理

**重要**：写回数据库时，**区县（如果存在这个扩展类型）作为主扩展区域写入**。

**示例**：
- 分配计算时的区域：["丹江（城网）", "丹江（农网）", "郧西（城网）", "郧西（农网）"]
- 写回数据库时的区域：["丹江（城网）", "丹江（农网）", "郧西（城网）", "郧西（农网）"]
  - 区县作为主扩展区域（写在括号外面）
  - 市场类型作为子扩展（写在括号里面）

---

### 5.3 多权重扩展类型的处理

#### 5.3.1 场景说明

如果主扩展和子扩展都是带权重的扩展类型，例如：**档位+市场类型+诚信自律小组**

#### 5.3.2 处理流程

**分两步拆分比例**：

1. **第一步**：按市场类型拆分预投放量
   - 城网组：目标量 = 总预投放量 × 0.4
   - 农网组：目标量 = 总预投放量 × 0.6

2. **第二步**：在每个市场类型组内，按诚信自律小组拆分预投放量
   - 城网组内：
     - 小组A：目标量 = 城网组目标量 × (小组A客户数 / 城网组总客户数)
     - 小组B：目标量 = 城网组目标量 × (小组B客户数 / 城网组总客户数)
   - 农网组内：
     - 小组C：目标量 = 农网组目标量 × (小组C客户数 / 农网组总客户数)
     - 小组D：目标量 = 农网组目标量 × (小组D客户数 / 农网组总客户数)

3. **最后**：每个小组（笛卡尔积后的区域）都能采用SINGLE_LEVEL算法

---

## 六、算法复杂度分析

### 6.1 时间复杂度

- **构建分组**：$O(R)$，其中 $R$ 为区域数量
- **计算分组权重**：$O(G)$，其中 $G$ 为分组数量
- **权重拆分**：$O(G)$
- **分组独立分配**：
  - 如果使用SINGLE_LEVEL：$O(B)$，其中 $B$ 为档位数量（通常为30）
  - 如果使用COLUMN_WISE：$O(B \times R_g)$，其中 $R_g$ 为分组内区域数量
- **单调性约束强制执行**：$O(R \times B)$

**总时间复杂度**：
- 如果所有分组都使用SINGLE_LEVEL：$O(R + G \times B)$
- 如果所有分组都使用COLUMN_WISE：$O(R + \sum_{g=1}^{G} (B \times R_g)) = O(R + B \times R) = O(B \times R)$

### 6.2 空间复杂度

- **分组映射存储**：$O(R)$
- **分组权重存储**：$O(G)$
- **分配矩阵存储**：$O(R \times B)$
- **分组子矩阵存储**：$O(\max(R_g) \times B)$

**总空间复杂度**：$O(R \times B)$

---

## 七、算法特点

### 7.1 优点

1. **灵活性**：支持任意数量的分组与权重
2. **可扩展性**：通过 `GroupRatioProvider` 接口支持新的扩展类型
3. **精确性**：每个分组独立运行最优算法，确保结果最优
4. **单调性保证**：严格满足单调性约束，确保分配合理性
5. **退化处理**：自动处理退化情况，避免不必要的权重拆分

### 7.2 适用场景

- **多区域分配**：有多个投放区域的情况
- **权重比例**：各分组之间需要按权重比例分配
- **扩展类型**：市场类型、诚信自律小组等需要权重比例的扩展类型
- **精确分配**：需要最小化误差的场景

---

## 八、示例

### 8.1 示例1：市场类型单扩展

**输入数据**：
- 总预投放量：$T = 100000$
- 区域：["城网", "农网"]
- 权重：{"城网": 0.4, "农网": 0.6}
- 扩展类型：档位+市场类型

**执行流程**：

1. **算法选择**：
   - 区域数量 = 2 > 1 ✓
   - 存在带权重扩展类型 ✓
   - 带权重扩展类型的区域种类数 = 2 > 1 ✓
   - 每个分组都有1个区域 → GROUP_SPLITTING（内部会选择SINGLE_LEVEL）

2. **权重拆分**：
   - 城网组目标 = 100000 × (0.4 / 1.0) = 40000
   - 农网组目标 = 100000 × (0.6 / 1.0) = 60000

3. **分组独立分配**：
   - 城网组：1个区域 → SINGLE_LEVEL算法，目标40000
   - 农网组：1个区域 → SINGLE_LEVEL算法，目标60000

4. **输出结果**：
   - 城网：实际投放量 ≈ 40000
   - 农网：实际投放量 ≈ 60000

---

### 8.2 示例2：市场类型+区县双扩展

**输入数据**：
- 总预投放量：$T = 100000$
- 区域：["丹江（城网）", "丹江（农网）", "郧西（城网）", "郧西（农网）"]
- 权重：{"城网": 0.4, "农网": 0.6}
- 扩展类型：档位+区县+市场类型

**执行流程**：

1. **算法选择**：
   - 区域数量 = 4 > 1 ✓
   - 存在带权重扩展类型 ✓
   - 带权重扩展类型的区域种类数 = 2（城网、农网）> 1 ✓
   - 至少有一个分组有多个区域 → GROUP_SPLITTING

2. **权重拆分**：
   - 城网组：["丹江（城网）", "郧西（城网）"]，目标 = 100000 × 0.4 = 40000
   - 农网组：["丹江（农网）", "郧西（农网）"]，目标 = 100000 × 0.6 = 60000

3. **分组独立分配**：
   - 城网组：2个区域 → COLUMN_WISE算法，目标40000
   - 农网组：2个区域 → COLUMN_WISE算法，目标60000

4. **输出结果**：
   - 城网组：实际投放量 ≈ 40000
   - 农网组：实际投放量 ≈ 60000
   - 写回数据库时，区县作为主扩展区域写入

---

### 8.3 示例3：市场类型+诚信自律小组双权重扩展

**输入数据**：
- 总预投放量：$T = 100000$
- 区域：["城网-小组A", "城网-小组B", "农网-小组C", "农网-小组D"]
- 第一步权重（市场类型）：{"城网": 0.4, "农网": 0.6}
- 第二步权重（诚信自律小组）：按客户数占比计算
- 扩展类型：档位+市场类型+诚信自律小组

**执行流程**：

1. **第一步：按市场类型拆分**：
   - 城网组：["城网-小组A", "城网-小组B"]，目标 = 100000 × 0.4 = 40000
   - 农网组：["农网-小组C", "农网-小组D"]，目标 = 100000 × 0.6 = 60000

2. **第二步：在每个市场类型组内，按诚信自律小组拆分**：
   - 城网组内：
     - 小组A客户数 = 1000，小组B客户数 = 2000，总客户数 = 3000
     - 小组A目标 = 40000 × (1000 / 3000) = 13333.33
     - 小组B目标 = 40000 × (2000 / 3000) = 26666.67
   - 农网组内：
     - 小组C客户数 = 1500，小组D客户数 = 2500，总客户数 = 4000
     - 小组C目标 = 60000 × (1500 / 4000) = 22500
     - 小组D目标 = 60000 × (2500 / 4000) = 37500

3. **分组独立分配**：
   - 每个小组：1个区域 → SINGLE_LEVEL算法

4. **输出结果**：
   - 每个小组的实际投放量 ≈ 对应的目标量

---

## 九、注意事项

### 9.1 算法选择

1. **退化逻辑**：在算法选择阶段就实现了退化逻辑检查，避免不必要的权重拆分
2. **区域数量检查**：如果只有1个区域，直接使用SINGLE_LEVEL，不进入GROUP_SPLITTING
3. **权重扩展类型检查**：如果带权重扩展类型只有1种，退化为COLUMN_WISE

### 9.2 权重拆分

1. **权重归一化**：权重不需要总和为1，算法会自动归一化
2. **默认权重**：如果某个分组没有权重，默认为1.0
3. **权重提供者**：通过 `GroupRatioProvider` 接口扩展，新增扩展类型只需实现该接口

### 9.3 分组独立分配

1. **算法选择**：根据分组内区域数选择算法（1个区域 → SINGLE_LEVEL，多个区域 → COLUMN_WISE）
2. **独立性**：每个分组独立运行分配算法，分组之间互不影响
3. **目标量**：每个分组的目标量由权重比例决定

### 9.4 单调性约束

1. **约束重要性**：约束条件重要性是第一位的，必须严格满足
2. **双重保险**：在最终返回前强制执行单调性约束，确保结果正确
3. **强制调整**：如果违反约束，强制调整（将低档位设为等于高档位）

### 9.5 主扩展和子扩展

1. **分配计算顺序**：先按带权重扩展类型拆分预投放量，再在拆分后的区域分组
2. **写回数据库**：写回数据库时，区县（如果存在）作为主扩展区域写入
3. **表达习惯**：主扩展和子扩展可以互换，只是为了表达习惯方便

### 9.6 多权重扩展类型

1. **分两步拆分**：如果主扩展和子扩展都是带权重的扩展类型，分两步拆分比例
2. **笛卡尔积**：最后笛卡尔积的每个区域都能采用SINGLE_LEVEL算法
3. **客户数占比**：诚信自律小组等扩展类型按客户数占比计算权重

---

## 十、与其他算法的关系

### 10.1 与SINGLE_LEVEL算法的关系

- GROUP_SPLITTING算法内部会调用SINGLE_LEVEL算法
- 当分组内只有1个区域时，使用SINGLE_LEVEL算法
- SINGLE_LEVEL算法是GROUP_SPLITTING算法的子算法

### 10.2 与COLUMN_WISE算法的关系

- GROUP_SPLITTING算法内部会调用COLUMN_WISE算法
- 当分组内有多个区域时，使用COLUMN_WISE算法
- COLUMN_WISE算法是GROUP_SPLITTING算法的子算法

### 10.3 算法选择逻辑

```
SINGLE_LEVEL ← 1个区域
    ↓
COLUMN_WISE ← 多个区域，无权重
    ↓
GROUP_SPLITTING ← 多个区域，有权重
    ├─ 分组内1个区域 → SINGLE_LEVEL
    └─ 分组内多个区域 → COLUMN_WISE
```

---

**文档版本**：v1.0  
**最后更新**：2025年1月

