package org.example.domain.model.valueobject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 区域客户数矩阵（值对象）。
 *
 * <p>用途：</p>
 * <ul>
 *   <li>承载 {@code region_customer_statistics} 分区表中各区域的 30 档位客户数；</li>
 *   <li>在分配算法中作为输入矩阵，行顺序与目标区域列表对齐。</li>
 * </ul>
 *
 * @author Robin
 */
public class RegionCustomerMatrix {
    
    private final List<Row> rows;
    
    /**
     * 构造区域客户矩阵。
     *
     * @param rows 行列表（每行一个区域与 30 档位客户数）
     */
    public RegionCustomerMatrix(List<Row> rows) {
        this.rows = rows != null ? rows : new ArrayList<>();
    }
    
    /**
     * 获取矩阵行列表。
     *
     * @return 行列表（可为空但不为 null）
     */
    public List<Row> getRows() {
        return rows;
    }
    
    /**
     * 判断矩阵是否为空。
     *
     * @return true 表示无任何区域行
     */
    public boolean isEmpty() {
        return rows.isEmpty();
    }
    
    /**
     * 区域行数据。
     */
    public static class Row {
        private final String region;
        private final BigDecimal[] grades; // 30个档位值（D30到D1）
        
        /**
         * 构造矩阵行。
         *
         * @param region 区域名称
         * @param grades 30 档位客户数（D30-D1），允许 null（将退化为空数组）
         */
        public Row(String region, BigDecimal[] grades) {
            this.region = region;
            this.grades = grades != null ? grades : new BigDecimal[30];
        }
        
        /**
         * 获取区域名称。
         *
         * @return 区域名称
         */
        public String getRegion() {
            return region;
        }
        
        /**
         * 获取 30 档位客户数数组（D30-D1）。
         *
         * @return 档位数组
         */
        public BigDecimal[] getGrades() {
            return grades;
        }
    }
}
