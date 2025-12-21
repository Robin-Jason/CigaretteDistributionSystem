package org.example.service.calculate;

import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * 测试用例SQL脚本生成器
 * <p>
 * 用途：根据TestCaseGenerator生成的测试用例，自动生成SQL INSERT语句
 * 输出：生成SQL脚本文件，可直接在数据库中执行
 * </p>
 *
 * @author Robin
 * @since 2025-12-20
 */
public class TestCaseSqlGenerator {

    private static final int TEST_YEAR = 2099;
    private static final int TEST_MONTH = 9;
    private static final int TEST_WEEK_SEQ = 1;
    private static final String SUPPLY_ATTRIBUTE = "正常";
    private static final BigDecimal URS = BigDecimal.ZERO;

    // 可用区域列表（按客户数从高到低排序）
    private static final String[] REGIONS = {
            "城区",
            "丹江口市",
            "房县",
            "郧阳区",
            "竹山县",
            "郧西县",
            "竹溪县"
    };

    /**
     * 生成SQL脚本文件
     *
     * @param outputPath 输出文件路径
     * @param maxRegions 最大区域数
     * @param randomSeed 随机种子
     */
    public static void generateSqlScript(String outputPath, int maxRegions, long randomSeed) {
        List<TestCaseGenerator.TestCaseConfig> testCases = TestCaseGenerator.generateAllTestCases(maxRegions, randomSeed);

        try (FileWriter writer = new FileWriter(outputPath)) {
            writeHeader(writer);
            writeConfig(writer);
            writeTestCases(writer, testCases);
            writeValidation(writer);
            writeStatistics(writer, testCases);

            System.out.println("SQL脚本生成成功: " + outputPath);
            System.out.println("总用例数: " + testCases.size());
        } catch (IOException e) {
            System.err.println("生成SQL脚本失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 写入SQL脚本头部
     */
    private static void writeHeader(FileWriter writer) throws IOException {
        writer.write("-- ============================================\n");
        writer.write("-- 测试用例数据生成脚本（自动生成）\n");
        writer.write("-- 用途：在 cigarette_distribution_info 表中插入测试用例数据\n");
        writer.write("-- 时间分区：2099年9月第1周（避免与真实数据冲突）\n");
        writer.write("-- 生成策略：基于 Pairwise 组合测试设计 + 预投放量分层采样\n");
        writer.write("-- 生成时间：" + java.time.LocalDateTime.now() + "\n");
        writer.write("-- ============================================\n\n");

        writer.write("-- 清理该分区的旧测试数据（可选，谨慎使用）\n");
        writer.write(String.format("-- DELETE FROM cigarette_distribution_info WHERE YEAR = %d AND MONTH = %d AND WEEK_SEQ = %d;\n\n",
                TEST_YEAR, TEST_MONTH, TEST_WEEK_SEQ));
    }

    /**
     * 写入配置参数
     */
    private static void writeConfig(FileWriter writer) throws IOException {
        writer.write("-- ============================================\n");
        writer.write("-- 配置参数\n");
        writer.write("-- ============================================\n");
        writer.write(String.format("SET @YEAR = %d;\n", TEST_YEAR));
        writer.write(String.format("SET @MONTH = %d;\n", TEST_MONTH));
        writer.write(String.format("SET @WEEK_SEQ = %d;\n", TEST_WEEK_SEQ));
        writer.write(String.format("SET @SUPPLY_ATTRIBUTE = '%s';\n", SUPPLY_ATTRIBUTE));
        writer.write(String.format("SET @URS = %s;\n\n", URS));

        writer.write("-- 可用区域列表（按客户数从高到低排序）\n");
        for (int i = 0; i < REGIONS.length; i++) {
            writer.write(String.format("SET @REGION_%d = '%s';\n", i + 1, REGIONS[i]));
        }
        writer.write("\n");
    }

    /**
     * 写入测试用例INSERT语句
     */
    private static void writeTestCases(FileWriter writer, List<TestCaseGenerator.TestCaseConfig> testCases) throws IOException {
        writer.write("-- ============================================\n");
        writer.write("-- 测试用例数据\n");
        writer.write("-- ============================================\n\n");

        writer.write("INSERT INTO `cigarette_distribution_info`\n");
        writer.write("(YEAR, MONTH, WEEK_SEQ, CIG_CODE, CIG_NAME, SUPPLY_ATTRIBUTE, URS, DELIVERY_METHOD, DELIVERY_ETYPE, TAG, ADV, DELIVERY_AREA, BZ)\n");
        writer.write("VALUES\n");

        for (int i = 0; i < testCases.size(); i++) {
            TestCaseGenerator.TestCaseConfig config = testCases.get(i);
            boolean isLast = (i == testCases.size() - 1);

            String cigCode = String.format("TEST_%05d", i + 1);
            String cigName = buildCigName(config);
            String deliveryArea = buildDeliveryArea(config.regionCount, config.deliveryMethod, 
                    config.deliveryEtype, config.availableRegions);
            String bz = buildBz(config, i + 1);

            writer.write(String.format(
                    "(%d, %d, %d, '%s', '%s', '%s', %s, %s, %s, %s, %s, '%s', '%s')%s\n",
                    TEST_YEAR,
                    TEST_MONTH,
                    TEST_WEEK_SEQ,
                    cigCode,
                    cigName,
                    SUPPLY_ATTRIBUTE,
                    URS,
                    escapeSql(config.deliveryMethod),
                    escapeSql(config.deliveryEtype),
                    escapeSql(config.tag),
                    config.adv,
                    deliveryArea,
                    bz,
                    isLast ? ";" : ","
            ));
        }
        writer.write("\n");
    }

    /**
     * 构建卷烟名称
     */
    private static String buildCigName(TestCaseGenerator.TestCaseConfig config) {
        StringBuilder name = new StringBuilder("测试卷烟");
        name.append("_").append(config.deliveryMethod);
        if (config.deliveryEtype != null) {
            name.append("_").append(config.deliveryEtype.replace("+", "_"));
        }
        if (config.tag != null) {
            name.append("_").append("有标签");
        }
        return name.toString();
    }

    /**
     * 构建投放区域字符串
     * 
     * 业务规则：
     * - regionCount = 0：表示"全市"
     * - regionCount > 0：从该扩展类型组合对应的可用区域集合中随机抽取regionCount个区域
     * 
     * 注意：DELIVERY_AREA字段有长度限制（通常为255或500字符），对于双扩展类型的笛卡尔积区域集合，
     * 如果区域数量过多导致字符串过长，需要限制区域数量或截断。
     */
    private static String buildDeliveryArea(int regionCount, String deliveryMethod, 
                                            String deliveryEtype, List<String> availableRegions) {
        // 按档位投放和按价位段自选投放：固定为"全市"
        if ("按档位投放".equals(deliveryMethod) || "按价位段自选投放".equals(deliveryMethod)) {
            return "全市";
        }

        // regionCount = 0 也表示"全市"（按档位扩展投放的特殊情况）
        if (regionCount <= 0 || availableRegions == null || availableRegions.isEmpty()) {
            return "全市";
        }

        // 按档位扩展投放：从可用区域集合中随机抽取regionCount个区域
        int actualRegionCount = regionCount;
        if (regionCount >= availableRegions.size()) {
            actualRegionCount = availableRegions.size();
        }

        // 随机抽取actualRegionCount个区域
        List<String> shuffled = new ArrayList<>(availableRegions);
        Collections.shuffle(shuffled, new Random(42)); // 使用固定种子保证可重复性
        List<String> selected = shuffled.subList(0, actualRegionCount);
        Collections.sort(selected); // 排序以保证结果一致性
        
        // 使用中文逗号"、"分隔（与实际数据格式一致）
        String result = String.join("、", selected);
        
        // 限制最大长度为255字符（DELIVERY_AREA字段为VARCHAR(255)）
        // 如果超过限制，从后往前截断，保留尽可能多的区域
        int maxLength = 255;
        if (result.length() > maxLength) {
            // 从后往前截断，保留前面的区域
            while (result.length() > maxLength && selected.size() > 1) {
                selected.remove(selected.size() - 1);
                result = String.join("、", selected);
            }
            // 如果单个区域就超过限制，截断该区域字符串
            if (result.length() > maxLength && selected.size() == 1) {
                result = selected.get(0).substring(0, Math.min(maxLength, selected.get(0).length()));
            }
        }
        
        return result;
    }

    /**
     * 构建备注信息
     */
    private static String buildBz(TestCaseGenerator.TestCaseConfig config, int caseIndex) {
        StringBuilder bz = new StringBuilder("测试用例#").append(caseIndex);
        bz.append(": ").append(config.deliveryMethod);
        if (config.deliveryEtype != null) {
            bz.append("+").append(config.deliveryEtype);
        }
        if (config.tag != null) {
            bz.append("+标签(").append(config.tag).append(")");
        }
        bz.append(", ADV=").append(config.adv);
        if (config.regionCount == 0) {
            bz.append(", 区域=全市");
        } else {
            bz.append(", 区域数=").append(config.regionCount);
        }
        return bz.toString();
    }

    /**
     * SQL转义（处理NULL值）
     */
    private static String escapeSql(String value) {
        if (value == null) {
            return "NULL";
        }
        // 简单的SQL注入防护：转义单引号
        return "'" + value.replace("'", "''") + "'";
    }

    /**
     * 写入验证SQL
     */
    private static void writeValidation(FileWriter writer) throws IOException {
        writer.write("-- ============================================\n");
        writer.write("-- 验证插入结果\n");
        writer.write("-- ============================================\n");
        writer.write(String.format(
                "SELECT \n" +
                        "    COUNT(*) AS total_cases,\n" +
                        "    COUNT(DISTINCT DELIVERY_METHOD) AS delivery_methods,\n" +
                        "    COUNT(DISTINCT DELIVERY_ETYPE) AS extension_types,\n" +
                        "    COUNT(DISTINCT TAG) AS tags,\n" +
                        "    MIN(ADV) AS min_adv,\n" +
                        "    MAX(ADV) AS max_adv,\n" +
                        "    COUNT(DISTINCT DELIVERY_AREA) AS region_combinations\n" +
                        "FROM cigarette_distribution_info\n" +
                        "WHERE YEAR = %d AND MONTH = %d AND WEEK_SEQ = %d;\n\n",
                TEST_YEAR, TEST_MONTH, TEST_WEEK_SEQ
        ));
    }

    /**
     * 写入统计SQL
     */
    private static void writeStatistics(FileWriter writer, List<TestCaseGenerator.TestCaseConfig> testCases) throws IOException {
        writer.write("-- ============================================\n");
        writer.write("-- 按投放方式统计\n");
        writer.write("-- ============================================\n");
        writer.write(String.format(
                "SELECT \n" +
                        "    DELIVERY_METHOD,\n" +
                        "    DELIVERY_ETYPE,\n" +
                        "    COUNT(*) AS case_count,\n" +
                        "    MIN(ADV) AS min_adv,\n" +
                        "    MAX(ADV) AS max_adv\n" +
                        "FROM cigarette_distribution_info\n" +
                        "WHERE YEAR = %d AND MONTH = %d AND WEEK_SEQ = %d\n" +
                        "GROUP BY DELIVERY_METHOD, DELIVERY_ETYPE\n" +
                        "ORDER BY DELIVERY_METHOD, DELIVERY_ETYPE;\n\n",
                TEST_YEAR, TEST_MONTH, TEST_WEEK_SEQ
        ));

        writer.write("-- ============================================\n");
        writer.write("-- 按预投放量范围统计\n");
        writer.write("-- ============================================\n");
        writer.write(String.format(
                "SELECT \n" +
                        "    CASE \n" +
                        "        WHEN ADV < 1000 THEN '0-1K'\n" +
                        "        WHEN ADV < 5000 THEN '1K-5K'\n" +
                        "        WHEN ADV < 20000 THEN '5K-20K'\n" +
                        "        WHEN ADV < 100000 THEN '20K-100K'\n" +
                        "        ELSE '100K-150K'\n" +
                        "    END AS adv_range,\n" +
                        "    COUNT(*) AS case_count\n" +
                        "FROM cigarette_distribution_info\n" +
                        "WHERE YEAR = %d AND MONTH = %d AND WEEK_SEQ = %d\n" +
                        "GROUP BY adv_range\n" +
                        "ORDER BY MIN(ADV);\n",
                TEST_YEAR, TEST_MONTH, TEST_WEEK_SEQ
        ));
    }

    /**
     * 主方法：生成SQL脚本
     */
    public static void main(String[] args) {
        String outputPath = args.length > 0 ? args[0] : "test_cases_2099.sql";
        int maxRegions = args.length > 1 ? Integer.parseInt(args[1]) : 7;
        long randomSeed = args.length > 2 ? Long.parseLong(args[2]) : 42;

        System.out.println("开始生成SQL脚本...");
        System.out.println("输出路径: " + outputPath);
        System.out.println("最大区域数: " + maxRegions);
        System.out.println("随机种子: " + randomSeed);

        generateSqlScript(outputPath, maxRegions, randomSeed);
    }
}

