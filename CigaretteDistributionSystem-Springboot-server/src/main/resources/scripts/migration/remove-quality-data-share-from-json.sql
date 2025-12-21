-- ============================================
-- 清理脚本：从DYNAMIC_TAGS JSON字段中移除quality_data_share键
-- 用途：清理之前错误迁移的QUALITY_DATA_SHARE数据（根据新业务规则，固定标签字段不应写入JSON）
-- 执行时间：阶段3之后
-- 注意：执行前请备份数据库，建议在低峰期执行
-- ============================================

-- 1. 检查有多少记录包含quality_data_share键
SELECT 
    COUNT(*) AS total_with_quality_data_share_in_json,
    COUNT(CASE WHEN JSON_EXTRACT(DYNAMIC_TAGS, '$.quality_data_share') IS NOT NULL THEN 1 END) AS has_quality_data_share
FROM base_customer_info
WHERE DYNAMIC_TAGS IS NOT NULL 
  AND DYNAMIC_TAGS != '{}'
  AND JSON_EXTRACT(DYNAMIC_TAGS, '$.quality_data_share') IS NOT NULL;

-- 2. 从base_customer_info表的DYNAMIC_TAGS中移除quality_data_share键
UPDATE base_customer_info
SET DYNAMIC_TAGS = JSON_REMOVE(DYNAMIC_TAGS, '$.quality_data_share')
WHERE DYNAMIC_TAGS IS NOT NULL 
  AND DYNAMIC_TAGS != '{}'
  AND JSON_EXTRACT(DYNAMIC_TAGS, '$.quality_data_share') IS NOT NULL;

-- 3. 从customer_filter表的DYNAMIC_TAGS中移除quality_data_share键
UPDATE customer_filter
SET DYNAMIC_TAGS = JSON_REMOVE(DYNAMIC_TAGS, '$.quality_data_share')
WHERE DYNAMIC_TAGS IS NOT NULL 
  AND DYNAMIC_TAGS != '{}'
  AND JSON_EXTRACT(DYNAMIC_TAGS, '$.quality_data_share') IS NOT NULL;

-- 4. 验证清理结果
SELECT 
    COUNT(*) AS total_customers,
    COUNT(CASE WHEN JSON_EXTRACT(DYNAMIC_TAGS, '$.quality_data_share') IS NOT NULL THEN 1 END) AS still_has_quality_data_share
FROM base_customer_info
WHERE DYNAMIC_TAGS IS NOT NULL 
  AND DYNAMIC_TAGS != '{}';

-- 5. 检查customer_filter表的清理结果
SELECT 
    COUNT(*) AS total_records,
    COUNT(CASE WHEN JSON_EXTRACT(DYNAMIC_TAGS, '$.quality_data_share') IS NOT NULL THEN 1 END) AS still_has_quality_data_share
FROM customer_filter
WHERE DYNAMIC_TAGS IS NOT NULL 
  AND DYNAMIC_TAGS != '{}';

-- 注意：
-- 1. 此脚本会从JSON字段中移除quality_data_share键，但不会影响QUALITY_DATA_SHARE固定字段
-- 2. 如果DYNAMIC_TAGS中只有quality_data_share一个键，移除后DYNAMIC_TAGS将变为空对象{}
-- 3. 执行后请检查验证查询的结果，确保清理成功
-- 4. 此脚本是幂等的，可以安全地多次执行

