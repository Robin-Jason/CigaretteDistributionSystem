package org.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 诚信互助小组编码映射表实体。
 */
@Data
@TableName("integrity_group_code_mapping")
public class IntegrityGroupMapping {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String groupName;

    private String groupCode;

    private BigDecimal customerCount;

    private Integer sortOrder;
}

