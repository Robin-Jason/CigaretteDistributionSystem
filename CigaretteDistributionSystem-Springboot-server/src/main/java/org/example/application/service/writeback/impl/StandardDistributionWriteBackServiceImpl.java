package org.example.application.service.writeback.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.application.service.encode.EncodeService;
import org.example.application.service.writeback.StandardDistributionWriteBackService;
import org.example.domain.repository.CigaretteDistributionPredictionPriceRepository;
import org.example.domain.repository.CigaretteDistributionPredictionRepository;
import org.example.domain.repository.RegionCustomerStatisticsRepository;
import org.example.domain.service.rule.BiWeeklyVisitBoostRule;
import org.example.infrastructure.persistence.po.CigaretteDistributionPredictionPO;
import org.example.shared.util.ActualDeliveryCalculator;
import org.example.shared.util.PartitionTableManager;
import org.example.shared.util.WriteBackHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 标准分配结果写回服务实现（按档位投放、按档位扩展投放等）。
 * <p>使用 REQUIRED 传播级别，加入调用方事务，避免嵌套事务导致的锁冲突。</p>
 *
 * @author Robin
 * @version 2.0
 * @since 2025-12-18
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StandardDistributionWriteBackServiceImpl implements StandardDistributionWriteBackService {

    private final CigaretteDistributionPredictionRepository predictionRepository;
    private final CigaretteDistributionPredictionPriceRepository predictionPriceRepository;
    private final RegionCustomerStatisticsRepository regionCustomerStatisticsRepository;
    private final PartitionTableManager partitionTableManager;
    private final EncodeService encodeService;
    private static final BiWeeklyVisitBoostRule BI_WEEKLY_VISIT_BOOST_RULE = new org.example.domain.service.rule.impl.BiWeeklyVisitBoostRuleImpl();
    private static final String PRICE_METHOD = "按价位段自选投放";

    @Override
    @Transactional(rollbackFor = Exception.class, timeout = 60)
    public boolean writeBackSingleCigarette(BigDecimal[][] allocationMatrix,
                                            BigDecimal[][] customerMatrix,
                                            List<String> targetList,
                                            String cigCode,
                                            String cigName,
                                            Integer year,
                                            Integer month,
                                            Integer weekSeq,
                                            String deliveryMethod,
                                            String deliveryEtype,
                                            String remark,
                                            String tag,
                                            String tagFilterConfig) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("【事务监控】写回事务开始: 卷烟 {} - {}, 表: {}_{}_{}",
                    cigCode, cigName, year, month, weekSeq);

            validateWriteBackParams(allocationMatrix, targetList, cigCode, cigName,
                    year, month, weekSeq, deliveryMethod, deliveryEtype);

            log.debug("writeBackSingleCigarette - 卷烟: {} - {}, deliveryMethod: {}, deliveryEtype: {}",
                    cigCode, cigName, deliveryMethod, deliveryEtype);

            // remark 参数是从 Info 表传入的 BZ 字段，直接使用
            // 如果 remark 为 null，使用默认值
            String finalRemark = (remark != null && !remark.trim().isEmpty()) ? remark : "算法自动生成";

            // 构建所有区域记录用于编码表达式
            List<CigaretteDistributionPredictionPO> allCigaretteRecords = buildPredictionRecords(
                    cigCode, cigName, deliveryMethod, deliveryEtype, allocationMatrix, targetList, tag, tagFilterConfig);

            List<CigaretteDistributionPredictionPO> predictionDataList = new ArrayList<>();
            List<String> failedTargets = new ArrayList<>();

            for (int i = 0; i < targetList.size(); i++) {
                String target = targetList.get(i);
                try {
                    BigDecimal actualDelivery;
                    BigDecimal[] customerCounts = null;
                    if (customerMatrix != null && i < customerMatrix.length && customerMatrix[i] != null) {
                        customerCounts = customerMatrix[i];
                        actualDelivery = ActualDeliveryCalculator.calculateFixed30(allocationMatrix[i], customerMatrix[i]);
                    } else {
                        customerCounts = findCustomerCountsByRegion(year, month, weekSeq, target);
                        if (customerCounts == null || customerCounts.length != 30) {
                            throw new IllegalStateException(String.format(
                                    "在分区表中未找到目标区域 '%s' (投放方法: %s, 投放类型: %s, 时间: %d-%d-%d) 的客户数数据",
                                    target, deliveryMethod, deliveryEtype, year, month, weekSeq));
                        }
                        // 检查区域客户数是否全为0
                        if (isAllGradesZero(customerCounts)) {
                            log.warn("跳过区域 '{}' 的分配：30个档位客户数全为0 (投放方法: {}, 投放类型: {}, 时间: {}-{}-{})",
                                    target, deliveryMethod, deliveryEtype, year, month, weekSeq);
                            failedTargets.add(target);
                            continue; // 跳过该区域，继续处理下一个区域
                        }
                        actualDelivery = ActualDeliveryCalculator.calculateFixed30(allocationMatrix[i], customerCounts);
                    }

                    String encoded = encodeService.encodeForSpecificArea(
                            cigCode, cigName, deliveryMethod, deliveryEtype, target, allCigaretteRecords);

                    CigaretteDistributionPredictionPO predictionData = new CigaretteDistributionPredictionPO();
                    predictionData.setCigCode(cigCode);
                    predictionData.setCigName(cigName);
                    predictionData.setDeliveryArea(target);
                    predictionData.setDeliveryMethod(deliveryMethod);
                    predictionData.setDeliveryEtype(deliveryEtype);
                    predictionData.setYear(year);
                    predictionData.setMonth(month);
                    predictionData.setWeekSeq(weekSeq);
                    predictionData.setTag(tag);
                    predictionData.setTagFilterConfig(tagFilterConfig);
                    predictionData.setActualDelivery(actualDelivery);
                    predictionData.setDeployinfoCode(encoded);
                    // 使用从 Info 表传入的备注
                    predictionData.setBz(finalRemark);

                    // 使用 WriteBackHelper 设置档位值
                    WriteBackHelper.setGradesToEntity(predictionData, allocationMatrix[i]);
                    predictionDataList.add(predictionData);
                } catch (Exception ex) {
                    log.warn("卷烟 '{}' 在区域 '{}' 处理失败: {}", cigName, target, ex.getMessage(), ex);
                    failedTargets.add(target);
                }
            }

            if (predictionDataList.isEmpty()) {
                log.warn("卷烟 {} 没有可插入的数据，所有区域处理失败: {}", cigName, failedTargets);
                return false;
            }

            int insertedCount = writeBackToProperTable(deliveryMethod,
                    year, month, weekSeq, cigCode, cigName, predictionDataList);
            log.info("卷烟 {} 写回完成，成功: {}/{}, 失败: {}", cigName,
                    insertedCount, predictionDataList.size(), failedTargets.size());

            if (!failedTargets.isEmpty()) {
                log.warn("卷烟 {} 部分区域处理失败: {}", cigName, failedTargets);
            }

            long elapsedTime = System.currentTimeMillis() - startTime;
            log.info("【事务监控】写回事务完成: 卷烟 {} - {}, 耗时: {}ms", cigCode, cigName, elapsedTime);
            return true;
        } catch (Exception e) {
            long elapsedTime = System.currentTimeMillis() - startTime;
            log.error("【事务监控】写回事务失败: 卷烟 {} - {}, 耗时: {}ms", cigCode, cigName, elapsedTime);
            log.error("写回数据库失败，卷烟: {} - {}, deliveryMethod: {}, deliveryEtype: {}, 错误类型: {}, 错误信息: {}",
                    cigCode, cigName, deliveryMethod, deliveryEtype, e.getClass().getSimpleName(), e.getMessage());
            log.error("详细堆栈信息:", e);
            return false;
        }
    }

    /**
     * 写回参数校验。
     *
     * @param allocationMatrix 分配矩阵
     * @param targetList       目标区域列表
     * @param cigCode          卷烟代码
     * @param cigName          卷烟名称
     * @param year             年份
     * @param month            月份
     * @param weekSeq          周序号
     * @param deliveryMethod   投放方式
     * @param deliveryEtype    扩展投放类型
     */
    private void validateWriteBackParams(BigDecimal[][] allocationMatrix, List<String> targetList,
                                         String cigCode, String cigName, Integer year, Integer month, Integer weekSeq,
                                         String deliveryMethod, String deliveryEtype) {
        if (allocationMatrix == null || allocationMatrix.length == 0) {
            throw new IllegalArgumentException("分配矩阵不能为空");
        }
        if (targetList == null || targetList.isEmpty()) {
            throw new IllegalArgumentException("目标区域列表不能为空");
        }
        if (cigCode == null || cigCode.trim().isEmpty()) {
            throw new IllegalArgumentException("卷烟代码不能为空");
        }
        if (cigName == null || cigName.trim().isEmpty()) {
            throw new IllegalArgumentException("卷烟名称不能为空");
        }
        if (year == null || month == null || weekSeq == null) {
            throw new IllegalArgumentException("时间参数不能为空");
        }
    }

    /**
     * 构建卷烟记录（含区域和档位数据），用于编码或写回。
     *
     * @param cigCode          卷烟代码
     * @param cigName          卷烟名称
     * @param deliveryMethod   投放方式
     * @param deliveryEtype    扩展投放类型
     * @param allocationMatrix 分配矩阵
     * @param targetList       目标区域列表
     * @param tag              标签
     * @param tagFilterConfig  标签过滤配置
     * @return 记录列表
     */
    private List<CigaretteDistributionPredictionPO> buildPredictionRecords(
            String cigCode, String cigName, String deliveryMethod, String deliveryEtype,
            BigDecimal[][] allocationMatrix, List<String> targetList, String tag, String tagFilterConfig) {
        List<CigaretteDistributionPredictionPO> records = new ArrayList<>();
        for (int i = 0; i < targetList.size() && i < allocationMatrix.length; i++) {
            CigaretteDistributionPredictionPO data = new CigaretteDistributionPredictionPO();
            data.setCigCode(cigCode);
            data.setCigName(cigName);
            data.setDeliveryMethod(deliveryMethod);
            data.setDeliveryEtype(deliveryEtype);
            data.setDeliveryArea(targetList.get(i));
            data.setTag(tag);
            data.setTagFilterConfig(tagFilterConfig);
            // 使用 WriteBackHelper 设置档位值
            WriteBackHelper.setGradesToEntity(data, allocationMatrix[i]);
            records.add(data);
        }
        return records;
    }



    /**
     * 根据投放方式选择对应分表写回。
     *
     * @param deliveryMethod 投放方式
     * @param year           年份
     * @param month          月份
     * @param weekSeq        周序号
     * @param cigCode        卷烟代码
     * @param cigName        卷烟名称
     * @param predictionDataList 预测数据列表
     * @return 插入/更新行数
     */
    private int writeBackToProperTable(String deliveryMethod,
                                       Integer year, Integer month, Integer weekSeq,
                                       String cigCode, String cigName,
                                       List<CigaretteDistributionPredictionPO> predictionDataList) {
        if (PRICE_METHOD.equals(deliveryMethod)) {
            partitionTableManager.ensurePartitionExists("cigarette_distribution_prediction_price", year, month, weekSeq);
            predictionDataList.forEach(d -> {
                d.setYear(year);
                d.setMonth(month);
                d.setWeekSeq(weekSeq);
            });
            predictionPriceRepository.deleteByCig(year, month, weekSeq, cigCode, cigName);
            return predictionPriceRepository.batchUpsert(predictionDataList);
        }
        partitionTableManager.ensurePartitionExists("cigarette_distribution_prediction", year, month, weekSeq);
        predictionDataList.forEach(d -> {
            d.setYear(year);
            d.setMonth(month);
            d.setWeekSeq(weekSeq);
        });
        // 使用精确删除方法，减少锁范围
        predictionRepository.deleteByCigarette(year, month, weekSeq, cigCode, cigName);
        return predictionRepository.batchUpsert(predictionDataList);
    }

    /**
     * 从分区表查询区域客户数。
     */
    private BigDecimal[] findCustomerCountsByRegion(Integer year, Integer month, Integer weekSeq, String region) {
        partitionTableManager.ensurePartitionExists("region_customer_statistics", year, month, weekSeq);
        Map<String, Object> row = regionCustomerStatisticsRepository.findByRegion(year, month, weekSeq, region);
        if (row == null) {
            return null;
        }
        BigDecimal[] counts = new BigDecimal[30];
        String[] gradeNames = {
                "D30","D29","D28","D27","D26","D25","D24","D23","D22","D21",
                "D20","D19","D18","D17","D16","D15","D14","D13","D12","D11",
                "D10","D9","D8","D7","D6","D5","D4","D3","D2","D1"
        };
        for (int i = 0; i < gradeNames.length; i++) {
            Object val = row.get(gradeNames[i]);
            counts[i] = val == null ? BigDecimal.ZERO : new BigDecimal(val.toString());
        }
        return counts;
    }

    /**
     * 检查区域客户数的30个档位是否全为0
     */
    private boolean isAllGradesZero(BigDecimal[] customerCounts) {
        if (customerCounts == null || customerCounts.length == 0) {
            return true;
        }
        for (BigDecimal count : customerCounts) {
            if (count != null && count.compareTo(BigDecimal.ZERO) > 0) {
                return false;
            }
        }
        return true;
    }
}