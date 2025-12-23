package org.example.application.service.calculate.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.domain.model.valueobject.RegionCustomerMatrix;
import org.example.application.service.calculate.PriceBandAllocationService;
import org.example.application.service.coordinator.CustomerMatrixBuilder;
import org.example.application.service.coordinator.PriceBandCandidateQueryService;
import org.example.application.service.writeback.PriceBandDistributionWriteBackService;
import org.example.domain.service.algorithm.PriceBandTruncationService;
import org.example.domain.service.algorithm.SingleLevelDistributionService;
import org.example.shared.util.WriteBackHelper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

/**
 * 按价位段自选投放（全市默认策略）分配服务实现。
 * <p>
 * 通过统一的分配编排引擎，基于价位段候选卷烟列表，完成按价位段自选投放场景下的
 * 单支初分配、价位段矩阵截断与微调，并将结果写回 {@code cigarette_distribution_prediction_price} 分区表。
 * </p>
 *
 * @author Robin
 * @since 2025-12-18
 */
@Slf4j
@Service
public class PriceBandAllocationServiceImpl implements PriceBandAllocationService {

    private final PriceBandCandidateQueryService priceBandCandidateQueryService;
    private final SingleLevelDistributionService singleLevelDistributionService;
    private final PriceBandTruncationService priceBandTruncationService;
    private final CustomerMatrixBuilder customerMatrixBuilder;
    private final PriceBandDistributionWriteBackService writeBackService;

    public PriceBandAllocationServiceImpl(
            PriceBandCandidateQueryService priceBandCandidateQueryService,
            SingleLevelDistributionService singleLevelDistributionService,
            PriceBandTruncationService priceBandTruncationService,
            CustomerMatrixBuilder customerMatrixBuilder,
            PriceBandDistributionWriteBackService writeBackService) {
        this.priceBandCandidateQueryService = priceBandCandidateQueryService;
        this.singleLevelDistributionService = singleLevelDistributionService;
        this.priceBandTruncationService = priceBandTruncationService;
        this.customerMatrixBuilder = customerMatrixBuilder;
        this.writeBackService = writeBackService;
    }

    /**
     * 执行价位段自选投放分配（已废弃，请使用带卷烟列表参数的重载方法）。
     * <p>
     * 此方法会自行查询候选卷烟列表，建议使用 {@link #allocateForPriceBand(List, Integer, Integer, Integer)}
     * 由 UnifiedAllocationService 统一查询后传入。
     * </p>
     *
     * @param year    年份，如 2025
     * @param month   月份，1-12
     * @param weekSeq 周序号，1-5
     * @deprecated 请使用 {@link #allocateForPriceBand(List, Integer, Integer, Integer)}
     * @example
     * <pre>{@code
     * // 已废弃用法
     * priceBandAllocationService.allocateForPriceBand(2025, 12, 4);
     * }</pre>
     */
    @Override
    @Deprecated
    public void allocateForPriceBand(Integer year, Integer month, Integer weekSeq) {
        log.info("Start price-band allocation for year={}, month={}, weekSeq={}", year, month, weekSeq);

        // 1) 获取候选卷烟列表（已按价位段升序、组内批发价降序排序）
        List<Map<String, Object>> candidates = priceBandCandidateQueryService.listOrderedPriceBandCandidates(year, month, weekSeq);
        if (candidates == null || candidates.isEmpty()) {
            log.info("价位段自选投放在 {}-{}-{} 分区无候选卷烟，跳过分配", year, month, weekSeq);
            return;
        }

        // 调用新方法处理
        allocateForPriceBand(candidates, year, month, weekSeq);
    }

    /**
     * 执行价位段自选投放分配。
     * <p>
     * 处理流程：
     * <ol>
     *   <li>补充价位段信息（从价目表获取 PRICE_BAND）</li>
     *   <li>构建"全市"区域客户矩阵（汇总所有区域的客户数）</li>
     *   <li>为每支卷烟执行 SingleLevel 初分配</li>
     *   <li>按价位段分组，筛选需要截断的价位段（组内卷烟数 > 1）</li>
     *   <li>对需要截断的价位段执行截断与微调</li>
     *   <li>将分配结果写回 cigarette_distribution_prediction_price 表</li>
     * </ol>
     * </p>
     *
     * @param cigaretteList 待分配的卷烟列表，每个 Map 包含 CIG_CODE、ADV 等字段
     * @param year          年份，如 2025
     * @param month         月份，1-12
     * @param weekSeq       周序号，1-5
     * @example
     * <pre>{@code
     * List<Map<String, Object>> priceBandCigs = Arrays.asList(
     *     Map.of("CIG_CODE", "C001", "ADV", new BigDecimal("1000")),
     *     Map.of("CIG_CODE", "C002", "ADV", new BigDecimal("2000"))
     * );
     * priceBandAllocationService.allocateForPriceBand(priceBandCigs, 2025, 12, 4);
     * }</pre>
     */
    @Override
    public int allocateForPriceBand(List<Map<String, Object>> cigaretteList, Integer year, Integer month, Integer weekSeq) {
        log.info("Start price-band allocation for year={}, month={}, weekSeq={}, cigarettes={}", 
                year, month, weekSeq, cigaretteList.size());

        if (cigaretteList == null || cigaretteList.isEmpty()) {
            log.info("价位段卷烟列表为空，跳过分配");
            return 0;
        }

        // 1) 补充价位段信息（从价目表获取 PRICE_BAND、BZ 等）
        List<Map<String, Object>> candidates = enrichWithPriceBand(cigaretteList, year, month, weekSeq);

        // 2) 构建"全市"区域客户矩阵（原始版本）
        RegionCustomerMatrix cityMatrixBase = customerMatrixBuilder.buildCityWideMatrix(year, month, weekSeq);
        if (cityMatrixBase.isEmpty()) {
            log.warn("region_customer_statistics 在 {}-{}-{} 分区无数据，无法执行价位段自选投放分配", year, month, weekSeq);
            return 0;
        }
        BigDecimal[] baseCustomerRow = cityMatrixBase.getRows().get(0).getGrades();

        // 3) 检查是否有卷烟需要两周一访上浮
        boolean anyNeedsBoost = checkIfAnyNeedsBoost(candidates);
        BigDecimal[] boostedCustomerRow = null;
        if (anyNeedsBoost) {
            // 构建上浮后的客户矩阵（传入任意一个需要上浮的卷烟备注即可）
            String boostRemark = extractBoostRemark(candidates);
            RegionCustomerMatrix cityMatrixBoosted = customerMatrixBuilder.buildWithBoost(
                    year, month, weekSeq, "全市",
                    "按价位段自选投放", null, null,
                    boostRemark, null);
            if (!cityMatrixBoosted.isEmpty()) {
                boostedCustomerRow = cityMatrixBoosted.getRows().get(0).getGrades();
                log.info("价位段分配：检测到需要两周一访上浮的卷烟，已准备上浮后客户数矩阵");
            }
        }

        List<String> targetRegions = Collections.singletonList("全市");

        // 4) 为每支候选卷烟执行 SingleLevel 初分配（根据备注选择客户数数组）
        executeSingleLevelAllocationWithBoost(candidates, targetRegions, baseCustomerRow, boostedCustomerRow);

        // 5) 按价位段分组，筛选需要截断的价位段
        Map<Integer, List<Map<String, Object>>> bandsNeedingTruncation = groupAndFilterBands(candidates);

        // 6) 对需要截断的价位段执行截断与微调（传入两份客户数数组和 GradeRange）
        priceBandTruncationService.truncateAndAdjust(bandsNeedingTruncation, baseCustomerRow, boostedCustomerRow, 
                org.example.domain.model.valueobject.GradeRange.full(), year, month, weekSeq);

        // 7) 将分配结果写回
        writeBackService.writeBackPriceBandAllocations(candidates, year, month, weekSeq, baseCustomerRow);

        log.info("Price-band allocation finished for year={}, month={}, weekSeq={}, processed={}", 
                year, month, weekSeq, candidates.size());
        return candidates.size();
    }

    /**
     * 补充价位段信息。
     * <p>
     * 从 PriceBandCandidateQueryService 获取完整的候选列表（包含 PRICE_BAND、WHOLESALE_PRICE 等），
     * 然后筛选出传入列表中存在的卷烟，保持排序顺序。
     * </p>
     *
     * @param cigaretteList 原始卷烟列表（来自 UnifiedAllocationService 的分流结果）
     * @param year          年份
     * @param month         月份
     * @param weekSeq       周序号
     * @return 补充了价位段信息的候选列表（已按价位段升序、组内批发价降序排序）
     */
    private List<Map<String, Object>> enrichWithPriceBand(List<Map<String, Object>> cigaretteList, 
                                                          Integer year, Integer month, Integer weekSeq) {
        // 获取完整的候选列表（包含 PRICE_BAND、WHOLESALE_PRICE 等，已排序）
        List<Map<String, Object>> orderedCandidates = priceBandCandidateQueryService.listOrderedPriceBandCandidates(year, month, weekSeq);
        
        if (orderedCandidates == null || orderedCandidates.isEmpty()) {
            log.warn("无法从 PriceBandCandidateQueryService 获取候选列表，使用原始列表");
            return cigaretteList;
        }
        
        // 构建传入列表的卷烟代码集合
        Set<String> inputCigCodes = new HashSet<>();
        for (Map<String, Object> cig : cigaretteList) {
            Object code = cig.get("CIG_CODE");
            if (code == null) {
                code = cig.get("cig_code");
            }
            if (code != null) {
                inputCigCodes.add(code.toString());
            }
        }
        
        // 筛选出传入列表中存在的卷烟，保持排序顺序
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> candidate : orderedCandidates) {
            Object code = candidate.get("CIG_CODE");
            if (code == null) {
                code = candidate.get("cig_code");
            }
            if (code != null && inputCigCodes.contains(code.toString())) {
                result.add(candidate);
            }
        }
        
        log.debug("价位段卷烟补充完成: 传入 {} 条, 匹配 {} 条", cigaretteList.size(), result.size());
        return result;
    }

    /**
     * 为每支候选卷烟执行 SingleLevel 初分配（支持两周一访上浮）。
     * <p>
     * 对每支卷烟根据其备注判断是否需要上浮：
     * - 如果备注包含"两周一访上浮100%"，使用上浮后的客户数
     * - 否则使用原始客户数
     * </p>
     *
     * @param candidates         候选卷烟列表
     * @param targetRegions      目标区域列表（价位段分配固定为 ["全市"]）
     * @param baseCustomerRow    原始客户数数组
     * @param boostedCustomerRow 上浮后客户数数组（可能为 null）
     */
    private void executeSingleLevelAllocationWithBoost(List<Map<String, Object>> candidates,
                                                       List<String> targetRegions,
                                                       BigDecimal[] baseCustomerRow,
                                                       BigDecimal[] boostedCustomerRow) {
        for (Map<String, Object> row : candidates) {
            BigDecimal adv = WriteBackHelper.toBigDecimal(row.get("ADV"));
            String cigCode = WriteBackHelper.getString(row, "CIG_CODE");
            String cigName = WriteBackHelper.getString(row, "CIG_NAME");
            String remark = WriteBackHelper.getString(row, "BZ");
            
            if (adv == null || adv.compareTo(BigDecimal.ZERO) <= 0) {
                row.put("GRADES", new BigDecimal[30]);
                continue;
            }

            // 根据备注选择使用哪个客户数数组
            boolean needsBoost = remark != null && remark.contains("两周一访上浮100%");
            BigDecimal[] customerRow = (needsBoost && boostedCustomerRow != null) ? boostedCustomerRow : baseCustomerRow;
            
            BigDecimal[][] regionCustomerMatrix = new BigDecimal[][]{customerRow};
            BigDecimal[][] allocation = singleLevelDistributionService.distribute(targetRegions, regionCustomerMatrix, adv, org.example.domain.model.valueobject.GradeRange.full());
            
            if (allocation == null || allocation.length == 0) {
                System.out.println("[价位段初分配] 卷烟=" + cigName + "(" + cigCode + "), ADV=" + adv + ", 分配结果为空数组!");
                row.put("GRADES", new BigDecimal[30]);
            } else {
                BigDecimal[] grades = allocation[0];
                if (grades == null || grades.length == 0) {
                    System.out.println("[价位段初分配] 卷烟=" + cigName + "(" + cigCode + "), ADV=" + adv + ", grades为空!");
                    row.put("GRADES", new BigDecimal[30]);
                } else {
                    row.put("GRADES", grades);
                }
            }
        }
    }

    /**
     * 按价位段分组，筛选需要截断的价位段。
     * <p>
     * 只有组内卷烟数 > 1 的价位段才需要执行截断与微调，
     * 单支卷烟的价位段直接使用初分配结果。
     * </p>
     *
     * @param candidates 候选卷烟列表（已完成初分配）
     * @return 需要截断的价位段映射，key 为价位段编号，value 为该价位段的卷烟列表
     */
    private Map<Integer, List<Map<String, Object>>> groupAndFilterBands(List<Map<String, Object>> candidates) {
        Map<Integer, List<Map<String, Object>>> bands = new TreeMap<>();
        for (Map<String, Object> row : candidates) {
            Object bandObj = row.get("PRICE_BAND");
            if (bandObj == null) {
                continue;
            }
            try {
                Integer band = Integer.valueOf(bandObj.toString());
                bands.computeIfAbsent(band, k -> new ArrayList<>()).add(row);
            } catch (NumberFormatException e) {
                // 忽略无效价位段
            }
        }

        Map<Integer, List<Map<String, Object>>> result = new TreeMap<>();
        for (Map.Entry<Integer, List<Map<String, Object>>> entry : bands.entrySet()) {
            if (entry.getValue().size() > 1) {
                result.put(entry.getKey(), entry.getValue());
            } else {
                log.debug("价位段 {} 仅包含 {} 支卷烟，跳过截断与微调", entry.getKey(), entry.getValue().size());
            }
        }
        return result;
    }

    /**
     * 检查是否有卷烟需要两周一访上浮。
     *
     * @param candidates 候选卷烟列表
     * @return 如果至少有一支卷烟的备注包含"两周一访上浮100%"，返回 true
     */
    private boolean checkIfAnyNeedsBoost(List<Map<String, Object>> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return false;
        }
        for (Map<String, Object> candidate : candidates) {
            String remark = WriteBackHelper.getString(candidate, "BZ");
            if (remark != null && remark.contains("两周一访上浮100%")) {
                return true;
            }
        }
        return false;
    }

    /**
     * 提取需要两周一访上浮的卷烟备注。
     * <p>
     * 用于调用 buildWithBoost 方法时传入，只需要找到一个包含关键字的备注即可。
     * </p>
     *
     * @param candidates 候选卷烟列表
     * @return 包含"两周一访上浮100%"的备注字符串，未找到时返回 null
     */
    private String extractBoostRemark(List<Map<String, Object>> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        for (Map<String, Object> candidate : candidates) {
            String remark = WriteBackHelper.getString(candidate, "BZ");
            if (remark != null && remark.contains("两周一访上浮100%")) {
                return remark;
            }
        }
        return null;
    }

}
