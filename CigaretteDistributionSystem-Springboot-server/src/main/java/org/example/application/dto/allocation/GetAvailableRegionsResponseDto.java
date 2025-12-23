package org.example.application.dto.allocation;

import lombok.Data;

import java.util.List;

/**
 * 获取可用投放区域列表响应 DTO
 *
 * @author Robin
 * @since 2025-12-23
 */
@Data
public class GetAvailableRegionsResponseDto {

    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * 消息
     */
    private String message;

    /**
     * 可用的投放区域列表（笛卡尔积后）
     * <p>
     * 例如：
     * - 单扩展：["丹江", "郧西", "竹山"]
     * - 双扩展：["丹江（城网）", "丹江（农网）", "郧西（城网）", "郧西（农网）"]
     * </p>
     */
    private List<String> availableRegions;

    /**
     * 区域总数
     */
    private Integer totalCount;

    /**
     * 是否追加构建了新的区域客户数据
     */
    private Boolean hasBuiltNewData;

    /**
     * 追加构建的区域列表（如果有）
     */
    private List<String> builtRegions;

    public static GetAvailableRegionsResponseDto success(List<String> regions, boolean hasBuiltNewData, List<String> builtRegions) {
        GetAvailableRegionsResponseDto dto = new GetAvailableRegionsResponseDto();
        dto.setSuccess(true);
        dto.setMessage("获取可用投放区域成功");
        dto.setAvailableRegions(regions);
        dto.setTotalCount(regions != null ? regions.size() : 0);
        dto.setHasBuiltNewData(hasBuiltNewData);
        dto.setBuiltRegions(builtRegions);
        return dto;
    }

    public static GetAvailableRegionsResponseDto failure(String message) {
        GetAvailableRegionsResponseDto dto = new GetAvailableRegionsResponseDto();
        dto.setSuccess(false);
        dto.setMessage(message);
        return dto;
    }
}
