package org.example.shared.util;

/**
 * 备注管理工具类
 * <p>
 * 提供统一的备注追加逻辑，确保所有备注操作都使用相同的分隔符和格式。
 * </p>
 *
 * @author Robin
 * @since 2025-12-23
 */
public class RemarkHelper {

    /** 备注分隔符：中文分号 */
    private static final String SEPARATOR = "；";

    /**
     * 追加备注信息。
     * <p>
     * 规则：
     * - 如果原始备注为空，直接返回新增内容
     * - 如果原始备注不为空，使用中文分号连接：{原始备注}；{新增内容}
     * </p>
     *
     * @param originalRemark 原始备注，可能为 null 或空字符串
     * @param addition       新增内容，不能为 null
     * @return 追加后的备注
     * @example
     * <pre>{@code
     * String result1 = RemarkHelper.appendRemark(null, "人工新增");
     * // result1: "人工新增"
     * 
     * String result2 = RemarkHelper.appendRemark("", "人工新增");
     * // result2: "人工新增"
     * 
     * String result3 = RemarkHelper.appendRemark("原始备注", "人工新增");
     * // result3: "原始备注；人工新增"
     * }</pre>
     */
    public static String appendRemark(String originalRemark, String addition) {
        if (addition == null) {
            throw new IllegalArgumentException("新增备注内容不能为 null");
        }
        
        if (originalRemark == null || originalRemark.trim().isEmpty()) {
            return addition;
        }
        
        return originalRemark + SEPARATOR + addition;
    }

    /**
     * 构建删除区域的备注信息。
     *
     * @param originalRemark 原始备注
     * @param deliveryArea   被删除的区域名称
     * @return 追加后的备注
     */
    public static String buildDeleteRegionRemark(String originalRemark, String deliveryArea) {
        return appendRemark(originalRemark, "人工删除区域：" + deliveryArea);
    }

    /**
     * 构建删除卷烟的备注信息。
     *
     * @param originalRemark 原始备注
     * @return 追加后的备注
     */
    public static String buildDeleteCigaretteRemark(String originalRemark) {
        return appendRemark(originalRemark, "人工已确认删除该卷烟");
    }

    /**
     * 构建新增区域的备注信息。
     *
     * @param originalRemark 原始备注（用户传入的备注）
     * @param deliveryArea   新增的区域名称
     * @return 追加后的备注
     */
    public static String buildAddRegionRemark(String originalRemark, String deliveryArea) {
        return appendRemark(originalRemark, "人工新增区域：" + deliveryArea);
    }

    /**
     * 构建 HG/LG 变更的备注信息。
     *
     * @param originalRemark 原始备注
     * @param infoHg         Info 表中的 HG
     * @param infoLg         Info 表中的 LG
     * @param newHg          用户设置的新 HG
     * @param newLg          用户设置的新 LG
     * @return 追加后的备注
     */
    public static String buildHgLgChangeRemark(String originalRemark, String infoHg, String infoLg, 
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

        return appendRemark(originalRemark, changeInfo.toString());
    }
}
