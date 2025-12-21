package org.example.application.service.coordinator;

import java.util.List;
import java.util.Map;

/**
 * 价位段自选投放候选卷烟查询服务。
 *
 * <p>职责：基于投放信息分区表和价目表，构建"按价位段升序、组内批发价降序"的候选卷烟列表，
 * 为后续按价位段自选投放算法提供输入。</p>
 *
 * @author Robin
 * @since 2025-12-19
 */
public interface PriceBandCandidateQueryService {

    /**
     * 查询指定分区下"按价位段自选投放"的候选卷烟，并按以下规则排序：
     * <ul>
     *   <li>组间：按价位段编号升序（1 段 → 9 段）；</li>
     *   <li>组内：按批发价 WHOLESALE_PRICE 降序。</li>
     * </ul>
     *
     * @param year    年份
     * @param month   月份
     * @param weekSeq 周序号
     *
     * @return 候选卷烟列表，每条至少包含 CIG_CODE、CIG_NAME、DELIVERY_AREA、DELIVERY_METHOD、DELIVERY_ETYPE、WHOLESALE_PRICE、PRICE_BAND 等字段
     *
     * @example 查询 2025-09-03 批次，并基于结果继续执行价位段自选投放算法
     */
    List<Map<String, Object>> listOrderedPriceBandCandidates(Integer year, Integer month, Integer weekSeq);
}

