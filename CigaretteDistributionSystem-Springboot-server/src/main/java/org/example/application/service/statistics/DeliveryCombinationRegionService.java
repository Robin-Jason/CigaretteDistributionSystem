package org.example.application.service.statistics;

import java.util.List;
import java.util.Map;

/**
 * 投放组合与区域映射查询服务接口。
 * <p>
 * 根据分配结果查询本次分配所包含的所有投放组合及其对应的区域全集。
 * </p>
 *
 * @author Robin
 * @since 2025-12-22
 */
public interface DeliveryCombinationRegionService {

    /**
     * 查询指定时间分区的投放组合与区域映射。
     *
     * @param year    年份
     * @param month   月份
     * @param weekSeq 周序号
     * @return 投放组合与区域映射结果
     */
    DeliveryCombinationRegionResult queryCombinationRegions(Integer year, Integer month, Integer weekSeq);

    /**
     * 投放组合与区域映射结果。
     */
    class DeliveryCombinationRegionResult {
        private List<CombinationRegionMapping> combinations;

        public List<CombinationRegionMapping> getCombinations() {
            return combinations;
        }

        public void setCombinations(List<CombinationRegionMapping> combinations) {
            this.combinations = combinations;
        }
    }

    /**
     * 单个投放组合的区域映射。
     */
    class CombinationRegionMapping {
        private String deliveryMethod;
        private String deliveryEtype;
        private String tag;
        
        /**
         * 单扩展时的区域列表
         */
        private List<String> regions;
        
        /**
         * 双扩展时的区域映射（key为扩展类型名称，value为该扩展类型的区域列表）
         */
        private Map<String, List<String>> extensionRegions;

        public String getDeliveryMethod() {
            return deliveryMethod;
        }

        public void setDeliveryMethod(String deliveryMethod) {
            this.deliveryMethod = deliveryMethod;
        }

        public String getDeliveryEtype() {
            return deliveryEtype;
        }

        public void setDeliveryEtype(String deliveryEtype) {
            this.deliveryEtype = deliveryEtype;
        }

        public String getTag() {
            return tag;
        }

        public void setTag(String tag) {
            this.tag = tag;
        }

        public List<String> getRegions() {
            return regions;
        }

        public void setRegions(List<String> regions) {
            this.regions = regions;
        }

        public Map<String, List<String>> getExtensionRegions() {
            return extensionRegions;
        }

        public void setExtensionRegions(Map<String, List<String>> extensionRegions) {
            this.extensionRegions = extensionRegions;
        }
    }
}
