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
    private List<String> chapterTitles; // 章节标题列表
    private int totalWords;         // 总字数
    private List<String> tags;      // 标签列表
    private String coverUrl;        // 封面图片URL
    private List<String> volumes;   // 卷信息列表（格式："卷名|章节数"）

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

    public List<String> getChapterTitles() { return chapterTitles; }
    public void setChapterTitles(List<String> chapterTitles) { this.chapterTitles = chapterTitles; }

    public int getTotalWords() { return totalWords; }
    public void setTotalWords(int totalWords) { this.totalWords = totalWords; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }

    public List<String> getVolumes() { return volumes; }
    public void setVolumes(List<String> volumes) { this.volumes = volumes; }

    /**
     * 将 NovelSummary 转换为 StorySetting 对象，存入素材库
     * 作为小说的整体设定素材
     */
    public StorySetting toStorySetting() {
        // 构建详细内容
        StringBuilder detailBuilder = new StringBuilder();
        detailBuilder.append("【作者】").append(author != null ? author : "未知").append("\n\n");
        detailBuilder.append("【简介】").append(description != null ? description : "无").append("\n\n");
        
        // 添加卷信息
        if (volumes != null && !volumes.isEmpty()) {
            detailBuilder.append("【卷信息】\n");
            for (String volume : volumes) {
                detailBuilder.append("  - ").append(volume).append("\n");
            }
            detailBuilder.append("\n");
        }
        
        detailBuilder.append("【大纲】").append(outline != null ? outline : "无").append("\n\n");
        detailBuilder.append("【总结】").append(summary != null ? summary : "无").append("\n\n");
        detailBuilder.append("【人物画像】").append(characterProfiles != null ? characterProfiles : "无").append("\n\n");
        detailBuilder.append("【来源】").append(sourceUrl != null ? sourceUrl : "无");

        // 创建StorySetting对象
        StorySetting setting = new StorySetting(0, "世界观", "地理环境", title != null ? title : "未知小说");
        setting.setSummary(description != null && !description.isEmpty() ? description : "小说整体设定");
        setting.setDetail(detailBuilder.toString());
        
        // 设置标签
        if (tags != null && !tags.isEmpty()) {
            com.google.gson.Gson gson = new com.google.gson.Gson();
            setting.setTags(gson.toJson(tags));
        }
        
        // 设置来源信息
        setting.setSourceUrl(sourceUrl);
        setting.setSourceTitle(title);
        setting.setSourceType("novel_summary");
        setting.setAiConfidence(1.0);  // 这是直接从小说提取的，置信度高
        setting.setCreateTime(createTime);
        setting.setUpdateTime(createTime);
        
        return setting;
    }
}
