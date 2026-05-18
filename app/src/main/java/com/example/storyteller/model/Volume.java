package com.example.storyteller.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 卷（Volume）模型类
 * 用于表示小说中的一个卷，包含多个章节
 */
public class Volume {
    private int id;
    private String title;
    private List<Chapter> chapters;
    
    // 大纲相关字段
    private String summary;              // 卷摘要
    private int targetWordCount;         // 目标字数
    private int targetChapterCount;      // 目标章节数

    public Volume() {
        this.chapters = new ArrayList<>();
        this.summary = "";
        this.targetWordCount = 0;
        this.targetChapterCount = 0;
    }

    public Volume(String title) {
        this.title = title;
        this.chapters = new ArrayList<>();
    }

    public Volume(int id, String title) {
        this.id = id;
        this.title = title;
        this.chapters = new ArrayList<>();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<Chapter> getChapters() {
        return chapters;
    }

    public void setChapters(List<Chapter> chapters) {
        this.chapters = chapters;
    }

    public void addChapter(Chapter chapter) {
        this.chapters.add(chapter);
    }

    public void removeChapter(int index) {
        if (index >= 0 && index < chapters.size()) {
            chapters.remove(index);
        }
    }

    public Chapter getChapter(int index) {
        if (index >= 0 && index < chapters.size()) {
            return chapters.get(index);
        }
        return null;
    }
    
    // 大纲字段 Getter & Setter
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    
    public int getTargetWordCount() { return targetWordCount; }
    public void setTargetWordCount(int targetWordCount) { this.targetWordCount = targetWordCount; }
    
    public int getTargetChapterCount() { return targetChapterCount; }
    public void setTargetChapterCount(int targetChapterCount) { this.targetChapterCount = targetChapterCount; }
}
