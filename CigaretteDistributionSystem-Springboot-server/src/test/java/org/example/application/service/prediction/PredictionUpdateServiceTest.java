package org.example.application.service.prediction;

import lombok.extern.slf4j.Slf4j;
import org.example.application.dto.prediction.UpdateRegionGradesDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 修改区域档位值功能测试类
 *
 * @author Robin
 * @since 2025-12-22
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("修改区域档位值功能测试")
class PredictionUpdateServiceTest {

    @Autowired
    private PredictionUpdateService predictionUpdateService;

    private static final Integer TEST_YEAR = 2025;
    private static final Integer TEST_MONTH = 9;
    private static final Integer TEST_WEEK_SEQ = 3;

    @Test
    @DisplayName("参数校验 - 年份为空")
    void testUpdate_YearNull() {
        UpdateRegionGradesDto dto = buildDto();
        dto.setYear(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> predictionUpdateService.updateRegionGrades(dto));
        assertTrue(ex.getMessage().contains("年份"));
        log.info("✅ 年份校验通过: {}", ex.getMessage());
    }

    @Test
    @DisplayName("参数校验 - 卷烟代码为空")
    void testUpdate_CigCodeEmpty() {
        UpdateRegionGradesDto dto = buildDto();
        dto.setCigCode("");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> predictionUpdateService.updateRegionGrades(dto));
        assertTrue(ex.getMessage().contains("卷烟代码"));
        log.info("✅ 卷烟代码校验通过: {}", ex.getMessage());
    }

    @Test
    @DisplayName("参数校验 - 档位数量错误")
    void testUpdate_GradesCountWrong() {
        UpdateRegionGradesDto dto = buildDto();
        dto.setGrades(new ArrayList<>()); // 空列表

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> predictionUpdateService.updateRegionGrades(dto));
        assertTrue(ex.getMessage().contains("档位"));
        log.info("✅ 档位数量校验通过: {}", ex.getMessage());
    }

    @Test
    @DisplayName("参数校验 - 档位单调性校验")
    void testUpdate_GradesMonotonicity() {
        UpdateRegionGradesDto dto = buildDto();
        // D30=1, D29=5 违反单调性
        List<BigDecimal> grades = new ArrayList<>();
        grades.add(BigDecimal.ONE);
        grades.add(new BigDecimal("5"));
        for (int i = 2; i < 30; i++) {
            grades.add(BigDecimal.ZERO);
        }
        dto.setGrades(grades);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> predictionUpdateService.updateRegionGrades(dto));
        assertTrue(ex.getMessage().contains("单调") || ex.getMessage().contains("D29"));
        log.info("✅ 档位单调性校验通过: {}", ex.getMessage());
    }

    @Test
    @DisplayName("业务校验 - 卷烟不存在")
    void testUpdate_CigaretteNotFound() {
        UpdateRegionGradesDto dto = buildDto();
        dto.setCigCode("99999999");
        dto.setCigName("不存在的卷烟");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> predictionUpdateService.updateRegionGrades(dto));
        assertTrue(ex.getMessage().contains("未找到") || ex.getMessage().contains("不存在"));
        log.info("✅ 卷烟不存在校验通过: {}", ex.getMessage());
    }

    @Test
    @DisplayName("业务校验 - 区域记录不存在")
    void testUpdate_RegionNotFound() {
        UpdateRegionGradesDto dto = buildDto();
        dto.setCigCode("42010020");
        dto.setCigName("红金龙(硬神州腾龙)");
        dto.setPrimaryRegion("不存在的区域");
        dto.setSecondaryRegion(null);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> predictionUpdateService.updateRegionGrades(dto));
        assertTrue(ex.getMessage().contains("未找到"));
        log.info("✅ 区域记录不存在校验通过: {}", ex.getMessage());
    }

    /**
     * 构建测试 DTO
     */
    private UpdateRegionGradesDto buildDto() {
        UpdateRegionGradesDto dto = new UpdateRegionGradesDto();
        dto.setYear(TEST_YEAR);
        dto.setMonth(TEST_MONTH);
        dto.setWeekSeq(TEST_WEEK_SEQ);
        dto.setCigCode("42010020");
        dto.setCigName("红金龙(硬神州腾龙)");
        dto.setPrimaryRegion("全市");
        dto.setSecondaryRegion(null);

        List<BigDecimal> grades = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            grades.add(BigDecimal.ONE);
        }
        dto.setGrades(grades);
        dto.setRemark("测试修改");
        return dto;
    }
}
