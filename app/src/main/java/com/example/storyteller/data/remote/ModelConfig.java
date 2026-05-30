package com.example.storyteller.data.remote;

import android.content.Context;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 支持多模型提供商的配置管理
 */
public class ModelConfig {
    
    // 模型提供商枚举
    public enum Provider {
        DEEPSEEK("DeepSeek", "https://api.deepseek.com/v1/chat/completions"),
        MINIMAX("MiniMax", "https://api.minimaxi.com/v1/chat/completions");
        
        private final String displayName;
        private final String baseUrl;
        
        Provider(String displayName, String baseUrl) {
            this.displayName = displayName;
            this.baseUrl = baseUrl;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public String getBaseUrl() {
            return baseUrl;
        }
    }
    
    // 预设模型配置（modelId -> 模型配置）
    public static final Map<String, ModelInfo> PRESET_MODELS = new HashMap<>();
    
    static {
        // DeepSeek 模型
        PRESET_MODELS.put("flash", new ModelInfo(
            "flash", Provider.DEEPSEEK, "deepseek-v4-flash", "Flash", "DeepSeek-V4-Flash")
        );
        PRESET_MODELS.put("pro", new ModelInfo(
            "pro", Provider.DEEPSEEK, "deepseek-v4-pro", "Pro", "DeepSeek-V4-Pro")
        );
        
        // MiniMax 模型（仅保留 2.5 和 2.7）
        PRESET_MODELS.put("m2.5", new ModelInfo(
            "m2.5", Provider.MINIMAX, "MiniMax-M2.5", "M2.5", "MiniMax-M2.5")
        );
        PRESET_MODELS.put("m2.7", new ModelInfo(
            "m2.7", Provider.MINIMAX, "MiniMax-M2.7", "M2.7", "MiniMax-M2.7")
        );
    }
    
    // 默认模型
    public static final String DEFAULT_MODEL = "flash";
    
    /**
     * 模型信息
     */
    public static class ModelInfo {
        public final String modelId;           // 内部模型ID
        public final Provider provider;       // 提供商
        public final String apiModelName;     // API请求的模型名称
        public final String displayName;      // 简称显示名称
        public final String fullName;          // 全称
        
        public ModelInfo(String modelId, Provider provider, String apiModelName, String displayName, String fullName) {
            this.modelId = modelId;
            this.provider = provider;
            this.apiModelName = apiModelName;
            this.displayName = displayName;
            this.fullName = fullName;
        }
    }
    
    /**
     * 获取所有可用模型列表
     */
    public static java.util.List<ModelInfo> getAllModels() {
        return new java.util.ArrayList<>(PRESET_MODELS.values());
    }
    
    /**
     * 按提供商获取模型列表
     */
    public static java.util.List<ModelInfo> getModelsByProvider(Provider provider) {
        java.util.List<ModelInfo> result = new java.util.ArrayList<>();
        for (ModelInfo info : PRESET_MODELS.values()) {
            if (info.provider == provider) {
                result.add(info);
            }
        }
        return result;
    }
    
    /**
     * 根据 modelId 获取模型信息
     */
    public static ModelInfo getModelInfo(String modelId) {
        return PRESET_MODELS.get(modelId);
    }
    
    /**
     * 获取默认模型的模型信息
     */
    public static ModelInfo getDefaultModelInfo() {
        return PRESET_MODELS.get(DEFAULT_MODEL);
    }
    
    /**
     * 获取所有提供商
     */
    public static Provider[] getAllProviders() {
        return Provider.values();
    }
    
    // 启用状态持久化
    private static final String PREF_ENABLED_PROVIDERS = "enabled_providers";
    
    /**
     * 检查提供商是否启用
     */
    public static boolean isProviderEnabled(Context context, Provider provider) {
        android.content.SharedPreferences prefs = context.getSharedPreferences("model_prefs", android.content.Context.MODE_PRIVATE);
        return prefs.getBoolean("provider_enabled_" + provider.name(), false); // 默认禁用，需用户显式启用
    }
    
    /**
     * 设置提供商启用状态
     */
    public static void setProviderEnabled(Context context, Provider provider, boolean enabled) {
        android.content.SharedPreferences prefs = context.getSharedPreferences("model_prefs", android.content.Context.MODE_PRIVATE);
        prefs.edit().putBoolean("provider_enabled_" + provider.name(), enabled).apply();
    }
    
    /**
     * 获取启用的提供商列表
     */
    public static List<Provider> getEnabledProviders(Context context) {
        List<Provider> enabled = new ArrayList<>();
        for (Provider p : Provider.values()) {
            if (isProviderEnabled(context, p)) {
                enabled.add(p);
            }
        }
        return enabled;
    }
}