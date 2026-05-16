package com.example.storyteller.model;

/**
 * 全局素材模型
 * 存储可复用的通用素材，可被多部小说导入
 */
public class GlobalMaterial {
    
    // === 基础字段 ===
    private int id;
    private String category;          // 顶层分类
    private String subCategory;       // 子分类
    private String title;             // 标题
    private String summary;           // 简要描述
    private String detail;            // 详细描述
    
    // === 结构化属性 ===
    private String attributes;        // 核心属性 JSON
    private String tags;              // 标签 JSON数组
    private String aliases;           // 别名 JSON数组
    private String specificAttributes; // 专属属性 JSON
    
    // === 来源信息 ===
    private String sourceType;        // manual/imported/ai_generated
    private String sourceUrl;         // 来源URL
    private double aiConfidence;      // AI置信度
    private String rawJson;           // 原始JSON
    
    // === 元数据 ===
    private long createTime;
    private long updateTime;
    private int usageCount;           // 被引用的次数（多少部小说导入了此素材）
    private boolean isPublic;         // 是否公开共享
    
    // === 构造方法 ===
    
    public GlobalMaterial() {
        this.createTime = System.currentTimeMillis();
        this.updateTime = this.createTime;
        this.aiConfidence = 0.5;
        this.usageCount = 0;
        this.isPublic = true;
    }
    
    public GlobalMaterial(String category, String subCategory, String title) {
        this();
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
    
    public int getUsageCount() {
        return usageCount;
    }
    
    public void setUsageCount(int usageCount) {
        this.usageCount = usageCount;
    }
    
    public void incrementUsageCount() {
        this.usageCount++;
    }
    
    public boolean isPublic() {
        return isPublic;
    }
    
    public void setPublic(boolean aPublic) {
        isPublic = aPublic;
    }
    
    @Override
    public String toString() {
        return "GlobalMaterial{" +
                "id=" + id +
                ", category='" + category + '\'' +
                ", subCategory='" + subCategory + '\'' +
                ", title='" + title + '\'' +
                ", usageCount=" + usageCount +
                '}';
    }
}
