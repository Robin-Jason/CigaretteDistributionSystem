package org.example.application.service;

import org.example.infrastructure.persistence.po.CigaretteDistributionPredictionPO;

import java.util.List;

/**
 * 语义编码服务（精简版）。
 * 仅保留“一键生成分配方案”流程所需的区域编码能力。
 *
 * @author Robin
 * @version 4.1
 * @since 2025-12-11
 */
public interface EncodeService {

    /**
     * 为特定区域生成编码表达式（单卷烟单区域）。
     *
     * @param cigCode               卷烟代码
     * @param cigName               卷烟名称
     * @param deliveryMethod        投放方式
     * @param deliveryEtype         扩展投放类型
     * @param targetArea            目标区域名称
     * @param allCigaretteRecords   全部区域的投放记录
     * @return 编码表达式，失败时返回空字符串
     */
    String encodeForSpecificArea(String cigCode, String cigName, String deliveryMethod, String deliveryEtype,
                                 String targetArea, List<CigaretteDistributionPredictionPO> allCigaretteRecords);
}

