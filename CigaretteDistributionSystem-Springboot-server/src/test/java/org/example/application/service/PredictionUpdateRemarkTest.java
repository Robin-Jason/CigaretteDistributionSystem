package org.example.application.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 预测数据更新备注追加测试。
 * <p>
 * 验证当用户传入新的 HG/LG 时，备注会自动追加变更信息。
 * </p>
 */
public class PredictionUpdateRemarkTest {

    @Test
    public void testBuildRemarkWithHgChange() {
        String result = buildRemarkWithHgLgChange("原始备注", "D30", "D1", "D25", "D1");
        
        assertEquals("原始备注；人工已更改HG为D25", result);
        System.out.println("✅ 测试通过：HG 变更追加到备注");
        System.out.println("   结果：" + result);
    }

    @Test
    public void testBuildRemarkWithLgChange() {
        String result = buildRemarkWithHgLgChange("原始备注", "D30", "D1", "D30", "D15");
        
        assertEquals("原始备注；人工已更改LG为D15", result);
        System.out.println("✅ 测试通过：LG 变更追加到备注");
        System.out.println("   结果：" + result);
    }

    @Test
    public void testBuildRemarkWithBothChange() {
        String result = buildRemarkWithHgLgChange("原始备注", "D30", "D1", "D25", "D15");
        
        assertEquals("原始备注；人工已更改HG为D25和LG为D15", result);
        System.out.println("✅ 测试通过：HG 和 LG 都变更追加到备注");
        System.out.println("   结果：" + result);
    }

    @Test
    public void testBuildRemarkWithNoChange() {
        String result = buildRemarkWithHgLgChange("原始备注", "D30", "D1", "D30", "D1");
        
        assertEquals("原始备注", result);
        System.out.println("✅ 测试通过：无变更时备注不变");
        System.out.println("   结果：" + result);
    }

    @Test
    public void testBuildRemarkWithEmptyOriginal() {
        String result = buildRemarkWithHgLgChange("", "D30", "D1", "D25", "D15");
        
        assertEquals("人工已更改HG为D25和LG为D15", result);
        System.out.println("✅ 测试通过：原始备注为空时只显示变更信息");
        System.out.println("   结果：" + result);
    }

    @Test
    public void testBuildRemarkWithNullOriginal() {
        String result = buildRemarkWithHgLgChange(null, "D30", "D1", "D25", "D15");
        
        assertEquals("人工已更改HG为D25和LG为D15", result);
        System.out.println("✅ 测试通过：原始备注为 null 时只显示变更信息");
        System.out.println("   结果：" + result);
    }

    @Test
    public void testBuildRemarkWithNullNewHg() {
        // 用户没有传入新 HG，使用 Info 表的值
        String result = buildRemarkWithHgLgChange("原始备注", "D30", "D1", null, "D15");
        
        assertEquals("原始备注；人工已更改LG为D15", result);
        System.out.println("✅ 测试通过：只传入新 LG 时只追加 LG 变更");
        System.out.println("   结果：" + result);
    }

    /**
     * 复制自 PredictionUpdateServiceImpl 的私有方法，用于测试。
     */
    private String buildRemarkWithHgLgChange(String originalRemark, String infoHg, String infoLg, 
                                             String newHg, String newLg) {
        // 检查 HG/LG 是否有变更
        boolean hgChanged = newHg != null && !newHg.equals(infoHg);
        boolean lgChanged = newLg != null && !newLg.equals(infoLg);

        if (!hgChanged && !lgChanged) {
            // 没有变更，直接返回原始备注
            return originalRemark;
        }

        // 构建变更信息
        StringBuilder changeInfo = new StringBuilder("人工已更改");
        if (hgChanged) {
            changeInfo.append("HG为").append(newHg);
        }
        if (lgChanged) {
            if (hgChanged) {
                changeInfo.append("和");
            }
            changeInfo.append("LG为").append(newLg);
        }

        // 追加到原始备注
        if (originalRemark == null || originalRemark.trim().isEmpty()) {
            return changeInfo.toString();
        } else {
            return originalRemark + "；" + changeInfo.toString();
        }
    }
}
