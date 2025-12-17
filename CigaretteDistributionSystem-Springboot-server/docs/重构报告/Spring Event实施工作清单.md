# Spring Event 实施工作清单

## 📋 概述

本文档提供了在卷烟分配系统中引入 Spring Event 的详细实施计划，用于解耦业务流程，提升系统的可扩展性和可维护性。

**预计工作量**: 3-5 个工作日  
**优先级**: ⭐⭐⭐⭐ (高)  
**技术栈**: Spring Boot 2.7.18 + Spring Event (内置，无需额外依赖)

---

## 🎯 实施目标

1. **解耦业务流程**: 将"一键生成分配方案"和"Excel导入"等复杂流程解耦
2. **支持异步处理**: 提升性能，支持并行处理多个卷烟分配
3. **易于扩展**: 新增功能（通知、审计、统计）无需修改主流程
4. **提升可观测性**: 统一的事件监控和日志记录

---

## 📦 阶段一：基础准备（0.5天）

### ✅ 任务 1.1: 启用异步支持

**文件**: `src/main/java/org/example/CigaretteDistributionApplication.java`

**操作**:
```java
@SpringBootApplication
@EnableCaching
@EnableScheduling
@EnableAsync  // ← 添加此注解
@org.mybatis.spring.annotation.MapperScan({ "org.example.infrastructure.persistence.mapper"})
public class CigaretteDistributionApplication {
    // ...
}
```

**验证**: 
- [ ] 编译通过
- [ ] 应用启动无错误

---

### ✅ 任务 1.2: 配置异步线程池（可选，推荐）

**文件**: `src/main/java/org/example/infrastructure/config/AsyncConfig.java` (新建)

**操作**: 创建异步配置类
```java
package org.example.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "eventTaskExecutor")
    public Executor eventTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("event-async-");
        executor.initialize();
        return executor;
    }
}
```

**验证**:
- [ ] 配置类创建成功
- [ ] 应用启动无错误

---

## 📦 阶段二：定义领域事件（0.5天）

### ✅ 任务 2.1: 创建事件包结构

**目录**: `src/main/java/org/example/domain/event/`

**操作**: 创建以下目录结构
```
domain/
└── event/
    ├── DistributionPlanGenerationStartedEvent.java
    ├── DistributionPlanGenerationCompletedEvent.java
    ├── ExistingDataDeletedEvent.java
    ├── CigaretteAllocationRequestedEvent.java
    ├── CigaretteAllocationCompletedEvent.java
    ├── CigaretteAllocationFailedEvent.java
    ├── DataImportStartedEvent.java
    ├── DataImportCompletedEvent.java
    └── DataImportFailedEvent.java
```

**验证**:
- [ ] 目录结构创建完成

---

### ✅ 任务 2.2: 定义分配方案生成相关事件

**文件**: `src/main/java/org/example/domain/event/DistributionPlanGenerationStartedEvent.java`

**操作**: 创建事件类
```java
package org.example.domain.event;

import lombok.Data;
import java.io.Serializable;

/**
 * 分配方案生成开始事件
 */
@Data
public class DistributionPlanGenerationStartedEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Integer year;
    private Integer month;
    private Integer weekSeq;
    private Long startTime;
    private String requestId; // 可选：用于追踪
    
    public DistributionPlanGenerationStartedEvent(Integer year, Integer month, Integer weekSeq) {
        this.year = year;
        this.month = month;
        this.weekSeq = weekSeq;
        this.startTime = System.currentTimeMillis();
    }
}
```

**文件**: `src/main/java/org/example/domain/event/DistributionPlanGenerationCompletedEvent.java`

**操作**: 创建完成事件
```java
package org.example.domain.event;

import lombok.Data;
import java.io.Serializable;

/**
 * 分配方案生成完成事件
 */
@Data
public class DistributionPlanGenerationCompletedEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Integer year;
    private Integer month;
    private Integer weekSeq;
    private Long startTime;
    private Long endTime;
    private Integer totalCount;
    private Integer successCount;
    private Integer failedCount;
    private Boolean success;
    private String message;
    
    public DistributionPlanGenerationCompletedEvent(Integer year, Integer month, Integer weekSeq) {
        this.year = year;
        this.month = month;
        this.weekSeq = weekSeq;
    }
}
```

**文件**: `src/main/java/org/example/domain/event/ExistingDataDeletedEvent.java`

**操作**: 创建删除事件
```java
package org.example.domain.event;

import lombok.Data;
import java.io.Serializable;

/**
 * 现有数据删除事件
 */
@Data
public class ExistingDataDeletedEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Integer year;
    private Integer month;
    private Integer weekSeq;
    private Integer deletedCount;
    
    public ExistingDataDeletedEvent(Integer year, Integer month, Integer weekSeq, Integer deletedCount) {
        this.year = year;
        this.month = month;
        this.weekSeq = weekSeq;
        this.deletedCount = deletedCount;
    }
}
```

**验证**:
- [ ] 所有事件类创建完成
- [ ] 编译通过

---

### ✅ 任务 2.3: 定义卷烟分配相关事件

**文件**: `src/main/java/org/example/domain/event/CigaretteAllocationRequestedEvent.java`

**操作**: 创建请求事件
```java
package org.example.domain.event;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Map;

/**
 * 卷烟分配请求事件
 */
@Data
public class CigaretteAllocationRequestedEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String cigCode;
    private String cigName;
    private Integer year;
    private Integer month;
    private Integer weekSeq;
    private String deliveryMethod;
    private String deliveryEtype;
    private String tag;
    private String deliveryArea;
    private BigDecimal adv;
    private Map<String, Object> advData;
    private Map<String, BigDecimal> marketRatios;
    private String remark;
    
    public CigaretteAllocationRequestedEvent(String cigCode, String cigName, 
                                            Integer year, Integer month, Integer weekSeq,
                                            String deliveryMethod, String deliveryEtype,
                                            String tag, String deliveryArea, BigDecimal adv,
                                            Map<String, Object> advData, 
                                            Map<String, BigDecimal> marketRatios,
                                            String remark) {
        this.cigCode = cigCode;
        this.cigName = cigName;
        this.year = year;
        this.month = month;
        this.weekSeq = weekSeq;
        this.deliveryMethod = deliveryMethod;
        this.deliveryEtype = deliveryEtype;
        this.tag = tag;
        this.deliveryArea = deliveryArea;
        this.adv = adv;
        this.advData = advData;
        this.marketRatios = marketRatios;
        this.remark = remark;
    }
}
```

**文件**: `src/main/java/org/example/domain/event/CigaretteAllocationCompletedEvent.java`

**操作**: 创建完成事件
```java
package org.example.domain.event;

import lombok.Data;
import java.io.Serializable;

/**
 * 卷烟分配完成事件
 */
@Data
public class CigaretteAllocationCompletedEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String cigCode;
    private String cigName;
    private Integer year;
    private Integer month;
    private Integer weekSeq;
    private Boolean success;
    private Long calcTimeMs;
    private Long writeBackTimeMs;
    private String message;
    
    public CigaretteAllocationCompletedEvent(String cigCode, String cigName,
                                            Integer year, Integer month, Integer weekSeq,
                                            Boolean success, Long calcTimeMs, Long writeBackTimeMs) {
        this.cigCode = cigCode;
        this.cigName = cigName;
        this.year = year;
        this.month = month;
        this.weekSeq = weekSeq;
        this.success = success;
        this.calcTimeMs = calcTimeMs;
        this.writeBackTimeMs = writeBackTimeMs;
    }
}
```

**文件**: `src/main/java/org/example/domain/event/CigaretteAllocationFailedEvent.java`

**操作**: 创建失败事件
```java
package org.example.domain.event;

import lombok.Data;
import java.io.Serializable;

/**
 * 卷烟分配失败事件
 */
@Data
public class CigaretteAllocationFailedEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String cigCode;
    private String cigName;
    private Integer year;
    private Integer month;
    private Integer weekSeq;
    private String errorMessage;
    private Exception exception;
    
    public CigaretteAllocationFailedEvent(String cigCode, String cigName,
                                         Integer year, Integer month, Integer weekSeq,
                                         String errorMessage, Exception exception) {
        this.cigCode = cigCode;
        this.cigName = cigName;
        this.year = year;
        this.month = month;
        this.weekSeq = weekSeq;
        this.errorMessage = errorMessage;
        this.exception = exception;
    }
}
```

**验证**:
- [ ] 所有事件类创建完成
- [ ] 编译通过

---

### ✅ 任务 2.4: 定义数据导入相关事件

**文件**: `src/main/java/org/example/domain/event/DataImportStartedEvent.java`

**操作**: 创建导入开始事件
```java
package org.example.domain.event;

import lombok.Data;
import java.io.Serializable;

/**
 * 数据导入开始事件
 */
@Data
public class DataImportStartedEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Integer year;
    private Integer month;
    private Integer weekSeq;
    private Boolean hasBaseCustomerFile;
    private Boolean hasCigaretteFile;
    private Long startTime;
    
    public DataImportStartedEvent(Integer year, Integer month, Integer weekSeq,
                                 Boolean hasBaseCustomerFile, Boolean hasCigaretteFile) {
        this.year = year;
        this.month = month;
        this.weekSeq = weekSeq;
        this.hasBaseCustomerFile = hasBaseCustomerFile;
        this.hasCigaretteFile = hasCigaretteFile;
        this.startTime = System.currentTimeMillis();
    }
}
```

**文件**: `src/main/java/org/example/domain/event/DataImportCompletedEvent.java`

**操作**: 创建导入完成事件
```java
package org.example.domain.event;

import lombok.Data;
import java.io.Serializable;
import java.util.Map;

/**
 * 数据导入完成事件
 */
@Data
public class DataImportCompletedEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Integer year;
    private Integer month;
    private Integer weekSeq;
    private Boolean success;
    private String message;
    private Map<String, Object> baseCustomerInfoResult;
    private Map<String, Object> cigaretteDistributionInfoResult;
    private Long startTime;
    private Long endTime;
    
    public DataImportCompletedEvent(Integer year, Integer month, Integer weekSeq,
                                   Boolean success, String message) {
        this.year = year;
        this.month = month;
        this.weekSeq = weekSeq;
        this.success = success;
        this.message = message;
    }
}
```

**文件**: `src/main/java/org/example/domain/event/DataImportFailedEvent.java`

**操作**: 创建导入失败事件
```java
package org.example.domain.event;

import lombok.Data;
import java.io.Serializable;

/**
 * 数据导入失败事件
 */
@Data
public class DataImportFailedEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Integer year;
    private Integer month;
    private Integer weekSeq;
    private String errorMessage;
    private Exception exception;
    
    public DataImportFailedEvent(Integer year, Integer month, Integer weekSeq,
                                String errorMessage, Exception exception) {
        this.year = year;
        this.month = month;
        this.weekSeq = weekSeq;
        this.errorMessage = errorMessage;
        this.exception = exception;
    }
}
```

**验证**:
- [ ] 所有事件类创建完成
- [ ] 编译通过

---

## 📦 阶段三：改造主流程 - 分配方案生成（1天）

### ✅ 任务 3.1: 修改 DistributionCalculateServiceImpl - 发布开始事件

**文件**: `src/main/java/org/example/application/service/impl/DistributionCalculateServiceImpl.java`

**操作**:
1. 添加 `ApplicationEventPublisher` 依赖注入
2. 在 `generateDistributionPlan` 方法开始处发布开始事件

**代码修改**:
```java
import org.springframework.context.ApplicationEventPublisher;
import org.example.domain.event.DistributionPlanGenerationStartedEvent;
import org.example.domain.event.ExistingDataDeletedEvent;
import org.example.domain.event.DistributionPlanGenerationCompletedEvent;

@Service
@RequiredArgsConstructor
public class DistributionCalculateServiceImpl implements DistributionCalculateService {
    
    // 添加事件发布器
    private final ApplicationEventPublisher eventPublisher;
    
    // ... 其他依赖 ...
    
    @Override
    public GenerateDistributionPlanResponseDto generateDistributionPlan(GenerateDistributionPlanRequestDto request) {
        log.info("开始一键生成分配方案，年份: {}, 月份: {}, 周序号: {}", 
                request.getYear(), request.getMonth(), request.getWeekSeq());
        
        // 发布开始事件
        eventPublisher.publishEvent(new DistributionPlanGenerationStartedEvent(
            request.getYear(), request.getMonth(), request.getWeekSeq()
        ));
        
        GenerateDistributionPlanResponseDto response = new GenerateDistributionPlanResponseDto();
        // ... 原有代码 ...
    }
}
```

**验证**:
- [ ] 编译通过
- [ ] 事件发布成功（通过日志验证）

---

### ✅ 任务 3.2: 发布删除完成事件

**文件**: `src/main/java/org/example/application/service/impl/DistributionCalculateServiceImpl.java`

**操作**: 在删除旧数据成功后发布事件

**代码修改**:
```java
// 在删除成功后
if (!Boolean.TRUE.equals(deleteResult.get("success"))) {
    // ... 错误处理 ...
} else {
    log.info("成功删除{}年{}月第{}周的{}条现有分配数据", ...);
    
    // 发布删除完成事件
    Object deletedCountObj = deleteResult.get("deletedCount");
    Integer deletedCount = // ... 转换逻辑 ...
    eventPublisher.publishEvent(new ExistingDataDeletedEvent(
        request.getYear(), request.getMonth(), request.getWeekSeq(), deletedCount
    ));
    
    // ... 原有代码 ...
}
```

**验证**:
- [ ] 编译通过
- [ ] 删除事件发布成功

---

### ✅ 任务 3.3: 发布卷烟分配请求事件（关键改造）

**文件**: `src/main/java/org/example/application/service/impl/DistributionCalculateServiceImpl.java`

**操作**: 将循环中的直接调用改为发布事件

**代码修改**:
```java
import org.example.domain.event.CigaretteAllocationRequestedEvent;

// 在循环中，替换直接调用
for (Map<String, Object> advData : advDataList) {
    // ... 数据准备 ...
    
    // 原代码（删除）:
    // AllocationCalculationResult result = distributionAllocationOrchestrator.calculateAllocationMatrix(...);
    // distributionWriteBackService.writeBackSingleCigarette(...);
    
    // 新代码（发布事件）:
    eventPublisher.publishEvent(new CigaretteAllocationRequestedEvent(
        cigCode, cigName, cigYear, cigMonth, cigWeekSeq,
        deliveryMethod, deliveryEtype, tag, deliveryArea, adv,
        advData, marketRatios, remark
    ));
}
```

**注意**: 这一步需要配合任务 4.1 一起完成，否则分配逻辑会丢失。

**验证**:
- [ ] 编译通过
- [ ] 事件发布成功

---

### ✅ 任务 3.4: 发布完成事件

**文件**: `src/main/java/org/example/application/service/impl/DistributionCalculateServiceImpl.java`

**操作**: 在方法结束前发布完成事件

**代码修改**:
```java
// 在方法返回前
response.setEndTime(System.currentTimeMillis());
response.setProcessingTime(/* 计算处理时间 */);

// 发布完成事件
eventPublisher.publishEvent(new DistributionPlanGenerationCompletedEvent(
    request.getYear(), request.getMonth(), request.getWeekSeq()
) {{
    setStartTime(response.getStartTime());
    setEndTime(response.getEndTime());
    setTotalCount(response.getTotalCigarettes());
    setSuccessCount(response.getSuccessfulAllocations());
    setFailedCount(response.getTotalCigarettes() - response.getSuccessfulAllocations());
    setSuccess(response.isSuccess());
    setMessage(response.getMessage());
}});

return response;
```

**验证**:
- [ ] 编译通过
- [ ] 完成事件发布成功

---

## 📦 阶段四：创建事件监听器（1.5天）

### ✅ 任务 4.1: 创建卷烟分配事件处理器（核心）

**文件**: `src/main/java/org/example/application/event/handler/CigaretteAllocationEventHandler.java` (新建)

**操作**: 创建事件处理器，处理卷烟分配逻辑

**代码**:
```java
package org.example.application.event.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.application.orchestrator.AllocationCalculationResult;
import org.example.application.orchestrator.DistributionAllocationOrchestrator;
import org.example.application.service.impl.DistributionWriteBackService;
import org.example.domain.event.CigaretteAllocationRequestedEvent;
import org.example.domain.event.CigaretteAllocationCompletedEvent;
import org.example.domain.event.CigaretteAllocationFailedEvent;
import org.example.infrastructure.config.TagFilterConfig;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 卷烟分配事件处理器
 * 负责处理单个卷烟的分配计算和写回
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CigaretteAllocationEventHandler {
    
    private final DistributionAllocationOrchestrator distributionAllocationOrchestrator;
    private final DistributionWriteBackService distributionWriteBackService;
    private final ApplicationEventPublisher eventPublisher;
    private final TagFilterConfig tagFilterConfig;
    
    /**
     * 处理卷烟分配请求事件
     * 异步执行，不阻塞主流程
     */
    @EventListener
    @Async("eventTaskExecutor")  // 使用配置的线程池
    public void handleCigaretteAllocation(CigaretteAllocationRequestedEvent event) {
        String cigCode = event.getCigCode();
        String cigName = event.getCigName();
        long calcStart = System.currentTimeMillis();
        
        try {
            log.info("开始处理卷烟分配: {} - {}", cigCode, cigName);
            
            // 步骤1: 执行算法分配计算
            AllocationCalculationResult allocationCalcResult = 
                distributionAllocationOrchestrator.calculateAllocationMatrix(
                    event.getCigCode(), event.getCigName(),
                    event.getDeliveryMethod(), event.getDeliveryEtype(),
                    event.getTag(), event.getDeliveryArea(), event.getAdv(),
                    event.getYear(), event.getMonth(), event.getWeekSeq(),
                    event.getAdvData(), event.getMarketRatios(), event.getRemark()
                );
            
            long calcElapsed = System.currentTimeMillis() - calcStart;
            log.info("【性能】算法耗时: 卷烟 {} - {}, {}ms, success={}",
                    cigCode, cigName, calcElapsed, allocationCalcResult.isSuccess());
            
            // 步骤2: 写回数据库
            if (allocationCalcResult.isSuccess() && 
                allocationCalcResult.getAllocationMatrix() != null) {
                
                long writeStart = System.currentTimeMillis();
                boolean writeBackSuccess = distributionWriteBackService.writeBackSingleCigarette(
                    allocationCalcResult.getAllocationMatrix(),
                    allocationCalcResult.getCustomerMatrix(),
                    allocationCalcResult.getTargetList(),
                    event.getCigCode(), event.getCigName(),
                    event.getYear(), event.getMonth(), event.getWeekSeq(),
                    event.getDeliveryMethod(), event.getDeliveryEtype(),
                    event.getRemark(), event.getTag(), tagFilterConfig
                );
                
                long writeElapsed = System.currentTimeMillis() - writeStart;
                log.info("【性能】写回耗时: 卷烟 {} - {}, {}ms, success={}",
                        cigCode, cigName, writeElapsed, writeBackSuccess);
                
                // 发布完成事件
                eventPublisher.publishEvent(new CigaretteAllocationCompletedEvent(
                    cigCode, cigName, event.getYear(), event.getMonth(), event.getWeekSeq(),
                    writeBackSuccess, calcElapsed, writeElapsed
                ));
            } else {
                // 发布失败事件
                eventPublisher.publishEvent(new CigaretteAllocationFailedEvent(
                    cigCode, cigName, event.getYear(), event.getMonth(), event.getWeekSeq(),
                    "分配计算失败", null
                ));
            }
            
        } catch (Exception e) {
            log.error("处理卷烟分配失败: {} - {}", cigCode, cigName, e);
            // 发布失败事件
            eventPublisher.publishEvent(new CigaretteAllocationFailedEvent(
                cigCode, cigName, event.getYear(), event.getMonth(), event.getWeekSeq(),
                "处理异常: " + e.getMessage(), e
            ));
        }
    }
}
```

**验证**:
- [ ] 编译通过
- [ ] 事件监听器正常工作
- [ ] 分配逻辑正确执行

---

### ✅ 任务 4.2: 创建分配方案监控处理器

**文件**: `src/main/java/org/example/application/event/handler/DistributionPlanMonitorHandler.java` (新建)

**操作**: 创建监控处理器，记录指标和日志

**代码**:
```java
package org.example.application.event.handler;

import lombok.extern.slf4j.Slf4j;
import org.example.domain.event.DistributionPlanGenerationStartedEvent;
import org.example.domain.event.DistributionPlanGenerationCompletedEvent;
import org.example.domain.event.ExistingDataDeletedEvent;
import org.example.domain.event.CigaretteAllocationCompletedEvent;
import org.example.domain.event.CigaretteAllocationFailedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 分配方案监控处理器
 * 负责记录监控指标、性能统计等
 */
@Slf4j
@Component
public class DistributionPlanMonitorHandler {
    
    @EventListener
    public void handlePlanStarted(DistributionPlanGenerationStartedEvent event) {
        log.info("【监控】分配方案生成开始 - {}-{}-{}", 
                event.getYear(), event.getMonth(), event.getWeekSeq());
        // TODO: 可以集成 Micrometer、Prometheus 等监控系统
    }
    
    @EventListener
    public void handleDataDeleted(ExistingDataDeletedEvent event) {
        log.info("【监控】删除现有数据 - {}-{}-{}, 删除数量: {}", 
                event.getYear(), event.getMonth(), event.getWeekSeq(), 
                event.getDeletedCount());
    }
    
    @EventListener
    public void handleCigaretteCompleted(CigaretteAllocationCompletedEvent event) {
        log.debug("【监控】卷烟分配完成 - {} - {}, 计算: {}ms, 写回: {}ms", 
                event.getCigCode(), event.getCigName(),
                event.getCalcTimeMs(), event.getWriteBackTimeMs());
    }
    
    @EventListener
    public void handleCigaretteFailed(CigaretteAllocationFailedEvent event) {
        log.warn("【监控】卷烟分配失败 - {} - {}, 错误: {}", 
                event.getCigCode(), event.getCigName(), event.getErrorMessage());
    }
    
    @EventListener
    public void handlePlanCompleted(DistributionPlanGenerationCompletedEvent event) {
        long duration = event.getEndTime() - event.getStartTime();
        log.info("【监控】分配方案生成完成 - {}-{}-{}, 总耗时: {}ms, 成功: {}/{}, 失败: {}", 
                event.getYear(), event.getMonth(), event.getWeekSeq(),
                duration, event.getSuccessCount(), event.getTotalCount(), event.getFailedCount());
        
        // TODO: 记录到监控系统
        // metricsService.recordTimer("distribution.plan.generation.duration", duration);
        // metricsService.recordGauge("distribution.plan.success.rate", 
        //     event.getSuccessCount() / (double) event.getTotalCount());
    }
}
```

**验证**:
- [ ] 编译通过
- [ ] 监控日志正常输出

---

### ✅ 任务 4.3: 创建分配方案统计处理器（可选）

**文件**: `src/main/java/org/example/application/event/handler/DistributionPlanStatisticsHandler.java` (新建)

**操作**: 创建统计处理器，更新统计数据

**代码**:
```java
package org.example.application.event.handler;

import lombok.extern.slf4j.Slf4j;
import org.example.domain.event.DistributionPlanGenerationCompletedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 分配方案统计处理器
 * 负责更新统计数据、生成报表等
 */
@Slf4j
@Component
public class DistributionPlanStatisticsHandler {
    
    @EventListener
    public void updateStatistics(DistributionPlanGenerationCompletedEvent event) {
        log.info("【统计】更新分配方案统计数据 - {}-{}-{}", 
                event.getYear(), event.getMonth(), event.getWeekSeq());
        
        // TODO: 更新统计数据
        // statisticsService.updateGenerationStats(
        //     event.getYear(), event.getMonth(), event.getWeekSeq(),
        //     event.getTotalCount(), event.getSuccessCount(), event.getFailedCount()
        // );
    }
}
```

**验证**:
- [ ] 编译通过
- [ ] 统计逻辑正常执行

---

### ✅ 任务 4.4: 创建分配方案通知处理器（可选）

**文件**: `src/main/java/org/example/application/event/handler/DistributionPlanNotificationHandler.java` (新建)

**操作**: 创建通知处理器，发送通知

**代码**:
```java
package org.example.application.event.handler;

import lombok.extern.slf4j.Slf4j;
import org.example.domain.event.DistributionPlanGenerationCompletedEvent;
import org.example.domain.event.DistributionPlanGenerationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 分配方案通知处理器
 * 负责发送邮件、短信等通知
 */
@Slf4j
@Component
public class DistributionPlanNotificationHandler {
    
    @EventListener
    public void notifyPlanStarted(DistributionPlanGenerationStartedEvent event) {
        log.info("【通知】分配方案生成开始通知 - {}-{}-{}", 
                event.getYear(), event.getMonth(), event.getWeekSeq());
        
        // TODO: 发送开始通知
        // notificationService.send("分配方案生成开始", event);
    }
    
    @EventListener
    public void notifyPlanCompleted(DistributionPlanGenerationCompletedEvent event) {
        log.info("【通知】分配方案生成完成通知 - {}-{}-{}", 
                event.getYear(), event.getMonth(), event.getWeekSeq());
        
        // TODO: 发送完成通知
        // if (event.getSuccess()) {
        //     notificationService.send("分配方案生成成功", event);
        // } else {
        //     notificationService.send("分配方案生成失败", event);
        // }
    }
}
```

**验证**:
- [ ] 编译通过
- [ ] 通知逻辑正常执行（如果已实现通知服务）

---

## 📦 阶段五：改造数据导入流程（0.5天）

### ✅ 任务 5.1: 修改 ExcelImportServiceImpl - 发布导入事件

**文件**: `src/main/java/org/example/application/service/impl/ExcelImportServiceImpl.java`

**操作**: 添加事件发布

**代码修改**:
```java
import org.springframework.context.ApplicationEventPublisher;
import org.example.domain.event.DataImportStartedEvent;
import org.example.domain.event.DataImportCompletedEvent;
import org.example.domain.event.DataImportFailedEvent;

@Service
@RequiredArgsConstructor
public class ExcelImportServiceImpl implements ExcelImportService {
    
    private final ApplicationEventPublisher eventPublisher;
    // ... 其他依赖 ...
    
    @Override
    @Transactional(rollbackFor = Exception.class, timeout = 300)
    public Map<String, Object> importData(DataImportRequestDto request) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 发布开始事件
            boolean hasBaseFile = request.getBaseCustomerInfoFile() != null 
                && !request.getBaseCustomerInfoFile().isEmpty();
            boolean hasCigFile = request.getCigaretteDistributionInfoFile() != null 
                && !request.getCigaretteDistributionInfoFile().isEmpty();
            
            eventPublisher.publishEvent(new DataImportStartedEvent(
                request.getYear(), request.getMonth(), request.getWeekSeq(),
                hasBaseFile, hasCigFile
            ));
            
            // ... 原有导入逻辑 ...
            
            // 发布完成事件
            eventPublisher.publishEvent(new DataImportCompletedEvent(
                request.getYear(), request.getMonth(), request.getWeekSeq(),
                Boolean.TRUE.equals(result.get("success")),
                (String) result.get("message")
            ) {{
                setBaseCustomerInfoResult((Map<String, Object>) result.get("baseCustomerInfoResult"));
                setCigaretteDistributionInfoResult((Map<String, Object>) result.get("cigaretteDistributionInfoResult"));
                setStartTime(/* 从开始事件获取 */);
                setEndTime(System.currentTimeMillis());
            }});
            
        } catch (Exception e) {
            log.error("统一数据导入失败", e);
            // 发布失败事件
            eventPublisher.publishEvent(new DataImportFailedEvent(
                request.getYear(), request.getMonth(), request.getWeekSeq(),
                "导入失败: " + e.getMessage(), e
            ));
            throw e;
        }
        
        return result;
    }
}
```

**验证**:
- [ ] 编译通过
- [ ] 导入事件发布成功

---

### ✅ 任务 5.2: 创建数据导入事件处理器（可选）

**文件**: `src/main/java/org/example/application/event/handler/DataImportEventHandler.java` (新建)

**操作**: 创建导入事件处理器

**代码**:
```java
package org.example.application.event.handler;

import lombok.extern.slf4j.Slf4j;
import org.example.domain.event.DataImportStartedEvent;
import org.example.domain.event.DataImportCompletedEvent;
import org.example.domain.event.DataImportFailedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 数据导入事件处理器
 */
@Slf4j
@Component
public class DataImportEventHandler {
    
    @EventListener
    public void handleImportStarted(DataImportStartedEvent event) {
        log.info("【导入】数据导入开始 - {}-{}-{}", 
                event.getYear(), event.getMonth(), event.getWeekSeq());
    }
    
    @EventListener
    public void handleImportCompleted(DataImportCompletedEvent event) {
        log.info("【导入】数据导入完成 - {}-{}-{}, 成功: {}", 
                event.getYear(), event.getMonth(), event.getWeekSeq(), event.getSuccess());
    }
    
    @EventListener
    public void handleImportFailed(DataImportFailedEvent event) {
        log.error("【导入】数据导入失败 - {}-{}-{}, 错误: {}", 
                event.getYear(), event.getMonth(), event.getWeekSeq(), event.getErrorMessage());
    }
}
```

**验证**:
- [ ] 编译通过
- [ ] 导入事件处理正常

---

## 📦 阶段六：测试验证（1天）

### ✅ 任务 6.1: 单元测试 - 事件发布测试

**文件**: `src/test/java/org/example/application/event/DistributionPlanEventTest.java` (新建)

**操作**: 创建事件发布测试

**代码**:
```java
package org.example.application.event;

import org.example.domain.event.DistributionPlanGenerationStartedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
public class DistributionPlanEventTest {
    
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    @Test
    public void testPublishStartEvent() {
        DistributionPlanGenerationStartedEvent event = 
            new DistributionPlanGenerationStartedEvent(2025, 9, 3);
        
        assertNotNull(event);
        eventPublisher.publishEvent(event);
        // 验证事件被发布（可以通过监听器日志验证）
    }
}
```

**验证**:
- [ ] 测试通过
- [ ] 事件正常发布

---

### ✅ 任务 6.2: 集成测试 - 完整流程测试

**文件**: `src/test/java/org/example/api/web/controller/ApiIntegrationTest.java`

**操作**: 更新现有集成测试，验证事件机制

**验证点**:
- [ ] 分配方案生成流程正常
- [ ] 事件正常发布和监听
- [ ] 异步处理正常工作
- [ ] 性能提升（如果启用异步）

---

### ✅ 任务 6.3: 性能测试

**操作**: 对比改造前后的性能

**测试场景**:
1. 同步模式（改造前）: 记录总耗时
2. 异步模式（改造后）: 记录总耗时

**验证**:
- [ ] 异步模式性能提升（预期提升 2-5 倍）
- [ ] 系统稳定性正常

---

## 📦 阶段七：文档和清理（0.5天）

### ✅ 任务 7.1: 更新架构文档

**文件**: `docs/高内聚低耦合优化措施分析.md`

**操作**: 更新事件驱动架构部分，标记为"已实施"

---

### ✅ 任务 7.2: 代码审查

**检查项**:
- [ ] 所有事件类遵循命名规范
- [ ] 所有监听器使用 `@EventListener` 和 `@Async`
- [ ] 异常处理完善
- [ ] 日志记录完整
- [ ] 无编译警告

---

### ✅ 任务 7.3: 清理临时代码

**操作**: 删除调试代码、注释掉的代码等

---

## 📊 实施检查清单

### 基础准备
- [ ] 启用 `@EnableAsync`
- [ ] 配置异步线程池（可选）
- [ ] 验证应用启动正常

### 事件定义
- [ ] 创建事件包结构
- [ ] 定义分配方案生成相关事件（3个）
- [ ] 定义卷烟分配相关事件（3个）
- [ ] 定义数据导入相关事件（3个）
- [ ] 所有事件类编译通过

### 主流程改造
- [ ] `DistributionCalculateServiceImpl` 发布开始事件
- [ ] `DistributionCalculateServiceImpl` 发布删除事件
- [ ] `DistributionCalculateServiceImpl` 发布分配请求事件
- [ ] `DistributionCalculateServiceImpl` 发布完成事件
- [ ] `ExcelImportServiceImpl` 发布导入事件

### 事件监听器
- [ ] 创建 `CigaretteAllocationEventHandler`（核心）
- [ ] 创建 `DistributionPlanMonitorHandler`
- [ ] 创建 `DistributionPlanStatisticsHandler`（可选）
- [ ] 创建 `DistributionPlanNotificationHandler`（可选）
- [ ] 创建 `DataImportEventHandler`（可选）

### 测试验证
- [ ] 单元测试通过
- [ ] 集成测试通过
- [ ] 性能测试完成
- [ ] 功能验证正常

### 文档和清理
- [ ] 更新架构文档
- [ ] 代码审查完成
- [ ] 清理临时代码

---

## 🚨 注意事项

1. **异步处理风险**: 
   - 异步处理可能导致主流程无法立即获取结果
   - 需要调整返回逻辑，可能需要使用 `CompletableFuture` 或轮询机制

2. **事务管理**: 
   - 异步方法中的事务需要单独管理
   - 确保 `@Transactional` 在异步方法中正常工作

3. **错误处理**: 
   - 异步处理中的异常不会传播到主流程
   - 需要完善的异常捕获和事件发布机制

4. **性能监控**: 
   - 监控线程池使用情况
   - 避免线程池队列溢出

5. **向后兼容**: 
   - 确保现有功能不受影响
   - 可以保留同步模式作为备选方案

---

## 📈 预期收益

1. **代码解耦**: 主流程代码减少 30-40%
2. **扩展性**: 新增功能只需添加监听器，无需修改主流程
3. **性能提升**: 异步处理性能提升 2-5 倍
4. **可维护性**: 职责清晰，易于维护和测试

---

## 🔄 后续优化

1. **事件持久化**: 考虑将事件持久化到数据库，支持事件溯源
2. **消息队列升级**: 如需跨应用通信，升级到 RocketMQ
3. **监控集成**: 集成 Micrometer、Prometheus 等监控系统
4. **事件重试机制**: 实现失败事件的重试机制

---

**最后更新**: 2025-01-XX  
**负责人**: [待填写]  
**状态**: 🟡 部分完成（事件基础设施与流程级事件已落地，细粒度异步化与MQ演进待实施）

---

## ✅ 当前已完成的工作（阶段性总结）

1. **事件基础设施落地**
   - 已启用 `@EnableAsync`，并新增自定义线程池配置 `AsyncConfig`
   - 已完成 Spring Event 基础集成，验证事件发布与监听机制可用
2. **分配主流程事件化改造（第一步）**
   - 在 `DistributionCalculateServiceImpl` 中发布流程级事件：开始、删除旧数据、完成
   - 已新增流程监控与统计监听器 `DistributionPlanMonitorHandler`、`DistributionPlanStatisticsHandler`
   - 保留卷烟级核心分配逻辑为同步，避免与异步写入产生死锁
3. **数据导入流程事件化改造（第一步）**
   - 在 `ExcelImportServiceImpl` 中发布导入开始/完成/失败事件
   - 导入过程中的详细日志由 `DataImportEventHandler` 承担，主流程日志已精简
4. **代码精简与警告清理**
   - 删除卷烟级分配事件及对应监听器，避免“双路径”写回导致并发锁冲突
   - 精简主流程中冗余的性能统计、调试日志和未使用字段/变量
   - 清理多处未使用的 import、字段与局部变量，当前编译与测试通过、无新增告警

---

## 📌 后续待进行任务（Roadmap）

### 一、事件驱动改造的深化（在保证稳定的前提下逐步推进）

1. **卷烟级分配异步化的再次尝试（第二版设计）**
   - 重新设计卷烟级事件模型，确保只有“一个路径”负责写回数据库（要么主流程，要么监听器）
   - 通过“分配计算事件 ➜ 写回监听器”模式，逐步迁移同步写回逻辑，并在小范围/灰度环境验证无死锁
   - 设计幂等机制（例如基于业务主键+批次号）避免重复写回
2. **导入与分配的解耦编排**
   - 将“导入完成 ➜ 触发一键分配”作为可选事件链路，而非强耦合调用
   - 引入“任务/批次”概念，对一次导入+分配全过程进行事件追踪
3. **事件模型梳理与统一规范**
   - 统一事件命名、字段规范（如 `correlationId`、`batchId`、`timestamp`、`source` 等）
   - 明确哪些是领域事件、哪些是应用事件，补充到 DDD 文档中

### 二、监控与可观测性增强

1. **事件级监控指标**
   - 为关键事件链路增加埋点（发布次数、失败次数、耗时分布）
   - 和线程池指标一起纳入统一监控（如 Micrometer / Prometheus）
2. **业务侧可观测性**
   - 为“一键生成分配方案”“数据导入”建立统一的监控视图/日志结构
   - 在现有“误差统计报告”的基础上，增加按批次的执行时长与失败率统计

### 三、与现有架构文档的联动更新

1. **更新 DDD 分层文档**
   - 在 `DDD分层完整目录结构.md` 中补充 `event` 相关包、监听器、配置类的分层定位与依赖关系
   - 标注哪些服务已经事件化、哪些仍是同步直连调用（便于后续迭代）
2. **高内聚低耦合措施文档补充**
   - 在 `高内聚低耦合优化措施分析.md` 中增加“事件驱动实践现状”与“下一步落地计划”章节
   - 明确：当前事件仅用于流程级监控与解耦，尚未完全承担业务主流程职责

### 四、事件驱动向 MQ 的平滑演进（中长期）

1. **Spring Event ➜ MQ 抽象层设计**
   - 抽象出统一的“事件发布接口”，屏蔽 Spring Event 与 MQ 具体实现差异
   - 为未来引入 RocketMQ 等中间件预留扩展点，保证当前代码可平滑迁移
2. **跨应用/跨进程场景评估**
   - 梳理需要跨系统扩散的关键业务事件（如区域分配结果、特殊客户标记等）
   - 结合业务优先级，规划首批上 MQ 的事件清单

### 五、测试与回归策略

1. **事件化回归测试用例补充**
   - 为“一键分配”“数据导入”相关的事件发布与监听行为补充集成测试/端到端测试
   - 为未来异步化方案预留测试场景（如线程池满载、监听器异常、部分失败重试等）
2. **性能与并发专项测试**
   - 在接近生产数据规模下，对同步/异步方案做基准对比，记录指标
   - 针对历史出现过的死锁场景，补充专门的并发测试用例
