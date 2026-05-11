package com.example.storyteller.data.remote;

import android.content.Context;
import android.util.Log;

import com.example.storyteller.data.local.db.MaterialDao;
import com.example.storyteller.model.Material;
import com.example.storyteller.model.NovelSummary;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 小说爬虫工具类
 * 用于爬取番茄小说等网站的关键信息（大纲、总结、人物画像），不存储完整正文
 */
public class NovelCrawler {

    private static final String TAG = "NovelCrawler";
    private static final int TIMEOUT = 10000; // 10秒超时

    // 回调接口
    public interface CrawlCallback {
        void onSuccess(NovelSummary summary);
        void onFailure(Exception e);
    }

    /**
     * 爬取番茄小说详情页
     * URL格式示例: https://fanqienovel.com/page/xxxxxx
     *
     * @param url      小说详情页URL
     * @param callback 回调
     */
    public void crawlNovelDetail(String url, CrawlCallback callback) {
        new Thread(() -> {
            try {
                Log.d(TAG, "开始爬取: " + url);

                // 1. 爬取详情页
                Document doc = Jsoup.connect(url)
                        .timeout(TIMEOUT)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                        .get();

                NovelSummary summary = new NovelSummary();
                summary.setSourceUrl(url);

                // 2. 提取标题
                String title = extractText(doc, "h1.novel-title");
                if (title == null) title = extractText(doc, "h1");
                summary.setTitle(title != null ? title.trim() : "未知标题");

                // 3. 提取作者
                String author = extractText(doc, "span.author-name");
                if (author == null) author = extractText(doc, ".author-name");
                summary.setAuthor(author != null ? author.trim() : "未知作者");

                // 4. 提取简介/描述
                String description = extractText(doc, "div.novel-desc");
                if (description == null) description = extractText(doc, ".novel-description");
                if (description == null) description = extractText(doc, ".desc");
                summary.setDescription(description != null ? description.trim() : "无简介");

                // 5. 提取目录列表，获取前几章内容来生成大纲
                List<String> chapterTitles = extractChapterTitles(doc);
                summary.setCharacters(new ArrayList<>());

                // 6. 爬取前几章内容，提取关键信息
                List<String> chapterUrls = extractChapterUrls(doc);
                StringBuilder outlineBuilder = new StringBuilder();
                StringBuilder characterBuilder = new StringBuilder();
                int chaptersToCrawl = Math.min(chapterUrls.size(), 5); // 只爬前5章

                for (int i = 0; i < chaptersToCrawl; i++) {
                    try {
                        String chapterContent = crawlChapterContent(chapterUrls.get(i));
                        if (chapterContent != null) {
                            // 提取本章关键情节（取前200字作为本章概要）
                            String chapterOutline = chapterContent.length() > 200
                                    ? chapterContent.substring(0, 200) + "..."
                                    : chapterContent;
                            outlineBuilder.append("第").append(i + 1).append("章概要：")
                                    .append(chapterOutline).append("\n\n");

                            // 提取本章出现的人物
                            extractCharactersFromText(chapterContent, summary.getCharacters());
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "爬取第" + (i + 1) + "章失败: " + e.getMessage());
                    }
                }

                // 7. 生成大纲
                if (outlineBuilder.length() > 0) {
                    summary.setOutline(outlineBuilder.toString());
                } else {
                    summary.setOutline("（未能获取章节内容，仅获取到目录）\n" +
                            String.join(" → ", chapterTitles.subList(0, Math.min(chapterTitles.size(), 10))));
                }

                // 8. 生成总结
                summary.setSummary(generateSummary(summary));

                // 9. 生成人物画像
                summary.setCharacterProfiles(generateCharacterProfiles(summary.getCharacters()));

                Log.d(TAG, "爬取完成: " + summary.getTitle());
                callback.onSuccess(summary);

            } catch (IOException e) {
                Log.e(TAG, "爬取失败: " + e.getMessage());
                callback.onFailure(e);
            }
        }).start();
    }

    /**
     * 爬取小说并直接存入素材库
     *
     * @param url      小说详情页URL
     * @param context  Android上下文
     * @param callback 回调
     */
    public void crawlAndSave(String url, Context context, CrawlCallback callback) {
        crawlNovelDetail(url, new CrawlCallback() {
            @Override
            public void onSuccess(NovelSummary summary) {
                // 转换为Material并存入数据库
                MaterialDao materialDao = new MaterialDao(context);
                Material material = summary.toMaterial();
                long id = materialDao.insert(material);
                if (id > 0) {
                    Log.d(TAG, "素材已存入数据库，ID: " + id);
                } else {
                    Log.w(TAG, "素材存入数据库失败");
                }
                callback.onSuccess(summary);
            }

            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }

    /**
     * 批量爬取多个小说
     *
     * @param urls      小说URL列表
     * @param context   Android上下文
     * @param callback  每个小说爬取完成后的回调
     */
    public void crawlBatch(List<String> urls, Context context, CrawlCallback callback) {
        for (String url : urls) {
            crawlAndSave(url, context, new CrawlCallback() {
                @Override
                public void onSuccess(NovelSummary summary) {
                    Log.d(TAG, "批量爬取完成: " + summary.getTitle());
                    callback.onSuccess(summary);
                }

                @Override
                public void onFailure(Exception e) {
                    Log.e(TAG, "批量爬取失败: " + url + " - " + e.getMessage());
                    callback.onFailure(e);
                }
            });

            // 爬取间隔，避免被网站封禁
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 提取元素的文本内容
     */
    private String extractText(Document doc, String cssSelector) {
        try {
            Element element = doc.selectFirst(cssSelector);
            return element != null ? element.text() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 提取章节标题列表
     */
    private List<String> extractChapterTitles(Document doc) {
        List<String> titles = new ArrayList<>();
        try {
            Elements chapterElements = doc.select("a.chapter-item");
            if (chapterElements.isEmpty()) {
                chapterElements = doc.select(".chapter-list a");
            }
            if (chapterElements.isEmpty()) {
                chapterElements = doc.select("a[href*=chapter]");
            }
            for (Element element : chapterElements) {
                String title = element.text();
                if (title != null && !title.isEmpty()) {
                    titles.add(title.trim());
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "提取章节标题失败: " + e.getMessage());
        }
        return titles;
    }

    /**
     * 提取章节URL列表
     */
    private List<String> extractChapterUrls(Document doc) {
        List<String> urls = new ArrayList<>();
        try {
            String baseUrl = doc.baseUri();
            Elements chapterElements = doc.select("a.chapter-item");
            if (chapterElements.isEmpty()) {
                chapterElements = doc.select(".chapter-list a");
            }
            if (chapterElements.isEmpty()) {
                chapterElements = doc.select("a[href*=chapter]");
            }
            for (Element element : chapterElements) {
                String href = element.attr("href");
                if (href != null && !href.isEmpty()) {
                    if (href.startsWith("http")) {
                        urls.add(href);
                    } else {
                        urls.add(baseUrl + href);
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "提取章节URL失败: " + e.getMessage());
        }
        return urls;
    }

    /**
     * 爬取单个章节的内容
     */
    private String crawlChapterContent(String url) {
        try {
            Document doc = Jsoup.connect(url)
                    .timeout(TIMEOUT)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .get();

            // 尝试多种选择器提取正文
            String content = extractText(doc, "div.chapter-content");
            if (content == null) content = extractText(doc, ".content");
            if (content == null) content = extractText(doc, "article");
            if (content == null) content = extractText(doc, "p");

            return content;
        } catch (Exception e) {
            Log.w(TAG, "爬取章节内容失败: " + url + " - " + e.getMessage());
            return null;
        }
    }

    /**
     * 从文本中提取人物名称
     */
    private void extractCharactersFromText(String text, List<String> characters) {
        if (text == null || characters == null) return;

        // 常见的中文人名模式：2-4个中文字符
        // 这里用简单的规则：提取对话中出现的名字
        String[] lines = text.split("[。！？\n]");
        for (String line : lines) {
            // 匹配 "XXX说"、"XXX道" 等模式
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("([\\u4e00-\\u9fa5]{2,4})(?:说|道|问|答|喊|叫|骂|笑|叹)");
            java.util.regex.Matcher matcher = pattern.matcher(line);
            while (matcher.find()) {
                String name = matcher.group(1);
                if (!characters.contains(name) && !isCommonWord(name)) {
                    characters.add(name);
                }
            }
        }
    }

    /**
     * 判断是否为常见词（非人名）
     */
    private boolean isCommonWord(String word) {
        String[] commonWords = {"我们", "他们", "你们", "大家", "自己", "别人", "什么", "怎么", "这样", "那样",
                "这个", "那个", "这些", "那些", "这里", "那里", "没有", "可以", "知道", "觉得",
                "突然", "开始", "已经", "还是", "就是", "只是", "但是", "而且", "因为", "所以"};
        for (String common : commonWords) {
            if (common.equals(word)) return true;
        }
        return false;
    }

    /**
     * 生成总结
     */
    private String generateSummary(NovelSummary summary) {
        StringBuilder sb = new StringBuilder();
        sb.append("小说《").append(summary.getTitle()).append("》");
        if (summary.getAuthor() != null) {
            sb.append("（作者：").append(summary.getAuthor()).append("）");
        }
        sb.append("\n\n");

        if (summary.getDescription() != null && !summary.getDescription().equals("无简介")) {
            sb.append("【内容简介】").append(summary.getDescription()).append("\n\n");
        }

        if (summary.getCharacters() != null && !summary.getCharacters().isEmpty()) {
            sb.append("【主要人物】").append(String.join("、", summary.getCharacters())).append("\n\n");
        }

        if (summary.getOutline() != null) {
            sb.append("【情节概要】").append(summary.getOutline().length() > 500
                    ? summary.getOutline().substring(0, 500) + "..."
                    : summary.getOutline());
        }

        return sb.toString();
    }

    /**
     * 生成人物画像
     */
    private String generateCharacterProfiles(List<String> characters) {
        if (characters == null || characters.isEmpty()) {
            return "（暂未识别到明确人物）";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("识别到以下主要人物，可用于后续生成人物画像：\n\n");
        for (int i = 0; i < characters.size(); i++) {
            sb.append(i + 1).append(". ").append(characters.get(i)).append("\n");
            sb.append("   - 性格特征：（待分析）\n");
            sb.append("   - 角色定位：（待分析）\n");
            sb.append("   - 人物关系：（待分析）\n\n");
        }
        return sb.toString();
    }
}
