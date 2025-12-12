package org.example.service.orchestrator;

import lombok.extern.slf4j.Slf4j;
import org.example.strategy.DistributionStrategyManager;
import org.example.strategy.orchestrator.StrategyExecutionRequest;
import org.example.strategy.orchestrator.StrategyExecutionResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 分配算法编排器
 * <p>负责组装策略执行请求、调用分配策略管理器并返回统一的计算结果。</p>
 *
 * @author Robin
 * @version 1.0
 * @since 2025-12-11
 */
@Slf4j
@Component
public class DistributionAllocationOrchestrator {

    private final DistributionStrategyManager strategyManager;

    public DistributionAllocationOrchestrator(DistributionStrategyManager strategyManager) {
        this.strategyManager = strategyManager;
    }

    /**
     * 执行算法分配计算。
     *
     * @param cigCode       卷烟代码
     * @param cigName       卷烟名称
     * @param deliveryMethod 投放方法
     * @param deliveryEtype  扩展投放类型
     * @param tag            标签
     * @param deliveryArea   投放区域
     * @param adv            预投放量
     * @param year           年份
     * @param month          月份
     * @param weekSeq        周序号
     * @param advData        原始预投放数据，用于附加信息提取
     * @param marketRatios   市场类型比例参数
     * @param remark         备注（如双周上浮标记）
     * @return 分配计算结果
     */
    public AllocationCalculationResult calculateAllocationMatrix(String cigCode,
                                                                 String cigName,
                                                                 String deliveryMethod,
                                                                 String deliveryEtype,
                                                                 String tag,
                                                                 String deliveryArea,
                                                                 BigDecimal adv,
                                                                 Integer year,
                                                                 Integer month,
                                                                 Integer weekSeq,
                                                                 Map<String, Object> advData,
                                                                 Map<String, BigDecimal> marketRatios,
                                                                 String remark) {

        long startTime = System.currentTimeMillis();
        log.info("【算法分配】开始计算分配矩阵: 卷烟 {} - {}, 组合={}/{}/{}",
                cigCode, cigName, deliveryMethod, deliveryEtype, tag);

        try {
            StrategyExecutionRequest request = new StrategyExecutionRequest();
            request.setDeliveryMethod(deliveryMethod);
            request.setDeliveryEtype(deliveryEtype);
            request.setTag(tag);
            request.setDeliveryArea(deliveryArea);
            request.setTargetAmount(adv);
            request.setYear(year);
            request.setMonth(month);
            request.setWeekSeq(weekSeq);
            request.setRemark(remark);

            Map<String, Object> extraInfoMap = buildExtraInfo(deliveryEtype, advData, marketRatios, cigCode, cigName);
            if (!extraInfoMap.isEmpty()) {
                request.setExtraInfo(extraInfoMap);
            }

            request.setMaxGrade(resolveGrade(advData.get("highest_grade"), "D30"));
            request.setMinGrade(resolveGrade(advData.get("lowest_grade"), "D1"));

            StrategyExecutionResult executionResult = strategyManager.execute(request);

            long elapsed = System.currentTimeMillis() - startTime;
            if (executionResult.isSuccess()) {
                List<String> targetList = executionResult.getTargetRegions();
                BigDecimal[][] allocationMatrix = executionResult.getDistributionMatrix();
                BigDecimal[][] customerMatrix = executionResult.getCustomerMatrix();

                log.info("【算法分配】计算完成: 卷烟={}, 区域数={}, 矩阵维度={}x{}, 耗时={}ms",
                        cigCode, targetList.size(),
                        allocationMatrix != null ? allocationMatrix.length : 0,
                        allocationMatrix != null && allocationMatrix.length > 0 ? allocationMatrix[0].length : 0,
                        elapsed);

                return AllocationCalculationResult.success(targetList, allocationMatrix, customerMatrix, "StrategyOrchestrator");
            } else {
                String errorMessage = executionResult.getMessage();
                log.error("【算法分配】计算失败: 卷烟={}, 错误={}, 耗时={}ms", cigCode, errorMessage, elapsed);
                return AllocationCalculationResult.failure(errorMessage);
            }
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("【算法分配】计算异常: 卷烟={}, 错误={}, 耗时={}ms", cigCode, e.getMessage(), elapsed, e);
            return AllocationCalculationResult.failure("策略执行异常: " + e.getMessage());
        }
    }

    private Map<String, Object> buildExtraInfo(String deliveryEtype,
                                               Map<String, Object> advData,
                                               Map<String, BigDecimal> marketRatios,
                                               String cigCode,
                                               String cigName) {
        Map<String, Object> extraInfoMap = new HashMap<>();

        if ("档位+市场类型".equals(deliveryEtype) && marketRatios != null) {
            BigDecimal urbanRatioParam = marketRatios.get("urbanRatio");
            BigDecimal ruralRatioParam = marketRatios.get("ruralRatio");
            if (urbanRatioParam != null && ruralRatioParam != null) {
                Map<String, BigDecimal> groupRatios = new HashMap<>();
                groupRatios.put("城网", urbanRatioParam);
                groupRatios.put("农网", ruralRatioParam);
                extraInfoMap.put(org.example.strategy.orchestrator.DistributionAlgorithmEngine.EXTRA_GROUP_RATIOS, groupRatios);
                log.debug("卷烟: {} - {}, 传递用户传入的市场类型比例到ExtraInfo - 城网: {}, 农网: {}",
                        cigCode, cigName, urbanRatioParam, ruralRatioParam);
            }
        }

        Object tagFilterConfig = advData.get("TAG_FILTER_CONFIG");
        if (tagFilterConfig != null) {
            extraInfoMap.put("TAG_FILTER_CONFIG", tagFilterConfig);
        }

        return extraInfoMap;
    }

    private String resolveGrade(Object raw, String fallback) {
        if (raw == null) {
            return fallback;
        }
        String value = raw.toString().trim().toUpperCase();
        return value.isEmpty() ? fallback : value;
    }
}

