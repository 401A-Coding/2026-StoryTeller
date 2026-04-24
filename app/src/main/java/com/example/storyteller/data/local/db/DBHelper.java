package com.example.storyteller.data.local.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {
    // 数据库名称和版本
    private static final String DB_NAME = "storyteller.db";
    private static final int DB_VERSION = 1;

    // 故事表字段
    public static final String TABLE_STORY = "story";
    public static final String COL_STORY_ID = "id";
    public static final String COL_STORY_TITLE = "title";
    public static final String COL_STORY_CONTENT = "content";
    public static final String COL_STORY_GENRE = "genre";
    public static final String COL_STORY_CREATE_TIME = "create_time";
    public static final String COL_STORY_IS_COLLECTED = "is_collected";

    // 人物表字段
    public static final String TABLE_CHARACTER = "character";
    public static final String COL_CHARACTER_ID = "id";
    public static final String COL_CHARACTER_STORY_ID = "story_id";
    public static final String COL_CHARACTER_NAME = "name";
    public static final String COL_CHARACTER_PROFILE = "profile";
    public static final String COL_CHARACTER_AVATAR = "avatar_res_id";

    // 单例模式（全局只有一个数据库实例）
    private static DBHelper instance;
    // 私有构造函数，防止外部直接实例化
    private DBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }
    // 获取单例实例的方法，确保全局只有一个数据库连接
    public static synchronized DBHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DBHelper(context.getApplicationContext());
        }
        return instance;
    }
    // 创建数据库表格的逻辑，后续补充逻辑
    @Override
    public void onCreate(SQLiteDatabase db) {
        // 创建表格SQL，后续补充逻辑
    }
    // 数据库升级逻辑，后续补充逻辑
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // 后续数据库升级逻辑（如新增表、修改字段），先留空
    }
}