package org.example.application.service.writeback.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.application.service.encode.EncodeService;
import org.example.application.service.writeback.PriceBandDistributionWriteBackService;
import org.example.domain.repository.CigaretteDistributionPredictionPriceRepository;
import org.example.shared.util.ActualDeliveryCalculator;
import org.example.shared.util.PartitionTableManager;
import org.example.shared.util.WriteBackHelper;
import org.example.infrastructure.persistence.po.CigaretteDistributionPredictionPricePO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
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
public class PriceBandDistributionWriteBackServiceImpl implements PriceBandDistributionWriteBackService {

    private final CigaretteDistributionPredictionPriceRepository predictionPriceRepository;
    private final PartitionTableManager partitionTableManager;
    private final EncodeService encodeService;

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

        List<CigaretteDistributionPredictionPricePO> predictionList = new ArrayList<>();

        for (Map<String, Object> row : candidates) {
            String cigCode = WriteBackHelper.getString(row, "CIG_CODE");
            String cigName = WriteBackHelper.getString(row, "CIG_NAME");
            String deliveryArea = WriteBackHelper.getString(row, "DELIVERY_AREA");
            String deliveryMethod = WriteBackHelper.getString(row, "DELIVERY_METHOD");
            String deliveryEtype = WriteBackHelper.getString(row, "DELIVERY_ETYPE");
            String tag = WriteBackHelper.getString(row, "TAG");
            String tagFilterConfig = WriteBackHelper.getString(row, "TAG_FILTER_CONFIG");
            // 从 Info 表获取的备注（BZ 字段）
            String remark = WriteBackHelper.getString(row, "BZ");

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
            CigaretteDistributionPredictionPricePO po = new CigaretteDistributionPredictionPricePO();
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
            // 使用从 Info 表获取的备注，如果为空则使用默认值
            po.setBz((remark != null && !remark.trim().isEmpty()) ? remark : "价位段自选投放算法自动生成");

            // 使用 WriteBackHelper 设置档位值（D30-D1）
            WriteBackHelper.setGradesToEntity(po, grades);
            
            // 通过 EncodeService 生成编码表达式（复用 encodeForSpecificArea）
            // 价位段自选投放的区域都是"全市"，传入单条记录列表即可
            String deployinfoCode = encodeService.encodeForSpecificArea(
                    cigCode, cigName, 
                    po.getDeliveryMethod(), deliveryEtype, 
                    po.getDeliveryArea(), Collections.singletonList(po));
            po.setDeployinfoCode(deployinfoCode);

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

