package org.example.infrastructure.repository.impl;

import lombok.RequiredArgsConstructor;
import org.example.domain.repository.IntegrityGroupMappingRepository;
import org.example.infrastructure.persistence.po.IntegrityGroupMappingPO;
import org.example.infrastructure.persistence.mapper.IntegrityGroupMappingMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * {@link IntegrityGroupMappingRepository} 的 MyBatis-Plus 实现。
 * <p>
 * 适配 {@code IntegrityGroupMappingMapper} 提供数据访问。
 * </p>
 *
 * @author Robin
 * @version 1.0
 * @since 2025-12-12
 */
@Repository
@RequiredArgsConstructor
public class IntegrityGroupMappingRepositoryImpl implements IntegrityGroupMappingRepository {

    private final IntegrityGroupMappingMapper integrityGroupMappingMapper;

    /**
     * 确保表存在（不存在则创建）
     */
    @Override
    public void createTableIfNotExists() {
        integrityGroupMappingMapper.createTableIfNotExists();
    }

    /**
     * 清空表数据
     */
    @Override
    public void truncateTable() {
        integrityGroupMappingMapper.truncateTable();
    }

    /**
     * 批量插入
     *
     * @param list 映射列表
     * @return 影响行数
     */
    @Override
    public int batchInsert(List<IntegrityGroupMappingPO> list) {
        return integrityGroupMappingMapper.batchInsert(list);
    }

    /**
     * 按排序查询全部
     *
     * @return 映射列表
     */
    @Override
    public List<IntegrityGroupMappingPO> selectAllOrderBySort() {
        return integrityGroupMappingMapper.selectAllOrderBySort();
    }

    /**
     * 根据 GROUP_NAME 查询 GROUP_CODE
     *
     * @param groupName 小组名称
     * @return 小组编码，如果未找到则返回 null
     */
    @Override
    public String selectGroupCodeByGroupName(String groupName) {
        return integrityGroupMappingMapper.selectGroupCodeByGroupName(groupName);
    }
}

