package com.example.storyteller.model;

/**
 * 章节（Chapter）模型类
 * 用于表示小说中的一个章节，包含标题和正文内容
 */
public class Chapter {
    private int id;
    private String title;
    private String content;

    public Chapter() {
    }

    public Chapter(String title) {
        this.title = title;
        this.content = "";
    }

    public Chapter(int id, String title, String content) {
        this.id = id;
        this.title = title;
        this.content = content;
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
}
