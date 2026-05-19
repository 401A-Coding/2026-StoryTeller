package com.example.storyteller.utils;

/**
 * AI 任务类型枚举
 * 定义所有需要调用 LLM 的专用任务
 */
public enum TaskType {
    
    // ==================== 素材相关 ====================
    /**
     * 从参考材料提取设定素材
     */
    EXTRACT_MATERIALS("extract_materials", "从参考材料提取设定素材"),
    
    // ==================== 角色相关 ====================
    /**
     * 批量提取小说中的角色
     */
    EXTRACT_CHARACTERS("extract_characters", "批量提取角色"),
    
    /**
     * 优化单个角色的画像
     */
    OPTIMIZE_CHARACTER("optimize_character", "优化单个角色画像"),
    
    // ==================== 剧情梳理相关 ====================
    /**
     * 单章剧情速记（极简版）
     */
    PLOT_QUICK_NOTE("plot_quick_note", "单章剧情速记"),
    
    /**
     * 单章剧情梳理（结构化版）
     */
    PLOT_CHAPTER_SUMMARY("plot_chapter_summary", "单章剧情梳理"),
    
    /**
     * 全书剧情汇总
     */
    PLOT_BOOK_SUMMARY("plot_book_summary", "全书剧情汇总"),
    
    /**
     * 批量章节分析
     */
    BATCH_CHAPTER_ANALYSIS("batch_chapter_analysis", "批量章节分析");
    
    private final String code;
    private final String description;
    
    TaskType(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
}
