package org.example.encode;

import lombok.extern.slf4j.Slf4j;
import org.example.application.service.encode.EncodeService;
import org.example.infrastructure.persistence.po.CigaretteDistributionPredictionPO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 验证编码表达式是否包含标签后缀的测试
 * 
 * @author Robin
 * @since 2025-12-20
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
public class EncodingTagSuffixVerificationTest {

    @Autowired
    private EncodeService encodeService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 验证2025/9/3分区中带标签的卷烟编码表达式是否包含标签后缀
     */
    @Test
    public void verifyTagSuffixInEncodingExpression() {
        int year = 2025;
        int month = 9;
        int weekSeq = 3;

        log.info("开始验证 {}年{}月第{}周的编码表达式标签后缀", year, month, weekSeq);

        // 查询带标签的卷烟记录
        String sql = "SELECT DISTINCT " +
                "  p.CIG_CODE, " +
                "  p.CIG_NAME, " +
                "  p.DELIVERY_METHOD, " +
                "  p.DELIVERY_ETYPE, " +
                "  p.TAG, " +
                "  p.DELIVERY_AREA, " +
                "  p.DEPLOYINFO_CODE " +
                "FROM cigarette_distribution_prediction p " +
                "WHERE p.YEAR = ? AND p.MONTH = ? AND p.WEEK_SEQ = ? " +
                "  AND p.TAG IS NOT NULL AND p.TAG != '' " +
                "ORDER BY p.CIG_CODE, p.DELIVERY_AREA " +
                "LIMIT 20";

        List<Map<String, Object>> records = jdbcTemplate.queryForList(sql, year, month, weekSeq);

        if (records.isEmpty()) {
            log.warn("未找到带标签的记录");
            return;
        }

        log.info("找到 {} 条带标签的记录，开始验证编码表达式", records.size());

        int correctCount = 0;
        int incorrectCount = 0;
        List<String> incorrectRecords = new ArrayList<>();

        for (Map<String, Object> record : records) {
            String cigCode = (String) record.get("CIG_CODE");
            String cigName = (String) record.get("CIG_NAME");
            String deliveryMethod = (String) record.get("DELIVERY_METHOD");
            String deliveryEtype = (String) record.get("DELIVERY_ETYPE");
            String tag = (String) record.get("TAG");
            String deliveryArea = (String) record.get("DELIVERY_AREA");
            String existingCode = (String) record.get("DEPLOYINFO_CODE");

            // 重新生成编码表达式（使用修复后的逻辑）
            String regeneratedCode = regenerateEncodingExpression(
                    cigCode, cigName, deliveryMethod, deliveryEtype, deliveryArea, tag, year, month, weekSeq);

            // 检查是否包含标签后缀
            boolean shouldHaveTagSuffix = tag != null && tag.contains("优质数据共享客户");
            boolean hasTagSuffix = regeneratedCode != null && regeneratedCode.contains("+a");

            if (shouldHaveTagSuffix && !hasTagSuffix) {
                incorrectCount++;
                String errorMsg = String.format(
                        "❌ 卷烟 %s (%s) - 区域: %s, 标签: %s, 现有编码: %s, 重新生成: %s",
                        cigCode, cigName, deliveryArea, tag, existingCode, regeneratedCode);
                incorrectRecords.add(errorMsg);
                log.error(errorMsg);
            } else if (shouldHaveTagSuffix && hasTagSuffix) {
                correctCount++;
                log.info("✅ 卷烟 {} ({}) - 区域: {}, 标签: {}, 编码: {}",
                        cigCode, cigName, deliveryArea, tag, regeneratedCode);
            }
        }

        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("验证结果统计：");
        log.info("  总记录数: {}", records.size());
        log.info("  正确（包含标签后缀）: {}", correctCount);
        log.info("  错误（缺少标签后缀）: {}", incorrectCount);
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        if (incorrectCount > 0) {
            log.error("发现 {} 条记录的编码表达式缺少标签后缀：", incorrectCount);
            incorrectRecords.forEach(log::error);
        } else {
            log.info("✅ 所有带标签的卷烟编码表达式都正确包含了标签后缀！");
        }
    }

    /**
     * 重新生成编码表达式（模拟修复后的逻辑）
     */
    private String regenerateEncodingExpression(
            String cigCode, String cigName, String deliveryMethod, String deliveryEtype,
            String deliveryArea, String tag, int year, int month, int weekSeq) {

        // 查询该卷烟的所有记录
        String sql = "SELECT * FROM cigarette_distribution_prediction " +
                "WHERE YEAR = ? AND MONTH = ? AND WEEK_SEQ = ? " +
                "  AND CIG_CODE = ? AND CIG_NAME = ?";

        List<Map<String, Object>> allRecords = jdbcTemplate.queryForList(
                sql, year, month, weekSeq, cigCode, cigName);

        // 转换为PO对象列表
        List<CigaretteDistributionPredictionPO> poList = new ArrayList<>();
        for (Map<String, Object> row : allRecords) {
            CigaretteDistributionPredictionPO po = new CigaretteDistributionPredictionPO();
            po.setCigCode((String) row.get("CIG_CODE"));
            po.setCigName((String) row.get("CIG_NAME"));
            po.setDeliveryMethod((String) row.get("DELIVERY_METHOD"));
            po.setDeliveryEtype((String) row.get("DELIVERY_ETYPE"));
            po.setDeliveryArea((String) row.get("DELIVERY_AREA"));
            po.setTag((String) row.get("TAG"));
            po.setTagFilterConfig((String) row.get("TAG_FILTER_CONFIG"));

            // 设置30个档位
            for (int i = 30; i >= 1; i--) {
                String gradeName = "D" + i;
                Object gradeValue = row.get(gradeName);
                if (gradeValue != null) {
                    BigDecimal value = gradeValue instanceof BigDecimal
                            ? (BigDecimal) gradeValue
                            : new BigDecimal(gradeValue.toString());
                    setGradeValue(po, i, value);
                }
            }

            poList.add(po);
        }

        // 调用编码服务生成表达式
        return encodeService.encodeForSpecificArea(
                cigCode, cigName, deliveryMethod, deliveryEtype, deliveryArea, poList);
    }

    /**
     * 设置档位值（使用反射或直接设置）
     */
    private void setGradeValue(CigaretteDistributionPredictionPO po, int grade, BigDecimal value) {
        switch (grade) {
            case 30: po.setD30(value); break;
            case 29: po.setD29(value); break;
            case 28: po.setD28(value); break;
            case 27: po.setD27(value); break;
            case 26: po.setD26(value); break;
            case 25: po.setD25(value); break;
            case 24: po.setD24(value); break;
            case 23: po.setD23(value); break;
            case 22: po.setD22(value); break;
            case 21: po.setD21(value); break;
            case 20: po.setD20(value); break;
            case 19: po.setD19(value); break;
            case 18: po.setD18(value); break;
            case 17: po.setD17(value); break;
            case 16: po.setD16(value); break;
            case 15: po.setD15(value); break;
            case 14: po.setD14(value); break;
            case 13: po.setD13(value); break;
            case 12: po.setD12(value); break;
            case 11: po.setD11(value); break;
            case 10: po.setD10(value); break;
            case 9: po.setD9(value); break;
            case 8: po.setD8(value); break;
            case 7: po.setD7(value); break;
            case 6: po.setD6(value); break;
            case 5: po.setD5(value); break;
            case 4: po.setD4(value); break;
            case 3: po.setD3(value); break;
            case 2: po.setD2(value); break;
            case 1: po.setD1(value); break;
        }
    }

    /**
     * 验证现有数据库中的编码表达式是否正确包含标签后缀
     * 这个测试检查已存储的DEPLOYINFO_CODE字段
     */
    @Test
    public void verifyExistingDeployinfoCodeTagSuffix() {
        int year = 2025;
        int month = 9;
        int weekSeq = 3;

        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("检查数据库中已存储的编码表达式（DEPLOYINFO_CODE）");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        // 查询带标签的记录
        String sql = "SELECT " +
                "  CIG_CODE, " +
                "  CIG_NAME, " +
                "  TAG, " +
                "  DELIVERY_AREA, " +
                "  DEPLOYINFO_CODE " +
                "FROM cigarette_distribution_prediction " +
                "WHERE YEAR = ? AND MONTH = ? AND WEEK_SEQ = ? " +
                "  AND TAG IS NOT NULL AND TAG != '' " +
                "ORDER BY CIG_CODE, DELIVERY_AREA";

        List<Map<String, Object>> records = jdbcTemplate.queryForList(sql, year, month, weekSeq);

        if (records.isEmpty()) {
            log.warn("⚠️ 未找到带标签的记录");
            return;
        }

        log.info("找到 {} 条带标签的记录", records.size());

        int hasTagSuffixCount = 0;
        int missingTagSuffixCount = 0;

        for (Map<String, Object> record : records) {
            String cigCode = (String) record.get("CIG_CODE");
            String cigName = (String) record.get("CIG_NAME");
            String tag = (String) record.get("TAG");
            String deliveryArea = (String) record.get("DELIVERY_AREA");
            String deployinfoCode = (String) record.get("DEPLOYINFO_CODE");

            boolean shouldHaveTagSuffix = tag != null && tag.contains("优质数据共享客户");
            boolean hasTagSuffix = deployinfoCode != null && deployinfoCode.contains("+a");

            if (shouldHaveTagSuffix) {
                if (hasTagSuffix) {
                    hasTagSuffixCount++;
                    log.info("✅ {} ({}) - 区域: {}, 编码: {}", cigCode, cigName, deliveryArea, deployinfoCode);
                } else {
                    missingTagSuffixCount++;
                    log.error("❌ {} ({}) - 区域: {}, 标签: {}, 编码缺少+a后缀: {}",
                            cigCode, cigName, deliveryArea, tag, deployinfoCode);
                }
            }
        }

        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("数据库编码表达式验证结果：");
        log.info("  总记录数: {}", records.size());
        log.info("  ✅ 正确包含标签后缀: {}", hasTagSuffixCount);
        log.info("  ❌ 缺少标签后缀: {}", missingTagSuffixCount);
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        if (missingTagSuffixCount > 0) {
            log.error("⚠️ 发现 {} 条记录的编码表达式缺少标签后缀，需要重新生成分配方案！", missingTagSuffixCount);
        } else {
            log.info("🎉 所有编码表达式都正确包含了标签后缀！");
        }
    }

    /**
     * 对比修复前后的编码表达式差异
     */
    @Test
    public void compareEncodingExpressionBeforeAndAfter() {
        int year = 2025;
        int month = 9;
        int weekSeq = 3;

        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("对比修复前后的编码表达式差异");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        // 查询带标签的记录
        String sql = "SELECT " +
                "  CIG_CODE, " +
                "  CIG_NAME, " +
                "  DELIVERY_METHOD, " +
                "  DELIVERY_ETYPE, " +
                "  TAG, " +
                "  DELIVERY_AREA, " +
                "  DEPLOYINFO_CODE " +
                "FROM cigarette_distribution_prediction " +
                "WHERE YEAR = ? AND MONTH = ? AND WEEK_SEQ = ? " +
                "  AND TAG IS NOT NULL AND TAG != '' " +
                "LIMIT 5";

        List<Map<String, Object>> records = jdbcTemplate.queryForList(sql, year, month, weekSeq);

        if (records.isEmpty()) {
            log.warn("⚠️ 未找到带标签的记录");
            return;
        }

        log.info("分析前 {} 条记录的编码表达式\n", records.size());

        for (Map<String, Object> record : records) {
            String cigCode = (String) record.get("CIG_CODE");
            String cigName = (String) record.get("CIG_NAME");
            String deliveryMethod = (String) record.get("DELIVERY_METHOD");
            String deliveryEtype = (String) record.get("DELIVERY_ETYPE");
            String tag = (String) record.get("TAG");
            String deliveryArea = (String) record.get("DELIVERY_AREA");
            String oldCode = (String) record.get("DEPLOYINFO_CODE");

            // 使用修复后的逻辑重新生成
            String newCode = regenerateEncodingExpression(
                    cigCode, cigName, deliveryMethod, deliveryEtype, deliveryArea, tag, year, month, weekSeq);

            log.info("卷烟: {} ({})", cigCode, cigName);
            log.info("  标签: {}", tag);
            log.info("  区域: {}", deliveryArea);
            log.info("  投放方式: {}", deliveryMethod);
            log.info("  修复前编码: {}", oldCode);
            log.info("  修复后编码: {}", newCode);
            
            if (oldCode != null && newCode != null) {
                if (oldCode.equals(newCode)) {
                    log.info("  ✅ 编码一致（已包含标签后缀）\n");
                } else {
                    log.warn("  ⚠️  编码不一致（需要更新）\n");
                }
            } else {
                log.error("  ❌ 编码为空\n");
            }
        }

        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    /**
     * 统计分析标签编码情况
     */
    @Test
    public void analyzeTagEncodingStatistics() {
        int year = 2025;
        int month = 9;
        int weekSeq = 3;

        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("统计分析 {}年{}月第{}周 的标签编码情况", year, month, weekSeq);
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        // 1. 统计总记录数
        String totalSql = "SELECT COUNT(*) FROM cigarette_distribution_prediction " +
                "WHERE YEAR = ? AND MONTH = ? AND WEEK_SEQ = ?";
        Integer totalCount = jdbcTemplate.queryForObject(totalSql, Integer.class, year, month, weekSeq);

        // 2. 统计带标签的记录数
        String tagSql = "SELECT COUNT(*) FROM cigarette_distribution_prediction " +
                "WHERE YEAR = ? AND MONTH = ? AND WEEK_SEQ = ? " +
                "AND TAG IS NOT NULL AND TAG != ''";
        Integer tagCount = jdbcTemplate.queryForObject(tagSql, Integer.class, year, month, weekSeq);

        // 3. 统计编码表达式包含+a的记录数
        String tagSuffixSql = "SELECT COUNT(*) FROM cigarette_distribution_prediction " +
                "WHERE YEAR = ? AND MONTH = ? AND WEEK_SEQ = ? " +
                "AND DEPLOYINFO_CODE LIKE '%+a%'";
        Integer tagSuffixCount = jdbcTemplate.queryForObject(tagSuffixSql, Integer.class, year, month, weekSeq);

        // 4. 查询带标签但编码缺少+a的记录
        String missingSql = "SELECT COUNT(*) FROM cigarette_distribution_prediction " +
                "WHERE YEAR = ? AND MONTH = ? AND WEEK_SEQ = ? " +
                "AND TAG IS NOT NULL AND TAG != '' " +
                "AND (DEPLOYINFO_CODE IS NULL OR DEPLOYINFO_CODE NOT LIKE '%+a%')";
        Integer missingCount = jdbcTemplate.queryForObject(missingSql, Integer.class, year, month, weekSeq);

        // 5. 统计不同标签类型
        String tagTypesSql = "SELECT TAG, COUNT(*) as cnt FROM cigarette_distribution_prediction " +
                "WHERE YEAR = ? AND MONTH = ? AND WEEK_SEQ = ? " +
                "AND TAG IS NOT NULL AND TAG != '' " +
                "GROUP BY TAG";
        List<Map<String, Object>> tagTypes = jdbcTemplate.queryForList(tagTypesSql, year, month, weekSeq);

        log.info("\n📊 统计结果：");
        log.info("  总记录数: {}", totalCount);
        log.info("  带标签记录数: {}", tagCount);
        log.info("  编码包含+a的记录数: {}", tagSuffixCount);
        log.info("  带标签但编码缺少+a: {}", missingCount);
        
        if (tagCount != null && tagCount > 0) {
            double tagPercentage = (tagCount * 100.0) / totalCount;
            log.info("  标签覆盖率: {:.2f}%", tagPercentage);
        }

        if (tagCount != null && tagSuffixCount != null && tagCount > 0) {
            double correctPercentage = (tagSuffixCount * 100.0) / tagCount;
            log.info("  标签编码正确率: {:.2f}%", correctPercentage);
        }

        log.info("\n📋 标签类型分布：");
        for (Map<String, Object> tagType : tagTypes) {
            String tag = (String) tagType.get("TAG");
            Object cnt = tagType.get("cnt");
            log.info("  {} : {} 条记录", tag, cnt);
        }

        // 6. 查询具体缺少标签后缀的卷烟
        if (missingCount != null && missingCount > 0) {
            log.info("\n❌ 缺少标签后缀的卷烟列表：");
            String detailSql = "SELECT DISTINCT CIG_CODE, CIG_NAME, TAG, DEPLOYINFO_CODE " +
                    "FROM cigarette_distribution_prediction " +
                    "WHERE YEAR = ? AND MONTH = ? AND WEEK_SEQ = ? " +
                    "AND TAG IS NOT NULL AND TAG != '' " +
                    "AND (DEPLOYINFO_CODE IS NULL OR DEPLOYINFO_CODE NOT LIKE '%+a%') " +
                    "LIMIT 10";
            List<Map<String, Object>> missingRecords = jdbcTemplate.queryForList(detailSql, year, month, weekSeq);
            
            for (Map<String, Object> record : missingRecords) {
                log.error("  {} - {} | 标签: {} | 编码: {}",
                        record.get("CIG_CODE"),
                        record.get("CIG_NAME"),
                        record.get("TAG"),
                        record.get("DEPLOYINFO_CODE"));
            }
        }

        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        if (missingCount != null && missingCount > 0) {
            log.error("⚠️  发现问题：有 {} 条记录的编码表达式缺少标签后缀！", missingCount);
            log.error("建议：重新运行分配算法以生成正确的编码表达式");
        } else {
            log.info("✅ 所有带标签的记录编码表达式都正确包含了标签后缀！");
        }
    }

    /**
     * 测试单个卷烟的编码表达式生成（带标签）
     */
    @Test
    public void testSingleCigaretteEncodingWithTag() {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("测试单个卷烟的编码表达式生成（带标签）");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        // 构造测试数据
        String cigCode = "42021111";
        String cigName = "黄楼(蓝)";
        String deliveryMethod = "按档位投放";
        String deliveryEtype = null;
        String targetArea = "全市";
        String tag = "优质数据共享客户";

        // 创建测试记录
        CigaretteDistributionPredictionPO record = new CigaretteDistributionPredictionPO();
        record.setCigCode(cigCode);
        record.setCigName(cigName);
        record.setDeliveryMethod(deliveryMethod);
        record.setDeliveryEtype(deliveryEtype);
        record.setDeliveryArea(targetArea);
        record.setTag(tag);
        
        // 设置档位数据（模拟：1×10+28×8+1×7）
        record.setD30(BigDecimal.TEN);
        for (int i = 29; i >= 2; i--) {
            setGradeValue(record, i, new BigDecimal("8"));
        }
        record.setD1(new BigDecimal("7"));

        List<CigaretteDistributionPredictionPO> records = new ArrayList<>();
        records.add(record);

        // 生成编码表达式
        String encodedExpression = encodeService.encodeForSpecificArea(
                cigCode, cigName, deliveryMethod, deliveryEtype, targetArea, records);

        log.info("\n测试结果：");
        log.info("  卷烟: {} - {}", cigCode, cigName);
        log.info("  标签: {}", tag);
        log.info("  投放方式: {}", deliveryMethod);
        log.info("  投放区域: {}", targetArea);
        log.info("  生成的编码表达式: {}", encodedExpression);

        // 验证编码表达式
        boolean hasMethodCode = encodedExpression != null && encodedExpression.startsWith("A");
        boolean hasTagSuffix = encodedExpression != null && encodedExpression.contains("+a");
        boolean hasGradeEncoding = encodedExpression != null && encodedExpression.contains("（") && encodedExpression.contains("）");

        log.info("\n验证结果：");
        log.info("  ✓ 包含投放方式编码(A): {}", hasMethodCode ? "✅" : "❌");
        log.info("  ✓ 包含标签后缀(+a): {}", hasTagSuffix ? "✅" : "❌");
        log.info("  ✓ 包含档位编码: {}", hasGradeEncoding ? "✅" : "❌");

        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        if (hasMethodCode && hasTagSuffix && hasGradeEncoding) {
            log.info("🎉 编码表达式生成正确！");
        } else {
            log.error("❌ 编码表达式生成有误！");
        }
    }
}