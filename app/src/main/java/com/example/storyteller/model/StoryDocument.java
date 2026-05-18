package com.example.storyteller.model;

/**
 * 故事文档模型
 * 用于存储小说相关的参考资料、笔记等非结构化文本内容
 * 支持Markdown格式
 */
public class StoryDocument {
    private int id;
    private int storyId;
    private String title;
    private String content;  // Markdown格式文本
    private String category; // world/character/plot/research/general
    private long createTime;
    private long updateTime;

    public StoryDocument() {
    }

    public StoryDocument(int storyId, String title, String content, String category) {
        this.storyId = storyId;
        this.title = title;
        this.content = content;
        this.category = category;
        this.createTime = System.currentTimeMillis();
        this.updateTime = System.currentTimeMillis();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getStoryId() {
        return storyId;
    }

    public void setStoryId(int storyId) {
        this.storyId = storyId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }

    /**
     * 获取分类的中文显示名称
     */
    public String getCategoryDisplayName() {
        if (category == null) {
            return "其他";
        }
        switch (category) {
            case "world":
                return "世界观";
            case "character":
                return "人物";
            case "plot":
                return "剧情";
            case "research":
                return "研究资料";
            case "general":
            default:
                return "其他";
        }
    }
}
