package org.example.application.service.writeback.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.application.service.writeback.DistributionWriteBackService;
import org.example.domain.repository.CigaretteDistributionPredictionPriceRepository;
import org.example.shared.util.ActualDeliveryCalculator;
import org.example.shared.util.PartitionTableManager;
import org.example.shared.util.WriteBackHelper;
import org.example.infrastructure.persistence.po.CigaretteDistributionPredictionPO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 价位段自选投放分配结果写回服务实现。
 * <p>专门负责价位段自选投放算法的分配结果批量写回 {@code cigarette_distribution_prediction_price} 分区表。</p>
 *
 * @author Robin
 * @since 2025-12-18
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PriceBandDistributionWriteBackServiceImpl implements DistributionWriteBackService {

    private final CigaretteDistributionPredictionPriceRepository predictionPriceRepository;
    private final PartitionTableManager partitionTableManager;

    @Override
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
        // 价位段写回服务不支持单条卷烟写回（标准场景）
        // 该方法应由 StandardDistributionWriteBackServiceImpl 实现
        throw new UnsupportedOperationException(
                "PriceBandDistributionWriteBackServiceImpl 不支持单条卷烟写回，请使用 StandardDistributionWriteBackServiceImpl");
    }

    @Override
    public void writeBackPriceBandAllocations(List<Map<String, Object>> candidates,
                                              Integer year,
                                              Integer month,
                                              Integer weekSeq,
                                              BigDecimal[] cityCustomerRow) {
        if (candidates == null || candidates.isEmpty()) {
            log.debug("无分配结果需要写回");
            return;
        }

        // 按价位段自选投放的卷烟投放区域应该都是"全市"，每条卷烟只应该有一条记录
        // 在写回前，先清理该分区中所有按价位段自选投放的旧数据，避免重复
        try {
            partitionTableManager.ensurePartitionExists("cigarette_distribution_prediction_price", year, month, weekSeq);
            int deletedCount = predictionPriceRepository.deleteByDeliveryMethod(year, month, weekSeq, "按价位段自选投放");
            if (deletedCount > 0) {
                log.info("清理旧的价位段自选投放数据: {}-{}-{}, 删除 {} 条记录", year, month, weekSeq, deletedCount);
            }
        } catch (Exception e) {
            log.warn("清理旧的价位段自选投放数据失败，继续写回: {}", e.getMessage());
        }

        List<CigaretteDistributionPredictionPO> predictionList = new ArrayList<>();

        for (Map<String, Object> row : candidates) {
            String cigCode = WriteBackHelper.getString(row, "CIG_CODE");
            String cigName = WriteBackHelper.getString(row, "CIG_NAME");
            String deliveryArea = WriteBackHelper.getString(row, "DELIVERY_AREA");
            String deliveryMethod = WriteBackHelper.getString(row, "DELIVERY_METHOD");
            String deliveryEtype = WriteBackHelper.getString(row, "DELIVERY_ETYPE");
            String tag = WriteBackHelper.getString(row, "TAG");
            String tagFilterConfig = WriteBackHelper.getString(row, "TAG_FILTER_CONFIG");

            if (cigCode == null || cigName == null) {
                log.warn("跳过写回：卷烟代码或名称为空，row={}", row);
                continue;
            }

            BigDecimal[] grades = (BigDecimal[]) row.get("GRADES");
            if (grades == null || grades.length != 30) {
                log.warn("跳过写回：档位数组无效，卷烟={}-{}", cigCode, cigName);
                continue;
            }

            // 计算实际投放量
            BigDecimal actualDelivery = ActualDeliveryCalculator.calculateFixed30(grades, cityCustomerRow);

            // 构建 PO 对象
            CigaretteDistributionPredictionPO po = new CigaretteDistributionPredictionPO();
            po.setYear(year);
            po.setMonth(month);
            po.setWeekSeq(weekSeq);
            po.setCigCode(cigCode);
            po.setCigName(cigName);
            po.setDeliveryArea(deliveryArea != null ? deliveryArea : "全市");
            po.setDeliveryMethod(deliveryMethod != null ? deliveryMethod : "按价位段自选投放");
            po.setDeliveryEtype(deliveryEtype);
            po.setTag(tag);
            po.setTagFilterConfig(tagFilterConfig);
            po.setActualDelivery(actualDelivery);
            po.setBz("价位段自选投放算法自动生成");

            // 使用 WriteBackHelper 设置档位值（D30-D1）
            WriteBackHelper.setGradesToEntity(po, grades);

            predictionList.add(po);
        }

        if (!predictionList.isEmpty()) {
            try {
                int count = predictionPriceRepository.batchUpsert(predictionList);
                log.info("价位段自选投放分配结果写回完成: {}-{}-{}, 写回 {} 条记录", year, month, weekSeq, count);
            } catch (Exception e) {
                log.error("价位段自选投放分配结果写回失败: {}-{}-{}", year, month, weekSeq, e);
                throw new RuntimeException("价位段自选投放分配结果写回失败", e);
            }
        }
    }

}

