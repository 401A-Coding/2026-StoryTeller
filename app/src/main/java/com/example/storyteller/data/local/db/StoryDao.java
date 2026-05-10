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
        return db.insert(DBHelper.TABLE_STORY, null, values);
    }

    public int updateStory(Story story) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DBHelper.COL_STORY_TITLE, story.getTitle());
        values.put(DBHelper.COL_STORY_CONTENT, story.getContent());
        values.put(DBHelper.COL_STORY_GENRE, story.getGenre());
        values.put(DBHelper.COL_STORY_IS_COLLECTED, story.isCollected() ? 1 : 0);
        return db.update(
                DBHelper.TABLE_STORY,
                values,
                DBHelper.COL_STORY_ID + "=?",
                new String[]{String.valueOf(story.getId())}
        );
    }

    public int deleteStory(int storyId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.delete(DBHelper.TABLE_STORY, DBHelper.COL_STORY_ID + "=?", new String[]{String.valueOf(storyId)});
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
        return new Story(id, title, content, genre, createTime, isCollected);
    }
}
