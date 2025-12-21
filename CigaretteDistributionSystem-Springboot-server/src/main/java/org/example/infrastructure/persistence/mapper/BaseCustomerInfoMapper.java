package org.example.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.infrastructure.persistence.po.BaseCustomerInfoPO;

import java.util.List;
import java.util.Map;

/**
 * 客户基础信息 Mapper（表：base_customer_info）
 * <p>用途：对客户基础信息表执行查询、建表、插入等操作，SQL 位于 resources/mapper/BaseCustomerInfoMapper.xml。</p>
 * <p>特性：固定表名；支持动态列建表与动态插入；通用 CRUD 复用 MyBatis-Plus BaseMapper。</p>
 *
 * @author Robin
 * @version 1.0
 * @since 2025-12-10
 */
@Mapper
public interface BaseCustomerInfoMapper extends BaseMapper<BaseCustomerInfoPO> {

    /**
     * 查询全表（动态列场景仍由 XML 处理）
     *
     * @return 所有客户数据
     */
    List<Map<String, Object>> selectAll();

    /**
     * 按订单周期筛选，固定排除“不访销”客户
     *
     * @param orderCycles 订单周期列表
     * @param nonVisitValue “不访销”标识值
     * @return 结果集（已剔除“不访销”）
     */
    List<Map<String, Object>> selectByOrderCycle(@Param("orderCycles") List<String> orderCycles,
                                                 @Param("nonVisitValue") String nonVisitValue);

    /**
     * 删除物理表（重建前使用）
     *
     * @return 影响行数
     */
    void dropTable();

    /**
     * 动态建表，列定义与顺序由 Excel 解析后传入
     *
     * @param columnOrder 列顺序
     * @param columnDefinitions 列定义映射
     * @param defaultColumnDefinition 默认列定义
     */
    void createTable(@Param("columnOrder") List<String> columnOrder,
                     @Param("columnDefinitions") Map<String, String> columnDefinitions,
                     @Param("defaultColumnDefinition") String defaultColumnDefinition);

    /**
     * 动态列插入一行数据
     *
     * @param columns 列名顺序
     * @param row 行数据
     * @param values 按列顺序的值列表（用于MyBatis动态绑定）
     * @return 影响行数
     */
    int insertRow(@Param("columns") List<String> columns,
                  @Param("row") Map<String, Object> row,
                  @Param("values") List<Object> values);

    /**
     * 诚信互助小组统计
     *
     * @return 统计结果
     */
    List<Map<String, Object>> selectGroupNameStatistics();

    /**
     * 计数
     *
     * @return 总记录数
     */
    Long countAll();

    /**
     * 列出表结构
     *
     * @return 列定义列表
     */
    List<Map<String, Object>> listColumns();

    /**
     * 判断表是否存在
     *
     * @return 1 表示存在，0 表示不存在
     */
    Integer existsTable();
}


