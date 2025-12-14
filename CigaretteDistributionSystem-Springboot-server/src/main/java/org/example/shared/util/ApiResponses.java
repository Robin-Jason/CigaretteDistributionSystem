package org.example.shared.util;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

/**
 * 简单的响应构建工具，避免控制层重复拼装 Map。
 */
public final class ApiResponses {

    private ApiResponses() {
    }

    public static ResponseEntity<Map<String, Object>> badRequest(String message, String errorCode) {
        return ResponseEntity.badRequest().body(errorBody(message, errorCode));
    }

    public static ResponseEntity<Map<String, Object>> internalError(String message, String errorCode) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorBody(message, errorCode));
    }

    private static Map<String, Object> errorBody(String message, String errorCode) {
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("message", message);
        if (errorCode != null && !errorCode.isEmpty()) {
            body.put("error", errorCode);
        }
        return body;
    }
}

