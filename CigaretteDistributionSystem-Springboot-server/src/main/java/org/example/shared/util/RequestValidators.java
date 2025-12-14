package org.example.shared.util;

import org.springframework.http.ResponseEntity;

import java.util.Collection;
import java.util.Map;

/**
 * 通用请求参数校验工具。
 */
public final class RequestValidators {

    private RequestValidators() {}

    /**
     * 校验集合非空。
     *
     * @return 校验失败返回 ResponseEntity，成功返回 null。
     */
    public static ResponseEntity<Map<String, Object>> requireNonEmpty(Collection<?> collection,
                                                                     String message,
                                                                     String errorCode) {
        if (collection == null || collection.isEmpty()) {
            return ApiResponses.badRequest(message, errorCode);
        }
        return null;
    }
}

