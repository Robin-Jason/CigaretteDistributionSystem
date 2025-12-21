package org.example.infrastructure.persistence.typehandler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * JSON Map类型处理器
 * <p>
 * 用于MyBatis处理Map&lt;String, Object&gt;与MySQL JSON类型之间的转换。
 * 支持将Java Map对象序列化为JSON字符串存储到数据库，以及从数据库JSON字符串反序列化为Map对象。
 * </p>
 *
 * @author Robin
 * @version 1.0
 * @since 2025-12-20
 */
@Slf4j
public class JsonMapTypeHandler extends BaseTypeHandler<Map<String, Object>> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> TYPE_REFERENCE = 
        new TypeReference<Map<String, Object>>() {};

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Map<String, Object> parameter, JdbcType jdbcType) throws SQLException {
        try {
            String json = OBJECT_MAPPER.writeValueAsString(parameter);
            ps.setString(i, json);
        } catch (Exception e) {
            log.error("序列化Map为JSON失败: {}", parameter, e);
            throw new SQLException("序列化Map为JSON失败", e);
        }
    }

    @Override
    public Map<String, Object> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String json = rs.getString(columnName);
        return parseJson(json);
    }

    @Override
    public Map<String, Object> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String json = rs.getString(columnIndex);
        return parseJson(json);
    }

    @Override
    public Map<String, Object> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String json = cs.getString(columnIndex);
        return parseJson(json);
    }

    /**
     * 解析JSON字符串为Map对象
     *
     * @param json JSON字符串
     * @return Map对象，如果json为null或空则返回空的HashMap
     */
    private Map<String, Object> parseJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new HashMap<>();
        }
        try {
            Map<String, Object> result = OBJECT_MAPPER.readValue(json, TYPE_REFERENCE);
            return result != null ? result : new HashMap<>();
        } catch (Exception e) {
            log.warn("反序列化JSON为Map失败: {}, 返回空Map", json, e);
            return new HashMap<>();
        }
    }
}

