package com.example.storyteller.model;

public class Material {
    private int id;
    private String category;
    private String title;
    private String content;
    private long createTime;
    private String sourceUrl;
    private String sourceTitle;
    private String sourceType;
    private double aiScore;
    private String rawJson;

    public Material(String category, String title, String content, long createTime) {
        this.category = category;
        this.title = title;
        this.content = content;
        this.createTime = createTime;
        this.aiScore = 0d;
    }

    public Material(int id, String category, String title, String content, long createTime) {
        this.id = id;
        this.category = category;
        this.title = title;
        this.content = content;
        this.createTime = createTime;
        this.aiScore = 0d;
    }

    public Material(String category, String title, String content, long createTime,
                    String sourceUrl, String sourceTitle, String sourceType,
                    double aiScore, String rawJson) {
        this(category, title, content, createTime);
        this.sourceUrl = sourceUrl;
        this.sourceTitle = sourceTitle;
        this.sourceType = sourceType;
        this.aiScore = aiScore;
        this.rawJson = rawJson;
    }

    public Material(int id, String category, String title, String content, long createTime,
                    String sourceUrl, String sourceTitle, String sourceType,
                    double aiScore, String rawJson) {
        this(id, category, title, content, createTime);
        this.sourceUrl = sourceUrl;
        this.sourceTitle = sourceTitle;
        this.sourceType = sourceType;
        this.aiScore = aiScore;
        this.rawJson = rawJson;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public long getCreateTime() { return createTime; }
    public void setCreateTime(long createTime) { this.createTime = createTime; }
    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }
    public String getSourceTitle() { return sourceTitle; }
    public void setSourceTitle(String sourceTitle) { this.sourceTitle = sourceTitle; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public double getAiScore() { return aiScore; }
    public void setAiScore(double aiScore) { this.aiScore = aiScore; }
    public String getRawJson() { return rawJson; }
    public void setRawJson(String rawJson) { this.rawJson = rawJson; }
}
