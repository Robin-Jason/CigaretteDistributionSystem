package org.example.domain.service.algorithm;

import org.example.domain.model.valueobject.GradeRange;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 多区域带权重分配领域服务接口。
 * <p>
 * 定义了多区域带权重分配的核心业务逻辑，不依赖于Spring框架或持久化层。
 * </p>
 *
 * @author Robin
 * @version 1.0
 * @since 2025-12-14
 */
public interface GroupSplittingDistributionService {

    /**
     * 根据分组与比例计算分配矩阵。
     *
     * @param regions               目标区域列表
     * @param customerMatrix        区域客户矩阵 [regionCount][30]
     * @param targetAmount          预投放量
     * @param gradeRange            档位范围，指定计算范围（HG到LG），为null时使用默认范围（D30-D1）
     * @param groupingFunction      区域 -> 分组 ID 的映射函数
     * @param groupRatios           分组比例（总量可不为 1，算法会自动归一化；允许任意数量分组）
     * @return 分配矩阵，维度 [regionCount][30]，范围外列为0
     */
    BigDecimal[][] distribute(List<String> regions,
                              BigDecimal[][] customerMatrix,
                              BigDecimal targetAmount,
                              GradeRange gradeRange,
                              Function<String, String> groupingFunction,
                              Map<String, BigDecimal> groupRatios);
}

