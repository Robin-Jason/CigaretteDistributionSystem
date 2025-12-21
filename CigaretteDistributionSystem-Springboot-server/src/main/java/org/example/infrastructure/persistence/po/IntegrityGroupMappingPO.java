package org.example.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 诚信互助小组编码映射持久化对象（PO）
 * <p>
 * 对应表：integrity_group_code_mapping
 * </p>
 *
 * @author Robin
 * @version 1.0
 * @since 2025-12-14
 */
@Data
@TableName("integrity_group_code_mapping")
public class IntegrityGroupMappingPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String groupName;

    private String groupCode;

    private BigDecimal customerCount;

    private Integer sortOrder;

    @TableField("updated_at")
    private java.time.LocalDateTime updatedAt;
}

