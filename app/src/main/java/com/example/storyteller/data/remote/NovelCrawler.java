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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 小说爬虫工具类
 * 用于爬取番茄小说等网站的关键信息（大纲、总结、人物画像），不存储完整正文
 * 适配番茄小说 SPA 页面结构
 */
public class NovelCrawler {

    private static final String TAG = "NovelCrawler";
    private static final int TIMEOUT = 15000; // 15秒超时

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
                        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                        .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                        .get();

                NovelSummary summary = new NovelSummary();
                summary.setSourceUrl(url);

                // 获取页面纯文本
                String pageText = doc.body().text();

                // 2. 提取标题 - 页面文本的第一个书名号内容或第一个粗体文字
                String title = extractTitle(pageText, doc);
                summary.setTitle(title != null ? title.trim() : "未知标题");

                // 3. 提取作者 - 在标题后面找作者名
                String author = extractAuthor(pageText, doc);
                summary.setAuthor(author != null ? author.trim() : "未知作者");

                // 4. 提取简介 - "作品简介"后面的内容
                String description = extractDescription(pageText);
                summary.setDescription(description != null ? description.trim() : "无简介");

                // 5. 提取目录列表
                List<String> chapterTitles = extractChapterTitles(pageText);
                summary.setCharacters(new ArrayList<>());

                // 6. 生成大纲（从目录中提取前10章标题作为大纲脉络）
                StringBuilder outlineBuilder = new StringBuilder();
                if (!chapterTitles.isEmpty()) {
                    outlineBuilder.append("【章节脉络】\n");
                    int maxChapters = Math.min(chapterTitles.size(), 30);
                    for (int i = 0; i < maxChapters; i++) {
                        outlineBuilder.append("第").append(i + 1).append("章：").append(chapterTitles.get(i)).append("\n");
                    }
                    if (chapterTitles.size() > 30) {
                        outlineBuilder.append("...共").append(chapterTitles.size()).append("章\n");
                    }
                }
                summary.setOutline(outlineBuilder.toString());

                // 7. 从简介中提取人物
                if (description != null) {
                    extractCharactersFromText(description, summary.getCharacters());
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
     */
    public void crawlAndSave(String url, Context context, CrawlCallback callback) {
        crawlNovelDetail(url, new CrawlCallback() {
            @Override
            public void onSuccess(NovelSummary summary) {
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

            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // ==================== 提取方法 ====================

    /**
     * 提取标题
     */
    private String extractTitle(String pageText, Document doc) {
        // 方法1: 从页面文本中找第一个《》书名号内容
        Pattern pattern = Pattern.compile("《([^》]+)》");
        Matcher matcher = pattern.matcher(pageText);
        if (matcher.find()) {
            return matcher.group(1);
        }

        // 方法2: 从页面标题中提取（title标签）
        String docTitle = doc.title();
        if (docTitle != null) {
            // 番茄小说标题格式: "小说名完整版在线免费阅读_小说名小说_番茄小说官网"
            int idx = docTitle.indexOf("完整版");
            if (idx > 0) {
                return docTitle.substring(0, idx).trim();
            }
            // 或者取第一个下划线之前的内容
            idx = docTitle.indexOf("_");
            if (idx > 0) {
                return docTitle.substring(0, idx).trim();
            }
        }

        return null;
    }

    /**
     * 提取作者
     */
    private String extractAuthor(String pageText, Document doc) {
        // 方法1: 在标题后面找作者（番茄小说页面中作者名通常在标题后面）
        // 匹配模式: "标题 作者名" 或 "标题 作者名 状态"
        String[] lines = pageText.split("\\s+");
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].contains("作者") || lines[i].contains("著")) {
                return lines[i].replace("作者", "").replace("著", "").trim();
            }
        }

        // 方法2: 从页面文本中找"作者名"模式（2-4个中文字符跟在小说标题后面）
        Pattern pattern = Pattern.compile("[\\u4e00-\\u9fa5]{2,4}(?=\\s+(?:作品|分类|状态|连载|完结))");
        Matcher matcher = pattern.matcher(pageText);
        if (matcher.find()) {
            return matcher.group();
        }

        return null;
    }

    /**
     * 提取简介
     */
    private String extractDescription(String pageText) {
        // 方法1: 找"作品简介"后面的内容
        Pattern pattern = Pattern.compile("作品简介[：:\\s]*([^。]+。[^。]*。[^。]*。)");
        Matcher matcher = pattern.matcher(pageText);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        // 方法2: 找"简介"后面的内容
        pattern = Pattern.compile("简介[：:\\s]*([^。]+。[^。]*。[^。]*。)");
        matcher = pattern.matcher(pageText);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        // 方法3: 取"作品简介"到"目录"之间的内容
        int start = pageText.indexOf("作品简介");
        int end = pageText.indexOf("目录");
        if (start > 0 && end > start) {
            String desc = pageText.substring(start + 4, end).trim();
            if (desc.length() > 10) {
                return desc;
            }
        }

        return null;
    }

    /**
     * 提取章节标题列表
     */
    private List<String> extractChapterTitles(String pageText) {
        List<String> titles = new ArrayList<>();

        // 番茄小说页面中章节格式: "第1章 标题" 或 "第1章标题"
        Pattern pattern = Pattern.compile("第\\d+章[\\s]*[\\u4e00-\\u9fa5][^\\d]{2,30}?");
        Matcher matcher = pattern.matcher(pageText);

        // 先找到"目录"或"第1章"开始的位置
        int startIdx = pageText.indexOf("目录");
        if (startIdx < 0) {
            startIdx = pageText.indexOf("第1章");
        }
        if (startIdx < 0) {
            startIdx = 0;
        }

        String searchText = pageText.substring(startIdx);
        matcher = pattern.matcher(searchText);

        while (matcher.find()) {
            String title = matcher.group().trim();
            if (!titles.contains(title)) {
                titles.add(title);
            }
            if (titles.size() >= 100) break; // 最多取100章
        }

        return titles;
    }

    /**
     * 从文本中提取人物名称
     */
    private void extractCharactersFromText(String text, List<String> characters) {
        if (text == null || characters == null) return;

        String[] lines = text.split("[。！？\n]");
        for (String line : lines) {
            Pattern pattern = Pattern.compile("([\\u4e00-\\u9fa5]{2,4})(?:说|道|问|答|喊|叫|骂|笑|叹)");
            Matcher matcher = pattern.matcher(line);
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
                "突然", "开始", "已经", "还是", "就是", "只是", "但是", "而且", "因为", "所以",
                "一个", "两个", "那个", "不是", "就是", "如果", "虽然", "然后", "最后", "终于"};
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
        if (summary.getAuthor() != null && !summary.getAuthor().equals("未知作者")) {
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
            return "（暂未识别到明确人物，建议阅读简介后手动添加）";
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
