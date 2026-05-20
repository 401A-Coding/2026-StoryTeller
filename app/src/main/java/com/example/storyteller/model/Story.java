package com.example.storyteller.model;

public class Story {
    // 数据库主键
    private int id;
    // 故事标题
    private String title;
    // 故事正文
    private String content;
    // 故事类型（如科幻/童话/悬疑）
    private String genre;
    // 创建时间
    private long createTime;
    // 是否加入书架
    private boolean isCollected;
    // 卷-章结构的JSON数据
    private String structure;
    // 小说简介
    private String description;
    // 剧情梳理快照 JSON
    private String plotSummaryJson;
    // 书架分类：全部/创作中/已完成/已收藏
    private String category;
    // 封面颜色（十六进制颜色值，作为封面图片未设置时的备用背景）
    private String coverColor;
    // 封面图片路径（用户上传的封面图片文件路径）
    private String coverPath;
    // 总字数
    private int wordCount;
    // 系列名称（可为空）
    private String seriesName;
    // 大纲数据JSON（与structure分离存储）
    private String outlineData;
    // 全局大纲（Markdown格式文本）
    private String globalOutline;
    // 最近编辑时间
    private long lastEditTime;

    // 构造方法（用于创建新故事）
    public Story(String title, String content, String genre, long createTime) {
        this.title = title;
        this.content = content;
        this.genre = genre;
        this.createTime = createTime;
        this.lastEditTime = createTime;
        this.isCollected = false;
        this.structure = null;
        this.description = null;
        this.plotSummaryJson = null;
        this.category = "创作中";
        this.coverColor = getDefaultCoverColor(title);
        this.coverPath = null;
        this.wordCount = 0;
        this.seriesName = null;
    }

    // 数据库查询用构造方法
    public Story(int id, String title, String content, String genre, long createTime, boolean isCollected) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.genre = genre;
        this.createTime = createTime;
        this.lastEditTime = createTime;
        this.isCollected = isCollected;
        this.structure = null;
        this.description = null;
        this.plotSummaryJson = null;
        this.category = "创作中";
        this.coverColor = getDefaultCoverColor(title);
        this.coverPath = null;
        this.wordCount = 0;
        this.seriesName = null;
    }

    // 完整构造方法（含 plotSummaryJson）
    public Story(int id, String title, String content, String genre, long createTime, boolean isCollected, String structure, String description, String plotSummaryJson) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.genre = genre;
        this.createTime = createTime;
        this.lastEditTime = createTime;
        this.isCollected = isCollected;
        this.structure = structure;
        this.description = description;
        this.plotSummaryJson = plotSummaryJson;
        this.category = "创作中";
        this.coverColor = getDefaultCoverColor(title);
        this.coverPath = null;
        this.wordCount = 0;
        this.seriesName = null;
    }

    // 完整构造方法（含分类和封面颜色）
    public Story(int id, String title, String content, String genre, long createTime, boolean isCollected, String structure, String description, String category, String coverColor) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.genre = genre;
        this.createTime = createTime;
        this.lastEditTime = createTime;
        this.isCollected = isCollected;
        this.structure = structure;
        this.description = description;
        this.plotSummaryJson = null;
        this.category = category;
        this.coverColor = coverColor;
        this.coverPath = null;
        this.wordCount = 0;
        this.seriesName = null;
    }

    // 完整构造方法（含所有字段）
    public Story(int id, String title, String content, String genre, long createTime, boolean isCollected, String structure, String description, String plotSummaryJson, String category, String coverColor, String coverPath) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.genre = genre;
        this.createTime = createTime;
        this.lastEditTime = createTime;
        this.isCollected = isCollected;
        this.structure = structure;
        this.description = description;
        this.plotSummaryJson = plotSummaryJson;
        this.category = category;
        this.coverColor = coverColor;
        this.coverPath = coverPath;
        this.wordCount = 0;
        this.seriesName = null;
    }

    // 完整构造方法（含wordCount）
    public Story(int id, String title, String content, String genre, long createTime, boolean isCollected, String structure, String description, String plotSummaryJson, String category, String coverColor, String coverPath, int wordCount) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.genre = genre;
        this.createTime = createTime;
        this.lastEditTime = createTime;
        this.isCollected = isCollected;
        this.structure = structure;
        this.description = description;
        this.plotSummaryJson = plotSummaryJson;
        this.category = category;
        this.coverColor = coverColor;
        this.coverPath = coverPath;
        this.wordCount = wordCount;
        this.seriesName = null;
    }

    // 完整构造方法（含seriesName）
    public Story(int id, String title, String content, String genre, long createTime, boolean isCollected, String structure, String description, String plotSummaryJson, String category, String coverColor, String coverPath, int wordCount, String seriesName) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.genre = genre;
        this.createTime = createTime;
        this.lastEditTime = createTime;
        this.isCollected = isCollected;
        this.structure = structure;
        this.description = description;
        this.plotSummaryJson = plotSummaryJson;
        this.category = category;
        this.coverColor = coverColor;
        this.coverPath = coverPath;
        this.wordCount = wordCount;
        this.seriesName = seriesName;
        this.outlineData = null;
        this.globalOutline = null;
    }

    // 完整构造方法（含lastEditTime）
    public Story(int id, String title, String content, String genre, long createTime, long lastEditTime, boolean isCollected, String structure, String description, String plotSummaryJson, String category, String coverColor, String coverPath, int wordCount, String seriesName, String outlineData, String globalOutline) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.genre = genre;
        this.createTime = createTime;
        this.lastEditTime = lastEditTime;
        this.isCollected = isCollected;
        this.structure = structure;
        this.description = description;
        this.plotSummaryJson = plotSummaryJson;
        this.category = category;
        this.coverColor = coverColor;
        this.coverPath = coverPath;
        this.wordCount = wordCount;
        this.seriesName = seriesName;
        this.outlineData = outlineData;
        this.globalOutline = globalOutline;
    }

    /**
     * 根据标题生成默认封面颜色
     */
    public static String getDefaultCoverColor(String title) {
        if (title == null || title.isEmpty()) {
            return "#1976D2";
        }
        // 根据标题哈希值选择颜色
        String[] colors = {
            "#1976D2", "#388E3C", "#F57C00", "#7B1FA2",
            "#C2185B", "#0097A7", "#5D4037", "#455A64",
            "#E64A19", "#303F9F", "#00796B", "#AFB42B"
        };
        int index = Math.abs(title.hashCode()) % colors.length;
        return colors[index];
    }

    // Getter & Setter
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }
    public long getCreateTime() { return createTime; }
    public void setCreateTime(long createTime) { this.createTime = createTime; }
    public boolean isCollected() { return isCollected; }
    public void setCollected(boolean collected) { isCollected = collected; }
    public String getStructure() { return structure; }
    public void setStructure(String structure) { this.structure = structure; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getPlotSummaryJson() { return plotSummaryJson; }
    public void setPlotSummaryJson(String plotSummaryJson) { this.plotSummaryJson = plotSummaryJson; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getCoverColor() { return coverColor; }
    public void setCoverColor(String coverColor) { this.coverColor = coverColor; }
    public String getCoverPath() { return coverPath; }
    public void setCoverPath(String coverPath) { this.coverPath = coverPath; }
    public int getWordCount() { return wordCount; }
    public void setWordCount(int wordCount) { this.wordCount = wordCount; }
    public String getSeriesName() { return seriesName; }
    public void setSeriesName(String seriesName) { this.seriesName = seriesName; }
    public String getOutlineData() { return outlineData; }
    public void setOutlineData(String outlineData) { this.outlineData = outlineData; }
    public String getGlobalOutline() { return globalOutline; }
    public void setGlobalOutline(String globalOutline) { this.globalOutline = globalOutline; }
    public long getLastEditTime() { return lastEditTime; }
    public void setLastEditTime(long lastEditTime) { this.lastEditTime = lastEditTime; }
}
