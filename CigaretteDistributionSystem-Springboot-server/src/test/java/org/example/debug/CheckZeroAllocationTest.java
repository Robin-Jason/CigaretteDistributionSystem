package org.example.debug;

import lombok.extern.slf4j.Slf4j;
import org.example.application.service.coordinator.CustomerMatrixBuilder;
import org.example.domain.model.valueobject.GradeRange;
import org.example.domain.model.valueobject.RegionCustomerMatrix;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 调试测试：检查实际投放为0的卷烟
 */
@Slf4j
@SpringBootTest
public class CheckZeroAllocationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CustomerMatrixBuilder customerMatrixBuilder;

    @Test
    public void checkZeroAllocationCigarettes() {
        int year = 2025;
        int month = 9;
        int weekSeq = 3;

        // 查询这3个卷烟的配置
        String[] cigNames = {"娇子(格调细支)", "红金龙(硬神州腾龙)", "利群(长嘴)"};
        
        for (String cigName : cigNames) {
            log.info("\n========== 检查卷烟: {} ==========", cigName);
            
            String sql = "SELECT CIG_CODE, CIG_NAME, HIGHEST_GRADE, LOWEST_GRADE, ADV, DELIVERY_METHOD, DELIVERY_AREA " +
                        "FROM cigarette_distribution_info " +
                        "WHERE YEAR = ? AND MONTH = ? AND WEEK_SEQ = ? AND CIG_NAME = ?";
            
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, year, month, weekSeq, cigName);
            
            if (results.isEmpty()) {
                log.warn("未找到卷烟: {}", cigName);
                continue;
            }
            
            for (Map<String, Object> row : results) {
                String cigCode = (String) row.get("CIG_CODE");
                String hg = (String) row.get("HIGHEST_GRADE");
                String lg = (String) row.get("LOWEST_GRADE");
                BigDecimal adv = (BigDecimal) row.get("ADV");
                String deliveryMethod = (String) row.get("DELIVERY_METHOD");
                String deliveryArea = (String) row.get("DELIVERY_AREA");
                
                log.info("卷烟代码: {}", cigCode);
                log.info("投放方式: {}", deliveryMethod);
                log.info("投放区域: {}", deliveryArea);
                log.info("HG: {}, LG: {}", hg, lg);
                log.info("ADV: {}", adv);
                
                // 创建 GradeRange
                GradeRange gradeRange = GradeRange.of(hg, lg);
                log.info("档位范围: {} (索引 {} 到 {})", gradeRange, gradeRange.getMaxIndex(), gradeRange.getMinIndex());
                
                // 获取客户矩阵
                try {
                    RegionCustomerMatrix matrix = customerMatrixBuilder.buildWithBoost(
                            year, month, weekSeq, deliveryArea, deliveryMethod, null, null, null, null);
                    
                    if (matrix.isEmpty()) {
                        log.warn("客户矩阵为空！");
                        continue;
                    }
                    
                    // 检查范围内的客户数
                    BigDecimal[] customerRow = matrix.getRows().get(0).getGrades();
                    log.info("客户矩阵列数: {}", customerRow.length);
                    
                    BigDecimal totalInRange = BigDecimal.ZERO;
                    for (int i = gradeRange.getMaxIndex(); i <= gradeRange.getMinIndex(); i++) {
                        BigDecimal count = customerRow[i];
                        if (count.compareTo(BigDecimal.ZERO) > 0) {
                            log.info("  档位索引 {} ({}): {} 个客户", i, getGradeName(i), count);
                        }
                        totalInRange = totalInRange.add(count);
                    }
                    
                    log.info("范围内总客户数: {}", totalInRange);
                    
                    if (totalInRange.compareTo(BigDecimal.ZERO) == 0) {
                        log.error("❌ 问题发现：档位范围 [{}-{}] 内客户数全为0，无法分配！", hg, lg);
                    } else {
                        log.info("✅ 范围内有客户，应该可以分配");
                    }
                    
                } catch (Exception e) {
                    log.error("构建客户矩阵失败: {}", e.getMessage(), e);
                }
            }
        }
    }
    
    private String getGradeName(int index) {
        return "D" + (30 - index);
    }
}
