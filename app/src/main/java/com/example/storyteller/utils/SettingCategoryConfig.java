package com.example.storyteller.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * 设定分类配置
 * 统一管理所有分类定义，避免重复
 */
public class SettingCategoryConfig {

    // === 6大顶层分类 ===
    public static final String[] MAIN_CATEGORIES = new String[]{
            "世界", "角色", "地点", "剧情", "规则体系", "创作控制"
    };
    
    // === 支持AI配图的主分类 ===
    private static final String[] AI_IMAGE_CATEGORIES = new String[]{
            "角色", "地点", "世界"
    };

    // === 子分类映射 ===
    private static final Map<String, String[]> SUB_CATEGORY_MAP = new HashMap<>();

    static {
        // 世界
        SUB_CATEGORY_MAP.put("世界", new String[]{
                "地理环境",
                "时代背景",
                "历史背景",
                "文明种族",
                "文化习俗",
                "社会制度",
                "政治势力",
                "科技发展",
                "物品资源"
        });

        // 角色
        SUB_CATEGORY_MAP.put("角色", new String[]{
                "主要角色",
                "次要角色",
                "反派角色",
                "组织阵营"
        });

        // 地点
        SUB_CATEGORY_MAP.put("地点", new String[]{
                "国家地区",
                "城市",
                "村庄",
                "自然景观",
                "关键场景",
                "建筑设施",
                "特殊空间"
        });

        // 剧情
        SUB_CATEGORY_MAP.put("剧情", new String[]{
                "主线剧情",
                "支线剧情",
                "关键事件",
                "悬念伏笔",
                "章节规划",
                "矛盾冲突",
                "时间线"
        });

        // 规则体系
        SUB_CATEGORY_MAP.put("规则体系", new String[]{
                "力量体系",
                "魔法或超能力",
                "战斗系统",
                "经济体系",
                "时间规则",
                "限制条件"
        });

        // 创作控制
        SUB_CATEGORY_MAP.put("创作控制", new String[]{
                "主题内核",
                "语言风格",
                "情感基调",
                "叙事视角",
                "节奏控制"
        });
    }

    /**
     * 获取某主分类下的所有子分类
     *
     * @param mainCategory 主分类名称
     * @return 子分类数组，如果不存在返回空数组
     */
    public static String[] getSubCategories(String mainCategory) {
        String[] subCategories = SUB_CATEGORY_MAP.get(mainCategory);
        return subCategories != null ? subCategories : new String[0];
    }

    /**
     * 检查子分类是否属于某个主分类
     *
     * @param mainCategory 主分类
     * @param subCategory  子分类
     * @return 是否有效
     */
    public static boolean isValidSubCategory(String mainCategory, String subCategory) {
        String[] subCategories = getSubCategories(mainCategory);
        for (String sub : subCategories) {
            if (sub.equals(subCategory)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取所有主分类
     *
     * @return 主分类数组
     */
    public static String[] getAllMainCategories() {
        return MAIN_CATEGORIES;
    }

    /**
     * 检查主分类是否有效
     *
     * @param mainCategory 主分类名称
     * @return 是否有效
     */
    public static boolean isValidMainCategory(String mainCategory) {
        for (String category : MAIN_CATEGORIES) {
            if (category.equals(mainCategory)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 检查主分类是否支持AI配图功能
     *
     * @param mainCategory 主分类名称
     * @return 是否支持
     */
    public static boolean supportsAiImageGeneration(String mainCategory) {
        for (String category : AI_IMAGE_CATEGORIES) {
            if (category.equals(mainCategory)) {
                return true;
            }
        }
        return false;
    }
}
