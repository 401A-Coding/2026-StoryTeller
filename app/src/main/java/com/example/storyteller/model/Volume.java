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

    public Volume() {
        this.chapters = new ArrayList<>();
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
}
