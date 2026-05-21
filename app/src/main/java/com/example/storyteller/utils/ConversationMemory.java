package com.example.storyteller.utils;

import android.util.Log;
import com.example.storyteller.model.ChatMessage;
import java.util.ArrayList;
import java.util.List;

/**
 * 短期记忆管理器
 * 管理当前会话中的对话历史，用于提供上下文给AI
 */
public class ConversationMemory {
    
    private static final String TAG = "ConversationMemory";
    private static final int MAX_HISTORY = 8; // 保留最近8条消息
    
    private List<ChatMessage> recentMessages = new ArrayList<>();
    
    /**
     * 添加消息到短期记忆
     */
    public void addMessage(ChatMessage message) {
        if (message == null) return;
        
        recentMessages.add(message);
        
        // 如果超过最大数量，移除最早的消息
        if (recentMessages.size() > MAX_HISTORY) {
            recentMessages.remove(0);
        }
        
        Log.d(TAG, "Added message to memory. Total: " + recentMessages.size());
    }
    
    /**
     * 构建上下文摘要，用于发送给AI
     * @return 格式化的对话历史文本
     */
    public String buildContextSummary() {
        if (recentMessages.isEmpty()) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("\n【近期对话历史】\n");
        
        for (ChatMessage msg : recentMessages) {
            // 只包含用户消息和AI的最终回复
            if (msg.isFromUser()) {
                sb.append("用户: ").append(msg.getContent()).append("\n");
            } else {
                // AI回复，使用resultContent或content
                String aiResponse = !msg.getResultContent().isEmpty() 
                    ? msg.getResultContent() 
                    : msg.getContent();
                
                // 限制长度，避免上下文过长
                if (aiResponse.length() > 200) {
                    aiResponse = aiResponse.substring(0, 200) + "...";
                }
                sb.append("AI: ").append(aiResponse).append("\n");
            }
        }
        
        return sb.toString();
    }
    
    /**
     * 获取最近的用户消息
     * @param count 获取数量
     * @return 用户消息列表
     */
    public List<String> getRecentUserMessages(int count) {
        List<String> userMessages = new ArrayList<>();
        
        for (int i = recentMessages.size() - 1; i >= 0 && userMessages.size() < count; i--) {
            ChatMessage msg = recentMessages.get(i);
            if (msg.isFromUser()) {
                userMessages.add(0, msg.getContent()); // 保持顺序
            }
        }
        
        return userMessages;
    }
    
    /**
     * 清空短期记忆
     */
    public void clear() {
        recentMessages.clear();
        Log.d(TAG, "Conversation memory cleared");
    }
    
    /**
     * 获取记忆中的消息数量
     */
    public int size() {
        return recentMessages.size();
    }
    
    /**
     * 检查是否有对话历史
     */
    public boolean hasHistory() {
        return !recentMessages.isEmpty();
    }
}
