-- ============================================
-- 迁移脚本：将现有固定标签字段迁移到DYNAMIC_TAGS JSON字段
-- 用途：阶段2 - 将QUALITY_DATA_SHARE等固定标签字段的数据迁移到JSON字段
-- 执行时间：阶段2 - 双写阶段
-- 注意：执行前请备份数据库，建议在低峰期执行
-- 
-- ⚠️ 重要更新（2025-12-20）：
-- 根据新的业务规则，QUALITY_DATA_SHARE等固定标签字段应该继续使用固定字段存储，
-- 不写入JSON字段。因此，此迁移脚本已不再需要。
-- 
-- 如果之前已经执行过此脚本，导致QUALITY_DATA_SHARE被错误地写入了JSON字段，
-- 请使用清理脚本 remove-quality-data-share-from-json.sql 来清理这些数据。
-- ============================================

-- 1. 为base_customer_info表迁移数据
-- 将QUALITY_DATA_SHARE字段的值迁移到DYNAMIC_TAGS JSON字段
UPDATE base_customer_info
SET DYNAMIC_TAGS = JSON_OBJECT(
    'quality_data_share', 
    CASE 
        WHEN QUALITY_DATA_SHARE IS NOT NULL AND QUALITY_DATA_SHARE != '' 
        THEN QUALITY_DATA_SHARE 
        ELSE NULL 
    END
)
WHERE (DYNAMIC_TAGS IS NULL OR DYNAMIC_TAGS = '{}')
  AND QUALITY_DATA_SHARE IS NOT NULL 
  AND QUALITY_DATA_SHARE != '';

-- 2. 如果DYNAMIC_TAGS已存在，则合并数据（保留现有标签，添加quality_data_share）
UPDATE base_customer_info
SET DYNAMIC_TAGS = JSON_SET(
    DYNAMIC_TAGS,
    '$.quality_data_share',
    CASE 
        WHEN QUALITY_DATA_SHARE IS NOT NULL AND QUALITY_DATA_SHARE != '' 
        THEN QUALITY_DATA_SHARE 
        ELSE NULL 
    END
)
WHERE DYNAMIC_TAGS IS NOT NULL 
  AND DYNAMIC_TAGS != '{}'
  AND (JSON_EXTRACT(DYNAMIC_TAGS, '$.quality_data_share') IS NULL 
       OR JSON_EXTRACT(DYNAMIC_TAGS, '$.quality_data_share') = 'null')
  AND QUALITY_DATA_SHARE IS NOT NULL 
  AND QUALITY_DATA_SHARE != '';

-- 3. 验证迁移结果
-- 统计迁移的数据量
SELECT 
    COUNT(*) AS total_customers,
    COUNT(QUALITY_DATA_SHARE) AS has_quality_data_share,
    COUNT(JSON_EXTRACT(DYNAMIC_TAGS, '$.quality_data_share')) AS has_json_quality_data_share,
    COUNT(CASE 
        WHEN QUALITY_DATA_SHARE IS NOT NULL 
             AND QUALITY_DATA_SHARE != ''
             AND JSON_EXTRACT(DYNAMIC_TAGS, '$.quality_data_share') IS NOT NULL
        THEN 1 
    END) AS migrated_count
FROM base_customer_info;

-- 4. 检查数据一致性（QUALITY_DATA_SHARE与JSON字段的值应该一致）
SELECT 
    CUST_CODE,
    QUALITY_DATA_SHARE,
    JSON_EXTRACT(DYNAMIC_TAGS, '$.quality_data_share') AS json_quality_data_share,
    CASE 
        WHEN QUALITY_DATA_SHARE = JSON_UNQUOTE(JSON_EXTRACT(DYNAMIC_TAGS, '$.quality_data_share'))
             OR (QUALITY_DATA_SHARE IS NULL AND JSON_EXTRACT(DYNAMIC_TAGS, '$.quality_data_share') IS NULL)
        THEN '一致'
        ELSE '不一致'
    END AS consistency_status
FROM base_customer_info
WHERE QUALITY_DATA_SHARE IS NOT NULL 
   OR JSON_EXTRACT(DYNAMIC_TAGS, '$.quality_data_share') IS NOT NULL
LIMIT 100;

-- 5. 查找不一致的数据（用于排查问题）
SELECT 
    CUST_CODE,
    QUALITY_DATA_SHARE,
    JSON_EXTRACT(DYNAMIC_TAGS, '$.quality_data_share') AS json_quality_data_share
FROM base_customer_info
WHERE (QUALITY_DATA_SHARE IS NOT NULL AND QUALITY_DATA_SHARE != '')
  AND (
      JSON_EXTRACT(DYNAMIC_TAGS, '$.quality_data_share') IS NULL
      OR JSON_EXTRACT(DYNAMIC_TAGS, '$.quality_data_share') = 'null'
      OR QUALITY_DATA_SHARE != JSON_UNQUOTE(JSON_EXTRACT(DYNAMIC_TAGS, '$.quality_data_share'))
  )
LIMIT 50;

-- 注意：
-- 1. 此脚本是幂等的，可以安全地多次执行
-- 2. 只会迁移非空值
-- 3. 如果DYNAMIC_TAGS已存在，会合并而不是覆盖
-- 4. 执行后请检查验证查询的结果，确保数据一致性

