package org.example.application.service.importing;

import org.example.application.dto.importing.BaseCustomerInfoImportRequestDto;
import org.example.application.dto.importing.CigaretteImportRequestDto;

import java.util.Map;

/**
 * Excel导入服务接口
 *
 * 【核心功能】
 * - 解析、验证并导入 Excel 数据到数据库
 * - 主要目标表：cigarette_distribution_info（分区表）、base_customer_info（非分区表）
 *
 * 【导入约定】
 * - 卷烟基础信息：写入 cigarette_distribution_info 分区表，对应分区键(year, month, weekSeq)
 * - 客户基础信息：写入 base_customer_info（非分区表），全量覆盖
 *
 * 【导入策略】
 * - 卷烟表：导入前按时间分区清理/覆盖目标分区
 * - 客户表：全量覆盖，同时刷新诚信互助小组编码映射表
 * - 事务安全：失败自动回滚
 * - 批量写入提升性能，并返回详细统计
 *
 * @since 2025-10-10
 */
public interface ExcelImportService {

    /**
     * 导入客户基础信息表。
     * <p>
     * 功能：
     * - 全量覆盖 base_customer_info 表
     * - 同步刷新 integrity_group_code_mapping 表（诚信互助小组编码映射）
     * </p>
     * 
     * @param request 导入请求，包含 Excel 文件
     * @return 导入结果，包含 success、message、insertedCount、integrityGroupMapping 等
     */
    Map<String, Object> importBaseCustomerInfo(BaseCustomerInfoImportRequestDto request);

    /**
     * 导入卷烟投放基础信息表。
     * <p>
     * 功能：
     * - 覆盖 cigarette_distribution_info 对应分区数据
     * - 执行业务合法性校验（全市占比、货源属性规则等）
     * </p>
     * 
     * @param request 导入请求，包含年份、月份、周序号及 Excel 文件
     * @return 导入结果，包含 success、message、insertedCount 等
     */
    Map<String, Object> importCigaretteDistributionInfo(CigaretteImportRequestDto request);
}