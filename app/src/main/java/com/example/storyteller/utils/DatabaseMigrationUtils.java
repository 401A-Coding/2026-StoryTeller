package com.example.storyteller.utils;

import android.content.Context;
import android.util.Log;

import com.example.storyteller.data.local.db.StoryDao;
import com.example.storyteller.data.local.prefs.PrefsUtils;

/**
 * 数据库迁移工具类
 * 用于处理数据库版本升级后的数据迁移任务
 */
public class DatabaseMigrationUtils {
    private static final String TAG = "DatabaseMigration";

    /**
     * 初始化所有故事的字数统计
     * 在应用首次启动或数据库升级后调用
     */
    public static void initializeWordCounts(Context context) {
        try {
            StoryDao storyDao = new StoryDao(context);
            
            // 检查是否已经初始化过（可以通过SharedPreferences记录）
            PrefsUtils prefs = PrefsUtils.getInstance(context);
            boolean wordCountInitialized = prefs.getBoolean("word_count_initialized", false);
            
            if (!wordCountInitialized) {
                Log.i(TAG, "开始初始化故事字数统计...");
                storyDao.recalculateAllWordCounts();
                prefs.putBoolean("word_count_initialized", true);
                Log.i(TAG, "故事字数统计初始化完成");
            } else {
                Log.d(TAG, "故事字数统计已初始化，跳过");
            }
        } catch (Exception e) {
            Log.e(TAG, "初始化故事字数统计失败", e);
        }
    }
}
