package com.example.storyteller.model;

/**
 * 小说设定模型
 * 支持5大分类：世界观、角色、剧情、风格、规则
 * 每个子分类有专属属性（JSON存储）
 */
public class StorySetting {
    
    // === 基础字段 ===
    private int id;
    private int storyId;              // 所属小说ID（0表示全局素材库）
    private String category;          // 顶层分类：世界观/角色/剧情/风格/规则
    private String subCategory;       // 子分类（如：地理环境、主要角色等）
    private String title;             // 标题/名称
    private String summary;           // 简要描述（200字内）
    private String detail;            // 详细描述
    
    // === 结构化属性（JSON存储）===
    private String attributes;        // 核心属性键值对 JSON（通用）
    private String tags;              // 标签列表 JSON数组
    private String aliases;           // 别名列表 JSON数组
    
    // === 专属属性（JSON存储，根据subCategory不同而不同）===
    private String specificAttributes; // JSON对象，存储该子分类的专属属性
    
    // === 来源追踪 ===
    private int sourceMaterialId;     // 源自哪个全局素材ID（0表示原创）
    private String sourceType;        // 来源类型：original/imported/modified/ai_generated
    private String sourceUrl;         // 来源URL（如果是导入的）
    private String sourceTitle;       // 来源标题
    private double aiConfidence;      // AI置信度（0-1）
    private String rawJson;           // 原始JSON（用于追溯）
    
    // === 同步相关（如果启用同步功能）===
    private long importTime;          // 导入时间
    private long lastSyncTime;        // 最后同步检查时间
    private boolean syncEnabled;      // 是否启用同步
    private boolean hasUpdates;       // 是否有可用更新
    
    // === 元数据 ===
    private long createTime;
    private long updateTime;
    private boolean isFavorite;       // 是否收藏
    private int usageCount;           // 使用次数（被AI引用的次数）
    
    // === 构造方法 ===
    
    public StorySetting() {
        this.createTime = System.currentTimeMillis();
        this.updateTime = this.createTime;
        this.aiConfidence = 0.5;
        this.isFavorite = false;
        this.usageCount = 0;
        this.sourceMaterialId = 0;
        this.importTime = 0;
        this.lastSyncTime = 0;
        this.syncEnabled = false;
        this.hasUpdates = false;
    }
    
    public StorySetting(int storyId, String category, String subCategory, String title) {
        this();
        this.storyId = storyId;
        this.category = category;
        this.subCategory = subCategory;
        this.title = title;
    }
    
    // === Getter & Setter ===
    
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public int getStoryId() {
        return storyId;
    }
    
    public void setStoryId(int storyId) {
        this.storyId = storyId;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public String getSubCategory() {
        return subCategory;
    }
    
    public void setSubCategory(String subCategory) {
        this.subCategory = subCategory;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getSummary() {
        return summary;
    }
    
    public void setSummary(String summary) {
        this.summary = summary;
    }
    
    public String getDetail() {
        return detail;
    }
    
    public void setDetail(String detail) {
        this.detail = detail;
    }
    
    public String getAttributes() {
        return attributes;
    }
    
    public void setAttributes(String attributes) {
        this.attributes = attributes;
    }
    
    public String getTags() {
        return tags;
    }
    
    public void setTags(String tags) {
        this.tags = tags;
    }
    
    public String getAliases() {
        return aliases;
    }
    
    public void setAliases(String aliases) {
        this.aliases = aliases;
    }
    
    public String getSpecificAttributes() {
        return specificAttributes;
    }
    
    public void setSpecificAttributes(String specificAttributes) {
        this.specificAttributes = specificAttributes;
    }
    
    public int getSourceMaterialId() {
        return sourceMaterialId;
    }
    
    public void setSourceMaterialId(int sourceMaterialId) {
        this.sourceMaterialId = sourceMaterialId;
    }
    
    public String getSourceType() {
        return sourceType;
    }
    
    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }
    
    public String getSourceUrl() {
        return sourceUrl;
    }
    
    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }
    
    public String getSourceTitle() {
        return sourceTitle;
    }
    
    public void setSourceTitle(String sourceTitle) {
        this.sourceTitle = sourceTitle;
    }
    
    public double getAiConfidence() {
        return aiConfidence;
    }
    
    public void setAiConfidence(double aiConfidence) {
        this.aiConfidence = aiConfidence;
    }
    
    public String getRawJson() {
        return rawJson;
    }
    
    public void setRawJson(String rawJson) {
        this.rawJson = rawJson;
    }
    
    public long getImportTime() {
        return importTime;
    }
    
    public void setImportTime(long importTime) {
        this.importTime = importTime;
    }
    
    public long getLastSyncTime() {
        return lastSyncTime;
    }
    
    public void setLastSyncTime(long lastSyncTime) {
        this.lastSyncTime = lastSyncTime;
    }
    
    public boolean isSyncEnabled() {
        return syncEnabled;
    }
    
    public void setSyncEnabled(boolean syncEnabled) {
        this.syncEnabled = syncEnabled;
    }
    
    public boolean isHasUpdates() {
        return hasUpdates;
    }
    
    public void setHasUpdates(boolean hasUpdates) {
        this.hasUpdates = hasUpdates;
    }
    
    public long getCreateTime() {
        return createTime;
    }
    
    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }
    
    public long getUpdateTime() {
        return updateTime;
    }
    
    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }
    
    public boolean isFavorite() {
        return isFavorite;
    }
    
    public void setFavorite(boolean favorite) {
        isFavorite = favorite;
    }
    
    public int getUsageCount() {
        return usageCount;
    }
    
    public void setUsageCount(int usageCount) {
        this.usageCount = usageCount;
    }
    
    /**
     * 增加使用次数
     */
    public void incrementUsageCount() {
        this.usageCount++;
    }
    
    @Override
    public String toString() {
        return "StorySetting{" +
                "id=" + id +
                ", storyId=" + storyId +
                ", category='" + category + '\'' +
                ", subCategory='" + subCategory + '\'' +
                ", title='" + title + '\'' +
                ", summary='" + summary + '\'' +
                ", createTime=" + createTime +
                '}';
    }
}
