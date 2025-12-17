package org.example.application.facade;

/**
 * 分配策略管理器 - 接口
 * <p>
 * 作为“执行分配策略”的业务入口，对上隐藏具体的策略编排实现细节。
 * </p>
 */
public interface DistributionStrategyManager {

    /**
     * 执行分配策略。
     *
     * @param command 业务语义的策略执行命令
     * @return 策略执行结果
     */
    org.example.application.orchestrator.strategy.StrategyExecutionResult executeDistributionStrategy(DistributionStrategyCommand command);
}

