package org.example.infrastructure.persistence.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.infrastructure.persistence.po.IntegrityGroupMappingPO;

import java.util.List;

/**
 * 诚信互助小组编码映射表 Mapper。
 */
@Mapper
public interface IntegrityGroupMappingMapper {

    /**
     * 确保表存在（不存在则创建）。
     */
    void createTableIfNotExists();

    /**
     * 清空表数据。
     */
    void truncateTable();

    /**
     * 批量插入。
     */
    int batchInsert(@Param("list") List<IntegrityGroupMappingPO> list);

    /**
     * 按排序查询全部。
     */
    List<IntegrityGroupMappingPO> selectAllOrderBySort();
}

