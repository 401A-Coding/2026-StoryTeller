package com.example.storyteller.data.remote;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.MediaType;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import java.io.IOException;
import java.util.List;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

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

    // 生成故事的方法
    public void generateStory(String prompt, String apiKey, Callback callback) {
        // 构建请求体
        DeepSeekRequest request = new DeepSeekRequest();
        request.messages = List.of(new Message("user", prompt));  // 用户提示作为消息

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
}