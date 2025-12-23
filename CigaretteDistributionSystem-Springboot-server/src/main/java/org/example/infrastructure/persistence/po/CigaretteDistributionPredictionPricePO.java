package org.example.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * 价位段自选投放预测持久化对象（PO）
 * <p>
 * 对应表：cigarette_distribution_prediction_price（分区表，按 YEAR/MONTH/WEEK_SEQ）
 * 专用于"按价位段自选投放"场景的分配结果存储。
 * </p>
 * <p>
 * 继承自 {@link CigaretteDistributionPredictionPO}，表结构完全一致，仅表名不同。
 * </p>
 *
 * @author Robin
 * @version 1.0
 * @since 2025-12-21
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "cigarette_distribution_prediction_price_dynamic")
@TableName("cigarette_distribution_prediction_price")
public class CigaretteDistributionPredictionPricePO extends CigaretteDistributionPredictionPO {
    // 继承父类所有字段，仅覆盖表名映射
}
