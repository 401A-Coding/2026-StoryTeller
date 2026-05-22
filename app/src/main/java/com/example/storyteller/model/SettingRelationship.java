package com.example.storyteller.model;

/**
 * 设定关系模型
 * 用于存储两个设定之间的关联关系
 * 支持有向关系和无向关系，可用于知识图谱构建
 */
public class SettingRelationship {
    
    // === 主键与归属 ===
    private int id;
    private int storyId;                      // 所属小说ID（用于快速查询单本小说的关系）
    
    // === 关系双方 ===
    private int sourceSettingId;             // 源设定ID（关系发起方）
    private int targetSettingId;             // 目标设定ID（关系接收方）
    
    // === 关系定义 ===
    private String relationshipType;         // 关系类型（RelationshipType枚举值，如"BELONGS_TO"）
    private String description;              // 关系描述（如"主角的武器"、"敌对关系"等）
    
    // === 来源与置信度 ===
    private String sourceType;               // 来源类型：manual/ai_inferred/user_confirmed
    private double confidence;               // 置信度(0-1)，用于AI推断的关系
    
    // === 元数据 ===
    private long createTime;
    private long updateTime;
    
    // === 临时字段（不存入数据库，用于UI展示） ===
    private String sourceSettingTitle;       // 源设定标题（JOIN查询填充）
    private String targetSettingTitle;       // 目标设定标题
    private String sourceSettingCategory;    // 源设定分类
    private String targetSettingCategory;    // 目标设定分类
    private String sourceSettingSubCategory; // 源设定子分类
    private String targetSettingSubCategory; // 目标设定子分类
    
    // === 来源类型常量 ===
    public static final String SOURCE_TYPE_MANUAL = "manual";           // 用户手动添加
    public static final String SOURCE_TYPE_AI_INFERRED = "ai_inferred"; // AI推断
    public static final String SOURCE_TYPE_USER_CONFIRMED = "user_confirmed"; // 用户确认的AI推断
    
    // === 构造方法 ===
    
    public SettingRelationship() {
        this.confidence = 0.8;
        this.sourceType = SOURCE_TYPE_MANUAL;
        this.createTime = System.currentTimeMillis();
        this.updateTime = this.createTime;
    }
    
    public SettingRelationship(int storyId, int sourceId, int targetId, String type) {
        this();
        this.storyId = storyId;
        this.sourceSettingId = sourceId;
        this.targetSettingId = targetId;
        this.relationshipType = type;
    }
    
    public SettingRelationship(int storyId, int sourceId, int targetId, RelationshipType type) {
        this(storyId, sourceId, targetId, type.name());
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
    
    public int getSourceSettingId() {
        return sourceSettingId;
    }
    
    public void setSourceSettingId(int sourceSettingId) {
        this.sourceSettingId = sourceSettingId;
    }
    
    public int getTargetSettingId() {
        return targetSettingId;
    }
    
    public void setTargetSettingId(int targetSettingId) {
        this.targetSettingId = targetSettingId;
    }
    
    public String getRelationshipType() {
        return relationshipType;
    }
    
    public void setRelationshipType(String relationshipType) {
        this.relationshipType = relationshipType;
    }
    
    public void setRelationshipType(RelationshipType type) {
        this.relationshipType = type.name();
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getSourceType() {
        return sourceType;
    }
    
    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }
    
    public double getConfidence() {
        return confidence;
    }
    
    public void setConfidence(double confidence) {
        this.confidence = confidence;
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
    
    public String getSourceSettingTitle() {
        return sourceSettingTitle;
    }
    
    public void setSourceSettingTitle(String sourceSettingTitle) {
        this.sourceSettingTitle = sourceSettingTitle;
    }
    
    public String getTargetSettingTitle() {
        return targetSettingTitle;
    }
    
    public void setTargetSettingTitle(String targetSettingTitle) {
        this.targetSettingTitle = targetSettingTitle;
    }
    
    public String getSourceSettingCategory() {
        return sourceSettingCategory;
    }
    
    public void setSourceSettingCategory(String sourceSettingCategory) {
        this.sourceSettingCategory = sourceSettingCategory;
    }
    
    public String getTargetSettingCategory() {
        return targetSettingCategory;
    }
    
    public void setTargetSettingCategory(String targetSettingCategory) {
        this.targetSettingCategory = targetSettingCategory;
    }
    
    public String getSourceSettingSubCategory() {
        return sourceSettingSubCategory;
    }
    
    public void setSourceSettingSubCategory(String sourceSettingSubCategory) {
        this.sourceSettingSubCategory = sourceSettingSubCategory;
    }
    
    public String getTargetSettingSubCategory() {
        return targetSettingSubCategory;
    }
    
    public void setTargetSettingSubCategory(String targetSettingSubCategory) {
        this.targetSettingSubCategory = targetSettingSubCategory;
    }
    
    // === 辅助方法 ===
    
    /**
     * 获取关系类型的显示名称
     */
    public String getTypeDisplayName() {
        if (relationshipType == null) {
            return "";
        }
        try {
            RelationshipType type = RelationshipType.valueOf(relationshipType);
            return type.getDisplayName();
        } catch (IllegalArgumentException e) {
            return relationshipType;
        }
    }
    
    /**
     * 获取关系类型的分类
     */
    public String getTypeCategory() {
        if (relationshipType == null) {
            return "";
        }
        try {
            RelationshipType type = RelationshipType.valueOf(relationshipType);
            return type.getCategory();
        } catch (IllegalArgumentException e) {
            return "";
        }
    }
    
    /**
     * 获取关系类型的分类显示名称
     */
    public String getTypeCategoryDisplayName() {
        return RelationshipType.getCategoryDisplayName(getTypeCategory());
    }
    
    /**
     * 判断是否为有向关系
     */
    public boolean isDirected() {
        if (relationshipType == null) {
            return true;
        }
        try {
            RelationshipType type = RelationshipType.valueOf(relationshipType);
            return type.isDirected();
        } catch (IllegalArgumentException e) {
            return true;
        }
    }
    
    /**
     * 判断是否为用户手动添加
     */
    public boolean isManual() {
        return SOURCE_TYPE_MANUAL.equals(sourceType);
    }
    
    /**
     * 判断是否为AI推断
     */
    public boolean isAiInferred() {
        return SOURCE_TYPE_AI_INFERRED.equals(sourceType) || SOURCE_TYPE_USER_CONFIRMED.equals(sourceType);
    }
    
    /**
     * 判断是否为用户确认的AI推断
     */
    public boolean isUserConfirmed() {
        return SOURCE_TYPE_USER_CONFIRMED.equals(sourceType);
    }
    
    /**
     * 获取源设定的显示信息（用于UI）
     */
    public String getSourceDisplayText() {
        if (sourceSettingTitle != null) {
            if (sourceSettingCategory != null && sourceSettingSubCategory != null) {
                return sourceSettingTitle + " (" + sourceSettingCategory + " · " + sourceSettingSubCategory + ")";
            }
            return sourceSettingTitle;
        }
        return String.valueOf(sourceSettingId);
    }
    
    /**
     * 获取目标设定的显示信息（用于UI）
     */
    public String getTargetDisplayText() {
        if (targetSettingTitle != null) {
            if (targetSettingCategory != null && targetSettingSubCategory != null) {
                return targetSettingTitle + " (" + targetSettingCategory + " · " + targetSettingSubCategory + ")";
            }
            return targetSettingTitle;
        }
        return String.valueOf(targetSettingId);
    }
    
    /**
     * 获取完整的关系描述文本（用于UI展示）
     * 格式：[源设定] -[关系]-> [目标设定]
     */
    public String getRelationshipText() {
        String source = getSourceDisplayText();
        String target = getTargetDisplayText();
        String typeDisplay = getTypeDisplayName();
        
        if (isDirected()) {
            return source + " → " + typeDisplay + " → " + target;
        } else {
            return source + " ↔ " + typeDisplay + " ↔ " + target;
        }
    }
    
    /**
     * 获取关系描述文本（简短版本，用于列表显示）
     */
    public String getBriefText() {
        String typeDisplay = getTypeDisplayName();
        if (description != null && !description.isEmpty()) {
            return typeDisplay + "（" + description + "）";
        }
        return typeDisplay;
    }
    
    @Override
    public String toString() {
        return "SettingRelationship{" +
                "id=" + id +
                ", storyId=" + storyId +
                ", sourceSettingId=" + sourceSettingId +
                ", targetSettingId=" + targetSettingId +
                ", relationshipType='" + relationshipType + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SettingRelationship that = (SettingRelationship) o;
        return sourceSettingId == that.sourceSettingId &&
                targetSettingId == that.targetSettingId &&
                relationshipType != null && relationshipType.equals(that.relationshipType);
    }
    
    @Override
    public int hashCode() {
        int result = sourceSettingId;
        result = 31 * result + targetSettingId;
        result = 31 * result + (relationshipType != null ? relationshipType.hashCode() : 0);
        return result;
    }
}