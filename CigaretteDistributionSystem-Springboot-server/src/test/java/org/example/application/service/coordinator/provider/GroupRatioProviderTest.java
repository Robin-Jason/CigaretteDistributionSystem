package org.example.application.service.coordinator.provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 分组比例提供者单元测试。
 */
class GroupRatioProviderTest {

    private MarketTypeRatioProvider marketTypeProvider;
    private IntegrityGroupRatioProvider integrityProvider;

    @BeforeEach
    void setUp() {
        marketTypeProvider = new MarketTypeRatioProvider();
        integrityProvider = new IntegrityGroupRatioProvider();
    }

    // ==================== MarketTypeRatioProvider 测试 ====================

    @Test
    void marketType_supports_档位加市场类型() {
        assertTrue(marketTypeProvider.supports("档位+市场类型"));
        assertFalse(marketTypeProvider.supports("档位+诚信互助小组"));
        assertFalse(marketTypeProvider.supports("按档位投放"));
        assertFalse(marketTypeProvider.supports(null));
    }

    @Test
    void marketType_用户传入比例时优先使用() {
        List<String> regions = Arrays.asList("城区", "农村");
        BigDecimal[][] matrix = new BigDecimal[][] {
            {new BigDecimal("100"), new BigDecimal("200")},
            {new BigDecimal("300"), new BigDecimal("400")}
        };
        
        // 用户传入 urbanRatio=0.7, ruralRatio=0.3
        Map<String, Object> extraInfo = new HashMap<>();
        extraInfo.put("urbanRatio", new BigDecimal("0.7"));
        extraInfo.put("ruralRatio", new BigDecimal("0.3"));

        Map<String, BigDecimal> ratios = marketTypeProvider.calculateGroupRatios(
                "档位+市场类型", regions, matrix, extraInfo);

        assertEquals(new BigDecimal("0.7"), ratios.get("城网"));
        assertEquals(new BigDecimal("0.3"), ratios.get("农网"));
    }

    @Test
    void marketType_无用户传入时使用默认比例40_60() {
        List<String> regions = Arrays.asList("城区", "农村");
        BigDecimal[][] matrix = new BigDecimal[][] {
            {new BigDecimal("100"), new BigDecimal("200")},
            {new BigDecimal("300"), new BigDecimal("400")}
        };

        Map<String, BigDecimal> ratios = marketTypeProvider.calculateGroupRatios(
                "档位+市场类型", regions, matrix, null);

        // 默认比例：城网 40%，农网 60%
        assertEquals(new BigDecimal("0.4"), ratios.get("城网"));
        assertEquals(new BigDecimal("0.6"), ratios.get("农网"));
    }

    @Test
    void marketType_extraInfo为空Map时使用默认比例() {
        List<String> regions = Arrays.asList("城区", "农村");
        BigDecimal[][] matrix = new BigDecimal[][] {
            {new BigDecimal("100")},
            {new BigDecimal("200")}
        };

        Map<String, BigDecimal> ratios = marketTypeProvider.calculateGroupRatios(
                "档位+市场类型", regions, matrix, new HashMap<>());

        assertEquals(new BigDecimal("0.4"), ratios.get("城网"));
        assertEquals(new BigDecimal("0.6"), ratios.get("农网"));
    }

    @Test
    void marketType_区域分组映射() {
        List<String> regions = Arrays.asList("城区", "城网", "农村", "农网", "其他区域");

        Map<String, String> mapping = marketTypeProvider.getRegionGroupMapping("档位+市场类型", regions);

        assertEquals("城网", mapping.get("城区"));
        assertEquals("城网", mapping.get("城网"));
        assertEquals("农网", mapping.get("农村"));
        assertEquals("农网", mapping.get("农网"));
        assertEquals("城网", mapping.get("其他区域")); // 默认归为城网
    }

    // ==================== IntegrityGroupRatioProvider 测试 ====================

    @Test
    void integrity_supports_档位加诚信互助小组() {
        assertTrue(integrityProvider.supports("档位+诚信互助小组"));
        assertFalse(integrityProvider.supports("档位+市场类型"));
        assertFalse(integrityProvider.supports("按档位投放"));
        assertFalse(integrityProvider.supports(null));
    }

    @Test
    void integrity_根据客户数占比计算() {
        List<String> regions = Arrays.asList("区域A", "区域B", "区域C");
        // 区域A: 100+200=300, 区域B: 300+400=700, 区域C: 500+500=1000
        // 总计: 2000
        // 比例: A=0.15, B=0.35, C=0.50
        BigDecimal[][] matrix = new BigDecimal[][] {
            {new BigDecimal("100"), new BigDecimal("200")},  // 300
            {new BigDecimal("300"), new BigDecimal("400")},  // 700
            {new BigDecimal("500"), new BigDecimal("500")}   // 1000
        };

        Map<String, BigDecimal> ratios = integrityProvider.calculateGroupRatios(
                "档位+诚信互助小组", regions, matrix, null);

        assertEquals(3, ratios.size());
        assertEquals(new BigDecimal("0.1500"), ratios.get("区域A"));
        assertEquals(new BigDecimal("0.3500"), ratios.get("区域B"));
        assertEquals(new BigDecimal("0.5000"), ratios.get("区域C"));
    }

    @Test
    void integrity_两个区域各占50() {
        List<String> regions = Arrays.asList("区域A", "区域B");
        BigDecimal[][] matrix = new BigDecimal[][] {
            {new BigDecimal("500")},  // 500
            {new BigDecimal("500")}   // 500
        };

        Map<String, BigDecimal> ratios = integrityProvider.calculateGroupRatios(
                "档位+诚信互助小组", regions, matrix, null);

        assertEquals(new BigDecimal("0.5000"), ratios.get("区域A"));
        assertEquals(new BigDecimal("0.5000"), ratios.get("区域B"));
    }

    @Test
    void integrity_每个区域作为独立分组() {
        List<String> regions = Arrays.asList("区域A", "区域B", "区域C");

        Map<String, String> mapping = integrityProvider.getRegionGroupMapping("档位+诚信互助小组", regions);

        assertEquals(3, mapping.size());
        assertEquals("区域A", mapping.get("区域A"));
        assertEquals("区域B", mapping.get("区域B"));
        assertEquals("区域C", mapping.get("区域C"));
    }

    @Test
    void integrity_客户数全为0时返回空Map() {
        List<String> regions = Arrays.asList("区域A", "区域B");
        BigDecimal[][] matrix = new BigDecimal[][] {
            {BigDecimal.ZERO, BigDecimal.ZERO},
            {BigDecimal.ZERO, BigDecimal.ZERO}
        };

        Map<String, BigDecimal> ratios = integrityProvider.calculateGroupRatios(
                "档位+诚信互助小组", regions, matrix, null);

        assertTrue(ratios.isEmpty());
    }
}
