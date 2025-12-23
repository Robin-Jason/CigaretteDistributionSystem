package org.example.domain.service.algorithm;

import org.example.domain.model.valueobject.GradeRange;

import java.math.BigDecimal;
import java.util.List;

/**
 * 单层区域分配领域服务接口。
 * 典型场景：按档位投放 / 按价位段自选投放（默认“全市”）等，
 * 仅需要根据客户矩阵和目标量以及输出HG~LG列分配矩阵。
 *
 * @author Robin
 * @version 1.0
 * @since 2025-12-14
 */
public interface SingleLevelDistributionService {

    /**
     * 根据目标区域、客户矩阵与预投放量计算分配矩阵。
     *
     * @param targetRegions        区域列表（顺序与结果矩阵行对应）
     * @param regionCustomerMatrix 区域客户数矩阵，维度 [regionCount][30]
     * @param targetAmount         预投放量
     * @param gradeRange           档位范围，指定计算范围（HG到LG），为null时使用默认范围（D30-D1）
     * @return 分配矩阵，维度 [regionCount][30]，范围外列为0
     */
    BigDecimal[][] distribute(List<String> targetRegions,
                              BigDecimal[][] regionCustomerMatrix,
                              BigDecimal targetAmount,
                              GradeRange gradeRange);
}

