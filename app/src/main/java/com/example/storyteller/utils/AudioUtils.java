package com.example.storyteller.utils;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import java.util.Locale;

public class AudioUtils {
    private static AudioUtils instance;
    private TextToSpeech textToSpeech;

    private AudioUtils(Context context) {
        // 初始化TTS（文本转语音），后续实现朗读功能
        textToSpeech = new TextToSpeech(context.getApplicationContext(), status -> {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(Locale.CHINA);
            }
        });
    }

    public static synchronized AudioUtils getInstance(Context context) {
        if (instance == null) {
            instance = new AudioUtils(context);
        }
        return instance;
    }

    // 后续实现朗读方法：public void speak(String text) { ... }
    // 后续实现语音输入相关工具方法
}