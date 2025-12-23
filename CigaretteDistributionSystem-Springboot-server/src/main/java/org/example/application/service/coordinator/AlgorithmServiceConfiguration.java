package org.example.application.service.coordinator;

import org.example.domain.service.algorithm.ColumnWiseAdjustmentService;
import org.example.domain.service.algorithm.GroupSplittingDistributionService;
import org.example.domain.service.algorithm.PriceBandTruncationService;
import org.example.domain.service.algorithm.SingleLevelDistributionService;
import org.example.domain.service.algorithm.impl.ColumnWiseAdjustmentServiceImpl;
import org.example.domain.service.algorithm.impl.GroupSplittingDistributionServiceImpl;
import org.example.domain.service.algorithm.impl.PriceBandTruncationServiceImpl;
import org.example.domain.service.algorithm.impl.SingleLevelDistributionServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 算法服务配置类。
 * <p>
 * 将纯领域算法实现注册到 Spring 容器中，
 * 供 {@link AllocationAlgorithmSelector} 等应用服务按依赖倒置原则调用。
 * </p>
 *
 * @author Robin
 */
@Configuration
public class AlgorithmServiceConfiguration {

    @Bean
    public SingleLevelDistributionService singleLevelDistributionService() {
        return new SingleLevelDistributionServiceImpl();
    }

    @Bean
    public ColumnWiseAdjustmentService columnWiseAdjustmentService() {
        return new ColumnWiseAdjustmentServiceImpl();
    }

    @Bean
    public GroupSplittingDistributionService groupSplittingDistributionService(
            SingleLevelDistributionService singleLevelService,
            ColumnWiseAdjustmentService columnWiseService) {
        return new GroupSplittingDistributionServiceImpl(singleLevelService, columnWiseService);
    }

    @Bean
    public PriceBandTruncationService priceBandTruncationService() {
        return new PriceBandTruncationServiceImpl();
    }
}
