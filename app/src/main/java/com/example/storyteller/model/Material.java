package com.example.storyteller.model;

public class Material {
    private int id;
    private String category;
    private String title;
    private String content;
    private long createTime;

    public Material(String category, String title, String content, long createTime) {
        this.category = category;
        this.title = title;
        this.content = content;
        this.createTime = createTime;
    }

    public Material(int id, String category, String title, String content, long createTime) {
        this.id = id;
        this.category = category;
        this.title = title;
        this.content = content;
        this.createTime = createTime;
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
}
