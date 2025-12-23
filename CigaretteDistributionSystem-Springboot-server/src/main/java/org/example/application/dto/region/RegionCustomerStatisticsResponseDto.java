package org.example.application.dto.region;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 区域客户统计构建结果 DTO。
 */
@Data
public class RegionCustomerStatisticsResponseDto {

    private boolean success;
    private String message;
    private String sourceTable;
    private String targetTable;
    private int processedCombinationCount;
    private int skippedCombinationCount;
    private int insertedRegionCount;
    private int filteredCustomerCount;
    private List<String> appliedCustomerTypes = new ArrayList<>();
    private List<String> appliedWorkdays = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();
    private List<CombinationDetail> combinationDetails = new ArrayList<>();

    @Data
    public static class CombinationDetail {
        private String combinationKey;
        private String method;
        private List<String> extensions = new ArrayList<>();
        private List<String> tags = new ArrayList<>();
        private int regionCount;
        private String status;
        private String remark;
    }
}

