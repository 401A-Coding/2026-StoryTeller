package com.example.storyteller.data.remote;

import android.content.Context;
import android.util.Log;

import com.example.storyteller.data.local.db.StorySettingDao;
import com.example.storyteller.model.StorySetting;
import com.example.storyteller.model.NovelSummary;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 番茄小说爬虫工具类
 * 用于爬取番茄小说网站的关键信息（大纲、总结、人物画像），不存储完整正文
 * 
 * 重构说明：
 * - 统一使用DOM提取，移除文本正则匹配降级方案
 * - 使用FanqieSelectors常量管理CSS选择器
 * - 简化主流程，提高可读性
 * - 规范化错误处理，统一返回null或空列表
 */
public class NovelCrawler {

    private static final String TAG = "NovelCrawler";
    private static final int TIMEOUT = 15000; // 15秒超时

    // 回调接口
    public interface CrawlCallback {
        void onSuccess(NovelSummary summary, int savedCount);
        void onFailure(Exception e);
    }

    public interface ExtractCallback {
        void onSuccess(NovelSummary summary, List<StorySetting> settings, String rawJson);
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

                // 1. 爬取页面
                Document doc = fetchDocument(url);
                
                // 2. 提取基本信息
                NovelSummary summary = extractBasicInfo(doc, url);
                
                // 3. 提取卷信息（新增）
                List<String> volumes = extractVolumes(doc);
                summary.setVolumes(volumes);
                
                // 4. 生成衍生内容（大纲、总结、人物画像）
                enrichSummary(summary);

                Log.d(TAG, "爬取完成: " + summary.getTitle() + 
                      ", 章节数: " + (summary.getChapterTitles() != null ? summary.getChapterTitles().size() : 0) +
                      ", 字数: " + summary.getTotalWords() +
                      ", 卷数: " + (volumes != null ? volumes.size() : 0));
                callback.onSuccess(summary, 0);

            } catch (IOException e) {
                Log.e(TAG, "爬取失败: " + e.getMessage(), e);
                callback.onFailure(e);
            }
        }).start();
    }

    /**
     * 爬取小说并直接存入素材库（新版 - AI直接返回StorySetting）
     */
    public void crawlAndSave(String url, Context context, CrawlCallback callback) {
        crawlAndExtract(url, context, null, new ExtractCallback() {
            @Override
            public void onSuccess(NovelSummary summary, List<StorySetting> settings, String rawJson) {
                StorySettingDao settingDao = new StorySettingDao(context);
                
                // 批量插入数据库
                int successCount = 0;
                for (StorySetting setting : settings) {
                    // 设置为全局素材库（storyId = 0）
                    setting.setStoryId(0);
                    long id = settingDao.insert(setting);
                    if (id > 0) {
                        successCount++;
                    }
                }
                
                if (successCount > 0) {
                    Log.d(TAG, "素材已批量存入数据库，成功: " + successCount + "/" + settings.size() + " 条");
                } else {
                    Log.w(TAG, "素材批量存入数据库失败");
                }
                callback.onSuccess(summary, successCount);
            }

            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }

    public void crawlAndExtract(String url, Context context, List<String> requestedTypes, ExtractCallback callback) {
        crawlNovelDetail(url, new CrawlCallback() {
            @Override
            public void onSuccess(NovelSummary summary, int ignored) {
                MaterialCandidateExtractor extractor = new MaterialCandidateExtractor();
                extractor.extract(summary, context, requestedTypes, new MaterialCandidateExtractor.Callback() {
                    @Override
                    public void onSuccess(List<StorySetting> settings, String rawJson) {
                        callback.onSuccess(summary, settings, rawJson);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        callback.onFailure(e);
                    }
                });

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
                public void onSuccess(NovelSummary summary, int savedCount) {
                    Log.d(TAG, "批量爬取完成: " + summary.getTitle() + ", 存入 " + savedCount + " 条素材");
                    callback.onSuccess(summary, savedCount);
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

    // ==================== 核心方法 ====================

    /**
     * 获取HTML文档
     */
    private Document fetchDocument(String url) throws IOException {
        return Jsoup.connect(url)
                .timeout(TIMEOUT)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                .get();
    }

    /**
     * 提取基本信息（标题、作者、简介、标签、字数、章节等）
     */
    private NovelSummary extractBasicInfo(Document doc, String url) {
        NovelSummary summary = new NovelSummary();
        summary.setSourceUrl(url);

        // 按顺序提取各项信息
        summary.setTitle(extractTitle(doc));
        summary.setAuthor(extractAuthor(doc));
        summary.setDescription(extractDescription(doc));
        summary.setTags(extractTags(doc));
        summary.setTotalWords(extractWordCount(doc));
        summary.setChapterTitles(extractChapterTitles(doc));
        summary.setCoverUrl(extractCoverUrl(doc));
        summary.setCharacters(new ArrayList<>());

        return summary;
    }

    /**
     * 丰富摘要内容（生成大纲、总结、人物画像）
     */
    private void enrichSummary(NovelSummary summary) {
        // 1. 从章节列表生成大纲
        generateOutline(summary);
        
        // 2. 从简介中提取人物
        extractCharactersFromText(summary.getDescription(), summary.getCharacters());
        
        // 3. 生成总结
        summary.setSummary(generateSummary(summary));
        
        // 4. 生成人物画像
        summary.setCharacterProfiles(generateCharacterProfiles(summary.getCharacters()));
    }

    // ==================== 提取方法 ====================

    /**
     * 提取标题
     */
    private String extractTitle(Document doc) {
        try {
            String title = doc.selectFirst(FanqieSelectors.TITLE).text().trim();
            if (!title.isEmpty()) {
                Log.d(TAG, "提取标题: " + title);
                return title;
            }
        } catch (Exception e) {
            Log.w(TAG, "提取标题失败: " + e.getMessage());
        }
        return "未知标题";
    }

    /**
     * 提取作者
     */
    private String extractAuthor(Document doc) {
        try {
            String author = doc.selectFirst(FanqieSelectors.AUTHOR_NAME).text().trim();
            if (!author.isEmpty()) {
                Log.d(TAG, "提取作者: " + author);
                return author;
            }
        } catch (Exception e) {
            Log.w(TAG, "提取作者失败: " + e.getMessage());
        }
        return "未知作者";
    }

    /**
     * 提取简介
     */
    private String extractDescription(Document doc) {
        try {
            String description = doc.selectFirst(FanqieSelectors.ABSTRACT_PARAGRAPH).text().trim();
            if (!description.isEmpty() && description.length() > 10) {
                Log.d(TAG, "提取简介，长度: " + description.length());
                return description;
            }
        } catch (Exception e) {
            Log.w(TAG, "提取简介失败: " + e.getMessage());
        }
        return "无简介";
    }

    /**
     * 提取标签/分类
     */
    private List<String> extractTags(Document doc) {
        List<String> tags = new ArrayList<>();
        
        try {
            doc.select(FanqieSelectors.TAG_ITEM).forEach(element -> {
                String tag = element.text().trim();
                if (!tag.isEmpty()) {
                    tags.add(tag);
                }
            });
            
            if (!tags.isEmpty()) {
                Log.d(TAG, "提取标签: " + String.join(", ", tags));
            }
        } catch (Exception e) {
            Log.w(TAG, "提取标签失败: " + e.getMessage());
        }
        
        return tags;
    }

    /**
     * 提取字数
     */
    private int extractWordCount(Document doc) {
        try {
            // 提取数值部分
            String numberText = doc.selectFirst(FanqieSelectors.WORD_COUNT_NUMBER).text().trim();
            double wordNum = Double.parseDouble(numberText);
            
            // 提取单位部分
            String unitText = doc.selectFirst(FanqieSelectors.WORD_COUNT_UNIT).text().trim();
            
            // 如果是"万字"，需要乘以10000
            if (unitText.contains("万")) {
                int totalWords = (int) (wordNum * 10000);
                Log.d(TAG, "提取字数: " + totalWords + " (" + wordNum + "万字)");
                return totalWords;
            }
        } catch (Exception e) {
            Log.w(TAG, "提取字数失败: " + e.getMessage());
        }
        
        return 0;
    }

    /**
     * 提取章节标题列表（支持多卷结构）
     */
    private List<String> extractChapterTitles(Document doc) {
        List<String> titles = new ArrayList<>();
        
        try {
            // 从所有章节链接中提取标题（包括所有卷）
            doc.select(FanqieSelectors.CHAPTER_ITEM).forEach(element -> {
                String title = element.text().trim();
                if (!title.isEmpty() && !titles.contains(title)) {
                    titles.add(title);
                }
            });
            
            if (!titles.isEmpty()) {
                Log.d(TAG, "提取章节数: " + titles.size());
            }
        } catch (Exception e) {
            Log.w(TAG, "提取章节失败: " + e.getMessage());
        }
        
        return titles;
    }

    /**
     * 提取卷信息（新增方法，用于获取多卷结构）
     * @return 卷信息列表，每个元素格式："卷名|章节数"
     */
    public List<String> extractVolumes(Document doc) {
        List<String> volumes = new ArrayList<>();
        
        try {
            // 选择所有卷元素
            doc.select(FanqieSelectors.VOLUME_ITEM).forEach(element -> {
                String volumeText = element.text().trim();
                if (!volumeText.isEmpty()) {
                    volumes.add(volumeText);
                }
            });
            
            if (!volumes.isEmpty()) {
                Log.d(TAG, "提取卷数: " + volumes.size());
                for (String vol : volumes) {
                    Log.d(TAG, "  - " + vol);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "提取卷信息失败: " + e.getMessage());
        }
        
        return volumes;
    }

    /**
     * 提取封面图片URL（暂时返回null，后续优化）
     */
    private String extractCoverUrl(Document doc) {
        // 番茄小说有防爬取机制，封面提取暂不实现
        return null;
    }

    // ==================== 生成方法 ====================

    /**
     * 从章节列表生成大纲
     */
    private void generateOutline(NovelSummary summary) {
        List<String> chapterTitles = summary.getChapterTitles();
        if (chapterTitles == null || chapterTitles.isEmpty()) {
            summary.setOutline("暂无章节信息");
            return;
        }

        StringBuilder outlineBuilder = new StringBuilder();
        outlineBuilder.append("【章节脉络】\n");
        
        // 最多显示前30章
        int maxChapters = Math.min(chapterTitles.size(), 30);
        for (int i = 0; i < maxChapters; i++) {
            outlineBuilder.append("第").append(i + 1).append("章：")
                         .append(chapterTitles.get(i)).append("\n");
        }
        
        if (chapterTitles.size() > 30) {
            outlineBuilder.append("...共").append(chapterTitles.size()).append("章\n");
        }
        
        summary.setOutline(outlineBuilder.toString());
    }

    /**
     * 从文本中提取人物名称
     */
    private void extractCharactersFromText(String text, List<String> characters) {
        if (text == null || characters == null || text.isEmpty()) {
            return;
        }

        // 简单的人物提取：查找2-4个中文字符后跟"说/道/问"等动词的模式
        String[] lines = text.split("[。！？\n]");
        for (String line : lines) {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "([\\u4e00-\\u9fa5]{2,4})(?:说|道|问|答|喊|叫|骂|笑|叹)"
            );
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
        String[] commonWords = {"我们", "他们", "你们", "大家", "自己", "别人", "什么", "怎么", 
                               "这样", "那样", "这个", "那个", "这些", "那些", "这里", "那里", 
                               "没有", "可以", "知道", "觉得", "突然", "开始", "已经", "还是", 
                               "就是", "只是", "但是", "而且", "因为", "所以", "一个", "两个", 
                               "不是", "如果", "虽然", "然后", "最后", "终于"};
        
        for (String common : commonWords) {
            if (common.equals(word)) {
                return true;
            }
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
            String outline = summary.getOutline();
            sb.append("【情节概要】").append(outline.length() > 500 
                ? outline.substring(0, 500) + "..." 
                : outline);
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
