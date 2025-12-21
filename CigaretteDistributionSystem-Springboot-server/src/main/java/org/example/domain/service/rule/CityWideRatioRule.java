package org.example.domain.service.rule;

/**
 * 全市投放占比校验规则领域服务接口。
 *
 * <p>只关心“总卷烟数量 / 全市投放卷烟数量 / 阈值”三者之间的关系，不依赖具体持久化或配置来源。</p>
 *
 * @author Robin
 * @since 2025-12-18
 */
public interface CityWideRatioRule {

    /**
     * @param totalCount       本期投放卷烟总数（记录条数或卷烟支数，取决于调用方约定）
     * @param cityWideCount    投放区域为“全市”的卷烟数量
     * @param minRequiredRatio 最小占比阈值（例如 0.4 表示至少 40%）
     *
     * @example totalCount=100, cityWideCount=45, minRequiredRatio=0.4 -> 校验通过
     */
    void validate(long totalCount, long cityWideCount, double minRequiredRatio);
}


