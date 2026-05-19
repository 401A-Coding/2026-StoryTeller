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
                .readTimeout(60, TimeUnit.SECONDS)  // AI响应可能较慢，增加超时
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

    // DeepSeek API请求模型
    public static class DeepSeekRequest {
        public String model = "deepseek-v4-flash";  // 根据文档调整模型名称
        public List<Message> messages;
        public int max_tokens = 1000;  // 控制故事长度
        public double temperature = 0.7;  // 创意度，0-1之间
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

    // DeepSeek API响应模型（简化版）
    public static class DeepSeekResponse {
        public List<Choice> choices;

        public static class Choice {
            public Message message;
        }
    }

    // 生成故事的方法（默认使用 flash 模型）
    public void generateStory(String prompt, Context context, Callback callback) {
        generateStory(prompt, "flash", context, callback);
    }

    /**
     * 生成故事的方法（支持模型选择）
     * @param prompt 提示词
     * @param model 模型类型："flash" 或 "pro"
     * @param context Android Context
     * @param callback 回调
     */
    public void generateStory(String prompt, String model, Context context, Callback callback) {
        generateStory(prompt, model, context, null, callback);
    }

    public void generateStory(String prompt, String model, Context context, RequestOptions options, Callback callback) {
        String apiKey = ApiKeyManager.getApiKey(context);
        if (apiKey.isEmpty()) {
            callback.onFailure(new Exception("API key not set"));
            return;
        }
        // 构建请求体
        DeepSeekRequest request = new DeepSeekRequest();
        // 根据选择的模型设置不同的模型名称
        if ("pro".equals(model)) {
            request.model = "deepseek-v4-pro";  // Pro 模型
        } else {
            request.model = "deepseek-v4-flash";  // Flash 模型（默认）
        }
        request.messages = List.of(new Message("user", prompt));  // 用户提示作为消息
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
                .url("https://api.deepseek.com/chat/completions")
                .addHeader("Authorization", "Bearer " + apiKey)  // 替换为你的API密钥
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        // 异步执行请求
        new Thread(() -> {
            try (Response response = okHttpClient.newCall(req).execute()) {
                if (response.isSuccessful()) {
                    String responseJson = response.body().string();
                    DeepSeekResponse apiResponse = gson.fromJson(responseJson, DeepSeekResponse.class);
                    if (apiResponse.choices != null && !apiResponse.choices.isEmpty()) {
                        String story = apiResponse.choices.get(0).message.content;
                        callback.onSuccess(story);
                    } else {
                        callback.onFailure(new Exception("No story generated"));
                    }
                } else {
                    callback.onFailure(new Exception("API Error: " + response.code() + " - " + response.message()));
                }
            } catch (IOException e) {
                callback.onFailure(e);
            }
        }).start();
    }

    // 回调接口
    public interface Callback {
        void onSuccess(String story);
        void onFailure(Exception e);
    }

    // ==================== 智能体功能 ====================

    /**
     * 智能体模式：分析用户意图并返回结构化命令（默认使用 flash 模型）
     * @param userMessage 用户消息
     * @param currentStoryContext 当前小说上下文（卷章结构、最近内容等）
     * @param context Android Context
     * @param callback 回调
     */
    public void processAgentCommand(String userMessage, String currentStoryContext, 
                                     Context context, AgentCallback callback) {
        processAgentCommand(userMessage, currentStoryContext, "flash", context, callback);
    }

    /**
     * 智能体模式：分析用户意图并返回结构化命令（支持模型选择）
     * @param userMessage 用户消息
     * @param currentStoryContext 当前小说上下文（卷章结构、最近内容等）
     * @param model 模型类型："flash" 或 "pro"
     * @param context Android Context
     * @param callback 回调
     */
    public void processAgentCommand(String userMessage, String currentStoryContext, String model,
                                     Context context, AgentCallback callback) {
        // 使用默认的编辑助手 System Prompt
        PromptManager promptManager = new PromptManager(context);
        String defaultSystemPrompt = promptManager.getAgentSystemPrompt("editor", null);
        if (defaultSystemPrompt == null || defaultSystemPrompt.isEmpty()) {
            callback.onFailure(new Exception("Failed to load agent system prompt"));
            return;
        }
        processAgentCommandWithSystemPrompt(userMessage, currentStoryContext, model, defaultSystemPrompt, context, callback);
    }
    
    /**
     * 智能体模式：使用自定义 System Prompt
     * @param userMessage 用户消息
     * @param currentStoryContext 当前小说上下文
     * @param model 模型类型
     * @param systemPrompt 自定义的系统提示词
     * @param context Android Context
     * @param callback 回调
     */
    public void processAgentCommandWithSystemPrompt(String userMessage, String currentStoryContext, 
                                                     String model, String systemPrompt,
                                                     Context context, AgentCallback callback) {
        String apiKey = ApiKeyManager.getApiKey(context);
        if (apiKey.isEmpty()) {
            callback.onFailure(new Exception("API key not set"));
            return;
        }

        // 构建请求
        DeepSeekRequest request = new DeepSeekRequest();
        // 根据选择的模型设置不同的模型名称
        if ("pro".equals(model)) {
            request.model = "deepseek-v4-pro";  // Pro 模型
        } else {
            request.model = "deepseek-v4-flash";  // Flash 模型（默认）
        }
        request.messages = Arrays.asList(
                new Message("system", systemPrompt),
                new Message("user", "当前小说上下文：\n" + currentStoryContext + "\n\n用户消息：" + userMessage)
        );
        request.max_tokens = 2000;  // 增加到 2000，避免 JSON 被截断
        request.temperature = 0.3;  // 降低温度，提高结构化输出的准确性

        String json = gson.toJson(request);
        RequestBody body = RequestBody.create(json, MediaType.get("application/json; charset=utf-8"));

        Request req = new Request.Builder()
                .url("https://api.deepseek.com/chat/completions")
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        // 异步执行
        new Thread(() -> {
            try (Response response = okHttpClient.newCall(req).execute()) {
                if (response.isSuccessful()) {
                    String responseJson = response.body().string();
                    DeepSeekResponse apiResponse = gson.fromJson(responseJson, DeepSeekResponse.class);
                    if (apiResponse.choices != null && !apiResponse.choices.isEmpty()) {
                        String aiResponse = apiResponse.choices.get(0).message.content;
                        
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
}
