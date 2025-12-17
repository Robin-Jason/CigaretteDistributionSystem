package org.example.application.facade;

import lombok.extern.slf4j.Slf4j;
import org.example.application.orchestrator.strategy.StrategyExecutionRequest;
import org.example.application.orchestrator.strategy.StrategyExecutionResult;
import org.example.application.orchestrator.strategy.StrategyOrchestrator;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 分配策略管理器默认实现。
 * <p>
 * - 对上暴露业务语义的 {@link DistributionStrategyCommand}；
 * - 在内部负责：
 *   1）统一的基础校验与默认值处理；
 *   2）将 Command 转换为 {@link StrategyExecutionRequest}；
 *   3）统一埋点/日志入口；
 *   4）委托 {@link StrategyOrchestrator} 执行策略。
 * </p>
 */
@Slf4j
@Component
public class DefaultDistributionStrategyManager implements DistributionStrategyManager {

    private final StrategyOrchestrator strategyOrchestrator;

    public DefaultDistributionStrategyManager(StrategyOrchestrator strategyOrchestrator) {
        this.strategyOrchestrator = strategyOrchestrator;
        log.info("DefaultDistributionStrategyManager 初始化完成，统一通过 StrategyOrchestrator 执行分配策略");
    }

    @Override
    public StrategyExecutionResult executeDistributionStrategy(DistributionStrategyCommand command) {
        // 基础校验（防御式）：避免明显错误请求继续往下传递
        if (command == null) {
            log.warn("执行分配策略失败：command 为空");
            return StrategyExecutionResult.failure("策略执行命令不能为空");
        }
        if (command.getTargetAmount() == null
                || command.getTargetAmount().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("执行分配策略失败：目标量非法，targetAmount={}", command.getTargetAmount());
            return StrategyExecutionResult.failure("预投放量必须为正数");
        }
        if (command.getYear() == null || command.getMonth() == null || command.getWeekSeq() == null) {
            log.warn("执行分配策略失败：批次信息不完整 year={}, month={}, weekSeq={}",
                    command.getYear(), command.getMonth(), command.getWeekSeq());
            return StrategyExecutionResult.failure("批次信息(year/month/weekSeq)不能为空");
        }

        StrategyExecutionRequest request = new StrategyExecutionRequest();
        request.setDeliveryMethod(command.getDeliveryMethod());
        request.setDeliveryEtype(command.getDeliveryEtype());
        request.setTag(command.getTag());
        request.setDeliveryArea(command.getDeliveryArea());
        request.setTargetAmount(command.getTargetAmount());
        request.setYear(command.getYear());
        request.setMonth(command.getMonth());
        request.setWeekSeq(command.getWeekSeq());
        request.setRemark(command.getRemark());

        // ExtraInfo：合并调用方传入的扩展信息
        Map<String, Object> extraInfo = new HashMap<>();
        if (command.getExtraInfo() != null && !command.getExtraInfo().isEmpty()) {
            extraInfo.putAll(command.getExtraInfo());
        }
        if (!extraInfo.isEmpty()) {
            request.setExtraInfo(extraInfo);
        }

        // 档位范围默认值处理
        String maxGrade = (command.getMaxGrade() == null || command.getMaxGrade().trim().isEmpty())
                ? "D30" : command.getMaxGrade().trim().toUpperCase();
        String minGrade = (command.getMinGrade() == null || command.getMinGrade().trim().isEmpty())
                ? "D1" : command.getMinGrade().trim().toUpperCase();
        request.setMaxGrade(maxGrade);
        request.setMinGrade(minGrade);

        log.info("开始执行分配策略，deliveryMethod={}, deliveryEtype={}, tag={}, year/month/weekSeq={}–{}–{}",
                command.getDeliveryMethod(), command.getDeliveryEtype(), command.getTag(),
                command.getYear(), command.getMonth(), command.getWeekSeq());

        return strategyOrchestrator.execute(request);
    }
}


