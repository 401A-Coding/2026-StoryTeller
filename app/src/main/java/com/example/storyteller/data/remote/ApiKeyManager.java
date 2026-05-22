package com.example.storyteller.data.remote;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;
import java.io.IOException;
import java.security.GeneralSecurityException;

public class ApiKeyManager {
    private static final String PREFS_NAME = "encrypted_api_prefs";
    private static final String KEY_DEEPSEEK_API_KEY = "deepseek_api_key";
    private static final String KEY_MINIMAX_API_KEY = "minimax_api_key";

    private static SharedPreferences getEncryptedPrefs(Context context) throws GeneralSecurityException, IOException {
        MasterKey masterKey = new MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();

        return EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        );
    }

    /**
     * 保存指定提供商的API密钥
     */
    public static void saveApiKey(Context context, ModelConfig.Provider provider, String apiKey) {
        try {
            SharedPreferences prefs = getEncryptedPrefs(context);
            String key = getKeyName(provider);
            prefs.edit().putString(key, apiKey).apply();
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取指定提供商的API密钥
     */
    public static String getApiKey(Context context, ModelConfig.Provider provider) {
        try {
            SharedPreferences prefs = getEncryptedPrefs(context);
            return prefs.getString(getKeyName(provider), "");
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    private static String getKeyName(ModelConfig.Provider provider) {
        switch (provider) {
            case MINIMAX:
                return KEY_MINIMAX_API_KEY;
            case DEEPSEEK:
            default:
                return KEY_DEEPSEEK_API_KEY;
        }
    }

    // 向后兼容：DeepSeek API密钥
    public static void saveApiKey(Context context, String apiKey) {
        saveApiKey(context, ModelConfig.Provider.DEEPSEEK, apiKey);
    }

    public static String getApiKey(Context context) {
        return getApiKey(context, ModelConfig.Provider.DEEPSEEK);
    }
}
