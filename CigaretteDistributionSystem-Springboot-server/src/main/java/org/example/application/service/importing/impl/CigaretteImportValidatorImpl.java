package org.example.application.service.importing.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.application.service.importing.CigaretteImportValidator;
import org.example.domain.service.rule.CityWideRatioRule;
import org.example.domain.service.rule.SupplySourceValidationRule;
import org.example.domain.service.rule.impl.CityWideRatioRuleImpl;
import org.example.domain.service.rule.impl.SupplySourceValidationRuleImpl;
import org.example.infrastructure.config.encoding.EncodingRuleRepository;
import org.example.infrastructure.config.importing.ImportValidationRuleProperties;
import org.example.infrastructure.config.importing.ImportValidationRuleRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 卷烟投放基础信息导入业务校验器实现。
 *
 * <p>职责：对整批导入数据执行“全市占比 + 货源属性合法性”等业务规则校验。</p>
 *
 * @author Robin
 * @since 2025-12-18
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CigaretteImportValidatorImpl implements CigaretteImportValidator {

    private final ImportValidationRuleRepository importValidationRuleRepository;
    private final EncodingRuleRepository encodingRuleRepository;

    /**
     * 领域级导入校验规则实现（纯 Java 对象，不注册为 Spring Bean）。
     */
    private final CityWideRatioRule cityWideRatioRule = new CityWideRatioRuleImpl();
    private final SupplySourceValidationRule supplySourceValidationRule = new SupplySourceValidationRuleImpl();

    @Override
    public void validate(List<Map<String, Object>> excelData) {
        if (excelData == null || excelData.isEmpty()) {
            return;
        }
        validateCityWideRatio(excelData);
        validateSupplyAttributeRules(excelData);
    }

    /**
     * 校验全市投放卷烟占比。
     *
     * @param excelData Excel 行数据
     *
     * @example cityWideCount/totalCount &lt; minRatio 时抛出 IllegalArgumentException
     */
    private void validateCityWideRatio(List<Map<String, Object>> excelData) {
        long totalCount = excelData.size();
        long cityWideCount = excelData.stream()
                .filter(this::isCityWideRecord)
                .count();
        double minRatio = importValidationRuleRepository.getMinCityWideRatio();
        cityWideRatioRule.validate(totalCount, cityWideCount, minRatio);
    }

    /**
     * 判断单条记录是否为“全市投放”。
     *
     * @param row 单行数据
     * @return 是否全市投放
     *
     * @example DELIVERY_AREA 列为 "全市" 或 "全市投放" 时返回 true
     */
    private boolean isCityWideRecord(Map<String, Object> row) {
        Object area = row.get("DELIVERY_AREA");
        if (area == null) {
            return false;
        }
        String value = area.toString().trim();
        return "全市".equals(value) || "全市投放".equals(value);
    }

    /**
     * 校验货源属性 + 投放类型 + 标签组合是否合法。
     *
     * @param excelData Excel 行数据
     *
     * @example SUPPLY_ATTRIBUTE="TIGHT" 且 DELIVERY_METHOD="按档位投放" 且 TAG 为空 -> 合法
     */
    private void validateSupplyAttributeRules(List<Map<String, Object>> excelData) {
        for (Map<String, Object> row : excelData) {
            String sourceCode = toSourceCode(row.get("SUPPLY_ATTRIBUTE"));
            if (sourceCode == null) {
                // 未配置货源属性编码时，跳过规则校验
                continue;
            }

            String deliveryMethodLabel = toString(row.get("DELIVERY_METHOD"));
            String deliveryMethodCode = encodingRuleRepository.findDeliveryMethodCode(deliveryMethodLabel);
            boolean hasTag = hasTag(row.get("TAG"));

            Optional<ImportValidationRuleProperties.SupplyRuleConfig> cfgOpt =
                    importValidationRuleRepository.findSupplyRule(sourceCode);
            if (!cfgOpt.isPresent()) {
                // 未配置对应货源属性规则时，默认放行
                log.debug("未找到货源属性[{}]的规则配置，跳过合法性校验", sourceCode);
                continue;
            }

            ImportValidationRuleProperties.SupplyRuleConfig cfg = cfgOpt.get();
            Set<String> allowedMethods = cfg.getAllowedDeliveryMethods().stream()
                    .filter(Objects::nonNull)
                    .map(s -> s.trim().toUpperCase(Locale.ROOT))
                    .collect(Collectors.toSet());
            SupplySourceValidationRule.SupplyRuleDefinition definition =
                    new SupplySourceValidationRule.SupplyRuleDefinition(
                            cfg.getSourceCode(), allowedMethods, cfg.isAllowTag());

            supplySourceValidationRule.validate(sourceCode, deliveryMethodCode, hasTag, definition);
        }
    }

    private String toSourceCode(Object value) {
        if (value == null) {
            return null;
        }
        String raw = value.toString().trim();
        if (raw.isEmpty()) {
            return null;
        }

        // 1) 优先按编码匹配（如 TIGHT/BALANCED/...）
        String upperCode = raw.toUpperCase(Locale.ROOT);
        if (importValidationRuleRepository.findSupplyRule(upperCode).isPresent()) {
            return upperCode;
        }

        // 2) 再按中文标签/别名匹配（如“紧俏货源”、“均衡满足”等）
        Optional<ImportValidationRuleProperties.SupplyRuleConfig> byLabel =
                importValidationRuleRepository.findSupplyRuleByLabel(raw);
        return byLabel.map(ImportValidationRuleProperties.SupplyRuleConfig::getSourceCode)
                .orElse(null);
    }

    private String toString(Object value) {
        return value == null ? null : value.toString();
    }

    private boolean hasTag(Object tagValue) {
        if (tagValue == null) {
            return false;
        }
        String s = tagValue.toString().trim();
        return !s.isEmpty();
    }
}


