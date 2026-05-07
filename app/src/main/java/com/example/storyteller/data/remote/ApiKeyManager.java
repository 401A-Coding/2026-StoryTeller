package com.example.storyteller.data.remote;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;
import java.io.IOException;
import java.security.GeneralSecurityException;

public class ApiKeyManager {
    private static final String PREFS_NAME = "encrypted_api_prefs";
    private static final String KEY_API_KEY = "deepseek_api_key";

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

    public static void saveApiKey(Context context, String apiKey) {
        try {
            SharedPreferences prefs = getEncryptedPrefs(context);
            prefs.edit().putString(KEY_API_KEY, apiKey).apply();
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();  // 处理异常
        }
    }

    public static String getApiKey(Context context) {
        try {
            SharedPreferences prefs = getEncryptedPrefs(context);
            return prefs.getString(KEY_API_KEY, "");
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
            return "";
        }
    }
}
