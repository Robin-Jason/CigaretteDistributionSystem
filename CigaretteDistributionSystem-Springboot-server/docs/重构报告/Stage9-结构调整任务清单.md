# Stage9: 结构调整任务清单

## 📋 目标

将当前实际结构调整为完全符合预期DDD分层架构，确保所有文件都在正确的位置。

## 🎯 任务列表

### 9.1 Mapper迁移到infrastructure/persistence/mapper/（高优先级）

**当前状态**: Mapper在`org.example.mapper/`，应在`infrastructure/persistence/mapper/`

**需要迁移的文件**（7个）:
- [ ] BaseCustomerInfoMapper.java
- [ ] CigaretteDistributionInfoMapper.java
- [ ] CigaretteDistributionPredictionMapper.java
- [ ] CigaretteDistributionPredictionPriceMapper.java
- [ ] RegionCustomerStatisticsMapper.java
- [ ] IntegrityGroupMappingMapper.java
- [ ] TemporaryCustomerTableMapper.java

**任务步骤**:
1. 创建`infrastructure/persistence/mapper/`目录
2. 移动所有Mapper接口文件
3. 更新package声明：`package org.example.infrastructure.persistence.mapper;`
4. 更新所有import引用（全局搜索替换）
5. 更新`@MapperScan`配置：`@MapperScan({ "org.example.infrastructure.persistence.mapper" })`
6. 验证Mapper XML文件引用（XML文件在`src/main/resources/mapper/`，namespace需要更新）
7. 编译测试通过

**影响范围**:
- Repository实现类（7个）
- Service实现类（多个）
- 测试类

### 9.2 DTO分类迁移（高优先级）

**当前状态**: 所有DTO在`org.example.dto/`，需要分类到`application/dto/`和`shared/dto/`

**需要分析的DTO文件**（18个）:
- [ ] GenerateDistributionPlanRequestDto.java → `application/dto/`
- [ ] GenerateDistributionPlanResponseDto.java → `application/dto/`
- [ ] DataImportRequestDto.java → `application/dto/`
- [ ] CigaretteImportRequestDto.java → `application/dto/`
- [ ] BaseCustomerInfoImportRequestDto.java → `application/dto/`
- [ ] QueryRequestDto.java → `application/dto/`
- [ ] QueryCigaretteDistributionRecordDto.java → `application/dto/`
- [ ] QueryCigaretteDistributionResponseDto.java → `application/dto/`
- [ ] RegionCustomerStatisticsRequestDto.java → `application/dto/`
- [ ] RegionCustomerStatisticsResponseDto.java → `application/dto/`
- [ ] UpdateCigaretteRequestDto.java → `application/dto/`
- [ ] UpdatePredictionGradesRequestDto.java → `application/dto/`
- [ ] DeleteAreasRequestDto.java → `application/dto/`
- [ ] DeleteDeliveryAreasResponseDto.java → `application/dto/`
- [ ] BatchUpdateFromExpressionsRequestDto.java → `application/dto/`
- [ ] CalRegionCustomerNumRequestDto.java → `application/dto/`
- [ ] TotalActualDeliveryResponseDto.java → `application/dto/`
- [ ] RegionCustomerRecord.java → `shared/dto/`（共享DTO，跨层使用）

**任务步骤**:
1. 创建`application/dto/`目录
2. 创建`shared/dto/`目录
3. 分析每个DTO的用途，确定分类
4. 迁移文件并更新package声明
5. 更新所有import引用（全局搜索替换）
6. 编译测试通过

**影响范围**:
- Controller类（3个）
- Service接口和实现类（多个）
- 测试类

### 9.3 算法适配器迁移到infrastructure/algorithm/（中优先级）

**当前状态**: 算法适配器在`org.example.algorithm/`，应在`infrastructure/algorithm/`

**需要迁移的文件**（6个）:
- [ ] SingleLevelDistributionAlgorithm.java
- [ ] ColumnWiseAdjustmentAlgorithm.java
- [ ] GroupSplittingDistributionAlgorithm.java
- [ ] impl/DefaultSingleLevelDistributionAlgorithm.java
- [ ] impl/DefaultColumnWiseAdjustmentAlgorithm.java
- [ ] impl/DefaultGroupSplittingDistributionAlgorithm.java

**任务步骤**:
1. 创建`infrastructure/algorithm/`目录结构
2. 移动所有算法适配器文件
3. 更新package声明：`package org.example.infrastructure.algorithm;`
4. 更新所有import引用
5. 编译测试通过

**影响范围**:
- 应用服务类（可能使用算法适配器）
- 测试类

### 9.4 转换器迁移（中优先级）

**当前状态**: `DistributionDataConverter`在`shared/util/`，应在`application/converter/`

**需要迁移的文件**（1个）:
- [ ] DistributionDataConverter.java

**任务步骤**:
1. 创建`application/converter/`目录
2. 移动文件
3. 更新package声明：`package org.example.application.converter;`
4. 更新所有import引用
5. 编译测试通过

**影响范围**:
- 应用服务实现类（使用转换器的地方）

### 9.5 清理遗留目录（低优先级）

**需要清理的目录**:
- [ ] `dao/` - 已迁移到Repository，检查后删除
- [ ] `entity/` - 已迁移到PO，检查后删除
- [ ] `service/` - 已迁移到application/service，检查遗留文件后删除
- [ ] `domain/service/impl/` - 空目录，直接删除
- [ ] `domain/rule/` - 空目录，直接删除
- [ ] `mapper/` - Mapper迁移后删除
- [ ] `dto/` - DTO迁移后删除
- [ ] `algorithm/` - 算法适配器迁移后删除

**任务步骤**:
1. 全局搜索确认每个目录无引用
2. 检查遗留文件，确认可安全删除
3. 删除空目录和遗留目录
4. 编译测试通过

## 📊 实施优先级

### 高优先级（影响架构清晰度）
1. **Stage9.1: Mapper迁移** - Mapper是基础设施层核心组件
2. **Stage9.2: DTO分类迁移** - DTO分类影响依赖方向

### 中优先级（提升架构完整性）
3. **Stage9.3: 算法适配器迁移** - 算法适配器属于基础设施层
4. **Stage9.4: 转换器迁移** - 转换器属于应用层职责

### 低优先级（清理工作）
5. **Stage9.5: 清理遗留目录** - 不影响功能，但影响代码整洁度

## ⚠️ 注意事项

1. **Mapper XML文件**
   - XML文件位置`src/main/resources/mapper/`是正确的，无需移动
   - 但需要更新XML中的`namespace`属性指向新的包路径

2. **@MapperScan配置**
   - 需要更新`CigaretteDistributionApplication.java`中的`@MapperScan`注解

3. **依赖关系检查**
   - 每次迁移后需要检查所有import引用
   - 确保依赖方向符合DDD原则

4. **测试验证**
   - 每个子任务完成后运行完整测试
   - 确保功能正常

5. **渐进式迁移**
   - 建议按子任务顺序进行，每次完成一个子任务
   - 确保编译和测试通过后再进行下一个

## 📈 预期收益

完成结构调整后：
- ✅ 完全符合DDD分层架构原则
- ✅ 依赖方向清晰明确
- ✅ 代码组织更加规范
- ✅ 便于后续维护和扩展

