package com.example.storyteller.data.local.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.storyteller.model.BehaviorLog;

import java.util.ArrayList;
import java.util.List;

public class BehaviorLogDao {
    private final DBHelper dbHelper;

    public BehaviorLogDao(Context context) {
        this.dbHelper = DBHelper.getInstance(context);
    }

    public long insert(BehaviorLog log) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DBHelper.COL_LOG_ACTION, log.getAction());
        values.put(DBHelper.COL_LOG_TARGET_ID, log.getTargetId());
        values.put(DBHelper.COL_LOG_EXTRA, log.getExtra());
        values.put(DBHelper.COL_LOG_CREATE_TIME, log.getCreateTime());
        return db.insert(DBHelper.TABLE_BEHAVIOR_LOG, null, values);
    }

    public List<BehaviorLog> getLatest(int limit) {
        List<BehaviorLog> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                DBHelper.TABLE_BEHAVIOR_LOG,
                null,
                null,
                null,
                null,
                null,
                DBHelper.COL_LOG_CREATE_TIME + " DESC",
                String.valueOf(limit)
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                list.add(map(cursor));
            }
            cursor.close();
        }
        return list;
    }

    public int clearAll() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.delete(DBHelper.TABLE_BEHAVIOR_LOG, null, null);
    }

    private BehaviorLog map(Cursor cursor) {
        int id = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COL_LOG_ID));
        String action = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_LOG_ACTION));
        int targetId = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COL_LOG_TARGET_ID));
        String extra = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_LOG_EXTRA));
        long createTime = cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.COL_LOG_CREATE_TIME));
        return new BehaviorLog(id, action, targetId, extra, createTime);
    }
}
