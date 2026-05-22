package com.example.storyteller.model;

/**
 * 设定关系类型枚举
 * 用于定义两个设定之间的关联关系
 * 
 * 包含七大类：
 * - hierarchy: 层级关系（属于、包含等）
 * - association: 关联关系（位于、拥有、朋友等）
 * - family: 家人关系（父母子女、兄弟姐妹等）
 * - opposition: 对立关系（敌对、竞争等）
 * - causality: 因果关系（导致、先于等）
 * - derivation: 衍生关系（衍生自、相似等）
 * - other: 其他关系
 */
public enum RelationshipType {
    
    // ========== 层级关系（Hierarchy）==========
    BELONGS_TO("属于", "表示从属关系，如角色属于组织、地点位于世界", true, "hierarchy"),
    PART_OF("是...的一部分", "表示组成关系，如角色是队伍的成员", true, "hierarchy"),
    CONTAINS("包含", "表示包含关系，如世界观包含多个地区", true, "hierarchy"),
    HEAD_OF("领导", "表示领导关系，如A是B的首领", true, "hierarchy"),
    MEMBER_OF("成员", "表示成员关系，如A是B的成员", true, "hierarchy"),
    
    // ========== 关联关系（Association）==========
    LOCATED_IN("位于", "实体与位置的关系，如角色位于某城市", true, "association"),
    OWNS("拥有", "所有权关系，如角色拥有某件装备", true, "association"),
    USES("使用", "使用关系，如角色使用某种技能", true, "association"),
    ALLIES_WITH("盟友", "同盟关系，如角色A与角色B是盟友", false, "association"),
    FRIENDS_WITH("朋友", "朋友关系，如角色A与角色B是朋友", false, "association"),
    LOVES("爱慕", "情感关系，如角色A爱慕角色B", true, "association"),
    MARRIED_TO("结婚", "婚姻关系", false, "association"),
    TEACHES("教导", "师徒关系，如A教导B", true, "association"),
    LEARNS_FROM("学习", "学习关系，如A向B学习", true, "association"),
    WORK_WITH("共事", "同事关系，如A与B共事于某组织", false, "association"),
    
    // ========== 对立关系（Opposition）==========
    OPPOSES("对立", "敌对关系，如正派与反派", false, "opposition"),
    ENEMY_OF("敌对", "明确敌对关系，如A是B的敌人", true, "opposition"),
    RIVALRY("竞争", "竞争关系，如A与B存在竞争", false, "opposition"),
    KILLS("杀害", "杀害关系，如A杀死了B", true, "opposition"),
    
    // ========== 因果关系（Causality）==========
    CAUSES("导致", "因果关系，如事件A导致事件B", true, "causality"),
    PRECEDES("先于", "先后关系，如事件A发生在事件B之前", true, "causality"),
    ENABLES("使能", "使能关系，如技能使能某动作", true, "causality"),
    BLOCKS("阻止", "阻止关系，如障碍阻止前进", true, "causality"),
    TRIGGERS("触发", "触发关系，如事件A触发了事件B", true, "causality"),
    
    // ========== 家人关系（Family）==========
    FAMILY("家人", "家人关系，如A与B是一家人", false, "family"),
    
    // ========== 衍生关系（Derivation）==========
    DERIVED_FROM("衍生自", "从某设定衍生，如B角色衍生自A角色", true, "derivation"),
    SIMILAR_TO("相似于", "相似关系，如设定A与设定B相似", false, "derivation"),
    INSPIRES("启发", "灵感关系，如B的创作灵感来自A", true, "derivation"),
    PARENT_OF("父母", "亲子关系，如A是B的父母", true, "derivation"),
    CHILD_OF("子女", "子女关系，如A是B的子女", true, "derivation"),
    
    // ========== 其他关系 ==========
    CUSTOM("自定义", "用户自定义关系，需要额外描述", true, "other"),
    REFERENCES("引用", "引用关系，如设定A引用了设定B的内容", true, "other"),
    CONFLICTS_WITH("冲突", "冲突关系，如A的观点与B冲突", false, "other");
    
    // 显示名称
    private final String displayName;
    
    // 详细说明
    private final String description;
    
    // 方向性：true=有方向（A→B），false=无方向（A↔B）
    private final boolean isDirected;
    
    // 分类：用于UI分组显示
    private final String category;
    
    /**
     * 构造函数
     * 
     * @param displayName 显示名称
     * @param description 详细说明
     * @param isDirected 是否为有向关系
     * @param category 分类
     */
    RelationshipType(String displayName, String description, boolean isDirected, String category) {
        this.displayName = displayName;
        this.description = description;
        this.isDirected = isDirected;
        this.category = category;
    }
    
    /**
     * 获取显示名称
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * 获取详细说明
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * 判断是否为有向关系
     */
    public boolean isDirected() {
        return isDirected;
    }
    
    /**
     * 获取分类
     */
    public String getCategory() {
        return category;
    }
    
    /**
     * 根据分类获取对应的关系类型列表
     */
    public static RelationshipType[] getByCategory(String category) {
        java.util.List<RelationshipType> result = new java.util.ArrayList<>();
        for (RelationshipType type : values()) {
            if (type.category.equals(category)) {
                result.add(type);
            }
        }
        return result.toArray(new RelationshipType[0]);
    }
    
    /**
     * 获取所有分类
     */
    public static String[] getCategories() {
        return new String[]{"hierarchy", "association", "family", "opposition", "causality", "derivation", "other"};
    }
    
    /**
     * 获取分类的显示名称
     */
    public static String getCategoryDisplayName(String category) {
        switch (category) {
            case "hierarchy": return "层级关系";
            case "association": return "关联关系";
            case "family": return "家人关系";
            case "opposition": return "对立关系";
            case "causality": return "因果关系";
            case "derivation": return "衍生关系";
            case "other": return "其他关系";
            default: return category;
        }
    }
    
    /**
     * 根据名称查找枚举值
     */
    public static RelationshipType fromName(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        try {
            return valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
    
    /**
     * 获取所有枚举值的显示名称列表（用于Spinner等）
     */
    public static String[] getDisplayNames() {
        RelationshipType[] types = values();
        String[] names = new String[types.length];
        for (int i = 0; i < types.length; i++) {
            names[i] = types[i].displayName;
        }
        return names;
    }
    
    @Override
    public String toString() {
        return displayName;
    }
}