package org.example.application.service.adjust;

import lombok.extern.slf4j.Slf4j;
import org.example.application.dto.allocation.AdjustCigaretteStrategyRequestDto;
import org.example.application.dto.allocation.AdjustCigaretteStrategyResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 卷烟投放策略调整服务测试
 *
 * @author Robin
 * @since 2025-12-22
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CigaretteStrategyAdjustServiceTest {

    @Autowired
    private CigaretteStrategyAdjustService adjustService;

    private static final Integer YEAR = 2025;
    private static final Integer MONTH = 9;
    private static final Integer WEEK_SEQ = 3;

    // ==================== 参数校验测试 ====================

    @Test
    @DisplayName("参数校验：年份为空")
    void testAdjust_YearNull() {
        AdjustCigaretteStrategyRequestDto request = buildBaseRequest();
        request.setYear(null);

        AdjustCigaretteStrategyResponseDto response = adjustService.adjustStrategy(request);

        assertFalse(response.getSuccess());
        assertTrue(response.getMessage().contains("年份"));
        log.info("✅ 年份校验通过: {}", response.getMessage());
    }

    @Test
    @DisplayName("参数校验：月份无效")
    void testAdjust_MonthInvalid() {
        AdjustCigaretteStrategyRequestDto request = buildBaseRequest();
        request.setMonth(13);

        AdjustCigaretteStrategyResponseDto response = adjustService.adjustStrategy(request);

        assertFalse(response.getSuccess());
        assertTrue(response.getMessage().contains("月份"));
        log.info("✅ 月份校验通过: {}", response.getMessage());
    }

    @Test
    @DisplayName("参数校验：卷烟代码为空")
    void testAdjust_CigCodeEmpty() {
        AdjustCigaretteStrategyRequestDto request = buildBaseRequest();
        request.setCigCode("");

        AdjustCigaretteStrategyResponseDto response = adjustService.adjustStrategy(request);

        assertFalse(response.getSuccess());
        assertTrue(response.getMessage().contains("卷烟代码"));
        log.info("✅ 卷烟代码校验通过: {}", response.getMessage());
    }

    @Test
    @DisplayName("参数校验：新投放类型为空")
    void testAdjust_DeliveryMethodEmpty() {
        AdjustCigaretteStrategyRequestDto request = buildBaseRequest();
        request.setNewDeliveryMethod("");

        AdjustCigaretteStrategyResponseDto response = adjustService.adjustStrategy(request);

        assertFalse(response.getSuccess());
        assertTrue(response.getMessage().contains("投放类型"));
        log.info("✅ 投放类型校验通过: {}", response.getMessage());
    }

    @Test
    @DisplayName("参数校验：建议投放量为0")
    void testAdjust_AdvAmountZero() {
        AdjustCigaretteStrategyRequestDto request = buildBaseRequest();
        request.setNewAdvAmount(BigDecimal.ZERO);

        AdjustCigaretteStrategyResponseDto response = adjustService.adjustStrategy(request);

        assertFalse(response.getSuccess());
        assertTrue(response.getMessage().contains("投放量"));
        log.info("✅ 建议投放量校验通过: {}", response.getMessage());
    }

    @Test
    @DisplayName("参数校验：有标签但无过滤值")
    void testAdjust_TagWithoutFilterValue() {
        AdjustCigaretteStrategyRequestDto request = buildBaseRequest();
        request.setNewTag("优质数据共享客户");
        request.setNewTagFilterValue(null);

        AdjustCigaretteStrategyResponseDto response = adjustService.adjustStrategy(request);

        assertFalse(response.getSuccess());
        assertTrue(response.getMessage().contains("过滤值"));
        log.info("✅ 标签过滤值校验通过: {}", response.getMessage());
    }

    // ==================== 业务校验测试 ====================

    @Test
    @DisplayName("业务校验：卷烟不存在")
    void testAdjust_CigaretteNotFound() {
        AdjustCigaretteStrategyRequestDto request = buildBaseRequest();
        request.setCigCode("99999999");
        request.setCigName("不存在的卷烟");

        AdjustCigaretteStrategyResponseDto response = adjustService.adjustStrategy(request);

        assertFalse(response.getSuccess());
        assertTrue(response.getMessage().contains("卷烟不存在") || response.getMessage().contains("未找到"));
        log.info("✅ 卷烟不存在校验通过: {}", response.getMessage());
    }

    @Test
    @DisplayName("业务校验：动态标签不存在")
    void testAdjust_DynamicTagNotFound() {
        AdjustCigaretteStrategyRequestDto request = buildBaseRequest();
        // 使用存在的卷烟
        request.setCigCode("42010020");
        request.setCigName("红金龙(硬神州腾龙)");
        // 使用不存在的动态标签
        request.setNewTag("不存在的标签ABC");
        request.setNewTagFilterValue("是");

        AdjustCigaretteStrategyResponseDto response = adjustService.adjustStrategy(request);

        assertFalse(response.getSuccess());
        assertTrue(response.getMessage().contains("标签") && response.getMessage().contains("不存在"));
        log.info("✅ 动态标签不存在校验通过: {}", response.getMessage());
    }

    @Test
    @DisplayName("业务校验：动态标签存在（优质客户）")
    void testAdjust_DynamicTagExists() {
        AdjustCigaretteStrategyRequestDto request = buildBaseRequest();
        request.setCigCode("42010020");
        request.setCigName("红金龙(硬神州腾龙)");
        // 使用存在的动态标签
        request.setNewTag("优质客户");
        request.setNewTagFilterValue("是");

        AdjustCigaretteStrategyResponseDto response = adjustService.adjustStrategy(request);

        // 如果动态标签存在，应该继续执行（可能因其他原因失败，但不应该是标签不存在）
        if (!response.getSuccess()) {
            assertFalse(response.getMessage().contains("标签") && response.getMessage().contains("不存在"),
                    "不应该因为标签不存在而失败");
        }
        log.info("✅ 动态标签存在校验: success={}, message={}", response.getSuccess(), response.getMessage());
    }

    @Test
    @DisplayName("业务校验：固定标签存在（优质数据共享客户）")
    void testAdjust_FixedTagExists() {
        AdjustCigaretteStrategyRequestDto request = buildBaseRequest();
        request.setCigCode("42010020");
        request.setCigName("红金龙(硬神州腾龙)");
        // 使用固定标签
        request.setNewTag("优质数据共享客户");
        request.setNewTagFilterValue("是");

        AdjustCigaretteStrategyResponseDto response = adjustService.adjustStrategy(request);

        // 如果固定标签存在，应该继续执行（可能因其他原因失败，但不应该是标签不存在）
        if (!response.getSuccess()) {
            assertFalse(response.getMessage().contains("标签") && response.getMessage().contains("不存在"),
                    "不应该因为标签不存在而失败");
        }
        log.info("✅ 固定标签存在校验: success={}, message={}", response.getSuccess(), response.getMessage());
    }

    // ==================== 辅助方法 ====================

    private AdjustCigaretteStrategyRequestDto buildBaseRequest() {
        AdjustCigaretteStrategyRequestDto request = new AdjustCigaretteStrategyRequestDto();
        request.setYear(YEAR);
        request.setMonth(MONTH);
        request.setWeekSeq(WEEK_SEQ);
        request.setCigCode("42010020");
        request.setCigName("红金龙(硬神州腾龙)");
        request.setNewDeliveryMethod("按档位投放");
        request.setNewAdvAmount(new BigDecimal("1000"));
        return request;
    }
}
