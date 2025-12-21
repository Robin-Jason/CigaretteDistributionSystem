package org.example.domain.service.rule;

import java.math.BigDecimal;

/**
 * 价位段规则领域服务接口。
 *
 * <p>根据卷烟批发价（WHOLESALE_PRICE）解析价位段编号，不依赖具体存储或配置来源。</p>
 *
 * @author Robin
 * @since 2025-12-18
 */
public interface PriceBandRule {

    /**
     * 根据批发价解析价位段编号。
     *
     * @param wholesalePrice 批发价（WHOLESALE_PRICE），允许为 null
     *
     * @return 价位段编号（如 1~9）；如果不在任一配置段内或批发价为空，则返回 0
     *
     * @example wholesalePrice=650 -> 1；wholesalePrice=395 -> 2；wholesalePrice&lt;109 或 null -> 0
     */
    int resolveBand(BigDecimal wholesalePrice);
}


