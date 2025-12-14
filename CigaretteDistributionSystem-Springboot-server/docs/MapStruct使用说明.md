# MapStruct 使用说明

## 📖 什么是 MapStruct？

**MapStruct** 是一个高效的 **Java 对象映射框架**，通过注解驱动的方式，在**编译时**自动生成类型安全的映射代码，用于简化对象之间的转换过程。

### 核心特点

| 特点 | 说明 |
|------|------|
| **编译时生成** | 在编译时生成映射代码，不是运行时反射 |
| **类型安全** | 编译时检查类型匹配，避免运行时错误 |
| **高性能** | 生成的代码使用直接方法调用，性能接近手写代码 |
| **零运行时开销** | 不需要额外的运行时库 |
| **简洁易用** | 通过注解配置，自动生成样板代码 |

---

## 🎯 为什么使用 MapStruct？

### 传统方式（手写转换器）

```java
// 手写转换器 - 代码冗长，容易出错
@Component
public class DistributionCalculateConverter {
    
    public GenerateDistributionPlanRequestDto toDto(GenerateDistributionPlanRequestVo vo) {
        GenerateDistributionPlanRequestDto dto = new GenerateDistributionPlanRequestDto();
        dto.setYear(vo.getYear());
        dto.setMonth(vo.getMonth());
        dto.setWeekSeq(vo.getWeekSeq());
        dto.setUrbanRatio(vo.getUrbanRatio());
        dto.setRuralRatio(vo.getRuralRatio());
        return dto;
    }
    
    public GenerateDistributionPlanResponseVo toVo(GenerateDistributionPlanResponseDto dto) {
        GenerateDistributionPlanResponseVo vo = new GenerateDistributionPlanResponseVo();
        vo.setSuccess(dto.isSuccess());
        vo.setMessage(dto.getMessage());
        vo.setErrorCode(dto.getError());
        vo.setYear(dto.getYear());
        vo.setMonth(dto.getMonth());
        vo.setWeekSeq(dto.getWeekSeq());
        vo.setProcessedCount(dto.getProcessedCount());
        vo.setProcessingTime(dto.getProcessingTime());
        return vo;
    }
}
```

**问题**：
- ❌ 代码冗长，大量样板代码
- ❌ 容易出错（字段名写错、遗漏字段）
- ❌ 维护成本高（字段变化需要手动修改）

### MapStruct 方式

```java
// MapStruct 映射器 - 简洁、类型安全
@Mapper(componentModel = "spring")
public interface DistributionCalculateConverter {
    
    GenerateDistributionPlanRequestDto toDto(GenerateDistributionPlanRequestVo vo);
    
    @Mapping(source = "error", target = "errorCode")
    @Mapping(target = "allocationDetails", ignore = true)  // 忽略不需要的字段
    GenerateDistributionPlanResponseVo toVo(GenerateDistributionPlanResponseDto dto);
}
```

**优势**：
- ✅ 代码简洁，只需定义接口
- ✅ 编译时检查，类型安全
- ✅ 自动生成实现代码
- ✅ 易于维护（字段变化自动适配）

---

## 📦 如何集成 MapStruct

### 1. 添加 Maven 依赖

在 `pom.xml` 中添加：

```xml
<properties>
    <org.mapstruct.version>1.5.5.Final</org.mapstruct.version>
</properties>

<dependencies>
    <!-- MapStruct 核心库 -->
    <dependency>
        <groupId>org.mapstruct</groupId>
        <artifactId>mapstruct</artifactId>
        <version>${org.mapstruct.version}</version>
    </dependency>
    
    <!-- MapStruct 处理器（编译时使用） -->
    <dependency>
        <groupId>org.mapstruct</groupId>
        <artifactId>mapstruct-processor</artifactId>
        <version>${org.mapstruct.version}</version>
        <scope>provided</scope>
    </dependency>
</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.11.0</version>
            <configuration>
                <source>8</source>
                <target>8</target>
                <annotationProcessorPaths>
                    <path>
                        <groupId>org.mapstruct</groupId>
                        <artifactId>mapstruct-processor</artifactId>
                        <version>${org.mapstruct.version}</version>
                    </path>
                    <!-- 如果使用 Lombok，需要同时配置 -->
                    <path>
                        <groupId>org.projectlombok</groupId>
                        <artifactId>lombok</artifactId>
                        <version>${lombok.version}</version>
                    </path>
                    <!-- Lombok 和 MapStruct 的桥接 -->
                    <path>
                        <groupId>org.projectlombok</groupId>
                        <artifactId>lombok-mapstruct-binding</artifactId>
                        <version>0.2.0</version>
                    </path>
                </annotationProcessorPaths>
            </configuration>
        </plugin>
    </plugins>
</build>
```

### 2. 创建映射器接口

```java
package org.example.api.web.converter;

import org.example.api.web.vo.request.GenerateDistributionPlanRequestVo;
import org.example.api.web.vo.response.GenerateDistributionPlanResponseVo;
import org.example.application.dto.GenerateDistributionPlanRequestDto;
import org.example.application.dto.GenerateDistributionPlanResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * 分配计算转换器
 * MapStruct 会在编译时自动生成实现类
 */
@Mapper(componentModel = "spring")  // 生成 Spring Bean
public interface DistributionCalculateConverter {
    
    /**
     * VO 转 DTO
     * MapStruct 会自动匹配同名字段
     */
    GenerateDistributionPlanRequestDto toDto(GenerateDistributionPlanRequestVo vo);
    
    /**
     * DTO 转 VO
     * 使用 @Mapping 注解处理字段名不同的情况
     */
    @Mapping(source = "error", target = "errorCode")
    @Mapping(target = "allocationDetails", ignore = true)  // 忽略字段
    @Mapping(target = "allocationResult", ignore = true)
    GenerateDistributionPlanResponseVo toVo(GenerateDistributionPlanResponseDto dto);
}
```

### 3. 使用映射器

```java
@RestController
@RequestMapping("/api/calculate")
public class DistributionCalculateController {
    
    @Autowired
    private DistributionCalculateConverter converter;  // MapStruct 自动生成的实现
    
    @Autowired
    private DistributionCalculateService distributionService;
    
    @PostMapping("/generate-distribution-plan")
    public ResponseEntity<ApiResponseVo<GenerateDistributionPlanResponseVo>> generateDistributionPlan(
            @Valid @RequestBody GenerateDistributionPlanRequestVo requestVo) {
        
        try {
            // 使用 MapStruct 转换器：VO → DTO
            GenerateDistributionPlanRequestDto requestDto = converter.toDto(requestVo);
            
            // 调用 Service 层
            GenerateDistributionPlanResponseDto responseDto = 
                distributionService.generateDistributionPlan(requestDto);
            
            // 使用 MapStruct 转换器：DTO → VO
            GenerateDistributionPlanResponseVo responseVo = converter.toVo(responseDto);
            
            return ResponseEntity.ok(ApiResponseVo.success(responseVo));
            
        } catch (Exception e) {
            return ResponseEntity.ok(
                ApiResponseVo.error("生成分配计划失败: " + e.getMessage(), "GENERATION_FAILED")
            );
        }
    }
}
```

---

## 🔧 MapStruct 常用注解

### @Mapper

```java
@Mapper(
    componentModel = "spring",  // 生成 Spring Bean
    unmappedTargetPolicy = ReportingPolicy.IGNORE  // 忽略未映射的字段
)
public interface MyConverter {
    // ...
}
```

**componentModel 选项**：
- `"default"` - 不使用依赖注入
- `"spring"` - 生成 Spring Bean（推荐）
- `"cdi"` - 生成 CDI Bean
- `"jsr330"` - 生成 JSR-330 Bean

### @Mapping

```java
@Mapping(source = "sourceField", target = "targetField")
@Mapping(target = "ignoredField", ignore = true)
@Mapping(source = "nested.field", target = "targetField")  // 嵌套字段
@Mapping(source = "date", target = "dateString", dateFormat = "yyyy-MM-dd")  // 日期格式化
TargetObject toTarget(SourceObject source);
```

### @Mappings

```java
@Mappings({
    @Mapping(source = "error", target = "errorCode"),
    @Mapping(target = "allocationDetails", ignore = true),
    @Mapping(source = "startTime", target = "startTimestamp")
})
TargetObject toTarget(SourceObject source);
```

### 集合映射

```java
// 自动映射 List
List<TargetVo> toVoList(List<SourceDto> dtoList);

// 自动映射 Map
Map<String, TargetVo> toVoMap(Map<String, SourceDto> dtoMap);
```

---

## 📝 完整示例

### 1. VO 类

```java
// api/web/vo/request/GenerateDistributionPlanRequestVo.java
package org.example.api.web.vo.request;

import lombok.Data;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Min;
import javax.validation.constraints.Max;
import java.math.BigDecimal;

@Data
public class GenerateDistributionPlanRequestVo {
    @NotNull(message = "年份不能为空")
    @Min(value = 2020)
    @Max(value = 2099)
    private Integer year;
    
    @NotNull(message = "月份不能为空")
    @Min(value = 1)
    @Max(value = 12)
    private Integer month;
    
    @NotNull(message = "周序号不能为空")
    @Min(value = 1)
    @Max(value = 5)
    private Integer weekSeq;
    
    private BigDecimal urbanRatio;
    private BigDecimal ruralRatio;
}
```

### 2. DTO 类

```java
// application/dto/GenerateDistributionPlanRequestDto.java
package org.example.application.dto;

import lombok.Data;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class GenerateDistributionPlanRequestDto {
    @NotNull(message = "年份不能为空")
    private Integer year;
    
    @NotNull(message = "月份不能为空")
    private Integer month;
    
    @NotNull(message = "周序号不能为空")
    private Integer weekSeq;
    
    private BigDecimal urbanRatio;
    private BigDecimal ruralRatio;
}
```

### 3. MapStruct 映射器

```java
// api/web/converter/DistributionCalculateConverter.java
package org.example.api.web.converter;

import org.example.api.web.vo.request.GenerateDistributionPlanRequestVo;
import org.example.api.web.vo.response.GenerateDistributionPlanResponseVo;
import org.example.application.dto.GenerateDistributionPlanRequestDto;
import org.example.application.dto.GenerateDistributionPlanResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE  // 忽略未映射的字段
)
public interface DistributionCalculateConverter {
    
    /**
     * VO → DTO
     * 字段名相同，自动映射
     */
    GenerateDistributionPlanRequestDto toDto(GenerateDistributionPlanRequestVo vo);
    
    /**
     * DTO → VO
     * 需要处理字段名不同的情况
     */
    @Mapping(source = "error", target = "errorCode")
    @Mapping(target = "allocationDetails", ignore = true)
    @Mapping(target = "allocationResult", ignore = true)
    @Mapping(target = "exception", ignore = true)
    GenerateDistributionPlanResponseVo toVo(GenerateDistributionPlanResponseDto dto);
    
    /**
     * 批量转换
     */
    java.util.List<GenerateDistributionPlanResponseVo> toVoList(
        java.util.List<GenerateDistributionPlanResponseDto> dtoList
    );
}
```

### 4. MapStruct 自动生成的实现类

编译后，MapStruct 会自动生成实现类（在 `target/generated-sources/annotations/` 目录下）：

```java
// 自动生成的实现类（不需要手写）
@Generated(value = "org.mapstruct.ap.MappingProcessor")
@Component
public class DistributionCalculateConverterImpl implements DistributionCalculateConverter {
    
    @Override
    public GenerateDistributionPlanRequestDto toDto(GenerateDistributionPlanRequestVo vo) {
        if (vo == null) {
            return null;
        }
        
        GenerateDistributionPlanRequestDto dto = new GenerateDistributionPlanRequestDto();
        dto.setYear(vo.getYear());
        dto.setMonth(vo.getMonth());
        dto.setWeekSeq(vo.getWeekSeq());
        dto.setUrbanRatio(vo.getUrbanRatio());
        dto.setRuralRatio(vo.getRuralRatio());
        return dto;
    }
    
    @Override
    public GenerateDistributionPlanResponseVo toVo(GenerateDistributionPlanResponseDto dto) {
        if (dto == null) {
            return null;
        }
        
        GenerateDistributionPlanResponseVo vo = new GenerateDistributionPlanResponseVo();
        vo.setSuccess(dto.isSuccess());
        vo.setMessage(dto.getMessage());
        vo.setErrorCode(dto.getError());  // 字段名映射
        vo.setYear(dto.getYear());
        vo.setMonth(dto.getMonth());
        vo.setWeekSeq(dto.getWeekSeq());
        vo.setProcessedCount(dto.getProcessedCount());
        vo.setProcessingTime(dto.getProcessingTime());
        // allocationDetails, allocationResult, exception 被忽略
        return vo;
    }
}
```

---

## 🆚 MapStruct vs 其他方案

| 方案 | 性能 | 类型安全 | 代码量 | 学习成本 |
|------|------|---------|--------|---------|
| **手写转换器** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐ | ⭐⭐⭐⭐⭐ |
| **MapStruct** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| **BeanUtils** | ⭐⭐⭐ | ⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| **Dozer** | ⭐⭐ | ⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ |
| **ModelMapper** | ⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ |

### 为什么选择 MapStruct？

1. **性能最优**：编译时生成代码，无反射开销
2. **类型安全**：编译时检查，避免运行时错误
3. **代码简洁**：只需定义接口，自动生成实现
4. **易于维护**：字段变化自动适配

---

## ⚠️ 注意事项

### 1. Lombok 集成

如果项目使用 Lombok，需要配置 `lombok-mapstruct-binding`：

```xml
<path>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok-mapstruct-binding</artifactId>
    <version>0.2.0</version>
</path>
```

### 2. 字段名匹配规则

- **同名字段**：自动映射
- **不同名字段**：使用 `@Mapping` 注解
- **忽略字段**：使用 `@Mapping(target = "field", ignore = true)`

### 3. 空值处理

```java
@Mapper(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface MyConverter {
    // 忽略 null 值
}
```

### 4. 编译时生成

MapStruct 在编译时生成代码，需要：
- 运行 `mvn clean compile` 生成实现类
- IDE 可能需要刷新项目才能看到生成的类

---

## 📚 参考资源

- **官方网站**：https://mapstruct.org/
- **GitHub**：https://github.com/mapstruct/mapstruct
- **文档**：https://mapstruct.org/documentation/stable/reference/html/

---

## 🎯 总结

**MapStruct 是一个优秀的对象映射框架**，特别适合在 DDD 分层架构中进行 VO ↔ DTO 转换：

✅ **优势**：
- 编译时生成，性能最优
- 类型安全，编译时检查
- 代码简洁，易于维护
- 与 Spring 无缝集成

✅ **适用场景**：
- VO ↔ DTO 转换
- Entity ↔ DTO 转换
- 批量对象转换

✅ **推荐使用**：⭐⭐⭐⭐⭐

---

**建议**：在实施 VO 层改造时，使用 MapStruct 可以显著减少转换代码的工作量，提高开发效率和代码质量。

