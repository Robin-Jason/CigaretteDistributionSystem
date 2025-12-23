package org.example.shared.util;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 分配矩阵工具类。
 * <p>
 * 提供分配矩阵的通用操作方法，包括：
 * <ul>
 *   <li>档位范围裁剪与扩展</li>
 *   <li>null 值归零</li>
 *   <li>矩阵验证</li>
 * </ul>
 * </p>
 *
 * @author Robin
 * @since 2025-12-22
 */
public final class AllocationMatrixUtils {

    private AllocationMatrixUtils() {
    }

    /**
     * 将数组中的 null 值归零。
     *
     * @param row 档位数组（会被原地修改）
     */
    public static void normalizeNulls(BigDecimal[] row) {
        if (row == null) {
            return;
        }
        for (int i = 0; i < row.length; i++) {
            if (row[i] == null) {
                row[i] = BigDecimal.ZERO;
            }
        }
    }

    /**
     * 计算矩阵总投放量。
     *
     * @param allocationMatrix 分配矩阵
     * @param customerMatrix   客户矩阵
     * @return 总投放量 = Σ(分配值 × 客户数)
     */
    public static BigDecimal calculateTotalAmount(BigDecimal[][] allocationMatrix, BigDecimal[][] customerMatrix) {
        if (allocationMatrix == null || customerMatrix == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = BigDecimal.ZERO;
        int rowCount = Math.min(allocationMatrix.length, customerMatrix.length);
        for (int r = 0; r < rowCount; r++) {
            BigDecimal[] allocRow = allocationMatrix[r];
            BigDecimal[] custRow = customerMatrix[r];
            if (allocRow == null || custRow == null) {
                continue;
            }
            int colCount = Math.min(allocRow.length, custRow.length);
            for (int c = 0; c < colCount; c++) {
                BigDecimal a = allocRow[c];
                BigDecimal k = custRow[c];
                if (a != null && k != null) {
                    total = total.add(a.multiply(k));
                }
            }
        }
        return total;
    }

    /**
     * 将多行矩阵按列求和，得到单行汇总结果。
     * <p>
     * 常用于将多区域客户矩阵汇总为"全市"单行矩阵。
     * </p>
     *
     * @param matrix 多行矩阵（区域 x 档位），每行代表一个区域的30个档位客户数
     * @return 按列求和后的单行数组，长度与输入矩阵列数一致；如果输入为空，返回长度为30的全0数组
     *
     * @example
     * <pre>{@code
     * // 3个区域的客户矩阵
     * BigDecimal[][] matrix = {
     *     {new BigDecimal("100"), new BigDecimal("200"), new BigDecimal("300")},  // 区域1
     *     {new BigDecimal("150"), new BigDecimal("250"), new BigDecimal("350")},  // 区域2
     *     {new BigDecimal("50"),  new BigDecimal("100"), new BigDecimal("150")}   // 区域3
     * };
     * 
     * BigDecimal[] sum = AllocationMatrixUtils.sumColumns(matrix);
     * // sum = [300, 550, 800]
     * // 即 D30=100+150+50=300, D29=200+250+100=550, D28=300+350+150=800
     * }</pre>
     */
    public static BigDecimal[] sumColumns(BigDecimal[][] matrix) {
        if (matrix == null || matrix.length == 0) {
            return new BigDecimal[30];
        }

        int colCount = matrix[0] != null ? matrix[0].length : 30;
        BigDecimal[] result = new BigDecimal[colCount];
        Arrays.fill(result, BigDecimal.ZERO);

        for (BigDecimal[] row : matrix) {
            if (row == null) {
                continue;
            }
            for (int i = 0; i < colCount && i < row.length; i++) {
                BigDecimal val = row[i] != null ? row[i] : BigDecimal.ZERO;
                result[i] = result[i].add(val);
            }
        }

        return result;
    }

    /**
     * 验证客户矩阵中不存在全为0的行。
     * <p>
     * 如果发现某一行（区域）的所有档位客户数全为0，返回错误信息。
     * 这是分配算法的前置条件，因为全为0的区域无法进行有效分配。
     * </p>
     *
     * @param regions        区域列表（与矩阵行一一对应）
     * @param customerMatrix 客户矩阵（区域 x 档位）
     * @return 如果验证通过返回 null；如果发现全为0的行，返回错误信息
     */
    public static String validateNoZeroRows(List<String> regions, BigDecimal[][] customerMatrix) {
        if (customerMatrix == null || customerMatrix.length == 0) {
            return "客户矩阵为空，无法进行分配";
        }

        if (regions == null || regions.size() != customerMatrix.length) {
            return String.format("区域列表与客户矩阵行数不匹配: regions=%d, matrix=%d",
                    regions != null ? regions.size() : 0, customerMatrix.length);
        }

        List<String> zeroRowRegions = new ArrayList<>();
        for (int r = 0; r < customerMatrix.length; r++) {
            BigDecimal[] row = customerMatrix[r];
            if (row == null || row.length == 0) {
                zeroRowRegions.add(regions.get(r));
                continue;
            }

            boolean allZero = true;
            for (BigDecimal value : row) {
                if (value != null && value.compareTo(BigDecimal.ZERO) > 0) {
                    allZero = false;
                    break;
                }
            }

            if (allZero) {
                zeroRowRegions.add(regions.get(r));
            }
        }

        if (!zeroRowRegions.isEmpty()) {
            return String.format("以下区域在客户矩阵中30个档位客户数全为0，无法进行分配: %s。请确保这些区域在 region_customer_statistics 表中有有效的客户数据。",
                    String.join(", ", zeroRowRegions));
        }

        return null;
    }

    /**
     * 验证客户矩阵在指定档位范围内不存在全为0的行。
     * <p>
     * 只检查 GradeRange 指定范围内的列，如果某区域在该范围内所有档位客户数全为0，返回错误信息。
     * 这是分配算法的前置条件，因为范围内全为0的区域无法进行有效分配。
     * </p>
     *
     * @param regions        区域列表（与矩阵行一一对应）
     * @param customerMatrix 客户矩阵（区域 x 档位）
     * @param gradeRange     档位范围，指定需要检查的列范围
     * @return 如果验证通过返回 null；如果发现范围内全为0的行，返回错误信息
     */
    public static String validateNoZeroRowsInRange(List<String> regions, 
                                                    BigDecimal[][] customerMatrix, 
                                                    org.example.domain.model.valueobject.GradeRange gradeRange) {
        if (customerMatrix == null || customerMatrix.length == 0) {
            return "客户矩阵为空，无法进行分配";
        }

        if (regions == null || regions.size() != customerMatrix.length) {
            return String.format("区域列表与客户矩阵行数不匹配: regions=%d, matrix=%d",
                    regions != null ? regions.size() : 0, customerMatrix.length);
        }

        if (gradeRange == null) {
            return validateNoZeroRows(regions, customerMatrix);
        }

        int maxIndex = gradeRange.getMaxIndex();
        int minIndex = gradeRange.getMinIndex();

        List<String> zeroRowRegions = new ArrayList<>();
        for (int r = 0; r < customerMatrix.length; r++) {
            BigDecimal[] row = customerMatrix[r];
            if (row == null || row.length == 0) {
                zeroRowRegions.add(regions.get(r));
                continue;
            }

            boolean allZero = true;
            // 只检查 maxIndex ~ minIndex 范围内的列
            for (int c = maxIndex; c <= minIndex && c < row.length; c++) {
                BigDecimal value = row[c];
                if (value != null && value.compareTo(BigDecimal.ZERO) > 0) {
                    allZero = false;
                    break;
                }
            }

            if (allZero) {
                zeroRowRegions.add(regions.get(r));
            }
        }

        if (!zeroRowRegions.isEmpty()) {
            return String.format("以下区域在档位范围 [%s-%s] 内客户数全为0，无法进行分配: %s。请确保这些区域在 region_customer_statistics 表中有有效的客户数据。",
                    gradeRange.getMaxGrade(), gradeRange.getMinGrade(), String.join(", ", zeroRowRegions));
        }

        return null;
    }
}
