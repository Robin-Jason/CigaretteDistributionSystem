package org.example.shared.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RemarkHelper 工具类测试
 *
 * @author Robin
 * @since 2025-12-23
 */
class RemarkHelperTest {

    @Test
    void testAppendRemark_WithNullOriginal() {
        String result = RemarkHelper.appendRemark(null, "新增内容");
        assertEquals("新增内容", result);
        System.out.println("✅ 测试通过：原始备注为 null 时直接返回新增内容");
        System.out.println("   结果：" + result);
    }

    @Test
    void testAppendRemark_WithEmptyOriginal() {
        String result = RemarkHelper.appendRemark("", "新增内容");
        assertEquals("新增内容", result);
        System.out.println("✅ 测试通过：原始备注为空字符串时直接返回新增内容");
        System.out.println("   结果：" + result);
    }

    @Test
    void testAppendRemark_WithWhitespaceOriginal() {
        String result = RemarkHelper.appendRemark("   ", "新增内容");
        assertEquals("新增内容", result);
        System.out.println("✅ 测试通过：原始备注为空白字符时直接返回新增内容");
        System.out.println("   结果：" + result);
    }

    @Test
    void testAppendRemark_WithValidOriginal() {
        String result = RemarkHelper.appendRemark("原始备注", "新增内容");
        assertEquals("原始备注；新增内容", result);
        System.out.println("✅ 测试通过：原始备注不为空时使用中文分号连接");
        System.out.println("   结果：" + result);
    }

    @Test
    void testAppendRemark_NullAddition() {
        assertThrows(IllegalArgumentException.class, () -> {
            RemarkHelper.appendRemark("原始备注", null);
        });
        System.out.println("✅ 测试通过：新增内容为 null 时抛出异常");
    }

    @Test
    void testBuildDeleteRegionRemark() {
        String result = RemarkHelper.buildDeleteRegionRemark("原始备注", "昆明市");
        assertEquals("原始备注；人工删除区域：昆明市", result);
        System.out.println("✅ 测试通过：删除区域备注格式正确");
        System.out.println("   结果：" + result);
    }

    @Test
    void testBuildDeleteRegionRemark_NullOriginal() {
        String result = RemarkHelper.buildDeleteRegionRemark(null, "昆明市");
        assertEquals("人工删除区域：昆明市", result);
        System.out.println("✅ 测试通过：删除区域备注（原始备注为 null）");
        System.out.println("   结果：" + result);
    }

    @Test
    void testBuildDeleteCigaretteRemark() {
        String result = RemarkHelper.buildDeleteCigaretteRemark("原始备注");
        assertEquals("原始备注；人工已确认删除该卷烟", result);
        System.out.println("✅ 测试通过：删除卷烟备注格式正确");
        System.out.println("   结果：" + result);
    }

    @Test
    void testBuildDeleteCigaretteRemark_NullOriginal() {
        String result = RemarkHelper.buildDeleteCigaretteRemark(null);
        assertEquals("人工已确认删除该卷烟", result);
        System.out.println("✅ 测试通过：删除卷烟备注（原始备注为 null）");
        System.out.println("   结果：" + result);
    }

    @Test
    void testBuildAddRegionRemark() {
        String result = RemarkHelper.buildAddRegionRemark("用户备注", "昆明市");
        assertEquals("用户备注；人工新增区域：昆明市", result);
        System.out.println("✅ 测试通过：新增区域备注格式正确");
        System.out.println("   结果：" + result);
    }

    @Test
    void testBuildAddRegionRemark_NullOriginal() {
        String result = RemarkHelper.buildAddRegionRemark(null, "昆明市");
        assertEquals("人工新增区域：昆明市", result);
        System.out.println("✅ 测试通过：新增区域备注（原始备注为 null）");
        System.out.println("   结果：" + result);
    }

    @Test
    void testBuildHgLgChangeRemark_BothChanged() {
        String result = RemarkHelper.buildHgLgChangeRemark("原始备注", "D30", "D1", "D25", "D15");
        assertEquals("原始备注；人工已更改HG为D25和LG为D15", result);
        System.out.println("✅ 测试通过：HG 和 LG 都变更时备注格式正确");
        System.out.println("   结果：" + result);
    }

    @Test
    void testBuildHgLgChangeRemark_OnlyHgChanged() {
        String result = RemarkHelper.buildHgLgChangeRemark("原始备注", "D30", "D1", "D25", "D1");
        assertEquals("原始备注；人工已更改HG为D25", result);
        System.out.println("✅ 测试通过：只有 HG 变更时备注格式正确");
        System.out.println("   结果：" + result);
    }

    @Test
    void testBuildHgLgChangeRemark_OnlyLgChanged() {
        String result = RemarkHelper.buildHgLgChangeRemark("原始备注", "D30", "D1", "D30", "D15");
        assertEquals("原始备注；人工已更改LG为D15", result);
        System.out.println("✅ 测试通过：只有 LG 变更时备注格式正确");
        System.out.println("   结果：" + result);
    }

    @Test
    void testBuildHgLgChangeRemark_NoChange() {
        String result = RemarkHelper.buildHgLgChangeRemark("原始备注", "D30", "D1", "D30", "D1");
        assertEquals("原始备注", result);
        System.out.println("✅ 测试通过：HG 和 LG 都未变更时备注不变");
        System.out.println("   结果：" + result);
    }

    @Test
    void testBuildHgLgChangeRemark_NullOriginal() {
        String result = RemarkHelper.buildHgLgChangeRemark(null, "D30", "D1", "D25", "D15");
        assertEquals("人工已更改HG为D25和LG为D15", result);
        System.out.println("✅ 测试通过：HG/LG 变更（原始备注为 null）");
        System.out.println("   结果：" + result);
    }

    @Test
    void testMultipleAppends() {
        String remark = null;
        remark = RemarkHelper.buildAddRegionRemark(remark, "昆明市");
        remark = RemarkHelper.buildHgLgChangeRemark(remark, "D30", "D1", "D25", "D15");
        remark = RemarkHelper.appendRemark(remark, "其他备注");
        
        assertEquals("人工新增区域：昆明市；人工已更改HG为D25和LG为D15；其他备注", remark);
        System.out.println("✅ 测试通过：多次追加备注格式正确");
        System.out.println("   结果：" + remark);
    }
}
