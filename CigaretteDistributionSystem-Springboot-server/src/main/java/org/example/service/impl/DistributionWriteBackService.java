package org.example.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.example.entity.CigaretteDistributionPredictionData;
import org.example.mapper.CigaretteDistributionPredictionMapper;
import org.example.mapper.CigaretteDistributionPredictionPriceMapper;
import org.example.mapper.RegionCustomerStatisticsMapper;
import org.example.service.EncodeService;
import org.example.util.PartitionTableManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 专门负责“按卷烟写回分配结果”的服务。
 * <p>使用 REQUIRES_NEW，保证每条卷烟写回在独立事务中完成，避免一键分配形成大事务导致长时间运行或整体回滚。</p>
 *
 * @author Robin
 * @version 1.0
 * @since 2025-12-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DistributionWriteBackService {

    private final CigaretteDistributionPredictionMapper predictionMapper;
    private final CigaretteDistributionPredictionPriceMapper predictionPriceMapper;
    private final RegionCustomerStatisticsMapper regionCustomerStatisticsMapper;
    private final PartitionTableManager partitionTableManager;
    private final EncodeService encodeService;
    private static final String PRICE_METHOD = "按价位段自选投放";

    @Transactional(rollbackFor = Exception.class, timeout = 60, propagation = Propagation.REQUIRES_NEW)
    /**
     * 单条卷烟写回分配矩阵。
     *
     * @param allocationMatrix 分配矩阵
     * @param customerMatrix   客户矩阵（可选，提升实际投放计算）
     * @param targetList       目标区域列表
     * @param cigCode          卷烟代码
     * @param cigName          卷烟名称
     * @param year             年份
     * @param month            月份
     * @param weekSeq          周序号
     * @param deliveryMethod   投放方式
     * @param deliveryEtype    扩展投放类型
     * @param remark           备注
     * @param tag              标签
     * @param tagFilterConfig  标签过滤配置
     * @return 写回是否成功
     */
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

            // 构建所有区域记录用于编码表达式
            List<CigaretteDistributionPredictionData> allCigaretteRecords = buildPredictionRecords(
                    cigCode, cigName, deliveryMethod, deliveryEtype, allocationMatrix, targetList);

            List<CigaretteDistributionPredictionData> predictionDataList = new ArrayList<>();
            List<String> failedTargets = new ArrayList<>();

            for (int i = 0; i < targetList.size(); i++) {
                String target = targetList.get(i);
                try {
                    BigDecimal actualDelivery = calculateActualDeliveryForRegionDynamic(
                            target, allocationMatrix[i],
                            customerMatrix != null && i < customerMatrix.length ? customerMatrix[i] : null,
                            deliveryMethod, deliveryEtype, year, month, weekSeq);

                    String encoded = encodeService.encodeForSpecificArea(
                            cigCode, cigName, deliveryMethod, deliveryEtype, target, allCigaretteRecords);

                    CigaretteDistributionPredictionData predictionData = new CigaretteDistributionPredictionData();
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

                    if (checkBiWeeklyFloatFromRemark(remark)) {
                        predictionData.setBz(remark);
                    } else {
                        predictionData.setBz("算法自动生成");
                    }

                    setGradesToEntity(predictionData, allocationMatrix[i]);
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

    // 以下方法从 DistributionCalculateServiceImpl 中抽取/复用的逻辑（简化版签名）

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
     * @return 记录列表
     */
    private List<CigaretteDistributionPredictionData> buildPredictionRecords(
            String cigCode, String cigName, String deliveryMethod, String deliveryEtype,
            BigDecimal[][] allocationMatrix, List<String> targetList) {
        List<CigaretteDistributionPredictionData> records = new ArrayList<>();
        for (int i = 0; i < targetList.size() && i < allocationMatrix.length; i++) {
            CigaretteDistributionPredictionData data = new CigaretteDistributionPredictionData();
            data.setCigCode(cigCode);
            data.setCigName(cigName);
            data.setDeliveryMethod(deliveryMethod);
            data.setDeliveryEtype(deliveryEtype);
            data.setDeliveryArea(targetList.get(i));
            setGradesToEntity(data, allocationMatrix[i]);
            records.add(data);
        }
        return records;
    }

    /**
     * 判断备注中是否包含双周上浮标记。
     *
     * @param remark 备注
     * @return 是否需要双周上浮
     */
    private boolean checkBiWeeklyFloatFromRemark(String remark) {
        if (remark == null || remark.trim().isEmpty()) {
            return false;
        }
        String lowerRemark = remark.toLowerCase();
        return lowerRemark.contains("双周") && lowerRemark.contains("上浮");
    }

    /**
     * 将 30 档位值设置到实体。
     *
     * @param predictionData 实体
     * @param grades         档位数组（长度 30）
     */
    private void setGradesToEntity(CigaretteDistributionPredictionData predictionData, BigDecimal[] grades) {
        if (grades == null || grades.length != 30) {
            throw new IllegalArgumentException("档位数组必须包含30个值");
        }
        predictionData.setD30(grades[0]);
        predictionData.setD29(grades[1]);
        predictionData.setD28(grades[2]);
        predictionData.setD27(grades[3]);
        predictionData.setD26(grades[4]);
        predictionData.setD25(grades[5]);
        predictionData.setD24(grades[6]);
        predictionData.setD23(grades[7]);
        predictionData.setD22(grades[8]);
        predictionData.setD21(grades[9]);
        predictionData.setD20(grades[10]);
        predictionData.setD19(grades[11]);
        predictionData.setD18(grades[12]);
        predictionData.setD17(grades[13]);
        predictionData.setD16(grades[14]);
        predictionData.setD15(grades[15]);
        predictionData.setD14(grades[16]);
        predictionData.setD13(grades[17]);
        predictionData.setD12(grades[18]);
        predictionData.setD11(grades[19]);
        predictionData.setD10(grades[20]);
        predictionData.setD9(grades[21]);
        predictionData.setD8(grades[22]);
        predictionData.setD7(grades[23]);
        predictionData.setD6(grades[24]);
        predictionData.setD5(grades[25]);
        predictionData.setD4(grades[26]);
        predictionData.setD3(grades[27]);
        predictionData.setD2(grades[28]);
        predictionData.setD1(grades[29]);
    }

    /**
     * 计算区域实际投放量（分区表/提供矩阵二选一）。
     *
     * @param target                目标区域
     * @param allocationRow         分配档位数组
     * @param providedCustomerCounts 可选客户矩阵行
     * @param deliveryMethod        投放方式
     * @param deliveryEtype         扩展投放类型
     * @param year                  年份
     * @param month                 月份
     * @param weekSeq               周序号
     * @return 实际投放量
     */
    private BigDecimal calculateActualDeliveryForRegionDynamic(String target, BigDecimal[] allocationRow,
                                                               BigDecimal[] providedCustomerCounts,
                                                               String deliveryMethod, String deliveryEtype,
                                                               Integer year, Integer month, Integer weekSeq) {
        if (target == null || target.trim().isEmpty()) {
            throw new IllegalArgumentException("目标区域不能为空");
        }
        if (allocationRow == null || allocationRow.length != 30) {
            throw new IllegalArgumentException("档位分配数组必须包含30个档位(D30-D1)");
        }
        if (year == null || month == null || weekSeq == null) {
            throw new IllegalArgumentException("年份、月份、周序号不能为空");
        }

        BigDecimal[] customerCounts;
        if (providedCustomerCounts != null && providedCustomerCounts.length == 30) {
            customerCounts = providedCustomerCounts;
        } else {
            customerCounts = findCustomerCountsByRegion(year, month, weekSeq, target);
        if (customerCounts == null || customerCounts.length != 30) {
            throw new IllegalStateException(String.format(
                    "在分区表中未找到目标区域 '%s' (投放方法: %s, 投放类型: %s, 时间: %d-%d-%d) 的客户数数据",
                    target, deliveryMethod, deliveryEtype, year, month, weekSeq));
            }
        }

        BigDecimal actualDelivery = BigDecimal.ZERO;
        for (int i = 0; i < 30; i++) {
            BigDecimal allocation = allocationRow[i] != null ? allocationRow[i] : BigDecimal.ZERO;
            BigDecimal customerCount = customerCounts[i] != null ? customerCounts[i] : BigDecimal.ZERO;
            actualDelivery = actualDelivery.add(allocation.multiply(customerCount));
        }
        return actualDelivery;
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
                                       List<CigaretteDistributionPredictionData> predictionDataList) {
        if (PRICE_METHOD.equals(deliveryMethod)) {
            partitionTableManager.ensurePartitionExists("cigarette_distribution_prediction_price", year, month, weekSeq);
            predictionDataList.forEach(d -> {
                d.setYear(year);
                d.setMonth(month);
                d.setWeekSeq(weekSeq);
            });
            predictionPriceMapper.deleteByCig(year, month, weekSeq, cigCode, cigName);
            return predictionPriceMapper.batchUpsert(predictionDataList);
        }
        partitionTableManager.ensurePartitionExists("cigarette_distribution_prediction", year, month, weekSeq);
        predictionDataList.forEach(d -> {
            d.setYear(year);
            d.setMonth(month);
            d.setWeekSeq(weekSeq);
        });
        QueryWrapper<CigaretteDistributionPredictionData> qw = new QueryWrapper<>();
        qw.eq("YEAR", year).eq("MONTH", month).eq("WEEK_SEQ", weekSeq)
                .eq("CIG_CODE", cigCode).eq("CIG_NAME", cigName);
        predictionMapper.delete(qw);
        return predictionMapper.batchUpsert(predictionDataList);
    }

    /**
     * 从分区表查询区域客户数。
     */
    private BigDecimal[] findCustomerCountsByRegion(Integer year, Integer month, Integer weekSeq, String region) {
        partitionTableManager.ensurePartitionExists("region_customer_statistics", year, month, weekSeq);
        java.util.Map<String, Object> row = regionCustomerStatisticsMapper.findByRegion(year, month, weekSeq, region);
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

}


