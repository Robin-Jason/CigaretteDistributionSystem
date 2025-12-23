package org.example.application.service.adjust.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.application.dto.allocation.AdjustCigaretteStrategyRequestDto;
import org.example.application.dto.allocation.AdjustCigaretteStrategyResponseDto;
import org.example.domain.model.valueobject.RegionCustomerMatrix;
import org.example.application.service.adjust.CigaretteStrategyAdjustService;
import org.example.application.service.coordinator.RegionCustomerStatisticsBuildService;
import org.example.application.service.coordinator.CustomerMatrixBuilder;
import org.example.application.service.coordinator.AllocationAlgorithmSelector;
import org.example.application.service.writeback.StandardDistributionWriteBackService;
import org.example.application.service.writeback.PriceBandDistributionWriteBackService;
import org.example.domain.repository.CigaretteDistributionInfoRepository;
import org.example.domain.repository.CigaretteDistributionPredictionRepository;
import org.example.domain.repository.CigaretteDistributionPredictionPriceRepository;
import org.example.domain.repository.RegionCustomerStatisticsRepository;
import org.example.infrastructure.persistence.po.CigaretteDistributionInfoPO;
import org.example.shared.util.PartitionTableManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;

/**
 * 卷烟投放策略调整服务实现类
 *
 * @author Robin
 * @since 2025-12-22
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CigaretteStrategyAdjustServiceImpl implements CigaretteStrategyAdjustService {

    private final CigaretteDistributionInfoRepository infoRepository;
    private final CigaretteDistributionPredictionRepository predictionRepository;
    private final CigaretteDistributionPredictionPriceRepository predictionPriceRepository;
    private final RegionCustomerStatisticsRepository regionCustomerStatisticsRepository;
    private final RegionCustomerStatisticsBuildService regionCustomerStatisticsBuildService;
    private final CustomerMatrixBuilder customerMatrixBuilder;
    private final AllocationAlgorithmSelector allocationAlgorithmSelector;
    private final StandardDistributionWriteBackService standardWriteBackService;
    private final PriceBandDistributionWriteBackService priceBandWriteBackService;
    private final PartitionTableManager partitionTableManager;
    private final JdbcTemplate jdbcTemplate;

    private static final String PRICE_BAND_DELIVERY_METHOD = "按价位段自选投放";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdjustCigaretteStrategyResponseDto adjustStrategy(AdjustCigaretteStrategyRequestDto request) {
        log.info("开始调整卷烟投放策略: {}-{}-{}, 卷烟: {}-{}, 新投放类型: {}", 
                request.getYear(), request.getMonth(), request.getWeekSeq(),
                request.getCigCode(), request.getCigName(), request.getNewDeliveryMethod());

        try {
            // 1. 参数校验
            validateRequest(request);

            // 2. 查询卷烟信息并校验存在性
            Map<String, Object> cigaretteInfo = queryCigaretteInfo(request);

            // 3. 标签校验（若有新标签）
            if (StringUtils.hasText(request.getNewTag())) {
                validateTagExists(request);
            }

            // 4. 获取原投放类型，判断数据所在表
            String originalDeliveryMethod = (String) cigaretteInfo.get("DELIVERY_METHOD");
            boolean originalIsPriceBand = PRICE_BAND_DELIVERY_METHOD.equals(originalDeliveryMethod);
            boolean newIsPriceBand = PRICE_BAND_DELIVERY_METHOD.equals(request.getNewDeliveryMethod());

            // 5. 删除旧记录（仅当原表和新表不同时才需要手动删除原表数据）
            // 注意：writeBackSingleCigarette 内部会删除新表中的旧数据，所以只需处理跨表情况
            if (originalIsPriceBand != newIsPriceBand) {
                deleteOldRecords(request, originalIsPriceBand);
            }

            // 6. 补充区域统计数据（如果需要）
            ensureRegionCustomerStatistics(request);

            // 7. 执行分配算法
            AllocationAlgorithmSelector.AllocationResult calcResult = executeAllocation(request, cigaretteInfo);
            if (!calcResult.isSuccess()) {
                return AdjustCigaretteStrategyResponseDto.failure("分配计算失败: " + calcResult.getMessage());
            }

            // 8. 写回新记录（内部会先删除新表中的旧数据再插入）
            writeBackNewRecords(request, calcResult, newIsPriceBand, cigaretteInfo);

            // 9. 更新 Info 表备注
            updateInfoRemark(request);

            // 10. 查询并返回新分配记录
            List<Map<String, Object>> newRecords = queryNewAllocationRecords(request, newIsPriceBand);

            String message = String.format("卷烟 %s(%s) 投放策略调整成功，生成 %d 条分配记录",
                    request.getCigName(), request.getCigCode(), newRecords.size());
            log.info(message);

            return AdjustCigaretteStrategyResponseDto.success(newRecords, message);

        } catch (IllegalArgumentException e) {
            log.warn("参数校验失败: {}", e.getMessage());
            return AdjustCigaretteStrategyResponseDto.failure(e.getMessage());
        } catch (Exception e) {
            log.error("调整卷烟投放策略失败", e);
            throw new RuntimeException("调整卷烟投放策略失败: " + e.getMessage(), e);
        }
    }


    /**
     * 参数校验
     */
    private void validateRequest(AdjustCigaretteStrategyRequestDto request) {
        if (request.getYear() == null || request.getYear() < 2020 || request.getYear() > 2100) {
            throw new IllegalArgumentException("年份无效，应为2020-2100之间的整数");
        }
        if (request.getMonth() == null || request.getMonth() < 1 || request.getMonth() > 12) {
            throw new IllegalArgumentException("月份无效，应为1-12之间的整数");
        }
        if (request.getWeekSeq() == null || request.getWeekSeq() < 1 || request.getWeekSeq() > 5) {
            throw new IllegalArgumentException("周序号无效，应为1-5之间的整数");
        }
        if (!StringUtils.hasText(request.getCigCode())) {
            throw new IllegalArgumentException("卷烟代码不能为空");
        }
        if (!StringUtils.hasText(request.getCigName())) {
            throw new IllegalArgumentException("卷烟名称不能为空");
        }
        if (!StringUtils.hasText(request.getNewDeliveryMethod())) {
            throw new IllegalArgumentException("新投放类型不能为空");
        }
        if (request.getNewAdvAmount() == null || request.getNewAdvAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("新建议投放量必须大于0");
        }
        // 如果有新标签，必须有过滤值
        if (StringUtils.hasText(request.getNewTag()) && !StringUtils.hasText(request.getNewTagFilterValue())) {
            throw new IllegalArgumentException("指定新标签时，标签过滤值不能为空");
        }
    }

    /**
     * 查询卷烟信息并校验存在性
     */
    private Map<String, Object> queryCigaretteInfo(AdjustCigaretteStrategyRequestDto request) {
        partitionTableManager.ensurePartitionExists("cigarette_distribution_info",
                request.getYear(), request.getMonth(), request.getWeekSeq());

        QueryWrapper<CigaretteDistributionInfoPO> query = new QueryWrapper<>();
        query.eq("YEAR", request.getYear())
                .eq("MONTH", request.getMonth())
                .eq("WEEK_SEQ", request.getWeekSeq())
                .eq("CIG_CODE", request.getCigCode())
                .eq("CIG_NAME", request.getCigName());

        List<Map<String, Object>> results = infoRepository.selectMaps(query);
        if (results.isEmpty()) {
            throw new IllegalArgumentException(String.format(
                    "卷烟不存在：代码[%s]、名称[%s]在批次[%d年%d月第%d周]的投放信息表中未找到",
                    request.getCigCode(), request.getCigName(),
                    request.getYear(), request.getMonth(), request.getWeekSeq()));
        }
        return results.get(0);
    }

    /**
     * 固定标签名称到列名的映射
     */
    private static final Map<String, String> FIXED_TAG_COLUMN_MAP = new HashMap<>();
    static {
        FIXED_TAG_COLUMN_MAP.put("优质数据共享客户", "QUALITY_DATA_SHARE");
    }

    /**
     * 校验标签是否存在于 customer_filter 表中
     * <p>
     * 支持两种标签类型：
     * 1. 固定标签（如"优质数据共享客户"）→ 检查对应的固定列（QUALITY_DATA_SHARE）
     * 2. 动态标签 → 检查 DYNAMIC_TAGS JSON 字段
     * </p>
     */
    private void validateTagExists(AdjustCigaretteStrategyRequestDto request) {
        String tagName = request.getNewTag();
        
        // 检查是否为固定标签
        String fixedColumn = FIXED_TAG_COLUMN_MAP.get(tagName);
        if (fixedColumn != null) {
            // 固定标签：检查对应列是否有非空值
            String sql = "SELECT COUNT(*) FROM customer_filter " +
                    "WHERE YEAR = ? AND MONTH = ? AND WEEK_SEQ = ? " +
                    "AND " + fixedColumn + " IS NOT NULL AND " + fixedColumn + " != ''";
            
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class,
                    request.getYear(), request.getMonth(), request.getWeekSeq());
            
            if (count == null || count == 0) {
                throw new IllegalArgumentException(String.format(
                        "标签[%s]不存在：在批次[%d年%d月第%d周]的客户数据中未找到该固定标签",
                        tagName, request.getYear(), request.getMonth(), request.getWeekSeq()));
            }
            log.debug("固定标签[{}]校验通过，存在于 {} 条客户记录中", tagName, count);
        } else {
            // 动态标签：检查 DYNAMIC_TAGS JSON 字段
            // JSON路径需要用引号包裹中文键名
            String jsonPath = "$." + "\"" + tagName + "\"";
            String sql = "SELECT COUNT(*) FROM customer_filter " +
                    "WHERE YEAR = ? AND MONTH = ? AND WEEK_SEQ = ? " +
                    "AND JSON_EXTRACT(DYNAMIC_TAGS, ?) IS NOT NULL";

            Integer count = jdbcTemplate.queryForObject(sql, Integer.class,
                    request.getYear(), request.getMonth(), request.getWeekSeq(), jsonPath);

            if (count == null || count == 0) {
                throw new IllegalArgumentException(String.format(
                        "标签[%s]不存在：在批次[%d年%d月第%d周]的客户数据中未找到该动态标签",
                        tagName, request.getYear(), request.getMonth(), request.getWeekSeq()));
            }
            log.debug("动态标签[{}]校验通过，存在于 {} 条客户记录中", tagName, count);
        }
    }

    /**
     * 删除旧的分配记录
     */
    private void deleteOldRecords(AdjustCigaretteStrategyRequestDto request, boolean isPriceBand) {
        int deletedCount;
        if (isPriceBand) {
            deletedCount = predictionPriceRepository.deleteByCig(
                    request.getYear(), request.getMonth(), request.getWeekSeq(),
                    request.getCigCode(), request.getCigName());
            log.info("删除 prediction_price 表旧记录: {} 条", deletedCount);
        } else {
            deletedCount = predictionRepository.deleteByCigarette(
                    request.getYear(), request.getMonth(), request.getWeekSeq(),
                    request.getCigCode(), request.getCigName());
            log.info("删除 prediction 表旧记录: {} 条", deletedCount);
        }
    }

    /**
     * 确保区域客户统计数据存在，如果不存在则追加构建
     */
    private void ensureRegionCustomerStatistics(AdjustCigaretteStrategyRequestDto request) {
        // 检查是否已有区域统计数据
        partitionTableManager.ensurePartitionExists("region_customer_statistics",
                request.getYear(), request.getMonth(), request.getWeekSeq());

        List<Map<String, Object>> existingStats = regionCustomerStatisticsRepository.findAll(
                request.getYear(), request.getMonth(), request.getWeekSeq());

        if (existingStats.isEmpty()) {
            log.info("区域客户统计数据不存在，开始构建: {}-{}-{}",
                    request.getYear(), request.getMonth(), request.getWeekSeq());
            regionCustomerStatisticsBuildService.buildRegionCustomerStatistics(
                    request.getYear(), request.getMonth(), request.getWeekSeq());
        } else {
            log.debug("区域客户统计数据已存在，共 {} 条记录", existingStats.size());
        }
    }


    /**
     * 执行分配算法
     */
    private AllocationAlgorithmSelector.AllocationResult executeAllocation(AdjustCigaretteStrategyRequestDto request,
                                                          Map<String, Object> cigaretteInfo) {
        // 构建投放区域（使用用户传入的区域列表）
        String deliveryArea = String.join(",", request.getNewDeliveryAreas());

        // 构建备注
        String remark = buildRemark(request);

        log.debug("执行分配: deliveryArea={}, tag={}", deliveryArea, request.getNewTag());

        // 1. 构建客户矩阵
        Map<String, Object> extraInfo = new HashMap<>();
        
        // 如果用户传入了市场类型比例，添加到 extraInfo
        if (request.getUrbanRatio() != null && request.getRuralRatio() != null) {
            extraInfo.put("urbanRatio", request.getUrbanRatio());
            extraInfo.put("ruralRatio", request.getRuralRatio());
            log.debug("使用用户指定的市场类型比例 - 城网: {}, 农网: {}", 
                    request.getUrbanRatio(), request.getRuralRatio());
        }
        
        RegionCustomerMatrix customerMatrix = customerMatrixBuilder.buildWithBoost(
                request.getYear(), request.getMonth(), request.getWeekSeq(),
                deliveryArea, request.getNewDeliveryMethod(), request.getNewDeliveryEtype(),
                request.getNewTag(), remark, extraInfo);

        if (customerMatrix == null || customerMatrix.isEmpty()) {
            return AllocationAlgorithmSelector.AllocationResult.failure("未找到匹配的投放区域");
        }

        // 2. 执行分配算法
        // 使用用户指定的 HG/LG，如果未指定则使用默认值 D30/D1
        String maxGrade = StringUtils.hasText(request.getNewHighestGrade()) 
                ? request.getNewHighestGrade() : "D30";
        String minGrade = StringUtils.hasText(request.getNewLowestGrade()) 
                ? request.getNewLowestGrade() : "D1";
        
        log.debug("使用档位范围: HG={}, LG={}", maxGrade, minGrade);
        
        return allocationAlgorithmSelector.execute(
                customerMatrix,
                request.getNewAdvAmount(),
                request.getNewDeliveryMethod(),
                request.getNewDeliveryEtype(),
                request.getNewTag(),
                maxGrade,
                minGrade,
                null,   // groupRatios
                null    // regionGroupMapping
        );
    }

    /**
     * 构建 TAG_FILTER_CONFIG JSON
     */
    private String buildTagFilterConfig(AdjustCigaretteStrategyRequestDto request) {
        if (!StringUtils.hasText(request.getNewTag())) {
            return null;
        }
        // 格式: {"标签名": {"column": "标签名", "operator": "=", "value": "过滤值", "valueType": "STRING"}}
        return String.format("{\"%s\": {\"column\": \"%s\", \"operator\": \"=\", \"value\": \"%s\", \"valueType\": \"STRING\"}}",
                request.getNewTag(), request.getNewTag(), request.getNewTagFilterValue());
    }

    /**
     * 构建备注（已人工调整策略?）
     */
    private String buildRemark(AdjustCigaretteStrategyRequestDto request) {
        // 如果用户指定了备注，直接使用
        if (StringUtils.hasText(request.getNewRemark())) {
            return request.getNewRemark();
        }
        
        // 否则自动生成备注
        StringBuilder sb = new StringBuilder("已人工调整策略");
        if (StringUtils.hasText(request.getNewDeliveryEtype())) {
            // 有扩展投放类型：扩展类型+标签
            sb.append(request.getNewDeliveryEtype());
        } else {
            // 无扩展投放类型：投放类型
            sb.append(request.getNewDeliveryMethod());
        }
        if (StringUtils.hasText(request.getNewTag())) {
            sb.append("（").append(request.getNewTag()).append("）");
        }
        return sb.toString();
    }

    /**
     * 写回新分配记录
     */
    private void writeBackNewRecords(AdjustCigaretteStrategyRequestDto request,
                                     AllocationAlgorithmSelector.AllocationResult calcResult,
                                     boolean isPriceBand,
                                     Map<String, Object> cigaretteInfo) {
        String tagFilterConfig = buildTagFilterConfig(request);
        // 预测表记录的备注：人工已确认投放组合与投放区域修改
        String predictionRemark = "人工已确认投放组合与投放区域修改";

        if (isPriceBand) {
            // 写入 prediction_price 表
            priceBandWriteBackService.writeBackPriceBandAllocations(
                    buildCandidatesFromResult(calcResult, request, cigaretteInfo, predictionRemark),
                    request.getYear(),
                    request.getMonth(),
                    request.getWeekSeq(),
                    calcResult.getCustomerMatrix() != null && calcResult.getCustomerMatrix().length > 0 
                            ? calcResult.getCustomerMatrix()[0] : new BigDecimal[30]
            );
            log.info("写入 prediction_price 表完成");
        } else {
            // 写入 prediction 表
            boolean success = standardWriteBackService.writeBackSingleCigarette(
                    calcResult.getAllocationMatrix(),
                    calcResult.getCustomerMatrix(),
                    calcResult.getRegions(),
                    request.getCigCode(),
                    request.getCigName(),
                    request.getYear(),
                    request.getMonth(),
                    request.getWeekSeq(),
                    request.getNewDeliveryMethod(),
                    request.getNewDeliveryEtype(),
                    predictionRemark,
                    request.getNewTag(),
                    tagFilterConfig
            );
            if (!success) {
                throw new RuntimeException("写入 prediction 表失败");
            }
            log.info("写入 prediction 表完成");
        }
    }

    /**
     * 从分配结果构建候选数据列表（用于价位段写回）
     */
    private List<Map<String, Object>> buildCandidatesFromResult(AllocationAlgorithmSelector.AllocationResult calcResult,
                                                                 AdjustCigaretteStrategyRequestDto request,
                                                                 Map<String, Object> cigaretteInfo,
                                                                 String predictionRemark) {
        List<Map<String, Object>> candidates = new ArrayList<>();
        List<String> targets = calcResult.getRegions();
        BigDecimal[][] allocationMatrix = calcResult.getAllocationMatrix();

        if (targets == null || allocationMatrix == null) {
            return candidates;
        }

        for (int i = 0; i < targets.size() && i < allocationMatrix.length; i++) {
            Map<String, Object> candidate = new HashMap<>();
            candidate.put("CIG_CODE", request.getCigCode());
            candidate.put("CIG_NAME", request.getCigName());
            candidate.put("DELIVERY_AREA", targets.get(i));
            candidate.put("DELIVERY_METHOD", request.getNewDeliveryMethod());
            candidate.put("DELIVERY_ETYPE", request.getNewDeliveryEtype());
            candidate.put("TAG", request.getNewTag());
            candidate.put("ADV", request.getNewAdvAmount());
            // 使用预测表专用备注
            candidate.put("BZ", predictionRemark);

            // 添加档位数据
            BigDecimal[] grades = allocationMatrix[i];
            for (int j = 0; j < 30 && j < grades.length; j++) {
                candidate.put("D" + (30 - j), grades[j]);
            }

            candidates.add(candidate);
        }
        return candidates;
    }

    /**
     * 更新 Info 表备注
     */
    private void updateInfoRemark(AdjustCigaretteStrategyRequestDto request) {
        String remark = buildRemark(request);
        String sql = "UPDATE cigarette_distribution_info SET BZ = ? " +
                "WHERE YEAR = ? AND MONTH = ? AND WEEK_SEQ = ? AND CIG_CODE = ? AND CIG_NAME = ?";

        int updated = jdbcTemplate.update(sql, remark,
                request.getYear(), request.getMonth(), request.getWeekSeq(),
                request.getCigCode(), request.getCigName());

        log.info("更新 Info 表备注: {} 条记录, 备注: {}", updated, remark);
    }

    /**
     * 查询新分配记录
     */
    private List<Map<String, Object>> queryNewAllocationRecords(AdjustCigaretteStrategyRequestDto request,
                                                                 boolean isPriceBand) {
        if (isPriceBand) {
            return predictionPriceRepository.findAll(request.getYear(), request.getMonth(), request.getWeekSeq())
                    .stream()
                    .filter(r -> request.getCigCode().equals(r.get("CIG_CODE"))
                            && request.getCigName().equals(r.get("CIG_NAME")))
                    .collect(java.util.stream.Collectors.toList());
        } else {
            return predictionRepository.findAll(request.getYear(), request.getMonth(), request.getWeekSeq())
                    .stream()
                    .filter(r -> request.getCigCode().equals(r.get("CIG_CODE"))
                            && request.getCigName().equals(r.get("CIG_NAME")))
                    .collect(java.util.stream.Collectors.toList());
        }
    }
}
