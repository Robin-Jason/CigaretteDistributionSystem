package org.example.application.facade;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 分配策略执行命令（业务语义入参）。
 * <p>
 * 调用方只需要关心卷烟、投放组合、批次等业务字段，
 * 由 {@link DefaultDistributionStrategyManager} 负责转换为底层的 {@code StrategyExecutionRequest}。
 * </p>
 */
@Data
@Builder
public class DistributionStrategyCommand {

    /** 投放方式 */
    private String deliveryMethod;

    /** 扩展投放方式 */
    private String deliveryEtype;

    /** 标签（可为空） */
    private String tag;

    /** 投放区域（可为空，部分组合不使用） */
    private String deliveryArea;

    /** 预投放量（ADV） */
    private BigDecimal targetAmount;

    /** 年份 */
    private Integer year;

    /** 月份 */
    private Integer month;

    /** 周序号 */
    private Integer weekSeq;

    /** 备注（如双周上浮标记） */
    private String remark;

    /**
     * 附加信息（如市场类型比例、标签过滤配置等），
     * 由调用方按约定的 key 填充。
     */
    private Map<String, Object> extraInfo;

    /** 最高档位（可选，缺省为 D30） */
    private String maxGrade;

    /** 最低档位（可选，缺省为 D1） */
    private String minGrade;
}


