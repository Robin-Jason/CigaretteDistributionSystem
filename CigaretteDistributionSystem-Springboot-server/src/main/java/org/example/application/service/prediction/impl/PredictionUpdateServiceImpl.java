package org.example.application.service.prediction.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.application.dto.prediction.UpdateRegionGradesDto;
import org.example.application.service.encode.EncodeService;
import org.example.application.service.prediction.PredictionUpdateService;
import org.example.domain.repository.CigaretteDistributionInfoRepository;
import org.example.domain.repository.CigaretteDistributionPredictionPriceRepository;
import org.example.domain.repository.CigaretteDistributionPredictionRepository;
import org.example.domain.repository.RegionCustomerStatisticsRepository;
import org.example.domain.service.rule.PredictionValidationRule;
import org.example.domain.service.rule.impl.PredictionValidationRuleImpl;
import org.example.infrastructure.persistence.po.CigaretteDistributionPredictionPO;
import org.example.infrastructure.persistence.po.CigaretteDistributionPredictionPricePO;
import org.example.shared.util.ActualDeliveryCalculator;
import org.example.shared.util.GradeExtractor;
import org.example.shared.util.ParamValidators;
import org.example.shared.util.RemarkHelper;
import org.example.shared.util.WriteBackHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 预测分配数据修改服务实现
 *
 * @author Robin
 * @since 2025-12-22
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PredictionUpdateServiceImpl implements PredictionUpdateService {

    private static final String PRICE_BAND_DELIVERY_METHOD = "按价位段自选投放";

    /** 领域级校验规则（纯 Java 对象，不注册为 Spring Bean） */
    private final PredictionValidationRule validationRule = new PredictionValidationRuleImpl();

    private final CigaretteDistributionInfoRepository infoRepository;
    private final CigaretteDistributionPredictionRepository predictionRepository;
    private final CigaretteDistributionPredictionPriceRepository predictionPriceRepository;
    private final RegionCustomerStatisticsRepository regionCustomerStatisticsRepository;
    private final EncodeService encodeService;

    @Override
    @Transactional
    public void updateRegionGrades(UpdateRegionGradesDto dto) {
        log.info("修改区域档位值: year={}, month={}, weekSeq={}, cigCode={}, cigName={}, primaryRegion={}, secondaryRegion={}",
                dto.getYear(), dto.getMonth(), dto.getWeekSeq(), dto.getCigCode(), dto.getCigName(),
                dto.getPrimaryRegion(), dto.getSecondaryRegion());

        // 1. 基础参数校验
        ParamValidators.validateAddOrUpdateParams(dto.getYear(), dto.getMonth(), dto.getWeekSeq(),
                dto.getCigCode(), dto.getCigName(), dto.getPrimaryRegion(), dto.getGrades());

        // 2. 从 info 表查询该卷烟的投放信息
        Map<String, Object> cigInfo = infoRepository.findByCigCodeAndName(
                dto.getYear(), dto.getMonth(), dto.getWeekSeq(), dto.getCigCode(), dto.getCigName());
        validationRule.validateCigaretteExists(cigInfo, dto.getCigCode(), dto.getCigName(),
                dto.getYear(), dto.getMonth(), dto.getWeekSeq()).throwIfInvalid();

        // 3. 提取投放方式和 HG/LG
        String deliveryMethod = getString(cigInfo, "DELIVERY_METHOD");
        String deliveryEtype = getString(cigInfo, "DELIVERY_ETYPE");
        String tag = getString(cigInfo, "TAG");
        String tagFilterConfig = getString(cigInfo, "TAG_FILTER_CONFIG");
        String infoHg = getString(cigInfo, "HG");
        String infoLg = getString(cigInfo, "LG");
        boolean isPriceBand = PRICE_BAND_DELIVERY_METHOD.equals(deliveryMethod);

        // 4. 确定使用的 HG/LG（优先使用前端传入的，否则使用 Info 表的）
        String hg = dto.getNewHg() != null ? dto.getNewHg() : infoHg;
        String lg = dto.getNewLg() != null ? dto.getNewLg() : infoLg;

        // 5. 拼接投放区域
        String deliveryArea = ParamValidators.buildDeliveryArea(
                dto.getPrimaryRegion(), dto.getSecondaryRegion());

        // 6. 校验档位值单调性（基于 HG/LG 范围）
        validationRule.validateGradesMonotonicityWithRange(dto.getGrades(), hg, lg).throwIfInvalid();

        // 7. 校验记录是否存在
        boolean recordExists = checkRecordExists(dto.getYear(), dto.getMonth(), dto.getWeekSeq(),
                dto.getCigCode(), dto.getCigName(), deliveryArea, isPriceBand);
        if (!recordExists) {
            throw new IllegalStateException(String.format(
                    "[业务错误] 未找到卷烟 %s(%s) 在区域 %s 的分配记录",
                    dto.getCigName(), dto.getCigCode(), deliveryArea));
        }

        // 8. 计算实际投放量
        BigDecimal[] gradesArray = dto.getGrades().toArray(new BigDecimal[0]);
        BigDecimal[] customerCounts = getCustomerCountsForRegion(
                dto.getYear(), dto.getMonth(), dto.getWeekSeq(), deliveryArea);
        BigDecimal actualDelivery = ActualDeliveryCalculator.calculateFixed30(gradesArray, customerCounts);

        // 9. 处理备注：如果 HG/LG 有变更，追加变更信息
        String finalRemark = RemarkHelper.buildHgLgChangeRemark(dto.getRemark(), infoHg, infoLg, hg, lg);

        // 10. 构建 PO 并生成编码表达式
        CigaretteDistributionPredictionPO po = new CigaretteDistributionPredictionPO();
        po.setYear(dto.getYear());
        po.setMonth(dto.getMonth());
        po.setWeekSeq(dto.getWeekSeq());
        po.setCigCode(dto.getCigCode());
        po.setCigName(dto.getCigName());
        po.setDeliveryArea(deliveryArea);
        po.setDeliveryMethod(deliveryMethod);
        po.setDeliveryEtype(deliveryEtype);
        po.setTag(tag);
        po.setTagFilterConfig(tagFilterConfig);
        po.setActualDelivery(actualDelivery);
        po.setBz(finalRemark);
        WriteBackHelper.setGradesToEntity(po, gradesArray);
        
        String deployinfoCode = encodeService.encodeForSpecificArea(
                dto.getCigCode(), dto.getCigName(), deliveryMethod, deliveryEtype,
                deliveryArea, Collections.singletonList(po));
        po.setDeployinfoCode(deployinfoCode);

        // 10. 执行更新
        int updatedCount;
        if (isPriceBand) {
            CigaretteDistributionPredictionPricePO pricePo = new CigaretteDistributionPredictionPricePO();
            pricePo.setYear(dto.getYear());
            pricePo.setMonth(dto.getMonth());
            pricePo.setWeekSeq(dto.getWeekSeq());
            pricePo.setCigCode(dto.getCigCode());
            pricePo.setCigName(dto.getCigName());
            pricePo.setDeliveryArea(deliveryArea);
            pricePo.setDeliveryMethod(deliveryMethod);
            pricePo.setDeliveryEtype(deliveryEtype);
            pricePo.setTag(tag);
            pricePo.setTagFilterConfig(tagFilterConfig);
            pricePo.setActualDelivery(actualDelivery);
            pricePo.setBz(finalRemark);
            pricePo.setDeployinfoCode(deployinfoCode);
            WriteBackHelper.setGradesToEntity(pricePo, gradesArray);
            updatedCount = predictionPriceRepository.updateOne(pricePo);
        } else {
            updatedCount = predictionRepository.updateOne(po);
        }

        if (updatedCount == 0) {
            throw new IllegalStateException(String.format(
                    "[业务错误] 更新失败，未找到卷烟 %s(%s) 在区域 %s 的分配记录",
                    dto.getCigName(), dto.getCigCode(), deliveryArea));
        }

        log.info("修改区域档位值成功: cigCode={}, cigName={}, deliveryArea={}, actualDelivery={}, deployinfoCode={}, remark={}",
                dto.getCigCode(), dto.getCigName(), deliveryArea, actualDelivery, deployinfoCode, finalRemark);
    }

    /**
     * 检查记录是否存在
     */
    private boolean checkRecordExists(Integer year, Integer month, Integer weekSeq,
                                      String cigCode, String cigName, String deliveryArea, boolean isPriceBand) {
        if (isPriceBand) {
            List<Map<String, Object>> existing = predictionPriceRepository.findAll(year, month, weekSeq);
            for (Map<String, Object> record : existing) {
                if (cigCode.equals(getString(record, "CIG_CODE"))
                        && cigName.equals(getString(record, "CIG_NAME"))
                        && deliveryArea.equals(getString(record, "DELIVERY_AREA"))) {
                    return true;
                }
            }
        } else {
            List<Map<String, Object>> existing = predictionRepository.findByCigCode(year, month, weekSeq, cigCode);
            for (Map<String, Object> record : existing) {
                if (cigName.equals(getString(record, "CIG_NAME"))
                        && deliveryArea.equals(getString(record, "DELIVERY_AREA"))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 获取指定区域的客户数数组
     */
    private BigDecimal[] getCustomerCountsForRegion(Integer year, Integer month, Integer weekSeq, String regionName) {
        Map<String, Object> stat = regionCustomerStatisticsRepository.findByRegion(year, month, weekSeq, regionName);
        if (stat == null) {
            log.warn("未找到区域 {} 的客户统计数据，使用全0数组", regionName);
            return new BigDecimal[30];
        }
        return GradeExtractor.extractFromMap(stat);
    }

    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }
}
