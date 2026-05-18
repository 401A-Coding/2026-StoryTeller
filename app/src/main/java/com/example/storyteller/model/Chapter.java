package com.example.storyteller.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 章节（Chapter）模型类
 * 用于表示小说中的一个章节，包含标题和正文内容
 */
public class Chapter {
    private int id;
    private String title;
    private String content;
    
    // 大纲相关字段 - 核心信息
    private String chapterRole;          // 章节作用
    private String chapterSummary;       // 章节总结
    private String chapterPurpose;       // 章节目的
    private float suspenseLevel;         // 悬念级别 0-10
    private String foreshadowing;        // 埋笔/伏笔
    private float twistLevel;            // 转折级别 0-5
    
    // 大纲相关字段 - 拓展信息
    private List<String> involvedCharacters;  // 涉及角色
    private List<String> keyItems;            // 关键物品
    private List<String> sceneLocations;      // 场景位置
    private String timeConstraint;            // 时间限制

    public Chapter() {
        this.content = "";
        this.chapterRole = "";
        this.chapterSummary = "";
        this.chapterPurpose = "";
        this.suspenseLevel = 0f;
        this.foreshadowing = "";
        this.twistLevel = 0f;
        this.involvedCharacters = new ArrayList<>();
        this.keyItems = new ArrayList<>();
        this.sceneLocations = new ArrayList<>();
        this.timeConstraint = "";
    }

    public Chapter(String title) {
        this.title = title;
        this.content = "";
        this.chapterRole = "";
        this.chapterSummary = "";
        this.chapterPurpose = "";
        this.suspenseLevel = 0f;
        this.foreshadowing = "";
        this.twistLevel = 0f;
        this.involvedCharacters = new ArrayList<>();
        this.keyItems = new ArrayList<>();
        this.sceneLocations = new ArrayList<>();
        this.timeConstraint = "";
    }

    public Chapter(int id, String title, String content) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.chapterRole = "";
        this.chapterSummary = "";
        this.chapterPurpose = "";
        this.suspenseLevel = 0f;
        this.foreshadowing = "";
        this.twistLevel = 0f;
        this.involvedCharacters = new ArrayList<>();
        this.keyItems = new ArrayList<>();
        this.sceneLocations = new ArrayList<>();
        this.timeConstraint = "";
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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
    
    // 大纲字段 Getter & Setter
    public String getChapterRole() { return chapterRole; }
    public void setChapterRole(String chapterRole) { this.chapterRole = chapterRole; }
    
    public String getChapterSummary() { return chapterSummary; }
    public void setChapterSummary(String chapterSummary) { this.chapterSummary = chapterSummary; }
    
    public String getChapterPurpose() { return chapterPurpose; }
    public void setChapterPurpose(String chapterPurpose) { this.chapterPurpose = chapterPurpose; }
    
    public float getSuspenseLevel() { return suspenseLevel; }
    public void setSuspenseLevel(float suspenseLevel) { this.suspenseLevel = suspenseLevel; }
    
    public String getForeshadowing() { return foreshadowing; }
    public void setForeshadowing(String foreshadowing) { this.foreshadowing = foreshadowing; }
    
    public float getTwistLevel() { return twistLevel; }
    public void setTwistLevel(float twistLevel) { this.twistLevel = twistLevel; }
    
    public List<String> getInvolvedCharacters() { return involvedCharacters; }
    public void setInvolvedCharacters(List<String> involvedCharacters) { this.involvedCharacters = involvedCharacters; }
    
    public List<String> getKeyItems() { return keyItems; }
    public void setKeyItems(List<String> keyItems) { this.keyItems = keyItems; }
    
    public List<String> getSceneLocations() { return sceneLocations; }
    public void setSceneLocations(List<String> sceneLocations) { this.sceneLocations = sceneLocations; }
    
    public String getTimeConstraint() { return timeConstraint; }
    public void setTimeConstraint(String timeConstraint) { this.timeConstraint = timeConstraint; }
}
