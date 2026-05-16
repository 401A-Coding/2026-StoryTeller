package com.example.storyteller.data.remote;

/**
 * 番茄小说页面DOM选择器常量
 * 统一管理所有CSS选择器，便于维护和修改
 */
public class FanqieSelectors {
    
    // ==================== 基本信息 ====================
    
    /** 标题选择器: div.info-name h1 */
    public static final String TITLE = "div.info-name h1";
    
    /** 作者头像和名称容器: div.author-info */
    public static final String AUTHOR_CONTAINER = "div.author-info";
    
    /** 作者名称: span.author-name-text */
    public static final String AUTHOR_NAME = "span.author-name-text";
    
    /** 作者简介: div.author-desc */
    public static final String AUTHOR_DESC = "div.author-desc";
    
    // ==================== 标签和统计 ====================
    
    /** 标签容器: div.info-label */
    public static final String TAGS_CONTAINER = "div.info-label";
    
    /** 标签项: div.info-label span */
    public static final String TAG_ITEM = "div.info-label span";
    
    /** 字数容器: div.info-count-word */
    public static final String WORD_COUNT_CONTAINER = "div.info-count-word";
    
    /** 字数数值: div.info-count-word span.detail */
    public static final String WORD_COUNT_NUMBER = "div.info-count-word span.detail";
    
    /** 字数单位: div.info-count-word span.text */
    public static final String WORD_COUNT_UNIT = "div.info-count-word span.text";
    
    // ==================== 简介 ====================
    
    /** 简介容器: div.page-abstract-content */
    public static final String ABSTRACT_CONTAINER = "div.page-abstract-content";
    
    /** 简介段落: div.page-abstract-content p */
    public static final String ABSTRACT_PARAGRAPH = "div.page-abstract-content p";
    
    // ==================== 目录 ====================
    
    /** 目录容器: div.page-directory-content */
    public static final String DIRECTORY_CONTAINER = "div.page-directory-content";
    
    /** 卷信息项: div.volume (包括第一卷和其他卷) */
    public static final String VOLUME_ITEM = "div.volume";
    
    /** 目录章节项: div.page-directory-content a.chapter-item-title */
    public static final String CHAPTER_ITEM = "div.page-directory-content a.chapter-item-title";
    
    /** 最新章节链接: a.chapter-item-title (在 info-last 中) */
    public static final String LATEST_CHAPTER = "div.info-last a.chapter-item-title";
    
    /** 最新章节标题: span.info-last-title */
    public static final String LATEST_CHAPTER_TITLE = "span.info-last-title";
    
    /** 更新时间: span.info-last-time */
    public static final String UPDATE_TIME = "span.info-last-time";
    
    // ==================== 封面 ====================
    
    /** 封面图片: img.book-cover-img */
    public static final String COVER_IMAGE = "img.book-cover-img";
    
    // 私有构造函数，防止实例化
    private FanqieSelectors() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
