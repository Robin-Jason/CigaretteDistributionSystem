-- 查询这3个卷烟的配置信息
SELECT 
    CIG_CODE, 
    CIG_NAME, 
    HG, 
    LG, 
    ADV, 
    DELIVERY_METHOD,
    DELIVERY_AREA
FROM cigarette_distribution_info 
WHERE YEAR = 2025 
  AND MONTH = 9 
  AND WEEK_SEQ = 3 
  AND CIG_NAME IN ('娇子(格调细支)', '红金龙(硬神州腾龙)', '利群(长嘴)')
ORDER BY CIG_NAME;
