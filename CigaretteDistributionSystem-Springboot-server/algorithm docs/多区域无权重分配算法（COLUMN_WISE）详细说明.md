# 多区域无权重分配算法（COLUMN_WISE）详细说明

## 一、算法概述

多区域无权重分配算法（COLUMN_WISE）用于处理**多区域**且**无权重比例**的卷烟分配问题，通过"粗调 + 高档位微调"的两阶段策略，生成4个候选方案并选择误差最小的方案。

---

## 二、数学模型

### 2.1 问题定义

设共有 $R$ 个投放区域和 $B$ 个档位（通常 $B = 30$，从 D30 到 D1，索引从 0 到 29，其中索引 0 对应 D30，索引 29 对应 D1）。

对于区域 $i$（$i = 0, 1, \ldots, R-1$）和档位 $j$（$j = 0, 1, \ldots, B-1$）：
- 令 $c_{ij}$ 表示区域 $i$ 档位 $j$ 的客户数（从给定的客户数表中获取）
- 令 $x_{ij}$ 表示分配给区域 $i$ 档位 $j$ 的卷烟数量，其中 $x_{ij}$ 为非负整数

### 2.2 约束条件

**单调性约束**（非递增约束）：
对于每个区域 $i$，必须满足：
$$x_{i0} \geq x_{i1} \geq \cdots \geq x_{i(B-1)}$$

即每个区域内，高档位分配值不小于低档位分配值（D30 ≥ D29 ≥ ... ≥ D1）。

**约束重要性**：单调性约束是第一位的，必须严格满足。如果某个操作违反约束，必须强制调整或跳过该操作。

### 2.3 目标函数

**实际投放量** $S$ 计算公式为：
$$S = \sum_{i=0}^{R-1} \sum_{j=0}^{B-1} x_{ij} \cdot c_{ij}$$

**目标**：最小化误差 $|S - T|$，其中 $T$ 为预投放量。

---

## 三、算法详细步骤

### 3.1 阶段1：粗调阶段（候选方案1）

#### 3.1.1 算法描述

从最高档位（HG，索引 0）到最低档位（LG，索引 $B-1$），多轮逐档位列 +1（整列+1，即该档位的所有区域都+1），直到刚好超出目标。

#### 3.1.2 具体步骤

1. **初始化**：
   - 将所有 $x_{ij}$ 初始化为 0
   - 当前实际投放量 $S = 0$

2. **多轮填充**：
   ```python
   while True:
       hasProgress = False
       for j in range(0, B):  # 从HG到LG
           # 计算该档位列+1的增量（所有区域该档位客户数总和）
           gradeIncrement = sum(c[i][j] for i in range(R))
           
           # 如果再加该档位列会超出目标，停止
           if S + gradeIncrement > T:
               return allocationMatrix  # 候选方案1（刚好超出前的状态）
           
           # 整列+1（所有区域该档位都+1）
           for i in range(R):
               x[i][j] += 1
           
           S += gradeIncrement
           hasProgress = True
       
       # 如果一轮循环没有任何进展，退出
       if not hasProgress:
           break
   ```

3. **输出**：候选方案1（粗调结果）

#### 3.1.3 特殊情况处理

- **恰好等于目标**：如果粗调阶段恰好等于目标（$S = T$），直接返回候选方案1，**不生成其他候选方案**。
- **整列+1的天然单调性**：由于整列+1操作（所有区域同时+1），天然满足单调性约束，不会违反约束。

---

### 3.2 阶段2：高档位微调阶段（候选方案2、3、4）

#### 3.2.1 候选方案2的生成

**步骤**：撤销粗调方案最后一次档位列+1操作（整列+1），作为候选方案2。

**具体实现**：
- 从低档位到高档位查找最后一次整列+1操作
- 撤销该操作（该档位的所有区域都-1），确保撤销后不超出目标
- 计算此时的余量：$\text{余量} = T - S_2$，其中 $S_2$ 为候选方案2的实际投放量

#### 3.2.2 迭代微调过程

基于候选方案2进行迭代微调，重复以下步骤直到满足终止条件：

**步骤a**：在候选方案2基础上，从HG开始逐档位列+1（整列+1），直到刚好超出目标。

**步骤b**：撤销最后一次档位列+1操作（整列+1），更新候选方案2为此时方案，计算新余量。

**步骤c**：检查终止条件。

**终止条件**：在某个迭代中，当执行步骤a（从HG开始逐档位列+1）时，HG档位列+1就会超出目标。

即：
$$S_{\text{before}} + \sum_{i=0}^{R-1} c_{i0} > T$$

其中 $S_{\text{before}}$ 为HG档位列+1前的实际投放量，$c_{i0}$ 为区域 $i$ HG档位的客户数。

**特殊情况**：如果HG档位列+1后恰好等于目标（$S_{\text{before}} + \sum_{i=0}^{R-1} c_{i0} = T$），则直接取此时的方案作为候选方案3，不生成候选方案4。

#### 3.2.3 候选方案3的生成

当满足终止条件时：

- **候选方案3**：最后一次迭代后，撤销最后一次+1之前的方案（即刚好超出时的方案）
  $$S_3 = S_{\text{before}} + \sum_{i=0}^{R-1} c_{i0}$$
  $$\text{误差}_3 = |S_3 - T|$$

**注意**：如果HG档位列+1后恰好等于目标，则候选方案3就是此时方案。

#### 3.2.4 候选方案4的生成

**步骤1**：撤销候选方案3最后一次HG列+1操作（整列+1），得到基础方案。

**步骤2**：在基础方案的基础上，选择HG列的某些区域进行+1，使得误差最小。

**选择策略**：

1. **贪心策略**（优先使用）：
   - 按区域客户数从大到小排序（区域HG列客户数多的优先）
   - 依次尝试对每个区域HG列+1，如果+1后误差减小或不变，则保留
   - 如果+1后超出目标，则回溯此次+1操作
   - 如果+1后恰好等于目标，则直接返回（最优方案）

2. **动态规划策略**（当贪心策略误差 ≥ 200时使用）：
   - 问题建模：0-1背包变种问题
   - 状态定义：$dp[i][S]$ = 前 $i$ 个区域中，实际投放量为 $S$ 时是否可达
   - 状态转移：
     $$dp[i][S] = dp[i-1][S] \lor dp[i-1][S - c_{i0}]$$
     其中 $c_{i0}$ 为区域 $i$ HG档位的客户数
   - 目标：找到所有可达的投放量中误差最小的方案
   - 复杂度：$O(R \times |\text{states}|)$，其中 $|\text{states}|$ 为可达状态数量

**具体实现**：

```python
def generateCandidate4(baseMatrix, customerMatrix, targetAmount, segmentOrder):
    """
    生成候选方案4
    
    参数:
        baseMatrix: 基础矩阵（候选方案3撤销最后一次HG列+1后）
        customerMatrix: 客户数矩阵
        targetAmount: 目标预投放量
        segmentOrder: 区域顺序（按HG列客户数从大到小）
    
    返回:
        候选方案4
    """
    # 步骤1：撤销候选方案3最后一次HG列+1操作
    baseMatrix = rollbackLastColumnIncrement(candidate3, customerMatrix, targetAmount)
    
    # 步骤2：选择HG列的某些区域进行+1
    currentAmount = calculateTotalAmount(baseMatrix, customerMatrix)
    remaining = targetAmount - currentAmount
    
    # 先使用贪心策略
    greedyResult = greedyStrategy(baseMatrix, customerMatrix, targetAmount, segmentOrder)
    greedyError = abs(calculateTotalAmount(greedyResult, customerMatrix) - targetAmount)
    
    # 如果贪心策略误差 < 200，直接返回
    if greedyError < 200:
        return greedyResult
    
    # 否则使用动态规划
    return dynamicProgrammingStrategy(baseMatrix, customerMatrix, targetAmount, segmentOrder)
```

**候选方案4的实际投放量和误差**：
$$S_4 = \text{选择后的实际投放量}$$
$$\text{误差}_4 = |S_4 - T|$$

---

### 3.3 阶段3：方案选择

#### 3.3.1 误差计算

对于每个候选方案 $k$（$k = 1, 2, 3, 4$），计算误差：
$$\text{误差}_k = |S_k - T|$$

其中 $S_k$ 为候选方案 $k$ 的实际投放量。

#### 3.3.2 选择规则

1. **选择误差最小的方案**：
   $$\text{最优方案} = \arg\min_{k \in \{1,2,3,4\}} \text{误差}_k$$

2. **相同误差的处理**：如果多个候选方案的误差相同，选择编号更大的候选方案（候选方案4 > 3 > 2 > 1）。

#### 3.3.3 单调性约束强制执行

在最终返回前，**强制执行单调性约束**作为双重保险：

```python
# 对于每个区域，确保不违反单调性约束
for i in range(R):
    for j in range(1, B):
        if x[i][j] > x[i][j-1]:
            x[i][j] = x[i][j-1]  # 强制调整
```

---

## 四、算法流程图

```
开始
  ↓
初始化：x_{ij} = 0, S = 0
  ↓
【阶段1：粗调阶段】
  ↓
从HG到LG逐档位列+1（整列+1）
  ├─ 如果恰好等于目标 → 返回候选方案1
  └─ 如果刚好超出目标 → 停止，得到候选方案1
  ↓
【阶段2：高档位微调阶段】
  ↓
撤销最后一次档位列+1（整列+1）→ 候选方案2
  ↓
迭代微调（重复a、b步骤）
  ├─ a. 从HG开始逐档位列+1（整列+1），直到刚好超出
  ├─ b. 撤销最后一次档位列+1（整列+1）→ 更新候选方案2
  └─ c. 检查终止条件（HG档位列+1是否恰好超出）
  ↓
生成候选方案3（最后一次迭代后，撤销最后一次+1之前的方案）
  ↓
生成候选方案4
  ├─ 步骤1：撤销候选方案3最后一次HG列+1操作（整列+1）
  └─ 步骤2：选择HG列的某些区域进行+1，使得误差最小
      ├─ 贪心策略（误差 < 200）
      └─ 动态规划策略（误差 ≥ 200）
  ↓
【阶段3：方案选择】
  ↓
计算4个候选方案的误差
  ↓
选择误差最小的方案
  ├─ 如果误差相同，选择编号更大的方案
  └─ 强制执行单调性约束
  ↓
返回最终方案
  ↓
结束
```

---

## 五、关键实现细节

### 5.1 撤销最后一次档位列+1操作

**撤销函数** `rollbackLastColumnIncrement(allocationMatrix, customerMatrix, targetAmount)`：

```python
def rollbackLastColumnIncrement(allocationMatrix, customerMatrix, targetAmount):
    """
    撤销导致超出目标的最后一次档位列+1操作（整列+1）
    
    参数:
        allocationMatrix: 当前分配矩阵
        customerMatrix: 客户数矩阵
        targetAmount: 目标预投放量
    
    返回:
        撤销后的分配矩阵
    """
    result = copy(allocationMatrix)
    R = len(allocationMatrix)
    B = len(allocationMatrix[0])
    
    # 从低档位到高档位查找最后一次整列+1操作
    for j in range(B-1, -1, -1):
        # 检查该档位是否所有区域都有分配值
        if all(result[i][j] > 0 for i in range(R)):
            # 尝试撤销整列+1
            for i in range(R):
                result[i][j] -= 1
            
            newS = calculateTotalAmount(result, customerMatrix)
            
            if newS <= targetAmount:
                # 撤销后不超出，返回
                return result
            
            # 撤销后仍然超出，恢复并继续
            for i in range(R):
                result[i][j] += 1
    
    return result
```

### 5.2 贪心策略实现

**贪心函数** `greedyStrategy(baseMatrix, customerMatrix, targetAmount, segmentOrder)`：

```python
def greedyStrategy(baseMatrix, customerMatrix, targetAmount, segmentOrder):
    """
    使用贪心策略选择HG列的某些区域进行+1
    
    参数:
        baseMatrix: 基础矩阵
        customerMatrix: 客户数矩阵
        targetAmount: 目标预投放量
        segmentOrder: 区域顺序（按HG列客户数从大到小）
    
    返回:
        贪心策略结果
    """
    result = copy(baseMatrix)
    currentAmount = calculateTotalAmount(result, customerMatrix)
    
    # 按区域顺序依次尝试+1
    for i in segmentOrder:
        # 检查单调性约束
        if not isValidIncrement(result, i, 0):
            continue
        
        weight = customerMatrix[i][0]
        newAmount = currentAmount + weight
        
        # 如果恰好等于目标，直接返回（最优方案）
        if newAmount == targetAmount:
            result[i][0] += 1
            return result
        
        # 如果超出目标，跳过
        if newAmount > targetAmount:
            continue
        
        # +1操作
        result[i][0] += 1
        currentAmount = newAmount
    
    return result
```

### 5.3 动态规划策略实现

**动态规划函数** `dynamicProgrammingStrategy(baseMatrix, customerMatrix, targetAmount, segmentOrder)`：

```python
def dynamicProgrammingStrategy(baseMatrix, customerMatrix, targetAmount, segmentOrder):
    """
    使用动态规划选择HG列的某些区域进行+1，使得误差最小
    
    参数:
        baseMatrix: 基础矩阵
        customerMatrix: 客户数矩阵
        targetAmount: 目标预投放量
        segmentOrder: 区域顺序（按HG列客户数从大到小）
    
    返回:
        动态规划策略结果
    """
    R = len(segmentOrder)
    currentAmount = calculateTotalAmount(baseMatrix, customerMatrix)
    remaining = targetAmount - currentAmount
    
    # 提取HG列的客户数（权重）
    weights = [customerMatrix[segmentOrder[i]][0] for i in range(R)]
    
    # DP状态：TreeMap<BigDecimal, boolean[]>
    # key: 实际投放量, value: 区域选择方案（boolean数组）
    dp = TreeMap()
    dp[currentAmount] = [False] * R
    
    # 动态规划
    for i in range(R):
        newDp = TreeMap()
        
        for amount, selection in dp.items():
            # 不选择当前区域
            newDp[amount] = copy(selection)
            
            # 选择当前区域
            newAmount = amount + weights[i]
            if newAmount <= targetAmount:
                newSelection = copy(selection)
                newSelection[i] = True
                
                # 如果该投放量已存在，选择误差更小的
                if newAmount not in newDp or abs(newAmount - targetAmount) < abs(newDp[newAmount] - targetAmount):
                    newDp[newAmount] = newSelection
        
        dp = newDp
    
    # 找到误差最小的方案
    bestAmount = currentAmount
    bestError = abs(currentAmount - targetAmount)
    bestSelection = [False] * R
    
    for amount, selection in dp.items():
        error = abs(amount - targetAmount)
        if error < bestError:
            bestError = error
            bestAmount = amount
            bestSelection = selection
    
    # 应用选择方案
    result = copy(baseMatrix)
    for i in range(R):
        if bestSelection[i]:
            segment = segmentOrder[i]
            result[segment][0] += 1
    
    return result
```

---

## 六、算法复杂度分析

### 6.1 时间复杂度

- **粗调阶段**：$O(B \times R)$，其中 $B$ 为档位数量（通常为30），$R$ 为区域数量
- **微调阶段**：$O(B \times I)$，其中 $I$ 为迭代次数（通常为1-5次）
- **候选方案4生成**：
  - 贪心策略：$O(R)$
  - 动态规划策略：$O(R \times |\text{states}|)$，其中 $|\text{states}|$ 为可达状态数量
- **方案选择**：$O(1)$，4个方案比较

**总时间复杂度**：
- 如果使用贪心策略：$O(B \times R)$
- 如果使用动态规划策略：$O(B \times R + R \times |\text{states}|)$

### 6.2 空间复杂度

- **分配矩阵存储**：$O(R \times B)$
- **候选方案存储**：$O(4 \times R \times B) = O(R \times B)$
- **动态规划状态存储**：$O(|\text{states}| \times R)$

**总空间复杂度**：
- 如果使用贪心策略：$O(R \times B)$
- 如果使用动态规划策略：$O(R \times B + |\text{states}| \times R)$

---

## 七、算法特点

### 7.1 优点

1. **误差最小化**：通过生成多个候选方案并选择误差最小的方案，确保结果最优
2. **全局最优**：候选方案4使用动态规划策略，可以找到全局最优解
3. **单调性保证**：严格满足单调性约束，确保分配合理性
4. **高效性**：优先使用贪心策略，只有在误差较大时才使用动态规划
5. **鲁棒性**：处理边界情况（恰好等于目标、整列+1等）

### 7.2 适用场景

- **多区域分配**：有多个投放区域的情况
- **无权重比例**：各区域之间没有权重比例要求
- **精确分配**：需要最小化误差的场景
- **实时计算**：需要快速响应的场景（优先使用贪心策略）

---

## 八、示例

### 8.1 输入数据

- **区域数量**：$R = 3$
- **档位数量**：$B = 30$
- **预投放量**：$T = 100000$
- **客户数矩阵**：$c_{ij}$（3个区域 × 30个档位）

### 8.2 执行过程

1. **粗调阶段**：
   - 从D30开始逐档位列+1（整列+1）
   - 当实际投放量达到100500时，刚好超出目标
   - 候选方案1：实际投放量 = 100500，误差 = 500

2. **微调阶段**：
   - 撤销最后一次档位列+1，得到候选方案2：实际投放量 = 99700，误差 = 300
   - 迭代微调，直到HG档位列+1恰好超出
   - 候选方案3：实际投放量 = 100200，误差 = 200

3. **候选方案4生成**：
   - 撤销候选方案3最后一次HG列+1操作
   - 使用贪心策略选择HG列的某些区域进行+1
   - 如果贪心策略误差 < 200，直接返回；否则使用动态规划
   - 候选方案4：实际投放量 = 99950，误差 = 50

4. **方案选择**：
   - 候选方案4误差最小（50），选择候选方案4

### 8.3 输出结果

- **最终方案**：候选方案4
- **实际投放量**：99950
- **误差**：50
- **分配矩阵**：满足单调性约束的3个区域 × 30个档位分配值

---

## 九、注意事项

1. **单调性约束**：约束条件重要性是第一位的，必须严格满足
2. **整列+1操作**：粗调和微调阶段使用整列+1操作，天然满足单调性约束
3. **恰好等于目标**：如果粗调阶段恰好等于目标，直接返回，不生成其他候选方案
4. **双重保险**：在最终返回前强制执行单调性约束，确保结果正确
5. **贪心策略优先**：优先使用贪心策略，只有在误差 ≥ 200时才使用动态规划，平衡效率和精度

---

**文档版本**：v1.0  
**最后更新**：2025年1月

