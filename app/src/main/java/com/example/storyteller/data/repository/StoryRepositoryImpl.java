package com.example.storyteller.data.repository;

import android.content.Context;
import com.example.storyteller.data.local.db.StoryDao;
import com.example.storyteller.model.Story;
import java.util.List;

/**
 * 故事数据仓库实现类
 */
public class StoryRepositoryImpl implements StoryRepository {
    
    private final StoryDao storyDao;
    
    public StoryRepositoryImpl(Context context) {
        this.storyDao = new StoryDao(context);
    }
    
    @Override
    public Story getStoryById(int storyId) {
        return storyDao.getStoryById(storyId);
    }
    
    @Override
    public int updateStory(Story story) {
        return storyDao.updateStory(story);
    }
    
    @Override
    public int updateStoryWriting(int storyId, String structure, int wordCount, String content) {
        return storyDao.updateStoryWriting(storyId, structure, wordCount, content);
    }
    
    @Override
    public List<Story> getAllStories() {
        return storyDao.getAllStories();
    }
    
    @Override
    public long insertStory(Story story) {
        return storyDao.insertStory(story);
    }
    
    @Override
    public int deleteStory(int storyId) {
        return storyDao.deleteStory(storyId);
    }
    
    @Override
    public void decrementWordCount(int storyId, int wordCount) {
        storyDao.decrementWordCount(storyId, wordCount);
    }
}
