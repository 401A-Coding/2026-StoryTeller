package com.example.storyteller.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import com.example.storyteller.data.local.db.DBHelper;
import com.example.storyteller.model.UserWritingPreference;

/**
 * 用户写作偏好管理器
 * 负责偏好数据的CRUD操作
 * 支持用户手动设置和AI分析两种来源的偏好
 */
public class PreferenceManager {
    
    private static final String TAG = "PreferenceManager";
    private static PreferenceManager instance;
    private DBHelper dbHelper;
    
    private PreferenceManager(Context context) {
        dbHelper = DBHelper.getInstance(context);
    }
    
    public static synchronized PreferenceManager getInstance(Context context) {
        if (instance == null) {
            instance = new PreferenceManager(context.getApplicationContext());
        }
        return instance;
    }
    
    /**
     * 获取全局偏好
     */
    public UserWritingPreference getGlobalPreference() {
        return getPreferenceByStoryId(null);
    }
    
    /**
     * 获取小说专属偏好
     */
    public UserWritingPreference getStoryPreference(int storyId) {
        return getPreferenceByStoryId(storyId);
    }
    
    /**
     * 获取偏好（storyId为null表示全局偏好）
     */
    private UserWritingPreference getPreferenceByStoryId(Integer storyId) {
        UserWritingPreference preference = new UserWritingPreference();
        preference.setStoryId(storyId);
        
        String selection = DBHelper.COL_PREF_STORY_ID + " " + (storyId == null ? "IS NULL" : "= ?");
        String[] selectionArgs = storyId == null ? null : new String[]{String.valueOf(storyId)};
        
        Log.d(TAG, "getPreferenceByStoryId - storyId: " + storyId + ", selection: " + selection);
        
        Cursor cursor = dbHelper.getReadableDatabase().query(
            DBHelper.TABLE_USER_PREFERENCES,
            null,
            selection,
            selectionArgs,
            null, null,
            DBHelper.COL_PREF_UPDATED_AT + " DESC",
            "1"
        );
        
        try {
            if (cursor != null && cursor.moveToFirst()) {
                preference.setId(cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COL_PREF_ID)));
                
                // 用户手动设置的偏好
                preference.setWritingStyle(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_PREF_WRITING_STYLE)));
                preference.setCustomStyle(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_PREF_CUSTOM_STYLE)));
                preference.setNarrativePerspective(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_PREF_NARRATIVE_PERSPECTIVE)));
                preference.setParagraphLength(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_PREF_PARAGRAPH_LENGTH)));
                preference.setAvoidBloody(cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COL_PREF_AVOID_BLOODY)) == 1);
                preference.setAvoidViolence(cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COL_PREF_AVOID_VIOLENCE)) == 1);
                preference.setAvoidSensitive(cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COL_PREF_AVOID_SENSITIVE)) == 1);
                preference.setSpecialRequirements(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_PREF_SPECIAL_REQUIREMENTS)));
                preference.setSource(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_PREF_SOURCE)));
                
                // AI分析的偏好
                // 检查列是否存在（可能是旧数据库）
                int aiWritingStyleCol = cursor.getColumnIndex(DBHelper.COL_PREF_AI_WRITING_STYLE);
                if (aiWritingStyleCol >= 0 && !cursor.isNull(aiWritingStyleCol)) {
                    preference.setAiWritingStyle(cursor.getString(aiWritingStyleCol));
                }
                int aiNarrativeCol = cursor.getColumnIndex(DBHelper.COL_PREF_AI_NARRATIVE_PERSPECTIVE);
                if (aiNarrativeCol >= 0 && !cursor.isNull(aiNarrativeCol)) {
                    preference.setAiNarrativePerspective(cursor.getString(aiNarrativeCol));
                }
                int aiParagraphCol = cursor.getColumnIndex(DBHelper.COL_PREF_AI_PARAGRAPH_LENGTH);
                if (aiParagraphCol >= 0 && !cursor.isNull(aiParagraphCol)) {
                    preference.setAiParagraphLength(cursor.getString(aiParagraphCol));
                }
                int aiBloodyCol = cursor.getColumnIndex(DBHelper.COL_PREF_AI_AVOID_BLOODY);
                if (aiBloodyCol >= 0 && !cursor.isNull(aiBloodyCol)) {
                    int bloody = cursor.getInt(aiBloodyCol);
                    preference.setAiAvoidBloody(bloody == 1 ? Boolean.TRUE : (bloody == 0 ? Boolean.FALSE : null));
                }
                int aiViolenceCol = cursor.getColumnIndex(DBHelper.COL_PREF_AI_AVOID_VIOLENCE);
                if (aiViolenceCol >= 0 && !cursor.isNull(aiViolenceCol)) {
                    int violence = cursor.getInt(aiViolenceCol);
                    preference.setAiAvoidViolence(violence == 1 ? Boolean.TRUE : (violence == 0 ? Boolean.FALSE : null));
                }
                int aiSensitiveCol = cursor.getColumnIndex(DBHelper.COL_PREF_AI_AVOID_SENSITIVE);
                if (aiSensitiveCol >= 0 && !cursor.isNull(aiSensitiveCol)) {
                    int sensitive = cursor.getInt(aiSensitiveCol);
                    preference.setAiAvoidSensitive(sensitive == 1 ? Boolean.TRUE : (sensitive == 0 ? Boolean.FALSE : null));
                }
                int aiSpecialCol = cursor.getColumnIndex(DBHelper.COL_PREF_AI_SPECIAL_REQUIREMENTS);
                if (aiSpecialCol >= 0 && !cursor.isNull(aiSpecialCol)) {
                    preference.setAiSpecialRequirements(cursor.getString(aiSpecialCol));
                }
                int aiSourceCol = cursor.getColumnIndex(DBHelper.COL_PREF_AI_SOURCE);
                if (aiSourceCol >= 0 && !cursor.isNull(aiSourceCol)) {
                    preference.setAiSource(cursor.getString(aiSourceCol));
                }
                
                preference.setUpdatedAt(cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.COL_PREF_UPDATED_AT)));
                
                Log.d(TAG, "  hasAiPreference check: " + preference.hasAiPreference());
            } else {
                Log.d(TAG, "  No record found in database");
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        
        return preference;
    }
    
    /**
     * 保存全局偏好（用户手动设置）
     */
    public boolean saveGlobalPreference(UserWritingPreference preference) {
        return savePreference(preference, null);
    }
    
    /**
     * 保存小说专属偏好（用户手动设置）
     */
    public boolean saveStoryPreference(UserWritingPreference preference, int storyId) {
        return savePreference(preference, storyId);
    }
    
    /**
     * 保存偏好（用户手动设置部分）
     * 注意：只更新用户设置的字段，保留AI分析的字段
     */
    private boolean savePreference(UserWritingPreference preference, Integer storyId) {
        try {
            preference.setStoryId(storyId);
            preference.setUpdatedAt(System.currentTimeMillis());
            
            // 先检查是否存在记录
            String checkSelection = DBHelper.COL_PREF_STORY_ID + " " + (storyId == null ? "IS NULL" : "= ?");
            String[] checkArgs = storyId == null ? null : new String[]{String.valueOf(storyId)};
            
            Cursor cursor = dbHelper.getReadableDatabase().query(
                DBHelper.TABLE_USER_PREFERENCES,
                new String[]{DBHelper.COL_PREF_ID},
                checkSelection,
                checkArgs,
                null, null,
                DBHelper.COL_PREF_UPDATED_AT + " DESC",
                "1"
            );
            
            boolean recordExists = cursor != null && cursor.moveToFirst();
            int existingId = recordExists ? cursor.getInt(0) : -1;
            if (cursor != null) cursor.close();
            
            Log.d(TAG, "savePreference - recordExists: " + recordExists + ", existingId: " + existingId);
            
            if (recordExists) {
                // 记录存在，使用UPDATE只更新用户设置的字段，保留AI字段
                ContentValues values = new ContentValues();
                values.put(DBHelper.COL_PREF_WRITING_STYLE, preference.getWritingStyle());
                values.put(DBHelper.COL_PREF_CUSTOM_STYLE, preference.getCustomStyle());
                values.put(DBHelper.COL_PREF_NARRATIVE_PERSPECTIVE, preference.getNarrativePerspective());
                values.put(DBHelper.COL_PREF_PARAGRAPH_LENGTH, preference.getParagraphLength());
                values.put(DBHelper.COL_PREF_AVOID_BLOODY, preference.isAvoidBloody() ? 1 : 0);
                values.put(DBHelper.COL_PREF_AVOID_VIOLENCE, preference.isAvoidViolence() ? 1 : 0);
                values.put(DBHelper.COL_PREF_AVOID_SENSITIVE, preference.isAvoidSensitive() ? 1 : 0);
                values.put(DBHelper.COL_PREF_SPECIAL_REQUIREMENTS, preference.getSpecialRequirements());
                values.put(DBHelper.COL_PREF_SOURCE, preference.getSource());
                values.put(DBHelper.COL_PREF_UPDATED_AT, preference.getUpdatedAt());
                
                dbHelper.getWritableDatabase().update(
                    DBHelper.TABLE_USER_PREFERENCES,
                    values,
                    DBHelper.COL_PREF_ID + " = ?",
                    new String[]{String.valueOf(existingId)}
                );
                
                Log.d(TAG, "Updated user preference fields, keeping AI fields intact");
            } else {
                // 记录不存在，创建新记录
                ContentValues values = new ContentValues();
                values.put(DBHelper.COL_PREF_WRITING_STYLE, preference.getWritingStyle());
                values.put(DBHelper.COL_PREF_CUSTOM_STYLE, preference.getCustomStyle());
                values.put(DBHelper.COL_PREF_NARRATIVE_PERSPECTIVE, preference.getNarrativePerspective());
                values.put(DBHelper.COL_PREF_PARAGRAPH_LENGTH, preference.getParagraphLength());
                values.put(DBHelper.COL_PREF_AVOID_BLOODY, preference.isAvoidBloody() ? 1 : 0);
                values.put(DBHelper.COL_PREF_AVOID_VIOLENCE, preference.isAvoidViolence() ? 1 : 0);
                values.put(DBHelper.COL_PREF_AVOID_SENSITIVE, preference.isAvoidSensitive() ? 1 : 0);
                values.put(DBHelper.COL_PREF_SPECIAL_REQUIREMENTS, preference.getSpecialRequirements());
                values.put(DBHelper.COL_PREF_SOURCE, preference.getSource());
                values.put(DBHelper.COL_PREF_UPDATED_AT, preference.getUpdatedAt());
                
                if (storyId == null) {
                    values.putNull(DBHelper.COL_PREF_STORY_ID);
                } else {
                    values.put(DBHelper.COL_PREF_STORY_ID, storyId);
                }
                
                long result = dbHelper.getWritableDatabase().insert(DBHelper.TABLE_USER_PREFERENCES, null, values);
                
                if (result != -1) {
                    Log.d(TAG, "Inserted new user preference record");
                } else {
                    Log.e(TAG, "Failed to insert user preference");
                    return false;
                }
            }
            
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error saving user preference", e);
            return false;
        }
    }
    
    /**
     * 保存AI分析偏好（支持全局或小说专属）
     * 只更新AI分析的字段，保留用户手动设置的字段
     */
    public boolean saveAiAnalyzedPreference(PreferenceExtractor.UserPreferences analyzed, Integer storyId) {
        try {
            // 获取现有记录
            UserWritingPreference existing = getPreferenceByStoryId(storyId);
            
            // 更新AI分析的字段
            if (analyzed.writing_style != null) {
                existing.setAiWritingStyle(analyzed.writing_style);
            }
            if (analyzed.narrative_perspective != null) {
                existing.setAiNarrativePerspective(analyzed.narrative_perspective);
            }
            if (analyzed.paragraph_length != null) {
                existing.setAiParagraphLength(analyzed.paragraph_length);
            }
            if (analyzed.avoid_bloody != null) {
                existing.setAiAvoidBloody(analyzed.avoid_bloody);
            }
            if (analyzed.avoid_violence != null) {
                existing.setAiAvoidViolence(analyzed.avoid_violence);
            }
            if (analyzed.avoid_sensitive != null) {
                existing.setAiAvoidSensitive(analyzed.avoid_sensitive);
            }
            if (analyzed.special_requirements != null) {
                existing.setAiSpecialRequirements(analyzed.special_requirements);
            }
            existing.setAiSource(UserWritingPreference.SOURCE_AI_EXTRACTED);
            existing.setUpdatedAt(System.currentTimeMillis());
            
            // 保存AI偏好字段
            return saveAiPreferenceOnly(existing, storyId);
        } catch (Exception e) {
            Log.e(TAG, "Error saving AI analyzed preference", e);
            return false;
        }
    }
    
    /**
     * 仅保存AI偏好字段（不覆盖用户设置的字段）
     */
    private boolean saveAiPreferenceOnly(UserWritingPreference preference, Integer storyId) {
        try {
            Log.d(TAG, "saveAiPreferenceOnly called - storyId: " + storyId);
            
            // 先检查是否存在记录
            String checkSelection = DBHelper.COL_PREF_STORY_ID + " " + (storyId == null ? "IS NULL" : "= ?");
            String[] checkArgs = storyId == null ? null : new String[]{String.valueOf(storyId)};
            
            Cursor cursor = dbHelper.getReadableDatabase().query(
                DBHelper.TABLE_USER_PREFERENCES,
                new String[]{DBHelper.COL_PREF_ID, DBHelper.COL_PREF_SOURCE},
                checkSelection,
                checkArgs,
                null, null,
                DBHelper.COL_PREF_UPDATED_AT + " DESC",
                "1"
            );
            
            boolean recordExists = cursor != null && cursor.moveToFirst();
            int existingId = recordExists ? cursor.getInt(0) : -1;
            String existingSource = recordExists ? cursor.getString(1) : null;
            if (cursor != null) cursor.close();
            
            Log.d(TAG, "Record exists: " + recordExists + ", existingId: " + existingId + ", existingSource: " + existingSource);
            
            ContentValues values = new ContentValues();
            values.put(DBHelper.COL_PREF_AI_WRITING_STYLE, preference.getAiWritingStyle());
            values.put(DBHelper.COL_PREF_AI_NARRATIVE_PERSPECTIVE, preference.getAiNarrativePerspective());
            values.put(DBHelper.COL_PREF_AI_PARAGRAPH_LENGTH, preference.getAiParagraphLength());
            values.put(DBHelper.COL_PREF_AI_AVOID_BLOODY, preference.getAiAvoidBloody() == null ? null : (preference.getAiAvoidBloody() ? 1 : 0));
            values.put(DBHelper.COL_PREF_AI_AVOID_VIOLENCE, preference.getAiAvoidViolence() == null ? null : (preference.getAiAvoidViolence() ? 1 : 0));
            values.put(DBHelper.COL_PREF_AI_AVOID_SENSITIVE, preference.getAiAvoidSensitive() == null ? null : (preference.getAiAvoidSensitive() ? 1 : 0));
            values.put(DBHelper.COL_PREF_AI_SPECIAL_REQUIREMENTS, preference.getAiSpecialRequirements());
            values.put(DBHelper.COL_PREF_AI_SOURCE, preference.getAiSource());
            values.put(DBHelper.COL_PREF_UPDATED_AT, preference.getUpdatedAt());
            
            if (recordExists) {
                // 使用UPDATE只更新AI分析的字段，保留用户设置的字段
                dbHelper.getWritableDatabase().update(
                    DBHelper.TABLE_USER_PREFERENCES,
                    values,
                    DBHelper.COL_PREF_ID + " = ?",
                    new String[]{String.valueOf(existingId)}
                );
                Log.d(TAG, "Updated AI preference fields via UPDATE");
            } else {
                // 创建新记录（仅包含AI分析偏好，用户偏好字段为空）
                if (storyId == null) {
                    values.putNull(DBHelper.COL_PREF_STORY_ID);
                } else {
                    values.put(DBHelper.COL_PREF_STORY_ID, storyId);
                }
                // 设置source为ai_extracted，表示这是AI分析的记录
                values.put(DBHelper.COL_PREF_SOURCE, UserWritingPreference.SOURCE_AI_EXTRACTED);
                long insertId = dbHelper.getWritableDatabase().insert(DBHelper.TABLE_USER_PREFERENCES, null, values);
                Log.d(TAG, "Inserted new record, insertId: " + insertId);
            }
            
            Log.d(TAG, "AI preference saved successfully for storyId: " + storyId);
            
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error saving AI preference only", e);
            return false;
        }
    }
    

    /**
     * 获取AI分析偏好（上次分析结果）
     * storyId为-1表示获取全局AI偏好，其他值获取小说专属AI偏好
     */
    public PreferenceExtractor.UserPreferences getAiAnalyzedPreference(int storyId) {
        if (storyId == -1) {
            // storyId为-1时，只从全局偏好获取
            UserWritingPreference globalPref = getGlobalPreference();
            if (globalPref.hasAiPreference()) {
                return convertToUserPreferences(globalPref);
            }
        } else if (storyId > 0) {
            // 有效的storyId，优先从小说专属获取
            UserWritingPreference storyPref = getStoryPreference(storyId);
            if (storyPref.hasAiPreference()) {
                return convertToUserPreferences(storyPref);
            }
            
            // 小说专属没有，再从全局获取
            UserWritingPreference globalPref = getGlobalPreference();
            if (globalPref.hasAiPreference()) {
                return convertToUserPreferences(globalPref);
            }
        }
        
        return null;
    }
    
    /**
     * 转换UserWritingPreference为PreferenceExtractor.UserPreferences
     */
    private PreferenceExtractor.UserPreferences convertToUserPreferences(UserWritingPreference pref) {
        PreferenceExtractor.UserPreferences analyzed = new PreferenceExtractor.UserPreferences();
        analyzed.writing_style = pref.getAiWritingStyle();
        analyzed.narrative_perspective = pref.getAiNarrativePerspective();
        analyzed.paragraph_length = pref.getAiParagraphLength();
        analyzed.avoid_bloody = pref.getAiAvoidBloody();
        analyzed.avoid_violence = pref.getAiAvoidViolence();
        analyzed.avoid_sensitive = pref.getAiAvoidSensitive();
        analyzed.special_requirements = pref.getAiSpecialRequirements();
        return analyzed;
    }
    
    /**
     * 删除全局偏好
     */
    public boolean deleteGlobalPreference() {
        return deletePreferenceByStoryId(null);
    }
    
    /**
     * 删除小说专属偏好
     */
    public boolean deleteStoryPreference(int storyId) {
        return deletePreferenceByStoryId(storyId);
    }
    
    /**
     * 删除偏好
     */
    private boolean deletePreferenceByStoryId(Integer storyId) {
        try {
            String selection = DBHelper.COL_PREF_STORY_ID + " " + (storyId == null ? "IS NULL" : "= ?");
            String[] selectionArgs = storyId == null ? null : new String[]{String.valueOf(storyId)};
            
            int rows = dbHelper.getWritableDatabase().delete(DBHelper.TABLE_USER_PREFERENCES, selection, selectionArgs);
            Log.d(TAG, "Deleted " + rows + " preference(s)");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error deleting preference", e);
            return false;
        }
    }
    
    /**
     * 获取合并后的偏好（用户设置优先，否则用AI分析的）
     */
    public UserWritingPreference getMergedPreference(int storyId) {
        UserWritingPreference storyPref = getStoryPreference(storyId);
        
        if (storyPref.hasAnyPreference()) {
            return storyPref;
        }
        
        // 如果没有小说专属偏好，返回全局偏好
        return getGlobalPreference();
    }
    
    /**
     * 复制全局偏好到新建的小说
     * 用于新建小说时初始化偏好设置
     * 
     * @param newStoryId 新小说的ID
     * @return 是否复制成功
     */
    public boolean copyGlobalPreferenceToStory(int newStoryId) {
        try {
            UserWritingPreference globalPref = getGlobalPreference();
            
            // 如果全局偏好没有任何设置，不需要复制
            if (!globalPref.hasAnyPreference()) {
                Log.d(TAG, "No global preference to copy");
                return true; // 不算失败，只是没有可复制的内容
            }
            
            // 创建新的偏好对象，只复制用户手动设置的字段
            UserWritingPreference newPref = new UserWritingPreference();
            newPref.setStoryId(newStoryId);
            
            // 复制用户设置的字段
            newPref.setWritingStyle(globalPref.getWritingStyle());
            newPref.setCustomStyle(globalPref.getCustomStyle());
            newPref.setNarrativePerspective(globalPref.getNarrativePerspective());
            newPref.setParagraphLength(globalPref.getParagraphLength());
            newPref.setAvoidBloody(globalPref.isAvoidBloody());
            newPref.setAvoidViolence(globalPref.isAvoidViolence());
            newPref.setAvoidSensitive(globalPref.isAvoidSensitive());
            newPref.setSpecialRequirements(globalPref.getSpecialRequirements());
            newPref.setSource(globalPref.getSource());
            
            // 不复制AI分析的字段，让新小说从零开始积累AI分析
            
            newPref.setUpdatedAt(System.currentTimeMillis());
            
            // 保存为新小说的专属偏好
            boolean success = savePreference(newPref, newStoryId);
            
            if (success) {
                Log.d(TAG, "Copied global preference to story " + newStoryId);
            }
            
            return success;
        } catch (Exception e) {
            Log.e(TAG, "Error copying global preference to story", e);
            return false;
        }
    }
}