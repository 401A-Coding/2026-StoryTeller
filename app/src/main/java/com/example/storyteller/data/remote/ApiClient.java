package com.example.storyteller.data.remote;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.MediaType;
import com.google.gson.Gson;
import java.io.IOException;
import java.util.List;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import android.content.Context;
import com.example.storyteller.utils.PromptManager;

public class ApiClient {
    // 单例模式
    private static ApiClient instance;
    private final OkHttpClient okHttpClient;
    private final Gson gson = new Gson();

    private ApiClient() {
        // 初始化OkHttpClient，设置超时时间
        okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)  // AI响应可能较慢，增加超时
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public static synchronized ApiClient getInstance() {
        if (instance == null) {
            instance = new ApiClient();
        }
        return instance;
    }

    // 获取OkHttpClient实例
    public OkHttpClient getOkHttpClient() {
        return okHttpClient;
    }

    // 通用请求模型
    public static class ChatRequest {
        public String model;
        public List<Message> messages;
        public int max_tokens = 1000;
        public double temperature = 0.7;
    }

    // DeepSeek请求模型（向后兼容）
    public static class DeepSeekRequest extends ChatRequest {
    }

    public static class RequestOptions {
        public Integer maxTokens;
        public Double temperature;

        public RequestOptions setMaxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        public RequestOptions setTemperature(double temperature) {
            this.temperature = temperature;
            return this;
        }
    }

    public static class Message {
        public String role;  // "user" 或 "system"
        public String content;

        public Message(String s, String s1) {
            this.role = s;
            this.content = s1;
        }
    }

    // 通用响应模型
    public static class ChatResponse {
        public List<Choice> choices;

        public static class Choice {
            public Message message;
        }
    }

    // DeepSeek响应模型（向后兼容）
    public static class DeepSeekResponse extends ChatResponse {
    }

    // 回调接口
    public interface Callback {
        void onSuccess(String story);
        void onFailure(Exception e);
    }

    /**
     * 生成故事的方法（使用默认模型）
     */
    public void generateStory(String prompt, Context context, Callback callback) {
        generateStory(prompt, ModelConfig.DEFAULT_MODEL, context, callback);
    }

    /**
     * 生成故事的方法（支持 modelId 选择模型）
     * @param prompt 提示词
     * @param modelId 内部模型ID（如 "deepseek-flash", "minimax-m2.7"）
     * @param context Android Context
     * @param callback 回调
     */
    public void generateStory(String prompt, String modelId, Context context, Callback callback) {
        generateStory(prompt, modelId, context, null, callback);
    }

    public void generateStory(String prompt, String modelId, Context context, RequestOptions options, Callback callback) {
        ModelConfig.ModelInfo modelInfo = ModelConfig.getModelInfo(modelId);
        if (modelInfo == null) {
            modelInfo = ModelConfig.getDefaultModelInfo();
        }
        
        String apiKey = ApiKeyManager.getApiKey(context, modelInfo.provider);
        if (apiKey.isEmpty()) {
            callback.onFailure(new Exception("API key not set for " + modelInfo.provider.getDisplayName()));
            return;
        }
        
        // 构建请求
        ChatRequest request = new ChatRequest();
        request.model = modelInfo.apiModelName;
        request.messages = List.of(new Message("user", prompt));
        if (options != null) {
            if (options.maxTokens != null && options.maxTokens > 0) {
                request.max_tokens = options.maxTokens;
            }
            if (options.temperature != null) {
                request.temperature = options.temperature;
            }
        }

        String json = gson.toJson(request);
        RequestBody body = RequestBody.create(json, MediaType.get("application/json; charset=utf-8"));

        Request req = new Request.Builder()
                .url(modelInfo.provider.getBaseUrl())
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        // 异步执行请求
        executeRequest(req, callback);
    }

    // ==================== 智能体功能 ====================

    /**
     * 智能体模式：分析用户意图并返回结构化命令（使用默认模型）
     */
    public void processAgentCommand(String userMessage, String currentStoryContext, 
                                     Context context, AgentCallback callback) {
        processAgentCommand(userMessage, currentStoryContext, ModelConfig.DEFAULT_MODEL, context, callback);
    }

    /**
     * 智能体模式：分析用户意图并返回结构化命令
     * @param userMessage 用户消息
     * @param currentStoryContext 当前小说上下文
     * @param modelId 内部模型ID
     * @param context Android Context
     * @param callback 回调
     */
    public void processAgentCommand(String userMessage, String currentStoryContext, String modelId,
                                     Context context, AgentCallback callback) {
        // 使用默认的编辑助手 System Prompt
        PromptManager promptManager = new PromptManager(context);
        String defaultSystemPrompt = promptManager.getAgentSystemPrompt("editor", null);
        if (defaultSystemPrompt == null || defaultSystemPrompt.isEmpty()) {
            callback.onFailure(new Exception("Failed to load agent system prompt"));
            return;
        }
        processAgentCommandWithSystemPrompt(userMessage, currentStoryContext, modelId, defaultSystemPrompt, context, callback);
    }
    
    /**
     * 智能体模式：使用自定义 System Prompt
     */
    public void processAgentCommandWithSystemPrompt(String userMessage, String currentStoryContext, 
                                                     String modelId, String systemPrompt,
                                                     Context context, AgentCallback callback) {
        ModelConfig.ModelInfo modelInfo = ModelConfig.getModelInfo(modelId);
        if (modelInfo == null) {
            modelInfo = ModelConfig.getDefaultModelInfo();
        }
        
        String apiKey = ApiKeyManager.getApiKey(context, modelInfo.provider);
        if (apiKey.isEmpty()) {
            callback.onFailure(new Exception("API key not set for " + modelInfo.provider.getDisplayName()));
            return;
        }

        // 构建请求
        ChatRequest request = new ChatRequest();
        request.model = modelInfo.apiModelName;
        request.messages = Arrays.asList(
                new Message("system", systemPrompt),
                new Message("user", "当前小说上下文：\n" + currentStoryContext + "\n\n用户消息：" + userMessage)
        );
        request.max_tokens = 2000;
        request.temperature = 0.3;

        String json = gson.toJson(request);
        RequestBody body = RequestBody.create(json, MediaType.get("application/json; charset=utf-8"));

        Request req = new Request.Builder()
                .url(modelInfo.provider.getBaseUrl())
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        // 异步执行
        executeAgentRequest(req, callback);
    }

    /**
     * 执行通用HTTP请求
     */
    private void executeRequest(Request req, Callback callback) {
        new Thread(() -> {
            try (Response response = okHttpClient.newCall(req).execute()) {
                if (response.isSuccessful()) {
                    String responseJson = response.body().string();
                    ChatResponse apiResponse = gson.fromJson(responseJson, ChatResponse.class);
                    if (apiResponse.choices != null && !apiResponse.choices.isEmpty()) {
                        String content = apiResponse.choices.get(0).message.content;
                        // 清理Markdown代码块标记
                        content = cleanMarkdownCodeBlock(content);
                        callback.onSuccess(content);
                    } else {
                        callback.onFailure(new Exception("No content generated"));
                    }
                } else {
                    callback.onFailure(new Exception("API Error: " + response.code() + " - " + response.message()));
                }
            } catch (IOException e) {
                callback.onFailure(e);
            }
        }).start();
    }

    /**
     * 执行智能体HTTP请求
     */
    private void executeAgentRequest(Request req, AgentCallback callback) {
        new Thread(() -> {
            try (Response response = okHttpClient.newCall(req).execute()) {
                if (response.isSuccessful()) {
                    String responseJson = response.body().string();
                    ChatResponse apiResponse = gson.fromJson(responseJson, ChatResponse.class);
                    if (apiResponse.choices != null && !apiResponse.choices.isEmpty()) {
                        String aiResponse = apiResponse.choices.get(0).message.content;
                        
                        // 清理Markdown代码块标记
                        aiResponse = cleanMarkdownCodeBlock(aiResponse);
                        
                        // 尝试解析为 JSON 命令
                        try {
                            AgentCommand command = gson.fromJson(aiResponse, AgentCommand.class);
                            callback.onCommandReady(command);
                        } catch (Exception e) {
                            // 如果解析失败，当作普通聊天处理
                            AgentCommand fallback = new AgentCommand();
                            fallback.action = "answer_question";
                            fallback.parameters = new java.util.HashMap<>();
                            fallback.parameters.put("response", aiResponse);
                            callback.onCommandReady(fallback);
                        }
                    } else {
                        callback.onFailure(new Exception("No response from AI"));
                    }
                } else {
                    callback.onFailure(new Exception("API Error: " + response.code()));
                }
            } catch (IOException e) {
                callback.onFailure(e);
            }
        }).start();
    }

    // 智能体命令模型
    public static class AgentCommand {
        public String action;
        public java.util.Map<String, Object> parameters;
        public String reasoning;
    }

    // 智能体回调接口
    public interface AgentCallback {
        void onCommandReady(AgentCommand command);
        void onFailure(Exception e);
    }
    
    /**
     * 清理AI返回内容中的Markdown代码块标记和思考过程标签
     */
    public String cleanMarkdownCodeBlock(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }
        
        String cleaned = content.trim();
        
        // 去除 <think> 标签及其内容
        int thinkStart = cleaned.indexOf("<think>");
        int thinkEnd = cleaned.indexOf("</think>");
        if (thinkStart != -1 && thinkEnd != -1 && thinkEnd > thinkStart) {
            cleaned = cleaned.substring(0, thinkStart) + cleaned.substring(thinkEnd + 8);
            cleaned = cleaned.trim();
        }
        
        // 去除开头的 ```json 或 ```
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        
        // 去除结尾的 ```
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        
        return cleaned.trim();
    }
}