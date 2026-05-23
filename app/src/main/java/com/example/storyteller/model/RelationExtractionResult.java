package com.example.storyteller.model;

import java.util.List;

/**
 * AI关系提取结果模型
 * 用于存储AI从文本中提取的待创建实体和潜在关系
 * 
 * 数据格式：
 * - 待定实体：只包含实体本身的字段，不存储关系
 * - 潜在关系：包含所有识别出的关系（统一在外部声明，方便AI去重）
 */
public class RelationExtractionResult {
    
    // === 待创建的实体列表 ===
    private List<PendingEntity> pendingEntities;
    
    // === 所有潜在关系列表（包括已有实体间的 + 与待创建实体相关的）===
    private List<PotentialRelation> potentialRelations;
    
    // === 构造方法 ===
    
    public RelationExtractionResult() {
    }
    
    public RelationExtractionResult(List<PendingEntity> pendingEntities,
                                    List<PotentialRelation> potentialRelations) {
        this.pendingEntities = pendingEntities;
        this.potentialRelations = potentialRelations;
    }
    
    // === Getter & Setter ===
    
    public List<PendingEntity> getPendingEntities() {
        return pendingEntities;
    }
    
    public void setPendingEntities(List<PendingEntity> pendingEntities) {
        this.pendingEntities = pendingEntities;
    }
    
    public List<PotentialRelation> getPotentialRelations() {
        return potentialRelations;
    }
    
    public void setPotentialRelations(List<PotentialRelation> potentialRelations) {
        this.potentialRelations = potentialRelations;
    }
    
    // === 内部类：待创建实体 ===
    
    /**
     * 待创建的实体
     */
    public static class PendingEntity {
        private String name;                    // 实体名称
        private String suggestedCategory;       // 建议的主分类
        private String suggestedSubcategory;    // 建议的子分类
        private String summary;                 // 简介
        private List<String> aliases;           // 别名列表
        private List<String> tags;              // 标签列表
        // 注意：实体本身不存储关系，所有关系统一在 potentialRelations 中声明
        
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
    }
    
    // === 内部类：潜在关系 ===
    
    /**
     * 潜在关系（所有识别出的关系，统一声明方便AI去重）
     */
    public static class PotentialRelation {
        private String sourceName;        // 源实体名称
        private String targetName;        // 目标实体名称
        private String relationshipType;   // 关系类型（自由文本）
        private boolean isDirected = true; // 是否为有向关系
        private String description;        // 关系描述（可选）
        
        public PotentialRelation() {
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
        
        public boolean isDirected() {
            return isDirected;
        }
        
        public void setDirected(boolean directed) {
            isDirected = directed;
        }
        
        public String getDescription() {
            return description;
        }
        
        public void setDescription(String description) {
            this.description = description;
        }
    }
}
