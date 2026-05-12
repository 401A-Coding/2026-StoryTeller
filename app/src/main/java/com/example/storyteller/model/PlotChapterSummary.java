package com.example.storyteller.model;

import java.util.ArrayList;
import java.util.List;

public class PlotChapterSummary {
    private int volumeIndex;
    private int chapterIndex;
    private String chapterLabel;
    private String chapterTitle;
    private String briefSummary;
    private String detailSummary;
    private List<String> keyEvents;
    private List<String> characters;
    private String conflict;
    private String storyFunction;
    private String source;

    public PlotChapterSummary() {
        this.keyEvents = new ArrayList<>();
        this.characters = new ArrayList<>();
    }

    public int getVolumeIndex() {
        return volumeIndex;
    }

    public void setVolumeIndex(int volumeIndex) {
        this.volumeIndex = volumeIndex;
    }

    public int getChapterIndex() {
        return chapterIndex;
    }

    public void setChapterIndex(int chapterIndex) {
        this.chapterIndex = chapterIndex;
    }

    public String getChapterLabel() {
        return chapterLabel;
    }

    public void setChapterLabel(String chapterLabel) {
        this.chapterLabel = chapterLabel;
    }

    public String getChapterTitle() {
        return chapterTitle;
    }

    public void setChapterTitle(String chapterTitle) {
        this.chapterTitle = chapterTitle;
    }

    public String getBriefSummary() {
        return briefSummary;
    }

    public void setBriefSummary(String briefSummary) {
        this.briefSummary = briefSummary;
    }

    public String getDetailSummary() {
        return detailSummary;
    }

    public void setDetailSummary(String detailSummary) {
        this.detailSummary = detailSummary;
    }

    public List<String> getKeyEvents() {
        return keyEvents;
    }

    public void setKeyEvents(List<String> keyEvents) {
        this.keyEvents = keyEvents == null ? new ArrayList<>() : keyEvents;
    }

    public List<String> getCharacters() {
        return characters;
    }

    public void setCharacters(List<String> characters) {
        this.characters = characters == null ? new ArrayList<>() : characters;
    }

    public String getConflict() {
        return conflict;
    }

    public void setConflict(String conflict) {
        this.conflict = conflict;
    }

    public String getStoryFunction() {
        return storyFunction;
    }

    public void setStoryFunction(String storyFunction) {
        this.storyFunction = storyFunction;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}

