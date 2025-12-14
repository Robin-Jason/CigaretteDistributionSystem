package org.example.domain.service.rule;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 双周访销上浮规则领域服务接口。
 * <p>
 * 定义双周访销上浮的核心业务规则，不依赖于Spring框架或持久化层。
 * 纯领域逻辑，可独立测试。
 * </p>
 *
 * @author Robin
 * @version 1.0
 * @since 2025-12-14
 */
public interface BiWeeklyVisitBoostRule {

    /**
     * 判断备注中是否包含"两周一访上浮100%"关键字。
     *
     * @param remark 备注字符串
     * @return 如果备注包含"两周一访上浮100%"关键字则返回 true，否则返回 false
     */
    boolean needsBoost(String remark);

    /**
     * 将增量叠加到目标区域行。
     * <p>
     * 将单周/双周客户的增量客户数叠加到原始矩阵的对应区域行中。
     * </p>
     *
     * @param rowIndex   区域行索引（区域名 -> 矩阵行）
     * @param increments 区域增量映射（区域名 -> 30档位客户数增量数组）
     */
    void applyBoost(Map<String, MatrixRow> rowIndex, Map<String, BigDecimal[]> increments);

    /**
     * 将矩阵行按区域名索引。
     *
     * @param rows 矩阵行列表
     * @return 区域名称到矩阵行的映射（LinkedHashMap，保持插入顺序）
     */
    Map<String, MatrixRow> indexRows(java.util.List<MatrixRow> rows);

    /**
     * 校验档位数组长度。
     * <p>
     * 确保档位数组长度至少为30，否则抛出异常。
     * </p>
     *
     * @param grades 档位数组（30个档位的客户数）
     * @throws IllegalStateException 如果数组长度小于30
     */
    void ensureLength(BigDecimal[] grades);

    /**
     * 矩阵行数据（值对象）。
     */
    class MatrixRow {
        private final String region;
        private final BigDecimal[] grades; // 30个档位值（D30到D1）

        public MatrixRow(String region, BigDecimal[] grades) {
            this.region = region;
            this.grades = grades != null ? grades : new BigDecimal[30];
        }

        public String getRegion() {
            return region;
        }

        public BigDecimal[] getGrades() {
            return grades;
        }
    }
}

