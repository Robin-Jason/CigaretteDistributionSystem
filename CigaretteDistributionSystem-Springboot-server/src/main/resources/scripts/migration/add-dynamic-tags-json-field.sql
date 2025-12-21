-- ============================================
-- 迁移脚本：添加动态标签JSON字段
-- 用途：为base_customer_info和customer_filter表添加DYNAMIC_TAGS JSON字段
-- 执行时间：阶段1 - 准备阶段
-- 注意：执行前请备份数据库
-- ============================================

-- 1. 为 base_customer_info 表添加 DYNAMIC_TAGS JSON字段
ALTER TABLE base_customer_info 
ADD COLUMN DYNAMIC_TAGS JSON DEFAULT NULL COMMENT '动态标签（JSON格式，存储所有动态标签键值对）';

-- 2. 为 customer_filter 表添加 DYNAMIC_TAGS JSON字段
ALTER TABLE customer_filter 
ADD COLUMN DYNAMIC_TAGS JSON DEFAULT NULL COMMENT '动态标签（JSON格式，从base_customer_info同步）';

-- 3. 创建JSON索引（可选，提高查询性能）
-- 为常用标签创建虚拟列和索引
-- 注意：根据实际使用的标签动态创建，这里仅作示例

-- 示例：为premium_customer标签创建索引
-- ALTER TABLE base_customer_info 
-- ADD COLUMN premium_customer_virtual VARCHAR(20) 
-- GENERATED ALWAYS AS (CAST(JSON_EXTRACT(DYNAMIC_TAGS, '$.premium_customer') AS CHAR(20))) VIRTUAL,
-- ADD INDEX idx_premium_customer_virtual (premium_customer_virtual);

-- 4. 验证字段添加成功
SELECT 
    TABLE_NAME,
    COLUMN_NAME,
    DATA_TYPE,
    COLUMN_TYPE,
    COLUMN_COMMENT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME IN ('base_customer_info', 'customer_filter')
  AND COLUMN_NAME = 'DYNAMIC_TAGS';

