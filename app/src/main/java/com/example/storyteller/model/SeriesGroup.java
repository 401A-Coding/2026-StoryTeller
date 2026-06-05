package com.example.storyteller.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 系列分组模型，用于书架中同一系列（seriesName）的折叠展示
 */
public class SeriesGroup {

    private final String seriesName;
    private final List<Story> stories;

    public SeriesGroup(String seriesName) {
        this.seriesName = seriesName;
        this.stories = new ArrayList<>();
    }

    public String getSeriesName() {
        return seriesName;
    }

    public List<Story> getStories() {
        return stories;
    }

    public void addStory(Story story) {
        stories.add(story);
    }

    public int getStoryCount() {
        return stories.size();
    }

    /**
     * 获取总字数（所有子故事的字数之和）
     */
    public int getTotalWordCount() {
        int total = 0;
        for (Story s : stories) {
            total += s.getWordCount();
        }
        return total;
    }

    /**
     * 获取第一个故事（用于显示封面预览等）
     */
    public Story getFirstStory() {
        return stories.isEmpty() ? null : stories.get(0);
    }
}
