package com.example.storyteller.utils;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import java.util.HashMap;
import java.util.Locale;

/**
 * 文字转语音工具类
 * 使用 Android 原生 TextToSpeech（TTS）实现
 */
public class AudioUtils {
    private static final String TAG = "AudioUtils";
    private static AudioUtils instance;
    private TextToSpeech textToSpeech;
    private boolean isInitialized = false;
    private TtsCallback callback;

    public interface TtsCallback {
        void onInit(boolean success);
        void onSpeakStart();
        void onSpeakDone();
        void onError(String message);
    }

    private AudioUtils() {
    }

    public static synchronized AudioUtils getInstance(Context context) {
        if (instance == null) {
            instance = new AudioUtils(context.getApplicationContext());
        }
        return instance;
    }

    private AudioUtils(Context context) {
        textToSpeech = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = textToSpeech.setLanguage(Locale.CHINA);
                isInitialized = true;
                Log.d(TAG, "TTS 初始化成功，语言: " + (result == TextToSpeech.LANG_AVAILABLE ? "可用" : "不可用"));
                
                // 设置监听器
                textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId) {
                        Log.d(TAG, "朗读开始: " + utteranceId);
                        if (callback != null) callback.onSpeakStart();
                    }

                    @Override
                    public void onDone(String utteranceId) {
                        Log.d(TAG, "朗读完成: " + utteranceId);
                        if (callback != null) callback.onSpeakDone();
                    }

                    @Override
                    public void onError(String utteranceId) {
                        Log.e(TAG, "朗读错误: " + utteranceId);
                        if (callback != null) callback.onError("朗读出错");
                    }
                });
                
                if (callback != null) callback.onInit(true);
            } else {
                Log.e(TAG, "TTS 初始化失败，状态码: " + status);
                isInitialized = false;
                if (callback != null) callback.onInit(false);
            }
        });
    }

    /**
     * 设置回调监听
     */
    public void setCallback(TtsCallback callback) {
        this.callback = callback;
    }

    /**
     * 检查 TTS 是否可用
     */
    public boolean isAvailable() {
        return isInitialized;
    }

    /**
     * 朗读文本
     * @param text 要朗读的文本
     */
    public void speak(String text) {
        if (!isInitialized) {
            Log.e(TAG, "TTS 未初始化");
            if (callback != null) callback.onError("TTS 未初始化");
            return;
        }
        
        // 使用 QueueMode 添加到队列
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tts_" + System.currentTimeMillis());
    }

    /**
     * 停止朗读
     */
    public void stop() {
        if (textToSpeech != null) {
            textToSpeech.stop();
        }
    }

    /**
     * 释放资源
     */
    public void release() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
            textToSpeech = null;
            isInitialized = false;
        }
    }
}