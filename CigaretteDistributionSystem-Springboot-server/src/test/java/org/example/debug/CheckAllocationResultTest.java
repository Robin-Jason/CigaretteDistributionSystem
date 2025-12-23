package org.example.debug;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 检查分配结果
 */
@Slf4j
@SpringBootTest
public class CheckAllocationResultTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void checkAllocationResults() {
        int year = 2025;
        int month = 9;
        int weekSeq = 3;

        String[] cigNames = {"娇子(格调细支)", "红金龙(硬神州腾龙)", "利群(长嘴)"};
        
        for (String cigName : cigNames) {
            log.info("\n========== 检查卷烟分配结果: {} ==========", cigName);
            
            String sql = "SELECT CIG_CODE, CIG_NAME, DELIVERY_AREA, ACTUAL_DELIVERY, " +
                        "D30, D29, D28, D27, D26, D25, D24, D23, D22, D21, " +
                        "D20, D19, D18, D17, D16, D15, D14, D13, D12, D11, " +
                        "D10, D9, D8, D7, D6, D5, D4, D3, D2, D1 " +
                        "FROM cigarette_distribution_prediction " +
                        "WHERE YEAR = ? AND MONTH = ? AND WEEK_SEQ = ? AND CIG_NAME = ?";
            
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, year, month, weekSeq, cigName);
            
            if (results.isEmpty()) {
                log.warn("未找到分配结果");
                continue;
            }
            
            for (Map<String, Object> row : results) {
                String deliveryArea = (String) row.get("DELIVERY_AREA");
                BigDecimal actualDelivery = (BigDecimal) row.get("ACTUAL_DELIVERY");
                
                log.info("区域: {}, 实际投放: {}", deliveryArea, actualDelivery);
                
                // 检查各档位的分配量
                BigDecimal total = BigDecimal.ZERO;
                for (int i = 30; i >= 1; i--) {
                    String gradeCol = "D" + i;
                    BigDecimal gradeValue = (BigDecimal) row.get(gradeCol);
                    if (gradeValue != null && gradeValue.compareTo(BigDecimal.ZERO) > 0) {
                        log.info("  {}: {}", gradeCol, gradeValue);
                        total = total.add(gradeValue);
                    }
                }
                
                log.info("  档位分配总和: {}", total);
                
                if (total.compareTo(BigDecimal.ZERO) == 0) {
                    log.error("❌ 所有档位分配量都是0！");
                }
            }
        }
    }
}
