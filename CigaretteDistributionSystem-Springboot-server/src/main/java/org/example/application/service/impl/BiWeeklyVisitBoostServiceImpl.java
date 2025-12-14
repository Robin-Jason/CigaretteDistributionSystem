package org.example.application.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.domain.repository.TemporaryCustomerTableRepository;
import org.example.application.service.BiWeeklyVisitBoostService;
import org.example.application.service.TagExtractionService;
import org.example.domain.service.rule.BiWeeklyVisitBoostRule;
import org.example.shared.constants.BusinessConstants;
import org.example.shared.constants.TableConstants;
import org.example.shared.util.CombinationStrategyAnalyzer;
import org.example.shared.util.OrderCycleMatrixCalculator;
import org.example.domain.model.tag.TagFilterRule;
import org.example.application.orchestrator.RegionCustomerMatrix;
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


    private static final BiWeeklyVisitBoostRule BOOST_RULE = new org.example.domain.service.rule.impl.BiWeeklyVisitBoostRuleImpl();

    private final TemporaryCustomerTableRepository temporaryCustomerTableRepository;
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

        if (!BOOST_RULE.needsBoost(remark)) {
            return baseMatrix;
        }

        String temporaryTable = TableConstants.TEMP_CUSTOMER_FILTER_PREFIX + year + "_" + month + "_" + weekSeq;
        Long exists = temporaryCustomerTableRepository.tableExists(temporaryTable);
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

        // 将RegionCustomerMatrix.Row转换为BiWeeklyVisitBoostRule.MatrixRow（使用原始数组引用，直接修改）
        Map<String, RegionCustomerMatrix.Row> rowIndex = new LinkedHashMap<>();
        for (RegionCustomerMatrix.Row row : baseMatrix.getRows()) {
            rowIndex.put(row.getRegion(), row);
        }
        
        for (OrderCycleMatrixCalculator.OrderCycleType type : boostTypes) {
            Map<String, BigDecimal[]> increments = matrixCalculator.calculateOrderCycleMatrix(
                    strategy, temporaryTable, tagRules, type);
            
            // 应用上浮规则：直接操作原始矩阵
            for (Map.Entry<String, BigDecimal[]> entry : increments.entrySet()) {
                RegionCustomerMatrix.Row row = rowIndex.get(entry.getKey());
                if (row == null) {
                    log.debug("两周一访上浮100%：区域 {} 未在客户矩阵中出现，跳过叠加", entry.getKey());
                    continue;
                }
                BigDecimal[] grades = row.getGrades();
                BigDecimal[] addition = entry.getValue();
                BOOST_RULE.ensureLength(grades);
                BOOST_RULE.ensureLength(addition);
                for (int i = 0; i < grades.length && i < addition.length; i++) {
                    BigDecimal base = grades[i] == null ? BigDecimal.ZERO : grades[i];
                    BigDecimal delta = addition[i] == null ? BigDecimal.ZERO : addition[i];
                    grades[i] = base.add(delta);
                }
                log.debug("两周一访上浮100%：区域 {} 叠加 {} 客户数成功", entry.getKey(), type);
            }
        }

        log.info("两周一访上浮100%：上浮完成，year={}, month={}, weekSeq={}, boostTypes={}", year, month, weekSeq, boostTypes);
        return baseMatrix;
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
        List<String> cycles = temporaryCustomerTableRepository.listOrderCycles(tableName);
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

