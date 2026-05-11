package com.example.storyteller.model;

import java.util.List;

/**
 * 小说爬取摘要模型
 * 存储从番茄小说爬取的关键信息：大纲、总结、人物画像等
 */
public class NovelSummary {
    private String title;           // 小说标题
    private String author;          // 作者
    private String description;     // 小说简介/描述
    private String outline;         // 大纲（主要情节脉络）
    private String summary;         // 总结（整体评价和风格）
    private List<String> characters; // 主要人物列表
    private String characterProfiles; // 人物画像（详细的人物性格、关系等）
    private String sourceUrl;       // 来源URL
    private long createTime;        // 爬取时间

    public NovelSummary() {
        this.createTime = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getOutline() { return outline; }
    public void setOutline(String outline) { this.outline = outline; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public List<String> getCharacters() { return characters; }
    public void setCharacters(List<String> characters) { this.characters = characters; }

    public String getCharacterProfiles() { return characterProfiles; }
    public void setCharacterProfiles(String characterProfiles) { this.characterProfiles = characterProfiles; }

    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }

    public long getCreateTime() { return createTime; }
    public void setCreateTime(long createTime) { this.createTime = createTime; }

    /**
     * 将 NovelSummary 转换为 Material 对象，存入素材库
     * category 固定为 "novel_summary"
     */
    public Material toMaterial() {
        StringBuilder contentBuilder = new StringBuilder();
        contentBuilder.append("【作者】").append(author != null ? author : "未知").append("\n\n");
        contentBuilder.append("【简介】").append(description != null ? description : "无").append("\n\n");
        contentBuilder.append("【大纲】").append(outline != null ? outline : "无").append("\n\n");
        contentBuilder.append("【总结】").append(summary != null ? summary : "无").append("\n\n");
        contentBuilder.append("【人物画像】").append(characterProfiles != null ? characterProfiles : "无").append("\n\n");
        contentBuilder.append("【来源】").append(sourceUrl != null ? sourceUrl : "无");

        Material material = new Material(
                "novel_summary",
                title != null ? title : "未知小说",
                contentBuilder.toString(),
                createTime
        );
        material.setSourceUrl(sourceUrl);
        material.setSourceTitle(title);
        material.setSourceType("novel_summary");
        material.setAiScore(0d);
        material.setRawJson(null);
        return material;
    }
}
