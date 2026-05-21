package com.example.storyteller.model;

/**
 * AI记忆数据模型
 * 用于存储AI认为重要的上下文信息
 */
public class AiMemory {
    
    // 记忆类型常量
    public static final String TYPE_PLOT = "plot";         // 剧情类
    public static final String TYPE_PERSONALITY = "personality";  // 人设类
    public static final String TYPE_WORLD = "world";       // 世界观类
    public static final String TYPE_OTHER = "other";       // 其他类
    
    private long id;
    private Integer storyId;  // null表示全局记忆
    private String memoryType;
    private String title;
    private String content;
    private int importance;  // 1-5
    private long createdAt;
    private long updatedAt;
    
    public AiMemory() {
        this.importance = 3;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }
    
    // Getter和Setter
    public long getId() {
        return id;
    }
    
    public void setId(long id) {
        this.id = id;
    }
    
    public Integer getStoryId() {
        return storyId;
    }
    
    public void setStoryId(Integer storyId) {
        this.storyId = storyId;
    }
    
    public String getMemoryType() {
        return memoryType;
    }
    
    public void setMemoryType(String memoryType) {
        this.memoryType = memoryType;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public int getImportance() {
        return importance;
    }
    
    public void setImportance(int importance) {
        this.importance = Math.max(1, Math.min(5, importance));
    }
    
    public long getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
    
    public long getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    /**
     * 获取记忆类型的显示文本
     */
    public String getTypeDisplayText() {
        if (TYPE_PLOT.equals(memoryType)) {
            return "剧情类";
        } else if (TYPE_PERSONALITY.equals(memoryType)) {
            return "人设类";
        } else if (TYPE_WORLD.equals(memoryType)) {
            return "世界观类";
        } else {
            return "其他类";
        }
    }
    
    /**
     * 获取记忆类型的图标
     */
    public String getTypeIcon() {
        if (TYPE_PLOT.equals(memoryType)) {
            return "📋";
        } else if (TYPE_PERSONALITY.equals(memoryType)) {
            return "👤";
        } else if (TYPE_WORLD.equals(memoryType)) {
            return "🌍";
        } else {
            return "📝";
        }
    }
    
    /**
     * 构建用于Prompt的记忆描述
     */
    public String buildMemoryDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append("- [").append(getTypeDisplayText()).append("] ");
        sb.append(title);
        if (content != null && !content.isEmpty()) {
            sb.append(": ").append(content);
        }
        return sb.toString();
    }
    
    /**
     * 检查是否为全局记忆
     */
    public boolean isGlobal() {
        return storyId == null || storyId <= 0;
    }
}