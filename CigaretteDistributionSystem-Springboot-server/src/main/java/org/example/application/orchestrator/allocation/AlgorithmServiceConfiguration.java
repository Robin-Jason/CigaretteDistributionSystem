package org.example.application.orchestrator.allocation;

import org.example.domain.service.algorithm.ColumnWiseAdjustmentService;
import org.example.domain.service.algorithm.GroupSplittingDistributionService;
import org.example.domain.service.algorithm.SingleLevelDistributionService;
import org.example.domain.service.algorithm.impl.ColumnWiseAdjustmentServiceImpl;
import org.example.domain.service.algorithm.impl.GroupSplittingDistributionServiceImpl;
import org.example.domain.service.algorithm.impl.SingleLevelDistributionServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 分配算法领域服务的 Spring 装配配置。
 * <p>
 * 通过 {@code @Bean} 将纯领域算法实现注册到 Spring 容器中，
 * 供 {@link DistributionAlgorithmEngine} 等应用服务按依赖倒置原则调用。
 * </p>
 */
@Configuration
public class AlgorithmServiceConfiguration {

    /**
     * 单层分配领域服务。
     *
     * @return 单层分配服务实现
     */
    @Bean
    public SingleLevelDistributionService singleLevelDistributionService() {
        return new SingleLevelDistributionServiceImpl();
    }

    /**
     * 按列分配领域服务。
     *
     * @return 按列分配服务实现
     */
    @Bean
    public ColumnWiseAdjustmentService columnWiseAdjustmentService() {
        return new ColumnWiseAdjustmentServiceImpl();
    }

    /**
     * 分组拆分领域服务。
     *
     * @param singleLevelDistributionService 单层分配领域服务
     * @param columnWiseAdjustmentService    按列分配领域服务
     * @return 多区域带权重分配服务实现
     */
    @Bean
    public GroupSplittingDistributionService groupSplittingDistributionService(
            SingleLevelDistributionService singleLevelDistributionService,
            ColumnWiseAdjustmentService columnWiseAdjustmentService) {
        return new GroupSplittingDistributionServiceImpl(singleLevelDistributionService, columnWiseAdjustmentService);
    }
}


