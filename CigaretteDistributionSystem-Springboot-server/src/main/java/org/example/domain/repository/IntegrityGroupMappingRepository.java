package org.example.domain.repository;

import org.example.infrastructure.persistence.po.IntegrityGroupMappingPO;

import java.util.List;

/**
 * 诚信互助小组编码映射仓储接口
 * <p>
 * 定义对 {@code integrity_group_code_mapping} 数据的抽象访问方式。
 * </p>
 *
 * @author Robin
 * @version 1.0
 * @since 2025-12-12
 */
public interface IntegrityGroupMappingRepository {

    /**
     * 确保表存在（不存在则创建）
     */
    void createTableIfNotExists();

    /**
     * 清空表数据
     */
    void truncateTable();

    /**
     * 批量插入
     *
     * @param list 映射列表
     * @return 影响行数
     */
    int batchInsert(List<IntegrityGroupMappingPO> list);

    /**
     * 按排序查询全部
     *
     * @return 映射列表
     */
    List<IntegrityGroupMappingPO> selectAllOrderBySort();

    /**
     * 根据 GROUP_NAME 查询 GROUP_CODE
     *
     * @param groupName 小组名称
     * @return 小组编码，如果未找到则返回 null
     */
    String selectGroupCodeByGroupName(String groupName);
}

