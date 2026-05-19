package com.example.storyteller.utils;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Prompt 管理器
 * 负责加载和管理 AI Prompt 模板
 * 支持从 res/raw/ 目录加载 Markdown 格式的 Prompt 文件
 */
public class PromptManager {
    
    private static final String TAG = "PromptManager";
    private final Context context;
    
    // 缓存已加载的 Prompt
    private final Map<String, String> promptCache = new HashMap<>();
    
    public PromptManager(Context context) {
        this.context = context.getApplicationContext();
    }
    
    /**
     * 获取智能体模式的 System Prompt
     * @param mode 智能体模式代码（如 "editor", "setting" 等）
     * @param defaultPrompt 默认 Prompt（如果资源文件不存在则使用）
     * @return System Prompt 字符串
     */
    public String getAgentSystemPrompt(String mode, String defaultPrompt) {
        String key = "agents/agent_" + mode + "_prompt";
        return loadPrompt(key, defaultPrompt);
    }
    
    /**
     * 获取专用任务的 Prompt
     * @param taskCode 任务代码（如 "extract_materials", "extract_characters" 等）
     * @param variables 模板变量
     * @return 渲染后的 Prompt 字符串
     */
    public String getTaskPrompt(String taskCode, Map<String, Object> variables) {
        String key = "tasks/task_" + taskCode;
        String template = loadPrompt(key, null);
        
        if (template == null) {
            Log.e(TAG, "Task prompt not found: " + key);
            return "";
        }
        
        // 替换模板变量
        return replaceVariables(template, variables);
    }
    
    /**
     * 加载 Prompt（带缓存）
     * @param key 资源键名（不含扩展名）
     * @param defaultValue 默认值
     * @return Prompt 字符串
     */
    private String loadPrompt(String key, String defaultValue) {
        // 检查缓存
        if (promptCache.containsKey(key)) {
            return promptCache.get(key);
        }
        
        // 从资源文件加载
        String prompt = loadFromResource(key);
        
        if (prompt != null) {
            promptCache.put(key, prompt);
            return prompt;
        }
        
        // 使用默认值
        if (defaultValue != null) {
            promptCache.put(key, defaultValue);
            return defaultValue;
        }
        
        Log.w(TAG, "Prompt not found and no default provided: " + key);
        return null;
    }
    
    /**
     * 从 res/raw/ 加载资源
     * @param resourceName 资源名称（路径格式，如 "agents/agent_editor_prompt"）
     * @return 资源内容，失败返回 null
     */
    private String loadFromResource(String resourceName) {
        try {
            // 将路径转换为资源名（替换 / 为 _）
            String resourceBaseName = resourceName.replace("/", "_");
            
            int resId = context.getResources().getIdentifier(
                resourceBaseName, "raw", context.getPackageName()
            );
            
            if (resId == 0) {
                Log.d(TAG, "Resource not found: " + resourceName);
                return null;
            }
            
            InputStream is = context.getResources().openRawResource(resId);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            
            reader.close();
            String content = sb.toString().trim();
            Log.d(TAG, "Successfully loaded prompt: " + resourceName + " (" + content.length() + " chars)");
            return content;
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to load prompt: " + resourceName, e);
            return null;
        }
    }
    
    /**
     * 替换模板变量
     * 支持 {{variable_name}} 语法
     * @param template 模板字符串
     * @param variables 变量映射
     * @return 替换后的字符串
     */
    private String replaceVariables(String template, Map<String, Object> variables) {
        if (variables == null || variables.isEmpty()) {
            return template;
        }
        
        String result = template;
        
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            String value = entry.getValue() != null ? entry.getValue().toString() : "";
            result = result.replace(placeholder, value);
        }
        
        // 处理条件块：删除未满足条件的块
        result = removeEmptyConditionalBlocks(result, variables);
        
        return result;
    }
    
    /**
     * 移除空的条件块
     * 支持 {{#if variable}}...{{/if}} 语法
     * @param text 文本
     * @param variables 变量映射
     * @return 处理后的文本
     */
    private String removeEmptyConditionalBlocks(String text, Map<String, Object> variables) {
        // 匹配 {{#if variable}}...{{/if}} 模式
        Pattern pattern = Pattern.compile("\\{\\{#if\\s+(\\w+)\\}\\}(.*?)\\{\\{/if\\}\\}", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(text);
        
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String varName = matcher.group(1);
            String blockContent = matcher.group(2);
            
            // 检查变量是否存在且非空
            Object value = variables.get(varName);
            boolean hasValue = value != null && !value.toString().trim().isEmpty();
            
            if (hasValue) {
                // 保留内容
                matcher.appendReplacement(sb, Matcher.quoteReplacement(blockContent));
            } else {
                // 删除整个块
                matcher.appendReplacement(sb, "");
            }
        }
        matcher.appendTail(sb);
        
        return sb.toString();
    }
    
    /**
     * 清除缓存（用于开发时热更新）
     */
    public void clearCache() {
        promptCache.clear();
        Log.d(TAG, "Prompt cache cleared");
    }
}
