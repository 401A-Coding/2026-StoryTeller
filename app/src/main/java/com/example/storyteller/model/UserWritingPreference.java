package com.example.storyteller.model;

/**
 * 用户写作偏好数据类
 * 用于存储用户的写作风格偏好
 */
public class UserWritingPreference {
    
    // 写作风格选项
    public static final String STYLE_SIMPLE = "simple";        // 简洁直白
    public static final String STYLE_ELEGANT = "elegant";      // 华丽抒情
    public static final String STYLE_HUMOROUS = "humorous";    // 幽默风趣
    public static final String STYLE_SUSPENSE = "suspense";     // 悬疑紧张
    public static final String STYLE_CUSTOM = "custom";         // 自定义
    
    // 叙事视角选项
    public static final String NARRATIVE_FIRST = "first";                          // 第一人称
    public static final String NARRATIVE_THIRD_LIMITED = "third_limited";           // 第三人称限知
    public static final String NARRATIVE_THIRD_OMNISCIENT = "third_omniscient";     // 第三人称全知
    
    // 段落长度选项
    public static final String PARAGRAPH_SHORT = "short";    // 短段落（快节奏）
    public static final String PARAGRAPH_MEDIUM = "medium"; // 中等
    public static final String PARAGRAPH_LONG = "long";      // 长段落（沉浸感）
    
    // 来源类型
    public static final String SOURCE_MANUAL = "manual";        // 手动设置
    public static final String SOURCE_AI_EXTRACTED = "ai_extracted";  // AI提取
    
    private int id;
    private Integer storyId;  // NULL表示全局偏好
    
    // === 用户手动设置的偏好 ===
    private String writingStyle;
    private String customStyle;  // 自定义风格描述
    private String narrativePerspective;
    private String paragraphLength;
    private boolean avoidBloody;
    private boolean avoidViolence;
    private boolean avoidSensitive;
    private String specialRequirements;
    private String source;  // 手动设置的来源
    
    // === AI分析的偏好（独立存储） ===
    private String aiWritingStyle;
    private String aiNarrativePerspective;
    private String aiParagraphLength;
    private Boolean aiAvoidBloody;
    private Boolean aiAvoidViolence;
    private Boolean aiAvoidSensitive;
    private String aiSpecialRequirements;
    private String aiSource;  // AI分析的来源
    
    private long updatedAt;
    
    public UserWritingPreference() {
        this.source = SOURCE_MANUAL;
        this.updatedAt = System.currentTimeMillis();
    }
    
    // Getters and Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public Integer getStoryId() {
        return storyId;
    }
    
    public void setStoryId(Integer storyId) {
        this.storyId = storyId;
    }
    
    public String getWritingStyle() {
        return writingStyle;
    }
    
    public void setWritingStyle(String writingStyle) {
        this.writingStyle = writingStyle;
    }
    
    public String getCustomStyle() {
        return customStyle;
    }
    
    public void setCustomStyle(String customStyle) {
        this.customStyle = customStyle;
    }
    
    public String getNarrativePerspective() {
        return narrativePerspective;
    }
    
    public void setNarrativePerspective(String narrativePerspective) {
        this.narrativePerspective = narrativePerspective;
    }
    
    public String getParagraphLength() {
        return paragraphLength;
    }
    
    public void setParagraphLength(String paragraphLength) {
        this.paragraphLength = paragraphLength;
    }
    
    public boolean isAvoidBloody() {
        return avoidBloody;
    }
    
    public void setAvoidBloody(boolean avoidBloody) {
        this.avoidBloody = avoidBloody;
    }
    
    public boolean isAvoidViolence() {
        return avoidViolence;
    }
    
    public void setAvoidViolence(boolean avoidViolence) {
        this.avoidViolence = avoidViolence;
    }
    
    public boolean isAvoidSensitive() {
        return avoidSensitive;
    }
    
    public void setAvoidSensitive(boolean avoidSensitive) {
        this.avoidSensitive = avoidSensitive;
    }
    
    public String getSpecialRequirements() {
        return specialRequirements;
    }
    
    public void setSpecialRequirements(String specialRequirements) {
        this.specialRequirements = specialRequirements;
    }
    
    public String getSource() {
        return source;
    }
    
    public void setSource(String source) {
        this.source = source;
    }
    
    // === AI偏好字段的Getter和Setter ===
    public String getAiWritingStyle() {
        return aiWritingStyle;
    }
    
    public void setAiWritingStyle(String aiWritingStyle) {
        this.aiWritingStyle = aiWritingStyle;
    }
    
    public String getAiNarrativePerspective() {
        return aiNarrativePerspective;
    }
    
    public void setAiNarrativePerspective(String aiNarrativePerspective) {
        this.aiNarrativePerspective = aiNarrativePerspective;
    }
    
    public String getAiParagraphLength() {
        return aiParagraphLength;
    }
    
    public void setAiParagraphLength(String aiParagraphLength) {
        this.aiParagraphLength = aiParagraphLength;
    }
    
    public Boolean getAiAvoidBloody() {
        return aiAvoidBloody;
    }
    
    public void setAiAvoidBloody(Boolean aiAvoidBloody) {
        this.aiAvoidBloody = aiAvoidBloody;
    }
    
    public Boolean getAiAvoidViolence() {
        return aiAvoidViolence;
    }
    
    public void setAiAvoidViolence(Boolean aiAvoidViolence) {
        this.aiAvoidViolence = aiAvoidViolence;
    }
    
    public Boolean getAiAvoidSensitive() {
        return aiAvoidSensitive;
    }
    
    public void setAiAvoidSensitive(Boolean aiAvoidSensitive) {
        this.aiAvoidSensitive = aiAvoidSensitive;
    }
    
    public String getAiSpecialRequirements() {
        return aiSpecialRequirements;
    }
    
    public void setAiSpecialRequirements(String aiSpecialRequirements) {
        this.aiSpecialRequirements = aiSpecialRequirements;
    }
    
    public String getAiSource() {
        return aiSource;
    }
    
    public void setAiSource(String aiSource) {
        this.aiSource = aiSource;
    }
    
    public long getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    /**
     * 检查是否有任何偏好设置（用户手动设置）
     */
    public boolean hasAnyPreference() {
        return !isEmpty(writingStyle) ||
               !isEmpty(customStyle) ||
               !isEmpty(narrativePerspective) ||
               !isEmpty(paragraphLength) ||
               avoidBloody ||
               avoidViolence ||
               avoidSensitive ||
               !isEmpty(specialRequirements);
    }
    
    /**
     * 检查是否有AI分析的偏好
     */
    public boolean hasAiPreference() {
        return !isEmpty(aiWritingStyle) ||
               !isEmpty(aiNarrativePerspective) ||
               !isEmpty(aiParagraphLength) ||
               aiAvoidBloody != null ||
               aiAvoidViolence != null ||
               aiAvoidSensitive != null ||
               !isEmpty(aiSpecialRequirements);
    }
    
    /**
     * 获取最终生效的写作风格（用户设置优先，否则用AI分析的）
     */
    public String getEffectiveWritingStyle() {
        return !isEmpty(writingStyle) ? writingStyle : aiWritingStyle;
    }
    
    /**
     * 获取最终生效的叙事视角
     */
    public String getEffectiveNarrativePerspective() {
        return !isEmpty(narrativePerspective) ? narrativePerspective : aiNarrativePerspective;
    }
    
    /**
     * 获取最终生效的段落长度
     */
    public String getEffectiveParagraphLength() {
        return !isEmpty(paragraphLength) ? paragraphLength : aiParagraphLength;
    }
    
    /**
     * 获取最终生效的避免血腥设置
     */
    public boolean isEffectiveAvoidBloody() {
        if (avoidBloody) return true;
        return aiAvoidBloody != null && aiAvoidBloody;
    }
    
    /**
     * 获取最终生效的避免暴力设置
     */
    public boolean isEffectiveAvoidViolence() {
        if (avoidViolence) return true;
        return aiAvoidViolence != null && aiAvoidViolence;
    }
    
    /**
     * 获取最终生效的避免敏感设置
     */
    public boolean isEffectiveAvoidSensitive() {
        if (avoidSensitive) return true;
        return aiAvoidSensitive != null && aiAvoidSensitive;
    }
    
    /**
     * 获取最终生效的特殊要求
     */
    public String getEffectiveSpecialRequirements() {
        return !isEmpty(specialRequirements) ? specialRequirements : aiSpecialRequirements;
    }
    
    private boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
    
    /**
     * 获取写作风格的中文显示文本
     */
    public static String getStyleDisplayText(String style) {
        if (style == null) return "未设置";
        switch (style) {
            case STYLE_SIMPLE: return "简洁直白";
            case STYLE_ELEGANT: return "华丽抒情";
            case STYLE_HUMOROUS: return "幽默风趣";
            case STYLE_SUSPENSE: return "悬疑紧张";
            case STYLE_CUSTOM: return "自定义";
            default: return style;
        }
    }
    
    /**
     * 获取叙事视角的中文显示文本
     */
    public static String getNarrativeDisplayText(String narrative) {
        if (narrative == null) return "未设置";
        switch (narrative) {
            case NARRATIVE_FIRST: return "第一人称";
            case NARRATIVE_THIRD_LIMITED: return "第三人称限知";
            case NARRATIVE_THIRD_OMNISCIENT: return "第三人称全知";
            default: return narrative;
        }
    }
    
    /**
     * 获取段落长度的中文显示文本
     */
    public static String getParagraphLengthDisplayText(String length) {
        if (length == null) return "未设置";
        switch (length) {
            case PARAGRAPH_SHORT: return "短段落";
            case PARAGRAPH_MEDIUM: return "中等";
            case PARAGRAPH_LONG: return "长段落";
            default: return length;
        }
    }
    
    /**
     * 构建用于Prompt的偏好描述
     * 合并用户手动设置和AI分析的偏好
     */
    public String buildPreferenceDescription() {
        StringBuilder sb = new StringBuilder();
        
        sb.append("【用户写作偏好】\n");
        
        boolean hasUserPref = false;
        boolean hasAiPref = hasAiPreference();
        
        // 用户设置的部分
        if (!isEmpty(writingStyle)) {
            sb.append("- 写作风格: ").append(getStyleDisplayText(writingStyle));
            if (STYLE_CUSTOM.equals(writingStyle) && !isEmpty(customStyle)) {
                sb.append(" (").append(customStyle).append(")");
            }
            sb.append(" [用户设置]\n");
            hasUserPref = true;
        }
        if (!isEmpty(narrativePerspective)) {
            sb.append("- 叙事视角: ").append(getNarrativeDisplayText(narrativePerspective)).append(" [用户设置]\n");
            hasUserPref = true;
        }
        if (!isEmpty(paragraphLength)) {
            String desc = getParagraphLengthDisplayText(paragraphLength);
            if (PARAGRAPH_SHORT.equals(paragraphLength)) {
                desc += "（快节奏）";
            } else if (PARAGRAPH_LONG.equals(paragraphLength)) {
                desc += "（沉浸感）";
            }
            sb.append("- 段落长度: ").append(desc).append(" [用户设置]\n");
            hasUserPref = true;
        }
        
        boolean hasUserTaboo = avoidBloody || avoidViolence || avoidSensitive;
        if (hasUserTaboo) {
            StringBuilder tabooBuilder = new StringBuilder();
            if (avoidBloody) tabooBuilder.append("血腥");
            if (avoidViolence) {
                if (tabooBuilder.length() > 0) tabooBuilder.append("、");
                tabooBuilder.append("暴力");
            }
            if (avoidSensitive) {
                if (tabooBuilder.length() > 0) tabooBuilder.append("、");
                tabooBuilder.append("敏感话题");
            }
            sb.append("- 禁忌内容: 避免").append(tabooBuilder).append(" [用户设置]\n");
            hasUserPref = true;
        }
        if (!isEmpty(specialRequirements)) {
            sb.append("- 特殊要求: ").append(specialRequirements).append(" [用户设置]\n");
            hasUserPref = true;
        }
        
        // AI分析的部分（只显示用户未设置的）
        if (hasAiPref) {
            if (isEmpty(writingStyle) && !isEmpty(aiWritingStyle)) {
                sb.append("- 写作风格: ").append(getStyleDisplayText(aiWritingStyle)).append(" [AI分析]\n");
            }
            if (isEmpty(narrativePerspective) && !isEmpty(aiNarrativePerspective)) {
                sb.append("- 叙事视角: ").append(getNarrativeDisplayText(aiNarrativePerspective)).append(" [AI分析]\n");
            }
            if (isEmpty(paragraphLength) && !isEmpty(aiParagraphLength)) {
                sb.append("- 段落长度: ").append(getParagraphLengthDisplayText(aiParagraphLength)).append(" [AI分析]\n");
            }
            boolean hasAiTaboo = (aiAvoidBloody != null && aiAvoidBloody) ||
                                   (aiAvoidViolence != null && aiAvoidViolence) ||
                                   (aiAvoidSensitive != null && aiAvoidSensitive);
            if (hasAiTaboo && !hasUserTaboo) {
                StringBuilder tabooBuilder = new StringBuilder();
                if (aiAvoidBloody != null && aiAvoidBloody) tabooBuilder.append("血腥");
                if (aiAvoidViolence != null && aiAvoidViolence) {
                    if (tabooBuilder.length() > 0) tabooBuilder.append("、");
                    tabooBuilder.append("暴力");
                }
                if (aiAvoidSensitive != null && aiAvoidSensitive) {
                    if (tabooBuilder.length() > 0) tabooBuilder.append("、");
                    tabooBuilder.append("敏感话题");
                }
                sb.append("- 禁忌内容: 避免").append(tabooBuilder).append(" [AI分析]\n");
            }
            if (isEmpty(specialRequirements) && !isEmpty(aiSpecialRequirements)) {
                sb.append("- 特殊要求: ").append(aiSpecialRequirements).append(" [AI分析]\n");
            }
        }
        
        return sb.toString();
    }
}
