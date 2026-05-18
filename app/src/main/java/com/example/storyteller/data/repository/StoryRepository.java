package com.example.storyteller.data.repository;

import com.example.storyteller.model.Story;
import java.util.List;

/**
 * 故事数据仓库接口
 * 用于解耦数据访问层和业务逻辑层
 */
public interface StoryRepository {
    
    /**
     * 根据ID获取故事
     */
    Story getStoryById(int storyId);
    
    /**
     * 更新故事
     */
    int updateStory(Story story);
    
    /**
     * 只更新写作相关字段（structure、wordCount、content）
     */
    int updateStoryWriting(int storyId, String structure, int wordCount, String content);
    
    /**
     * 只更新大纲数据（outline_data）
     */
    int updateStoryOutline(int storyId, String outlineData);
    
    /**
     * 只更新全局大纲（global_outline）
     */
    int updateStoryGlobalOutline(int storyId, String globalOutline);
    
    /**
     * 获取所有故事
     */
    List<Story> getAllStories();
    
    /**
     * 插入新故事
     */
    long insertStory(Story story);
    
    /**
     * 删除故事
     */
    int deleteStory(int storyId);
    
    /**
     * 减少故事字数
     */
    void decrementWordCount(int storyId, int wordCount);
}
