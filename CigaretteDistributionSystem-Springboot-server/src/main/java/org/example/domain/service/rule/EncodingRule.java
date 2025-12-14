package org.example.domain.service.rule;

import java.math.BigDecimal;

/**
 * 编码规则领域服务接口。
 * <p>
 * 定义编码规则的核心业务逻辑，不依赖于Spring框架或持久化层。
 * 纯领域逻辑，可独立测试。
 * </p>
 *
 * @author Robin
 * @version 1.0
 * @since 2025-12-14
 */
public interface EncodingRule {

    /**
     * 编码档位序列。
     * <p>
     * 将30个档位的客户数数组编码为压缩格式，连续相同值的档位用"数量×值"表示，不同值之间用"+"连接。
     * </p>
     *
     * @param grades 30个档位的客户数数组（索引0对应D30，索引29对应D1）
     * @return 编码后的档位序列字符串，格式如："5×10+3×20+2×15"
     */
    String encodeGradeSequences(BigDecimal[] grades);

    /**
     * 将BigDecimal格式化为整数字符串。
     * <p>
     * 去除小数点和尾随零，返回纯数字字符串。如果值为null则返回"0"。
     * </p>
     *
     * @param value BigDecimal值（可为null）
     * @return 格式化后的字符串（如："10"、"0"），如果值为null则返回"0"
     */
    String formatAsInteger(BigDecimal value);
}

