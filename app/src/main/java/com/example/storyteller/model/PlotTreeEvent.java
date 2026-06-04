package com.example.storyteller.model;

import java.util.ArrayList;
import java.util.List;

public class PlotTreeEvent {
    private int id;
    private String title;
    private String summary;
    private String note;
    private List<String> tags;
    private long createTime;
    private long updateTime;

    public PlotTreeEvent() {
        long now = System.currentTimeMillis();
        this.createTime = now;
        this.updateTime = now;
        this.tags = new ArrayList<>();
    }

    public PlotTreeEvent(int id, String title, String summary) {
        this();
        this.id = id;
        this.title = title;
        this.summary = summary;
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

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags == null ? new ArrayList<>() : tags;
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
}

