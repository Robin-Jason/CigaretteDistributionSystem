package org.example.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.mapper.TemporaryCustomerTableMapper;
import org.example.service.BiWeeklyVisitBoostService;
import org.example.service.TagExtractionService;
import org.example.service.util.CombinationStrategyAnalyzer;
import org.example.service.util.OrderCycleMatrixCalculator;
import org.example.service.model.tag.TagFilterRule;
import org.example.strategy.orchestrator.RegionCustomerMatrix;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;

/**
 * “两周一访上浮100%”双周/单周客户上浮服务实现类。
 *
 * @author Robin
 * @version 1.0
 * @since 2025-12-10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BiWeeklyVisitBoostServiceImpl implements BiWeeklyVisitBoostService {

    private static final String BI_WEEKLY_PHRASE = "两周一访上浮100%";
    private static final String TEMP_TABLE_PREFIX = "temp_customer_filter_";

    private final TemporaryCustomerTableMapper temporaryCustomerTableMapper;
    private final TagExtractionService tagExtractionService;
    private final CombinationStrategyAnalyzer strategyAnalyzer;
    private final OrderCycleMatrixCalculator matrixCalculator;

    /**
     * 对卷烟的客户矩阵执行“两周一访上浮100%”：
     * - 备注需包含关键字
     * - 临时表存在且含单周/双周客户
     * - 不支持的投放方式直接跳过
     *
     * @param baseMatrix     原始区域客户矩阵
     * @param year           年（如：2025）
     * @param month          月（1-12）
     * @param weekSeq        周序号（1-4）
     * @param deliveryMethod 投放方式（如："按档位投放"、"按价位段自选投放"）
     * @param deliveryEtype 扩展投放类型（如："区县公司+市场类型"）
     * @param tag            标签（可选）
     * @param deliveryArea   投放区域（可选）
     * @param remark         备注（需包含"两周一访上浮100%"关键字）
     * @param extraInfo      额外信息（可包含 TAG_FILTER_CONFIG）
     * @return 处理后的矩阵，未触发则返回原矩阵
     * @example
     * <pre>
     *     RegionCustomerMatrix matrix = new RegionCustomerMatrix();
     *     // ... 初始化矩阵
     *     Map<String, Object> extraInfo = new HashMap<>();
     *     extraInfo.put("TAG_FILTER_CONFIG", "{\"tag1\": {...}}");
     *     RegionCustomerMatrix boosted = applyBiWeeklyBoostIfNeeded(
     *         matrix, 2025, 9, 3, "按档位投放", null, null, null,
     *         "两周一访上浮100%", extraInfo
     *     );
     *     // 如果备注包含关键字且临时表存在单周/双周客户，则矩阵中的客户数会上浮100%
     * </pre>
     */
    @Override
    public RegionCustomerMatrix applyBiWeeklyBoostIfNeeded(
            RegionCustomerMatrix baseMatrix,
            Integer year,
            Integer month,
            Integer weekSeq,
            String deliveryMethod,
            String deliveryEtype,
            String tag,
            String deliveryArea,
            String remark,
            Map<String, Object> extraInfo) {

        if (baseMatrix == null || baseMatrix.isEmpty()) {
            log.debug("两周一访上浮100%：矩阵为空，跳过上浮处理。year={}, month={}, weekSeq={}", year, month, weekSeq);
            return baseMatrix;
        }

        if (!needsBoost(remark)) {
            return baseMatrix;
        }

        String temporaryTable = buildTemporaryTableName(year, month, weekSeq);
        Long exists = temporaryCustomerTableMapper.tableExists(temporaryTable);
        if (exists == null || exists == 0L) {
            log.warn("两周一访上浮100%：临时表 {} 不存在，无法执行上浮", temporaryTable);
            return baseMatrix;
        }

        Set<OrderCycleMatrixCalculator.OrderCycleType> boostTypes = detectBoostTypes(temporaryTable);
        if (boostTypes.isEmpty()) {
            log.info("两周一访上浮100%：临时表 {} 中不存在单周/双周客户，跳过上浮", temporaryTable);
            return baseMatrix;
        }

        CombinationStrategyAnalyzer.CombinationStrategy strategy = strategyAnalyzer.analyzeCombination(deliveryMethod, deliveryEtype);
        if (strategy.mode == CombinationStrategyAnalyzer.CombinationMode.SKIPPED) {
            log.info("两周一访上浮100%：投放类型 {} 当前不支持上浮，跳过", deliveryMethod);
            return baseMatrix;
        }

        List<TagFilterRule> tagRules = resolveTagRules(tag, extraInfo);

        Map<String, RegionCustomerMatrix.Row> rowIndex = indexRows(baseMatrix);
        for (OrderCycleMatrixCalculator.OrderCycleType type : boostTypes) {
            Map<String, BigDecimal[]> increments = matrixCalculator.calculateOrderCycleMatrix(
                    strategy, temporaryTable, tagRules, type);
            applyBoost(rowIndex, increments, type);
        }

        log.info("两周一访上浮100%：上浮完成，year={}, month={}, weekSeq={}, boostTypes={}", year, month, weekSeq, boostTypes);
        return baseMatrix;
    }

    /**
     * 判断备注中是否包含“两周一访上浮100%”关键字。
     *
     * @param remark 备注字符串
     * @return 如果备注包含"两周一访上浮100%"关键字则返回 true，否则返回 false
     * @example
     * <pre>
     *     boolean needs = needsBoost("两周一访上浮100%"); // true
     *     boolean needs2 = needsBoost("普通备注"); // false
     *     boolean needs3 = needsBoost(null); // false
     * </pre>
     */
    private boolean needsBoost(String remark) {
        if (remark == null) {
            return false;
        }
        String normalized = remark.replace(" ", "");
        return !normalized.isEmpty() && normalized.contains(BI_WEEKLY_PHRASE);
    }

    /**
     * 构造本周的临时表名。
     *
     * @param year    年份（如：2025）
     * @param month   月份（1-12）
     * @param weekSeq 周序号（1-4）
     * @return 临时表名，格式为 "temp_customer_filter_{year}_{month}_{weekSeq}"
     * @example
     * <pre>
     *     String tableName = buildTemporaryTableName(2025, 9, 3);
     *     // 返回: "temp_customer_filter_2025_9_3"
     * </pre>
     */
    private String buildTemporaryTableName(Integer year, Integer month, Integer weekSeq) {
        return TEMP_TABLE_PREFIX + year + "_" + month + "_" + weekSeq;
    }

    /**
     * 从临时表扫描订单周期，判断是否存在单周/双周客户。
     *
     * @param tableName 临时表名（如："temp_customer_filter_2025_9_3"）
     * @return 检测到的周期类型集合，可能包含 OrderCycleType.SINGLE（单周）和/或 OrderCycleType.DOUBLE（双周）
     * @example
     * <pre>
     *     Set<OrderCycleMatrixCalculator.OrderCycleType> types = detectBoostTypes("temp_customer_filter_2025_9_3");
     *     // 如果临时表中存在"单周"订单周期，则 types 包含 SINGLE
     *     // 如果临时表中存在"双周"订单周期，则 types 包含 DOUBLE
     * </pre>
     */
    private Set<OrderCycleMatrixCalculator.OrderCycleType> detectBoostTypes(String tableName) {
        List<String> cycles = temporaryCustomerTableMapper.listOrderCycles(tableName);
        Set<OrderCycleMatrixCalculator.OrderCycleType> types = EnumSet.noneOf(OrderCycleMatrixCalculator.OrderCycleType.class);
        cycles.stream()
                .filter(Objects::nonNull)
                .forEach(cycle -> {
                    if (cycle.contains("单周")) {
                        types.add(OrderCycleMatrixCalculator.OrderCycleType.SINGLE);
                    }
                    if (cycle.contains("双周")) {
                        types.add(OrderCycleMatrixCalculator.OrderCycleType.DOUBLE);
                    }
                });
        return types;
    }

    /**
     * 将矩阵行按区域名索引。
     *
     * @param matrix 区域客户矩阵
     * @return 区域名称到矩阵行的映射（LinkedHashMap，保持插入顺序）
     * @example
     * <pre>
     *     RegionCustomerMatrix matrix = new RegionCustomerMatrix();
     *     // ... 添加行数据
     *     Map<String, RegionCustomerMatrix.Row> index = indexRows(matrix);
     *     // index.get("全市") 返回对应的行对象
     * </pre>
     */
    private Map<String, RegionCustomerMatrix.Row> indexRows(RegionCustomerMatrix matrix) {
        Map<String, RegionCustomerMatrix.Row> map = new LinkedHashMap<>();
        for (RegionCustomerMatrix.Row row : matrix.getRows()) {
            map.put(row.getRegion(), row);
        }
        return map;
    }

    /**
     * 将增量叠加到目标区域行。
     * <p>
     * 将单周/双周客户的增量客户数叠加到原始矩阵的对应区域行中。
     * </p>
     *
     * @param rowIndex   区域行索引（区域名 -> 矩阵行）
     * @param increments 区域增量映射（区域名 -> 30档位客户数增量数组）
     * @param type       单周/双周类型（用于日志记录）
     * @example
     * <pre>
     *     Map<String, RegionCustomerMatrix.Row> rowIndex = indexRows(matrix);
     *     Map<String, BigDecimal[]> increments = new HashMap<>();
     *     BigDecimal[] grades = new BigDecimal[30];
     *     grades[0] = BigDecimal.valueOf(10); // D30档位增加10个客户
     *     increments.put("全市", grades);
     *     applyBoost(rowIndex, increments, OrderCycleMatrixCalculator.OrderCycleType.SINGLE);
     *     // 矩阵中"全市"区域的D30档位客户数会增加10
     * </pre>
     */
    private void applyBoost(Map<String, RegionCustomerMatrix.Row> rowIndex,
                            Map<String, BigDecimal[]> increments,
                            OrderCycleMatrixCalculator.OrderCycleType type) {
        if (increments.isEmpty()) {
            log.info("两周一访上浮100%：{} 客户未匹配到任何区域，跳过叠加", type);
            return;
        }
        for (Map.Entry<String, BigDecimal[]> entry : increments.entrySet()) {
            RegionCustomerMatrix.Row row = rowIndex.get(entry.getKey());
            if (row == null) {
                log.debug("两周一访上浮100%：区域 {} 未在客户矩阵中出现，跳过叠加", entry.getKey());
                continue;
            }
            BigDecimal[] grades = row.getGrades();
            BigDecimal[] addition = entry.getValue();
            ensureLength(grades);
            ensureLength(addition);
            for (int i = 0; i < grades.length; i++) {
                BigDecimal base = grades[i] == null ? BigDecimal.ZERO : grades[i];
                BigDecimal delta = addition[i] == null ? BigDecimal.ZERO : addition[i];
                grades[i] = base.add(delta);
            }
            log.debug("两周一访上浮100%：区域 {} 叠加 {} 客户数成功", entry.getKey(), type);
        }
    }

    /**
     * 校验档位数组长度。
     * <p>
     * 确保档位数组长度至少为30，否则抛出异常。
     * </p>
     *
     * @param grades 档位数组（30个档位的客户数）
     * @throws IllegalStateException 如果数组长度小于30
     * @example
     * <pre>
     *     BigDecimal[] grades = new BigDecimal[30];
     *     ensureLength(grades); // 正常通过
     *     BigDecimal[] shortGrades = new BigDecimal[20];
     *     ensureLength(shortGrades); // 抛出 IllegalStateException
     * </pre>
     */
    private void ensureLength(BigDecimal[] grades) {
        if (grades == null) {
            return;
        }
        if (grades.length < 30) {
            throw new IllegalStateException("档位数组长度不足30位");
        }
    }

    /**
     * 解析标签过滤规则。
     * <p>
     * 从tag字符串和extraInfo中提取标签信息，调用TagExtractionService解析为标签过滤规则列表。
     * </p>
     *
     * @param tag      标签字符串（可选）
     * @param extraInfo 额外信息Map（可包含TAG_FILTER_CONFIG）
     * @return 标签过滤规则列表
     * @example
     * <pre>
     *     Map<String, Object> extraInfo = new HashMap<>();
     *     extraInfo.put("TAG_FILTER_CONFIG", "{\"tag1\": {...}}");
     *     List<TagFilterRule> rules = resolveTagRules("tag1", extraInfo);
     *     // 返回: [TagFilterRule对象列表]
     * </pre>
     */
    private List<TagFilterRule> resolveTagRules(String tag, Map<String, Object> extraInfo) {
        Map<String, Object> info = new HashMap<>();
        if (StringUtils.hasText(tag)) {
            info.put("TAG", tag);
        }
        if (extraInfo != null && extraInfo.containsKey("TAG_FILTER_CONFIG")) {
            info.put("TAG_FILTER_CONFIG", extraInfo.get("TAG_FILTER_CONFIG"));
        }
        return tagExtractionService.resolveTagFilters(info);
    }
}

