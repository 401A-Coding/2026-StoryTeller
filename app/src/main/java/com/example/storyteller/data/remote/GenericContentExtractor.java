package com.example.storyteller.data.remote;

import android.util.Log;

import com.example.storyteller.model.NovelSummary;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 通用网页内容提取器
 * 支持从任意URL提取标题、描述等基本信息
 */
public class GenericContentExtractor {

    private static final String TAG = "GenericContentExtractor";
    private static final int TIMEOUT = 10000;

    public interface ExtractCallback {
        void onSuccess(NovelSummary summary);
        void onFailure(Exception e);
    }

    /**
     * 从任意URL提取基本信息
     */
    public void extract(String url, ExtractCallback callback) {
        new Thread(() -> {
            try {
                Log.d(TAG, "开始提取: " + url);
                
                Document doc = Jsoup.connect(url)
                        .timeout(TIMEOUT)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                        .header("Accept", "text/html,application/xhtml+xml")
                        .get();

                NovelSummary summary = new NovelSummary();
                summary.setSourceUrl(url);
                
                // 提取标题
                String title = extractTitle(doc);
                summary.setTitle(title != null ? title : "未知标题");
                
                // 提取描述
                String description = extractDescription(doc);
                summary.setDescription(description != null ? description : "无简介");
                
                // 提取作者（如果存在）
                String author = extractAuthor(doc);
                summary.setAuthor(author != null ? author : "未知作者");
                
                // 提取标签
                List<String> tags = extractTags(doc);
                summary.setTags(tags);
                
                Log.d(TAG, "提取完成: " + summary.getTitle());
                callback.onSuccess(summary);

            } catch (IOException e) {
                Log.e(TAG, "提取失败: " + e.getMessage(), e);
                callback.onFailure(e);
            }
        }).start();
    }

    /**
     * 提取标题（优先级：og:title > meta title > h1 > title标签）
     */
    private String extractTitle(Document doc) {
        // 尝试Open Graph标题
        Element ogTitle = doc.selectFirst("meta[property=og:title]");
        if (ogTitle != null && !ogTitle.attr("content").isEmpty()) {
            return ogTitle.attr("content");
        }
        
        // 尝试meta title
        Element metaTitle = doc.selectFirst("meta[name=title]");
        if (metaTitle != null && !metaTitle.attr("content").isEmpty()) {
            return metaTitle.attr("content");
        }
        
        // 尝试h1标签
        Element h1 = doc.selectFirst("h1");
        if (h1 != null && !h1.text().isEmpty()) {
            return h1.text();
        }
        
        // 最后使用title标签
        String title = doc.title();
        return title != null && !title.isEmpty() ? title : null;
    }

    /**
     * 提取描述（优先级：og:description > meta description > first paragraph）
     */
    private String extractDescription(Document doc) {
        // 尝试Open Graph描述
        Element ogDesc = doc.selectFirst("meta[property=og:description]");
        if (ogDesc != null && !ogDesc.attr("content").isEmpty()) {
            return ogDesc.attr("content");
        }
        
        // 尝试meta description
        Element metaDesc = doc.selectFirst("meta[name=description]");
        if (metaDesc != null && !metaDesc.attr("content").isEmpty()) {
            return metaDesc.attr("content");
        }
        
        // 尝试第一段文字
        Element firstP = doc.selectFirst("p");
        if (firstP != null && !firstP.text().isEmpty()) {
            String text = firstP.text();
            return text.length() > 500 ? text.substring(0, 500) : text;
        }
        
        return null;
    }

    /**
     * 提取作者（尝试多种常见模式）
     */
    private String extractAuthor(Document doc) {
        // 尝试Open Graph作者
        Element ogAuthor = doc.selectFirst("meta[property=article:author]");
        if (ogAuthor != null && !ogAuthor.attr("content").isEmpty()) {
            return ogAuthor.attr("content");
        }
        
        // 尝试meta author
        Element metaAuthor = doc.selectFirst("meta[name=author]");
        if (metaAuthor != null && !metaAuthor.attr("content").isEmpty()) {
            return metaAuthor.attr("content");
        }
        
        return null;
    }

    /**
     * 提取标签（从keywords meta或页面标签元素）
     */
    private List<String> extractTags(Document doc) {
        List<String> tags = new ArrayList<>();
        
        // 尝试meta keywords
        Element metaKeywords = doc.selectFirst("meta[name=keywords]");
        if (metaKeywords != null && !metaKeywords.attr("content").isEmpty()) {
            String[] keywords = metaKeywords.attr("content").split("[,，]");
            for (String keyword : keywords) {
                String trimmed = keyword.trim();
                if (!trimmed.isEmpty()) {
                    tags.add(trimmed);
                }
            }
        }
        
        return tags;
    }
}
