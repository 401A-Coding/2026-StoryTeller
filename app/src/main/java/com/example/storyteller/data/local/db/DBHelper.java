package com.example.storyteller.data.local.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {
    // 数据库名称和版本
    private static final String DB_NAME = "storyteller.db";
    private static final int DB_VERSION = 24;  // 版本24：补全story_setting表缺失的image_path列

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
    public static final String COL_STORY_PLOT_TREE = "plot_tree_json";  // 剧情版本树工作区
    public static final String COL_STORY_CATEGORY = "category";  // 书架分类：全部/创作中/已完成/已收藏
    public static final String COL_STORY_COVER_COLOR = "cover_color";  // 封面颜色
    public static final String COL_STORY_COVER_PATH = "cover_path";  // 封面图片路径
    public static final String COL_STORY_WORD_COUNT = "word_count";  // 总字数
    public static final String COL_STORY_SERIES_NAME = "series_name";  // 系列名称
    public static final String COL_STORY_OUTLINE_DATA = "outline_data";  // 大纲数据JSON（与structure分离存储）
    public static final String COL_STORY_GLOBAL_OUTLINE = "global_outline";  // 全局大纲（Markdown格式文本）
    public static final String COL_STORY_LAST_EDIT_TIME = "last_edit_time";  // 最近编辑时间

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
    public static final String COL_STORY_SETTING_IMAGE_PATH = "image_path";

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
    
    // 用户写作偏好表字段
    public static final String TABLE_USER_PREFERENCES = "user_preferences";
    public static final String COL_PREF_ID = "id";
    public static final String COL_PREF_STORY_ID = "story_id";  // NULL表示全局偏好
    public static final String COL_PREF_WRITING_STYLE = "writing_style";  // simple/elegant/humorous/suspense/custom
    public static final String COL_PREF_CUSTOM_STYLE = "custom_style";  // 自定义风格描述
    public static final String COL_PREF_NARRATIVE_PERSPECTIVE = "narrative_perspective";  // first/third_limited/third_omniscient
    public static final String COL_PREF_PARAGRAPH_LENGTH = "paragraph_length";  // short/medium/long
    public static final String COL_PREF_AVOID_BLOODY = "avoid_bloody";  // 避免血腥
    public static final String COL_PREF_AVOID_VIOLENCE = "avoid_violence";  // 避免暴力
    public static final String COL_PREF_AVOID_SENSITIVE = "avoid_sensitive";  // 避免敏感话题
    public static final String COL_PREF_SPECIAL_REQUIREMENTS = "special_requirements";  // 其他特殊要求
    public static final String COL_PREF_SOURCE = "source";  // manual/ai_extracted
    public static final String COL_PREF_UPDATED_AT = "updated_at";
    
    // AI分析偏好字段（新增）
    public static final String COL_PREF_AI_WRITING_STYLE = "ai_writing_style";
    public static final String COL_PREF_AI_NARRATIVE_PERSPECTIVE = "ai_narrative_perspective";
    public static final String COL_PREF_AI_PARAGRAPH_LENGTH = "ai_paragraph_length";
    public static final String COL_PREF_AI_AVOID_BLOODY = "ai_avoid_bloody";
    public static final String COL_PREF_AI_AVOID_VIOLENCE = "ai_avoid_violence";
    public static final String COL_PREF_AI_AVOID_SENSITIVE = "ai_avoid_sensitive";
    public static final String COL_PREF_AI_SPECIAL_REQUIREMENTS = "ai_special_requirements";
    public static final String COL_PREF_AI_SOURCE = "ai_source";

    // AI记忆表字段
    public static final String TABLE_AI_MEMORY = "ai_memory";
    public static final String COL_MEMORY_ID = "id";
    public static final String COL_MEMORY_STORY_ID = "story_id";  // NULL表示全局记忆
    public static final String COL_MEMORY_TYPE = "memory_type";  // plot/personality/world/other
    public static final String COL_MEMORY_TITLE = "title";  // 记忆标题（用于显示）
    public static final String COL_MEMORY_CONTENT = "content";  // 记忆内容
    public static final String COL_MEMORY_IMPORTANCE = "importance";  // 重要性 1-5
    public static final String COL_MEMORY_CREATED_AT = "created_at";
    public static final String COL_MEMORY_UPDATED_AT = "updated_at";

    // === 设定关系表字段 ===
    public static final String TABLE_SETTING_RELATIONSHIPS = "setting_relationships";
    public static final String COL_REL_ID = "id";
    public static final String COL_REL_STORY_ID = "story_id";
    public static final String COL_REL_SOURCE_SETTING_ID = "source_setting_id";
    public static final String COL_REL_TARGET_SETTING_ID = "target_setting_id";
    public static final String COL_REL_RELATIONSHIP_TYPE = "relationship_type";
    public static final String COL_REL_DESCRIPTION = "description";
    public static final String COL_REL_SOURCE_TYPE = "source_type";  // manual/ai_inferred/user_confirmed
    public static final String COL_REL_CONFIDENCE = "confidence";  // 置信度 0-1
    public static final String COL_REL_IS_DIRECTED = "is_directed";  // 是否为有向关系（1=有向，0=无向）
    public static final String COL_REL_CREATE_TIME = "create_time";
    public static final String COL_REL_UPDATE_TIME = "update_time";

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
                + COL_STORY_PLOT_TREE + " TEXT, "
                + COL_STORY_CATEGORY + " TEXT DEFAULT '创作中', "
                + COL_STORY_COVER_COLOR + " TEXT DEFAULT '#1976D2', "
                + COL_STORY_COVER_PATH + " TEXT, "
                + COL_STORY_WORD_COUNT + " INTEGER DEFAULT 0, "
                + COL_STORY_SERIES_NAME + " TEXT, "
                + COL_STORY_OUTLINE_DATA + " TEXT, "
                + COL_STORY_GLOBAL_OUTLINE + " TEXT, "
                + COL_STORY_LAST_EDIT_TIME + " INTEGER"
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
        
        // 创建用户写作偏好表
        createUserPreferencesTable(db);
        
        // 创建AI记忆表
        createAiMemoryTable(db);
        
        // 创建设定关系表
        createSettingRelationshipsTable(db);
    }
    
    /**
     * 添加AI分析偏好字段（版本18升级）
     */
    private void addAiPreferenceColumns(SQLiteDatabase db) {
        try {
            db.execSQL("ALTER TABLE " + TABLE_USER_PREFERENCES + " ADD COLUMN " + COL_PREF_AI_WRITING_STYLE + " TEXT");
        } catch (Exception e) { /* 字段可能已存在 */ }
        try {
            db.execSQL("ALTER TABLE " + TABLE_USER_PREFERENCES + " ADD COLUMN " + COL_PREF_AI_NARRATIVE_PERSPECTIVE + " TEXT");
        } catch (Exception e) { /* 字段可能已存在 */ }
        try {
            db.execSQL("ALTER TABLE " + TABLE_USER_PREFERENCES + " ADD COLUMN " + COL_PREF_AI_PARAGRAPH_LENGTH + " TEXT");
        } catch (Exception e) { /* 字段可能已存在 */ }
        try {
            db.execSQL("ALTER TABLE " + TABLE_USER_PREFERENCES + " ADD COLUMN " + COL_PREF_AI_AVOID_BLOODY + " INTEGER");
        } catch (Exception e) { /* 字段可能已存在 */ }
        try {
            db.execSQL("ALTER TABLE " + TABLE_USER_PREFERENCES + " ADD COLUMN " + COL_PREF_AI_AVOID_VIOLENCE + " INTEGER");
        } catch (Exception e) { /* 字段可能已存在 */ }
        try {
            db.execSQL("ALTER TABLE " + TABLE_USER_PREFERENCES + " ADD COLUMN " + COL_PREF_AI_AVOID_SENSITIVE + " INTEGER");
        } catch (Exception e) { /* 字段可能已存在 */ }
        try {
            db.execSQL("ALTER TABLE " + TABLE_USER_PREFERENCES + " ADD COLUMN " + COL_PREF_AI_SPECIAL_REQUIREMENTS + " TEXT");
        } catch (Exception e) { /* 字段可能已存在 */ }
        try {
            db.execSQL("ALTER TABLE " + TABLE_USER_PREFERENCES + " ADD COLUMN " + COL_PREF_AI_SOURCE + " TEXT");
        } catch (Exception e) { /* 字段可能已存在 */ }
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
        if (oldVersion < 15) {
            // 版本15：添加last_edit_time字段，用于记录最近编辑时间
            try {
                db.execSQL("ALTER TABLE " + TABLE_STORY + " ADD COLUMN " + COL_STORY_LAST_EDIT_TIME + " INTEGER");
                // 将现有数据的last_edit_time初始化为create_time
                db.execSQL("UPDATE " + TABLE_STORY + " SET " + COL_STORY_LAST_EDIT_TIME + " = " + COL_STORY_CREATE_TIME + " WHERE " + COL_STORY_LAST_EDIT_TIME + " IS NULL");
            } catch (Exception e) {
                // 字段可能已存在
            }
        }
        if (oldVersion < 16) {
            // 版本16：添加用户写作偏好表
            createUserPreferencesTable(db);
        }
        if (oldVersion < 17) {
            // 版本17：添加AI记忆表
            createAiMemoryTable(db);
        }
        if (oldVersion < 19) {
            // 版本19：添加设定关系表
            createSettingRelationshipsTable(db);
        }
        if (oldVersion < 20) {
            // 版本20：添加 is_directed 字段到设定关系表
            try {
                db.execSQL("ALTER TABLE " + TABLE_SETTING_RELATIONSHIPS + " ADD COLUMN " + COL_REL_IS_DIRECTED + " INTEGER DEFAULT 1");
            } catch (Exception e) {
                // 字段可能已存在
            }
        }
        if (oldVersion < 21) {
            // 版本21：移除关系表UNIQUE约束（SQLite不支持删除约束，需要重建表）
            // 检查并删除旧表，重建（仅删除UNIQUE约束）
            try {
                // SQLite不支持直接删除UNIQUE约束，需要重建表
                // 先备份数据，然后重建表（不带UNIQUE）
                db.execSQL("ALTER TABLE " + TABLE_SETTING_RELATIONSHIPS + " RENAME TO " + TABLE_SETTING_RELATIONSHIPS + "_old");
                createSettingRelationshipsTable(db);
                // 迁移数据
                db.execSQL("INSERT INTO " + TABLE_SETTING_RELATIONSHIPS + " SELECT * FROM " + TABLE_SETTING_RELATIONSHIPS + "_old");
                db.execSQL("DROP TABLE " + TABLE_SETTING_RELATIONSHIPS + "_old");
            } catch (Exception e) {
                // 可能表不存在或其他错误
            }
        }
        if (oldVersion < 22) {
            // 版本22：添加设定配图字段
            try {
                db.execSQL("ALTER TABLE " + TABLE_STORY_SETTING + " ADD COLUMN " + COL_STORY_SETTING_IMAGE_PATH + " TEXT");
            } catch (Exception e) {
                // 字段可能已存在
            }
        }
        if (oldVersion < 23) {
            // 版本23：添加剧情版本树字段
            try {
                db.execSQL("ALTER TABLE " + TABLE_STORY + " ADD COLUMN " + COL_STORY_PLOT_TREE + " TEXT");
            } catch (Exception e) {
                // 字段可能已存在
            }
        }
        if (oldVersion < 24) {
            // 版本24：补全story_setting表中可能缺失的image_path列
            // 此前版本22升级时虽然有ALTER TABLE逻辑，但部分用户的数据库表结构中仍缺少该列
            // 此处再次执行补全，使用try-catch确保幂等性
            try {
                db.execSQL("ALTER TABLE " + TABLE_STORY_SETTING + " ADD COLUMN " + COL_STORY_SETTING_IMAGE_PATH + " TEXT");
            } catch (Exception e) {
                // 字段可能已存在
            }
        }
    }

    /**
     * 创建设定关系表
     * 用于存储设定之间的关联关系，支持知识图谱构建
     */
    private void createSettingRelationshipsTable(SQLiteDatabase db) {
        String createTable = "CREATE TABLE IF NOT EXISTS " + TABLE_SETTING_RELATIONSHIPS + " ("
                + COL_REL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_REL_STORY_ID + " INTEGER NOT NULL, "
                + COL_REL_SOURCE_SETTING_ID + " INTEGER NOT NULL, "
                + COL_REL_TARGET_SETTING_ID + " INTEGER NOT NULL, "
                + COL_REL_RELATIONSHIP_TYPE + " TEXT NOT NULL, "
                + COL_REL_DESCRIPTION + " TEXT, "
                + COL_REL_SOURCE_TYPE + " TEXT DEFAULT 'manual', "
                + COL_REL_CONFIDENCE + " REAL DEFAULT 0.8, "
                + COL_REL_IS_DIRECTED + " INTEGER DEFAULT 1, "
                + COL_REL_CREATE_TIME + " INTEGER NOT NULL, "
                + COL_REL_UPDATE_TIME + " INTEGER NOT NULL, "
                + "FOREIGN KEY(" + COL_REL_SOURCE_SETTING_ID + ") REFERENCES " + 
                  TABLE_STORY_SETTING + "(" + COL_STORY_SETTING_ID + ") ON DELETE CASCADE, "
                + "FOREIGN KEY(" + COL_REL_TARGET_SETTING_ID + ") REFERENCES " + 
                  TABLE_STORY_SETTING + "(" + COL_STORY_SETTING_ID + ") ON DELETE CASCADE"
                + ")";
        db.execSQL(createTable);
        
        // 创建索引
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_rel_story_id ON " + 
                   TABLE_SETTING_RELATIONSHIPS + "(" + COL_REL_STORY_ID + ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_rel_source ON " + 
                   TABLE_SETTING_RELATIONSHIPS + "(" + COL_REL_SOURCE_SETTING_ID + ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_rel_target ON " + 
                   TABLE_SETTING_RELATIONSHIPS + "(" + COL_REL_TARGET_SETTING_ID + ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_rel_type ON " + 
                   TABLE_SETTING_RELATIONSHIPS + "(" + COL_REL_RELATIONSHIP_TYPE + ")");
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
                + COL_STORY_SETTING_IMAGE_PATH + " TEXT, "
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
    
    /**
     * 创建用户写作偏好表
     * story_id为NULL表示全局偏好，非NULL表示小说专属偏好
     * 包含用户手动设置的偏好和AI分析的偏好
     */
    private void createUserPreferencesTable(SQLiteDatabase db) {
        String createTable = "CREATE TABLE IF NOT EXISTS " + TABLE_USER_PREFERENCES + " ("
                + COL_PREF_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_PREF_STORY_ID + " INTEGER, "  // NULL表示全局偏好
                + COL_PREF_WRITING_STYLE + " TEXT, "
                + COL_PREF_CUSTOM_STYLE + " TEXT, "
                + COL_PREF_NARRATIVE_PERSPECTIVE + " TEXT, "
                + COL_PREF_PARAGRAPH_LENGTH + " TEXT, "
                + COL_PREF_AVOID_BLOODY + " INTEGER DEFAULT 0, "
                + COL_PREF_AVOID_VIOLENCE + " INTEGER DEFAULT 0, "
                + COL_PREF_AVOID_SENSITIVE + " INTEGER DEFAULT 0, "
                + COL_PREF_SPECIAL_REQUIREMENTS + " TEXT, "
                + COL_PREF_SOURCE + " TEXT DEFAULT 'manual', "
                // AI分析的偏好字段
                + COL_PREF_AI_WRITING_STYLE + " TEXT, "
                + COL_PREF_AI_NARRATIVE_PERSPECTIVE + " TEXT, "
                + COL_PREF_AI_PARAGRAPH_LENGTH + " TEXT, "
                + COL_PREF_AI_AVOID_BLOODY + " INTEGER, "
                + COL_PREF_AI_AVOID_VIOLENCE + " INTEGER, "
                + COL_PREF_AI_AVOID_SENSITIVE + " INTEGER, "
                + COL_PREF_AI_SPECIAL_REQUIREMENTS + " TEXT, "
                + COL_PREF_AI_SOURCE + " TEXT, "
                + COL_PREF_UPDATED_AT + " INTEGER, "
                + "FOREIGN KEY(" + COL_PREF_STORY_ID + ") REFERENCES " + TABLE_STORY + "(" + COL_STORY_ID + ") ON DELETE CASCADE"
                + ")";
        db.execSQL(createTable);
        
        // 创建索引
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_user_pref_story_id ON " + 
                   TABLE_USER_PREFERENCES + "(" + COL_PREF_STORY_ID + ")");
    }
    
    /**
     * 创建AI记忆表
     * 用于存储AI认为重要的上下文信息，可按小说隔离
     */
    private void createAiMemoryTable(SQLiteDatabase db) {
        String createTable = "CREATE TABLE IF NOT EXISTS " + TABLE_AI_MEMORY + " ("
                + COL_MEMORY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_MEMORY_STORY_ID + " INTEGER, "  // NULL表示全局记忆
                + COL_MEMORY_TYPE + " TEXT NOT NULL, "  // plot/personality/world/other
                + COL_MEMORY_TITLE + " TEXT NOT NULL, "
                + COL_MEMORY_CONTENT + " TEXT, "
                + COL_MEMORY_IMPORTANCE + " INTEGER DEFAULT 3, "  // 重要性 1-5
                + COL_MEMORY_CREATED_AT + " INTEGER, "
                + COL_MEMORY_UPDATED_AT + " INTEGER, "
                + "FOREIGN KEY(" + COL_MEMORY_STORY_ID + ") REFERENCES " + TABLE_STORY + "(" + COL_STORY_ID + ") ON DELETE CASCADE"
                + ")";
        db.execSQL(createTable);
        
        // 创建索引
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_ai_memory_story_id ON " + 
                   TABLE_AI_MEMORY + "(" + COL_MEMORY_STORY_ID + ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_ai_memory_type ON " + 
                   TABLE_AI_MEMORY + "(" + COL_MEMORY_TYPE + ")");
    }
}
