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
        return db.update(
                DBHelper.TABLE_STORY,
                values,
                DBHelper.COL_STORY_ID + "=?",
                new String[]{String.valueOf(story.getId())}
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

        return new Story(id, title, content, genre, createTime, isCollected, structure, description, plotSummaryJson, category, coverColor, coverPath);
    }
}
