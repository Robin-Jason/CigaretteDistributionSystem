package org.example.application.service.importing;

import java.util.List;
import java.util.Map;

/**
 * 卷烟投放基础信息导入业务校验器。
 *
 * <p>职责：在写库前对整批 Excel 行数据执行“全市占比 + 货源属性合法性”等业务校验。</p>
 *
 * @author Robin
 * @since 2025-12-18
 */
public interface CigaretteImportValidator {

    /**
     * 对卷烟投放基础信息 Excel 行数据执行业务合法性校验。
     *
     * @param excelData 解析后的所有行数据（每行一个列名->值的 Map）
     *
     * @example excelData.size()=100 -> 执行一次批次级校验，不合法时抛出业务异常
     */
    void validate(List<Map<String, Object>> excelData);
}


