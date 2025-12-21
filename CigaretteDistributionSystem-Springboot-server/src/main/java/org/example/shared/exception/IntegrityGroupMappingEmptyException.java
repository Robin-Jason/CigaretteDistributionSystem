package org.example.shared.exception;

/**
 * 诚信互助小组映射表为空异常
 * <p>
 * 当 integrity_group_code_mapping 表为空或查询失败时抛出此异常。
 * 该异常表示系统无法获取诚信互助小组的理论区域集合，无法进行区域客户数统计。
 * </p>
 *
 * @author Robin
 * @version 1.0
 * @since 2025-12-15
 */
public class IntegrityGroupMappingEmptyException extends RuntimeException {

    public IntegrityGroupMappingEmptyException(String message) {
        super(message);
    }

    public IntegrityGroupMappingEmptyException(String message, Throwable cause) {
        super(message, cause);
    }
}

