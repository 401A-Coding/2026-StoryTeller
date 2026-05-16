package com.example.storyteller.model;

import java.util.List;

/**
 * 导入的小说模型
 * 用于存储从外部爬取的小说信息
 */
public class ImportedNovel {
    private int id;
    private String title;           // 小说标题
    private String author;          // 作者
    private String sourceUrl;       // 来源URL（唯一标识）
    private String coverUrl;        // 封面图片URL
    private String description;     // 简介
    private long importTime;        // 导入时间
    private String status;          // 状态：imported/analyzing/analyzed
    private String structureJson;   // 卷章结构JSON
    private String contentDir;      // 内容存储目录路径
    private int totalChapters;      // 总章节数
    private int totalWords;         // 总字数
    private String tags;            // JSON数组：["玄幻", "热血"]

    public ImportedNovel() {
        this.importTime = System.currentTimeMillis();
        this.status = "imported";
        this.totalChapters = 0;
        this.totalWords = 0;
    }

    public ImportedNovel(String title, String author, String sourceUrl) {
        this();
        this.title = title;
        this.author = author;
        this.sourceUrl = sourceUrl;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }

    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public long getImportTime() { return importTime; }
    public void setImportTime(long importTime) { this.importTime = importTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getStructureJson() { return structureJson; }
    public void setStructureJson(String structureJson) { this.structureJson = structureJson; }

    public String getContentDir() { return contentDir; }
    public void setContentDir(String contentDir) { this.contentDir = contentDir; }

    public int getTotalChapters() { return totalChapters; }
    public void setTotalChapters(int totalChapters) { this.totalChapters = totalChapters; }

    public int getTotalWords() { return totalWords; }
    public void setTotalWords(int totalWords) { this.totalWords = totalWords; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
}
