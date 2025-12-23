# 算法分配功能演示测试命令

## 前置条件
1. 启动应用：`mvn spring-boot:run`
2. 确保数据库中已导入 2025/9/3 和 2025/9/4 的卷烟投放信息数据
3. 应用端口为 **28080**

---

## 0. 全链路集成测试（推荐先运行）

### Week3 全链路测试
```bash
mvn test -Dtest=Week3FullPipelineTest -q
```

### Week4 全链路测试
```bash
mvn test -Dtest=Week4FullPipelineTest -q
```

### 同时运行 Week3 和 Week4 测试
```bash
mvn test -Dtest=Week3FullPipelineTest,Week4FullPipelineTest -q
```

> 全链路测试会自动完成：数据准备 → 客户筛选 → 区域统计构建 → 分配执行 → 结果验证 → 误差分析

---

## 1. 一键生成分配方案

### 2025年9月第3周（标准分配：按档位投放等）
```bash
curl -X POST http://localhost:28080/api/calculate/generate-distribution-plan \
  -H "Content-Type: application/json" \
  -d '{
    "year": 2025,
    "month": 9,
    "weekSeq": 3,
    "urbanRatio": 0.6,
    "ruralRatio": 0.4
  }' | jq
```

### 2025年9月第4周（价位段自选投放）
> 注意：Week4 数据全部为"按价位段自选投放"类型，分配结果写入 `cigarette_distribution_prediction_price` 表

```bash
curl -X POST http://localhost:28080/api/calculate/generate-distribution-plan \
  -H "Content-Type: application/json" \
  -d '{
    "year": 2025,
    "month": 9,
    "weekSeq": 4,
    "urbanRatio": 0.6,
    "ruralRatio": 0.4
  }' | jq
```

### 查询 Week4 价位段分配结果
```bash
# 查询价位段分配记录数和总投放量
mysql -uroot -p'LuvuubyRK*Jason1258' marketing -e "SELECT COUNT(*) as count, SUM(ACTUAL_DELIVERY) as total_delivery FROM cigarette_distribution_prediction_price WHERE YEAR = 2025 AND MONTH = 9 AND WEEK_SEQ = 4;"
```

---

## 2. 查询总实际投放量（验证分配结果）

### 2025年9月第3周
```bash
curl -X POST "http://localhost:28080/api/calculate/total-actual-delivery?year=2025&month=9&weekSeq=3" | jq
```

### 2025年9月第4周
```bash
curl -X POST "http://localhost:28080/api/calculate/total-actual-delivery?year=2025&month=9&weekSeq=4" | jq
```

---

## 3. 查询分配结果详情

### 查询预测表数据（2025/9/3）
```bash
curl -X GET "http://localhost:28080/api/prediction/query?year=2025&month=9&weekSeq=3" | jq
```

### 查询预测表数据（2025/9/4）
```bash
curl -X GET "http://localhost:28080/api/prediction/query?year=2025&month=9&weekSeq=4" | jq
```

---

## 4. 查询价位段订购量上限

### 2025年9月第3周
```bash
curl -X GET "http://localhost:28080/api/statistics/price-band-order-limits?year=2025&month=9&weekSeq=3" | jq
```

### 2025年9月第4周
```bash
curl -X GET "http://localhost:28080/api/statistics/price-band-order-limits?year=2025&month=9&weekSeq=4" | jq
```

---

## 5. 查询投放组合与区域映射

### 2025年9月第3周
```bash
curl -X GET "http://localhost:28080/api/statistics/delivery-combination-regions?year=2025&month=9&weekSeq=3" | jq
```

### 2025年9月第4周
```bash
curl -X GET "http://localhost:28080/api/statistics/delivery-combination-regions?year=2025&month=9&weekSeq=4" | jq
```

---

## 6. 调整单支卷烟投放策略（示例）

```bash
curl -X POST http://localhost:28080/api/calculate/adjust-strategy \
  -H "Content-Type: application/json" \
  -d '{
    "year": 2025,
    "month": 9,
    "weekSeq": 3,
    "cigCode": "42010020",
    "cigName": "红金龙(硬神州腾龙)",
    "newDeliveryMethod": "按档位投放",
    "newDeliveryEtype": null,
    "newTag": null,
    "newTagFilterValue": null,
    "newAdvAmount": 5000
  }' | jq
```

---

## 7. 数据库直接查询（可选）

### 查看 Week3 标准分配结果统计
```sql
-- 2025/9/3 标准分配结果统计
SELECT DELIVERY_METHOD, COUNT(*) as count, SUM(ACTUAL_DELIVERY) as total_delivery
FROM cigarette_distribution_prediction
WHERE YEAR = 2025 AND MONTH = 9 AND WEEK_SEQ = 3
GROUP BY DELIVERY_METHOD;
```

### 查看 Week4 价位段分配结果统计
```sql
-- 2025/9/4 价位段分配结果统计（数据在 prediction_price 表）
SELECT COUNT(*) as count, SUM(ACTUAL_DELIVERY) as total_delivery
FROM cigarette_distribution_prediction_price
WHERE YEAR = 2025 AND MONTH = 9 AND WEEK_SEQ = 4;

-- 查看具体卷烟分配详情
SELECT CIG_CODE, CIG_NAME, DELIVERY_AREA, ACTUAL_DELIVERY
FROM cigarette_distribution_prediction_price
WHERE YEAR = 2025 AND MONTH = 9 AND WEEK_SEQ = 4
ORDER BY CIG_CODE
LIMIT 10;
```

### 查看价位段分配结果
```sql
-- 按价位段自选投放结果
SELECT CIG_CODE, CIG_NAME, DELIVERY_AREA, ACTUAL_DELIVERY
FROM cigarette_distribution_prediction_price
WHERE YEAR = 2025 AND MONTH = 9 AND WEEK_SEQ = 3
ORDER BY CIG_CODE;
```

---

## 注意事项
1. 如果没有安装 `jq`，可以去掉命令末尾的 `| jq`
2. 应用端口为 28080
3. 首次运行分配前，需要先导入客户基础信息和卷烟投放信息
4. 推荐先运行全链路测试（第0节），会自动准备数据并验证结果
5. **Week3 数据类型**：按档位投放、按档位扩展投放等（标准分配）→ 结果在 `cigarette_distribution_prediction` 表
6. **Week4 数据类型**：按价位段自选投放 → 结果在 `cigarette_distribution_prediction_price` 表
