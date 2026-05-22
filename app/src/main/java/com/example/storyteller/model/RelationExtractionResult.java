package com.example.storyteller.model;

import java.util.List;

/**
 * AI关系提取结果模型
 * 用于存储AI从文本中提取的设定关系和待创建的实体
 */
public class RelationExtractionResult {
    
    // === 已确认的关系（两端实体都存在） ===
    private List<ConfirmedRelation> confirmedRelations;
    
    // === 待创建的实体及其关联关系 ===
    private List<PendingEntity> pendingEntities;
    
    // === 构造方法 ===
    
    public RelationExtractionResult() {
    }
    
    public RelationExtractionResult(List<ConfirmedRelation> confirmedRelations, 
                                   List<PendingEntity> pendingEntities) {
        this.confirmedRelations = confirmedRelations;
        this.pendingEntities = pendingEntities;
    }
    
    // === Getter & Setter ===
    
    public List<ConfirmedRelation> getConfirmedRelations() {
        return confirmedRelations;
    }
    
    public void setConfirmedRelations(List<ConfirmedRelation> confirmedRelations) {
        this.confirmedRelations = confirmedRelations;
    }
    
    public List<PendingEntity> getPendingEntities() {
        return pendingEntities;
    }
    
    public void setPendingEntities(List<PendingEntity> pendingEntities) {
        this.pendingEntities = pendingEntities;
    }
    
    // === 内部类：已确认的关系 ===
    
    /**
     * 已确认的关系（源和目标实体都已存在）
     */
    public static class ConfirmedRelation {
        private String sourceName;        // 源实体名称
        private String targetName;        // 目标实体名称
        private String relationshipType;  // 关系类型（枚举名）
        private double confidence;        // 置信度 0.0-1.0
        private String evidence;          // 文本证据
        private String description;       // 关系描述（可选）
        
        public ConfirmedRelation() {
        }
        
        public String getSourceName() {
            return sourceName;
        }
        
        public void setSourceName(String sourceName) {
            this.sourceName = sourceName;
        }
        
        public String getTargetName() {
            return targetName;
        }
        
        public void setTargetName(String targetName) {
            this.targetName = targetName;
        }
        
        public String getRelationshipType() {
            return relationshipType;
        }
        
        public void setRelationshipType(String relationshipType) {
            this.relationshipType = relationshipType;
        }
        
        public double getConfidence() {
            return confidence;
        }
        
        public void setConfidence(double confidence) {
            this.confidence = confidence;
        }
        
        public String getEvidence() {
            return evidence;
        }
        
        public void setEvidence(String evidence) {
            this.evidence = evidence;
        }
        
        public String getDescription() {
            return description;
        }
        
        public void setDescription(String description) {
            this.description = description;
        }
    }
    
    // === 内部类：待创建的实体 ===
    
    /**
     * 待创建的实体（在现有设定库中不存在）
     */
    public static class PendingEntity {
        private String name;                    // 实体名称
        private String suggestedCategory;       // 建议的主分类
        private String suggestedSubcategory;    // 建议的子分类
        private String summary;                 // 简介
        private List<String> aliases;           // 别名列表
        private List<String> tags;              // 标签列表
        private List<EntityRelation> relations; // 该实体涉及的关系
        
        public PendingEntity() {
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getSuggestedCategory() {
            return suggestedCategory;
        }
        
        public void setSuggestedCategory(String suggestedCategory) {
            this.suggestedCategory = suggestedCategory;
        }
        
        public String getSuggestedSubcategory() {
            return suggestedSubcategory;
        }
        
        public void setSuggestedSubcategory(String suggestedSubcategory) {
            this.suggestedSubcategory = suggestedSubcategory;
        }
        
        public String getSummary() {
            return summary;
        }
        
        public void setSummary(String summary) {
            this.summary = summary;
        }
        
        public List<String> getAliases() {
            return aliases;
        }
        
        public void setAliases(List<String> aliases) {
            this.aliases = aliases;
        }
        
        public List<String> getTags() {
            return tags;
        }
        
        public void setTags(List<String> tags) {
            this.tags = tags;
        }
        
        public List<EntityRelation> getRelations() {
            return relations;
        }
        
        public void setRelations(List<EntityRelation> relations) {
            this.relations = relations;
        }
    }
    
    // === 内部类：待创建实体的关系 ===
    
    /**
     * 待创建实体涉及的关系
     */
    public static class EntityRelation {
        private String targetName;         // 目标实体名称（可能是已有实体或其他待创建实体）
        private String relationshipType;   // 关系类型
        private double confidence;         // 置信度
        private String evidence;           // 文本证据
        private String description;        // 关系描述（可选）
        
        public EntityRelation() {
        }
        
        public String getTargetName() {
            return targetName;
        }
        
        public void setTargetName(String targetName) {
            this.targetName = targetName;
        }
        
        public String getRelationshipType() {
            return relationshipType;
        }
        
        public void setRelationshipType(String relationshipType) {
            this.relationshipType = relationshipType;
        }
        
        public double getConfidence() {
            return confidence;
        }
        
        public void setConfidence(double confidence) {
            this.confidence = confidence;
        }
        
        public String getEvidence() {
            return evidence;
        }
        
        public void setEvidence(String evidence) {
            this.evidence = evidence;
        }
        
        public String getDescription() {
            return description;
        }
        
        public void setDescription(String description) {
            this.description = description;
        }
    }
}
