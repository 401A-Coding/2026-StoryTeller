package com.example.storyteller.utils;

import android.text.TextUtils;
import com.example.storyteller.data.remote.MaterialCandidateExtractor;
import com.example.storyteller.model.StorySetting;
import com.google.gson.Gson;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 素材类型映射工具
 * 将旧版素材类型映射到新版分类体系
 */
public class MaterialTypeMapper {

    // AI提取类型到新版分类的映射
    private static final Map<String, String[]> TYPE_TO_CATEGORY_MAP = new HashMap<>();
    
    static {
        // worldview -> 世界观/地理环境
        TYPE_TO_CATEGORY_MAP.put(MaterialCandidateExtractor.TYPE_WORLDVIEW, 
                new String[]{"世界观", "地理环境"});
        // character -> 角色/主要角色
        TYPE_TO_CATEGORY_MAP.put(MaterialCandidateExtractor.TYPE_CHARACTER, 
                new String[]{"角色", "主要角色"});
        // plot -> 剧情/关键事件
        TYPE_TO_CATEGORY_MAP.put(MaterialCandidateExtractor.TYPE_PLOT, 
                new String[]{"剧情", "关键事件"});
        // style -> 风格/叙事风格
        TYPE_TO_CATEGORY_MAP.put(MaterialCandidateExtractor.TYPE_STYLE, 
                new String[]{"风格", "叙事风格"});
        // rule -> 规则/魔法规则
        TYPE_TO_CATEGORY_MAP.put(MaterialCandidateExtractor.TYPE_RULE, 
                new String[]{"规则", "魔法规则"});
    }

    /**
     * 将旧版素材类型映射到新版主分类
     * @param oldType 旧版类型：persona/plot/theme
     * @return 新版主分类
     */
    public static String mapToNewCategory(String oldType) {
        String[] mapping = TYPE_TO_CATEGORY_MAP.get(oldType);
        return mapping != null ? mapping[0] : "其他";
    }

    /**
     * 将旧版素材类型映射到新版子分类
     * @param oldType 旧版类型
     * @return 新版子分类
     */
    public static String mapToNewSubCategory(String oldType) {
        String[] mapping = TYPE_TO_CATEGORY_MAP.get(oldType);
        return mapping != null ? mapping[1] : "其他";
    }

    /**
     * 将旧版Material转换为新版StorySetting
     * 会解析content字段中的结构化信息，优先使用AI返回的subCategory
     */
    public static StorySetting convertToStorySetting(com.example.storyteller.model.Material oldMaterial) {
        StorySetting setting = new StorySetting();
        
        // 映射主分类
        String category = mapToNewCategory(oldMaterial.getSourceType());
        setting.setCategory(category);
        
        // 基础字段
        setting.setTitle(oldMaterial.getTitle());
        
        // 解析content中的结构化信息
        MaterialContentParser.ParsedContent parsed = MaterialContentParser.parse(oldMaterial.getContent());
        
        // 设置summary和detail
        setting.setSummary(parsed.getSummary());
        setting.setDetail(parsed.getDetail());
        
        // 设置子分类：优先使用AI返回的subCategory，否则使用默认映射
        String subCategory = parsed.getSubCategory();
        if (TextUtils.isEmpty(subCategory)) {
            subCategory = mapToNewSubCategory(oldMaterial.getSourceType());
        }
        // 验证子分类是否属于当前主分类
        if (!SettingCategoryConfig.isValidSubCategory(category, subCategory)) {
            subCategory = mapToNewSubCategory(oldMaterial.getSourceType());
        }
        setting.setSubCategory(subCategory);
        
        // 设置tags（JSON数组格式）
        if (!parsed.getTags().isEmpty()) {
            Gson gson = new Gson();
            setting.setTags(gson.toJson(parsed.getTags()));
        }
        
        // 来源信息
        setting.setSourceType(oldMaterial.getSourceType());
        setting.setSourceUrl(oldMaterial.getSourceUrl());
        setting.setSourceTitle(oldMaterial.getSourceTitle());
        setting.setAiConfidence(oldMaterial.getAiScore());
        setting.setRawJson(oldMaterial.getRawJson());
        
        // 时间戳
        setting.setCreateTime(oldMaterial.getCreateTime());
        setting.setUpdateTime(System.currentTimeMillis());
        
        return setting;
    }
}
