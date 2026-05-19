package com.example.storyteller.data.local.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.storyteller.model.Story;

import java.util.ArrayList;
import java.util.List;

public class StoryDao {
    private final DBHelper dbHelper;

    public StoryDao(Context context) {
        this.dbHelper = DBHelper.getInstance(context);
    }

    public long insertStory(Story story) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DBHelper.COL_STORY_TITLE, story.getTitle());
        values.put(DBHelper.COL_STORY_CONTENT, story.getContent());
        values.put(DBHelper.COL_STORY_GENRE, story.getGenre());
        values.put(DBHelper.COL_STORY_CREATE_TIME, story.getCreateTime());
        values.put(DBHelper.COL_STORY_IS_COLLECTED, story.isCollected() ? 1 : 0);
        values.put(DBHelper.COL_STORY_STRUCTURE, story.getStructure());
        values.put(DBHelper.COL_STORY_DESCRIPTION, story.getDescription());
        values.put(DBHelper.COL_STORY_PLOT_SUMMARY, story.getPlotSummaryJson());
        values.put(DBHelper.COL_STORY_CATEGORY, story.getCategory());
        values.put(DBHelper.COL_STORY_COVER_COLOR, story.getCoverColor());
        values.put(DBHelper.COL_STORY_COVER_PATH, story.getCoverPath());
        values.put(DBHelper.COL_STORY_WORD_COUNT, story.getWordCount());
        values.put(DBHelper.COL_STORY_SERIES_NAME, story.getSeriesName());
        return db.insert(DBHelper.TABLE_STORY, null, values);
    }

    public int updateStory(Story story) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DBHelper.COL_STORY_TITLE, story.getTitle());
        values.put(DBHelper.COL_STORY_CONTENT, story.getContent());
        values.put(DBHelper.COL_STORY_GENRE, story.getGenre());
        values.put(DBHelper.COL_STORY_IS_COLLECTED, story.isCollected() ? 1 : 0);
        values.put(DBHelper.COL_STORY_STRUCTURE, story.getStructure());
        values.put(DBHelper.COL_STORY_DESCRIPTION, story.getDescription());
        values.put(DBHelper.COL_STORY_PLOT_SUMMARY, story.getPlotSummaryJson());
        values.put(DBHelper.COL_STORY_CATEGORY, story.getCategory());
        values.put(DBHelper.COL_STORY_COVER_COLOR, story.getCoverColor());
        values.put(DBHelper.COL_STORY_COVER_PATH, story.getCoverPath());
        values.put(DBHelper.COL_STORY_WORD_COUNT, story.getWordCount());
        values.put(DBHelper.COL_STORY_SERIES_NAME, story.getSeriesName());
        
        return db.update(
                DBHelper.TABLE_STORY,
                values,
                DBHelper.COL_STORY_ID + "=?",
                new String[]{String.valueOf(story.getId())}
        );
    }

    /**
     * 只更新架构相关字段（标题、分类、标签、简介），不覆盖字数和结构
     */
    public int updateStoryArchitecture(int storyId, String title, String category, String genre, String description) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DBHelper.COL_STORY_TITLE, title);
        values.put(DBHelper.COL_STORY_CATEGORY, category);
        values.put(DBHelper.COL_STORY_GENRE, genre);
        values.put(DBHelper.COL_STORY_DESCRIPTION, description);
        
        return db.update(
                DBHelper.TABLE_STORY,
                values,
                DBHelper.COL_STORY_ID + "=?",
                new String[]{String.valueOf(storyId)}
        );
    }

    /**
     * 只更新写作相关字段（structure、wordCount、content），不覆盖架构信息
     */
    public int updateStoryWriting(int storyId, String structure, int wordCount, String content) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DBHelper.COL_STORY_STRUCTURE, structure);
        values.put(DBHelper.COL_STORY_WORD_COUNT, wordCount);
        values.put(DBHelper.COL_STORY_CONTENT, content);
        values.put(DBHelper.COL_STORY_CREATE_TIME, System.currentTimeMillis());
        
        return db.update(
                DBHelper.TABLE_STORY,
                values,
                DBHelper.COL_STORY_ID + "=?",
                new String[]{String.valueOf(storyId)}
        );
    }

    /**
     * 只更新大纲数据（outline_data），不覆盖其他字段
     */
    public int updateStoryOutline(int storyId, String outlineData) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DBHelper.COL_STORY_OUTLINE_DATA, outlineData);
        values.put(DBHelper.COL_STORY_CREATE_TIME, System.currentTimeMillis());
        
        return db.update(
                DBHelper.TABLE_STORY,
                values,
                DBHelper.COL_STORY_ID + "=?",
                new String[]{String.valueOf(storyId)}
        );
    }
    
    /**
     * 只更新卷章结构（structure），不覆盖其他字段
     */
    public int updateStoryStructure(int storyId, String structure) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DBHelper.COL_STORY_STRUCTURE, structure);
        values.put(DBHelper.COL_STORY_CREATE_TIME, System.currentTimeMillis());
        
        return db.update(
                DBHelper.TABLE_STORY,
                values,
                DBHelper.COL_STORY_ID + "=?",
                new String[]{String.valueOf(storyId)}
        );
    }

    /**
     * 只更新全局大纲（global_outline），不覆盖其他字段
     */
    public int updateStoryGlobalOutline(int storyId, String globalOutline) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DBHelper.COL_STORY_GLOBAL_OUTLINE, globalOutline);
        values.put(DBHelper.COL_STORY_CREATE_TIME, System.currentTimeMillis());
        
        return db.update(
                DBHelper.TABLE_STORY,
                values,
                DBHelper.COL_STORY_ID + "=?",
                new String[]{String.valueOf(storyId)}
        );
    }

    public int updatePlotSummary(int storyId, String plotSummaryJson) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DBHelper.COL_STORY_PLOT_SUMMARY, plotSummaryJson);
        return db.update(
                DBHelper.TABLE_STORY,
                values,
                DBHelper.COL_STORY_ID + "=?",
                new String[]{String.valueOf(storyId)}
        );
    }

    public int deleteStory(int storyId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.delete(DBHelper.TABLE_STORY, DBHelper.COL_STORY_ID + "=?", new String[]{String.valueOf(storyId)});
    }

    public int updateStoryCategory(int storyId, String category) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DBHelper.COL_STORY_CATEGORY, category);
        return db.update(
                DBHelper.TABLE_STORY,
                values,
                DBHelper.COL_STORY_ID + "=?",
                new String[]{String.valueOf(storyId)}
        );
    }

    public int updateStoryCoverPath(int storyId, String coverPath) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DBHelper.COL_STORY_COVER_PATH, coverPath);
        return db.update(
                DBHelper.TABLE_STORY,
                values,
                DBHelper.COL_STORY_ID + "=?",
                new String[]{String.valueOf(storyId)}
        );
    }

    public int updateStoryCoverColor(int storyId, String coverColor) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DBHelper.COL_STORY_COVER_COLOR, coverColor);
        return db.update(
                DBHelper.TABLE_STORY,
                values,
                DBHelper.COL_STORY_ID + "=?",
                new String[]{String.valueOf(storyId)}
        );
    }

    public int updateStoryCollected(int storyId, boolean collected) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DBHelper.COL_STORY_IS_COLLECTED, collected ? 1 : 0);
        return db.update(
                DBHelper.TABLE_STORY,
                values,
                DBHelper.COL_STORY_ID + "=?",
                new String[]{String.valueOf(storyId)}
        );
    }

    /**
     * 增加故事字数（创建章节时调用）
     */
    public void incrementWordCount(int storyId, int wordCount) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.execSQL(
                "UPDATE " + DBHelper.TABLE_STORY + 
                " SET " + DBHelper.COL_STORY_WORD_COUNT + " = " + DBHelper.COL_STORY_WORD_COUNT + " + ?, " +
                DBHelper.COL_STORY_CREATE_TIME + " = ? " +
                "WHERE " + DBHelper.COL_STORY_ID + " = ?",
                new Object[]{wordCount, System.currentTimeMillis(), storyId}
        );
    }

    /**
     * 减少故事字数（删除章节时调用）
     */
    public void decrementWordCount(int storyId, int wordCount) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.execSQL(
                "UPDATE " + DBHelper.TABLE_STORY + 
                " SET " + DBHelper.COL_STORY_WORD_COUNT + " = " + DBHelper.COL_STORY_WORD_COUNT + " - ?, " +
                DBHelper.COL_STORY_CREATE_TIME + " = ? " +
                "WHERE " + DBHelper.COL_STORY_ID + " = ?",
                new Object[]{wordCount, System.currentTimeMillis(), storyId}
        );
    }

    /**
     * 重新计算故事字数（编辑章节或数据修复时调用）
     */
    public void recalculateWordCount(int storyId) {
        Story story = getStoryById(storyId);
        if (story == null) {
            return;
        }
        
        int totalWords = calculateWordCountFromStructure(story.getStructure());
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DBHelper.COL_STORY_WORD_COUNT, totalWords);
        values.put(DBHelper.COL_STORY_CREATE_TIME, System.currentTimeMillis());
        db.update(
                DBHelper.TABLE_STORY,
                values,
                DBHelper.COL_STORY_ID + "=?",
                new String[]{String.valueOf(storyId)}
        );
    }

    /**
     * 批量初始化所有故事的字数（一次性任务）
     */
    public void recalculateAllWordCounts() {
        List<Story> allStories = getAllStories();
        for (Story story : allStories) {
            recalculateWordCount(story.getId());
        }
    }

    /**
     * 从JSON结构计算字数
     */
    private int calculateWordCountFromStructure(String structureJson) {
        if (structureJson == null || structureJson.isEmpty()) {
            return 0;
        }
        
        try {
            // 使用TypeToken解析List<Volume>
            java.util.List<com.example.storyteller.model.Volume> volumes = 
                com.example.storyteller.utils.JsonUtils.fromJson(structureJson,
                    new com.google.gson.reflect.TypeToken<java.util.List<com.example.storyteller.model.Volume>>(){}.getType());
            
            int total = 0;
            if (volumes != null) {
                for (com.example.storyteller.model.Volume volume : volumes) {
                    if (volume.getChapters() != null) {
                        for (com.example.storyteller.model.Chapter chapter : volume.getChapters()) {
                            if (chapter.getContent() != null) {
                                total += chapter.getContent().length();
                            }
                        }
                    }
                }
            }
            return total;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public int setCollected(int storyId, boolean collected) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DBHelper.COL_STORY_IS_COLLECTED, collected ? 1 : 0);
        return db.update(
                DBHelper.TABLE_STORY,
                values,
                DBHelper.COL_STORY_ID + "=?",
                new String[]{String.valueOf(storyId)}
        );
    }

    public Story getStoryById(int storyId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                DBHelper.TABLE_STORY,
                null,
                DBHelper.COL_STORY_ID + "=?",
                new String[]{String.valueOf(storyId)},
                null,
                null,
                null
        );
        Story story = null;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                story = mapStory(cursor);
            }
            cursor.close();
        }
        return story;
    }

    public List<Story> getAllStories() {
        List<Story> stories = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        Cursor cursor = db.query(
                DBHelper.TABLE_STORY,
                null,
                null,
                null,
                null,
                null,
                DBHelper.COL_STORY_CREATE_TIME + " DESC"
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                stories.add(mapStory(cursor));
            }
            cursor.close();
        }
        
        return stories;
    }

    public List<Story> getCollectedStories() {
        List<Story> stories = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                DBHelper.TABLE_STORY,
                null,
                DBHelper.COL_STORY_IS_COLLECTED + "=?",
                new String[]{"1"},
                null,
                null,
                DBHelper.COL_STORY_CREATE_TIME + " DESC"
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                stories.add(mapStory(cursor));
            }
            cursor.close();
        }
        return stories;
    }

    public Story getLatestStory() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                DBHelper.TABLE_STORY,
                null,
                null,
                null,
                null,
                null,
                DBHelper.COL_STORY_CREATE_TIME + " DESC",
                "1"
        );
        Story story = null;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                story = mapStory(cursor);
            }
            cursor.close();
        }
        return story;
    }

    private Story mapStory(Cursor cursor) {
        int id = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COL_STORY_ID));
        String title = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_STORY_TITLE));
        String content = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_STORY_CONTENT));
        String genre = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_STORY_GENRE));
        long createTime = cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.COL_STORY_CREATE_TIME));
        boolean isCollected = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COL_STORY_IS_COLLECTED)) == 1;
        String structure = null;
        int structureIndex = cursor.getColumnIndex(DBHelper.COL_STORY_STRUCTURE);
        if (structureIndex >= 0 && !cursor.isNull(structureIndex)) {
            structure = cursor.getString(structureIndex);
        }
        
        String description = null;
        int descriptionIndex = cursor.getColumnIndex(DBHelper.COL_STORY_DESCRIPTION);
        if (descriptionIndex >= 0 && !cursor.isNull(descriptionIndex)) {
            description = cursor.getString(descriptionIndex);
        }

        String plotSummaryJson = null;
        int plotSummaryIndex = cursor.getColumnIndex(DBHelper.COL_STORY_PLOT_SUMMARY);
        if (plotSummaryIndex >= 0 && !cursor.isNull(plotSummaryIndex)) {
            plotSummaryJson = cursor.getString(plotSummaryIndex);
        }

        String category = "创作中";
        int categoryIndex = cursor.getColumnIndex(DBHelper.COL_STORY_CATEGORY);
        if (categoryIndex >= 0 && !cursor.isNull(categoryIndex)) {
            category = cursor.getString(categoryIndex);
        }

        String coverColor = "#1976D2";
        int coverColorIndex = cursor.getColumnIndex(DBHelper.COL_STORY_COVER_COLOR);
        if (coverColorIndex >= 0 && !cursor.isNull(coverColorIndex)) {
            coverColor = cursor.getString(coverColorIndex);
        }
        
        String coverPath = null;
        int coverPathIndex = cursor.getColumnIndex(DBHelper.COL_STORY_COVER_PATH);
        if (coverPathIndex >= 0 && !cursor.isNull(coverPathIndex)) {
            coverPath = cursor.getString(coverPathIndex);
        }

        int wordCount = 0;
        int wordCountIndex = cursor.getColumnIndex(DBHelper.COL_STORY_WORD_COUNT);
        if (wordCountIndex >= 0 && !cursor.isNull(wordCountIndex)) {
            wordCount = cursor.getInt(wordCountIndex);
        }

        String seriesName = null;
        try {
            int seriesNameIndex = cursor.getColumnIndex(DBHelper.COL_STORY_SERIES_NAME);
            if (seriesNameIndex >= 0 && !cursor.isNull(seriesNameIndex)) {
                seriesName = cursor.getString(seriesNameIndex);
            }
        } catch (Exception e) {
            // 如果系列名字段不存在，使用null
            seriesName = null;
        }
        
        String outlineData = null;
        int outlineDataIndex = cursor.getColumnIndex(DBHelper.COL_STORY_OUTLINE_DATA);
        if (outlineDataIndex >= 0 && !cursor.isNull(outlineDataIndex)) {
            outlineData = cursor.getString(outlineDataIndex);
        }
        
        String globalOutline = null;
        int globalOutlineIndex = cursor.getColumnIndex(DBHelper.COL_STORY_GLOBAL_OUTLINE);
        if (globalOutlineIndex >= 0 && !cursor.isNull(globalOutlineIndex)) {
            globalOutline = cursor.getString(globalOutlineIndex);
        }

        Story story = new Story(id, title, content, genre, createTime, isCollected, structure, description, plotSummaryJson, category, coverColor, coverPath, wordCount, seriesName);
        story.setOutlineData(outlineData);
        story.setGlobalOutline(globalOutline);
        return story;
    }
}
