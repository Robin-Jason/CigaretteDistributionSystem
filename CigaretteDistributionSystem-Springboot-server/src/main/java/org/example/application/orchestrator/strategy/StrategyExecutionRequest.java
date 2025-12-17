package org.example.application.orchestrator.strategy;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 策略执行请求（编排器输入 DTO）。
 *
 * <p>典型用途：由 {@code DistributionAllocationOrchestrator} 组装请求后交由 {@code StrategyOrchestrator} 执行。</p>
 *
 * @author Robin
 *
 * @example
 * <pre>
 *     StrategyExecutionRequest req = new StrategyExecutionRequest();
 *     req.setYear(2025);
 *     req.setMonth(9);
 *     req.setWeekSeq(3);
 *     req.setDeliveryMethod("按档位扩展投放");
 *     req.setDeliveryEtype("档位+区县+市场类型");
 *     req.setDeliveryArea("丹江（城网）");
 *     req.setTargetAmount(new BigDecimal("51230"));
 * </pre>
 */
public class StrategyExecutionRequest {

    private String deliveryMethod;
    private String deliveryEtype;
    private String tag;
    private String deliveryArea;
    private BigDecimal targetAmount;
    private Integer year;
    private Integer month;
    private Integer weekSeq;
    private String maxGrade;
    private String minGrade;
    private String remark;
    private Map<String, Object> extraInfo = new HashMap<>();

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

    public String getDeliveryArea() {
        return deliveryArea;
    }

    public void setDeliveryArea(String deliveryArea) {
        this.deliveryArea = deliveryArea;
    }

    public BigDecimal getTargetAmount() {
        return targetAmount;
    }

    public void setTargetAmount(BigDecimal targetAmount) {
        this.targetAmount = targetAmount;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Integer getMonth() {
        return month;
    }

    public void setMonth(Integer month) {
        this.month = month;
    }

    public Integer getWeekSeq() {
        return weekSeq;
    }

    public void setWeekSeq(Integer weekSeq) {
        this.weekSeq = weekSeq;
    }

    public String getMaxGrade() {
        return maxGrade;
    }

    public void setMaxGrade(String maxGrade) {
        this.maxGrade = maxGrade;
    }

    public String getMinGrade() {
        return minGrade;
    }

    public void setMinGrade(String minGrade) {
        this.minGrade = minGrade;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Map<String, Object> getExtraInfo() {
        return extraInfo == null ? Collections.emptyMap() : extraInfo;
    }

    public void setExtraInfo(Map<String, Object> extraInfo) {
        this.extraInfo = extraInfo;
    }
}

