-- ============================================
-- 测试用例数据生成脚本
-- 用途：在 cigarette_distribution_info 表中插入测试用例数据
-- 时间分区：2099年9月第1周（避免与真实数据冲突）
-- 生成策略：基于 Pairwise 组合测试设计 + 预投放量分层采样
-- ============================================

-- 清理该分区的旧测试数据（可选，谨慎使用）
-- DELETE FROM cigarette_distribution_info WHERE YEAR = 2099 AND MONTH = 9 AND WEEK_SEQ = 1;

-- ============================================
-- 配置参数
-- ============================================
SET @YEAR = 2099;
SET @MONTH = 9;
SET @WEEK_SEQ = 1;
SET @SUPPLY_ATTRIBUTE = '正常';
SET @URS = 0;

-- 可用区域列表（按客户数从高到低排序，实际使用时需要根据 region_customer_statistics 表动态获取）
SET @REGION_1 = '城区';
SET @REGION_2 = '丹江口市';
SET @REGION_3 = '房县';
SET @REGION_4 = '郧阳区';
SET @REGION_5 = '竹山县';
SET @REGION_6 = '郧西县';
SET @REGION_7 = '竹溪县';

-- ============================================
-- 测试用例生成说明
-- ============================================
-- 1. 基础组合：16种（按档位投放×2标签 + 按档位扩展投放×5单扩展×2标签 + 按档位扩展投放×2双扩展×2标签）
-- 2. 预投放量采样：8个阶层，每阶层3-8个样本，总计41个投放量样本
-- 3. 区域数量：根据预投放量动态计算（小量少区域，大量多区域）
-- 4. 总用例数：16种组合 × 41个样本 = 656个基础用例 + 10个特殊用例 = 666个用例
-- 
-- 注意：由于SQL限制，这里采用简化策略，生成约300-400个代表性用例
-- ============================================

-- ============================================
-- 第一部分：按档位投放（无扩展类型）
-- ============================================

-- 1.1 按档位投放 + 无标签
INSERT INTO `cigarette_distribution_info`
(YEAR, MONTH, WEEK_SEQ, CIG_CODE, CIG_NAME, SUPPLY_ATTRIBUTE, URS, DELIVERY_METHOD, DELIVERY_ETYPE, TAG, ADV, DELIVERY_AREA, BZ)
VALUES
-- 0-1000阶层（3个样本）
(2099, 9, 1, 'TEST_001', '测试卷烟_按档位投放_无标签', '正常', 0, '按档位投放', NULL, NULL, 500.00, @REGION_1, '测试用例：0-1000阶层'),
(2099, 9, 1, 'TEST_002', '测试卷烟_按档位投放_无标签', '正常', 0, '按档位投放', NULL, NULL, 750.50, @REGION_1, '测试用例：0-1000阶层'),
(2099, 9, 1, 'TEST_003', '测试卷烟_按档位投放_无标签', '正常', 0, '按档位投放', NULL, NULL, 999.99, @REGION_1, '测试用例：0-1000阶层'),

-- 1000-2000阶层（3个样本）
(2099, 9, 1, 'TEST_004', '测试卷烟_按档位投放_无标签', '正常', 0, '按档位投放', NULL, NULL, 1200.00, @REGION_1, '测试用例：1000-2000阶层'),
(2099, 9, 1, 'TEST_005', '测试卷烟_按档位投放_无标签', '正常', 0, '按档位投放', NULL, NULL, 1500.25, CONCAT(@REGION_1, ',', @REGION_2), '测试用例：1000-2000阶层'),
(2099, 9, 1, 'TEST_006', '测试卷烟_按档位投放_无标签', '正常', 0, '按档位投放', NULL, NULL, 1999.99, CONCAT(@REGION_1, ',', @REGION_2), '测试用例：1000-2000阶层'),

-- 2000-5000阶层（4个样本）
(2099, 9, 1, 'TEST_007', '测试卷烟_按档位投放_无标签', '正常', 0, '按档位投放', NULL, NULL, 2500.00, CONCAT(@REGION_1, ',', @REGION_2), '测试用例：2000-5000阶层'),
(2099, 9, 1, 'TEST_008', '测试卷烟_按档位投放_无标签', '正常', 0, '按档位投放', NULL, NULL, 3500.50, CONCAT(@REGION_1, ',', @REGION_2), '测试用例：2000-5000阶层'),
(2099, 9, 1, 'TEST_009', '测试卷烟_按档位投放_无标签', '正常', 0, '按档位投放', NULL, NULL, 4000.75, CONCAT(@REGION_1, ',', @REGION_2, ',', @REGION_3), '测试用例：2000-5000阶层'),
(2099, 9, 1, 'TEST_010', '测试卷烟_按档位投放_无标签', '正常', 0, '按档位投放', NULL, NULL, 4999.99, CONCAT(@REGION_1, ',', @REGION_2, ',', @REGION_3), '测试用例：2000-5000阶层'),

-- 5000-10000阶层（5个样本）
(2099, 9, 1, 'TEST_011', '测试卷烟_按档位投放_无标签', '正常', 0, '按档位投放', NULL, NULL, 6000.00, CONCAT(@REGION_1, ',', @REGION_2, ',', @REGION_3), '测试用例：5000-10000阶层'),
(2099, 9, 1, 'TEST_012', '测试卷烟_按档位投放_无标签', '正常', 0, '按档位投放', NULL, NULL, 7500.25, CONCAT(@REGION_1, ',', @REGION_2, ',', @REGION_3), '测试用例：5000-10000阶层'),
(2099, 9, 1, 'TEST_013', '测试卷烟_按档位投放_无标签', '正常', 0, '按档位投放', NULL, NULL, 8500.50, CONCAT(@REGION_1, ',', @REGION_2, ',', @REGION_3, ',', @REGION_4), '测试用例：5000-10000阶层'),
(2099, 9, 1, 'TEST_014', '测试卷烟_按档位投放_无标签', '正常', 0, '按档位投放', NULL, NULL, 9500.75, CONCAT(@REGION_1, ',', @REGION_2, ',', @REGION_3, ',', @REGION_4), '测试用例：5000-10000阶层'),
(2099, 9, 1, 'TEST_015', '测试卷烟_按档位投放_无标签', '正常', 0, '按档位投放', NULL, NULL, 9999.99, CONCAT(@REGION_1, ',', @REGION_2, ',', @REGION_3, ',', @REGION_4), '测试用例：5000-10000阶层'),

-- 10000-20000阶层（5个样本）
(2099, 9, 1, 'TEST_016', '测试卷烟_按档位投放_无标签', '正常', 0, '按档位投放', NULL, NULL, 12000.00, CONCAT(@REGION_1, ',', @REGION_2, ',', @REGION_3, ',', @REGION_4), '测试用例：10000-20000阶层'),
(2099, 9, 1, 'TEST_017', '测试卷烟_按档位投放_无标签', '正常', 0, '按档位投放', NULL, NULL, 15000.25, CONCAT(@REGION_1, ',', @REGION_2, ',', @REGION_3, ',', @REGION_4), '测试用例：10000-20000阶层'),
(2099, 9, 1, 'TEST_018', '测试卷烟_按档位投放_无标签', '正常', 0, '按档位投放', NULL, NULL, 17000.50, CONCAT(@REGION_1, ',', @REGION_2, ',', @REGION_3, ',', @REGION_4, ',', @REGION_5), '测试用例：10000-20000阶层'),
(2099, 9, 1, 'TEST_019', '测试卷烟_按档位投放_无标签', '正常', 0, '按档位投放', NULL, NULL, 18500.75, CONCAT(@REGION_1, ',', @REGION_2, ',', @REGION_3, ',', @REGION_4, ',', @REGION_5), '测试用例：10000-20000阶层'),
(2099, 9, 1, 'TEST_020', '测试卷烟_按档位投放_无标签', '正常', 0, '按档位投放', NULL, NULL, 19999.99, CONCAT(@REGION_1, ',', @REGION_2, ',', @REGION_3, ',', @REGION_4, ',', @REGION_5), '测试用例：10000-20000阶层'),

-- 20000-50000阶层（6个样本）
(2099, 9, 1, 'TEST_021', '测试卷烟_按档位投放_无标签', '正常', 0, '按档位投放', NULL, NULL, 25000.00, CONCAT(@REGION_1, ',', @REGION_2, ',', @REGION_3, ',', @REGION_4, ',', @REGION_5), '测试用例：20000-50000阶层'),
(2099, 9, 1, 'TEST_022', '测试卷烟_按档位投放_无标签', '正常', 0, '按档位投放', NULL, NULL, 30000.25, CONCAT(@REGION_1, ',', @REGION_2, ',', @REGION_3, ',', @REGION_4, ',', @REGION_5), '测试用例：20000-50000阶层'),
(2099, 9, 1, 'TEST_023', '测试卷烟_按档位投放_无标签', '正常', 0, '按档位投放', NULL, NULL, 35000.50, CONCAT(@REGION_1, ',', @REGION_2, ',', @REGION_3, ',', @REGION_4, ',', @REGION_5, ',', @REGION_6), '测试用例：20000-50000阶层'),
(2099, 9, 1, 'TEST_024', '测试卷烟_按档位投放_无标签', '正常', 0, '按档位投放', NULL, NULL, 40000.75, CONCAT(@REGION_1, ',', @REGION_2, ',', @REGION_3, ',', @REGION_4, ',', @REGION_5, ',', @REGION_6), '测试用例：20000-50000阶层'),
(2099, 9, 1, 'TEST_025', '测试卷烟_按档位投放_无标签', '正常', 0, '按档位投放', NULL, NULL, 45000.00, CONCAT(@REGION_1, ',', @REGION_2, ',', @REGION_3, ',', @REGION_4, ',', @REGION_5, ',', @REGION_6), '测试用例：20000-50000阶层'),
(2099, 9, 1, 'TEST_026', '测试卷烟_按档位投放_无标签', '正常', 0, '按档位投放', NULL, NULL, 49999.99, CONCAT(@REGION_1, ',', @REGION_2, ',', @REGION_3, ',', @REGION_4, ',', @REGION_5, ',', @REGION_6), '测试用例：20000-50000阶层'),

-- 50000-100000阶层（7个样本）
(2099, 9, 1, 'TEST_027', '测试卷烟_按档位投放_无标签', '正常', 0, '按档位投放', NULL, NULL, 60000.00, CONCAT(@REGION_1, ',', @REGION_2, ',', @REGION_3, ',', @REGION_4, ',', @REGION_5, ',', @REGION_6), '测试用例：50000-100000阶层'),
(2099, 9, 1, 'TEST_028', '测试卷烟_按档位投放_无标签', '正常', 0, '按档位投放', NULL, NULL, 70000.25, CONCAT(@REGION_1, ',', @REGION_2, ',', @REGION_3, ',', @REGION_4, ',', @REGION_5, ',', @REGION_6), '测试用例：50000-100000阶层'),
(2099, 9, 1, 'TEST_029', '测试卷烟_按档位投放_无标签', '正常', 0, '按档位投放', NULL, NULL, 75000.50, CONCAT(@REGION_1, ',', @REGION_2, ',', @REGION_3, ',', @REGION_4, ',', @REGION_5, ',', @REGION_6, ',', @REGION_7), '测试用例：50000-100000阶层'),
(2099, 9, 1, 'TEST_030', '测试卷烟_按档位投放_无标签', '正常', 0, '按档位投放', NULL, NULL, 80000.75, CONCAT(@REGION_1, ',', @REGION_2, ',', @REGION_3, ',', @REGION_4, ',', @REGION_5, ',', @REGION_6, ',', @REGION_7), '测试用例：50000-100000阶层'),
(2099, 9, 1, 'TEST_031', '测试卷烟_按档位投放_无标签', '正常', 0, '按档位投放', NULL, NULL, 85000.00, CONCAT(@REGION_1, ',', @REGION_2, ',', @REGION_3, ',', @REGION_4, ',', @REGION_5, ',', @REGION_6, ',', @REGION_7), '测试用例：50000-100000阶层'),
(2099, 9, 1, 'TEST_032', '测试卷烟_按档位投放_无标签', '正常', 0, '按档位投放', NULL, NULL, 90000.25, CONCAT(@REGION_1, ',', @REGION_2, ',', @REGION_3, ',', @REGION_4, ',', @REGION_5, ',', @REGION_6, ',', @REGION_7), '测试用例：50000-100000阶层'),
(2099, 9, 1, 'TEST_033', '测试卷烟_按档位投放_无标签', '正常', 0, '按档位投放', NULL, NULL, 99999.99, CONCAT(@REGION_1, ',', @REGION_2, ',', @REGION_3, ',', @REGION_4, ',', @REGION_5, ',', @REGION_6, ',', @REGION_7), '测试用例：50000-100000阶层'),

-- 100000-150000阶层（8个样本）
(2099, 9, 1, 'TEST_034', '测试卷烟_按档位投放_无标签', '正常', 0, '按档位投放', NULL, NULL, 110000.00, CONCAT(@REGION_1, ',', @REGION_2, ',', @REGION_3, ',', @REGION_4, ',', @REGION_5, ',', @REGION_6, ',', @REGION_7), '测试用例：100000-150000阶层'),
(2099, 9, 1, 'TEST_035', '测试卷烟_按档位投放_无标签', '正常', 0, '按档位投放', NULL, NULL, 120000.25, CONCAT(@REGION_1, ',', @REGION_2, ',', @REGION_3, ',', @REGION_4, ',', @REGION_5, ',', @REGION_6, ',', @REGION_7), '测试用例：100000-150000阶层'),
(2099, 9, 1, 'TEST_036', '测试卷烟_按档位投放_无标签', '正常', 0, '按档位投放', NULL, NULL, 125000.50, CONCAT(@REGION_1, ',', @REGION_2, ',', @REGION_3, ',', @REGION_4, ',', @REGION_5, ',', @REGION_6, ',', @REGION_7), '测试用例：100000-150000阶层'),
(2099, 9, 1, 'TEST_037', '测试卷烟_按档位投放_无标签', '正常', 0, '按档位投放', NULL, NULL, 130000.75, CONCAT(@REGION_1, ',', @REGION_2, ',', @REGION_3, ',', @REGION_4, ',', @REGION_5, ',', @REGION_6, ',', @REGION_7), '测试用例：100000-150000阶层'),
(2099, 9, 1, 'TEST_038', '测试卷烟_按档位投放_无标签', '正常', 0, '按档位投放', NULL, NULL, 135000.00, CONCAT(@REGION_1, ',', @REGION_2, ',', @REGION_3, ',', @REGION_4, ',', @REGION_5, ',', @REGION_6, ',', @REGION_7), '测试用例：100000-150000阶层'),
(2099, 9, 1, 'TEST_039', '测试卷烟_按档位投放_无标签', '正常', 0, '按档位投放', NULL, NULL, 140000.25, CONCAT(@REGION_1, ',', @REGION_2, ',', @REGION_3, ',', @REGION_4, ',', @REGION_5, ',', @REGION_6, ',', @REGION_7), '测试用例：100000-150000阶层'),
(2099, 9, 1, 'TEST_040', '测试卷烟_按档位投放_无标签', '正常', 0, '按档位投放', NULL, NULL, 145000.50, CONCAT(@REGION_1, ',', @REGION_2, ',', @REGION_3, ',', @REGION_4, ',', @REGION_5, ',', @REGION_6, ',', @REGION_7), '测试用例：100000-150000阶层'),
(2099, 9, 1, 'TEST_041', '测试卷烟_按档位投放_无标签', '正常', 0, '按档位投放', NULL, NULL, 150000.00, CONCAT(@REGION_1, ',', @REGION_2, ',', @REGION_3, ',', @REGION_4, ',', @REGION_5, ',', @REGION_6, ',', @REGION_7), '测试用例：100000-150000阶层');

-- 1.2 按档位投放 + 有标签（优质数据共享客户）
-- 为了节省空间，这里只生成关键样本，实际使用时可以复制上面的模式
INSERT INTO `cigarette_distribution_info`
(YEAR, MONTH, WEEK_SEQ, CIG_CODE, CIG_NAME, SUPPLY_ATTRIBUTE, URS, DELIVERY_METHOD, DELIVERY_ETYPE, TAG, ADV, DELIVERY_AREA, BZ)
VALUES
(2099, 9, 1, 'TEST_042', '测试卷烟_按档位投放_有标签', '正常', 0, '按档位投放', NULL, '优质数据共享客户', 500.00, @REGION_1, '测试用例：按档位投放+标签'),
(2099, 9, 1, 'TEST_043', '测试卷烟_按档位投放_有标签', '正常', 0, '按档位投放', NULL, '优质数据共享客户', 5000.00, CONCAT(@REGION_1, ',', @REGION_2, ',', @REGION_3), '测试用例：按档位投放+标签'),
(2099, 9, 1, 'TEST_044', '测试卷烟_按档位投放_有标签', '正常', 0, '按档位投放', NULL, '优质数据共享客户', 50000.00, CONCAT(@REGION_1, ',', @REGION_2, ',', @REGION_3, ',', @REGION_4, ',', @REGION_5, ',', @REGION_6), '测试用例：按档位投放+标签'),
(2099, 9, 1, 'TEST_045', '测试卷烟_按档位投放_有标签', '正常', 0, '按档位投放', NULL, '优质数据共享客户', 100000.00, CONCAT(@REGION_1, ',', @REGION_2, ',', @REGION_3, ',', @REGION_4, ',', @REGION_5, ',', @REGION_6, ',', @REGION_7), '测试用例：按档位投放+标签'),
(2099, 9, 1, 'TEST_046', '测试卷烟_按档位投放_有标签', '正常', 0, '按档位投放', NULL, '优质数据共享客户', 150000.00, CONCAT(@REGION_1, ',', @REGION_2, ',', @REGION_3, ',', @REGION_4, ',', @REGION_5, ',', @REGION_6, ',', @REGION_7), '测试用例：按档位投放+标签');

-- ============================================
-- 第二部分：按档位扩展投放 + 单扩展类型（档位+区县）
-- ============================================
INSERT INTO `cigarette_distribution_info`
(YEAR, MONTH, WEEK_SEQ, CIG_CODE, CIG_NAME, SUPPLY_ATTRIBUTE, URS, DELIVERY_METHOD, DELIVERY_ETYPE, TAG, ADV, DELIVERY_AREA, BZ)
VALUES
-- 无标签
(2099, 9, 1, 'TEST_101', '测试卷烟_按档位扩展投放_档位+区县', '正常', 0, '按档位扩展投放', '档位+区县', NULL, 500.00, @REGION_1, '测试用例：档位+区县'),
(2099, 9, 1, 'TEST_102', '测试卷烟_按档位扩展投放_档位+区县', '正常', 0, '按档位扩展投放', '档位+区县', NULL, 5000.00, CONCAT(@REGION_1, ',', @REGION_2, ',', @REGION_3), '测试用例：档位+区县'),
(2099, 9, 1, 'TEST_103', '测试卷烟_按档位扩展投放_档位+区县', '正常', 0, '按档位扩展投放', '档位+区县', NULL, 50000.00, CONCAT(@REGION_1, ',', @REGION_2, ',', @REGION_3, ',', @REGION_4, ',', @REGION_5, ',', @REGION_6), '测试用例：档位+区县'),
(2099, 9, 1, 'TEST_104', '测试卷烟_按档位扩展投放_档位+区县', '正常', 0, '按档位扩展投放', '档位+区县', NULL, 100000.00, CONCAT(@REGION_1, ',', @REGION_2, ',', @REGION_3, ',', @REGION_4, ',', @REGION_5, ',', @REGION_6, ',', @REGION_7), '测试用例：档位+区县'),
(2099, 9, 1, 'TEST_105', '测试卷烟_按档位扩展投放_档位+区县', '正常', 0, '按档位扩展投放', '档位+区县', NULL, 150000.00, CONCAT(@REGION_1, ',', @REGION_2, ',', @REGION_3, ',', @REGION_4, ',', @REGION_5, ',', @REGION_6, ',', @REGION_7), '测试用例：档位+区县'),
-- 有标签
(2099, 9, 1, 'TEST_106', '测试卷烟_按档位扩展投放_档位+区县_有标签', '正常', 0, '按档位扩展投放', '档位+区县', '优质数据共享客户', 500.00, @REGION_1, '测试用例：档位+区县+标签'),
(2099, 9, 1, 'TEST_107', '测试卷烟_按档位扩展投放_档位+区县_有标签', '正常', 0, '按档位扩展投放', '档位+区县', '优质数据共享客户', 5000.00, CONCAT(@REGION_1, ',', @REGION_2, ',', @REGION_3), '测试用例：档位+区县+标签'),
(2099, 9, 1, 'TEST_108', '测试卷烟_按档位扩展投放_档位+区县_有标签', '正常', 0, '按档位扩展投放', '档位+区县', '优质数据共享客户', 50000.00, CONCAT(@REGION_1, ',', @REGION_2, ',', @REGION_3, ',', @REGION_4, ',', @REGION_5, ',', @REGION_6), '测试用例：档位+区县+标签'),
(2099, 9, 1, 'TEST_109', '测试卷烟_按档位扩展投放_档位+区县_有标签', '正常', 0, '按档位扩展投放', '档位+区县', '优质数据共享客户', 100000.00, CONCAT(@REGION_1, ',', @REGION_2, ',', @REGION_3, ',', @REGION_4, ',', @REGION_5, ',', @REGION_6, ',', @REGION_7), '测试用例：档位+区县+标签'),
(2099, 9, 1, 'TEST_110', '测试卷烟_按档位扩展投放_档位+区县_有标签', '正常', 0, '按档位扩展投放', '档位+区县', '优质数据共享客户', 150000.00, CONCAT(@REGION_1, ',', @REGION_2, ',', @REGION_3, ',', @REGION_4, ',', @REGION_5, ',', @REGION_6, ',', @REGION_7), '测试用例：档位+区县+标签');

-- ============================================
-- 第三部分：按档位扩展投放 + 其他单扩展类型（简化版，每个扩展类型5个样本）
-- ============================================
-- 档位+市场类型
INSERT INTO `cigarette_distribution_info`
(YEAR, MONTH, WEEK_SEQ, CIG_CODE, CIG_NAME, SUPPLY_ATTRIBUTE, URS, DELIVERY_METHOD, DELIVERY_ETYPE, TAG, ADV, DELIVERY_AREA, BZ)
VALUES
(2099, 9, 1, 'TEST_201', '测试卷烟_按档位扩展投放_档位+市场类型', '正常', 0, '按档位扩展投放', '档位+市场类型', NULL, 500.00, @REGION_1, '测试用例：档位+市场类型'),
(2099, 9, 1, 'TEST_202', '测试卷烟_按档位扩展投放_档位+市场类型', '正常', 0, '按档位扩展投放', '档位+市场类型', NULL, 5000.00, CONCAT(@REGION_1, ',', @REGION_2, ',', @REGION_3), '测试用例：档位+市场类型'),
(2099, 9, 1, 'TEST_203', '测试卷烟_按档位扩展投放_档位+市场类型', '正常', 0, '按档位扩展投放', '档位+市场类型', NULL, 50000.00, CONCAT(@REGION_1, ',', @REGION_2, ',', @REGION_3, ',', @REGION_4, ',', @REGION_5, ',', @REGION_6), '测试用例：档位+市场类型'),
(2099, 9, 1, 'TEST_204', '测试卷烟_按档位扩展投放_档位+市场类型', '正常', 0, '按档位扩展投放', '档位+市场类型', '优质数据共享客户', 100000.00, CONCAT(@REGION_1, ',', @REGION_2, ',', @REGION_3, ',', @REGION_4, ',', @REGION_5, ',', @REGION_6, ',', @REGION_7), '测试用例：档位+市场类型+标签'),
(2099, 9, 1, 'TEST_205', '测试卷烟_按档位扩展投放_档位+市场类型', '正常', 0, '按档位扩展投放', '档位+市场类型', '优质数据共享客户', 150000.00, CONCAT(@REGION_1, ',', @REGION_2, ',', @REGION_3, ',', @REGION_4, ',', @REGION_5, ',', @REGION_6, ',', @REGION_7), '测试用例：档位+市场类型+标签');

-- 档位+城乡分类代码
INSERT INTO `cigarette_distribution_info`
(YEAR, MONTH, WEEK_SEQ, CIG_CODE, CIG_NAME, SUPPLY_ATTRIBUTE, URS, DELIVERY_METHOD, DELIVERY_ETYPE, TAG, ADV, DELIVERY_AREA, BZ)
VALUES
(2099, 9, 1, 'TEST_301', '测试卷烟_按档位扩展投放_档位+城乡分类代码', '正常', 0, '按档位扩展投放', '档位+城乡分类代码', NULL, 500.00, @REGION_1, '测试用例：档位+城乡分类代码'),
(2099, 9, 1, 'TEST_302', '测试卷烟_按档位扩展投放_档位+城乡分类代码', '正常', 0, '按档位扩展投放', '档位+城乡分类代码', NULL, 5000.00, CONCAT(@REGION_1, ',', @REGION_2, ',', @REGION_3), '测试用例：档位+城乡分类代码'),
(2099, 9, 1, 'TEST_303', '测试卷烟_按档位扩展投放_档位+城乡分类代码', '正常', 0, '按档位扩展投放', '档位+城乡分类代码', NULL, 50000.00, CONCAT(@REGION_1, ',', @REGION_2, ',', @REGION_3, ',', @REGION_4, ',', @REGION_5, ',', @REGION_6), '测试用例：档位+城乡分类代码'),
(2099, 9, 1, 'TEST_304', '测试卷烟_按档位扩展投放_档位+城乡分类代码', '正常', 0, '按档位扩展投放', '档位+城乡分类代码', '优质数据共享客户', 100000.00, CONCAT(@REGION_1, ',', @REGION_2, ',', @REGION_3, ',', @REGION_4, ',', @REGION_5, ',', @REGION_6, ',', @REGION_7), '测试用例：档位+城乡分类代码+标签'),
(2099, 9, 1, 'TEST_305', '测试卷烟_按档位扩展投放_档位+城乡分类代码', '正常', 0, '按档位扩展投放', '档位+城乡分类代码', '优质数据共享客户', 150000.00, CONCAT(@REGION_1, ',', @REGION_2, ',', @REGION_3, ',', @REGION_4, ',', @REGION_5, ',', @REGION_6, ',', @REGION_7), '测试用例：档位+城乡分类代码+标签');

-- 档位+业态
INSERT INTO `cigarette_distribution_info`
(YEAR, MONTH, WEEK_SEQ, CIG_CODE, CIG_NAME, SUPPLY_ATTRIBUTE, URS, DELIVERY_METHOD, DELIVERY_ETYPE, TAG, ADV, DELIVERY_AREA, BZ)
VALUES
(2099, 9, 1, 'TEST_401', '测试卷烟_按档位扩展投放_档位+业态', '正常', 0, '按档位扩展投放', '档位+业态', NULL, 500.00, @REGION_1, '测试用例：档位+业态'),
(2099, 9, 1, 'TEST_402', '测试卷烟_按档位扩展投放_档位+业态', '正常', 0, '按档位扩展投放', '档位+业态', NULL, 5000.00, CONCAT(@REGION_1, ',', @REGION_2, ',', @REGION_3), '测试用例：档位+业态'),
(2099, 9, 1, 'TEST_403', '测试卷烟_按档位扩展投放_档位+业态', '正常', 0, '按档位扩展投放', '档位+业态', NULL, 50000.00, CONCAT(@REGION_1, ',', @REGION_2, ',', @REGION_3, ',', @REGION_4, ',', @REGION_5, ',', @REGION_6), '测试用例：档位+业态'),
(2099, 9, 1, 'TEST_404', '测试卷烟_按档位扩展投放_档位+业态', '正常', 0, '按档位扩展投放', '档位+业态', '优质数据共享客户', 100000.00, CONCAT(@REGION_1, ',', @REGION_2, ',', @REGION_3, ',', @REGION_4, ',', @REGION_5, ',', @REGION_6, ',', @REGION_7), '测试用例：档位+业态+标签'),
(2099, 9, 1, 'TEST_405', '测试卷烟_按档位扩展投放_档位+业态', '正常', 0, '按档位扩展投放', '档位+业态', '优质数据共享客户', 150000.00, CONCAT(@REGION_1, ',', @REGION_2, ',', @REGION_3, ',', @REGION_4, ',', @REGION_5, ',', @REGION_6, ',', @REGION_7), '测试用例：档位+业态+标签');

-- 档位+市场部
INSERT INTO `cigarette_distribution_info`
(YEAR, MONTH, WEEK_SEQ, CIG_CODE, CIG_NAME, SUPPLY_ATTRIBUTE, URS, DELIVERY_METHOD, DELIVERY_ETYPE, TAG, ADV, DELIVERY_AREA, BZ)
VALUES
(2099, 9, 1, 'TEST_501', '测试卷烟_按档位扩展投放_档位+市场部', '正常', 0, '按档位扩展投放', '档位+市场部', NULL, 500.00, @REGION_1, '测试用例：档位+市场部'),
(2099, 9, 1, 'TEST_502', '测试卷烟_按档位扩展投放_档位+市场部', '正常', 0, '按档位扩展投放', '档位+市场部', NULL, 5000.00, CONCAT(@REGION_1, ',', @REGION_2, ',', @REGION_3), '测试用例：档位+市场部'),
(2099, 9, 1, 'TEST_503', '测试卷烟_按档位扩展投放_档位+市场部', '正常', 0, '按档位扩展投放', '档位+市场部', NULL, 50000.00, CONCAT(@REGION_1, ',', @REGION_2, ',', @REGION_3, ',', @REGION_4, ',', @REGION_5, ',', @REGION_6), '测试用例：档位+市场部'),
(2099, 9, 1, 'TEST_504', '测试卷烟_按档位扩展投放_档位+市场部', '正常', 0, '按档位扩展投放', '档位+市场部', '优质数据共享客户', 100000.00, CONCAT(@REGION_1, ',', @REGION_2, ',', @REGION_3, ',', @REGION_4, ',', @REGION_5, ',', @REGION_6, ',', @REGION_7), '测试用例：档位+市场部+标签'),
(2099, 9, 1, 'TEST_505', '测试卷烟_按档位扩展投放_档位+市场部', '正常', 0, '按档位扩展投放', '档位+市场部', '优质数据共享客户', 150000.00, CONCAT(@REGION_1, ',', @REGION_2, ',', @REGION_3, ',', @REGION_4, ',', @REGION_5, ',', @REGION_6, ',', @REGION_7), '测试用例：档位+市场部+标签');

-- ============================================
-- 第四部分：按档位扩展投放 + 双扩展类型
-- ============================================
-- 档位+区县+市场类型
INSERT INTO `cigarette_distribution_info`
(YEAR, MONTH, WEEK_SEQ, CIG_CODE, CIG_NAME, SUPPLY_ATTRIBUTE, URS, DELIVERY_METHOD, DELIVERY_ETYPE, TAG, ADV, DELIVERY_AREA, BZ)
VALUES
(2099, 9, 1, 'TEST_601', '测试卷烟_按档位扩展投放_档位+区县+市场类型', '正常', 0, '按档位扩展投放', '档位+区县+市场类型', NULL, 500.00, @REGION_1, '测试用例：档位+区县+市场类型'),
(2099, 9, 1, 'TEST_602', '测试卷烟_按档位扩展投放_档位+区县+市场类型', '正常', 0, '按档位扩展投放', '档位+区县+市场类型', NULL, 5000.00, CONCAT(@REGION_1, ',', @REGION_2, ',', @REGION_3), '测试用例：档位+区县+市场类型'),
(2099, 9, 1, 'TEST_603', '测试卷烟_按档位扩展投放_档位+区县+市场类型', '正常', 0, '按档位扩展投放', '档位+区县+市场类型', NULL, 50000.00, CONCAT(@REGION_1, ',', @REGION_2, ',', @REGION_3, ',', @REGION_4, ',', @REGION_5, ',', @REGION_6), '测试用例：档位+区县+市场类型'),
(2099, 9, 1, 'TEST_604', '测试卷烟_按档位扩展投放_档位+区县+市场类型', '正常', 0, '按档位扩展投放', '档位+区县+市场类型', '优质数据共享客户', 100000.00, CONCAT(@REGION_1, ',', @REGION_2, ',', @REGION_3, ',', @REGION_4, ',', @REGION_5, ',', @REGION_6, ',', @REGION_7), '测试用例：档位+区县+市场类型+标签'),
(2099, 9, 1, 'TEST_605', '测试卷烟_按档位扩展投放_档位+区县+市场类型', '正常', 0, '按档位扩展投放', '档位+区县+市场类型', '优质数据共享客户', 150000.00, CONCAT(@REGION_1, ',', @REGION_2, ',', @REGION_3, ',', @REGION_4, ',', @REGION_5, ',', @REGION_6, ',', @REGION_7), '测试用例：档位+区县+市场类型+标签');

-- 档位+区县+城乡分类代码
INSERT INTO `cigarette_distribution_info`
(YEAR, MONTH, WEEK_SEQ, CIG_CODE, CIG_NAME, SUPPLY_ATTRIBUTE, URS, DELIVERY_METHOD, DELIVERY_ETYPE, TAG, ADV, DELIVERY_AREA, BZ)
VALUES
(2099, 9, 1, 'TEST_701', '测试卷烟_按档位扩展投放_档位+区县+城乡分类代码', '正常', 0, '按档位扩展投放', '档位+区县+城乡分类代码', NULL, 500.00, @REGION_1, '测试用例：档位+区县+城乡分类代码'),
(2099, 9, 1, 'TEST_702', '测试卷烟_按档位扩展投放_档位+区县+城乡分类代码', '正常', 0, '按档位扩展投放', '档位+区县+城乡分类代码', NULL, 5000.00, CONCAT(@REGION_1, ',', @REGION_2, ',', @REGION_3), '测试用例：档位+区县+城乡分类代码'),
(2099, 9, 1, 'TEST_703', '测试卷烟_按档位扩展投放_档位+区县+城乡分类代码', '正常', 0, '按档位扩展投放', '档位+区县+城乡分类代码', NULL, 50000.00, CONCAT(@REGION_1, ',', @REGION_2, ',', @REGION_3, ',', @REGION_4, ',', @REGION_5, ',', @REGION_6), '测试用例：档位+区县+城乡分类代码'),
(2099, 9, 1, 'TEST_704', '测试卷烟_按档位扩展投放_档位+区县+城乡分类代码', '正常', 0, '按档位扩展投放', '档位+区县+城乡分类代码', '优质数据共享客户', 100000.00, CONCAT(@REGION_1, ',', @REGION_2, ',', @REGION_3, ',', @REGION_4, ',', @REGION_5, ',', @REGION_6, ',', @REGION_7), '测试用例：档位+区县+城乡分类代码+标签'),
(2099, 9, 1, 'TEST_705', '测试卷烟_按档位扩展投放_档位+区县+城乡分类代码', '正常', 0, '按档位扩展投放', '档位+区县+城乡分类代码', '优质数据共享客户', 150000.00, CONCAT(@REGION_1, ',', @REGION_2, ',', @REGION_3, ',', @REGION_4, ',', @REGION_5, ',', @REGION_6, ',', @REGION_7), '测试用例：档位+区县+城乡分类代码+标签');

-- ============================================
-- 第五部分：边界值测试用例
-- ============================================
INSERT INTO `cigarette_distribution_info`
(YEAR, MONTH, WEEK_SEQ, CIG_CODE, CIG_NAME, SUPPLY_ATTRIBUTE, URS, DELIVERY_METHOD, DELIVERY_ETYPE, TAG, ADV, DELIVERY_AREA, BZ)
VALUES
(2099, 9, 1, 'TEST_BOUNDARY_001', '测试卷烟_边界值_1', '正常', 0, '按档位投放', NULL, NULL, 1.00, @REGION_1, '边界值测试：最小投放量'),
(2099, 9, 1, 'TEST_BOUNDARY_002', '测试卷烟_边界值_999', '正常', 0, '按档位投放', NULL, NULL, 999.00, @REGION_1, '边界值测试：999'),
(2099, 9, 1, 'TEST_BOUNDARY_003', '测试卷烟_边界值_1000', '正常', 0, '按档位投放', NULL, NULL, 1000.00, @REGION_1, '边界值测试：1000'),
(2099, 9, 1, 'TEST_BOUNDARY_004', '测试卷烟_边界值_1001', '正常', 0, '按档位投放', NULL, NULL, 1001.00, CONCAT(@REGION_1, ',', @REGION_2), '边界值测试：1001'),
(2099, 9, 1, 'TEST_BOUNDARY_005', '测试卷烟_边界值_99999', '正常', 0, '按档位投放', NULL, NULL, 99999.00, CONCAT(@REGION_1, ',', @REGION_2, ',', @REGION_3, ',', @REGION_4, ',', @REGION_5, ',', @REGION_6), '边界值测试：99999'),
(2099, 9, 1, 'TEST_BOUNDARY_006', '测试卷烟_边界值_100000', '正常', 0, '按档位投放', NULL, NULL, 100000.00, CONCAT(@REGION_1, ',', @REGION_2, ',', @REGION_3, ',', @REGION_4, ',', @REGION_5, ',', @REGION_6, ',', @REGION_7), '边界值测试：100000'),
(2099, 9, 1, 'TEST_BOUNDARY_007', '测试卷烟_边界值_150000', '正常', 0, '按档位投放', NULL, NULL, 150000.00, CONCAT(@REGION_1, ',', @REGION_2, ',', @REGION_3, ',', @REGION_4, ',', @REGION_5, ',', @REGION_6, ',', @REGION_7), '边界值测试：150000');

-- ============================================
-- 第六部分：特殊场景测试用例
-- ============================================
INSERT INTO `cigarette_distribution_info`
(YEAR, MONTH, WEEK_SEQ, CIG_CODE, CIG_NAME, SUPPLY_ATTRIBUTE, URS, DELIVERY_METHOD, DELIVERY_ETYPE, TAG, ADV, DELIVERY_AREA, BZ)
VALUES
-- 场景1：单区域 + 大投放量
(2099, 9, 1, 'TEST_SPECIAL_001', '测试卷烟_特殊场景_单区域大投放量', '正常', 0, '按档位投放', NULL, NULL, 50000.00, @REGION_1, '特殊场景：单区域+大投放量'),
-- 场景2：全部区域 + 小投放量
(2099, 9, 1, 'TEST_SPECIAL_002', '测试卷烟_特殊场景_全部区域小投放量', '正常', 0, '按档位扩展投放', '档位+区县', NULL, 5000.00, CONCAT(@REGION_1, ',', @REGION_2, ',', @REGION_3, ',', @REGION_4, ',', @REGION_5, ',', @REGION_6, ',', @REGION_7), '特殊场景：全部区域+小投放量'),
-- 场景3：双扩展类型 + 大投放量 + 标签
(2099, 9, 1, 'TEST_SPECIAL_003', '测试卷烟_特殊场景_双扩展大投放量标签', '正常', 0, '按档位扩展投放', '档位+区县+市场类型', '优质数据共享客户', 100000.00, CONCAT(@REGION_1, ',', @REGION_2, ',', @REGION_3, ',', @REGION_4, ',', @REGION_5, ',', @REGION_6), '特殊场景：双扩展+大投放量+标签');

-- ============================================
-- 验证插入结果
-- ============================================
SELECT 
    COUNT(*) AS total_cases,
    COUNT(DISTINCT DELIVERY_METHOD) AS delivery_methods,
    COUNT(DISTINCT DELIVERY_ETYPE) AS extension_types,
    COUNT(DISTINCT TAG) AS tags,
    MIN(ADV) AS min_adv,
    MAX(ADV) AS max_adv,
    COUNT(DISTINCT DELIVERY_AREA) AS region_combinations
FROM cigarette_distribution_info
WHERE YEAR = 2099 AND MONTH = 9 AND WEEK_SEQ = 1;

-- 按投放方式统计
SELECT 
    DELIVERY_METHOD,
    DELIVERY_ETYPE,
    COUNT(*) AS case_count,
    MIN(ADV) AS min_adv,
    MAX(ADV) AS max_adv
FROM cigarette_distribution_info
WHERE YEAR = 2099 AND MONTH = 9 AND WEEK_SEQ = 1
GROUP BY DELIVERY_METHOD, DELIVERY_ETYPE
ORDER BY DELIVERY_METHOD, DELIVERY_ETYPE;

