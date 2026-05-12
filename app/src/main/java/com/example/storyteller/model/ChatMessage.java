package com.example.storyteller.model;

import java.util.ArrayList;
import java.util.List;

public class ChatMessage {

    private final String content;
    private final boolean fromUser;
    private String displayContent; // 用于打字机效果的显示内容
    private boolean isTyping = false; // 是否正在打字
    
    // 执行步骤相关（用于 Agent 模式）
    private MessageType messageType = MessageType.NORMAL;
    private List<ExecutionStep> steps = new ArrayList<>();
    private long startTime = 0;
    private long endTime = 0;
    private String resultContent = ""; // 用于存储执行完成后的最终结果
    
    // 重试相关
    private boolean canRetry = false; // 是否可以重试
    private String originalUserMessage = ""; // 原始用户消息（用于重试）
    
    public enum MessageType {
        NORMAL,          // 普通消息
        PROCESSING,      // 处理中（可更新步骤）
        COMPLETED        // 已完成
    }
    
    public enum StepStatus {
        PENDING,     // 待执行
        RUNNING,     // 执行中
        COMPLETED,   // 已完成
        FAILED       // 失败
    }
    
    public static class ExecutionStep {
        public String icon;         // 图标 emoji
        public String title;        // 步骤标题
        public String detail;       // 详细信息
        public StepStatus status;   // 状态
        public long timestamp;      // 时间戳
        
        public ExecutionStep(String icon, String title, String detail, StepStatus status) {
            this.icon = icon;
            this.title = title;
            this.detail = detail;
            this.status = status;
            this.timestamp = System.currentTimeMillis();
        }
    }

    public ChatMessage(String content, boolean fromUser) {
        this.content = content;
        this.fromUser = fromUser;
        this.displayContent = ""; // 初始为空，逐步显示
    }
    
    // 构造函数重载：直接显示完整内容（不启用打字机效果）
    public ChatMessage(String content, boolean fromUser, boolean useTypewriter) {
        this.content = content;
        this.fromUser = fromUser;
        this.displayContent = useTypewriter ? "" : content;
        this.isTyping = useTypewriter;
    }
    
    // 构造函数：创建处理中的消息
    public ChatMessage(boolean fromUser, MessageType messageType) {
        this.content = "";
        this.fromUser = fromUser;
        this.displayContent = "";
        this.messageType = messageType;
        this.startTime = System.currentTimeMillis();
    }

    public String getContent() {
        return content;
    }
    
    public String getDisplayContent() {
        return displayContent;
    }
    
    public void setDisplayContent(String displayContent) {
        this.displayContent = displayContent;
    }
    
    public boolean isTyping() {
        return isTyping;
    }
    
    public void setTyping(boolean typing) {
        isTyping = typing;
    }

    public boolean isFromUser() {
        return fromUser;
    }
    
    // 执行步骤相关方法
    public MessageType getMessageType() {
        return messageType;
    }
    
    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
        if (messageType == MessageType.COMPLETED) {
            this.endTime = System.currentTimeMillis();
        }
    }
    
    public List<ExecutionStep> getSteps() {
        return steps;
    }
    
    public void addStep(ExecutionStep step) {
        this.steps.add(step);
    }
    
    public void updateStep(int index, StepStatus newStatus, String newDetail) {
        if (index >= 0 && index < steps.size()) {
            ExecutionStep step = steps.get(index);
            step.status = newStatus;
            if (newDetail != null) {
                step.detail = newDetail;
            }
        }
    }
    
    public void clearSteps() {
        this.steps.clear();
    }
    
    public long getStartTime() {
        return startTime;
    }
    
    public long getEndTime() {
        return endTime;
    }
    
    public long getDuration() {
        if (endTime > 0) {
            return endTime - startTime;
        }
        return System.currentTimeMillis() - startTime;
    }
    
    public String getResultContent() {
        return resultContent;
    }
    
    public void setResultContent(String resultContent) {
        this.resultContent = resultContent;
    }
    
    // 重试相关方法
    public boolean canRetry() {
        return canRetry;
    }
    
    public void setCanRetry(boolean canRetry) {
        this.canRetry = canRetry;
    }
    
    public String getOriginalUserMessage() {
        return originalUserMessage;
    }
    
    public void setOriginalUserMessage(String originalUserMessage) {
        this.originalUserMessage = originalUserMessage;
    }
    
    // 清除重试状态（用于重试后）
    public void clearRetryState() {
        this.canRetry = false;
        this.originalUserMessage = "";
    }
}
