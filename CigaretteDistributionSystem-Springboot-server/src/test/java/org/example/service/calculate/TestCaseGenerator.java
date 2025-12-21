package org.example.service.calculate;

import lombok.extern.slf4j.Slf4j;
import org.example.domain.model.valueobject.DeliveryExtensionType;
import org.example.domain.repository.IntegrityGroupMappingRepository;
import org.example.infrastructure.config.encoding.EncodingRuleProperties;
import org.example.infrastructure.persistence.po.IntegrityGroupMappingPO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * 测试用例生成器。
 * <p>
 * 用于生成高覆盖率的测试用例组合，支持：
 * 1. 预投放量分层生成（按1000为阶层）
 * 2. Pairwise组合测试设计
 * 3. 区域数量动态计算
 * 4. 从 encoding-rules.yml 读取区域名称规则
 * </p>
 *
 * @author Robin
 * @since 2025-12-20
 */
@Slf4j
@Component
public class TestCaseGenerator {

    @Autowired(required = false)
    private EncodingRuleProperties encodingRuleProperties;

    @Autowired(required = false)
    private IntegrityGroupMappingRepository integrityGroupMappingRepository;

    // 缓存从配置文件读取的区域名称
    private Map<String, List<String>> regionCache = new HashMap<>();

    @PostConstruct
    public void init() {
        if (encodingRuleProperties != null && encodingRuleProperties.getRegionTypes() != null 
                && !encodingRuleProperties.getRegionTypes().isEmpty()) {
            loadRegionsFromConfig();
        } else {
            // 如果没有配置，使用默认值
            loadDefaultRegions();
        }
    }

    /**
     * 从 encoding-rules.yml 配置文件加载区域名称
     */
    private void loadRegionsFromConfig() {
        if (encodingRuleProperties == null || encodingRuleProperties.getRegionTypes() == null) {
            loadDefaultRegions();
            return;
        }

        // 加载区县（COUNTY）
        List<String> countyRegions = loadRegionsByType(DeliveryExtensionType.COUNTY, true);
        if (!countyRegions.isEmpty()) {
            regionCache.put("区县", countyRegions);
        }

        // 加载市场类型（MARKET_TYPE）
        List<String> marketTypeRegions = loadRegionsByType(DeliveryExtensionType.MARKET_TYPE, false);
        if (!marketTypeRegions.isEmpty()) {
            regionCache.put("市场类型", marketTypeRegions);
        }

        // 加载城乡分类代码（URBAN_RURAL_CODE）
        List<String> urbanRuralRegions = loadRegionsByType(DeliveryExtensionType.URBAN_RURAL_CODE, false);
        if (!urbanRuralRegions.isEmpty()) {
            regionCache.put("城乡分类代码", urbanRuralRegions);
        }

        // 加载业态（BUSINESS_FORMAT）
        List<String> businessFormatRegions = loadRegionsByType(DeliveryExtensionType.BUSINESS_FORMAT, false);
        if (!businessFormatRegions.isEmpty()) {
            regionCache.put("业态", businessFormatRegions);
        }

        // 加载市场部（MARKET_DEPARTMENT）
        List<String> marketDeptRegions = loadRegionsByType(DeliveryExtensionType.MARKET_DEPARTMENT, true);
        if (!marketDeptRegions.isEmpty()) {
            regionCache.put("市场部", marketDeptRegions);
        }

        // 加载商圈类型（BUSINESS_DISTRICT）
        List<String> businessDistrictRegions = loadRegionsByType(DeliveryExtensionType.BUSINESS_DISTRICT, false);
        if (!businessDistrictRegions.isEmpty()) {
            regionCache.put("商圈类型", businessDistrictRegions);
        }

        // 加载信用等级（CREDIT_LEVEL）
        List<String> creditLevelRegions = loadRegionsByType(DeliveryExtensionType.CREDIT_LEVEL, false);
        if (!creditLevelRegions.isEmpty()) {
            regionCache.put("信用等级", creditLevelRegions);
        }

        // 加载诚信互助小组（INTEGRITY_GROUP）：使用编号（Z1, Z2, ...）而不是名称
        // 注意：诚信互助小组的区域应该从integrity_group_code_mapping表的GROUP_CODE编码集合中抽取
        List<String> integrityGroupCodes = loadIntegrityGroupCodes();
        if (!integrityGroupCodes.isEmpty()) {
            regionCache.put("诚信互助小组", integrityGroupCodes);
        } else {
            // 如果没有编号数据，不应该使用区县作为占位，应该抛出异常或记录警告
            log.warn("警告：诚信互助小组的编码数据为空，无法生成测试用例。请确保integrity_group_code_mapping表中有数据。");
            // 不设置regionCache，让后续代码能够检测到问题
        }
    }

    /**
     * 从配置文件中加载指定类型的区域名称
     * 
     * @param type 扩展类型
     * @param useSimplified 是否使用简化名称（区县取前两个字，市场部去掉后缀）
     * @return 区域名称列表
     */
    private List<String> loadRegionsByType(DeliveryExtensionType type, boolean useSimplified) {
        List<String> regions = new ArrayList<>();
        
        for (EncodingRuleProperties.RegionTypeRule regionTypeRule : encodingRuleProperties.getRegionTypes()) {
            if (regionTypeRule.getType() == type) {
                for (EncodingRuleProperties.RegionEntry entry : regionTypeRule.getEntries()) {
                    if (entry.getLabels() == null || entry.getLabels().isEmpty()) {
                        continue;
                    }
                    
                    String regionName;
                    if (useSimplified) {
                        // 对于区县和市场部，使用简化版本
                        if (type == DeliveryExtensionType.COUNTY) {
                            // 区县：使用最后一个label（简化版本，前两个字）
                            // 配置文件格式：["完整名称1", "完整名称2", "简化名称"]
                            if (entry.getLabels().size() >= 3) {
                                regionName = entry.getLabels().get(entry.getLabels().size() - 1); // 最后一个
                            } else if (entry.getLabels().size() == 2) {
                                regionName = entry.getLabels().get(1); // 第二个
                            } else {
                                // 如果只有一个label，取前两个字
                                regionName = entry.getLabels().get(0);
                                if (regionName.length() > 2 && !regionName.equals("城区") && !regionName.equals("房县")) {
                                    regionName = regionName.substring(0, 2);
                                }
                            }
                        } else if (type == DeliveryExtensionType.MARKET_DEPARTMENT) {
                            // 市场部：使用第二个label（去掉"市场部"后缀的版本）
                            if (entry.getLabels().size() > 1) {
                                regionName = entry.getLabels().get(1);
                            } else {
                                // 如果只有一个label，去掉"市场部"后缀
                                regionName = entry.getLabels().get(0).replace("市场部", "");
                            }
                        } else {
                            // 其他类型：使用第一个label
                            regionName = entry.getLabels().get(0);
                        }
                    } else {
                        // 使用第一个label（完整版本）
                        regionName = entry.getLabels().get(0);
                    }
                    
                    if (regionName != null && !regionName.isEmpty()) {
                        regions.add(regionName);
                    }
                }
                break;
            }
        }
        
        return regions;
    }

    /**
     * 加载默认区域名称（当配置文件不可用时使用）
     */
    private void loadDefaultRegions() {
        regionCache.put("区县", Arrays.asList("城区", "丹江", "房县", "郧阳", "竹山", "郧西", "竹溪"));
        regionCache.put("市场类型", Arrays.asList("城网", "农网"));
        regionCache.put("城乡分类代码", Arrays.asList("主城区", "村庄", "镇中心区", "镇乡结合区", "乡中心区", "特殊区域", "城乡结合区"));
        regionCache.put("业态", Arrays.asList("便利店", "其他", "烟草专业店", "娱乐服务类", "超市", "商场"));
        regionCache.put("市场部", Arrays.asList("茅箭", "白浪", "张湾", "丹江口城区", "房县城关",
                "城关", "竹山城关", "武当山", "郧阳城关", "宝丰",
                "竹溪城关", "羊尾", "柳陂", "南化", "店子",
                "竹坪", "浪河", "蒋家堰", "化龙", "水坪",
                "习家店", "泉溪", "大木", "青峰", "官渡",
                "门古", "鲍峡"));
        regionCache.put("商圈类型", Arrays.asList("居民区", "集贸区", "工业区", "商业娱乐区", "办公区", "旅游景区", 
                "交通枢纽区", "院校学区", "其他"));
        regionCache.put("信用等级", Arrays.asList("AA", "C", "D", "A", "AAA", "B"));
        // 注意：诚信互助小组不应该使用区县作为默认值，应该从integrity_group_code_mapping表加载
        // 这里保留是为了向后兼容，但实际应该从数据库加载
        // regionCache.put("诚信互助小组", Arrays.asList("城区", "丹江", "房县", "郧阳", "竹山", "郧西", "竹溪"));
    }

    /**
     * 预投放量阶层配置
     */
    public static class AdvTier {
        public final double min;
        public final double max;
        public final int sampleCount;

        public AdvTier(double min, double max, int sampleCount) {
            this.min = min;
            this.max = max;
            this.sampleCount = sampleCount;
        }
    }

    /**
     * 测试用例配置
     */
    public static class TestCaseConfig {
        public final String deliveryMethod;
        public final String deliveryEtype;
        public final String tag;
        public final BigDecimal adv;
        public final int regionCount;
        public final List<String> availableRegions; // 该扩展类型组合对应的可用区域集合

        public TestCaseConfig(String deliveryMethod, String deliveryEtype, String tag,
                             BigDecimal adv, int regionCount, List<String> availableRegions) {
            this.deliveryMethod = deliveryMethod;
            this.deliveryEtype = deliveryEtype;
            this.tag = tag;
            this.adv = adv;
            this.regionCount = regionCount;
            this.availableRegions = availableRegions;
        }

        @Override
        public String toString() {
            return String.format("TestCase{method=%s, etype=%s, tag=%s, adv=%s, regions=%d, availableRegions=%d}",
                    deliveryMethod, deliveryEtype, tag, adv, regionCount, 
                    availableRegions != null ? availableRegions.size() : 0);
        }
    }

    // 预投放量阶层配置
    public static final List<AdvTier> ADV_TIERS = Arrays.asList(
            new AdvTier(0, 1000, 3),           // 0-1000: 3个用例
            new AdvTier(1000, 2000, 3),        // 1000-2000: 3个用例
            new AdvTier(2000, 5000, 4),        // 2000-5000: 4个用例
            new AdvTier(5000, 10000, 5),       // 5000-10000: 5个用例
            new AdvTier(10000, 20000, 5),     // 10000-20000: 5个用例
            new AdvTier(20000, 50000, 6),      // 20000-50000: 6个用例
            new AdvTier(50000, 100000, 7),     // 50000-100000: 7个用例
            new AdvTier(100000, 150000, 8)     // 100000-150000: 8个用例
    );

    // 投放方式
    public static final List<String> DELIVERY_METHODS = Arrays.asList(
            "按档位投放",
            "按档位扩展投放"
    );

    // 单扩展类型（完整覆盖8种）
    public static final List<String> SINGLE_EXTENSION_TYPES = Arrays.asList(
            "档位+区县",              // 编码1
            "档位+市场类型",          // 编码2
            "档位+城乡分类代码",      // 编码3
            "档位+业态",              // 编码4
            "档位+市场部",            // 编码5
            "档位+商圈类型",          // 编码6
            "档位+信用等级",          // 编码7
            "档位+诚信互助小组"       // 编码8
    );

    // 双扩展类型（完整覆盖常用组合）
    public static final List<String> DUAL_EXTENSION_TYPES = Arrays.asList(
            // 区县 + 其他类型（最常用）
            "档位+区县+市场类型",          // 区县 + 市场类型
            "档位+区县+城乡分类代码",      // 区县 + 城乡分类代码
            "档位+区县+业态",              // 区县 + 业态
            "档位+区县+市场部",            // 区县 + 市场部
            "档位+区县+商圈类型",          // 区县 + 商圈类型
            "档位+区县+信用等级",          // 区县 + 信用等级
            "档位+区县+诚信互助小组",      // 区县 + 诚信互助小组
            // 市场类型 + 其他类型
            "档位+市场类型+业态",          // 市场类型 + 业态
            "档位+市场类型+市场部",        // 市场类型 + 市场部
            // 城乡分类代码 + 其他类型
            "档位+城乡分类代码+业态",      // 城乡分类代码 + 业态
            // 业态 + 其他类型
            "档位+业态+市场部"            // 业态 + 市场部
    );

    // 标签
    public static final List<String> TAGS = Arrays.asList(
            null,
            "优质数据共享客户"
    );

    /**
     * 生成所有测试用例（静态方法，用于向后兼容）
     *
     * @param maxRegions 最大区域数
     * @param randomSeed 随机种子（保证可重复性）
     * @return 测试用例列表
     */
    public static List<TestCaseConfig> generateAllTestCases(int maxRegions, long randomSeed) {
        // 创建临时实例以使用默认区域
        TestCaseGenerator generator = new TestCaseGenerator();
        generator.loadDefaultRegions();
        return generator.generateAllTestCasesInternal(maxRegions, randomSeed);
    }

    /**
     * 生成所有测试用例（实例方法，使用配置的区域名称）
     *
     * @param maxRegions 最大区域数
     * @param randomSeed 随机种子（保证可重复性）
     * @return 测试用例列表
     */
    public List<TestCaseConfig> generateAllTestCasesInternal(int maxRegions, long randomSeed) {
        List<TestCaseConfig> testCases = new ArrayList<>();
        Random random = new Random(randomSeed);

        // 1. 生成基础组合（Pairwise）
        List<BaseCombination> baseCombinations = generatePairwiseCombinations();

        // 2. 为每个基础组合生成不同投放量阶层的用例
        for (BaseCombination base : baseCombinations) {
            // 获取该扩展类型组合对应的可用区域集合
            List<String> availableRegions = getAvailableRegionsForExtensionType(base.deliveryEtype);
            
            for (AdvTier tier : ADV_TIERS) {
                for (int i = 0; i < tier.sampleCount; i++) {
                    BigDecimal adv = generateRandomAdvInTier(tier, random);
                    // 根据投放方式和扩展类型计算区域数量（0表示"全市"，>0表示具体区域数量）
                    int regionCount = calculateRegionCountByAdv(adv, availableRegions.size(), random, 
                            base.deliveryMethod, base.deliveryEtype);
                    testCases.add(new TestCaseConfig(
                            base.deliveryMethod,
                            base.deliveryEtype,
                            base.tag,
                            adv,
                            regionCount,
                            availableRegions
                    ));
                }
            }
        }

        // 3. 添加边界值用例
        addBoundaryValueTestCases(testCases, maxRegions, random);

        // 4. 添加特殊场景用例
        addSpecialScenarioTestCases(testCases, maxRegions);

        return testCases;
    }

    /**
     * 生成Pairwise组合
     * 
     * 业务规则：
     * 1. 按档位投放：不允许扩展类型，投放区域默认为"全市"
     * 2. 按档位扩展投放：必须有1-2个扩展类型，投放区域可以是具体区域组合
     * 3. 按价位段自选投放：可以有0-2个扩展类型，但投放区域默认为"全市"
     * 4. 标签：最多1个，目前只有"优质数据共享客户"，标签过滤值为0
     */
    private static List<BaseCombination> generatePairwiseCombinations() {
        List<BaseCombination> combinations = new ArrayList<>();

        // 1. 按档位投放（不允许扩展类型，默认全市）
        for (String tag : TAGS) {
            combinations.add(new BaseCombination("按档位投放", null, tag));
        }

        // 2. 按档位扩展投放 + 单扩展（必须有1个扩展类型）
        for (String etype : SINGLE_EXTENSION_TYPES) {
            for (String tag : TAGS) {
                combinations.add(new BaseCombination("按档位扩展投放", etype, tag));
            }
        }

        // 3. 按档位扩展投放 + 双扩展（必须有2个扩展类型）
        for (String etype : DUAL_EXTENSION_TYPES) {
            for (String tag : TAGS) {
                combinations.add(new BaseCombination("按档位扩展投放", etype, tag));
            }
        }

        // 4. 按价位段自选投放（不允许扩展类型，默认全市）
        // 注意：按价位段自选投放不允许扩展类型，投放区域固定为"全市"
        for (String tag : TAGS) {
            // 无扩展类型（唯一允许的组合）
            combinations.add(new BaseCombination("按价位段自选投放", null, tag));
        }

        return combinations;
    }

    /**
     * 根据扩展类型获取对应的可用区域集合
     * 
     * 业务规则：
     * - 单扩展类型：返回该扩展类型对应的区域集合
     * - 双扩展类型：返回两个扩展类型的笛卡尔积区域集合
     * - 无扩展类型：返回空列表（表示"全市"）
     * 
     * @param deliveryEtype 扩展类型（如"档位+区县"、"档位+区县+市场类型"）
     * @return 可用区域集合
     */
    private List<String> getAvailableRegionsForExtensionType(String deliveryEtype) {
        if (deliveryEtype == null || deliveryEtype.isEmpty()) {
            return Collections.emptyList(); // 无扩展类型，表示"全市"
        }

        // 解析扩展类型
        String[] parts = deliveryEtype.split("\\+");
        if (parts.length < 2) {
            return Collections.emptyList();
        }

        // 提取扩展类型（去掉"档位"）
        List<String> extensionTypes = new ArrayList<>();
        for (int i = 1; i < parts.length; i++) {
            extensionTypes.add(parts[i].trim());
        }

        // 单扩展类型
        if (extensionTypes.size() == 1) {
            return getRegionsForSingleExtension(extensionTypes.get(0));
        }

        // 双扩展类型：计算笛卡尔积
        if (extensionTypes.size() == 2) {
            List<String> regions1 = getRegionsForSingleExtension(extensionTypes.get(0));
            List<String> regions2 = getRegionsForSingleExtension(extensionTypes.get(1));
            return calculateCartesianProduct(regions1, regions2);
        }

        // 更多扩展类型暂不支持
        return Collections.emptyList();
    }

    /**
     * 获取单扩展类型对应的区域集合
     * 
     * 优先从 encoding-rules.yml 配置文件读取，如果配置不可用则使用默认值。
     * 特殊处理规则：
     * 1. 区县：使用配置中的简化名称（前两个字）
     * 2. 市场部：使用配置中的简化名称（去掉"市场部"后缀）
     */
    private List<String> getRegionsForSingleExtension(String extensionType) {
        // 优先从缓存中获取
        if (regionCache.containsKey(extensionType)) {
            return new ArrayList<>(regionCache.get(extensionType));
        }
        
        // 如果缓存中没有，返回空列表
        return Collections.emptyList();
    }

    /**
     * 计算两个区域集合的笛卡尔积
     * 格式：主区域（子区域），例如："丹江（城网）"、"城区（Z1）"
     * 
     * 业务规则：
     * - 双扩展类型的投放区域格式为：主区域（子区域），用逗号分隔
     * - 例如："丹江（城网）、丹江（农网）、房县（城网），房县（农网）"
     */
    private List<String> calculateCartesianProduct(List<String> regions1, List<String> regions2) {
        List<String> result = new ArrayList<>();
        for (String r1 : regions1) {
            for (String r2 : regions2) {
                // 格式：主区域（子区域）
                result.add(r1 + "（" + r2 + "）");
            }
        }
        return result;
    }

    /**
     * 从数据库加载诚信互助小组的编号（Z1, Z2, ...）
     * 从integrity_group_code_mapping表的GROUP_CODE字段获取编码集合
     * 
     * @return 诚信互助小组编号列表（如Z1, Z2, Z3等）
     */
    private List<String> loadIntegrityGroupCodes() {
        List<String> codes = new ArrayList<>();
        if (integrityGroupMappingRepository == null) {
            return codes;
        }
        
        try {
            List<IntegrityGroupMappingPO> mappings = integrityGroupMappingRepository.selectAllOrderBySort();
            for (IntegrityGroupMappingPO mapping : mappings) {
                if (mapping.getGroupCode() != null && !mapping.getGroupCode().isEmpty()) {
                    codes.add(mapping.getGroupCode());
                }
            }
        } catch (Exception e) {
            // 如果查询失败，返回空列表
            // 在测试环境中，可能表不存在或数据未初始化
        }
        
        return codes;
    }

    /**
     * 根据预投放量和投放方式计算区域数量
     * 
     * 业务规则：
     * - 按档位投放：固定为"全市"（regionCount=0表示全市）
     * - 按价位段自选投放：固定为"全市"（regionCount=0表示全市）
     * - 按档位扩展投放：根据预投放量和可用区域数量动态计算区域数量
     *   - 小投放量：区域较少（1-30%）
     *   - 大投放量：区域较多（50%-100%）
     * 
     * @param adv 预投放量
     * @param maxAvailableRegions 该扩展类型组合的最大可用区域数
     * @param random 随机数生成器
     * @param deliveryMethod 投放方式
     * @param deliveryEtype 扩展类型
     * @return 区域数量（0表示全市，>0表示具体区域数量）
     */
    private int calculateRegionCountByAdv(BigDecimal adv, int maxAvailableRegions, Random random, 
                                                 String deliveryMethod, String deliveryEtype) {
        // 按档位投放和按价位段自选投放：固定为"全市"
        if ("按档位投放".equals(deliveryMethod) || "按价位段自选投放".equals(deliveryMethod)) {
            return 0; // 0表示"全市"
        }

        // 如果没有可用区域，返回0（全市）
        if (maxAvailableRegions <= 0) {
            return 0;
        }

        // 按档位扩展投放：根据预投放量动态计算区域数量
        double advValue = adv.doubleValue();
        if (advValue < 1000) {
            // 小投放量：1个区域
            return 1;
        } else if (advValue < 5000) {
            // 小投放量：1-2个区域（最多30%）
            int maxCount = Math.max(1, (int) (maxAvailableRegions * 0.3));
            return random.nextInt(Math.min(2, maxCount)) + 1;
        } else if (advValue < 20000) {
            // 中投放量：30%-50%的区域
            int minCount = Math.max(1, (int) (maxAvailableRegions * 0.3));
            int maxCount = Math.max(minCount, (int) (maxAvailableRegions * 0.5));
            return random.nextInt(maxCount - minCount + 1) + minCount;
        } else if (advValue < 100000) {
            // 大投放量：50%-80%的区域
            int minCount = Math.max(1, (int) (maxAvailableRegions * 0.5));
            int maxCount = Math.max(minCount, (int) (maxAvailableRegions * 0.8));
            return random.nextInt(maxCount - minCount + 1) + minCount;
        } else {
            // 超大投放量：80%-100%的区域
            int minCount = Math.max(1, (int) (maxAvailableRegions * 0.8));
            return random.nextInt(maxAvailableRegions - minCount + 1) + minCount;
        }
    }

    /**
     * 在阶层内随机生成预投放量
     */
    private static BigDecimal generateRandomAdvInTier(AdvTier tier, Random random) {
        double value = tier.min + (tier.max - tier.min) * random.nextDouble();
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 添加边界值用例
     */
    private void addBoundaryValueTestCases(List<TestCaseConfig> testCases,
                                                  int maxRegions, Random random) {
        BigDecimal[] boundaries = {
                BigDecimal.ONE,
                BigDecimal.valueOf(999),
                BigDecimal.valueOf(1000),
                BigDecimal.valueOf(1001),
                BigDecimal.valueOf(99999),
                BigDecimal.valueOf(100000),
                BigDecimal.valueOf(150000)
        };

        for (BigDecimal adv : boundaries) {
            // 按档位投放：固定为"全市"（regionCount=0）
            testCases.add(new TestCaseConfig(
                    "按档位投放",
                    null,
                    null,
                    adv,
                    0,  // 0表示"全市"
                    Collections.emptyList()
            ));
        }
    }

    /**
     * 添加特殊场景用例
     */
    private void addSpecialScenarioTestCases(List<TestCaseConfig> testCases, int maxRegions) {
        // 场景1：按档位投放 + 大投放量（固定全市）
        testCases.add(new TestCaseConfig("按档位投放", null, null,
                BigDecimal.valueOf(50000), 0, Collections.emptyList()));  // 0表示"全市"

        // 场景2：按档位扩展投放 + 单区域 + 大投放量（区县类型）
        List<String> countyRegions = getRegionsForSingleExtension("区县");
        testCases.add(new TestCaseConfig("按档位扩展投放", "档位+区县", null,
                BigDecimal.valueOf(50000), 1, countyRegions));

        // 场景3：按档位扩展投放 + 全部区域 + 小投放量（区县类型）
        testCases.add(new TestCaseConfig("按档位扩展投放", "档位+区县", null,
                BigDecimal.valueOf(5000), countyRegions.size(), countyRegions));

        // 场景4：按档位扩展投放 + 双扩展 + 大投放量 + 标签（区县+市场类型）
        List<String> dualRegions = calculateCartesianProduct(
                getRegionsForSingleExtension("区县"),
                getRegionsForSingleExtension("市场类型"));
        testCases.add(new TestCaseConfig("按档位扩展投放", "档位+区县+市场类型",
                "优质数据共享客户", BigDecimal.valueOf(100000), 
                Math.max(1, dualRegions.size() - 1), dualRegions));

        // 场景5：按价位段自选投放 + 大投放量（固定全市）
        testCases.add(new TestCaseConfig("按价位段自选投放", null, null,
                BigDecimal.valueOf(100000), 0, Collections.emptyList()));  // 0表示"全市"

        // 场景6：按档位扩展投放 + 新增扩展类型（商圈类型）
        List<String> districtRegions = getRegionsForSingleExtension("商圈类型");
        testCases.add(new TestCaseConfig("按档位扩展投放", "档位+商圈类型", null,
                BigDecimal.valueOf(30000), 3, districtRegions));

        // 场景7：按档位扩展投放 + 新增扩展类型（信用等级）+ 标签
        List<String> creditRegions = getRegionsForSingleExtension("信用等级");
        testCases.add(new TestCaseConfig("按档位扩展投放", "档位+信用等级", "优质数据共享客户",
                BigDecimal.valueOf(40000), 4, creditRegions));

        // 场景8：按档位扩展投放 + 新增扩展类型（诚信互助小组）
        List<String> groupRegions = getRegionsForSingleExtension("诚信互助小组");
        if (groupRegions == null || groupRegions.isEmpty()) {
            // 如果诚信互助小组编码数据为空，跳过此测试用例
            log.warn("诚信互助小组编码数据为空，跳过场景8测试用例");
        } else {
            testCases.add(new TestCaseConfig("按档位扩展投放", "档位+诚信互助小组", null,
                    BigDecimal.valueOf(25000), 2, groupRegions));
        }

        // 场景9：按档位扩展投放 + 新增双扩展类型（区县+商圈类型）
        List<String> countyDistrictRegions = calculateCartesianProduct(
                getRegionsForSingleExtension("区县"),
                getRegionsForSingleExtension("商圈类型"));
        testCases.add(new TestCaseConfig("按档位扩展投放", "档位+区县+商圈类型", null,
                BigDecimal.valueOf(60000), 5, countyDistrictRegions));

        // 场景10：按价位段自选投放 + 大投放量（固定全市，无扩展类型）
        testCases.add(new TestCaseConfig("按价位段自选投放", null, "优质数据共享客户",
                BigDecimal.valueOf(80000), 0, Collections.emptyList()));  // 0表示"全市"
    }

    /**
     * 基础组合
     */
    private static class BaseCombination {
        final String deliveryMethod;
        final String deliveryEtype;
        final String tag;

        BaseCombination(String deliveryMethod, String deliveryEtype, String tag) {
            this.deliveryMethod = deliveryMethod;
            this.deliveryEtype = deliveryEtype;
            this.tag = tag;
        }
    }

    /**
     * 统计用例数量（用于验证）
     */
    public static void printStatistics(int maxRegions) {
        TestCaseGenerator generator = new TestCaseGenerator();
        generator.loadDefaultRegions();
        List<TestCaseConfig> cases = generator.generateAllTestCasesInternal(maxRegions, 42);
        System.out.println("=== 测试用例统计 ===");
        System.out.println("总用例数: " + cases.size());

        Map<String, Long> byMethod = cases.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        c -> c.deliveryMethod, java.util.stream.Collectors.counting()));
        System.out.println("按投放方式分布: " + byMethod);

        Map<String, Long> byAdvRange = cases.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        c -> getAdvRange(c.adv), java.util.stream.Collectors.counting()));
        System.out.println("按投放量范围分布: " + byAdvRange);

        Map<Integer, Long> byRegionCount = cases.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        c -> c.regionCount, java.util.stream.Collectors.counting()));
        System.out.println("按区域数量分布: " + byRegionCount);
    }

    private static String getAdvRange(BigDecimal adv) {
        double v = adv.doubleValue();
        if (v < 1000) return "0-1K";
        if (v < 5000) return "1K-5K";
        if (v < 20000) return "5K-20K";
        if (v < 100000) return "20K-100K";
        return "100K-150K";
    }
}

