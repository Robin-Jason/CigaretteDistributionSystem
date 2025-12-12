package org.example.util;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class KmpMatcher {

    private static final Pattern NORMALIZE_PATTERN = Pattern.compile("[\\s()（）\\+]+");
    
    /**
     * 使用KMP算法匹配字符串
     * @param text 母字符串
     * @param patterns 模式字符串列表
     * @return 匹配成功的模式字符串列表
     */
    public List<String> matchPatterns(String text, List<String> patterns) {
        List<String> matchedPatterns = new ArrayList<>();

        if (text == null || patterns == null || patterns.isEmpty()) {
            return matchedPatterns;
        }

        String normalizedText = normalize(text);
        if (normalizedText.isEmpty()) {
            return matchedPatterns;
        }

        for (String candidate : patterns) {
            if (candidate == null) {
                continue;
            }
            String normalizedCandidate = normalize(candidate);
            if (normalizedCandidate.isEmpty()) {
                continue;
            }
            // 仅当字符串去除括号、加号、空白后完全相同时才认为匹配
            if (normalizedCandidate.equals(normalizedText)) {
                matchedPatterns.add(candidate);
                continue;
            }
            if (normalizedCandidate.length() == normalizedText.length() &&
                kmpSearch(normalizedText, normalizedCandidate)) {
                matchedPatterns.add(candidate);
            }
        }

        return matchedPatterns;
    }
    
    /**
     * 规范化字符串，移除括号、加号和空白字符
     */
    private String normalize(String input) {
        if (input == null) {
            return "";
        }
        String trimmed = input.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        return NORMALIZE_PATTERN.matcher(trimmed).replaceAll("");
    }
    
    /**
     * KMP搜索算法
     * @param text 文本字符串
     * @param pattern 模式字符串
     * @return 是否匹配成功
     */
    private boolean kmpSearch(String text, String pattern) {
        if (text == null || pattern == null || pattern.isEmpty()) {
            return false;
        }
        if (text.length() != pattern.length()) {
            return false;
        }
        
        int[] lps = computeLPSArray(pattern);
        int i = 0; // text的索引
        int j = 0; // pattern的索引
        
        while (i < text.length()) {
            if (pattern.charAt(j) == text.charAt(i)) {
                i++;
                j++;
            }
            
            if (j == pattern.length()) {
                return true; // 找到匹配
            } else if (i < text.length() && pattern.charAt(j) != text.charAt(i)) {
                if (j != 0) {
                    j = lps[j - 1];
                } else {
                    i++;
                }
            }
        }
        
        return false;
    }
    
    /**
     * 计算LPS（Longest Proper Prefix which is also Suffix）数组
     * @param pattern 模式字符串
     * @return LPS数组
     */
    private int[] computeLPSArray(String pattern) {
        int[] lps = new int[pattern.length()];
        int len = 0;
        int i = 1;
        
        lps[0] = 0; // lps[0]总是0
        
        while (i < pattern.length()) {
            if (pattern.charAt(i) == pattern.charAt(len)) {
                len++;
                lps[i] = len;
                i++;
            } else {
                if (len != 0) {
                    len = lps[len - 1];
                } else {
                    lps[i] = 0;
                    i++;
                }
            }
        }
        
        return lps;
    }
}
