package org.example.application.orchestrator;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 区域客户数矩阵
 * 用于替代已删除的RegionMatrixSnapshot
 */
public class RegionCustomerMatrix {
    
    private final List<Row> rows;
    
    public RegionCustomerMatrix(List<Row> rows) {
        this.rows = rows != null ? rows : new ArrayList<>();
    }
    
    public List<Row> getRows() {
        return rows;
    }
    
    public boolean isEmpty() {
        return rows.isEmpty();
    }
    
    /**
     * 区域行数据
     */
    public static class Row {
        private final String region;
        private final BigDecimal[] grades; // 30个档位值（D30到D1）
        
        public Row(String region, BigDecimal[] grades) {
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

