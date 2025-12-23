package org.example.application.service.prediction.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.application.dto.prediction.AddRegionAllocationDto;
import org.example.application.service.encode.EncodeService;
import org.example.application.service.prediction.PredictionAddService;
import org.example.domain.repository.CigaretteDistributionInfoRepository;
import org.example.domain.repository.CigaretteDistributionPredictionPriceRepository;
import org.example.domain.repository.CigaretteDistributionPredictionRepository;
import org.example.domain.repository.RegionCustomerStatisticsRepository;
import org.example.domain.service.rule.PredictionValidationRule;
import org.example.domain.service.rule.PredictionValidationRule.RegionSets;
import org.example.domain.service.rule.PredictionValidationRule.ValidationResult;
import org.example.domain.service.rule.impl.PredictionValidationRuleImpl;
import org.example.infrastructure.persistence.po.CigaretteDistributionPredictionPO;
import org.example.infrastructure.persistence.po.CigaretteDistributionPredictionPricePO;
import org.example.shared.util.ActualDeliveryCalculator;
import org.example.shared.util.ParamValidators;
import org.example.shared.util.RemarkHelper;
import org.example.shared.util.WriteBackHelper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 预测分配数据新增服务实现
 *
 * @author Robin
 * @since 2025-12-22
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PredictionAddServiceImpl implements PredictionAddService {

    private static final String PRICE_BAND_DELIVERY_METHOD = "按价位段自选投放";

    /** 领域级校验规则（纯 Java 对象，不注册为 Spring Bean） */
    private final PredictionValidationRule validationRule = new PredictionValidationRuleImpl();

    private final CigaretteDistributionInfoRepository infoRepository;
    private final CigaretteDistributionPredictionRepository predictionRepository;
    private final CigaretteDistributionPredictionPriceRepository predictionPriceRepository;
    private final RegionCustomerStatisticsRepository regionCustomerStatisticsRepository;
    private final EncodeService encodeService;

    @Override
    public void addRegionAllocation(AddRegionAllocationDto dto) {
        log.info("新增投放区域分配记录: year={}, month={}, weekSeq={}, cigCode={}, cigName={}, primaryRegion={}, secondaryRegion={}",
                dto.getYear(), dto.getMonth(), dto.getWeekSeq(), dto.getCigCode(), dto.getCigName(),
                dto.getPrimaryRegion(), dto.getSecondaryRegion());

        // 0. 基础参数校验
        ParamValidators.validateAddOrUpdateParams(dto.getYear(), dto.getMonth(), dto.getWeekSeq(),
                dto.getCigCode(), dto.getCigName(), dto.getPrimaryRegion(), dto.getGrades());

        // 1. 从 info 表查询该卷烟的投放信息
        Map<String, Object> cigInfo = infoRepository.findByCigCodeAndName(
                dto.getYear(), dto.getMonth(), dto.getWeekSeq(), dto.getCigCode(), dto.getCigName());
        validationRule.validateCigaretteExists(cigInfo, dto.getCigCode(), dto.getCigName(),
                dto.getYear(), dto.getMonth(), dto.getWeekSeq()).throwIfInvalid();

        // 2. 提取投放相关字段
        String deliveryMethod = getString(cigInfo, "DELIVERY_METHOD");
        String deliveryEtype = getString(cigInfo, "DELIVERY_ETYPE");
        String tag = getString(cigInfo, "TAG");
        String tagFilterConfig = getString(cigInfo, "TAG_FILTER_CONFIG");
        String hg = getString(cigInfo, "HG");
        String lg = getString(cigInfo, "LG");

        // 3. 拼接投放区域
        String deliveryArea = ParamValidators.buildDeliveryArea(
                dto.getPrimaryRegion(), dto.getSecondaryRegion());

        // 4. 校验投放区域合法性
        validateDeliveryArea(dto.getYear(), dto.getMonth(), dto.getWeekSeq(),
                dto.getPrimaryRegion(), dto.getSecondaryRegion(), deliveryArea);

        // 5. 校验档位值单调性（基于 HG/LG 范围）
        validationRule.validateGradesMonotonicityWithRange(dto.getGrades(), hg, lg).throwIfInvalid();

        // 6. 校验记录是否已存在
        boolean recordExists = checkRecordExists(dto.getYear(), dto.getMonth(), dto.getWeekSeq(),
                dto.getCigCode(), deliveryArea, deliveryMethod);
        validationRule.validateRecordNotExists(recordExists, dto.getCigCode(),
                dto.getCigName(), deliveryArea).throwIfInvalid();

        // 7. 计算实际投放量
        BigDecimal[] gradesArray = dto.getGrades().toArray(new BigDecimal[0]);
        BigDecimal[] customerCounts = getCustomerCountsForRegion(
                dto.getYear(), dto.getMonth(), dto.getWeekSeq(), deliveryArea);
        BigDecimal actualDelivery = ActualDeliveryCalculator.calculateFixed30(gradesArray, customerCounts);

        // 8. 根据投放方式判断写入哪张表
        boolean isPriceBand = PRICE_BAND_DELIVERY_METHOD.equals(deliveryMethod);

        if (isPriceBand) {
            // 写入 prediction_price 表
            CigaretteDistributionPredictionPricePO po = buildPricePO(dto, deliveryArea,
                    deliveryMethod, deliveryEtype, tag, tagFilterConfig, actualDelivery, gradesArray);
            
            // 生成编码表达式
            String deployinfoCode = encodeService.encodeForSpecificArea(
                    dto.getCigCode(), dto.getCigName(), deliveryMethod, deliveryEtype,
                    deliveryArea, Collections.singletonList(po));
            po.setDeployinfoCode(deployinfoCode);

            predictionPriceRepository.upsert(po);
            log.info("新增价位段预测记录成功: cigCode={}, deliveryArea={}", dto.getCigCode(), deliveryArea);
        } else {
            // 写入 prediction 表
            CigaretteDistributionPredictionPO po = buildPredictionPO(dto, deliveryArea,
                    deliveryMethod, deliveryEtype, tag, tagFilterConfig, actualDelivery, gradesArray);

            // 生成编码表达式
            String deployinfoCode = encodeService.encodeForSpecificArea(
                    dto.getCigCode(), dto.getCigName(), deliveryMethod, deliveryEtype,
                    deliveryArea, Collections.singletonList(po));
            po.setDeployinfoCode(deployinfoCode);

            predictionRepository.upsert(po);
            log.info("新增预测记录成功: cigCode={}, deliveryArea={}", dto.getCigCode(), deliveryArea);
        }
    }

    /**
     * 校验投放区域合法性
     */
    private void validateDeliveryArea(Integer year, Integer month, Integer weekSeq,
                                      String primaryRegion, String secondaryRegion, String deliveryArea) {
        List<Map<String, Object>> allStats = regionCustomerStatisticsRepository.findAll(year, month, weekSeq);
        validationRule.validateBatchRegionStatsExists(allStats, year, month, weekSeq).throwIfInvalid();

        RegionSets regionSets = validationRule.parseRegionSets(allStats);

        ValidationResult result;
        if (!ParamValidators.isDualExtension(secondaryRegion)) {
            result = validationRule.validateSingleExtensionRegion(
                    primaryRegion, regionSets.getValidRegions(), regionSets.getPrimaryRegions());
        } else {
            result = validationRule.validateDualExtensionRegion(
                    primaryRegion, secondaryRegion, deliveryArea,
                    regionSets.getValidRegions(), regionSets.getPrimaryRegions(), regionSets.getSecondaryRegions());
        }
        result.throwIfInvalid();
    }

    /**
     * 检查记录是否已存在
     */
    private boolean checkRecordExists(Integer year, Integer month, Integer weekSeq,
                                      String cigCode, String deliveryArea, String deliveryMethod) {
        boolean isPriceBand = PRICE_BAND_DELIVERY_METHOD.equals(deliveryMethod);

        if (isPriceBand) {
            List<Map<String, Object>> existing = predictionPriceRepository.findAll(year, month, weekSeq);
            for (Map<String, Object> record : existing) {
                if (cigCode.equals(getString(record, "CIG_CODE"))
                        && deliveryArea.equals(getString(record, "DELIVERY_AREA"))) {
                    return true;
                }
            }
        } else {
            List<Map<String, Object>> existing = predictionRepository.findByCigCode(year, month, weekSeq, cigCode);
            for (Map<String, Object> record : existing) {
                if (deliveryArea.equals(getString(record, "DELIVERY_AREA"))) {
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
        return org.example.shared.util.GradeExtractor.extractFromMap(stat);
    }

    /**
     * 构建 prediction 表 PO
     */
    private CigaretteDistributionPredictionPO buildPredictionPO(AddRegionAllocationDto dto, String deliveryArea,
                                                                 String deliveryMethod, String deliveryEtype,
                                                                 String tag, String tagFilterConfig,
                                                                 BigDecimal actualDelivery, BigDecimal[] grades) {
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
        // 追加"人工新增区域"到备注
        po.setBz(RemarkHelper.buildAddRegionRemark(dto.getRemark(), deliveryArea));
        WriteBackHelper.setGradesToEntity(po, grades);
        return po;
    }

    /**
     * 构建 prediction_price 表 PO
     */
    private CigaretteDistributionPredictionPricePO buildPricePO(AddRegionAllocationDto dto, String deliveryArea,
                                                                 String deliveryMethod, String deliveryEtype,
                                                                 String tag, String tagFilterConfig,
                                                                 BigDecimal actualDelivery, BigDecimal[] grades) {
        CigaretteDistributionPredictionPricePO po = new CigaretteDistributionPredictionPricePO();
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
        // 追加"人工新增区域"到备注
        po.setBz(RemarkHelper.buildAddRegionRemark(dto.getRemark(), deliveryArea));
        WriteBackHelper.setGradesToEntity(po, grades);
        return po;
    }

    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }
}
