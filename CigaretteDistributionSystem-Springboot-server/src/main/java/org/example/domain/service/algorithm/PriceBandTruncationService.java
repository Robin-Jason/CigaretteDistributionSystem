package org.example.domain.service.algorithm;

import org.example.domain.model.valueobject.GradeRange;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 价位段截断与微调算法接口。
 * <p>
 * 用于处理"按价位段自选投放"场景下，同一价位段内多支卷烟的分配结果截断与误差微调。
 * 核心逻辑：
 * <ol>
 *   <li>从 minIndex→maxIndex 扫描，找到首个"该列至少有两支卷烟分配量 > 0"的档位</li>
 *   <li>将低于该档位的所有列清零（截断）</li>
 *   <li>对每支卷烟独立执行误差微调</li>
 *   <li>检测全0分配异常</li>
 * </ol>
 * </p>
 *
 * @author Robin
 * @since 2025-12-22
 */
public interface PriceBandTruncationService {

    /**
     * 对需要截断的价位段执行截断与微调操作。
     * <p>
     * 算法流程：
     * <ol>
     *   <li>保存截断前的分配结果（用于异常时恢复）</li>
     *   <li>从 minIndex→maxIndex 扫描，找到首个"该列至少有两支卷烟分配量 > 0"的档位作为截断点</li>
     *   <li>将低于截断点的所有档位列清零</li>
     *   <li>对每支卷烟独立执行误差微调，尽量逼近目标投放量</li>
     *   <li>检测全0分配异常，如发现则恢复截断前的分配结果并抛出异常</li>
     * </ol>
     * </p>
     *
     * @param bandsNeedingTruncation 需要截断的价位段分组，key 为价位段编号，value 为该价位段内的卷烟列表。
     *                               每个卷烟 Map 需包含 "GRADES"（BigDecimal[30] 分配方案）和 "ADV"（目标投放量）字段
     * @param cityCustomerRow        全市客户数数组（30个档位），索引0对应D30，索引29对应D1
     * @param gradeRange             档位范围，指定分配的有效档位区间
     * @param year                   年份，用于异常信息中标识批次
     * @param month                  月份，用于异常信息中标识批次
     * @param weekSeq                周序号，用于异常信息中标识批次
     * @throws IllegalStateException 如果某支卷烟经过截断和微调后所有档位分配量均为0
     *
     * @example
     * <pre>{@code
     * // 准备价位段分组数据
     * Map<Integer, List<Map<String, Object>>> bands = new TreeMap<>();
     * List<Map<String, Object>> band100 = new ArrayList<>();
     * 
     * Map<String, Object> cig1 = new HashMap<>();
     * cig1.put("CIG_CODE", "31020004");
     * cig1.put("CIG_NAME", "中华(软)");
     * cig1.put("ADV", new BigDecimal("3000"));
     * cig1.put("GRADES", new BigDecimal[30]); // 已由 SingleLevel 初分配填充
     * band100.add(cig1);
     * 
     * bands.put(100, band100);
     * 
     * // 全市客户数数组
     * BigDecimal[] cityCustomerRow = new BigDecimal[30];
     * // ... 填充客户数
     * 
     * // 执行截断与微调
     * service.truncateAndAdjust(bands, cityCustomerRow, GradeRange.full(), 2025, 9, 4);
     * }</pre>
     */
    void truncateAndAdjust(Map<Integer, List<Map<String, Object>>> bandsNeedingTruncation,
                           BigDecimal[] cityCustomerRow,
                           GradeRange gradeRange,
                           Integer year, Integer month, Integer weekSeq);

    /**
     * 对需要截断的价位段执行截断与微调操作（支持两周一访上浮）。
     * <p>
     * 与普通版本的区别：支持传入两份客户数数组（原始 + 上浮），
     * 微调时根据每支卷烟的备注（BZ字段）判断使用哪份客户数。
     * </p>
     *
     * @param bandsNeedingTruncation 需要截断的价位段分组
     * @param baseCustomerRow        原始客户数数组
     * @param boostedCustomerRow     上浮后客户数数组（可能为 null，如果本次无需上浮）
     * @param gradeRange             档位范围，指定分配的有效档位区间
     * @param year                   年份
     * @param month                  月份
     * @param weekSeq                周序号
     * @throws IllegalStateException 如果某支卷烟经过截断和微调后所有档位分配量均为0
     */
    void truncateAndAdjust(Map<Integer, List<Map<String, Object>>> bandsNeedingTruncation,
                           BigDecimal[] baseCustomerRow,
                           BigDecimal[] boostedCustomerRow,
                           GradeRange gradeRange,
                           Integer year, Integer month, Integer weekSeq);
}
