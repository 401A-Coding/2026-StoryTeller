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
        return db.insert(DBHelper.TABLE_MATERIAL, null, values);
    }

    public int update(Material material) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DBHelper.COL_MATERIAL_CATEGORY, material.getCategory());
        values.put(DBHelper.COL_MATERIAL_TITLE, material.getTitle());
        values.put(DBHelper.COL_MATERIAL_CONTENT, material.getContent());
        return db.update(DBHelper.TABLE_MATERIAL, values, DBHelper.COL_MATERIAL_ID + "=?", new String[]{String.valueOf(material.getId())});
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
        return new Material(id, category, title, content, createTime);
    }
}
