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

    // 构造方法（用于创建新故事）
    public Story(String title, String content, String genre, long createTime) {
        this.title = title;
        this.content = content;
        this.genre = genre;
        this.createTime = createTime;
        this.isCollected = false;
        this.structure = null;
        this.description = null;
        this.plotSummaryJson = null;
    }

    // 数据库查询用构造方法
    public Story(int id, String title, String content, String genre, long createTime, boolean isCollected) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.genre = genre;
        this.createTime = createTime;
        this.isCollected = isCollected;
        this.structure = null;
        this.description = null;
        this.plotSummaryJson = null;
    }

    // 完整构造方法
    public Story(int id, String title, String content, String genre, long createTime, boolean isCollected, String structure, String description, String plotSummaryJson) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.genre = genre;
        this.createTime = createTime;
        this.isCollected = isCollected;
        this.structure = structure;
        this.description = description;
        this.plotSummaryJson = plotSummaryJson;
    }

    // Getter & Setter（后续所有属性都需要，可通过Android Studio自动生成）
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
}