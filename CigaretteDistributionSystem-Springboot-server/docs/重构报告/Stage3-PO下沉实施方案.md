# Stage3: PO下沉实施方案

## 目标
将MyBatis-Plus实体类从`org.example.entity`迁移到`org.example.infrastructure.persistence.po`，明确基础设施层边界。

## 需要迁移的实体类

1. `BaseCustomerInfo` → `BaseCustomerInfoPO`
2. `CigaretteDistributionInfoData` → `CigaretteDistributionInfoPO`
3. `CigaretteDistributionPredictionData` → `CigaretteDistributionPredictionPO`
4. `IntegrityGroupMapping` → `IntegrityGroupMappingPO`

## 迁移步骤

### 1. 创建目标目录
- ✅ 已创建：`src/main/java/org/example/infrastructure/persistence/po`

### 2. 迁移实体类
- [ ] 迁移`BaseCustomerInfo`并重命名为`BaseCustomerInfoPO`
- [ ] 迁移`CigaretteDistributionInfoData`并重命名为`CigaretteDistributionInfoPO`
- [ ] 迁移`CigaretteDistributionPredictionData`并重命名为`CigaretteDistributionPredictionPO`
- [ ] 迁移`IntegrityGroupMapping`并重命名为`IntegrityGroupMappingPO`

### 3. 更新引用
需要更新以下文件的import：
- Mapper接口（4个）
- Repository接口（4个）
- Repository实现类（4个）
- Service实现类（多个）
- 其他引用（DAO接口等）

### 4. 编译测试
- [ ] 编译验证
- [ ] 运行测试

## 注意事项

1. **保留MyBatis-Plus注解**：所有实体类需要保留`@TableName`、`@TableId`、`@TableField`等注解
2. **更新包名**：将`package org.example.entity;`改为`package org.example.infrastructure.persistence.po;`
3. **更新类名**：添加`PO`后缀
4. **批量替换import**：使用IDE的批量替换功能更新所有引用

## 影响范围

根据grep结果，约有27个文件引用了`org.example.entity`包，需要全部更新。

