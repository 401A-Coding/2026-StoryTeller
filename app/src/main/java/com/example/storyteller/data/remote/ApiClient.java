package com.example.storyteller.data.remote;

import okhttp3.OkHttpClient;
import java.util.concurrent.TimeUnit;

public class ApiClient {
    // 单例模式
    private static ApiClient instance;
    private final OkHttpClient okHttpClient;

    private ApiClient() {
        // 初始化OkHttpClient，设置超时时间
        okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public static synchronized ApiClient getInstance() {
        if (instance == null) {
            instance = new ApiClient();
        }
        return instance;
    }

    // 获取OkHttpClient实例（后续发送请求用）
    public OkHttpClient getOkHttpClient() {
        return okHttpClient;
    }

    // 后续在这里封装大模型API请求方法，示例：
    // public void generateStory(String prompt, Callback callback) {
    //     1. 构建请求体（JSON格式，包含prompt、故事类型、长度）
    //     2. 用okHttpClient发送POST请求
    //     3. 处理响应，解析返回的故事内容
    // }
}