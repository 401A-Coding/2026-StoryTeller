package com.example.storyteller.utils;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

/**
 * 主题管理器
 * 负责管理应用的主题模式（深色/浅色/跟随系统）
 */
public class ThemeManager {

    private static final String PREFS_NAME = "theme_prefs";
    private static final String KEY_THEME_MODE = "theme_mode";

    // 主题模式常量
    public static final int MODE_LIGHT = 0;
    public static final int MODE_DARK = 1;
    public static final int MODE_SYSTEM = 2;

    private static ThemeManager instance;
    private SharedPreferences prefs;

    private ThemeManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized ThemeManager getInstance(Context context) {
        if (instance == null) {
            instance = new ThemeManager(context);
        }
        return instance;
    }

    /**
     * 获取当前主题模式
     */
    public int getThemeMode() {
        return prefs.getInt(KEY_THEME_MODE, MODE_SYSTEM);
    }

    /**
     * 设置主题模式
     */
    public void setThemeMode(int mode) {
        prefs.edit().putInt(KEY_THEME_MODE, mode).apply();
        applyTheme(mode);
    }

    /**
     * 应用主题到 AppCompatDelegate
     */
    public void applyTheme(int mode) {
        switch (mode) {
            case MODE_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case MODE_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case MODE_SYSTEM:
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }

    /**
     * 应用保存的主题设置（在 Application 或 MainActivity 中调用）
     */
    public void applySavedTheme() {
        applyTheme(getThemeMode());
    }

    /**
     * 获取主题模式的中文描述
     */
    public static String getThemeModeDisplayText(int mode) {
        switch (mode) {
            case MODE_LIGHT:
                return "浅色";
            case MODE_DARK:
                return "深色";
            case MODE_SYSTEM:
            default:
                return "跟随系统";
        }
    }

    /**
     * 获取所有主题选项
     */
    public static String[] getThemeModeOptions() {
        return new String[]{"浅色", "深色", "跟随系统"};
    }
}