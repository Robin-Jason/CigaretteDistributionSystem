-- ============================================
-- 为2099/9/1分区的测试卷烟添加批发价数据
-- 用途：支持按价位段自选投放的分配写回功能测试
-- ============================================

-- 清理该分区的测试卷烟价格数据（如果存在）
DELETE FROM base_cigarette_price 
WHERE CIG_CODE LIKE 'TEST_%' 
  AND CIG_CODE IN (
    SELECT DISTINCT CIG_CODE 
    FROM cigarette_distribution_info 
    WHERE YEAR = 2099 AND MONTH = 9 AND WEEK_SEQ = 1 
      AND DELIVERY_METHOD = '按价位段自选投放'
  );

-- 为按价位段自选投放的测试卷烟添加批发价数据
-- 批发价分布在不同价位段，以便测试分配功能
INSERT INTO base_cigarette_price (CIG_CODE, CIG_NAME, WHOLESALE_PRICE, CATEGORY, PRICE_TIER, PRICE_HL)
SELECT 
    i.CIG_CODE,
    i.CIG_NAME,
    -- 根据CIG_CODE生成不同的批发价，分布在各个价位段
    CASE 
        -- 第1段：>= 600
        WHEN i.CIG_CODE LIKE '%641' THEN 650.00
        WHEN i.CIG_CODE LIKE '%642' THEN 700.00
        WHEN i.CIG_CODE LIKE '%643' THEN 800.00
        WHEN i.CIG_CODE LIKE '%644' THEN 900.00
        WHEN i.CIG_CODE LIKE '%645' THEN 1000.00
        
        -- 第2段：400-600
        WHEN i.CIG_CODE LIKE '%646' THEN 450.00
        WHEN i.CIG_CODE LIKE '%647' THEN 500.00
        WHEN i.CIG_CODE LIKE '%648' THEN 550.00
        WHEN i.CIG_CODE LIKE '%649' THEN 580.00
        WHEN i.CIG_CODE LIKE '%650' THEN 590.00
        
        -- 第3段：290-400
        WHEN i.CIG_CODE LIKE '%651' THEN 300.00
        WHEN i.CIG_CODE LIKE '%652' THEN 320.00
        WHEN i.CIG_CODE LIKE '%653' THEN 350.00
        WHEN i.CIG_CODE LIKE '%654' THEN 380.00
        WHEN i.CIG_CODE LIKE '%655' THEN 390.00
        
        -- 第4段：263-290
        WHEN i.CIG_CODE LIKE '%656' THEN 270.00
        WHEN i.CIG_CODE LIKE '%657' THEN 275.00
        WHEN i.CIG_CODE LIKE '%658' THEN 280.00
        WHEN i.CIG_CODE LIKE '%659' THEN 285.00
        WHEN i.CIG_CODE LIKE '%660' THEN 289.00
        
        -- 第5段：220-263
        WHEN i.CIG_CODE LIKE '%661' THEN 230.00
        WHEN i.CIG_CODE LIKE '%662' THEN 240.00
        WHEN i.CIG_CODE LIKE '%663' THEN 250.00
        WHEN i.CIG_CODE LIKE '%664' THEN 255.00
        WHEN i.CIG_CODE LIKE '%665' THEN 260.00
        
        -- 第6段：180-220
        WHEN i.CIG_CODE LIKE '%666' THEN 190.00
        WHEN i.CIG_CODE LIKE '%667' THEN 200.00
        WHEN i.CIG_CODE LIKE '%668' THEN 210.00
        WHEN i.CIG_CODE LIKE '%669' THEN 215.00
        WHEN i.CIG_CODE LIKE '%670' THEN 219.00
        
        -- 第7段：158-180
        WHEN i.CIG_CODE LIKE '%671' THEN 160.00
        WHEN i.CIG_CODE LIKE '%672' THEN 165.00
        WHEN i.CIG_CODE LIKE '%673' THEN 170.00
        WHEN i.CIG_CODE LIKE '%674' THEN 175.00
        WHEN i.CIG_CODE LIKE '%675' THEN 179.00
        
        -- 第8段：130-158
        WHEN i.CIG_CODE LIKE '%676' THEN 140.00
        WHEN i.CIG_CODE LIKE '%677' THEN 145.00
        WHEN i.CIG_CODE LIKE '%678' THEN 150.00
        WHEN i.CIG_CODE LIKE '%679' THEN 155.00
        WHEN i.CIG_CODE LIKE '%680' THEN 157.00
        
        -- 第9段：109-130
        WHEN i.CIG_CODE LIKE '%681' THEN 115.00
        WHEN i.CIG_CODE LIKE '%682' THEN 120.00
        WHEN i.CIG_CODE LIKE '%683' THEN 125.00
        WHEN i.CIG_CODE LIKE '%684' THEN 128.00
        WHEN i.CIG_CODE LIKE '%685' THEN 129.00
        
        -- 有标签的卷烟也分配不同价位段
        WHEN i.CIG_CODE LIKE '%682' THEN 650.00  -- 第1段
        WHEN i.CIG_CODE LIKE '%683' THEN 500.00  -- 第2段
        WHEN i.CIG_CODE LIKE '%684' THEN 350.00  -- 第3段
        WHEN i.CIG_CODE LIKE '%685' THEN 270.00  -- 第4段
        WHEN i.CIG_CODE LIKE '%686' THEN 240.00  -- 第5段
        WHEN i.CIG_CODE LIKE '%687' THEN 200.00  -- 第6段
        WHEN i.CIG_CODE LIKE '%688' THEN 170.00  -- 第7段
        WHEN i.CIG_CODE LIKE '%689' THEN 150.00  -- 第8段
        WHEN i.CIG_CODE LIKE '%690' THEN 120.00  -- 第9段
        
        -- 默认值：第1段
        ELSE 650.00
    END AS WHOLESALE_PRICE,
    '测试价类' AS CATEGORY,
    '测试价档' AS PRICE_TIER,
    1 AS PRICE_HL
FROM cigarette_distribution_info i
WHERE i.YEAR = 2099 
  AND i.MONTH = 9 
  AND i.WEEK_SEQ = 1
  AND i.DELIVERY_METHOD = '按价位段自选投放'
  AND NOT EXISTS (
    SELECT 1 
    FROM base_cigarette_price p 
    WHERE p.CIG_CODE = i.CIG_CODE
  );

-- 验证插入的数据
SELECT 
    p.CIG_CODE,
    p.CIG_NAME,
    p.WHOLESALE_PRICE,
    CASE 
        WHEN p.WHOLESALE_PRICE >= 600 THEN '第1段'
        WHEN p.WHOLESALE_PRICE >= 400 THEN '第2段'
        WHEN p.WHOLESALE_PRICE >= 290 THEN '第3段'
        WHEN p.WHOLESALE_PRICE >= 263 THEN '第4段'
        WHEN p.WHOLESALE_PRICE >= 220 THEN '第5段'
        WHEN p.WHOLESALE_PRICE >= 180 THEN '第6段'
        WHEN p.WHOLESALE_PRICE >= 158 THEN '第7段'
        WHEN p.WHOLESALE_PRICE >= 130 THEN '第8段'
        WHEN p.WHOLESALE_PRICE >= 109 THEN '第9段'
        ELSE '未匹配'
    END AS PRICE_BAND
FROM base_cigarette_price p
WHERE p.CIG_CODE LIKE 'TEST_%'
  AND p.CIG_CODE IN (
    SELECT DISTINCT CIG_CODE 
    FROM cigarette_distribution_info 
    WHERE YEAR = 2099 AND MONTH = 9 AND WEEK_SEQ = 1 
      AND DELIVERY_METHOD = '按价位段自选投放'
  )
ORDER BY p.WHOLESALE_PRICE DESC
LIMIT 20;

