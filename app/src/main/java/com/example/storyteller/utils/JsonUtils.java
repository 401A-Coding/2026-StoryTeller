package com.example.storyteller.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.lang.reflect.Type;

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

    // JSON字符串转对象（支持泛型类型）
    public static <T> T fromJson(String json, Type typeOfT) {
        return gson.fromJson(json, typeOfT);
    }
}