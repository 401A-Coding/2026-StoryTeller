package com.example.storyteller.data.local.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {
    // 数据库名称和版本
    private static final String DB_NAME = "storyteller.db";
    private static final int DB_VERSION = 9;

    // 故事表字段
    public static final String TABLE_STORY = "story";
    public static final String COL_STORY_ID = "id";
    public static final String COL_STORY_TITLE = "title";
    public static final String COL_STORY_CONTENT = "content";
    public static final String COL_STORY_GENRE = "genre";
    public static final String COL_STORY_CREATE_TIME = "create_time";
    public static final String COL_STORY_IS_COLLECTED = "is_collected";
    public static final String COL_STORY_STRUCTURE = "structure";  // 存储卷-章结构的JSON数据
    public static final String COL_STORY_DESCRIPTION = "description";  // 小说简介
    public static final String COL_STORY_PLOT_SUMMARY = "plot_summary_json";  // 剧情梳理结果快照
    public static final String COL_STORY_CATEGORY = "category";  // 书架分类：全部/创作中/已完成/已收藏
    public static final String COL_STORY_COVER_COLOR = "cover_color";  // 封面颜色
    public static final String COL_STORY_COVER_PATH = "cover_path";  // 封面图片路径
    public static final String COL_STORY_WORD_COUNT = "word_count";  // 总字数
    public static final String COL_STORY_SERIES_NAME = "series_name";  // 系列名称

    // 素材表字段
    public static final String TABLE_MATERIAL = "material";
    public static final String COL_MATERIAL_ID = "id";
    public static final String COL_MATERIAL_CATEGORY = "category";
    public static final String COL_MATERIAL_TITLE = "title";
    public static final String COL_MATERIAL_CONTENT = "content";
    public static final String COL_MATERIAL_CREATE_TIME = "create_time";
    public static final String COL_MATERIAL_SOURCE_URL = "source_url";
    public static final String COL_MATERIAL_SOURCE_TITLE = "source_title";
    public static final String COL_MATERIAL_SOURCE_TYPE = "source_type";
    public static final String COL_MATERIAL_AI_SCORE = "ai_score";
    public static final String COL_MATERIAL_RAW_JSON = "raw_json";

    // 用户行为日志表字段
    public static final String TABLE_BEHAVIOR_LOG = "behavior_log";
    public static final String COL_LOG_ID = "id";
    public static final String COL_LOG_ACTION = "action";
    public static final String COL_LOG_TARGET_ID = "target_id";
    public static final String COL_LOG_EXTRA = "extra";
    public static final String COL_LOG_CREATE_TIME = "create_time";

    // 人物表字段
    public static final String TABLE_CHARACTER = "character";
    public static final String COL_CHARACTER_ID = "id";
    public static final String COL_CHARACTER_STORY_ID = "story_id";
    public static final String COL_CHARACTER_NAME = "name";
    public static final String COL_CHARACTER_PROFILE = "profile";
    public static final String COL_CHARACTER_DETAIL = "detail";
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
        String createStoryTable = "CREATE TABLE IF NOT EXISTS " + TABLE_STORY + " ("
                + COL_STORY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_STORY_TITLE + " TEXT NOT NULL, "
                + COL_STORY_CONTENT + " TEXT NOT NULL, "
                + COL_STORY_GENRE + " TEXT, "
                + COL_STORY_CREATE_TIME + " INTEGER, "
                + COL_STORY_IS_COLLECTED + " INTEGER DEFAULT 0, "
                + COL_STORY_STRUCTURE + " TEXT, "
                + COL_STORY_DESCRIPTION + " TEXT, "
                + COL_STORY_PLOT_SUMMARY + " TEXT"
                + ")";

        String createCharacterTable = "CREATE TABLE IF NOT EXISTS " + TABLE_CHARACTER + " ("
                + COL_CHARACTER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_CHARACTER_STORY_ID + " INTEGER NOT NULL, "
                + COL_CHARACTER_NAME + " TEXT NOT NULL, "
                + COL_CHARACTER_PROFILE + " TEXT, "
                + COL_CHARACTER_DETAIL + " TEXT, "
                + COL_CHARACTER_AVATAR + " INTEGER DEFAULT 0, "
                + "FOREIGN KEY(" + COL_CHARACTER_STORY_ID + ") REFERENCES " + TABLE_STORY + "(" + COL_STORY_ID + ")"
                + ")";

        String createMaterialTable = "CREATE TABLE IF NOT EXISTS " + TABLE_MATERIAL + " ("
                + COL_MATERIAL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_MATERIAL_CATEGORY + " TEXT NOT NULL, "
                + COL_MATERIAL_TITLE + " TEXT NOT NULL, "
                + COL_MATERIAL_CONTENT + " TEXT NOT NULL, "
                + COL_MATERIAL_CREATE_TIME + " INTEGER, "
                + COL_MATERIAL_SOURCE_URL + " TEXT, "
                + COL_MATERIAL_SOURCE_TITLE + " TEXT, "
                + COL_MATERIAL_SOURCE_TYPE + " TEXT, "
                + COL_MATERIAL_AI_SCORE + " REAL DEFAULT 0, "
                + COL_MATERIAL_RAW_JSON + " TEXT"
                + ")";

        String createBehaviorLogTable = "CREATE TABLE IF NOT EXISTS " + TABLE_BEHAVIOR_LOG + " ("
                + COL_LOG_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "\"" + COL_LOG_ACTION + "\" TEXT NOT NULL, "
                + COL_LOG_TARGET_ID + " INTEGER DEFAULT 0, "
                + COL_LOG_EXTRA + " TEXT, "
                + COL_LOG_CREATE_TIME + " INTEGER"
                + ")";

        db.execSQL(createStoryTable);
        db.execSQL(createCharacterTable);
        db.execSQL(createMaterialTable);
        db.execSQL(createBehaviorLogTable);
    }
    // 数据库升级逻辑，后续补充逻辑
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + TABLE_CHARACTER + " ADD COLUMN " + COL_CHARACTER_DETAIL + " TEXT");
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE " + TABLE_STORY + " ADD COLUMN " + COL_STORY_STRUCTURE + " TEXT");
        }
        if (oldVersion < 4) {
            db.execSQL("ALTER TABLE " + TABLE_MATERIAL + " ADD COLUMN " + COL_MATERIAL_SOURCE_URL + " TEXT");
            db.execSQL("ALTER TABLE " + TABLE_MATERIAL + " ADD COLUMN " + COL_MATERIAL_SOURCE_TITLE + " TEXT");
            db.execSQL("ALTER TABLE " + TABLE_MATERIAL + " ADD COLUMN " + COL_MATERIAL_SOURCE_TYPE + " TEXT");
            db.execSQL("ALTER TABLE " + TABLE_MATERIAL + " ADD COLUMN " + COL_MATERIAL_AI_SCORE + " REAL DEFAULT 0");
            db.execSQL("ALTER TABLE " + TABLE_MATERIAL + " ADD COLUMN " + COL_MATERIAL_RAW_JSON + " TEXT");
        }
        if (oldVersion < 5) {
            db.execSQL("ALTER TABLE " + TABLE_STORY + " ADD COLUMN " + COL_STORY_DESCRIPTION + " TEXT");
        }
        if (oldVersion < 6) {
            db.execSQL("ALTER TABLE " + TABLE_STORY + " ADD COLUMN " + COL_STORY_PLOT_SUMMARY + " TEXT");
        }
        if (oldVersion < 7) {
            db.execSQL("ALTER TABLE " + TABLE_STORY + " ADD COLUMN " + COL_STORY_CATEGORY + " TEXT DEFAULT '全部'");
            db.execSQL("ALTER TABLE " + TABLE_STORY + " ADD COLUMN " + COL_STORY_COVER_COLOR + " TEXT DEFAULT '#1976D2'");
            db.execSQL("ALTER TABLE " + TABLE_STORY + " ADD COLUMN " + COL_STORY_COVER_PATH + " TEXT");
        }
        if (oldVersion < 8) {
            db.execSQL("ALTER TABLE " + TABLE_STORY + " ADD COLUMN " + COL_STORY_WORD_COUNT + " INTEGER DEFAULT 0");
        }
        if (oldVersion < 9) {
            db.execSQL("ALTER TABLE " + TABLE_STORY + " ADD COLUMN " + COL_STORY_SERIES_NAME + " TEXT");
        }
    }
}
