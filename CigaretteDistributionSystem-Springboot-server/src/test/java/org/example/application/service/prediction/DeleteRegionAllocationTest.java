package org.example.application.service.prediction;

import org.example.application.dto.prediction.DeleteCigaretteDto;
import org.example.application.dto.prediction.DeleteRegionAllocationDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 删除区域分配功能测试类
 *
 * @author Robin
 * @since 2025-12-22
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DeleteRegionAllocationTest {

    @Autowired
    private PredictionDeleteService predictionDeleteService;

    // 测试数据：2025/9/3 分区
    private static final Integer TEST_YEAR = 2025;
    private static final Integer TEST_MONTH = 9;
    private static final Integer TEST_WEEK_SEQ = 3;

    @Test
    @DisplayName("删除特定区域 - 参数校验：年份为空")
    void testDeleteRegion_YearNull() {
        DeleteRegionAllocationDto dto = new DeleteRegionAllocationDto();
        dto.setYear(null);
        dto.setMonth(TEST_MONTH);
        dto.setWeekSeq(TEST_WEEK_SEQ);
        dto.setCigCode("42010020");
        dto.setCigName("红金龙（硬神州腾龙）");
        dto.setPrimaryRegion("城区");
        dto.setSecondaryRegion("城网");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> predictionDeleteService.deleteRegionAllocation(dto));
        assertTrue(ex.getMessage().contains("年份"));
    }

    @Test
    @DisplayName("删除特定区域 - 参数校验：卷烟代码为空")
    void testDeleteRegion_CigCodeEmpty() {
        DeleteRegionAllocationDto dto = new DeleteRegionAllocationDto();
        dto.setYear(TEST_YEAR);
        dto.setMonth(TEST_MONTH);
        dto.setWeekSeq(TEST_WEEK_SEQ);
        dto.setCigCode("");
        dto.setCigName("红金龙（硬神州腾龙）");
        dto.setPrimaryRegion("城区");
        dto.setSecondaryRegion("城网");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> predictionDeleteService.deleteRegionAllocation(dto));
        assertTrue(ex.getMessage().contains("卷烟代码"));
    }

    @Test
    @DisplayName("删除特定区域 - 参数校验：主区域为空")
    void testDeleteRegion_PrimaryRegionEmpty() {
        DeleteRegionAllocationDto dto = new DeleteRegionAllocationDto();
        dto.setYear(TEST_YEAR);
        dto.setMonth(TEST_MONTH);
        dto.setWeekSeq(TEST_WEEK_SEQ);
        dto.setCigCode("42010020");
        dto.setCigName("红金龙（硬神州腾龙）");
        dto.setPrimaryRegion("");
        dto.setSecondaryRegion("城网");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> predictionDeleteService.deleteRegionAllocation(dto));
        assertTrue(ex.getMessage().contains("主投放区域"));
    }

    @Test
    @DisplayName("删除特定区域 - 卷烟不存在")
    void testDeleteRegion_CigaretteNotFound() {
        DeleteRegionAllocationDto dto = new DeleteRegionAllocationDto();
        dto.setYear(TEST_YEAR);
        dto.setMonth(TEST_MONTH);
        dto.setWeekSeq(TEST_WEEK_SEQ);
        dto.setCigCode("99999999");
        dto.setCigName("不存在的卷烟");
        dto.setPrimaryRegion("城区");
        dto.setSecondaryRegion("城网");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> predictionDeleteService.deleteRegionAllocation(dto));
        assertTrue(ex.getMessage().contains("未找到") || ex.getMessage().contains("不存在"));
    }

    @Test
    @DisplayName("删除整个卷烟 - 参数校验：月份无效")
    void testDeleteCigarette_MonthInvalid() {
        DeleteCigaretteDto dto = new DeleteCigaretteDto();
        dto.setYear(TEST_YEAR);
        dto.setMonth(13);
        dto.setWeekSeq(TEST_WEEK_SEQ);
        dto.setCigCode("42010020");
        dto.setCigName("红金龙（硬神州腾龙）");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> predictionDeleteService.deleteCigarette(dto));
        assertTrue(ex.getMessage().contains("月份"));
    }

    @Test
    @DisplayName("删除整个卷烟 - 参数校验：周序号无效")
    void testDeleteCigarette_WeekSeqInvalid() {
        DeleteCigaretteDto dto = new DeleteCigaretteDto();
        dto.setYear(TEST_YEAR);
        dto.setMonth(TEST_MONTH);
        dto.setWeekSeq(6);
        dto.setCigCode("42010020");
        dto.setCigName("红金龙（硬神州腾龙）");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> predictionDeleteService.deleteCigarette(dto));
        assertTrue(ex.getMessage().contains("周序号"));
    }

    @Test
    @DisplayName("删除整个卷烟 - 卷烟不存在")
    void testDeleteCigarette_CigaretteNotFound() {
        DeleteCigaretteDto dto = new DeleteCigaretteDto();
        dto.setYear(TEST_YEAR);
        dto.setMonth(TEST_MONTH);
        dto.setWeekSeq(TEST_WEEK_SEQ);
        dto.setCigCode("99999999");
        dto.setCigName("不存在的卷烟");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> predictionDeleteService.deleteCigarette(dto));
        assertTrue(ex.getMessage().contains("未找到") || ex.getMessage().contains("不存在"));
    }
}
