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
}
