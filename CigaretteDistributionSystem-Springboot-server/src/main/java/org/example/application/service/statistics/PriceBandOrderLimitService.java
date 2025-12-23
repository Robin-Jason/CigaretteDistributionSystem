package org.example.application.service.statistics;

import java.util.Map;

/**
 * 价位段订购量上限统计服务接口。
 * <p>
 * 根据按价位段自选投放的分配结果，计算每个价位段各档位的订购量上限。
 * </p>
 *
 * @author Robin
 * @since 2025-12-22
 */
public interface PriceBandOrderLimitService {

    /**
     * 查询指定时间分区的价位段订购量上限。
     * <p>
     * 计算规则：价位段订购量上限 = 价位段单档位投放量之和 / 设定阈值（向下取整）
     * </p>
     *
     * @param year    年份
     * @param month   月份
     * @param weekSeq 周序号
     * @return 价位段订购量上限统计结果
     */
    PriceBandOrderLimitResult queryOrderLimits(Integer year, Integer month, Integer weekSeq);

    /**
     * 价位段订购量上限统计结果。
     */
    class PriceBandOrderLimitResult {
        private java.math.BigDecimal threshold;
        private Map<Integer, PriceBandLimitDetail> priceBandLimits;

        public java.math.BigDecimal getThreshold() {
            return threshold;
        }

        public void setThreshold(java.math.BigDecimal threshold) {
            this.threshold = threshold;
        }

        public Map<Integer, PriceBandLimitDetail> getPriceBandLimits() {
            return priceBandLimits;
        }

        public void setPriceBandLimits(Map<Integer, PriceBandLimitDetail> priceBandLimits) {
            this.priceBandLimits = priceBandLimits;
        }
    }

    /**
     * 单个价位段的订购量上限详情。
     */
    class PriceBandLimitDetail {
        private String label;
        private int[] limits;

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public int[] getLimits() {
            return limits;
        }

        public void setLimits(int[] limits) {
            this.limits = limits;
        }
    }
}
