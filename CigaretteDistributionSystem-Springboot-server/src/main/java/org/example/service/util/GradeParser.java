package org.example.service.util;

import org.springframework.util.StringUtils;

import java.util.Locale;

/**
 * 档位解析工具类
 * <p>
 * 提供档位字符串解析功能，支持多种格式（D30、第30档、中文数字等）。
 * 主要用于将档位字符串转换为数组索引，以及从档位字符串中提取档位编号。
 * </p>
 *
 * @author Robin
 * @version 1.0
 * @since 2025-12-11
 */
public final class GradeParser {

    private GradeParser() {
        // 工具类，禁止实例化
    }

    /**
     * 将档位字符串转换为数组索引。
     * <p>
     * 档位编号（1-30）转换为数组索引（29-0），即D30对应索引0，D1对应索引29。
     * </p>
     *
     * @param grade 档位字符串（如："D30"、"D1"、"第30档"）
     * @return 数组索引（0-29），如果档位无效则返回 -1
     * @example
     * <pre>
     *     int index = GradeParser.parseGradeToIndex("D30");
     *     // 返回: 0（D30对应索引0）
     *     int index2 = GradeParser.parseGradeToIndex("D1");
     *     // 返回: 29（D1对应索引29）
     *     int index3 = GradeParser.parseGradeToIndex("D31");
     *     // 返回: -1（无效档位）
     * </pre>
     */
    public static int parseGradeToIndex(String grade) {
        int gradeNumber = extractGradeNumber(grade);
        if (gradeNumber < 1 || gradeNumber > 30) {
            return -1;
        }
        return 30 - gradeNumber;
    }

    /**
     * 从档位字符串中提取档位编号。
     * <p>
     * 支持多种格式：D30、D1、第30档、30、三十等。
     * </p>
     *
     * @param grade 档位字符串（如："D30"、"第30档"、"三十"）
     * @return 档位编号（1-30），如果无法解析则返回 -1
     * @example
     * <pre>
     *     int num = GradeParser.extractGradeNumber("D30");
     *     // 返回: 30
     *     int num2 = GradeParser.extractGradeNumber("第30档");
     *     // 返回: 30
     *     int num3 = GradeParser.extractGradeNumber("三十");
     *     // 返回: 30（支持中文数字）
     *     int num4 = GradeParser.extractGradeNumber("无效");
     *     // 返回: -1
     * </pre>
     */
    public static int extractGradeNumber(String grade) {
        if (!StringUtils.hasText(grade)) {
            return -1;
        }
        String trimmed = grade.trim();
        String upper = trimmed.toUpperCase(Locale.ROOT);

        if (upper.startsWith("D")) {
            String numeric = upper.substring(1).replaceAll("[^0-9]", "");
            if (!numeric.isEmpty()) {
                try {
                    return Integer.parseInt(numeric);
                } catch (NumberFormatException ignored) {
                }
            }
        }

        String digitsOnly = trimmed.replaceAll("[^0-9]", "");
        if (!digitsOnly.isEmpty()) {
            try {
                return Integer.parseInt(digitsOnly);
            } catch (NumberFormatException ignored) {
            }
        }

        String chinesePart = trimmed.replaceAll("[^一二三四五六七八九十零〇两壹贰叁肆伍陆柒捌玖拾]", "");
        if (!chinesePart.isEmpty()) {
            return parseChineseNumber(chinesePart);
        }

        return -1;
    }

    /**
     * 解析中文数字字符串。
     * <p>
     * 将中文数字（如："三十"、"二十五"）转换为阿拉伯数字。
     * 支持"零"、"一"到"九"、"十"等字符。
     * </p>
     *
     * @param raw 原始中文数字字符串（如："第三十档"、"二十五"）
     * @return 阿拉伯数字（如：30、25），如果无法解析则返回 -1
     * @example
     * <pre>
     *     int num = GradeParser.parseChineseNumber("第三十档");
     *     // 返回: 30
     *     int num2 = GradeParser.parseChineseNumber("二十五");
     *     // 返回: 25
     *     int num3 = GradeParser.parseChineseNumber("十");
     *     // 返回: 10
     * </pre>
     */
    public static int parseChineseNumber(String raw) {
        String normalized = raw.replace("第", "")
                .replace("档", "")
                .replace("级", "")
                .replace("等", "")
                .replace("位", "")
                .replace("层", "")
                .replace("拾", "十")
                .trim();
        if (!StringUtils.hasText(normalized)) {
            return -1;
        }

        int result = 0;
        int temp = 0;
        for (int i = 0; i < normalized.length(); i++) {
            int num = chineseDigitToNumber(normalized.charAt(i));
            if (num == -1) {
                continue;
            }
            if (num == 10) {
                temp = temp == 0 ? 1 : temp;
                result += temp * 10;
                temp = 0;
            } else {
                temp = num;
            }
        }
        result += temp;
        return result;
    }

    /**
     * 将单个中文字符转换为数字。
     * <p>
     * 支持多种中文数字表示方式（简体、繁体、大写等）。
     * </p>
     *
     * @param ch 中文字符（如：'一'、'壹'、'十'）
     * @return 对应的数字（0-10），"十"返回10，如果无法识别则返回 -1
     * @example
     * <pre>
     *     int num = GradeParser.chineseDigitToNumber('一');
     *     // 返回: 1
     *     int num2 = GradeParser.chineseDigitToNumber('十');
     *     // 返回: 10
     *     int num3 = GradeParser.chineseDigitToNumber('零');
     *     // 返回: 0
     *     int num4 = GradeParser.chineseDigitToNumber('A');
     *     // 返回: -1（无法识别）
     * </pre>
     */
    public static int chineseDigitToNumber(char ch) {
        switch (ch) {
            case '零':
            case '〇':
                return 0;
            case '一':
            case '壹':
                return 1;
            case '二':
            case '贰':
            case '两':
                return 2;
            case '三':
            case '叁':
                return 3;
            case '四':
            case '肆':
                return 4;
            case '五':
            case '伍':
                return 5;
            case '六':
            case '陆':
                return 6;
            case '七':
            case '柒':
                return 7;
            case '八':
            case '捌':
                return 8;
            case '九':
            case '玖':
                return 9;
            case '十':
                return 10;
            default:
                return -1;
        }
    }
}

