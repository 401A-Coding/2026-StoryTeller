package com.example.storyteller.data.local.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.storyteller.model.ImportedNovel;

import java.util.ArrayList;
import java.util.List;

public class ImportedNovelDao {
    private final DBHelper dbHelper;

    public ImportedNovelDao(Context context) {
        this.dbHelper = DBHelper.getInstance(context);
    }

    /**
     * 插入导入的小说
     */
    public long insert(ImportedNovel novel) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DBHelper.COL_IMPORTED_NOVEL_TITLE, novel.getTitle());
        values.put(DBHelper.COL_IMPORTED_NOVEL_AUTHOR, novel.getAuthor());
        values.put(DBHelper.COL_IMPORTED_NOVEL_SOURCE_URL, novel.getSourceUrl());
        values.put(DBHelper.COL_IMPORTED_NOVEL_COVER_URL, novel.getCoverUrl());
        values.put(DBHelper.COL_IMPORTED_NOVEL_DESCRIPTION, novel.getDescription());
        values.put(DBHelper.COL_IMPORTED_NOVEL_IMPORT_TIME, novel.getImportTime());
        values.put(DBHelper.COL_IMPORTED_NOVEL_STATUS, novel.getStatus());
        values.put(DBHelper.COL_IMPORTED_NOVEL_STRUCTURE_JSON, novel.getStructureJson());
        values.put(DBHelper.COL_IMPORTED_NOVEL_CONTENT_DIR, novel.getContentDir());
        values.put(DBHelper.COL_IMPORTED_NOVEL_TOTAL_CHAPTERS, novel.getTotalChapters());
        values.put(DBHelper.COL_IMPORTED_NOVEL_TOTAL_WORDS, novel.getTotalWords());
        values.put(DBHelper.COL_IMPORTED_NOVEL_TAGS, novel.getTags());
        return db.insert(DBHelper.TABLE_IMPORTED_NOVEL, null, values);
    }

    /**
     * 更新导入的小说
     */
    public int update(ImportedNovel novel) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DBHelper.COL_IMPORTED_NOVEL_TITLE, novel.getTitle());
        values.put(DBHelper.COL_IMPORTED_NOVEL_AUTHOR, novel.getAuthor());
        values.put(DBHelper.COL_IMPORTED_NOVEL_COVER_URL, novel.getCoverUrl());
        values.put(DBHelper.COL_IMPORTED_NOVEL_DESCRIPTION, novel.getDescription());
        values.put(DBHelper.COL_IMPORTED_NOVEL_STATUS, novel.getStatus());
        values.put(DBHelper.COL_IMPORTED_NOVEL_STRUCTURE_JSON, novel.getStructureJson());
        values.put(DBHelper.COL_IMPORTED_NOVEL_CONTENT_DIR, novel.getContentDir());
        values.put(DBHelper.COL_IMPORTED_NOVEL_TOTAL_CHAPTERS, novel.getTotalChapters());
        values.put(DBHelper.COL_IMPORTED_NOVEL_TOTAL_WORDS, novel.getTotalWords());
        values.put(DBHelper.COL_IMPORTED_NOVEL_TAGS, novel.getTags());
        return db.update(DBHelper.TABLE_IMPORTED_NOVEL, values, 
                DBHelper.COL_IMPORTED_NOVEL_ID + "=?", 
                new String[]{String.valueOf(novel.getId())});
    }

    /**
     * 根据ID查询
     */
    public ImportedNovel getById(int id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                DBHelper.TABLE_IMPORTED_NOVEL,
                null,
                DBHelper.COL_IMPORTED_NOVEL_ID + "=?",
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
     * 根据URL查询（检查是否已导入）
     */
    public ImportedNovel getByUrl(String url) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                DBHelper.TABLE_IMPORTED_NOVEL,
                null,
                DBHelper.COL_IMPORTED_NOVEL_SOURCE_URL + "=?",
                new String[]{url},
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
     * 获取所有导入的小说（按导入时间倒序）
     */
    public List<ImportedNovel> getAll() {
        List<ImportedNovel> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                DBHelper.TABLE_IMPORTED_NOVEL,
                null, null, null, null, null,
                DBHelper.COL_IMPORTED_NOVEL_IMPORT_TIME + " DESC"
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
     * 删除导入的小说
     */
    public int delete(int id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.delete(DBHelper.TABLE_IMPORTED_NOVEL,
                DBHelper.COL_IMPORTED_NOVEL_ID + "=?",
                new String[]{String.valueOf(id)});
    }

    /**
     * 将Cursor映射为ImportedNovel对象
     */
    private ImportedNovel mapCursor(Cursor cursor) {
        ImportedNovel novel = new ImportedNovel();
        novel.setId(cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COL_IMPORTED_NOVEL_ID)));
        novel.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_IMPORTED_NOVEL_TITLE)));
        novel.setAuthor(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_IMPORTED_NOVEL_AUTHOR)));
        novel.setSourceUrl(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_IMPORTED_NOVEL_SOURCE_URL)));
        novel.setCoverUrl(getColumnString(cursor, DBHelper.COL_IMPORTED_NOVEL_COVER_URL));
        novel.setDescription(getColumnString(cursor, DBHelper.COL_IMPORTED_NOVEL_DESCRIPTION));
        novel.setImportTime(cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.COL_IMPORTED_NOVEL_IMPORT_TIME)));
        novel.setStatus(getColumnString(cursor, DBHelper.COL_IMPORTED_NOVEL_STATUS, "imported"));
        novel.setStructureJson(getColumnString(cursor, DBHelper.COL_IMPORTED_NOVEL_STRUCTURE_JSON));
        novel.setContentDir(getColumnString(cursor, DBHelper.COL_IMPORTED_NOVEL_CONTENT_DIR));
        novel.setTotalChapters(cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COL_IMPORTED_NOVEL_TOTAL_CHAPTERS)));
        novel.setTotalWords(cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COL_IMPORTED_NOVEL_TOTAL_WORDS)));
        novel.setTags(getColumnString(cursor, DBHelper.COL_IMPORTED_NOVEL_TAGS));
        return novel;
    }

    private String getColumnString(Cursor cursor, String column) {
        return getColumnString(cursor, column, null);
    }

    private String getColumnString(Cursor cursor, String column, String defaultValue) {
        int index = cursor.getColumnIndex(column);
        if (index < 0 || cursor.isNull(index)) {
            return defaultValue;
        }
        return cursor.getString(index);
    }
}
