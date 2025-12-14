package org.example.shared.util;

import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 上传文件通用校验工具。
 */
public final class UploadValidators {

    private UploadValidators() {}

    /**
     * 校验必填文件是否存在且未超出大小限制。
     *
     * @return 若校验失败返回 ResponseEntity，否则返回 null 表示通过。
     */
    public static ResponseEntity<Map<String, Object>> validateRequiredFile(MultipartFile file,
                                                                           String emptyMessage,
                                                                           String emptyCode,
                                                                           long maxSizeBytes,
                                                                           String tooLargeMessage,
                                                                           String tooLargeCode) {
        if (file == null || file.isEmpty()) {
            return ApiResponses.badRequest(emptyMessage, emptyCode);
        }
        if (maxSizeBytes > 0 && file.getSize() > maxSizeBytes) {
            return ApiResponses.badRequest(tooLargeMessage, tooLargeCode);
        }
        return null;
    }
}

