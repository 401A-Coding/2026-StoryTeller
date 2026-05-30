package com.example.storyteller.utils;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import com.example.storyteller.data.remote.ApiClient;
import com.example.storyteller.data.remote.ModelConfig;
import com.google.gson.Gson;
import java.util.Map;

/**
 * 用户偏好提取器
 * 从对话中自动提取用户的写作偏好
 */
public class PreferenceExtractor {
    
    private static final String TAG = "PreferenceExtractor";
    private final Context context;
    private final ApiClient apiClient;
    private final PromptManager promptManager;
    private final Gson gson = new Gson();
    
    /**
     * 获取第一个已启用的模型
     */
    private String getFirstEnabledModel() {
        java.util.List<ModelConfig.ModelInfo> allModels = ModelConfig.getAllModels();
        for (ModelConfig.ModelInfo model : allModels) {
            if (ModelConfig.isProviderEnabled(context, model.provider)) {
                return model.modelId;
            }
        }
        // 如果没有启用任何模型，使用默认模型（用户需去设置启用）
        return ModelConfig.DEFAULT_MODEL;
    }
    
    public PreferenceExtractor(Context context) {
        this.context = context.getApplicationContext();
        this.apiClient = ApiClient.getInstance();
        this.promptManager = new PromptManager(context);
    }
    
    /**
     * 从对话中提取用户偏好
     * @param conversationHistory 对话历史
     * @param callback 回调
     */
    public void extractPreferences(String conversationHistory, Callback callback) {
        extractPreferences(conversationHistory, null, callback);
    }
    
    /**
     * 从对话中提取用户偏好（带历史分析结果）
     * @param conversationHistory 对话历史
     * @param previousAnalyzed 上次分析结果（可为null）
     * @param callback 回调
     */
    public void extractPreferences(String conversationHistory, UserPreferences previousAnalyzed, Callback callback) {
        if (TextUtils.isEmpty(conversationHistory)) {
            Log.w(TAG, "Conversation history is empty");
            callback.onFailure(new Exception("对话历史为空"));
            return;
        }
        
        // 构建提取Prompt
        String prompt = buildExtractionPrompt(conversationHistory, previousAnalyzed);
        
        Log.d(TAG, "Starting preference extraction with prompt length: " + prompt.length());
        
        // 自动选择第一个已启用的模型
        String modelToUse = getFirstEnabledModel();
        
        // 调用AI提取
        apiClient.generateStory(prompt, modelToUse, context, new ApiClient.Callback() {
            @Override
            public void onSuccess(String responseText) {
                try {
                    Log.d(TAG, "API returned: " + responseText);
                    UserPreferences preferences = parsePreferences(responseText);
                    Log.d(TAG, "Parsed preferences - writing_style: " + preferences.writing_style + 
                          ", narrative_perspective: " + preferences.narrative_perspective);
                    // 即使没有检测到偏好，也触发回调以显示结果
                    callback.onPreferencesExtracted(preferences);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to parse preferences", e);
                    callback.onFailure(e);
                }
            }
            
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "API call failed", e);
                callback.onFailure(e);
            }
        });
    }
    
    public interface Callback {
        void onPreferencesExtracted(UserPreferences preferences);
        void onFailure(Exception e);
    }
    
    /**
     * 构建提取Prompt（使用PromptManager统一管理）
     * @param conversationHistory 对话历史
     * @param previousAnalyzed 上次分析结果（可为null）
     */
    private String buildExtractionPrompt(String conversationHistory, UserPreferences previousAnalyzed) {
        // 使用PromptManager加载模板
        Map<String, Object> variables = new java.util.HashMap<>();
        variables.put("conversation_history", conversationHistory);
        
        // 添加历史分析结果
        if (previousAnalyzed != null) {
            StringBuilder historyContext = new StringBuilder();
            if (previousAnalyzed.writing_style != null) {
                historyContext.append("写作风格：").append(previousAnalyzed.writing_style).append("\n");
            }
            if (previousAnalyzed.narrative_perspective != null) {
                historyContext.append("叙事视角：").append(previousAnalyzed.narrative_perspective).append("\n");
            }
            if (previousAnalyzed.paragraph_length != null) {
                historyContext.append("段落长度：").append(previousAnalyzed.paragraph_length).append("\n");
            }
            if (previousAnalyzed.avoid_bloody != null) {
                historyContext.append("避免血腥：").append(previousAnalyzed.avoid_bloody).append("\n");
            }
            if (previousAnalyzed.avoid_violence != null) {
                historyContext.append("避免暴力：").append(previousAnalyzed.avoid_violence).append("\n");
            }
            if (previousAnalyzed.avoid_sensitive != null) {
                historyContext.append("避免敏感：").append(previousAnalyzed.avoid_sensitive).append("\n");
            }
            if (previousAnalyzed.special_requirements != null) {
                historyContext.append("特殊要求：").append(previousAnalyzed.special_requirements).append("\n");
            }
            variables.put("previous_analysis", historyContext.toString());
        } else {
            variables.put("previous_analysis", "暂无");
        }
        
        String prompt = promptManager.getTaskPrompt("preference_extractor", variables);
        
        if (prompt == null || prompt.isEmpty()) {
            Log.e(TAG, "Failed to load preference extractor prompt, using fallback");
            // 备用版本
            return "# 任务：从对话中提取用户写作偏好\n\n" +
                   "分析以下对话历史，提取用户的写作偏好信息。\n\n" +
                   "## 对话历史\n" + conversationHistory + "\n\n" +
                   "## 提取规则\n" +
                   "只提取明确表达的偏好，不要猜测。\n\n" +
                   "## 输出格式\n" +
                   "返回JSON格式，如：{\"writing_style\": \"simple\"}\n\n" +
                   "如果没有检测到任何偏好，返回空对象：{}";
        }
        
        return prompt;
    }
    
    /**
     * 解析AI返回的偏好
     */
    private UserPreferences parsePreferences(String response) {
        try {
            // 清理JSON字符串
            String json = cleanJsonResponse(response);
            
            if (json == null || json.isEmpty()) {
                Log.w(TAG, "Response is not valid JSON, creating empty preferences");
                return new UserPreferences();
            }
            
            return gson.fromJson(json, UserPreferences.class);
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse JSON: " + response, e);
            return new UserPreferences();
        }
    }
    
    /**
     * 清理AI返回的JSON响应
     * 处理AI可能返回的非JSON内容
     */
    private String cleanJsonResponse(String response) {
        if (TextUtils.isEmpty(response)) {
            return null;
        }
        
        String trimmed = response.trim();
        
        // 检查是否已经是有效的JSON对象
        if (trimmed.startsWith("{")) {
            int end = trimmed.lastIndexOf("}");
            if (end > 0) {
                return trimmed.substring(0, end + 1);
            }
        }
        
        // 尝试从响应中提取JSON部分
        int jsonStart = trimmed.indexOf("{");
        if (jsonStart >= 0) {
            int jsonEnd = trimmed.lastIndexOf("}");
            if (jsonEnd > jsonStart) {
                String json = trimmed.substring(jsonStart, jsonEnd + 1);
                // 验证是否是有效的JSON
                if (json.contains("writing_style") || json.contains("narrative_perspective") || 
                    json.contains("paragraph_length") || json.contains("avoid_")) {
                    return json;
                }
            }
        }
        
        // 如果不是JSON格式，尝试解析为普通对象
        // 检查是否有类似JSON的内容
        if (trimmed.contains("writing_style") || trimmed.contains("narrative")) {
            // 尝试手动构建JSON
            return buildJsonFromText(trimmed);
        }
        
        Log.w(TAG, "No valid JSON found in response");
        return null;
    }
    
    /**
     * 从文本中提取偏好信息并构建JSON
     */
    private String buildJsonFromText(String text) {
        StringBuilder json = new StringBuilder("{");
        boolean needsComma = false;
        
        // 提取写作风格
        if (text.contains("写作风格") || text.contains("风格") || text.contains("style")) {
            String style = extractValue(text, "写作风格", "style", "风格");
            if (style != null) {
                if (needsComma) json.append(",");
                json.append("\"writing_style\":\"").append(style).append("\"");
                needsComma = true;
            }
        }
        
        // 提取叙事视角
        if (text.contains("叙事视角") || text.contains("视角") || text.contains("perspective")) {
            String perspective = extractValue(text, "叙事视角", "perspective", "视角");
            if (perspective != null) {
                if (needsComma) json.append(",");
                json.append("\"narrative_perspective\":\"").append(perspective).append("\"");
                needsComma = true;
            }
        }
        
        // 提取段落长度
        if (text.contains("段落长度") || text.contains("段落") || text.contains("paragraph")) {
            String length = extractValue(text, "段落长度", "paragraph", "段落");
            if (length != null) {
                if (needsComma) json.append(",");
                json.append("\"paragraph_length\":\"").append(length).append("\"");
                needsComma = true;
            }
        }
        
        json.append("}");
        
        // 如果没有提取到任何内容，返回空对象
        if (json.toString().equals("{}")) {
            return null;
        }
        
        return json.toString();
    }
    
    /**
     * 从文本中提取指定键的值
     */
    private String extractValue(String text, String... keywords) {
        for (String keyword : keywords) {
            int idx = text.indexOf(keyword);
            if (idx >= 0) {
                // 找到关键词后的内容
                String remainder = text.substring(idx + keyword.length());
                // 提取冒号或等号后的值
                int colonIdx = remainder.indexOf(":");
                int equalIdx = remainder.indexOf("=");
                int startIdx = -1;
                if (colonIdx >= 0 && (equalIdx < 0 || colonIdx < equalIdx)) {
                    startIdx = colonIdx;
                } else if (equalIdx >= 0) {
                    startIdx = equalIdx;
                }
                
                if (startIdx >= 0) {
                    String value = remainder.substring(startIdx + 1).trim();
                    // 清理引号和多余字符
                    value = value.replaceAll("^[\\\"']|[\\\"']$|", "").trim();
                    // 找到值结束位置（到换行或逗号）
                    int endIdx = value.length();
                    for (int i = 0; i < value.length(); i++) {
                        char c = value.charAt(i);
                        if (c == '\n' || c == ',' || c == '}' || c == '。' || c == '。') {
                            endIdx = i;
                            break;
                        }
                    }
                    value = value.substring(0, endIdx).trim();
                    if (!value.isEmpty()) {
                        return value;
                    }
                }
            }
        }
        return null;
    }
    
    /**
     * 用户偏好数据类
     */
    public static class UserPreferences {
        public String writing_style;
        public String narrative_perspective;
        public String paragraph_length;
        public Boolean avoid_bloody;
        public Boolean avoid_violence;
        public Boolean avoid_sensitive;
        public String special_requirements;
        
        /**
         * 检查是否有任何偏好
         */
        public boolean hasAnyPreference() {
            return !TextUtils.isEmpty(writing_style) ||
                   !TextUtils.isEmpty(narrative_perspective) ||
                   !TextUtils.isEmpty(paragraph_length) ||
                   (avoid_bloody != null && avoid_bloody) ||
                   (avoid_violence != null && avoid_violence) ||
                   (avoid_sensitive != null && avoid_sensitive) ||
                   !TextUtils.isEmpty(special_requirements);
        }
    }
}