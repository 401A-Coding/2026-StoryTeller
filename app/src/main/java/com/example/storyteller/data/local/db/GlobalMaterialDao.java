package com.example.storyteller.data.local.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.example.storyteller.model.GlobalMaterial;
import java.util.ArrayList;
import java.util.List;

/**
 * 全局素材库数据访问对象
 */
public class GlobalMaterialDao {
    private final DBHelper dbHelper;

    public GlobalMaterialDao(Context context) {
        this.dbHelper = DBHelper.getInstance(context);
    }

    /**
     * 插入全局素材
     */
    public long insert(GlobalMaterial material) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        
        values.put(DBHelper.COL_GLOBAL_MATERIAL_CATEGORY, material.getCategory());
        values.put(DBHelper.COL_GLOBAL_MATERIAL_SUB_CATEGORY, material.getSubCategory());
        values.put(DBHelper.COL_GLOBAL_MATERIAL_TITLE, material.getTitle());
        values.put(DBHelper.COL_GLOBAL_MATERIAL_SUMMARY, material.getSummary());
        values.put(DBHelper.COL_GLOBAL_MATERIAL_DETAIL, material.getDetail());
        values.put(DBHelper.COL_GLOBAL_MATERIAL_ATTRIBUTES, material.getAttributes());
        values.put(DBHelper.COL_GLOBAL_MATERIAL_TAGS, material.getTags());
        values.put(DBHelper.COL_GLOBAL_MATERIAL_ALIASES, material.getAliases());
        values.put(DBHelper.COL_GLOBAL_MATERIAL_SPECIFIC_ATTRS, material.getSpecificAttributes());
        values.put(DBHelper.COL_GLOBAL_MATERIAL_SOURCE_TYPE, material.getSourceType());
        values.put(DBHelper.COL_GLOBAL_MATERIAL_SOURCE_URL, material.getSourceUrl());
        values.put(DBHelper.COL_GLOBAL_MATERIAL_AI_CONFIDENCE, material.getAiConfidence());
        values.put(DBHelper.COL_GLOBAL_MATERIAL_RAW_JSON, material.getRawJson());
        values.put(DBHelper.COL_GLOBAL_MATERIAL_CREATE_TIME, material.getCreateTime());
        values.put(DBHelper.COL_GLOBAL_MATERIAL_UPDATE_TIME, material.getUpdateTime());
        values.put(DBHelper.COL_GLOBAL_MATERIAL_USAGE_COUNT, material.getUsageCount());
        values.put(DBHelper.COL_GLOBAL_MATERIAL_IS_PUBLIC, material.isPublic() ? 1 : 0);
        
        return db.insert(DBHelper.TABLE_GLOBAL_MATERIAL, null, values);
    }

    /**
     * 更新全局素材
     */
    public int update(GlobalMaterial material) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        
        values.put(DBHelper.COL_GLOBAL_MATERIAL_CATEGORY, material.getCategory());
        values.put(DBHelper.COL_GLOBAL_MATERIAL_SUB_CATEGORY, material.getSubCategory());
        values.put(DBHelper.COL_GLOBAL_MATERIAL_TITLE, material.getTitle());
        values.put(DBHelper.COL_GLOBAL_MATERIAL_SUMMARY, material.getSummary());
        values.put(DBHelper.COL_GLOBAL_MATERIAL_DETAIL, material.getDetail());
        values.put(DBHelper.COL_GLOBAL_MATERIAL_ATTRIBUTES, material.getAttributes());
        values.put(DBHelper.COL_GLOBAL_MATERIAL_TAGS, material.getTags());
        values.put(DBHelper.COL_GLOBAL_MATERIAL_ALIASES, material.getAliases());
        values.put(DBHelper.COL_GLOBAL_MATERIAL_SPECIFIC_ATTRS, material.getSpecificAttributes());
        values.put(DBHelper.COL_GLOBAL_MATERIAL_SOURCE_TYPE, material.getSourceType());
        values.put(DBHelper.COL_GLOBAL_MATERIAL_SOURCE_URL, material.getSourceUrl());
        values.put(DBHelper.COL_GLOBAL_MATERIAL_AI_CONFIDENCE, material.getAiConfidence());
        values.put(DBHelper.COL_GLOBAL_MATERIAL_RAW_JSON, material.getRawJson());
        values.put(DBHelper.COL_GLOBAL_MATERIAL_UPDATE_TIME, System.currentTimeMillis());
        values.put(DBHelper.COL_GLOBAL_MATERIAL_USAGE_COUNT, material.getUsageCount());
        values.put(DBHelper.COL_GLOBAL_MATERIAL_IS_PUBLIC, material.isPublic() ? 1 : 0);
        
        return db.update(DBHelper.TABLE_GLOBAL_MATERIAL, values,
                DBHelper.COL_GLOBAL_MATERIAL_ID + "=?",
                new String[]{String.valueOf(material.getId())});
    }

    /**
     * 删除全局素材
     */
    public int delete(int id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.delete(DBHelper.TABLE_GLOBAL_MATERIAL,
                DBHelper.COL_GLOBAL_MATERIAL_ID + "=?",
                new String[]{String.valueOf(id)});
    }

    /**
     * 根据ID查询
     */
    public GlobalMaterial getById(int id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                DBHelper.TABLE_GLOBAL_MATERIAL,
                null,
                DBHelper.COL_GLOBAL_MATERIAL_ID + "=?",
                new String[]{String.valueOf(id)},
                null, null, null
        );
        try {
            if (cursor.moveToFirst()) {
                return mapCursor(cursor);
            }
            return null;
        } finally {
            cursor.close();
        }
    }

    /**
     * 查询所有全局素材
     */
    public List<GlobalMaterial> getAll() {
        List<GlobalMaterial> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                DBHelper.TABLE_GLOBAL_MATERIAL,
                null,
                null, null, null, null,
                DBHelper.COL_GLOBAL_MATERIAL_UPDATE_TIME + " DESC"
        );
        try {
            while (cursor.moveToNext()) {
                list.add(mapCursor(cursor));
            }
        } finally {
            cursor.close();
        }
        return list;
    }

    /**
     * 按分类查询
     */
    public List<GlobalMaterial> getByCategory(String category) {
        List<GlobalMaterial> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                DBHelper.TABLE_GLOBAL_MATERIAL,
                null,
                DBHelper.COL_GLOBAL_MATERIAL_CATEGORY + "=?",
                new String[]{category},
                null, null,
                DBHelper.COL_GLOBAL_MATERIAL_UPDATE_TIME + " DESC"
        );
        try {
            while (cursor.moveToNext()) {
                list.add(mapCursor(cursor));
            }
        } finally {
            cursor.close();
        }
        return list;
    }

    /**
     * 按子分类查询
     */
    public List<GlobalMaterial> getBySubCategory(String subCategory) {
        List<GlobalMaterial> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                DBHelper.TABLE_GLOBAL_MATERIAL,
                null,
                DBHelper.COL_GLOBAL_MATERIAL_SUB_CATEGORY + "=?",
                new String[]{subCategory},
                null, null,
                DBHelper.COL_GLOBAL_MATERIAL_UPDATE_TIME + " DESC"
        );
        try {
            while (cursor.moveToNext()) {
                list.add(mapCursor(cursor));
            }
        } finally {
            cursor.close();
        }
        return list;
    }

    /**
     * 关键词搜索
     */
    public List<GlobalMaterial> searchByKeyword(String keyword) {
        List<GlobalMaterial> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String like = "%" + keyword + "%";
        Cursor cursor = db.query(
                DBHelper.TABLE_GLOBAL_MATERIAL,
                null,
                DBHelper.COL_GLOBAL_MATERIAL_TITLE + " LIKE ? OR " +
                DBHelper.COL_GLOBAL_MATERIAL_SUMMARY + " LIKE ? OR " +
                DBHelper.COL_GLOBAL_MATERIAL_DETAIL + " LIKE ?",
                new String[]{like, like, like},
                null, null,
                DBHelper.COL_GLOBAL_MATERIAL_UPDATE_TIME + " DESC"
        );
        try {
            while (cursor.moveToNext()) {
                list.add(mapCursor(cursor));
            }
        } finally {
            cursor.close();
        }
        return list;
    }

    /**
     * 增加使用次数
     */
    public void incrementUsageCount(int id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.execSQL(
            "UPDATE " + DBHelper.TABLE_GLOBAL_MATERIAL + 
            " SET " + DBHelper.COL_GLOBAL_MATERIAL_USAGE_COUNT + "=" + 
            DBHelper.COL_GLOBAL_MATERIAL_USAGE_COUNT + "+1 WHERE " +
            DBHelper.COL_GLOBAL_MATERIAL_ID + "=?",
            new Object[]{id}
        );
    }

    /**
     * 将Cursor映射为GlobalMaterial对象
     */
    private GlobalMaterial mapCursor(Cursor cursor) {
        GlobalMaterial material = new GlobalMaterial();
        material.setId(cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COL_GLOBAL_MATERIAL_ID)));
        material.setCategory(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_GLOBAL_MATERIAL_CATEGORY)));
        material.setSubCategory(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_GLOBAL_MATERIAL_SUB_CATEGORY)));
        material.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_GLOBAL_MATERIAL_TITLE)));
        material.setSummary(getColumnString(cursor, DBHelper.COL_GLOBAL_MATERIAL_SUMMARY));
        material.setDetail(getColumnString(cursor, DBHelper.COL_GLOBAL_MATERIAL_DETAIL));
        material.setAttributes(getColumnString(cursor, DBHelper.COL_GLOBAL_MATERIAL_ATTRIBUTES));
        material.setTags(getColumnString(cursor, DBHelper.COL_GLOBAL_MATERIAL_TAGS));
        material.setAliases(getColumnString(cursor, DBHelper.COL_GLOBAL_MATERIAL_ALIASES));
        material.setSpecificAttributes(getColumnString(cursor, DBHelper.COL_GLOBAL_MATERIAL_SPECIFIC_ATTRS));
        material.setSourceType(getColumnString(cursor, DBHelper.COL_GLOBAL_MATERIAL_SOURCE_TYPE));
        material.setSourceUrl(getColumnString(cursor, DBHelper.COL_GLOBAL_MATERIAL_SOURCE_URL));
        material.setAiConfidence(getColumnDouble(cursor, DBHelper.COL_GLOBAL_MATERIAL_AI_CONFIDENCE, 0.5));
        material.setRawJson(getColumnString(cursor, DBHelper.COL_GLOBAL_MATERIAL_RAW_JSON));
        material.setCreateTime(cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.COL_GLOBAL_MATERIAL_CREATE_TIME)));
        material.setUpdateTime(cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.COL_GLOBAL_MATERIAL_UPDATE_TIME)));
        material.setUsageCount(cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COL_GLOBAL_MATERIAL_USAGE_COUNT)));
        material.setPublic(cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COL_GLOBAL_MATERIAL_IS_PUBLIC)) == 1);
        return material;
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
