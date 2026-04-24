package com.example.storyteller.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class JsonUtils {
    private static final Gson gson = new GsonBuilder().create();

    // 对象转JSON字符串
    public static String toJson(Object obj) {
        return gson.toJson(obj);
    }

    // JSON字符串转对象
    public static <T> T fromJson(String json, Class<T> clazz) {
        return gson.fromJson(json, clazz);
    }
}