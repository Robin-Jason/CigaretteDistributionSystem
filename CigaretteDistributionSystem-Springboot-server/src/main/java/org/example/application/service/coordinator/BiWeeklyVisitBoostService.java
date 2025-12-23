package org.example.application.service.coordinator;

import org.example.domain.model.valueobject.RegionCustomerMatrix;

/**
 * “两周一访上浮100%”双周/单周客户上浮服务。
 *
 * @author Robin
 * @version 1.0
 * @since 2025-12-11
 */
public interface BiWeeklyVisitBoostService {

    /**
     * 对给定卷烟的待投放区域客户数矩阵做“两周一访上浮100%”处理。
     *
     * @param baseMatrix     已从region_customer_statistics构建好的子矩阵
     * @param year           年
     * @param month          月
     * @param weekSeq        周序号
     * @param deliveryMethod 投放类型
     * @param deliveryEtype  扩展投放类型
     * @param tag            标签
     * @param deliveryArea   投放区域字符串
     * @param remark         BZ备注
     * @param extraInfo      额外信息
     * @return 处理后的矩阵；如无需/暂不执行上浮，则返回原矩阵引用
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


