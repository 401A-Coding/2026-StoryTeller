package com.example.storyteller.data.local.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.storyteller.model.Material;

import java.util.ArrayList;
import java.util.List;

public class MaterialDao {
    private final DBHelper dbHelper;

    public MaterialDao(Context context) {
        this.dbHelper = DBHelper.getInstance(context);
    }

    public long insert(Material material) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DBHelper.COL_MATERIAL_CATEGORY, material.getCategory());
        values.put(DBHelper.COL_MATERIAL_TITLE, material.getTitle());
        values.put(DBHelper.COL_MATERIAL_CONTENT, material.getContent());
        values.put(DBHelper.COL_MATERIAL_CREATE_TIME, material.getCreateTime());
        values.put(DBHelper.COL_MATERIAL_SOURCE_URL, material.getSourceUrl());
        values.put(DBHelper.COL_MATERIAL_SOURCE_TITLE, material.getSourceTitle());
        values.put(DBHelper.COL_MATERIAL_SOURCE_TYPE, material.getSourceType());
        values.put(DBHelper.COL_MATERIAL_AI_SCORE, material.getAiScore());
        values.put(DBHelper.COL_MATERIAL_RAW_JSON, material.getRawJson());
        return db.insert(DBHelper.TABLE_MATERIAL, null, values);
    }

    public int update(Material material) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DBHelper.COL_MATERIAL_CATEGORY, material.getCategory());
        values.put(DBHelper.COL_MATERIAL_TITLE, material.getTitle());
        values.put(DBHelper.COL_MATERIAL_CONTENT, material.getContent());
        values.put(DBHelper.COL_MATERIAL_SOURCE_URL, material.getSourceUrl());
        values.put(DBHelper.COL_MATERIAL_SOURCE_TITLE, material.getSourceTitle());
        values.put(DBHelper.COL_MATERIAL_SOURCE_TYPE, material.getSourceType());
        values.put(DBHelper.COL_MATERIAL_AI_SCORE, material.getAiScore());
        values.put(DBHelper.COL_MATERIAL_RAW_JSON, material.getRawJson());
        return db.update(DBHelper.TABLE_MATERIAL, values, DBHelper.COL_MATERIAL_ID + "=?", new String[]{String.valueOf(material.getId())});
    }

    public Material getById(int materialId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                DBHelper.TABLE_MATERIAL,
                null,
                DBHelper.COL_MATERIAL_ID + "=?",
                new String[]{String.valueOf(materialId)},
                null,
                null,
                null
        );
        try {
            if (cursor.moveToFirst()) {
                return map(cursor);
            }
            return null;
        } finally {
            cursor.close();
        }
    }

    public long replaceBySource(String sourceUrl, List<Material> materials) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete(DBHelper.TABLE_MATERIAL, DBHelper.COL_MATERIAL_SOURCE_URL + "=?", new String[]{sourceUrl});
            long lastId = -1;
            if (materials != null) {
                for (Material material : materials) {
                    lastId = insertInTransaction(db, material);
                }
            }
            db.setTransactionSuccessful();
            return lastId;
        } finally {
            db.endTransaction();
        }
    }

    private long insertInTransaction(SQLiteDatabase db, Material material) {
        ContentValues values = new ContentValues();
        values.put(DBHelper.COL_MATERIAL_CATEGORY, material.getCategory());
        values.put(DBHelper.COL_MATERIAL_TITLE, material.getTitle());
        values.put(DBHelper.COL_MATERIAL_CONTENT, material.getContent());
        values.put(DBHelper.COL_MATERIAL_CREATE_TIME, material.getCreateTime());
        values.put(DBHelper.COL_MATERIAL_SOURCE_URL, material.getSourceUrl());
        values.put(DBHelper.COL_MATERIAL_SOURCE_TITLE, material.getSourceTitle());
        values.put(DBHelper.COL_MATERIAL_SOURCE_TYPE, material.getSourceType());
        values.put(DBHelper.COL_MATERIAL_AI_SCORE, material.getAiScore());
        values.put(DBHelper.COL_MATERIAL_RAW_JSON, material.getRawJson());
        return db.insert(DBHelper.TABLE_MATERIAL, null, values);
    }

    public int delete(int materialId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.delete(DBHelper.TABLE_MATERIAL, DBHelper.COL_MATERIAL_ID + "=?", new String[]{String.valueOf(materialId)});
    }

    public List<Material> getAll() {
        List<Material> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DBHelper.TABLE_MATERIAL, null, null, null, null, null, DBHelper.COL_MATERIAL_CREATE_TIME + " DESC");
        if (cursor != null) {
            while (cursor.moveToNext()) {
                list.add(map(cursor));
            }
            cursor.close();
        }
        return list;
    }

    public List<Material> searchByKeyword(String keyword) {
        List<Material> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String like = "%" + keyword + "%";
        Cursor cursor = db.query(
                DBHelper.TABLE_MATERIAL,
                null,
                DBHelper.COL_MATERIAL_TITLE + " LIKE ? OR " + DBHelper.COL_MATERIAL_CONTENT + " LIKE ?",
                new String[]{like, like},
                null,
                null,
                DBHelper.COL_MATERIAL_CREATE_TIME + " DESC"
        );
        if (cursor != null) {
            while (cursor.moveToNext()) {
                list.add(map(cursor));
            }
            cursor.close();
        }
        return list;
    }

    public List<Material> getByCategory(String category) {
        List<Material> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                DBHelper.TABLE_MATERIAL,
                null,
                DBHelper.COL_MATERIAL_CATEGORY + "=?",
                new String[]{category},
                null,
                null,
                DBHelper.COL_MATERIAL_CREATE_TIME + " DESC"
        );
        if (cursor != null) {
            while (cursor.moveToNext()) {
                list.add(map(cursor));
            }
            cursor.close();
        }
        return list;
    }

    private Material map(Cursor cursor) {
        int id = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COL_MATERIAL_ID));
        String category = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_MATERIAL_CATEGORY));
        String title = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_MATERIAL_TITLE));
        String content = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_MATERIAL_CONTENT));
        long createTime = cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.COL_MATERIAL_CREATE_TIME));
        String sourceUrl = getColumnString(cursor, DBHelper.COL_MATERIAL_SOURCE_URL);
        String sourceTitle = getColumnString(cursor, DBHelper.COL_MATERIAL_SOURCE_TITLE);
        String sourceType = getColumnString(cursor, DBHelper.COL_MATERIAL_SOURCE_TYPE);
        double aiScore = getColumnDouble(cursor, DBHelper.COL_MATERIAL_AI_SCORE, 0d);
        String rawJson = getColumnString(cursor, DBHelper.COL_MATERIAL_RAW_JSON);
        return new Material(id, category, title, content, createTime, sourceUrl, sourceTitle, sourceType, aiScore, rawJson);
    }

    private String getColumnString(Cursor cursor, String column) {
        int index = cursor.getColumnIndex(column);
        if (index < 0 || cursor.isNull(index)) {
            return null;
        }
        return cursor.getString(index);
    }

    private double getColumnDouble(Cursor cursor, String column, double defaultValue) {
        int index = cursor.getColumnIndex(column);
        if (index < 0 || cursor.isNull(index)) {
            return defaultValue;
        }
        return cursor.getDouble(index);
    }
}
