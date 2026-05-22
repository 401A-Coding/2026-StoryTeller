package com.example.storyteller.data.local.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import com.example.storyteller.model.SettingRelationship;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 设定关系数据访问对象
 * 负责 setting_relationships 表的 CRUD 操作
 * 
 * 支持功能：
 * - 创建、读取、更新、删除设定关系
 * - 按小说、设定、类型查询关系
 * - 批量操作
 * - 关系统计
 */
public class SettingRelationshipDao {
    private final DBHelper dbHelper;
    
    public SettingRelationshipDao(Context context) {
        this.dbHelper = DBHelper.getInstance(context);
    }
    
    // ==================== 基本 CRUD ====================
    
    /**
     * 插入新关系
     * @param relationship 关系对象
     * @return 新关系ID，失败返回-1
     */
    public long insert(SettingRelationship relationship) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = toContentValues(relationship);
        return db.insert(DBHelper.TABLE_SETTING_RELATIONSHIPS, null, values);
    }
    
    /**
     * 更新关系
     * @param relationship 关系对象（需包含id）
     * @return 影响行数
     */
    public int update(SettingRelationship relationship) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        
        values.put(DBHelper.COL_REL_RELATIONSHIP_TYPE, relationship.getRelationshipType());
        values.put(DBHelper.COL_REL_DESCRIPTION, relationship.getDescription());
        values.put(DBHelper.COL_REL_SOURCE_TYPE, relationship.getSourceType());
        values.put(DBHelper.COL_REL_CONFIDENCE, relationship.getConfidence());
        values.put(DBHelper.COL_REL_UPDATE_TIME, System.currentTimeMillis());
        
        return db.update(
            DBHelper.TABLE_SETTING_RELATIONSHIPS, 
            values,
            DBHelper.COL_REL_ID + "=?",
            new String[]{String.valueOf(relationship.getId())}
        );
    }
    
    /**
     * 删除关系
     * @param relationshipId 关系ID
     * @return 影响行数
     */
    public int delete(int relationshipId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.delete(
            DBHelper.TABLE_SETTING_RELATIONSHIPS,
            DBHelper.COL_REL_ID + "=?",
            new String[]{String.valueOf(relationshipId)}
        );
    }
    
    /**
     * 根据ID获取关系
     * @param id 关系ID
     * @return 关系对象，不存在返回null
     */
    public SettingRelationship getById(int id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
            DBHelper.TABLE_SETTING_RELATIONSHIPS,
            null,
            DBHelper.COL_REL_ID + "=?",
            new String[]{String.valueOf(id)},
            null, null, null
        );
        
        SettingRelationship relationship = null;
        if (cursor.moveToFirst()) {
            relationship = mapCursor(cursor);
            fillSettingTitles(relationship);
        }
        cursor.close();
        return relationship;
    }
    
    // ==================== 查询方法 ====================
    
    /**
     * 获取某小说的所有关系
     * @param storyId 小说ID
     * @return 关系列表（按更新时间倒序）
     */
    public List<SettingRelationship> getByStoryId(int storyId) {
        List<SettingRelationship> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
            DBHelper.TABLE_SETTING_RELATIONSHIPS,
            null,
            DBHelper.COL_REL_STORY_ID + "=?",
            new String[]{String.valueOf(storyId)},
            null, null,
            DBHelper.COL_REL_UPDATE_TIME + " DESC"
        );
        
        try {
            while (cursor.moveToNext()) {
                SettingRelationship rel = mapCursor(cursor);
                fillSettingTitles(rel);
                list.add(rel);
            }
        } finally {
            cursor.close();
        }
        return list;
    }
    
    /**
     * 获取某设定的所有关系（作为源或目标）
     * @param settingId 设定ID
     * @return 关系列表
     */
    public List<SettingRelationship> getBySettingId(int settingId) {
        List<SettingRelationship> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
            DBHelper.TABLE_SETTING_RELATIONSHIPS,
            null,
            DBHelper.COL_REL_SOURCE_SETTING_ID + "=? OR " + DBHelper.COL_REL_TARGET_SETTING_ID + "=?",
            new String[]{String.valueOf(settingId), String.valueOf(settingId)},
            null, null,
            DBHelper.COL_REL_UPDATE_TIME + " DESC"
        );
        
        try {
            while (cursor.moveToNext()) {
                SettingRelationship rel = mapCursor(cursor);
                fillSettingTitles(rel);
                list.add(rel);
            }
        } finally {
            cursor.close();
        }
        return list;
    }
    
    /**
     * 获取某设定的外向关系（作为源）
     * @param settingId 设定ID
     * @return 关系列表
     */
    public List<SettingRelationship> getOutgoingRelations(int settingId) {
        List<SettingRelationship> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
            DBHelper.TABLE_SETTING_RELATIONSHIPS,
            null,
            DBHelper.COL_REL_SOURCE_SETTING_ID + "=?",
            new String[]{String.valueOf(settingId)},
            null, null,
            DBHelper.COL_REL_UPDATE_TIME + " DESC"
        );
        
        try {
            while (cursor.moveToNext()) {
                SettingRelationship rel = mapCursor(cursor);
                fillSettingTitles(rel);
                list.add(rel);
            }
        } finally {
            cursor.close();
        }
        return list;
    }
    
    /**
     * 获取某设定的内向关系（作为目标）
     * @param settingId 设定ID
     * @return 关系列表
     */
    public List<SettingRelationship> getIncomingRelations(int settingId) {
        List<SettingRelationship> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
            DBHelper.TABLE_SETTING_RELATIONSHIPS,
            null,
            DBHelper.COL_REL_TARGET_SETTING_ID + "=?",
            new String[]{String.valueOf(settingId)},
            null, null,
            DBHelper.COL_REL_UPDATE_TIME + " DESC"
        );
        
        try {
            while (cursor.moveToNext()) {
                SettingRelationship rel = mapCursor(cursor);
                fillSettingTitles(rel);
                list.add(rel);
            }
        } finally {
            cursor.close();
        }
        return list;
    }
    
    /**
     * 按关系类型查询
     * @param storyId 小说ID
     * @param relationshipType 关系类型
     * @return 关系列表
     */
    public List<SettingRelationship> getByType(int storyId, String relationshipType) {
        List<SettingRelationship> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
            DBHelper.TABLE_SETTING_RELATIONSHIPS,
            null,
            DBHelper.COL_REL_STORY_ID + "=? AND " + DBHelper.COL_REL_RELATIONSHIP_TYPE + "=?",
            new String[]{String.valueOf(storyId), relationshipType},
            null, null,
            DBHelper.COL_REL_UPDATE_TIME + " DESC"
        );
        
        try {
            while (cursor.moveToNext()) {
                SettingRelationship rel = mapCursor(cursor);
                fillSettingTitles(rel);
                list.add(rel);
            }
        } finally {
            cursor.close();
        }
        return list;
    }
    
    /**
     * 按分类查询关系（如 hierarchy、association 等）
     * @param storyId 小说ID
     * @param category 关系分类
     * @return 关系列表
     */
    public List<SettingRelationship> getByCategory(int storyId, String category) {
        List<SettingRelationship> all = getByStoryId(storyId);
        List<SettingRelationship> filtered = new ArrayList<>();
        for (SettingRelationship rel : all) {
            if (category.equals(rel.getTypeCategory())) {
                filtered.add(rel);
            }
        }
        return filtered;
    }
    
    // ==================== 存在性检查 ====================
    
    /**
     * 检查两个设定之间是否存在指定关系
     * @param sourceId 源设定ID
     * @param targetId 目标设定ID
     * @param type 关系类型
     * @return 是否存在
     */
    public boolean exists(int sourceId, int targetId, String type) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
            DBHelper.TABLE_SETTING_RELATIONSHIPS,
            new String[]{DBHelper.COL_REL_ID},
            DBHelper.COL_REL_SOURCE_SETTING_ID + "=? AND " + 
            DBHelper.COL_REL_TARGET_SETTING_ID + "=? AND " + 
            DBHelper.COL_REL_RELATIONSHIP_TYPE + "=?",
            new String[]{String.valueOf(sourceId), String.valueOf(targetId), type},
            null, null, null
        );
        
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }
    
    /**
     * 检查两个设定之间是否存在任何关系
     * @param settingId1 设定ID1
     * @param settingId2 设定ID2
     * @return 是否存在任何关系
     */
    public boolean hasAnyRelation(int settingId1, int settingId2) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
            DBHelper.TABLE_SETTING_RELATIONSHIPS,
            new String[]{DBHelper.COL_REL_ID},
            "(" + DBHelper.COL_REL_SOURCE_SETTING_ID + "=? AND " + DBHelper.COL_REL_TARGET_SETTING_ID + "=?) OR " +
            "(" + DBHelper.COL_REL_SOURCE_SETTING_ID + "=? AND " + DBHelper.COL_REL_TARGET_SETTING_ID + "=?)",
            new String[]{
                String.valueOf(settingId1), String.valueOf(settingId2),
                String.valueOf(settingId2), String.valueOf(settingId1)
            },
            null, null, null
        );
        
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }
    
    // ==================== 批量操作 ====================
    
    /**
     * 删除某设定的所有关系
     * @param settingId 设定ID
     * @return 删除的行数
     */
    public int deleteBySettingId(int settingId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.delete(
            DBHelper.TABLE_SETTING_RELATIONSHIPS,
            DBHelper.COL_REL_SOURCE_SETTING_ID + "=? OR " + DBHelper.COL_REL_TARGET_SETTING_ID + "=?",
            new String[]{String.valueOf(settingId), String.valueOf(settingId)}
        );
    }
    
    /**
     * 删除某小说的所有关系
     * @param storyId 小说ID
     * @return 删除的行数
     */
    public int deleteByStoryId(int storyId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.delete(
            DBHelper.TABLE_SETTING_RELATIONSHIPS,
            DBHelper.COL_REL_STORY_ID + "=?",
            new String[]{String.valueOf(storyId)}
        );
    }
    
    /**
     * 批量插入关系
     * @param relationships 关系列表
     * @return 成功插入的数量
     */
    public int batchInsert(List<SettingRelationship> relationships) {
        if (relationships == null || relationships.isEmpty()) {
            return 0;
        }
        
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int count = 0;
        
        db.beginTransaction();
        try {
            for (SettingRelationship rel : relationships) {
                long id = db.insert(DBHelper.TABLE_SETTING_RELATIONSHIPS, null, toContentValues(rel));
                if (id > 0) count++;
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        
        return count;
    }
    
    /**
     * 批量更新关系来源类型（如将AI推断的改为用户确认）
     * @param ids 需要更新的关系ID列表
     * @param sourceType 新的来源类型
     * @return 更新的行数
     */
    public int batchUpdateSourceType(List<Integer> ids, String sourceType) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int count = 0;
        
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put(DBHelper.COL_REL_SOURCE_TYPE, sourceType);
            values.put(DBHelper.COL_REL_UPDATE_TIME, System.currentTimeMillis());
            
            for (Integer id : ids) {
                int updated = db.update(
                    DBHelper.TABLE_SETTING_RELATIONSHIPS,
                    values,
                    DBHelper.COL_REL_ID + "=?",
                    new String[]{String.valueOf(id)}
                );
                count += updated;
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        
        return count;
    }
    
    // ==================== 统计方法 ====================
    
    /**
     * 获取某小说的关系总数
     * @param storyId 小说ID
     * @return 关系数量
     */
    public int getCountByStoryId(int storyId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
            "SELECT COUNT(*) FROM " + DBHelper.TABLE_SETTING_RELATIONSHIPS + 
            " WHERE " + DBHelper.COL_REL_STORY_ID + "=?",
            new String[]{String.valueOf(storyId)}
        );
        
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }
    
    /**
     * 获取某设定的关系数量
     * @param settingId 设定ID
     * @return 关系数量
     */
    public int getRelationCount(int settingId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
            "SELECT COUNT(*) FROM " + DBHelper.TABLE_SETTING_RELATIONSHIPS + 
            " WHERE " + DBHelper.COL_REL_SOURCE_SETTING_ID + "=? OR " + DBHelper.COL_REL_TARGET_SETTING_ID + "=?",
            new String[]{String.valueOf(settingId), String.valueOf(settingId)}
        );
        
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }
    
    /**
     * 获取各类型关系数量统计
     * @param storyId 小说ID
     * @return 类型->数量的Map
     */
    public Map<String, Integer> getTypeStatistics(int storyId) {
        Map<String, Integer> stats = new HashMap<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
            "SELECT " + DBHelper.COL_REL_RELATIONSHIP_TYPE + ", COUNT(*) as cnt FROM " + 
            DBHelper.TABLE_SETTING_RELATIONSHIPS + 
            " WHERE " + DBHelper.COL_REL_STORY_ID + "=?" +
            " GROUP BY " + DBHelper.COL_REL_RELATIONSHIP_TYPE,
            new String[]{String.valueOf(storyId)}
        );
        
        try {
            while (cursor.moveToNext()) {
                String type = cursor.getString(0);
                int count = cursor.getInt(1);
                stats.put(type, count);
            }
        } finally {
            cursor.close();
        }
        return stats;
    }
    
    /**
     * 获取各分类关系数量统计
     * @param storyId 小说ID
     * @return 分类->数量的Map
     */
    public Map<String, Integer> getCategoryStatistics(int storyId) {
        Map<String, Integer> stats = new HashMap<>();
        List<SettingRelationship> all = getByStoryId(storyId);
        
        for (SettingRelationship rel : all) {
            String category = rel.getTypeCategory();
            Integer count = stats.get(category);
            if (count == null) {
                count = 0;
            }
            stats.put(category, count + 1);
        }
        
        return stats;
    }
    
    /**
     * 获取某个设定被引用次数（作为源或目标）
     * @param settingId 设定ID
     * @return 被引用的次数
     */
    public int getReferencedCount(int settingId) {
        return getRelationCount(settingId);
    }
    
    /**
     * 获取AI推断但尚未确认的关系
     * @param storyId 小说ID
     * @return AI推断的关系列表
     */
    public List<SettingRelationship> getUnconfirmedAiRelations(int storyId) {
        List<SettingRelationship> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
            DBHelper.TABLE_SETTING_RELATIONSHIPS,
            null,
            DBHelper.COL_REL_STORY_ID + "=? AND " + DBHelper.COL_REL_SOURCE_TYPE + "=?",
            new String[]{String.valueOf(storyId), SettingRelationship.SOURCE_TYPE_AI_INFERRED},
            null, null,
            DBHelper.COL_REL_UPDATE_TIME + " DESC"
        );
        
        try {
            while (cursor.moveToNext()) {
                SettingRelationship rel = mapCursor(cursor);
                fillSettingTitles(rel);
                list.add(rel);
            }
        } finally {
            cursor.close();
        }
        return list;
    }
    
    // ==================== 图结构辅助方法 ====================
    
    /**
     * 获取某设定的所有邻居设定ID（直接关联的设定）
     * @param settingId 设定ID
     * @return 邻居设定ID列表
     */
    public List<Integer> getNeighborIds(int settingId) {
        List<Integer> neighborIds = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
            "SELECT DISTINCT CASE WHEN " + DBHelper.COL_REL_SOURCE_SETTING_ID + "=? THEN " + 
            DBHelper.COL_REL_TARGET_SETTING_ID + " ELSE " + DBHelper.COL_REL_SOURCE_SETTING_ID + 
            " END FROM " + DBHelper.TABLE_SETTING_RELATIONSHIPS + 
            " WHERE " + DBHelper.COL_REL_SOURCE_SETTING_ID + "=? OR " + 
            DBHelper.COL_REL_TARGET_SETTING_ID + "=?",
            new String[]{String.valueOf(settingId), String.valueOf(settingId), String.valueOf(settingId)}
        );
        
        try {
            while (cursor.moveToNext()) {
                neighborIds.add(cursor.getInt(0));
            }
        } finally {
            cursor.close();
        }
        return neighborIds;
    }
    
    /**
     * 构建某小说的关系图邻接表
     * @param storyId 小说ID
     * @return 设定ID -> 邻居设定ID列表 的映射
     */
    public Map<Integer, List<Integer>> buildAdjacencyList(int storyId) {
        Map<Integer, List<Integer>> adjacencyList = new HashMap<>();
        List<SettingRelationship> relationships = getByStoryId(storyId);
        
        for (SettingRelationship rel : relationships) {
            int sourceId = rel.getSourceSettingId();
            int targetId = rel.getTargetSettingId();
            
            // 添加 source -> target 的边
            List<Integer> sourceNeighbors = adjacencyList.get(sourceId);
            if (sourceNeighbors == null) {
                sourceNeighbors = new ArrayList<>();
                adjacencyList.put(sourceId, sourceNeighbors);
            }
            if (!sourceNeighbors.contains(targetId)) {
                sourceNeighbors.add(targetId);
            }
            
            // 对于无向关系，添加反向边
            if (!rel.isDirected()) {
                List<Integer> targetNeighbors = adjacencyList.get(targetId);
                if (targetNeighbors == null) {
                    targetNeighbors = new ArrayList<>();
                    adjacencyList.put(targetId, targetNeighbors);
                }
                if (!targetNeighbors.contains(sourceId)) {
                    targetNeighbors.add(sourceId);
                }
            }
        }
        
        return adjacencyList;
    }
    
    // ==================== 私有方法 ====================
    
    /**
     * 将 SettingRelationship 对象转换为 ContentValues
     */
    private ContentValues toContentValues(SettingRelationship relationship) {
        ContentValues values = new ContentValues();
        values.put(DBHelper.COL_REL_STORY_ID, relationship.getStoryId());
        values.put(DBHelper.COL_REL_SOURCE_SETTING_ID, relationship.getSourceSettingId());
        values.put(DBHelper.COL_REL_TARGET_SETTING_ID, relationship.getTargetSettingId());
        values.put(DBHelper.COL_REL_RELATIONSHIP_TYPE, relationship.getRelationshipType());
        values.put(DBHelper.COL_REL_DESCRIPTION, relationship.getDescription());
        values.put(DBHelper.COL_REL_SOURCE_TYPE, relationship.getSourceType());
        values.put(DBHelper.COL_REL_CONFIDENCE, relationship.getConfidence());
        values.put(DBHelper.COL_REL_CREATE_TIME, relationship.getCreateTime());
        values.put(DBHelper.COL_REL_UPDATE_TIME, relationship.getUpdateTime());
        return values;
    }
    
    /**
     * 将 Cursor 映射为 SettingRelationship 对象
     */
    private SettingRelationship mapCursor(Cursor cursor) {
        SettingRelationship rel = new SettingRelationship();
        rel.setId(cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COL_REL_ID)));
        rel.setStoryId(cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COL_REL_STORY_ID)));
        rel.setSourceSettingId(cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COL_REL_SOURCE_SETTING_ID)));
        rel.setTargetSettingId(cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COL_REL_TARGET_SETTING_ID)));
        rel.setRelationshipType(getColumnString(cursor, DBHelper.COL_REL_RELATIONSHIP_TYPE));
        rel.setDescription(getColumnString(cursor, DBHelper.COL_REL_DESCRIPTION));
        rel.setSourceType(getColumnString(cursor, DBHelper.COL_REL_SOURCE_TYPE));
        rel.setConfidence(getColumnDouble(cursor, DBHelper.COL_REL_CONFIDENCE, 0.8));
        rel.setCreateTime(cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.COL_REL_CREATE_TIME)));
        rel.setUpdateTime(cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.COL_REL_UPDATE_TIME)));
        return rel;
    }
    
    /**
     * 填充设定的标题信息
     */
    private void fillSettingTitles(SettingRelationship relationship) {
        // 查询源设定信息
        fillSourceSettingInfo(relationship);
        // 查询目标设定信息
        fillTargetSettingInfo(relationship);
    }
    
    /**
     * 填充源设定的标题信息
     */
    private void fillSourceSettingInfo(SettingRelationship relationship) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
            DBHelper.TABLE_STORY_SETTING,
            new String[]{
                DBHelper.COL_STORY_SETTING_TITLE, 
                DBHelper.COL_STORY_SETTING_CATEGORY,
                DBHelper.COL_STORY_SETTING_SUB_CATEGORY
            },
            DBHelper.COL_STORY_SETTING_ID + "=?",
            new String[]{String.valueOf(relationship.getSourceSettingId())},
            null, null, null
        );
        
        try {
            if (cursor.moveToFirst()) {
                relationship.setSourceSettingTitle(getColumnString(cursor, DBHelper.COL_STORY_SETTING_TITLE));
                relationship.setSourceSettingCategory(getColumnString(cursor, DBHelper.COL_STORY_SETTING_CATEGORY));
                relationship.setSourceSettingSubCategory(getColumnString(cursor, DBHelper.COL_STORY_SETTING_SUB_CATEGORY));
            }
        } finally {
            cursor.close();
        }
    }
    
    /**
     * 填充目标设定的标题信息
     */
    private void fillTargetSettingInfo(SettingRelationship relationship) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
            DBHelper.TABLE_STORY_SETTING,
            new String[]{
                DBHelper.COL_STORY_SETTING_TITLE, 
                DBHelper.COL_STORY_SETTING_CATEGORY,
                DBHelper.COL_STORY_SETTING_SUB_CATEGORY
            },
            DBHelper.COL_STORY_SETTING_ID + "=?",
            new String[]{String.valueOf(relationship.getTargetSettingId())},
            null, null, null
        );
        
        try {
            if (cursor.moveToFirst()) {
                relationship.setTargetSettingTitle(getColumnString(cursor, DBHelper.COL_STORY_SETTING_TITLE));
                relationship.setTargetSettingCategory(getColumnString(cursor, DBHelper.COL_STORY_SETTING_CATEGORY));
                relationship.setTargetSettingSubCategory(getColumnString(cursor, DBHelper.COL_STORY_SETTING_SUB_CATEGORY));
            }
        } finally {
            cursor.close();
        }
    }
    
    /**
     * 获取列的字符串值
     */
    private String getColumnString(Cursor cursor, String column) {
        int index = cursor.getColumnIndex(column);
        if (index < 0 || cursor.isNull(index)) {
            return null;
        }
        return cursor.getString(index);
    }
    
    /**
     * 获取列的double值
     */
    private double getColumnDouble(Cursor cursor, String column, double defaultValue) {
        int index = cursor.getColumnIndex(column);
        if (index < 0 || cursor.isNull(index)) {
            return defaultValue;
        }
        return cursor.getDouble(index);
    }
}