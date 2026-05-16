package com.example.storyteller.utils;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 素材内容解析工具
 * 用于解析AI返回的结构化文本格式
 */
public class MaterialContentParser {

    /**
     * 解析AI返回的结构化内容
     * 支持多种格式：
     * 1. 【分类】xxx 【子分类】xxx 【一句话】xxx 【详细说明】xxx 【标签】xxx
     * 2. 【分类】xxx 【一句话】xxx 【详细说明】xxx 【标签】xxx (无子分类)
     * 3. 纯文本（自动提取摘要）
     *
     * @param content AI返回的原始内容
     * @return 解析结果
     */
    public static ParsedContent parse(String content) {
        if (TextUtils.isEmpty(content)) {
            return new ParsedContent("", "", "", "", new ArrayList<>());
        }

        // 尝试提取结构化标记
        String category = extractSection(content, "分类");
        String subCategory = extractSection(content, "子分类");
        String summary = extractSection(content, "一句话");
        String detail = extractSection(content, "详细说明");
        List<String> tags = extractTags(content);
        
        // 如果找到结构化标记，使用解析结果
        if (!TextUtils.isEmpty(summary) || !TextUtils.isEmpty(detail)) {
            return new ParsedContent(
                category != null ? category : "",
                subCategory != null ? subCategory : "",
                summary != null ? summary : "",
                detail != null ? detail : content,
                tags
            );
        }
        
        // 否则，将原始内容作为detail，并自动生成summary
        return generateSummaryFromContent(content);
    }

    /**
     * 提取指定标记的内容
     */
    private static String extractSection(String content, String sectionName) {
        Pattern pattern = Pattern.compile("【" + sectionName + "】\\s*([^【]*)");
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            String value = matcher.group(1).trim();
            // 移除末尾的换行符
            return value.replaceAll("[\\r\\n]+$", "");
        }
        return null;
    }

    /**
     * 提取标签列表
     */
    private static List<String> extractTags(String content) {
        List<String> tags = new ArrayList<>();
        
        Pattern pattern = Pattern.compile("【标签】\\s*([^【\\n]*)");
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            String tagText = matcher.group(1).trim();
            // 支持中文顿号和英文逗号分隔
            String[] tagArray = tagText.split("[、,，]");
            for (String tag : tagArray) {
                String trimmed = tag.trim();
                if (!trimmed.isEmpty()) {
                    tags.add(trimmed);
                }
            }
        }
        
        return tags;
    }

    /**
     * 解析结果
     */
    public static class ParsedContent {
        private final String category;
        private final String subCategory;
        private final String summary;
        private final String detail;
        private final List<String> tags;

        public ParsedContent(String category, String subCategory, String summary, String detail, List<String> tags) {
            this.category = category;
            this.subCategory = subCategory;
            this.summary = summary;
            this.detail = detail;
            this.tags = tags;
        }

        public String getCategory() {
            return category;
        }

        public String getSubCategory() {
            return subCategory;
        }

        public String getSummary() {
            return summary;
        }

        public String getDetail() {
            return detail;
        }

        public List<String> getTags() {
            return tags;
        }
    }

    /**
     * 从原始内容生成summary
     */
    private static ParsedContent generateSummaryFromContent(String content) {
        String summary;
        String detail = content;
        
        // 策略1：取第一段作为summary
        int firstNewLine = content.indexOf('\n');
        if (firstNewLine > 0 && firstNewLine < 200) {
            summary = content.substring(0, firstNewLine).trim();
        } 
        // 策略2：取前200字符
        else if (content.length() > 200) {
            // 尝试在句子边界截断
            int cutPoint = 200;
            for (int i = 200; i >= 150; i--) {
                if (i < content.length() && 
                    (content.charAt(i) == '。' || content.charAt(i) == '！' || 
                     content.charAt(i) == '？' || content.charAt(i) == '\n')) {
                    cutPoint = i + 1;
                    break;
                }
            }
            summary = content.substring(0, cutPoint).trim();
        } 
        // 策略3：整个内容较短，直接使用
        else {
            summary = content;
            detail = "";
        }
        
        return new ParsedContent("", "", summary, detail, new ArrayList<>());
    }
}
