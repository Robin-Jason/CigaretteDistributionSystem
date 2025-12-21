package org.example.service.calculate;

import org.example.application.service.coordinator.impl.PriceBandCandidateQueryServiceImpl;
import org.example.domain.repository.CigaretteDistributionInfoRepository;
import org.example.infrastructure.config.price.PriceBandRuleProperties;
import org.example.infrastructure.config.price.PriceBandRuleRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.*;

/**
 * {@link PriceBandCandidateQueryServiceImpl} 的单元测试。
 */
class PriceBandCandidateQueryServiceImplTest {

    private CigaretteDistributionInfoRepository infoRepository;
    private PriceBandRuleRepository priceBandRuleRepository;
    private PriceBandCandidateQueryServiceImpl service;

    @BeforeEach
    void setUp() {
        infoRepository = Mockito.mock(CigaretteDistributionInfoRepository.class);

        // 构造简单的价位段配置：1 段 >= 600, 2 段 [400,600), 3 段 [100,400)
        PriceBandRuleProperties props = new PriceBandRuleProperties();
        List<PriceBandRuleProperties.PriceBandConfig> bands = new ArrayList<>();

        PriceBandRuleProperties.PriceBandConfig b1 = new PriceBandRuleProperties.PriceBandConfig();
        b1.setCode(1);
        b1.setLabel("第1段");
        b1.setMinInclusive(BigDecimal.valueOf(600));
        bands.add(b1);

        PriceBandRuleProperties.PriceBandConfig b2 = new PriceBandRuleProperties.PriceBandConfig();
        b2.setCode(2);
        b2.setLabel("第2段");
        b2.setMinInclusive(BigDecimal.valueOf(400));
        b2.setMaxExclusive(BigDecimal.valueOf(600));
        bands.add(b2);

        PriceBandRuleProperties.PriceBandConfig b3 = new PriceBandRuleProperties.PriceBandConfig();
        b3.setCode(3);
        b3.setLabel("第3段");
        b3.setMinInclusive(BigDecimal.valueOf(100));
        b3.setMaxExclusive(BigDecimal.valueOf(400));
        bands.add(b3);

        props.setPriceBands(bands);
        priceBandRuleRepository = new PriceBandRuleRepository(props);
        priceBandRuleRepository.reload();

        service = new PriceBandCandidateQueryServiceImpl(infoRepository, priceBandRuleRepository);
    }

    @Test
    void listOrderedPriceBandCandidates_shouldGroupByBandAndSortWithinGroup() {
        // 模拟仓储返回的原始数据：不同批发价的三支卷烟
        List<Map<String, Object>> raw = new ArrayList<>();
        raw.add(row("A1", "卷烟A1", 650));  // band=1
        raw.add(row("B1", "卷烟B1", 450));  // band=2
        raw.add(row("C1", "卷烟C1", 350));  // band=3
        raw.add(row("B2", "卷烟B2", 580));  // band=2, 组内排序用

        Mockito.when(infoRepository.findPriceBandCandidates(2025, 9, 3))
                .thenReturn(raw);

        List<Map<String, Object>> ordered = service.listOrderedPriceBandCandidates(2025, 9, 3);

        // 应只包含 4 条记录
        Assertions.assertEquals(4, ordered.size());

        // 验证顺序：band 1 -> band 2 (按批发价降序) -> band 3
        Assertions.assertEquals("A1", ordered.get(0).get("CIG_CODE")); // band=1

        // band=2 中，580 应该在 450 前面
        Assertions.assertEquals("B2", ordered.get(1).get("CIG_CODE"));
        Assertions.assertEquals("B1", ordered.get(2).get("CIG_CODE"));

        // 最后一条 band=3
        Assertions.assertEquals("C1", ordered.get(3).get("CIG_CODE"));

        // 验证 PRICE_BAND 字段存在
        Assertions.assertEquals(1, ordered.get(0).get("PRICE_BAND"));
        Assertions.assertEquals(2, ordered.get(1).get("PRICE_BAND"));
    }

    private Map<String, Object> row(String cigCode, String cigName, double wholesalePrice) {
        Map<String, Object> map = new HashMap<>();
        map.put("CIG_CODE", cigCode);
        map.put("CIG_NAME", cigName);
        map.put("WHOLESALE_PRICE", BigDecimal.valueOf(wholesalePrice));
        map.put("DELIVERY_AREA", "全市");
        map.put("DELIVERY_METHOD", "按价位段自选投放");
        map.put("DELIVERY_ETYPE", null);
        return map;
    }
}


