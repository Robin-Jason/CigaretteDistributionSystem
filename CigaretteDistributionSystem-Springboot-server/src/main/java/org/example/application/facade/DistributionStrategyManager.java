package org.example.application.facade;

import lombok.extern.slf4j.Slf4j;
import org.example.application.orchestrator.StrategyExecutionRequest;
import org.example.application.orchestrator.StrategyExecutionResult;
import org.example.application.orchestrator.StrategyOrchestrator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


/**
 * 分配策略管理器
 * 
 * 【核心功能】
 * 提供统一的策略执行接口，统一使用StrategyOrchestrator执行所有投放组合
 * 
 * 【架构改进】
 * - 已移除旧策略（CityDistributionStrategy等），统一使用新流程
 * - 新系统支持：投放类型+扩展投放类型+标签的组合方式
 * - 新系统自动选择三种抽象算法之一：
 *   - SingleLevelDistributionAlgorithm（单区域）
 *   - ColumnWiseAdjustmentAlgorithm（多区域无权重）
 *   - GroupSplittingDistributionAlgorithm（多区域带权重）
 * 
 * 【设计模式】
 * - 编排器模式：StrategyOrchestrator统一编排策略执行
 * - 适配器模式：作为StrategyOrchestrator的适配器
 * 
 * @author Robin
 * @version 2.0 - 统一使用新流程
 * @since 2025-11-29
 */
@Slf4j
@Component
public class DistributionStrategyManager {
    
    private final StrategyOrchestrator strategyOrchestrator;
    
    /**
     * 构造方法，注入StrategyOrchestrator
     * 
     * 【架构改进】
     * - 已移除旧策略注册逻辑
     * - 统一使用StrategyOrchestrator执行所有投放组合
     */
    @Autowired
    public DistributionStrategyManager(StrategyOrchestrator strategyOrchestrator) {
        this.strategyOrchestrator = strategyOrchestrator;
        log.info("分配策略管理器初始化完成，统一使用StrategyOrchestrator执行策略");
    }

    /**
     * 执行策略
     * 统一使用StrategyOrchestrator执行所有投放组合
     * 
     * @param request 策略执行请求
     * @return 策略执行结果
     */
    public StrategyExecutionResult execute(StrategyExecutionRequest request) {
        return strategyOrchestrator.execute(request);
    }
}
