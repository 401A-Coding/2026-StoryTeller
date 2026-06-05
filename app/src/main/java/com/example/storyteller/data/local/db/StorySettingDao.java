package com.example.storyteller.data.local.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.example.storyteller.model.StorySetting;
import java.util.ArrayList;
import java.util.List;

/**
 * 小说设定数据访问对象
 */
public class StorySettingDao {
    private final DBHelper dbHelper;

    public StorySettingDao(Context context) {
        this.dbHelper = DBHelper.getInstance(context);
    }

    /**
     * 插入设定
     */
    public long insert(StorySetting setting) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        
        values.put(DBHelper.COL_STORY_SETTING_STORY_ID, setting.getStoryId());
        values.put(DBHelper.COL_STORY_SETTING_CATEGORY, setting.getCategory());
        values.put(DBHelper.COL_STORY_SETTING_SUB_CATEGORY, setting.getSubCategory());
        values.put(DBHelper.COL_STORY_SETTING_TITLE, setting.getTitle());
        values.put(DBHelper.COL_STORY_SETTING_SUMMARY, setting.getSummary());
        values.put(DBHelper.COL_STORY_SETTING_DETAIL, setting.getDetail());
        values.put(DBHelper.COL_STORY_SETTING_ATTRIBUTES, setting.getAttributes());
        values.put(DBHelper.COL_STORY_SETTING_TAGS, setting.getTags());
        values.put(DBHelper.COL_STORY_SETTING_ALIASES, setting.getAliases());
        values.put(DBHelper.COL_STORY_SETTING_SPECIFIC_ATTRS, setting.getSpecificAttributes());
        values.put(DBHelper.COL_STORY_SETTING_SOURCE_MATERIAL_ID, setting.getSourceMaterialId());
        values.put(DBHelper.COL_STORY_SETTING_SOURCE_TYPE, setting.getSourceType());
        values.put(DBHelper.COL_STORY_SETTING_SOURCE_URL, setting.getSourceUrl());
        values.put(DBHelper.COL_STORY_SETTING_SOURCE_TITLE, setting.getSourceTitle());
        values.put(DBHelper.COL_STORY_SETTING_AI_CONFIDENCE, setting.getAiConfidence());
        values.put(DBHelper.COL_STORY_SETTING_RAW_JSON, setting.getRawJson());
        values.put(DBHelper.COL_STORY_SETTING_IMPORT_TIME, setting.getImportTime());
        values.put(DBHelper.COL_STORY_SETTING_LAST_SYNC_TIME, setting.getLastSyncTime());
        values.put(DBHelper.COL_STORY_SETTING_SYNC_ENABLED, setting.isSyncEnabled() ? 1 : 0);
        values.put(DBHelper.COL_STORY_SETTING_HAS_UPDATES, setting.isHasUpdates() ? 1 : 0);
        values.put(DBHelper.COL_STORY_SETTING_CREATE_TIME, setting.getCreateTime());
        values.put(DBHelper.COL_STORY_SETTING_UPDATE_TIME, setting.getUpdateTime());
        values.put(DBHelper.COL_STORY_SETTING_IS_FAVORITE, setting.isFavorite() ? 1 : 0);
        values.put(DBHelper.COL_STORY_SETTING_USAGE_COUNT, setting.getUsageCount());
        values.put(DBHelper.COL_STORY_SETTING_IMAGE_PATH, setting.getImagePath());
        values.put(DBHelper.COL_STORY_SETTING_PRESET_TEMPLATE_ID, setting.getPresetTemplateId());
        values.put(DBHelper.COL_STORY_SETTING_PRESET_VERSION, setting.getPresetVersion());

        return db.insert(DBHelper.TABLE_STORY_SETTING, null, values);
    }

    /**
     * 更新设定
     */
    public int update(StorySetting setting) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        
        values.put(DBHelper.COL_STORY_SETTING_CATEGORY, setting.getCategory());
        values.put(DBHelper.COL_STORY_SETTING_SUB_CATEGORY, setting.getSubCategory());
        values.put(DBHelper.COL_STORY_SETTING_TITLE, setting.getTitle());
        values.put(DBHelper.COL_STORY_SETTING_SUMMARY, setting.getSummary());
        values.put(DBHelper.COL_STORY_SETTING_DETAIL, setting.getDetail());
        values.put(DBHelper.COL_STORY_SETTING_ATTRIBUTES, setting.getAttributes());
        values.put(DBHelper.COL_STORY_SETTING_TAGS, setting.getTags());
        values.put(DBHelper.COL_STORY_SETTING_ALIASES, setting.getAliases());
        values.put(DBHelper.COL_STORY_SETTING_SPECIFIC_ATTRS, setting.getSpecificAttributes());
        values.put(DBHelper.COL_STORY_SETTING_SOURCE_TYPE, setting.getSourceType());
        values.put(DBHelper.COL_STORY_SETTING_SOURCE_URL, setting.getSourceUrl());
        values.put(DBHelper.COL_STORY_SETTING_SOURCE_TITLE, setting.getSourceTitle());
        values.put(DBHelper.COL_STORY_SETTING_AI_CONFIDENCE, setting.getAiConfidence());
        values.put(DBHelper.COL_STORY_SETTING_RAW_JSON, setting.getRawJson());
        values.put(DBHelper.COL_STORY_SETTING_LAST_SYNC_TIME, setting.getLastSyncTime());
        values.put(DBHelper.COL_STORY_SETTING_SYNC_ENABLED, setting.isSyncEnabled() ? 1 : 0);
        values.put(DBHelper.COL_STORY_SETTING_HAS_UPDATES, setting.isHasUpdates() ? 1 : 0);
        values.put(DBHelper.COL_STORY_SETTING_UPDATE_TIME, System.currentTimeMillis());
        values.put(DBHelper.COL_STORY_SETTING_IS_FAVORITE, setting.isFavorite() ? 1 : 0);
        values.put(DBHelper.COL_STORY_SETTING_USAGE_COUNT, setting.getUsageCount());
        values.put(DBHelper.COL_STORY_SETTING_IMAGE_PATH, setting.getImagePath());
        values.put(DBHelper.COL_STORY_SETTING_PRESET_TEMPLATE_ID, setting.getPresetTemplateId());
        values.put(DBHelper.COL_STORY_SETTING_PRESET_VERSION, setting.getPresetVersion());

        return db.update(DBHelper.TABLE_STORY_SETTING, values,
                DBHelper.COL_STORY_SETTING_ID + "=?",
                new String[]{String.valueOf(setting.getId())});
    }

    /**
     * 删除设定
     */
    public int delete(int id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.delete(DBHelper.TABLE_STORY_SETTING,
                DBHelper.COL_STORY_SETTING_ID + "=?",
                new String[]{String.valueOf(id)});
    }

    /**
     * 根据ID查询
     */
    public StorySetting getById(int id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                DBHelper.TABLE_STORY_SETTING,
                null,
                DBHelper.COL_STORY_SETTING_ID + "=?",
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
     * 查询某小说的所有设定
     */
    public List<StorySetting> getByStoryId(int storyId) {
        List<StorySetting> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                DBHelper.TABLE_STORY_SETTING,
                null,
                DBHelper.COL_STORY_SETTING_STORY_ID + "=?",
                new String[]{String.valueOf(storyId)},
                null, null,
                DBHelper.COL_STORY_SETTING_UPDATE_TIME + " DESC"
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
    public List<StorySetting> getByCategory(int storyId, String category) {
        List<StorySetting> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                DBHelper.TABLE_STORY_SETTING,
                null,
                DBHelper.COL_STORY_SETTING_STORY_ID + "=? AND " + 
                DBHelper.COL_STORY_SETTING_CATEGORY + "=?",
                new String[]{String.valueOf(storyId), category},
                null, null,
                DBHelper.COL_STORY_SETTING_UPDATE_TIME + " DESC"
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
    public List<StorySetting> getBySubCategory(int storyId, String subCategory) {
        List<StorySetting> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                DBHelper.TABLE_STORY_SETTING,
                null,
                DBHelper.COL_STORY_SETTING_STORY_ID + "=? AND " + 
                DBHelper.COL_STORY_SETTING_SUB_CATEGORY + "=?",
                new String[]{String.valueOf(storyId), subCategory},
                null, null,
                DBHelper.COL_STORY_SETTING_UPDATE_TIME + " DESC"
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
    public List<StorySetting> searchByKeyword(int storyId, String keyword) {
        List<StorySetting> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String like = "%" + keyword + "%";
        Cursor cursor = db.query(
                DBHelper.TABLE_STORY_SETTING,
                null,
                DBHelper.COL_STORY_SETTING_STORY_ID + "=? AND (" +
                DBHelper.COL_STORY_SETTING_TITLE + " LIKE ? OR " +
                DBHelper.COL_STORY_SETTING_SUMMARY + " LIKE ? OR " +
                DBHelper.COL_STORY_SETTING_DETAIL + " LIKE ?)",
                new String[]{String.valueOf(storyId), like, like, like},
                null, null,
                DBHelper.COL_STORY_SETTING_UPDATE_TIME + " DESC"
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
     * 查询收藏的设定
     */
    public List<StorySetting> getFavorites(int storyId) {
        List<StorySetting> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                DBHelper.TABLE_STORY_SETTING,
                null,
                DBHelper.COL_STORY_SETTING_STORY_ID + "=? AND " +
                DBHelper.COL_STORY_SETTING_IS_FAVORITE + "=1",
                new String[]{String.valueOf(storyId)},
                null, null,
                DBHelper.COL_STORY_SETTING_UPDATE_TIME + " DESC"
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
     * 查询源自某个全局素材的所有设定
     */
    public List<StorySetting> getBySourceMaterialId(int sourceMaterialId) {
        List<StorySetting> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                DBHelper.TABLE_STORY_SETTING,
                null,
                DBHelper.COL_STORY_SETTING_SOURCE_MATERIAL_ID + "=?",
                new String[]{String.valueOf(sourceMaterialId)},
                null, null,
                DBHelper.COL_STORY_SETTING_CREATE_TIME + " DESC"
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
     * 查询指定小说中属于某预设模板的全部素材。
     *
     * @param storyId    小说ID（0=全局素材库）
     * @param templateId 模板ID
     */
    public List<StorySetting> getByPresetTemplateId(int storyId, String templateId) {
        List<StorySetting> list = new ArrayList<>();
        if (templateId == null) {
            return list;
        }
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                DBHelper.TABLE_STORY_SETTING,
                null,
                DBHelper.COL_STORY_SETTING_STORY_ID + "=? AND " +
                DBHelper.COL_STORY_SETTING_PRESET_TEMPLATE_ID + "=?",
                new String[]{String.valueOf(storyId), templateId},
                null, null,
                DBHelper.COL_STORY_SETTING_UPDATE_TIME + " DESC"
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
     * 删除指定小说中属于某预设模板的全部素材（卸载模板时调用）。
     * 不会级联删除已创建的关联关系，调用方需要自行处理。
     *
     * @return 受影响的行数
     */
    public int deleteByPresetTemplateId(int storyId, String templateId) {
        if (templateId == null) {
            return 0;
        }
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.delete(
                DBHelper.TABLE_STORY_SETTING,
                DBHelper.COL_STORY_SETTING_STORY_ID + "=? AND " +
                DBHelper.COL_STORY_SETTING_PRESET_TEMPLATE_ID + "=?",
                new String[]{String.valueOf(storyId), templateId});
    }

    /**
     * 查询指定小说中某预设模板已安装的最高版本号；未安装时返回 0。
     */
    public int getMaxPresetVersion(int storyId, String templateId) {
        if (templateId == null) {
            return 0;
        }
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                DBHelper.TABLE_STORY_SETTING,
                new String[]{"MAX(" + DBHelper.COL_STORY_SETTING_PRESET_VERSION + ")"},
                DBHelper.COL_STORY_SETTING_STORY_ID + "=? AND " +
                DBHelper.COL_STORY_SETTING_PRESET_TEMPLATE_ID + "=?",
                new String[]{String.valueOf(storyId), templateId},
                null, null, null
        );
        try {
            if (cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex("MAX(" + DBHelper.COL_STORY_SETTING_PRESET_VERSION + ")");
                if (idx >= 0 && !cursor.isNull(idx)) {
                    return cursor.getInt(idx);
                }
            }
            return 0;
        } finally {
            cursor.close();
        }
    }

    /**
     * 根据标题查询设定
     */
    public StorySetting getByTitle(String title) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                DBHelper.TABLE_STORY_SETTING,
                null,
                DBHelper.COL_STORY_SETTING_TITLE + "=?",
                new String[]{title},
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
     * 增加使用次数
     */
    public void incrementUsageCount(int id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.execSQL(
            "UPDATE " + DBHelper.TABLE_STORY_SETTING + 
            " SET " + DBHelper.COL_STORY_SETTING_USAGE_COUNT + "=" + 
            DBHelper.COL_STORY_SETTING_USAGE_COUNT + "+1 WHERE " +
            DBHelper.COL_STORY_SETTING_ID + "=?",
            new Object[]{id}
        );
    }
    
    /**
     * 更新设定配图
     */
    public int updateSettingImage(int id, String imagePath) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DBHelper.COL_STORY_SETTING_IMAGE_PATH, imagePath);
        values.put(DBHelper.COL_STORY_SETTING_UPDATE_TIME, System.currentTimeMillis());
        return db.update(DBHelper.TABLE_STORY_SETTING, values,
                DBHelper.COL_STORY_SETTING_ID + "=?",
                new String[]{String.valueOf(id)});
    }

    /**
     * 批量删除某小说的所有设定
     */
    public int deleteByStoryId(int storyId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.delete(DBHelper.TABLE_STORY_SETTING,
                DBHelper.COL_STORY_SETTING_STORY_ID + "=?",
                new String[]{String.valueOf(storyId)});
    }

    /**
     * 将Cursor映射为StorySetting对象
     */
    private StorySetting mapCursor(Cursor cursor) {
        StorySetting setting = new StorySetting();
        setting.setId(cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COL_STORY_SETTING_ID)));
        setting.setStoryId(cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COL_STORY_SETTING_STORY_ID)));
        setting.setCategory(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_STORY_SETTING_CATEGORY)));
        setting.setSubCategory(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_STORY_SETTING_SUB_CATEGORY)));
        setting.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_STORY_SETTING_TITLE)));
        setting.setSummary(getColumnString(cursor, DBHelper.COL_STORY_SETTING_SUMMARY));
        setting.setDetail(getColumnString(cursor, DBHelper.COL_STORY_SETTING_DETAIL));
        setting.setAttributes(getColumnString(cursor, DBHelper.COL_STORY_SETTING_ATTRIBUTES));
        setting.setTags(getColumnString(cursor, DBHelper.COL_STORY_SETTING_TAGS));
        setting.setAliases(getColumnString(cursor, DBHelper.COL_STORY_SETTING_ALIASES));
        setting.setSpecificAttributes(getColumnString(cursor, DBHelper.COL_STORY_SETTING_SPECIFIC_ATTRS));
        setting.setSourceMaterialId(cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COL_STORY_SETTING_SOURCE_MATERIAL_ID)));
        setting.setSourceType(getColumnString(cursor, DBHelper.COL_STORY_SETTING_SOURCE_TYPE));
        setting.setSourceUrl(getColumnString(cursor, DBHelper.COL_STORY_SETTING_SOURCE_URL));
        setting.setSourceTitle(getColumnString(cursor, DBHelper.COL_STORY_SETTING_SOURCE_TITLE));
        setting.setAiConfidence(getColumnDouble(cursor, DBHelper.COL_STORY_SETTING_AI_CONFIDENCE, 0.5));
        setting.setRawJson(getColumnString(cursor, DBHelper.COL_STORY_SETTING_RAW_JSON));
        setting.setImportTime(cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.COL_STORY_SETTING_IMPORT_TIME)));
        setting.setLastSyncTime(cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.COL_STORY_SETTING_LAST_SYNC_TIME)));
        setting.setSyncEnabled(cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COL_STORY_SETTING_SYNC_ENABLED)) == 1);
        setting.setHasUpdates(cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COL_STORY_SETTING_HAS_UPDATES)) == 1);
        setting.setCreateTime(cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.COL_STORY_SETTING_CREATE_TIME)));
        setting.setUpdateTime(cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.COL_STORY_SETTING_UPDATE_TIME)));
        setting.setFavorite(cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COL_STORY_SETTING_IS_FAVORITE)) == 1);
        setting.setUsageCount(cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COL_STORY_SETTING_USAGE_COUNT)));
        setting.setImagePath(getColumnString(cursor, DBHelper.COL_STORY_SETTING_IMAGE_PATH));
        setting.setPresetTemplateId(getColumnString(cursor, DBHelper.COL_STORY_SETTING_PRESET_TEMPLATE_ID));
        setting.setPresetVersion(getColumnInt(cursor, DBHelper.COL_STORY_SETTING_PRESET_VERSION, 0));
        return setting;
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

    private int getColumnInt(Cursor cursor, String column, int defaultValue) {
        int index = cursor.getColumnIndex(column);
        if (index < 0 || cursor.isNull(index)) {
            return defaultValue;
        }
        return cursor.getInt(index);
    }
}
