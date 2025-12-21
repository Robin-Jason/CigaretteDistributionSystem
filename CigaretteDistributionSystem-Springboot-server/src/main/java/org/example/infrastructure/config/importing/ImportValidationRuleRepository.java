package org.example.infrastructure.config.importing;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 导入合法性校验规则内存仓库。
 *
 * 作用：
 * - 启动时加载 {@link ImportValidationRuleProperties}，构建便于查询的规则映射。
 * - 对外提供“全市占比阈值”和“按货源属性编码查找合法规则”的访问接口。
 *
 * 使用方式：
 * - 领域层导入校验规则（如 CityWideRatioRule、SupplySourceValidationRule）通过依赖该仓库来获取配置。
 *
 * @author Robin
 * @since 2025-12-18
 */
@Slf4j
@Component
public class ImportValidationRuleRepository {

    private final ImportValidationRuleProperties properties;

    /**
     * 货源属性编码 -> 规则配置 映射。
     */
    private Map<String, ImportValidationRuleProperties.SupplyRuleConfig> supplyRuleMap = Collections.emptyMap();

    /**
     * 货源属性标签（中文名称/别名）-> 货源属性编码 映射。
     */
    private Map<String, String> supplyLabelMap = Collections.emptyMap();

    public ImportValidationRuleRepository(ImportValidationRuleProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void initialize() {
        reload();
    }

    /**
     * 重新加载全部导入校验规则。
     */
    public synchronized void reload() {
        Map<String, ImportValidationRuleProperties.SupplyRuleConfig> rules = new LinkedHashMap<>();
        Map<String, String> labels = new LinkedHashMap<>();
        if (properties.getSupplyRules() != null) {
            for (ImportValidationRuleProperties.SupplyRuleConfig config : properties.getSupplyRules()) {
                if (!StringUtils.hasText(config.getSourceCode())) {
                    continue;
                }
                String key = config.getSourceCode().trim();
                // 后出现的配置覆盖前面的同名配置，便于运维修正
                rules.put(key, config);

                if (config.getLabels() != null) {
                    for (String label : config.getLabels()) {
                        if (!StringUtils.hasText(label)) {
                            continue;
                        }
                        String normalized = label.trim();
                        labels.put(normalized, key);
                    }
                }
            }
        }
        this.supplyRuleMap = Collections.unmodifiableMap(rules);
        this.supplyLabelMap = Collections.unmodifiableMap(labels);

        log.info("Import validation rule repository initialized: {} supply rules, {} labels, city-wide minRatio={}",
                supplyRuleMap.size(),
                supplyLabelMap.size(),
                getMinCityWideRatio());
    }

    /**
     * 获取“全市投放卷烟数量 / 本期投放所有卷烟数量”的最小占比阈值。
     */
    public double getMinCityWideRatio() {
        ImportValidationRuleProperties.CityWideConfig cfg = properties.getCityWide();
        if (cfg == null) {
            return 0D;
        }
        return cfg.getMinRatio();
    }

    /**
     * 根据货源属性编码查找对应的合法规则配置。
     *
     * @param sourceCode 货源属性编码（如 TIGHT / BALANCED / FULL / NEW）
     * @return 命中则返回 Optional.of(config)，否则 Optional.empty()
     */
    public Optional<ImportValidationRuleProperties.SupplyRuleConfig> findSupplyRule(String sourceCode) {
        if (!StringUtils.hasText(sourceCode)) {
            return Optional.empty();
        }
        return Optional.ofNullable(supplyRuleMap.get(sourceCode.trim()));
    }

    /**
     * 根据货源属性标签（中文名称/别名）查找对应的合法规则配置。
     *
     * @param label 货源属性标签（如“紧俏货源”、“均衡满足”等）
     * @return 命中则返回 Optional.of(config)，否则 Optional.empty()
     */
    public Optional<ImportValidationRuleProperties.SupplyRuleConfig> findSupplyRuleByLabel(String label) {
        if (!StringUtils.hasText(label)) {
            return Optional.empty();
        }
        String code = supplyLabelMap.get(label.trim());
        if (code == null) {
            return Optional.empty();
        }
        return findSupplyRule(code);
    }
}


