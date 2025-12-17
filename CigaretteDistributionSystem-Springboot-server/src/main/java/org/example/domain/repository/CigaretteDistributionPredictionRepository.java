package org.example.domain.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.example.infrastructure.persistence.po.CigaretteDistributionPredictionPO;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 预测分配表仓储接口（cigarette_distribution_prediction）
 * 暂时作为 Mapper 的抽象门面，后续可替换为领域模型。
 */
public interface CigaretteDistributionPredictionRepository {

    int upsert(CigaretteDistributionPredictionPO data);

    int batchUpsert(List<CigaretteDistributionPredictionPO> list);

    int updateOne(CigaretteDistributionPredictionPO data);

    int updateGrades(Integer year, Integer month, Integer weekSeq,
                    String cigCode, String cigName, String deliveryArea, BigDecimal[] grades);

    List<Map<String, Object>> findAllWithAdv(Integer year, Integer month, Integer weekSeq);

    /**
     * 查询指定年月周下某支卷烟的所有预测记录（按区域）。
     *
     * <p>用于前端“点击卷烟后懒加载聚合编码表达式”等场景，避免全批次查询后再过滤。</p>
     *
     * @param year    年份
     * @param month   月份
     * @param weekSeq 周序号
     * @param cigCode 卷烟代码
     * @return 该卷烟在该批次下的预测数据列表（Map形式，字段同 prediction 表）
     */
    List<Map<String, Object>> findByCigCode(Integer year, Integer month, Integer weekSeq, String cigCode);

    /**
     * 查询指定年月周的所有预测数据
     *
     * @param year    年份
     * @param month   月份
     * @param weekSeq 周序号
     * @return 预测数据列表
     */
    List<Map<String, Object>> findAll(Integer year, Integer month, Integer weekSeq);

    /**
     * 统计指定年月周的记录数
     *
     * @param year    年份
     * @param month   月份
     * @param weekSeq 周序号
     * @return 记录数
     */
    long count(Integer year, Integer month, Integer weekSeq);

    /**
     * 删除指定年月周的所有预测数据
     *
     * @param year    年份
     * @param month   月份
     * @param weekSeq 周序号
     * @return 删除的记录数
     */
    int deleteByYearMonthWeekSeq(Integer year, Integer month, Integer weekSeq);

    int delete(QueryWrapper<CigaretteDistributionPredictionPO> queryWrapper);
}

