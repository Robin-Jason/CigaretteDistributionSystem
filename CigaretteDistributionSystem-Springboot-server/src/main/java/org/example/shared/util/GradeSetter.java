package org.example.shared.util;

import org.example.infrastructure.persistence.po.CigaretteDistributionPredictionPO;

import java.math.BigDecimal;

/**
 * 档位批量设置工具类。
 * <p>
 * 用于将 BigDecimal[30] 数组批量设置到 PO 对象的 D30~D1 字段。
 * </p>
 *
 * @author Robin
 * @since 2025-12-22
 */
public final class GradeSetter {

    private GradeSetter() {
    }

    /**
     * 将档位数组批量设置到 PO 对象。
     *
     * @param po     目标 PO 对象
     * @param grades 档位数组，索引0对应D30，索引29对应D1
     *
     * @example
     * <pre>{@code
     * CigaretteDistributionPredictionPO po = new CigaretteDistributionPredictionPO();
     * BigDecimal[] grades = GradeExtractor.extractFromMap(row);
     * GradeSetter.setGrades(po, grades);
     * }</pre>
     */
    public static void setGrades(CigaretteDistributionPredictionPO po, BigDecimal[] grades) {
        if (po == null || grades == null) {
            return;
        }
        po.setD30(safeGet(grades, 0));
        po.setD29(safeGet(grades, 1));
        po.setD28(safeGet(grades, 2));
        po.setD27(safeGet(grades, 3));
        po.setD26(safeGet(grades, 4));
        po.setD25(safeGet(grades, 5));
        po.setD24(safeGet(grades, 6));
        po.setD23(safeGet(grades, 7));
        po.setD22(safeGet(grades, 8));
        po.setD21(safeGet(grades, 9));
        po.setD20(safeGet(grades, 10));
        po.setD19(safeGet(grades, 11));
        po.setD18(safeGet(grades, 12));
        po.setD17(safeGet(grades, 13));
        po.setD16(safeGet(grades, 14));
        po.setD15(safeGet(grades, 15));
        po.setD14(safeGet(grades, 16));
        po.setD13(safeGet(grades, 17));
        po.setD12(safeGet(grades, 18));
        po.setD11(safeGet(grades, 19));
        po.setD10(safeGet(grades, 20));
        po.setD9(safeGet(grades, 21));
        po.setD8(safeGet(grades, 22));
        po.setD7(safeGet(grades, 23));
        po.setD6(safeGet(grades, 24));
        po.setD5(safeGet(grades, 25));
        po.setD4(safeGet(grades, 26));
        po.setD3(safeGet(grades, 27));
        po.setD2(safeGet(grades, 28));
        po.setD1(safeGet(grades, 29));
    }

    private static BigDecimal safeGet(BigDecimal[] arr, int index) {
        return (arr != null && index >= 0 && index < arr.length) ? arr[index] : null;
    }
}
