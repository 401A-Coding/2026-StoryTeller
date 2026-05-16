package com.example.storyteller.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * 设定分类配置
 * 统一管理所有分类定义，避免重复
 */
public class SettingCategoryConfig {

    // === 5大顶层分类 ===
    public static final String[] MAIN_CATEGORIES = new String[] {
        "世界观", "角色", "剧情", "风格", "规则"
    };

    // === 子分类映射 ===
    private static final Map<String, String[]> SUB_CATEGORY_MAP = new HashMap<>();

    static {
        // 世界观
        SUB_CATEGORY_MAP.put("世界观", new String[] {
            "地理环境", "历史背景", "种族文化", "社会制度", "科技水平"
        });

        // 角色
        SUB_CATEGORY_MAP.put("角色", new String[] {
            "主要角色", "次要角色", "反派角色", "群体角色"
        });

        // 剧情
        SUB_CATEGORY_MAP.put("剧情", new String[] {
            "主线任务", "支线任务", "悬念伏笔", "关键事件"
        });

        // 风格
        SUB_CATEGORY_MAP.put("风格", new String[] {
            "叙事风格", "语言风格", "节奏控制", "情感基调"
        });

        // 规则
        SUB_CATEGORY_MAP.put("规则", new String[] {
            "魔法规则", "战斗系统", "经济体系", "时间规则"
        });
    }

    /**
     * 获取某主分类下的所有子分类
     * @param mainCategory 主分类名称
     * @return 子分类数组，如果不存在返回空数组
     */
    public static String[] getSubCategories(String mainCategory) {
        String[] subCategories = SUB_CATEGORY_MAP.get(mainCategory);
        return subCategories != null ? subCategories : new String[0];
    }

    /**
     * 检查子分类是否属于某个主分类
     * @param mainCategory 主分类
     * @param subCategory 子分类
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
     * @return 主分类数组
     */
    public static String[] getAllMainCategories() {
        return MAIN_CATEGORIES;
    }

    /**
     * 检查主分类是否有效
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
}
