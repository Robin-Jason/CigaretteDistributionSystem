package org.example.application.service.prediction.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.application.dto.prediction.DeleteCigaretteDto;
import org.example.application.dto.prediction.DeleteRegionAllocationDto;
import org.example.application.service.prediction.PredictionDeleteService;
import org.example.domain.repository.CigaretteDistributionInfoRepository;
import org.example.domain.repository.CigaretteDistributionPredictionPriceRepository;
import org.example.domain.repository.CigaretteDistributionPredictionRepository;
import org.example.domain.service.rule.PredictionValidationRule;
import org.example.domain.service.rule.impl.PredictionValidationRuleImpl;
import org.example.shared.util.ParamValidators;
import org.example.shared.util.RemarkHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * 预测分配数据删除服务实现
 *
 * @author Robin
 * @since 2025-12-22
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PredictionDeleteServiceImpl implements PredictionDeleteService {

    private static final String PRICE_BAND_DELIVERY_METHOD = "按价位段自选投放";

    /** 领域级校验规则（纯 Java 对象，不注册为 Spring Bean） */
    private final PredictionValidationRule validationRule = new PredictionValidationRuleImpl();

    private final CigaretteDistributionInfoRepository infoRepository;
    private final CigaretteDistributionPredictionRepository predictionRepository;
    private final CigaretteDistributionPredictionPriceRepository predictionPriceRepository;

    @Override
    @Transactional
    public void deleteRegionAllocation(DeleteRegionAllocationDto dto) {
        log.info("删除特定区域分配记录: year={}, month={}, weekSeq={}, cigCode={}, cigName={}, primaryRegion={}, secondaryRegion={}",
                dto.getYear(), dto.getMonth(), dto.getWeekSeq(), dto.getCigCode(), dto.getCigName(),
                dto.getPrimaryRegion(), dto.getSecondaryRegion());

        // 1. 基础参数校验
        ParamValidators.validateDeleteRegionParams(dto.getYear(), dto.getMonth(), dto.getWeekSeq(),
                dto.getCigCode(), dto.getCigName(), dto.getPrimaryRegion());

        // 2. 从 info 表查询该卷烟的投放信息
        Map<String, Object> cigInfo = infoRepository.findByCigCodeAndName(
                dto.getYear(), dto.getMonth(), dto.getWeekSeq(), dto.getCigCode(), dto.getCigName());
        validationRule.validateCigaretteExists(cigInfo, dto.getCigCode(), dto.getCigName(),
                dto.getYear(), dto.getMonth(), dto.getWeekSeq()).throwIfInvalid();

        // 3. 提取投放方式
        String deliveryMethod = getString(cigInfo, "DELIVERY_METHOD");
        boolean isPriceBand = PRICE_BAND_DELIVERY_METHOD.equals(deliveryMethod);

        // 4. 拼接投放区域
        String deliveryArea = ParamValidators.buildDeliveryArea(
                dto.getPrimaryRegion(), dto.getSecondaryRegion());

        // 5. 校验该卷烟的区域记录数，确保至少保留一条
        int regionCount;
        if (isPriceBand) {
            regionCount = predictionPriceRepository.countByCigarette(
                    dto.getYear(), dto.getMonth(), dto.getWeekSeq(), dto.getCigCode(), dto.getCigName());
        } else {
            regionCount = predictionRepository.countByCigarette(
                    dto.getYear(), dto.getMonth(), dto.getWeekSeq(), dto.getCigCode(), dto.getCigName());
        }

        if (regionCount <= 1) {
            throw new IllegalStateException(String.format(
                    "[业务错误] 卷烟 %s(%s) 仅剩 %d 条区域记录，无法删除。如需删除整个卷烟，请使用删除卷烟功能",
                    dto.getCigName(), dto.getCigCode(), regionCount));
        }

        // 6. 执行删除
        int deletedCount;
        if (isPriceBand) {
            deletedCount = predictionPriceRepository.deleteByDeliveryArea(
                    dto.getYear(), dto.getMonth(), dto.getWeekSeq(), dto.getCigCode(), dto.getCigName(), deliveryArea);
        } else {
            deletedCount = predictionRepository.deleteByDeliveryArea(
                    dto.getYear(), dto.getMonth(), dto.getWeekSeq(), dto.getCigCode(), dto.getCigName(), deliveryArea);
        }

        if (deletedCount == 0) {
            throw new IllegalStateException(String.format(
                    "[业务错误] 未找到卷烟 %s(%s) 在区域 %s 的分配记录",
                    dto.getCigName(), dto.getCigCode(), deliveryArea));
        }

        // 7. 更新 info 表备注（追加而非覆盖）
        String originalRemark = getString(cigInfo, "BZ");
        String newRemark = RemarkHelper.buildDeleteRegionRemark(originalRemark, deliveryArea);
        infoRepository.updateRemark(dto.getYear(), dto.getMonth(), dto.getWeekSeq(),
                dto.getCigCode(), dto.getCigName(), newRemark);

        log.info("删除特定区域分配记录成功: cigCode={}, cigName={}, deliveryArea={}, 删除 {} 条记录, 备注={}",
                dto.getCigCode(), dto.getCigName(), deliveryArea, deletedCount, newRemark);
    }

    @Override
    @Transactional
    public void deleteCigarette(DeleteCigaretteDto dto) {
        log.info("删除整个卷烟分配记录: year={}, month={}, weekSeq={}, cigCode={}, cigName={}",
                dto.getYear(), dto.getMonth(), dto.getWeekSeq(), dto.getCigCode(), dto.getCigName());

        // 1. 基础参数校验
        ParamValidators.validateDeleteCigaretteParams(dto.getYear(), dto.getMonth(), dto.getWeekSeq(),
                dto.getCigCode(), dto.getCigName());

        // 2. 从 info 表查询该卷烟的投放信息
        Map<String, Object> cigInfo = infoRepository.findByCigCodeAndName(
                dto.getYear(), dto.getMonth(), dto.getWeekSeq(), dto.getCigCode(), dto.getCigName());
        validationRule.validateCigaretteExists(cigInfo, dto.getCigCode(), dto.getCigName(),
                dto.getYear(), dto.getMonth(), dto.getWeekSeq()).throwIfInvalid();

        // 3. 提取投放方式
        String deliveryMethod = getString(cigInfo, "DELIVERY_METHOD");
        boolean isPriceBand = PRICE_BAND_DELIVERY_METHOD.equals(deliveryMethod);

        // 4. 执行删除
        int deletedCount;
        if (isPriceBand) {
            deletedCount = predictionPriceRepository.deleteByCig(
                    dto.getYear(), dto.getMonth(), dto.getWeekSeq(), dto.getCigCode(), dto.getCigName());
        } else {
            deletedCount = predictionRepository.deleteByCigarette(
                    dto.getYear(), dto.getMonth(), dto.getWeekSeq(), dto.getCigCode(), dto.getCigName());
        }

        if (deletedCount == 0) {
            throw new IllegalStateException(String.format(
                    "[业务错误] 未找到卷烟 %s(%s) 的分配记录",
                    dto.getCigName(), dto.getCigCode()));
        }

        // 5. 更新 info 表备注（追加而非覆盖）
        String originalRemark = getString(cigInfo, "BZ");
        String newRemark = RemarkHelper.buildDeleteCigaretteRemark(originalRemark);
        infoRepository.updateRemark(dto.getYear(), dto.getMonth(), dto.getWeekSeq(),
                dto.getCigCode(), dto.getCigName(), newRemark);

        log.info("删除整个卷烟分配记录成功: cigCode={}, cigName={}, 删除 {} 条记录, 备注={}",
                dto.getCigCode(), dto.getCigName(), deletedCount, newRemark);
    }

    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }
}
