package com.example.storyteller.utils;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import com.example.storyteller.model.Chapter;
import com.example.storyteller.model.Volume;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 朗读控制器 - 管理章节内容的分段朗读
 */
public class ReadingController {
    private static final String TAG = "ReadingController";
    private static final int SEGMENT_MAX_LENGTH = 100; // 每段最大字数

    private TextToSpeech textToSpeech;
    private boolean isInitialized = false;
    private boolean isPlaying = false;
    private boolean isPaused = false;

    private List<String> segments = new ArrayList<>();
    private int currentSegmentIndex = 0;

    private ReadingCallback callback;
    private String currentChapterTitle = "";

    public interface ReadingCallback {
        void onInit(boolean success);
        void onChapterChanged(String chapterTitle, int current, int total);
        void onProgress(int current, int total);
        void onPlayStateChanged(boolean isPlaying);
        void onComplete();
        void onError(String message);
    }

    public ReadingController(Context context) {
        textToSpeech = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = textToSpeech.setLanguage(Locale.CHINA);
                isInitialized = result != TextToSpeech.LANG_MISSING_DATA 
                             && result != TextToSpeech.LANG_NOT_SUPPORTED;
                Log.d(TAG, "TTS初始化" + (isInitialized ? "成功" : "失败"));
                
                textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId) {
                        isPlaying = true;
                        if (callback != null) callback.onPlayStateChanged(true);
                    }

                    @Override
                    public void onDone(String utteranceId) {
                        playNextSegment();
                    }

                    @Override
                    public void onError(String utteranceId) {
                        isPlaying = false;
                        if (callback != null) callback.onError("朗读出错");
                        if (callback != null) callback.onPlayStateChanged(false);
                    }
                });
                
                if (callback != null) callback.onInit(true);
            } else {
                isInitialized = false;
                if (callback != null) callback.onInit(false);
                Log.e(TAG, "TTS初始化失败");
            }
        });
    }

    public void setCallback(ReadingCallback callback) {
        this.callback = callback;
    }

    public boolean isInitialized() {
        return isInitialized;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public boolean isPaused() {
        return isPaused;
    }

    /**
     * 加载章节内容进行朗读准备
     */
    public void loadChapter(String chapterTitle, String content) {
        this.currentChapterTitle = chapterTitle;
        segments.clear();
        currentSegmentIndex = 0;

        if (content == null || content.isEmpty()) {
            return;
        }

        // 分段处理
        while (content.length() > SEGMENT_MAX_LENGTH) {
            int cutIndex = findCutPoint(content, SEGMENT_MAX_LENGTH);
            segments.add(content.substring(0, cutIndex));
            content = content.substring(cutIndex);
        }
        if (!content.isEmpty()) {
            segments.add(content);
        }

        if (callback != null && !segments.isEmpty()) {
            callback.onChapterChanged(chapterTitle, 1, segments.size());
            callback.onProgress(0, segments.size());
        }
    }

    /**
     * 找合适的断句点
     */
    private int findCutPoint(String text, int maxLength) {
        // 优先在句号、逗号、分号处断句
        String[] separators = {"。", "，", "；", "！", "？", "\n"};
        
        for (String sep : separators) {
            int index = text.lastIndexOf(sep, maxLength);
            if (index > maxLength / 2) {
                return index + sep.length();
            }
        }
        
        // 如果找不到合适的断句点，强行在maxLength处断开
        return maxLength;
    }

    /**
     * 开始朗读
     */
    public void start() {
        if (!isInitialized || segments.isEmpty()) {
            if (callback != null) callback.onError("TTS未就绪或内容为空");
            return;
        }
        
        isPlaying = true;
        playSegment(currentSegmentIndex);
    }

    /**
     * 播放指定段落
     */
    private void playSegment(int index) {
        if (index >= segments.size()) {
            isPlaying = false;
            if (callback != null) {
                callback.onPlayStateChanged(false);
                callback.onComplete();
            }
            return;
        }
        
        currentSegmentIndex = index;
        if (callback != null) {
            callback.onProgress(index + 1, segments.size());
        }
        
        String utteranceId = "segment_" + index;
        textToSpeech.speak(segments.get(index), TextToSpeech.QUEUE_FLUSH, null, utteranceId);
    }

    /**
     * 播放下一段
     */
    private void playNextSegment() {
        playSegment(currentSegmentIndex + 1);
    }

    /**
     * 暂停朗读
     */
    public void pause() {
        if (textToSpeech != null && (isPlaying || isPaused)) {
            textToSpeech.stop();
            isPlaying = false;
            isPaused = true;
            if (callback != null) callback.onPlayStateChanged(false);
        }
    }

    /**
     * 继续朗读（从暂停处继续）
     */
    public void resume() {
        if (!isInitialized || segments.isEmpty()) {
            if (callback != null) callback.onError("TTS未就绪或内容为空");
            return;
        }
        
        isPlaying = true;
        isPaused = false;
        playSegment(currentSegmentIndex);
    }

    /**
     * 停止并重置
     */
    public void stop() {
        if (textToSpeech != null) {
            textToSpeech.stop();
        }
        isPlaying = false;
        isPaused = false;
        // 不重置 currentSegmentIndex，保留进度
        
        if (callback != null) {
            callback.onPlayStateChanged(false);
            callback.onProgress(currentSegmentIndex + 1, segments.size());
        }
    }

    /**
     * 跳转到指定段落
     */
    public void seekTo(int segmentIndex) {
        if (segmentIndex >= 0 && segmentIndex < segments.size()) {
            boolean wasPlaying = isPlaying;
            textToSpeech.stop();
            currentSegmentIndex = segmentIndex;
            
            if (callback != null) {
                callback.onProgress(segmentIndex + 1, segments.size());
            }
            
            if (wasPlaying) {
                playSegment(segmentIndex);
            }
        }
    }

    /**
     * 获取当前进度（0.0 - 1.0）
     */
    public float getProgress() {
        if (segments.isEmpty()) return 0f;
        return (float) currentSegmentIndex / segments.size();
    }

    /**
     * 释放资源
     */
    public void release() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
            textToSpeech = null;
        }
        isInitialized = false;
        isPlaying = false;
        segments.clear();
    }
}