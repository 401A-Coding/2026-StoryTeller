package com.example.storyteller.data.remote;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.MediaType;
import com.google.gson.Gson;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import android.content.Context;
import android.graphics.Bitmap;

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
    
    // ==================== 图像生成功能 ====================
    
    // 图像生成请求模型
    public static class ImageGenerationRequest {
        public String model = "image-01";
        public String prompt;
        public String aspect_ratio = "3:4";  // 书籍封面比例
        public String response_format = "url";
        public int n = 1;
        public Boolean prompt_optimizer = true;
        public Boolean aigc_watermark = false;
    }
    
    // 图像生成响应模型
    public static class ImageGenerationResponse {
        public ImageData data;
        public ImageMetadata metadata;
        public String id;
        public BaseResp base_resp;
        
        public static class ImageData {
            public List<String> image_urls;
            public List<String> image_base64;
        }
        
        public static class ImageMetadata {
            public int success_count;
            public int failed_count;
        }
        
        public static class BaseResp {
            public int status_code;
            public String status_msg;
        }
    }
    
    // 图像生成回调接口
    public interface CoverCallback {
        void onSuccess(List<String> imageUrls);
        void onFailure(Exception e);
    }
    
    /**
     * 生成AI封面
     * @param prompt 封面描述
     * @param n 生成数量
     * @param context Android Context
     * @param callback 回调
     */
    public void generateCover(String prompt, int n, Context context, CoverCallback callback) {
        String apiKey = ApiKeyManager.getApiKey(context, ModelConfig.Provider.MINIMAX);
        if (apiKey.isEmpty()) {
            callback.onFailure(new Exception("请先配置 MiniMax API Key"));
            return;
        }
        
        // 构建请求
        ImageGenerationRequest request = new ImageGenerationRequest();
        request.prompt = prompt;
        request.n = Math.min(Math.max(n, 1), 9);  // 限制在1-9之间
        request.aspect_ratio = "3:4";  // 书籍封面比例 3:4
        request.response_format = "url";
        request.prompt_optimizer = true;
        
        String json = gson.toJson(request);
        RequestBody body = RequestBody.create(json, MediaType.get("application/json; charset=utf-8"));
        
        Request req = new Request.Builder()
                .url("https://api.minimaxi.com/v1/image_generation")
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();
        
        // 异步执行请求
        new Thread(() -> {
            try (Response response = okHttpClient.newCall(req).execute()) {
                if (response.isSuccessful()) {
                    String responseJson = response.body().string();
                    ImageGenerationResponse apiResponse = gson.fromJson(responseJson, ImageGenerationResponse.class);
                    
                    if (apiResponse.base_resp != null && apiResponse.base_resp.status_code != 0) {
                        String errorMsg = getImageApiErrorMessage(apiResponse.base_resp.status_code);
                        callback.onFailure(new Exception(errorMsg));
                        return;
                    }
                    
                    if (apiResponse.data != null && apiResponse.data.image_urls != null) {
                        callback.onSuccess(apiResponse.data.image_urls);
                    } else {
                        callback.onFailure(new Exception("生成封面失败，请重试"));
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
     * 下载图片并转换为 Bitmap
     */
    public void downloadImageAsBitmap(String imageUrl, android.content.Context context, java.util.concurrent.ExecutorService executor, java.util.function.Consumer<Bitmap> onSuccess, java.util.function.Consumer<Exception> onFailure) {
        executor.execute(() -> {
            try {
                java.net.URL url = new URL(imageUrl);
                java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                connection.connect();
                
                android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(connection.getInputStream());
                connection.disconnect();
                
                if (bitmap != null) {
                    onSuccess.accept(bitmap);
                } else {
                    onFailure.accept(new Exception("图片下载失败"));
                }
            } catch (Exception e) {
                onFailure.accept(e);
            }
        });
    }
    
    /**
     * 获取图像API错误信息
     */
    private String getImageApiErrorMessage(int statusCode) {
        switch (statusCode) {
            case 1002: return "触发限流，请稍后再试";
            case 1004: return "账号鉴权失败，请检查 API Key 是否正确";
            case 1008: return "账号余额不足";
            case 1026: return "图片描述涉及敏感内容，请修改后重试";
            case 2013: return "传入参数异常";
            case 2049: return "无效的 API Key";
            default: return "生成失败，错误码: " + statusCode;
        }
    }
    
    /**
     * 构建封面生成提示词
     */
    public static String buildCoverPrompt(String title, String description, List<String> genres) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Create a book cover for a novel with the following details:\n\n");
        prompt.append("Title: ").append(title).append("\n");
        
        if (description != null && !description.isEmpty()) {
            prompt.append("Synopsis: ").append(description).append("\n");
        }
        
        if (genres != null && !genres.isEmpty()) {
            prompt.append("Genres: ").append(String.join(", ", genres)).append("\n");
        }
        
        prompt.append("\nRequirements:\n");
        prompt.append("- Aspect ratio 3:4 (book cover format)\n");
        prompt.append("- Style should match the story's genre and mood\n");
        prompt.append("- Include key elements or characters from the story\n");
        prompt.append("- Color scheme should be harmonious and visually striking\n");
        prompt.append("- Clear composition, suitable for book cover\n");
        prompt.append("- NO text or letters on the image - leave space for title overlay\n");
        prompt.append("- Cinematic, high quality, detailed\n");
        
        return prompt.toString();
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