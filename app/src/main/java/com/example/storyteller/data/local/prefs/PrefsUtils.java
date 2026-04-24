package com.example.storyteller.data.local.prefs;

import android.content.Context;
import android.content.SharedPreferences;

public class PrefsUtils {
    private static final String PREFS_NAME = "storyteller_prefs";
    private static PrefsUtils instance;
    private final SharedPreferences prefs;

    private PrefsUtils(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized PrefsUtils getInstance(Context context) {
        if (instance == null) {
            instance = new PrefsUtils(context);
        }
        return instance;
    }

    // 存储字符串（如默认故事类型）
    public void putString(String key, String value) {
        prefs.edit().putString(key, value).apply();
    }

    // 获取字符串
    public String getString(String key, String defaultValue) {
        return prefs.getString(key, defaultValue);
    }

    // 存储布尔值（如是否开启夜间模式）
    public void putBoolean(String key, boolean value) {
        prefs.edit().putBoolean(key, value).apply();
    }

    // 获取布尔值
    public boolean getBoolean(String key, boolean defaultValue) {
        return prefs.getBoolean(key, defaultValue);
    }

    // 后续可扩展存储int、long等类型
}