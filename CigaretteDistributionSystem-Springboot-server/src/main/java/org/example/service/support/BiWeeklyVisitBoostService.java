package org.example.service.support;

import org.example.strategy.orchestrator.RegionCustomerMatrix;

/**
 * "两周一访上浮100%"双周/单周客户上浮服务接口
 * <p>
 * 负责对区域客户数矩阵进行"两周一访上浮100%"处理。
 * 当备注中包含"双周上浮"或"两周一访上浮100%"时，对单周/双周客户的客户数进行上浮处理。
 * </p>
 *
 * @author Robin
 * @version 1.0
 * @since 2025-12-11
 */
public interface BiWeeklyVisitBoostService {

    /**
     * 对给定卷烟的待投放区域客户数矩阵做"两周一访上浮100%"处理
     * <p>
     * 根据备注信息判断是否需要执行上浮处理。
     * 如果需要上浮，则对单周/双周客户的客户数进行100%上浮（即乘以2）。
     * </p>
     *
     * @param baseMatrix     已从region_customer_statistics构建好的子矩阵
     * @param year           年份
     * @param month          月份
     * @param weekSeq        周序号
     * @param deliveryMethod 投放类型（如："按档位投放"）
     * @param deliveryEtype  扩展投放类型（如："区县公司+市场类型"）
     * @param tag            标签
     * @param deliveryArea   投放区域字符串（如："全市"）
     * @param remark         BZ备注（如："两周一访上浮100%"）
     * @param extraInfo      额外信息（可选）
     * @return 处理后的矩阵；如无需/暂不执行上浮，则返回原矩阵引用
     * @example
     * <pre>
     *     RegionCustomerMatrix baseMatrix = ...;
     *     RegionCustomerMatrix boostedMatrix = service.applyBiWeeklyBoostIfNeeded(
     *         baseMatrix, 2025, 9, 3, "按档位投放", null, null, "全市", "两周一访上浮100%", null
     *     );
     *     // 返回: 上浮后的矩阵（单周/双周客户数乘以2）
     * </pre>
     */
    RegionCustomerMatrix applyBiWeeklyBoostIfNeeded(
        RegionCustomerMatrix baseMatrix,
        Integer year,
        Integer month,
        Integer weekSeq,
        String deliveryMethod,
        String deliveryEtype,
        String tag,
        String deliveryArea,
        String remark,
        java.util.Map<String, Object> extraInfo
    );
}


