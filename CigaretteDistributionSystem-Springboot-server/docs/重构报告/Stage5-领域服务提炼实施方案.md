# Stage5: 领域服务提炼实施方案

## 目标
将算法逻辑从应用层提炼到领域层，提升代码可测试性和业务逻辑内聚性。

## 当前架构

### 算法接口位置
- `org.example.algorithm.SingleLevelDistributionAlgorithm`
- `org.example.algorithm.ColumnWiseAdjustmentAlgorithm`
- `org.example.algorithm.GroupSplittingDistributionAlgorithm`

### 算法实现位置
- `org.example.algorithm.impl.DefaultSingleLevelDistributionAlgorithm` (使用Spring @Component)
- `org.example.algorithm.impl.DefaultColumnWiseAdjustmentAlgorithm` (使用Spring @Component)
- `org.example.algorithm.impl.DefaultGroupSplittingDistributionAlgorithm` (使用Spring @Component)

## 目标架构

### 领域服务接口（纯Java，无Spring依赖）
- `org.example.domain.service.SingleLevelDistributionService`
- `org.example.domain.service.ColumnWiseAdjustmentService`
- `org.example.domain.service.GroupSplittingDistributionService`

### 领域服务实现（纯Java，无Spring依赖）
- `org.example.domain.service.impl.SingleLevelDistributionServiceImpl`
- `org.example.domain.service.impl.ColumnWiseAdjustmentServiceImpl`
- `org.example.domain.service.impl.GroupSplittingDistributionServiceImpl`

### 适配器层（保留Spring注解，调用领域服务）
- `org.example.algorithm.impl.DefaultSingleLevelDistributionAlgorithm` → 适配器，调用领域服务
- `org.example.algorithm.impl.DefaultColumnWiseAdjustmentAlgorithm` → 适配器，调用领域服务
- `org.example.algorithm.impl.DefaultGroupSplittingDistributionAlgorithm` → 适配器，调用领域服务

## 实施步骤

### 步骤1: 创建领域服务接口和实现
1. 创建`domain/service`目录 ✅
2. 创建领域服务接口（不含Spring注解）
3. 创建领域服务实现（不含Spring注解，纯Java逻辑）

### 步骤2: 迁移算法逻辑
1. 将算法核心逻辑从`algorithm/impl`迁移到`domain/service/impl`
2. 移除Spring注解和依赖
3. 保持接口签名一致

### 步骤3: 创建适配器
1. 在`algorithm/impl`中创建适配器，调用领域服务
2. 保持Spring注解，确保现有代码正常工作
3. 适配器仅负责依赖注入和调用领域服务

### 步骤4: 测试验证
1. 运行集成测试验证功能正常
2. 验证领域服务可独立测试（无Spring/DB依赖）

## 优势

1. **可测试性提升**: 领域服务可独立测试，无需Spring容器
2. **业务逻辑内聚**: 核心算法逻辑集中在领域层
3. **向后兼容**: 保留适配器层，现有代码无需修改
4. **架构清晰**: 明确区分领域逻辑和基础设施依赖

## 注意事项

1. **渐进式重构**: 先提炼一个算法作为试点
2. **保持接口一致**: 确保领域服务接口与现有算法接口兼容
3. **测试覆盖**: 每个算法提炼后运行完整测试
4. **文档更新**: 及时更新架构文档

