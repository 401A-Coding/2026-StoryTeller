package com.example.storyteller.data.local.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.example.storyteller.model.AiMemory;
import java.util.ArrayList;
import java.util.List;

/**
 * AI记忆数据访问对象
 */
public class AiMemoryDao {
    private final DBHelper dbHelper;

    public AiMemoryDao(Context context) {
        this.dbHelper = DBHelper.getInstance(context);
    }

    /**
     * 插入记忆
     */
    public long insert(AiMemory memory) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        
        if (memory.getStoryId() != null) {
            values.put(DBHelper.COL_MEMORY_STORY_ID, memory.getStoryId());
        }
        values.put(DBHelper.COL_MEMORY_TYPE, memory.getMemoryType());
        values.put(DBHelper.COL_MEMORY_TITLE, memory.getTitle());
        values.put(DBHelper.COL_MEMORY_CONTENT, memory.getContent());
        values.put(DBHelper.COL_MEMORY_IMPORTANCE, memory.getImportance());
        values.put(DBHelper.COL_MEMORY_CREATED_AT, memory.getCreatedAt());
        values.put(DBHelper.COL_MEMORY_UPDATED_AT, memory.getUpdatedAt());
        
        return db.insert(DBHelper.TABLE_AI_MEMORY, null, values);
    }

    /**
     * 更新记忆
     */
    public int update(AiMemory memory) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        
        values.put(DBHelper.COL_MEMORY_TYPE, memory.getMemoryType());
        values.put(DBHelper.COL_MEMORY_TITLE, memory.getTitle());
        values.put(DBHelper.COL_MEMORY_CONTENT, memory.getContent());
        values.put(DBHelper.COL_MEMORY_IMPORTANCE, memory.getImportance());
        values.put(DBHelper.COL_MEMORY_UPDATED_AT, System.currentTimeMillis());
        
        return db.update(DBHelper.TABLE_AI_MEMORY, values,
                DBHelper.COL_MEMORY_ID + "=?",
                new String[]{String.valueOf(memory.getId())});
    }

    /**
     * 删除记忆
     */
    public int delete(long id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.delete(DBHelper.TABLE_AI_MEMORY,
                DBHelper.COL_MEMORY_ID + "=?",
                new String[]{String.valueOf(id)});
    }

    /**
     * 根据ID查询
     */
    public AiMemory getById(long id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                DBHelper.TABLE_AI_MEMORY,
                null,
                DBHelper.COL_MEMORY_ID + "=?",
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
     * 查询某小说的所有记忆（包含全局记忆）
     */
    public List<AiMemory> getByStoryId(int storyId) {
        List<AiMemory> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                DBHelper.TABLE_AI_MEMORY,
                null,
                DBHelper.COL_MEMORY_STORY_ID + "=? OR " + DBHelper.COL_MEMORY_STORY_ID + " IS NULL",
                new String[]{String.valueOf(storyId)},
                null, null,
                DBHelper.COL_MEMORY_IMPORTANCE + " DESC, " + DBHelper.COL_MEMORY_UPDATED_AT + " DESC"
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
     * 查询所有全局记忆
     */
    public List<AiMemory> getGlobalMemories() {
        List<AiMemory> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                DBHelper.TABLE_AI_MEMORY,
                null,
                DBHelper.COL_MEMORY_STORY_ID + " IS NULL",
                null, null, null,
                DBHelper.COL_MEMORY_IMPORTANCE + " DESC, " + DBHelper.COL_MEMORY_UPDATED_AT + " DESC"
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
     * 按类型查询
     */
    public List<AiMemory> getByType(int storyId, String memoryType) {
        List<AiMemory> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                DBHelper.TABLE_AI_MEMORY,
                null,
                "(" + DBHelper.COL_MEMORY_STORY_ID + "=? OR " + DBHelper.COL_MEMORY_STORY_ID + " IS NULL) AND " +
                DBHelper.COL_MEMORY_TYPE + "=?",
                new String[]{String.valueOf(storyId), memoryType},
                null, null,
                DBHelper.COL_MEMORY_IMPORTANCE + " DESC, " + DBHelper.COL_MEMORY_UPDATED_AT + " DESC"
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
     * 删除某小说的所有记忆（不删除全局记忆）
     */
    public int deleteByStoryId(int storyId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.delete(DBHelper.TABLE_AI_MEMORY,
                DBHelper.COL_MEMORY_STORY_ID + "=?",
                new String[]{String.valueOf(storyId)});
    }

    /**
     * 删除所有记忆（包括全局）
     */
    public int deleteAll() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.delete(DBHelper.TABLE_AI_MEMORY, null, null);
    }

    /**
     * 获取记忆数量
     */
    public int getCount(int storyId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT COUNT(*) FROM " + DBHelper.TABLE_AI_MEMORY + 
                " WHERE " + DBHelper.COL_MEMORY_STORY_ID + "=? OR " + DBHelper.COL_MEMORY_STORY_ID + " IS NULL",
                new String[]{String.valueOf(storyId)}
        );
        try {
            if (cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
            return 0;
        } finally {
            cursor.close();
        }
    }

    /**
     * 按类型获取记忆数量
     */
    public int getCountByType(int storyId, String memoryType) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT COUNT(*) FROM " + DBHelper.TABLE_AI_MEMORY + 
                " WHERE (" + DBHelper.COL_MEMORY_STORY_ID + "=? OR " + DBHelper.COL_MEMORY_STORY_ID + " IS NULL) AND " +
                DBHelper.COL_MEMORY_TYPE + "=?",
                new String[]{String.valueOf(storyId), memoryType}
        );
        try {
            if (cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
            return 0;
        } finally {
            cursor.close();
        }
    }

    /**
     * 将Cursor映射为AiMemory对象
     */
    private AiMemory mapCursor(Cursor cursor) {
        AiMemory memory = new AiMemory();
        memory.setId(cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.COL_MEMORY_ID)));
        
        int storyIdIndex = cursor.getColumnIndexOrThrow(DBHelper.COL_MEMORY_STORY_ID);
        if (cursor.isNull(storyIdIndex)) {
            memory.setStoryId(null);
        } else {
            memory.setStoryId(cursor.getInt(storyIdIndex));
        }
        
        memory.setMemoryType(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_MEMORY_TYPE)));
        memory.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_MEMORY_TITLE)));
        
        int contentIndex = cursor.getColumnIndexOrThrow(DBHelper.COL_MEMORY_CONTENT);
        memory.setContent(cursor.isNull(contentIndex) ? null : cursor.getString(contentIndex));
        
        memory.setImportance(cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COL_MEMORY_IMPORTANCE)));
        memory.setCreatedAt(cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.COL_MEMORY_CREATED_AT)));
        memory.setUpdatedAt(cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.COL_MEMORY_UPDATED_AT)));
        
        return memory;
    }
}