package org.example.api.web.vo.response;

import lombok.Data;
import java.io.Serializable;

/**
 * 统一API响应格式
 * 
 * @param <T> 响应数据类型
 */
@Data
public class ApiResponseVo<T> implements Serializable {
    private static final long serialVersionUID = 1L;
    
    /**
     * 是否成功
     */
    private Boolean success;
    
    /**
     * 响应消息
     */
    private String message;
    
    /**
     * 错误码
     */
    private String errorCode;
    
    /**
     * 响应数据
     */
    private T data;
    
    /**
     * 时间戳
     */
    private Long timestamp;
    
    /**
     * 成功响应
     */
    public static <T> ApiResponseVo<T> success(T data) {
        ApiResponseVo<T> response = new ApiResponseVo<>();
        response.setSuccess(true);
        response.setData(data);
        response.setTimestamp(System.currentTimeMillis());
        return response;
    }
    
    /**
     * 成功响应（带消息）
     */
    public static <T> ApiResponseVo<T> success(T data, String message) {
        ApiResponseVo<T> response = new ApiResponseVo<>();
        response.setSuccess(true);
        response.setMessage(message);
        response.setData(data);
        response.setTimestamp(System.currentTimeMillis());
        return response;
    }
    
    /**
     * 错误响应
     */
    public static <T> ApiResponseVo<T> error(String message, String errorCode) {
        ApiResponseVo<T> response = new ApiResponseVo<>();
        response.setSuccess(false);
        response.setMessage(message);
        response.setErrorCode(errorCode);
        response.setTimestamp(System.currentTimeMillis());
        return response;
    }
}

