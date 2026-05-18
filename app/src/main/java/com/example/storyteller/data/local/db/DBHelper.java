package com.example.storyteller.data.local.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {
    // 数据库名称和版本
    private static final String DB_NAME = "storyteller.db";
    private static final int DB_VERSION = 14;  // 升级到版本14，添加story_documents表

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
    public static final String COL_STORY_OUTLINE_DATA = "outline_data";  // 大纲数据JSON（与structure分离存储）
    public static final String COL_STORY_GLOBAL_OUTLINE = "global_outline";  // 全局大纲（Markdown格式文本）

    // 故事文档表字段
    public static final String TABLE_STORY_DOCUMENT = "story_document";
    public static final String COL_STORY_DOCUMENT_ID = "id";
    public static final String COL_STORY_DOCUMENT_STORY_ID = "story_id";
    public static final String COL_STORY_DOCUMENT_TITLE = "title";
    public static final String COL_STORY_DOCUMENT_CONTENT = "content";
    public static final String COL_STORY_DOCUMENT_CATEGORY = "category";
    public static final String COL_STORY_DOCUMENT_CREATE_TIME = "create_time";
    public static final String COL_STORY_DOCUMENT_UPDATE_TIME = "update_time";

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

    // 导入小说表字段
    public static final String TABLE_IMPORTED_NOVEL = "imported_novel";
    public static final String COL_IMPORTED_NOVEL_ID = "id";
    public static final String COL_IMPORTED_NOVEL_TITLE = "title";
    public static final String COL_IMPORTED_NOVEL_AUTHOR = "author";
    public static final String COL_IMPORTED_NOVEL_SOURCE_URL = "source_url";
    public static final String COL_IMPORTED_NOVEL_COVER_URL = "cover_url";
    public static final String COL_IMPORTED_NOVEL_DESCRIPTION = "description";
    public static final String COL_IMPORTED_NOVEL_IMPORT_TIME = "import_time";
    public static final String COL_IMPORTED_NOVEL_STATUS = "status";
    public static final String COL_IMPORTED_NOVEL_STRUCTURE_JSON = "structure_json";
    public static final String COL_IMPORTED_NOVEL_CONTENT_DIR = "content_dir";
    public static final String COL_IMPORTED_NOVEL_TOTAL_CHAPTERS = "total_chapters";
    public static final String COL_IMPORTED_NOVEL_TOTAL_WORDS = "total_words";
    public static final String COL_IMPORTED_NOVEL_TAGS = "tags";

    // === 新增强：设定相关表字段 ===
    
    // 全局素材库表
    public static final String TABLE_GLOBAL_MATERIAL = "global_material";
    public static final String COL_GLOBAL_MATERIAL_ID = "id";
    public static final String COL_GLOBAL_MATERIAL_CATEGORY = "category";
    public static final String COL_GLOBAL_MATERIAL_SUB_CATEGORY = "sub_category";
    public static final String COL_GLOBAL_MATERIAL_TITLE = "title";
    public static final String COL_GLOBAL_MATERIAL_SUMMARY = "summary";
    public static final String COL_GLOBAL_MATERIAL_DETAIL = "detail";
    public static final String COL_GLOBAL_MATERIAL_ATTRIBUTES = "attributes";
    public static final String COL_GLOBAL_MATERIAL_TAGS = "tags";
    public static final String COL_GLOBAL_MATERIAL_ALIASES = "aliases";
    public static final String COL_GLOBAL_MATERIAL_SPECIFIC_ATTRS = "specific_attributes";
    public static final String COL_GLOBAL_MATERIAL_SOURCE_TYPE = "source_type";
    public static final String COL_GLOBAL_MATERIAL_SOURCE_URL = "source_url";
    public static final String COL_GLOBAL_MATERIAL_AI_CONFIDENCE = "ai_confidence";
    public static final String COL_GLOBAL_MATERIAL_RAW_JSON = "raw_json";
    public static final String COL_GLOBAL_MATERIAL_CREATE_TIME = "create_time";
    public static final String COL_GLOBAL_MATERIAL_UPDATE_TIME = "update_time";
    public static final String COL_GLOBAL_MATERIAL_USAGE_COUNT = "usage_count";
    public static final String COL_GLOBAL_MATERIAL_IS_PUBLIC = "is_public";
    
    // 小说专属设定表
    public static final String TABLE_STORY_SETTING = "story_setting";
    public static final String COL_STORY_SETTING_ID = "id";
    public static final String COL_STORY_SETTING_STORY_ID = "story_id";
    public static final String COL_STORY_SETTING_CATEGORY = "category";
    public static final String COL_STORY_SETTING_SUB_CATEGORY = "sub_category";
    public static final String COL_STORY_SETTING_TITLE = "title";
    public static final String COL_STORY_SETTING_SUMMARY = "summary";
    public static final String COL_STORY_SETTING_DETAIL = "detail";
    public static final String COL_STORY_SETTING_ATTRIBUTES = "attributes";
    public static final String COL_STORY_SETTING_TAGS = "tags";
    public static final String COL_STORY_SETTING_ALIASES = "aliases";
    public static final String COL_STORY_SETTING_SPECIFIC_ATTRS = "specific_attributes";
    public static final String COL_STORY_SETTING_SOURCE_MATERIAL_ID = "source_material_id";
    public static final String COL_STORY_SETTING_SOURCE_TYPE = "source_type";
    public static final String COL_STORY_SETTING_SOURCE_URL = "source_url";
    public static final String COL_STORY_SETTING_SOURCE_TITLE = "source_title";
    public static final String COL_STORY_SETTING_AI_CONFIDENCE = "ai_confidence";
    public static final String COL_STORY_SETTING_RAW_JSON = "raw_json";
    public static final String COL_STORY_SETTING_IMPORT_TIME = "import_time";
    public static final String COL_STORY_SETTING_LAST_SYNC_TIME = "last_sync_time";
    public static final String COL_STORY_SETTING_SYNC_ENABLED = "sync_enabled";
    public static final String COL_STORY_SETTING_HAS_UPDATES = "has_updates";
    public static final String COL_STORY_SETTING_CREATE_TIME = "create_time";
    public static final String COL_STORY_SETTING_UPDATE_TIME = "update_time";
    public static final String COL_STORY_SETTING_IS_FAVORITE = "is_favorite";
    public static final String COL_STORY_SETTING_USAGE_COUNT = "usage_count";

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
                + COL_STORY_PLOT_SUMMARY + " TEXT, "
                + COL_STORY_CATEGORY + " TEXT DEFAULT '创作中', "
                + COL_STORY_COVER_COLOR + " TEXT DEFAULT '#1976D2', "
                + COL_STORY_COVER_PATH + " TEXT, "
                + COL_STORY_WORD_COUNT + " INTEGER DEFAULT 0, "
                + COL_STORY_SERIES_NAME + " TEXT, "
                + COL_STORY_OUTLINE_DATA + " TEXT"
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

        String createImportedNovelTable = "CREATE TABLE IF NOT EXISTS " + TABLE_IMPORTED_NOVEL + " ("
                + COL_IMPORTED_NOVEL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_IMPORTED_NOVEL_TITLE + " TEXT NOT NULL, "
                + COL_IMPORTED_NOVEL_AUTHOR + " TEXT, "
                + COL_IMPORTED_NOVEL_SOURCE_URL + " TEXT NOT NULL UNIQUE, "
                + COL_IMPORTED_NOVEL_COVER_URL + " TEXT, "
                + COL_IMPORTED_NOVEL_DESCRIPTION + " TEXT, "
                + COL_IMPORTED_NOVEL_IMPORT_TIME + " INTEGER NOT NULL, "
                + COL_IMPORTED_NOVEL_STATUS + " TEXT DEFAULT 'imported', "
                + COL_IMPORTED_NOVEL_STRUCTURE_JSON + " TEXT, "
                + COL_IMPORTED_NOVEL_CONTENT_DIR + " TEXT, "
                + COL_IMPORTED_NOVEL_TOTAL_CHAPTERS + " INTEGER DEFAULT 0, "
                + COL_IMPORTED_NOVEL_TOTAL_WORDS + " INTEGER DEFAULT 0, "
                + COL_IMPORTED_NOVEL_TAGS + " TEXT"
                + ")";

        db.execSQL(createStoryTable);
        db.execSQL(createCharacterTable);
        db.execSQL(createMaterialTable);
        db.execSQL(createBehaviorLogTable);
        db.execSQL(createImportedNovelTable);
        
        // 创建新表：全局素材库、小说设定和故事文档
        createGlobalMaterialTable(db);
        createStorySettingTable(db);
        createStoryDocumentTable(db);
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
            try {
                db.execSQL("ALTER TABLE " + TABLE_STORY + " ADD COLUMN " + COL_STORY_WORD_COUNT + " INTEGER DEFAULT 0");
            } catch (Exception e) {
                // 字段可能已存在
            }
        }
        if (oldVersion < 9) {
            try {
                db.execSQL("ALTER TABLE " + TABLE_STORY + " ADD COLUMN " + COL_STORY_SERIES_NAME + " TEXT");
            } catch (Exception e) {
                // 字段可能已存在
            }
        }
        if (oldVersion < 10) {
            createImportedNovelTableIfNotExists(db);
        }
        if (oldVersion < 11) {
            // 版本11：添加设定相关表
            createGlobalMaterialTable(db);
            createStorySettingTable(db);
        }
        if (oldVersion < 12) {
            // 版本12：添加outline_data字段，用于分离存储大纲数据
            try {
                db.execSQL("ALTER TABLE " + TABLE_STORY + " ADD COLUMN " + COL_STORY_OUTLINE_DATA + " TEXT");
            } catch (Exception e) {
                // 字段可能已存在
            }
        }
        if (oldVersion < 13) {
            // 版本13：添加global_outline字段，用于存储全局大纲（Markdown格式）
            try {
                db.execSQL("ALTER TABLE " + TABLE_STORY + " ADD COLUMN " + COL_STORY_GLOBAL_OUTLINE + " TEXT");
            } catch (Exception e) {
                // 字段可能已存在
            }
        }
        if (oldVersion < 14) {
            // 版本14：添加story_documents表，用于存储故事相关文档
            createStoryDocumentTable(db);
        }
    }

    private void createImportedNovelTableIfNotExists(SQLiteDatabase db) {
        String createTable = "CREATE TABLE IF NOT EXISTS " + TABLE_IMPORTED_NOVEL + " ("
                + COL_IMPORTED_NOVEL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_IMPORTED_NOVEL_TITLE + " TEXT NOT NULL, "
                + COL_IMPORTED_NOVEL_AUTHOR + " TEXT, "
                + COL_IMPORTED_NOVEL_SOURCE_URL + " TEXT NOT NULL UNIQUE, "
                + COL_IMPORTED_NOVEL_COVER_URL + " TEXT, "
                + COL_IMPORTED_NOVEL_DESCRIPTION + " TEXT, "
                + COL_IMPORTED_NOVEL_IMPORT_TIME + " INTEGER NOT NULL, "
                + COL_IMPORTED_NOVEL_STATUS + " TEXT DEFAULT 'imported', "
                + COL_IMPORTED_NOVEL_STRUCTURE_JSON + " TEXT, "
                + COL_IMPORTED_NOVEL_CONTENT_DIR + " TEXT, "
                + COL_IMPORTED_NOVEL_TOTAL_CHAPTERS + " INTEGER DEFAULT 0, "
                + COL_IMPORTED_NOVEL_TOTAL_WORDS + " INTEGER DEFAULT 0, "
                + COL_IMPORTED_NOVEL_TAGS + " TEXT"
                + ")";
        db.execSQL(createTable);
    }
    
    /**
     * 创建全局素材库表
     */
    private void createGlobalMaterialTable(SQLiteDatabase db) {
        String createTable = "CREATE TABLE IF NOT EXISTS " + TABLE_GLOBAL_MATERIAL + " ("
                + COL_GLOBAL_MATERIAL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_GLOBAL_MATERIAL_CATEGORY + " TEXT NOT NULL, "
                + COL_GLOBAL_MATERIAL_SUB_CATEGORY + " TEXT NOT NULL, "
                + COL_GLOBAL_MATERIAL_TITLE + " TEXT NOT NULL, "
                + COL_GLOBAL_MATERIAL_SUMMARY + " TEXT, "
                + COL_GLOBAL_MATERIAL_DETAIL + " TEXT, "
                + COL_GLOBAL_MATERIAL_ATTRIBUTES + " TEXT, "
                + COL_GLOBAL_MATERIAL_TAGS + " TEXT, "
                + COL_GLOBAL_MATERIAL_ALIASES + " TEXT, "
                + COL_GLOBAL_MATERIAL_SPECIFIC_ATTRS + " TEXT, "
                + COL_GLOBAL_MATERIAL_SOURCE_TYPE + " TEXT DEFAULT 'manual', "
                + COL_GLOBAL_MATERIAL_SOURCE_URL + " TEXT, "
                + COL_GLOBAL_MATERIAL_AI_CONFIDENCE + " REAL DEFAULT 0.5, "
                + COL_GLOBAL_MATERIAL_RAW_JSON + " TEXT, "
                + COL_GLOBAL_MATERIAL_CREATE_TIME + " INTEGER NOT NULL, "
                + COL_GLOBAL_MATERIAL_UPDATE_TIME + " INTEGER NOT NULL, "
                + COL_GLOBAL_MATERIAL_USAGE_COUNT + " INTEGER DEFAULT 0, "
                + COL_GLOBAL_MATERIAL_IS_PUBLIC + " INTEGER DEFAULT 1"
                + ")";
        db.execSQL(createTable);
        
        // 创建索引
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_global_material_category ON " + 
                   TABLE_GLOBAL_MATERIAL + "(" + COL_GLOBAL_MATERIAL_CATEGORY + ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_global_material_sub_category ON " + 
                   TABLE_GLOBAL_MATERIAL + "(" + COL_GLOBAL_MATERIAL_SUB_CATEGORY + ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_global_material_title ON " + 
                   TABLE_GLOBAL_MATERIAL + "(" + COL_GLOBAL_MATERIAL_TITLE + ")");
    }
    
    /**
     * 创建小说专属设定表
     */
    private void createStorySettingTable(SQLiteDatabase db) {
        String createTable = "CREATE TABLE IF NOT EXISTS " + TABLE_STORY_SETTING + " ("
                + COL_STORY_SETTING_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_STORY_SETTING_STORY_ID + " INTEGER NOT NULL DEFAULT 0, "
                + COL_STORY_SETTING_CATEGORY + " TEXT NOT NULL, "
                + COL_STORY_SETTING_SUB_CATEGORY + " TEXT NOT NULL, "
                + COL_STORY_SETTING_TITLE + " TEXT NOT NULL, "
                + COL_STORY_SETTING_SUMMARY + " TEXT, "
                + COL_STORY_SETTING_DETAIL + " TEXT, "
                + COL_STORY_SETTING_ATTRIBUTES + " TEXT, "
                + COL_STORY_SETTING_TAGS + " TEXT, "
                + COL_STORY_SETTING_ALIASES + " TEXT, "
                + COL_STORY_SETTING_SPECIFIC_ATTRS + " TEXT, "
                + COL_STORY_SETTING_SOURCE_MATERIAL_ID + " INTEGER DEFAULT 0, "
                + COL_STORY_SETTING_SOURCE_TYPE + " TEXT DEFAULT 'original', "
                + COL_STORY_SETTING_SOURCE_URL + " TEXT, "
                + COL_STORY_SETTING_SOURCE_TITLE + " TEXT, "
                + COL_STORY_SETTING_AI_CONFIDENCE + " REAL DEFAULT 0.5, "
                + COL_STORY_SETTING_RAW_JSON + " TEXT, "
                + COL_STORY_SETTING_IMPORT_TIME + " INTEGER DEFAULT 0, "
                + COL_STORY_SETTING_LAST_SYNC_TIME + " INTEGER DEFAULT 0, "
                + COL_STORY_SETTING_SYNC_ENABLED + " INTEGER DEFAULT 0, "
                + COL_STORY_SETTING_HAS_UPDATES + " INTEGER DEFAULT 0, "
                + COL_STORY_SETTING_CREATE_TIME + " INTEGER NOT NULL, "
                + COL_STORY_SETTING_UPDATE_TIME + " INTEGER NOT NULL, "
                + COL_STORY_SETTING_IS_FAVORITE + " INTEGER DEFAULT 0, "
                + COL_STORY_SETTING_USAGE_COUNT + " INTEGER DEFAULT 0, "
                + "FOREIGN KEY (" + COL_STORY_SETTING_STORY_ID + ") REFERENCES " + 
                TABLE_STORY + "(" + COL_STORY_ID + ") ON DELETE CASCADE, "
                + "FOREIGN KEY (" + COL_STORY_SETTING_SOURCE_MATERIAL_ID + ") REFERENCES " +
                TABLE_GLOBAL_MATERIAL + "(" + COL_GLOBAL_MATERIAL_ID + ")"
                + ")";
        db.execSQL(createTable);
        
        // 创建索引
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_story_setting_story_id ON " + 
                   TABLE_STORY_SETTING + "(" + COL_STORY_SETTING_STORY_ID + ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_story_setting_category ON " + 
                   TABLE_STORY_SETTING + "(" + COL_STORY_SETTING_CATEGORY + ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_story_setting_sub_category ON " + 
                   TABLE_STORY_SETTING + "(" + COL_STORY_SETTING_SUB_CATEGORY + ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_story_setting_title ON " + 
                   TABLE_STORY_SETTING + "(" + COL_STORY_SETTING_TITLE + ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_story_setting_source_material ON " + 
                   TABLE_STORY_SETTING + "(" + COL_STORY_SETTING_SOURCE_MATERIAL_ID + ")");
    }
    
    /**
     * 创建故事文档表
     */
    private void createStoryDocumentTable(SQLiteDatabase db) {
        String createTable = "CREATE TABLE IF NOT EXISTS " + TABLE_STORY_DOCUMENT + " ("
                + COL_STORY_DOCUMENT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_STORY_DOCUMENT_STORY_ID + " INTEGER NOT NULL, "
                + COL_STORY_DOCUMENT_TITLE + " TEXT NOT NULL, "
                + COL_STORY_DOCUMENT_CONTENT + " TEXT DEFAULT '', "
                + COL_STORY_DOCUMENT_CATEGORY + " TEXT DEFAULT 'general', "
                + COL_STORY_DOCUMENT_CREATE_TIME + " INTEGER NOT NULL, "
                + COL_STORY_DOCUMENT_UPDATE_TIME + " INTEGER NOT NULL, "
                + "FOREIGN KEY (" + COL_STORY_DOCUMENT_STORY_ID + ") REFERENCES " + 
                TABLE_STORY + "(" + COL_STORY_ID + ") ON DELETE CASCADE"
                + ")";
        db.execSQL(createTable);
        
        // 创建索引
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_story_doc_story_id ON " + 
                   TABLE_STORY_DOCUMENT + "(" + COL_STORY_DOCUMENT_STORY_ID + ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_story_doc_category ON " + 
                   TABLE_STORY_DOCUMENT + "(" + COL_STORY_DOCUMENT_CATEGORY + ")");
    }
}
