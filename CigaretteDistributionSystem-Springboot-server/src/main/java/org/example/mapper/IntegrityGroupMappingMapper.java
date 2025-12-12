package org.example.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.entity.IntegrityGroupMapping;

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
    int batchInsert(@Param("list") List<IntegrityGroupMapping> list);

    /**
     * 按排序查询全部。
     */
    List<IntegrityGroupMapping> selectAllOrderBySort();
}

