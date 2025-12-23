package org.example.application.service.region.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.application.dto.allocation.GetAvailableRegionsRequestDto;
import org.example.application.dto.allocation.GetAvailableRegionsResponseDto;
import org.example.application.service.region.GetAvailableRegionsService;
import org.example.domain.model.valueobject.DeliveryExtensionType;
import org.example.domain.repository.RegionCustomerStatisticsRepository;
import org.example.shared.dto.RegionCustomerRecord;
import org.example.shared.util.CombinationStrategyAnalyzer;
import org.example.shared.util.RegionRecordBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 获取可用投放区域列表服务实现类
 * 
 * @author System
 * @version 1.0
 * @since 2025-12-23
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GetAvailableRegionsServiceImpl implements GetAvailableRegionsService {

    private final RegionCustomerStatisticsRepository regionCustomerStatisticsRepository;
    private final RegionRecordBuilder regionRecordBuilder;

    /**
     * 获取可用投放区域列表
     * 
     * 流程：
     * 1. 使用 RegionRecordBuilder 构建该投放组合的所有理论区域数据
     * 2. 查询 region_customer_statistics 表，找出缺失的区域
     * 3. 将缺失的区域数据追加写入 region_customer_statistics 表
     * 4. 返回完整的区域列表和构建信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public GetAvailableRegionsResponseDto getAvailableRegions(GetAvailableRegionsRequestDto request) {
        log.info("开始获取可用投放区域列表: {}-{}-{}, 投放类型={}, 扩展类型={}", 
                request.getYear(), request.getMonth(), request.getWeekSeq(),
                request.getDeliveryMethod(), request.getDeliveryEtype());

        GetAvailableRegionsResponseDto response = new GetAvailableRegionsResponseDto();
        response.setSuccess(true);

        try {
            // 1. 使用 RegionRecordBuilder 构建该投放组合的所有理论区域数据
            // 传入空标签列表，因为我们只需要基础区域数据（不考虑标签过滤）
            List<RegionCustomerRecord> theoreticalRecords = regionRecordBuilder.buildRecordsForCombination(
                    request.getDeliveryMethod(), 
                    request.getDeliveryEtype(), 
                    Collections.emptyList(), 
                    request.getYear(), 
                    request.getMonth(), 
                    request.getWeekSeq());
            
            if (theoreticalRecords.isEmpty()) {
                response.setSuccess(false);
                response.setMessage("无法解析投放类型和扩展类型对应的区域列表");
                response.setAvailableRegions(Collections.emptyList());
                return response;
            }

            // 提取所有理论区域名称
            List<String> theoreticalRegions = theoreticalRecords.stream()
                    .map(RegionCustomerRecord::getRegion)
                    .collect(Collectors.toList());

            log.info("解析到 {} 个理论区域", theoreticalRegions.size());

            // 2. 查询 region_customer_statistics 表，获取已存在的区域
            List<Map<String, Object>> existingStats = regionCustomerStatisticsRepository.findAll(
                    request.getYear(), request.getMonth(), request.getWeekSeq());
            
            Set<String> existingRegions = existingStats.stream()
                    .map(stat -> (String) stat.get("REGION"))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            log.info("region_customer_statistics 表中已存在 {} 个区域", existingRegions.size());

            // 3. 找出缺失的区域
            List<RegionCustomerRecord> missingRecords = theoreticalRecords.stream()
                    .filter(record -> !existingRegions.contains(record.getRegion()))
                    .collect(Collectors.toList());

            if (missingRecords.isEmpty()) {
                log.info("所有理论区域都已存在，无需追加构建");
                response.setHasBuiltNewData(false);
                response.setBuiltRegions(Collections.emptyList());
                response.setAvailableRegions(theoreticalRegions);
                response.setMessage("所有区域数据已存在");
                return response;
            }

            List<String> missingRegionNames = missingRecords.stream()
                    .map(RegionCustomerRecord::getRegion)
                    .collect(Collectors.toList());

            log.info("发现 {} 个缺失区域: {}", missingRegionNames.size(), missingRegionNames);

            // 4. 批量插入缺失的区域数据到 region_customer_statistics 表
            int insertedCount = regionCustomerStatisticsRepository.batchUpsert(
                    request.getYear(), request.getMonth(), request.getWeekSeq(), missingRecords);
            
            log.info("追加构建完成，插入 {} 条新记录", insertedCount);
            
            response.setHasBuiltNewData(true);
            response.setBuiltRegions(missingRegionNames);
            response.setAvailableRegions(theoreticalRegions);
            response.setMessage(String.format("成功追加构建 %d 个区域的客户数据", insertedCount));

        } catch (Exception e) {
            log.error("获取可用投放区域列表失败", e);
            response.setSuccess(false);
            response.setMessage("获取可用投放区域列表失败: " + e.getMessage());
            response.setAvailableRegions(Collections.emptyList());
        }

        return response;
    }
}
