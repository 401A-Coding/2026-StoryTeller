package com.example.storyteller.data.local.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.storyteller.model.Character;

import java.util.ArrayList;
import java.util.List;

public class CharacterDao {
    private final DBHelper dbHelper;

    public CharacterDao(Context context) {
        this.dbHelper = DBHelper.getInstance(context);
    }

    public List<Character> getCharactersByStoryId(int storyId) {
        List<Character> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                DBHelper.TABLE_CHARACTER,
                null,
                DBHelper.COL_CHARACTER_STORY_ID + "=?",
                new String[]{String.valueOf(storyId)},
                null,
                null,
                DBHelper.COL_CHARACTER_ID + " ASC"
        );

        while (cursor.moveToNext()) {
            list.add(map(cursor));
        }
        cursor.close();
        return list;
    }

    public void replaceCharactersForStory(int storyId, List<Character> characters) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete(DBHelper.TABLE_CHARACTER, DBHelper.COL_CHARACTER_STORY_ID + "=?", new String[]{String.valueOf(storyId)});
            if (characters != null) {
                for (Character character : characters) {
                    ContentValues values = new ContentValues();
                    values.put(DBHelper.COL_CHARACTER_STORY_ID, storyId);
                    values.put(DBHelper.COL_CHARACTER_NAME, character.getName());
                    values.put(DBHelper.COL_CHARACTER_PROFILE, character.getProfile());
                    values.put(DBHelper.COL_CHARACTER_DETAIL, character.getDetail());
                    values.put(DBHelper.COL_CHARACTER_AVATAR, character.getAvatarResId());
                    db.insert(DBHelper.TABLE_CHARACTER, null, values);
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public int deleteCharactersByStoryId(int storyId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.delete(DBHelper.TABLE_CHARACTER, DBHelper.COL_CHARACTER_STORY_ID + "=?", new String[]{String.valueOf(storyId)});
    }

    public int deleteCharacterById(int characterId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.delete(DBHelper.TABLE_CHARACTER, DBHelper.COL_CHARACTER_ID + "=?", new String[]{String.valueOf(characterId)});
    }

    private Character map(Cursor cursor) {
        int id = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COL_CHARACTER_ID));
        int storyId = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COL_CHARACTER_STORY_ID));
        String name = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_CHARACTER_NAME));
        String profile = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_CHARACTER_PROFILE));
        String detail = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_CHARACTER_DETAIL));
        int avatar = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COL_CHARACTER_AVATAR));

        Character character = new Character(storyId, name, profile, detail, avatar);
        character.setId(id);
        return character;
    }
}

