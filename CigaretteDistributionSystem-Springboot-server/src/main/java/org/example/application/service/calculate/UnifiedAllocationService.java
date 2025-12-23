package org.example.application.service.calculate;

import org.example.application.dto.allocation.GenerateDistributionPlanRequestDto;
import org.example.application.dto.allocation.GenerateDistributionPlanResponseDto;

/**
 * 统一分配服务接口。
 * <p>
 * 协调标准分配（按档位投放、按档位扩展投放）和价位段分配（按价位段自选投放）两种流程。
 * </p>
 *
 * @author Robin
 * @since 2025-12-22
 */
public interface UnifiedAllocationService {

    /**
     * 一键生成分配方案（统一入口）。
     * <p>
     * 内部依次调用：
     * <ul>
     *   <li>StandardAllocationService - 处理按档位投放、按档位扩展投放</li>
     *   <li>PriceBandAllocationService - 处理按价位段自选投放</li>
     * </ul>
     * </p>
     *
     * @param request 生成分配方案请求 DTO
     * @return 生成结果响应 DTO
     */
    GenerateDistributionPlanResponseDto generateDistributionPlan(GenerateDistributionPlanRequestDto request);
}
