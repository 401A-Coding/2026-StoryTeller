package com.example.storyteller.utils;

import android.text.TextUtils;
import android.util.Log;

import com.example.storyteller.model.StorySetting;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 设定专属属性解析器 v3.0
 * 基于SettingCategoryConfig.java的6大分类体系设计
 * 
 * 分类体系：
 * - 世界：地理环境、时代背景、历史背景、文明种族、文化习俗、社会制度、政治势力、科技发展、物品资源
 * - 角色：主要角色、次要角色、反派角色、组织阵营
 * - 地点：国家地区、城市、村庄、自然景观、关键场景、建筑设施、特殊空间
 * - 剧情：主线剧情、支线剧情、关键事件、悬念伏笔、章节规划、矛盾冲突、时间线
 * - 规则体系：力量体系、魔法或超能力、战斗系统、经济体系、时间规则、限制条件
 * - 创作控制：主题内核、语言风格、情感基调、叙事视角、节奏控制
 */
public class SpecificAttributesParser {

    private static final String TAG = "SpecificAttrsParser";
    
    // 子分类与中文名称的映射（用于显示）
    private static final Map<String, String> SUB_CATEGORY_DISPLAY_MAP = new HashMap<>();
    
    // 子分类对应的属性字段定义
    private static final Map<String, List<AttributeField>> SUB_CATEGORY_FIELDS_MAP = new HashMap<>();
    
    private final Gson gson = new Gson();
    
    static {
        initSubCategoryMappings();
    }
    
    private static void initSubCategoryMappings() {
        // ========== 一、世界 ==========
        initWorldFields();
        
        // ========== 二、角色 ==========
        initCharacterFields();
        
        // ========== 三、地点 ==========
        initLocationFields();
        
        // ========== 四、剧情 ==========
        initPlotFields();
        
        // ========== 五、规则体系 ==========
        initRuleSystemFields();
        
        // ========== 六、创作控制 ==========
        initCreativeControlFields();
    }
    
    // ========== 世界分类 ==========
    private static void initWorldFields() {
        // 世界 - 地理环境
        SUB_CATEGORY_DISPLAY_MAP.put("地理环境", "地理");
        List<AttributeField> geoFields = new ArrayList<>();
        geoFields.add(new AttributeField("area", "面积", FieldType.TEXT, null));
        geoFields.add(new AttributeField("terrain", "地形", FieldType.TAG_LIST, null));
        geoFields.add(new AttributeField("climate", "气候", FieldType.TEXT, null));
        geoFields.add(new AttributeField("resources", "资源", FieldType.TAG_LIST, null));
        geoFields.add(new AttributeField("dangerLevel", "危险等级", FieldType.SLIDER, null));
        geoFields.add(new AttributeField("coordinates", "坐标位置", FieldType.TEXT, null));
        geoFields.add(new AttributeField("features", "特色", FieldType.TAG_LIST, null));
        geoFields.add(new AttributeField("strategicValue", "战略价值", FieldType.TEXT_MULTI, null));
        SUB_CATEGORY_FIELDS_MAP.put("地理环境", geoFields);
        
        // 世界 - 时代背景
        SUB_CATEGORY_DISPLAY_MAP.put("时代背景", "时代");
        List<AttributeField> eraFields = new ArrayList<>();
        eraFields.add(new AttributeField("eraName", "时代名称", FieldType.TEXT, null));
        eraFields.add(new AttributeField("timePeriod", "时间段", FieldType.TEXT, null));
        eraFields.add(new AttributeField("techLevel", "科技水平", FieldType.SELECT, 
            new String[]{"原始", "古代", "中世纪", "近代", "现代", "未来", "修真"}));
        eraFields.add(new AttributeField("culturalLevel", "文化程度", FieldType.SELECT, 
            new String[]{"蒙昧", "启蒙", "发展", "繁荣", "衰退"}));
        eraFields.add(new AttributeField("mainConflicts", "主要冲突", FieldType.TAG_LIST, null));
        eraFields.add(new AttributeField("socialMood", "社会氛围", FieldType.TEXT, null));
        SUB_CATEGORY_FIELDS_MAP.put("时代背景", eraFields);
        
        // 世界 - 历史背景
        SUB_CATEGORY_DISPLAY_MAP.put("历史背景", "历史");
        List<AttributeField> historyFields = new ArrayList<>();
        historyFields.add(new AttributeField("majorEvents", "重大事件", FieldType.STRUCTURED_LIST, null, "年份,事件,重要性", "\n", ":"));
        historyFields.add(new AttributeField("keyFigures", "关键人物", FieldType.TAG_LIST, null));
        historyFields.add(new AttributeField("historicalImpact", "历史影响", FieldType.TEXT_MULTI, null));
        historyFields.add(new AttributeField("legacy", "遗留影响", FieldType.TEXT_MULTI, null));
        SUB_CATEGORY_FIELDS_MAP.put("历史背景", historyFields);
        
        // 世界 - 文明种族
        SUB_CATEGORY_DISPLAY_MAP.put("文明种族", "种族");
        List<AttributeField> raceFields = new ArrayList<>();
        raceFields.add(new AttributeField("raceName", "种族名称", FieldType.TEXT, null));
        raceFields.add(new AttributeField("origin", "起源", FieldType.TEXT, null));
        raceFields.add(new AttributeField("physicalTraits", "身体特征", FieldType.TAG_LIST, null));
        raceFields.add(new AttributeField("culturalTraits", "文化特征", FieldType.TAG_LIST, null));
        raceFields.add(new AttributeField("language", "语言特点", FieldType.TEXT, null));
        raceFields.add(new AttributeField("religion", "宗教信仰", FieldType.TEXT, null));
        raceFields.add(new AttributeField("population", "人口规模", FieldType.SELECT, 
            new String[]{"稀少", "少量", "中等", "众多", "海量"}));
        raceFields.add(new AttributeField("status", "现状", FieldType.SELECT, 
            new String[]{"兴旺", "平稳", "衰落", "濒危", "灭绝"}));
        SUB_CATEGORY_FIELDS_MAP.put("文明种族", raceFields);
        
        // 世界 - 文化习俗
        SUB_CATEGORY_DISPLAY_MAP.put("文化习俗", "文化");
        List<AttributeField> cultureFields = new ArrayList<>();
        cultureFields.add(new AttributeField("festivals", "节日", FieldType.TAG_LIST, null));
        cultureFields.add(new AttributeField("traditions", "传统习俗", FieldType.TAG_LIST, null));
        cultureFields.add(new AttributeField("taboos", "禁忌", FieldType.TAG_LIST, null));
        cultureFields.add(new AttributeField("culturalValues", "文化价值观", FieldType.TAG_LIST, null));
        cultureFields.add(new AttributeField("artForms", "艺术形式", FieldType.TAG_LIST, null));
        cultureFields.add(new AttributeField("dietaryHabits", "饮食习惯", FieldType.TEXT, null));
        SUB_CATEGORY_FIELDS_MAP.put("文化习俗", cultureFields);
        
        // 世界 - 社会制度
        SUB_CATEGORY_DISPLAY_MAP.put("社会制度", "社会");
        List<AttributeField> societyFields = new ArrayList<>();
        societyFields.add(new AttributeField("governmentType", "政体", FieldType.SELECT, 
            new String[]{"君主制", "贵族制", "共和制", "民主制", "神权制", "独裁制", "联邦制"}));
        societyFields.add(new AttributeField("socialHierarchy", "社会阶层", FieldType.TAG_LIST, null));
        societyFields.add(new AttributeField("laws", "重要法律", FieldType.TAG_LIST, null));
        societyFields.add(new AttributeField("justiceSystem", "司法制度", FieldType.TEXT_MULTI, null));
        societyFields.add(new AttributeField("classMobility", "阶级流动性", FieldType.SELECT, 
            new String[]{"高", "中", "低", "固化"}));
        societyFields.add(new AttributeField("corruption", "腐败程度", FieldType.SLIDER, null));
        societyFields.add(new AttributeField("stability", "稳定程度", FieldType.SLIDER, null));
        SUB_CATEGORY_FIELDS_MAP.put("社会制度", societyFields);
        
        // 世界 - 政治势力
        SUB_CATEGORY_DISPLAY_MAP.put("政治势力", "势力");
        List<AttributeField> factionFields = new ArrayList<>();
        factionFields.add(new AttributeField("factionName", "势力名称", FieldType.TEXT, null));
        factionFields.add(new AttributeField("factionType", "势力类型", FieldType.SELECT, 
            new String[]{"王国", "帝国", "宗门", "帮派", "家族", "商会", "教派", "联盟", "秘密组织"}));
        factionFields.add(new AttributeField("leader", "领导者", FieldType.TEXT, null));
        factionFields.add(new AttributeField("territory", "控制区域", FieldType.TEXT, null));
        factionFields.add(new AttributeField("militaryStrength", "军事力量", FieldType.SELECT, 
            new String[]{"薄弱", "一般", "强大", "顶尖"}));
        factionFields.add(new AttributeField("economicStrength", "经济实力", FieldType.SELECT, 
            new String[]{"贫困", "温饱", "富裕", "巨富"}));
        factionFields.add(new AttributeField("politicalIdeology", "政治理念", FieldType.TEXT, null));
        factionFields.add(new AttributeField("allies", "盟友", FieldType.TAG_LIST, null));
        factionFields.add(new AttributeField("enemies", "敌对势力", FieldType.TAG_LIST, null));
        factionFields.add(new AttributeField("influence", "影响力", FieldType.SLIDER, null));
        SUB_CATEGORY_FIELDS_MAP.put("政治势力", factionFields);
        
        // 世界 - 科技发展
        SUB_CATEGORY_DISPLAY_MAP.put("科技发展", "科技");
        List<AttributeField> techFields = new ArrayList<>();
        techFields.add(new AttributeField("techLevel", "科技等级", FieldType.TEXT, null));
        techFields.add(new AttributeField("mainTechnologies", "主要技术", FieldType.TAG_LIST, null));
        techFields.add(new AttributeField("forbiddenTechs", "禁忌技术", FieldType.TAG_LIST, null));
        techFields.add(new AttributeField("researchDirection", "研究方向", FieldType.TEXT_MULTI, null));
        techFields.add(new AttributeField("techFeatures", "科技特色", FieldType.TAG_LIST, null));
        techFields.add(new AttributeField("developmentStage", "发展阶段", FieldType.SELECT, 
            new String[]{"萌芽", "发展中", "成熟", "衰退"}));
        SUB_CATEGORY_FIELDS_MAP.put("科技发展", techFields);
        
        // 世界 - 物品资源
        SUB_CATEGORY_DISPLAY_MAP.put("物品资源", "物品");
        List<AttributeField> itemFields = new ArrayList<>();
        itemFields.add(new AttributeField("itemName", "物品名称", FieldType.TEXT, null));
        itemFields.add(new AttributeField("itemType", "物品类型", FieldType.SELECT, 
            new String[]{"材料", "装备", "丹药", "功法", "道具", "货币", "秘宝"}));
        itemFields.add(new AttributeField("rarity", "稀有度", FieldType.SELECT, 
            new String[]{"普通", "稀有", "珍稀", "传说", "神话"}));
        itemFields.add(new AttributeField("source", "来源", FieldType.TEXT_MULTI, null));
        itemFields.add(new AttributeField("usage", "用途", FieldType.TEXT_MULTI, null));
        itemFields.add(new AttributeField("value", "价值", FieldType.TEXT, null));
        itemFields.add(new AttributeField("sideEffects", "副作用", FieldType.TAG_LIST, null));
        itemFields.add(new AttributeField("relatedLocations", "关联地点", FieldType.TAG_LIST, null));
        SUB_CATEGORY_FIELDS_MAP.put("物品资源", itemFields);
    }
    
    // ========== 角色分类 ==========
    private static void initCharacterFields() {
        // 角色 - 主要角色
        SUB_CATEGORY_DISPLAY_MAP.put("主要角色", "角色");
        List<AttributeField> mainCharFields = new ArrayList<>();
        mainCharFields.add(new AttributeField("roleType", "角色类型", FieldType.SELECT, 
            new String[]{"protagonist:主角", "supporting:配角", "mentor:导师"}));
        mainCharFields.add(new AttributeField("age", "年龄", FieldType.NUMBER, null));
        mainCharFields.add(new AttributeField("gender", "性别", FieldType.SELECT, 
            new String[]{"male:男", "female:女", "other:其他"}));
        mainCharFields.add(new AttributeField("appearance", "外貌描述", FieldType.TEXT_MULTI, null));
        mainCharFields.add(new AttributeField("personalityTraits", "性格特征", FieldType.TAG_LIST, null));
        mainCharFields.add(new AttributeField("goals", "目标", FieldType.TAG_LIST, null));
        mainCharFields.add(new AttributeField("internalConflicts", "内在冲突", FieldType.TAG_LIST, null));
        mainCharFields.add(new AttributeField("secrets", "隐藏秘密", FieldType.TAG_LIST, null));
        mainCharFields.add(new AttributeField("weaknesses", "弱点", FieldType.TAG_LIST, null));
        mainCharFields.add(new AttributeField("strengths", "优势", FieldType.TAG_LIST, null));
        mainCharFields.add(new AttributeField("backgroundStory", "背景故事", FieldType.TEXT_MULTI, null));
        mainCharFields.add(new AttributeField("relationships", "关系网", FieldType.STRUCTURED_LIST, null, "角色名,关系,描述", "\n", ":"));
        mainCharFields.add(new AttributeField("characterArc", "成长弧光", FieldType.TEXT_MULTI, null));
        mainCharFields.add(new AttributeField("signatureItems", "标志性物品", FieldType.TAG_LIST, null));
        mainCharFields.add(new AttributeField("quotes", "经典台词", FieldType.TAG_LIST, null));
        mainCharFields.add(new AttributeField("currentStatus", "现状", FieldType.SELECT, 
            new String[]{"活跃", "退场", "死亡", "失踪"}));
        mainCharFields.add(new AttributeField("plotImportance", "剧情重要性", FieldType.SLIDER, null));
        SUB_CATEGORY_FIELDS_MAP.put("主要角色", mainCharFields);
        
        // 角色 - 次要角色
        SUB_CATEGORY_DISPLAY_MAP.put("次要角色", "角色");
        List<AttributeField> subCharFields = new ArrayList<>();
        subCharFields.add(new AttributeField("roleType", "角色类型", FieldType.SELECT, 
            new String[]{"supporting:配角", "mentor:导师", "sidekick:跟班"}));
        subCharFields.add(new AttributeField("age", "年龄", FieldType.NUMBER, null));
        subCharFields.add(new AttributeField("gender", "性别", FieldType.SELECT, 
            new String[]{"male:男", "female:女", "other:其他"}));
        subCharFields.add(new AttributeField("appearance", "外貌描述", FieldType.TEXT_MULTI, null));
        subCharFields.add(new AttributeField("personalityTraits", "性格特征", FieldType.TAG_LIST, null));
        subCharFields.add(new AttributeField("mainFunction", "主要功能", FieldType.TEXT_MULTI, null));
        subCharFields.add(new AttributeField("relationshipToProtagonist", "与主角关系", FieldType.TEXT, null));
        subCharFields.add(new AttributeField("keyScenes", "关键场景", FieldType.TAG_LIST, null));
        subCharFields.add(new AttributeField("developmentPotential", "发展空间", FieldType.TEXT_MULTI, null));
        subCharFields.add(new AttributeField("screenTime", "戏份程度", FieldType.SLIDER, null));
        subCharFields.add(new AttributeField("currentStatus", "现状", FieldType.SELECT, 
            new String[]{"活跃", "退场", "死亡"}));
        SUB_CATEGORY_FIELDS_MAP.put("次要角色", subCharFields);
        
        // 角色 - 反派角色
        SUB_CATEGORY_DISPLAY_MAP.put("反派角色", "角色");
        List<AttributeField> villainFields = new ArrayList<>();
        villainFields.add(new AttributeField("roleType", "角色类型", FieldType.SELECT, 
            new String[]{"antagonist:反派"}));
        villainFields.add(new AttributeField("age", "年龄", FieldType.NUMBER, null));
        villainFields.add(new AttributeField("gender", "性别", FieldType.SELECT, 
            new String[]{"male:男", "female:女", "other:其他"}));
        villainFields.add(new AttributeField("appearance", "外貌描述", FieldType.TEXT_MULTI, null));
        villainFields.add(new AttributeField("personalityTraits", "性格特征", FieldType.TAG_LIST, null));
        villainFields.add(new AttributeField("evilType", "邪恶类型", FieldType.SELECT, 
            new String[]{"野心型", "报复型", "狂热型", "权欲型", "扭曲型"}));
        villainFields.add(new AttributeField("goals", "目标", FieldType.TAG_LIST, null));
        villainFields.add(new AttributeField("methods", "作恶手段", FieldType.TAG_LIST, null));
        villainFields.add(new AttributeField("strengths", "优势", FieldType.TAG_LIST, null));
        villainFields.add(new AttributeField("weaknesses", "弱点", FieldType.TAG_LIST, null));
        villainFields.add(new AttributeField("relationships", "关系网", FieldType.STRUCTURED_LIST, null, "角色名,关系,描述", "\n", ":"));
        villainFields.add(new AttributeField("motive", "动机", FieldType.TEXT_MULTI, null));
        villainFields.add(new AttributeField("threatLevel", "威胁等级", FieldType.SLIDER, null));
        villainFields.add(new AttributeField("currentStatus", "现状", FieldType.SELECT, 
            new String[]{"活跃", "退场", "死亡"}));
        SUB_CATEGORY_FIELDS_MAP.put("反派角色", villainFields);
        
        // 角色 - 组织阵营
        SUB_CATEGORY_DISPLAY_MAP.put("组织阵营", "组织");
        List<AttributeField> orgFields = new ArrayList<>();
        orgFields.add(new AttributeField("groupName", "组织名称", FieldType.TEXT, null));
        orgFields.add(new AttributeField("groupType", "组织类型", FieldType.SELECT, 
            new String[]{"宗门", "帮派", "家族", "王国", "教派", "商会", "秘密组织"}));
        orgFields.add(new AttributeField("scale", "规模", FieldType.SELECT, 
            new String[]{"微型", "小型", "中型", "大型", "巨型"}));
        orgFields.add(new AttributeField("leader", "首领", FieldType.TEXT, null));
        orgFields.add(new AttributeField("coreMembers", "核心成员", FieldType.TAG_LIST, null));
        orgFields.add(new AttributeField("commonTraits", "共同特征", FieldType.TAG_LIST, null));
        orgFields.add(new AttributeField("hierarchy", "内部层级", FieldType.TEXT_MULTI, null));
        orgFields.add(new AttributeField("goals", "组织目标", FieldType.TAG_LIST, null));
        orgFields.add(new AttributeField("codeOfConduct", "行事准则", FieldType.TEXT_MULTI, null));
        orgFields.add(new AttributeField("resources", "资源", FieldType.TAG_LIST, null));
        orgFields.add(new AttributeField("influence", "影响力", FieldType.SLIDER, null));
        orgFields.add(new AttributeField("alignment", "阵营", FieldType.SELECT, 
            new String[]{"正义", "中立", "邪恶", "灰色"}));
        SUB_CATEGORY_FIELDS_MAP.put("组织阵营", orgFields);
    }
    
    // ========== 地点分类 ==========
    private static void initLocationFields() {
        // 地点 - 国家地区
        SUB_CATEGORY_DISPLAY_MAP.put("国家地区", "国家");
        List<AttributeField> countryFields = new ArrayList<>();
        countryFields.add(new AttributeField("countryName", "国家名称", FieldType.TEXT, null));
        countryFields.add(new AttributeField("area", "面积", FieldType.TEXT, null));
        countryFields.add(new AttributeField("population", "人口", FieldType.TEXT, null));
        countryFields.add(new AttributeField("governmentType", "政体", FieldType.SELECT, 
            new String[]{"君主制", "贵族制", "共和制", "民主制", "神权制", "独裁制"}));
        countryFields.add(new AttributeField("capital", "首都", FieldType.TEXT, null));
        countryFields.add(new AttributeField("mainCities", "主要城市", FieldType.TAG_LIST, null));
        countryFields.add(new AttributeField("terrain", "地形", FieldType.TAG_LIST, null));
        countryFields.add(new AttributeField("climate", "气候", FieldType.TEXT, null));
        countryFields.add(new AttributeField("resources", "资源", FieldType.TAG_LIST, null));
        countryFields.add(new AttributeField("economy", "经济状况", FieldType.SELECT, 
            new String[]{"贫困", "温饱", "发达", "强盛"}));
        countryFields.add(new AttributeField("military", "军事实力", FieldType.SELECT, 
            new String[]{"薄弱", "一般", "强大", "顶尖"}));
        countryFields.add(new AttributeField("allies", "盟友", FieldType.TAG_LIST, null));
        countryFields.add(new AttributeField("enemies", "敌国", FieldType.TAG_LIST, null));
        countryFields.add(new AttributeField("stability", "稳定度", FieldType.SLIDER, null));
        countryFields.add(new AttributeField("nationalStrength", "国力", FieldType.SLIDER, null));
        SUB_CATEGORY_FIELDS_MAP.put("国家地区", countryFields);
        
        // 地点 - 城市
        SUB_CATEGORY_DISPLAY_MAP.put("城市", "城市");
        List<AttributeField> cityFields = new ArrayList<>();
        cityFields.add(new AttributeField("cityName", "城市名称", FieldType.TEXT, null));
        cityFields.add(new AttributeField("cityType", "城市类型", FieldType.SELECT, 
            new String[]{"首都", "港口", "商业", "工业", "文化", "军事", "宗教"}));
        cityFields.add(new AttributeField("population", "人口", FieldType.TEXT, null));
        cityFields.add(new AttributeField("location", "位置", FieldType.TEXT, null));
        cityFields.add(new AttributeField("mainDistricts", "主要区域", FieldType.TAG_LIST, null));
        cityFields.add(new AttributeField("features", "城市特色", FieldType.TAG_LIST, null));
        cityFields.add(new AttributeField("economy", "经济", FieldType.SELECT, 
            new String[]{"萧条", "一般", "繁荣", "繁华"}));
        cityFields.add(new AttributeField("famousBuildings", "著名建筑", FieldType.TAG_LIST, null));
        cityFields.add(new AttributeField("notableResidents", "知名居民", FieldType.TAG_LIST, null));
        cityFields.add(new AttributeField("dangerLevel", "危险程度", FieldType.SLIDER, null));
        cityFields.add(new AttributeField("prosperity", "繁华程度", FieldType.SLIDER, null));
        SUB_CATEGORY_FIELDS_MAP.put("城市", cityFields);
        
        // 地点 - 村庄
        SUB_CATEGORY_DISPLAY_MAP.put("村庄", "村庄");
        List<AttributeField> villageFields = new ArrayList<>();
        villageFields.add(new AttributeField("villageName", "村庄名称", FieldType.TEXT, null));
        villageFields.add(new AttributeField("location", "位置", FieldType.TEXT, null));
        villageFields.add(new AttributeField("population", "人口", FieldType.TEXT, null));
        villageFields.add(new AttributeField("mainIndustry", "主要产业", FieldType.SELECT, 
            new String[]{"农业", "渔业", "林业", "牧业", "手工业", "商业"}));
        villageFields.add(new AttributeField("features", "村庄特色", FieldType.TAG_LIST, null));
        villageFields.add(new AttributeField("customs", "习俗", FieldType.TAG_LIST, null));
        villageFields.add(new AttributeField("notableNPCs", "知名NPC", FieldType.TAG_LIST, null));
        villageFields.add(new AttributeField("relationships", "与主要势力关系", FieldType.TEXT_MULTI, null));
        villageFields.add(new AttributeField("development", "发展状况", FieldType.SELECT, 
            new String[]{"落后", "普通", "发达"}));
        villageFields.add(new AttributeField("dangerLevel", "危险程度", FieldType.SLIDER, null));
        villageFields.add(new AttributeField("isolation", "封闭程度", FieldType.SLIDER, null));
        SUB_CATEGORY_FIELDS_MAP.put("村庄", villageFields);
        
        // 地点 - 自然景观
        SUB_CATEGORY_DISPLAY_MAP.put("自然景观", "景观");
        List<AttributeField> landscapeFields = new ArrayList<>();
        landscapeFields.add(new AttributeField("landscapeName", "景观名称", FieldType.TEXT, null));
        landscapeFields.add(new AttributeField("landscapeType", "景观类型", FieldType.SELECT, 
            new String[]{"山脉", "水域", "森林", "沙漠", "草原", "冰川", "火山", "洞穴", "海岛"}));
        landscapeFields.add(new AttributeField("location", "位置", FieldType.TEXT, null));
        landscapeFields.add(new AttributeField("features", "景观特征", FieldType.TAG_LIST, null));
        landscapeFields.add(new AttributeField("dangerLevel", "危险程度", FieldType.SLIDER, null));
        landscapeFields.add(new AttributeField("resources", "资源", FieldType.TAG_LIST, null));
        landscapeFields.add(new AttributeField("legends", "传说典故", FieldType.TAG_LIST, null));
        landscapeFields.add(new AttributeField("visitors", "访客情况", FieldType.SELECT, 
            new String[]{"无人区", "探险地", "旅游地", "禁区"}));
        landscapeFields.add(new AttributeField("ecology", "生态环境", FieldType.TEXT_MULTI, null));
        SUB_CATEGORY_FIELDS_MAP.put("自然景观", landscapeFields);
        
        // 地点 - 关键场景
        SUB_CATEGORY_DISPLAY_MAP.put("关键场景", "场景");
        List<AttributeField> sceneFields = new ArrayList<>();
        sceneFields.add(new AttributeField("sceneName", "场景名称", FieldType.TEXT, null));
        sceneFields.add(new AttributeField("sceneType", "场景类型", FieldType.SELECT, 
            new String[]{"室内", "室外", "私密", "公开", "战斗", "会议", "日常"}));
        sceneFields.add(new AttributeField("location", "位置", FieldType.TEXT, null));
        sceneFields.add(new AttributeField("atmosphere", "氛围", FieldType.TEXT_MULTI, null));
        sceneFields.add(new AttributeField("keyItems", "关键物品", FieldType.TAG_LIST, null));
        sceneFields.add(new AttributeField("importantEvents", "重要事件", FieldType.TAG_LIST, null));
        sceneFields.add(new AttributeField("associatedCharacters", "关联角色", FieldType.TAG_LIST, null));
        sceneFields.add(new AttributeField("function", "剧情功能", FieldType.TEXT_MULTI, null));
        sceneFields.add(new AttributeField("accessibility", "可达性", FieldType.SELECT, 
            new String[]{"自由出入", "需要许可", "隐藏", "危险"}));
        SUB_CATEGORY_FIELDS_MAP.put("关键场景", sceneFields);
        
        // 地点 - 建筑设施
        SUB_CATEGORY_DISPLAY_MAP.put("建筑设施", "建筑");
        List<AttributeField> buildingFields = new ArrayList<>();
        buildingFields.add(new AttributeField("buildingName", "建筑名称", FieldType.TEXT, null));
        buildingFields.add(new AttributeField("buildingType", "建筑类型", FieldType.SELECT, 
            new String[]{"宫殿", "府邸", "商铺", "客栈", "工坊", "塔楼", "城墙", "祭坛", "监狱"}));
        buildingFields.add(new AttributeField("location", "位置", FieldType.TEXT, null));
        buildingFields.add(new AttributeField("size", "规模", FieldType.SELECT, 
            new String[]{"小型", "中型", "大型", "巨型"}));
        buildingFields.add(new AttributeField("features", "建筑特色", FieldType.TAG_LIST, null));
        buildingFields.add(new AttributeField("defense", "防御能力", FieldType.TEXT_MULTI, null));
        buildingFields.add(new AttributeField("facilities", "内部设施", FieldType.TAG_LIST, null));
        buildingFields.add(new AttributeField("owner", "所有者", FieldType.TEXT, null));
        buildingFields.add(new AttributeField("staff", "人员", FieldType.TAG_LIST, null));
        buildingFields.add(new AttributeField("security", "安保程度", FieldType.SLIDER, null));
        SUB_CATEGORY_FIELDS_MAP.put("建筑设施", buildingFields);
        
        // 地点 - 特殊空间
        SUB_CATEGORY_DISPLAY_MAP.put("特殊空间", "空间");
        List<AttributeField> spaceFields = new ArrayList<>();
        spaceFields.add(new AttributeField("spaceName", "空间名称", FieldType.TEXT, null));
        spaceFields.add(new AttributeField("spaceType", "空间类型", FieldType.SELECT, 
            new String[]{"小世界", "秘境", "遗迹", "阵法空间", "异次元", "梦境", "传承空间"}));
        spaceFields.add(new AttributeField("entryCondition", "进入条件", FieldType.TEXT_MULTI, null));
        spaceFields.add(new AttributeField("size", "空间大小", FieldType.SELECT, 
            new String[]{"小型", "中型", "大型", "无限"}));
        spaceFields.add(new AttributeField("timeFlow", "时间流速", FieldType.SELECT, 
            new String[]{"正常", "加速", "减缓", "静止", "混乱"}));
        spaceFields.add(new AttributeField("rules", "特殊规则", FieldType.TAG_LIST, null));
        spaceFields.add(new AttributeField("dangers", "危险", FieldType.TAG_LIST, null));
        spaceFields.add(new AttributeField("treasures", "宝物", FieldType.TAG_LIST, null));
        spaceFields.add(new AttributeField("origin", "起源", FieldType.TEXT_MULTI, null));
        SUB_CATEGORY_FIELDS_MAP.put("特殊空间", spaceFields);
    }
    
    // ========== 剧情分类 ==========
    private static void initPlotFields() {
        // 剧情 - 主线剧情
        SUB_CATEGORY_DISPLAY_MAP.put("主线剧情", "主线");
        List<AttributeField> mainPlotFields = new ArrayList<>();
        mainPlotFields.add(new AttributeField("storyArc", "起承转合", FieldType.TEXT_MULTI, null));
        mainPlotFields.add(new AttributeField("keyTurningPoints", "关键转折点", FieldType.STRUCTURED_LIST, null, "章节,事件,重要性", "\n", ":"));
        mainPlotFields.add(new AttributeField("centralConflict", "核心冲突", FieldType.TEXT_MULTI, null));
        mainPlotFields.add(new AttributeField("resolution", "结局走向", FieldType.TEXT_MULTI, null));
        mainPlotFields.add(new AttributeField("themes", "涉及主题", FieldType.TAG_LIST, null));
        mainPlotFields.add(new AttributeField("pacing", "节奏", FieldType.SELECT, 
            new String[]{"慢热", "平稳", "紧凑", "高潮迭起"}));
        mainPlotFields.add(new AttributeField("length", "篇幅", FieldType.SELECT, 
            new String[]{"短篇", "中篇", "长篇", "超长篇"}));
        SUB_CATEGORY_FIELDS_MAP.put("主线剧情", mainPlotFields);
        
        // 剧情 - 支线剧情
        SUB_CATEGORY_DISPLAY_MAP.put("支线剧情", "支线");
        List<AttributeField> subplotFields = new ArrayList<>();
        subplotFields.add(new AttributeField("subplotName", "支线名称", FieldType.TEXT, null));
        subplotFields.add(new AttributeField("relatedMainPlot", "关联主线", FieldType.TEXT, null));
        subplotFields.add(new AttributeField("purpose", "支线作用", FieldType.TEXT_MULTI, null));
        subplotFields.add(new AttributeField("startChapter", "起始章节", FieldType.NUMBER, null));
        subplotFields.add(new AttributeField("endChapter", "结束章节", FieldType.NUMBER, null));
        subplotFields.add(new AttributeField("keyCharacters", "关键角色", FieldType.TAG_LIST, null));
        subplotFields.add(new AttributeField("resolutionImpact", "对主线影响", FieldType.TEXT_MULTI, null));
        subplotFields.add(new AttributeField("status", "状态", FieldType.SELECT, 
            new String[]{"待开启", "进行中", "已完成", "已放弃"}));
        SUB_CATEGORY_FIELDS_MAP.put("支线剧情", subplotFields);
        
        // 剧情 - 关键事件
        SUB_CATEGORY_DISPLAY_MAP.put("关键事件", "事件");
        List<AttributeField> eventFields = new ArrayList<>();
        eventFields.add(new AttributeField("eventName", "事件名称", FieldType.TEXT, null));
        eventFields.add(new AttributeField("eventType", "事件类型", FieldType.SELECT, 
            new String[]{"battle:战斗", "discovery:发现", "betrayal:背叛", "revelation:揭露", "decision:抉择", "ceremony:仪式", "other:其他"}));
        eventFields.add(new AttributeField("chapterNumber", "发生章节", FieldType.TEXT, null));
        eventFields.add(new AttributeField("participants", "参与者", FieldType.TAG_LIST, null));
        eventFields.add(new AttributeField("outcome", "事件结果", FieldType.TEXT_MULTI, null));
        eventFields.add(new AttributeField("consequences", "后续影响", FieldType.TAG_LIST, null));
        eventFields.add(new AttributeField("significance", "重要性", FieldType.SLIDER, null));
        eventFields.add(new AttributeField("description", "详细描述", FieldType.TEXT_MULTI, null));
        SUB_CATEGORY_FIELDS_MAP.put("关键事件", eventFields);
        
        // 剧情 - 悬念伏笔
        SUB_CATEGORY_DISPLAY_MAP.put("悬念伏笔", "悬念");
        List<AttributeField> suspenseFields = new ArrayList<>();
        suspenseFields.add(new AttributeField("suspenseName", "悬念名称", FieldType.TEXT, null));
        suspenseFields.add(new AttributeField("suspenseStatus", "状态", FieldType.SELECT, 
            new String[]{"pending:待回收", "reinforced:已强化", "resolved:已回收", "abandoned:已放弃"}));
        suspenseFields.add(new AttributeField("suspenseIntensity", "悬念强度", FieldType.SLIDER, null));
        suspenseFields.add(new AttributeField("setupChapterNumber", "埋设章节", FieldType.TEXT, null));
        suspenseFields.add(new AttributeField("setupDescription", "埋设描述", FieldType.TEXT_MULTI, null));
        suspenseFields.add(new AttributeField("resolveChapterNumber", "回收章节", FieldType.TEXT, null));
        suspenseFields.add(new AttributeField("resolveDescription", "回收描述", FieldType.TEXT_MULTI, null));
        suspenseFields.add(new AttributeField("clueList", "相关线索", FieldType.TAG_LIST, null));
        suspenseFields.add(new AttributeField("expectedReveal", "预期揭示方式", FieldType.TEXT_MULTI, null));
        suspenseFields.add(new AttributeField("readerExpectation", "读者期待度", FieldType.SLIDER, null));
        SUB_CATEGORY_FIELDS_MAP.put("悬念伏笔", suspenseFields);
        
        // 剧情 - 章节规划
        SUB_CATEGORY_DISPLAY_MAP.put("章节规划", "章节");
        List<AttributeField> chapterFields = new ArrayList<>();
        chapterFields.add(new AttributeField("chapterNumber", "章节序号", FieldType.TEXT, null));
        chapterFields.add(new AttributeField("chapterTitle", "章节标题", FieldType.TEXT, null));
        chapterFields.add(new AttributeField("chapterType", "章节类型", FieldType.SELECT, 
            new String[]{"序章", "开篇", "发展", "高潮", "转折", "过渡", "结局", "尾声"}));
        chapterFields.add(new AttributeField("pov", "视角人物", FieldType.TEXT, null));
        chapterFields.add(new AttributeField("mainContent", "主要内容", FieldType.TEXT_MULTI, null));
        chapterFields.add(new AttributeField("wordCount", "预计字数", FieldType.NUMBER, null));
        chapterFields.add(new AttributeField("keyPlotPoints", "关键情节点", FieldType.TAG_LIST, null));
        chapterFields.add(new AttributeField("foreshadowing", "伏笔", FieldType.TAG_LIST, null));
        chapterFields.add(new AttributeField("mood", "章节氛围", FieldType.TEXT, null));
        SUB_CATEGORY_FIELDS_MAP.put("章节规划", chapterFields);
        
        // 剧情 - 矛盾冲突
        SUB_CATEGORY_DISPLAY_MAP.put("矛盾冲突", "冲突");
        List<AttributeField> conflictFields = new ArrayList<>();
        conflictFields.add(new AttributeField("conflictName", "冲突名称", FieldType.TEXT, null));
        conflictFields.add(new AttributeField("conflictType", "冲突类型", FieldType.SELECT, 
            new String[]{"个人", "势力", "理念", "利益", "情感", "生存"}));
        conflictFields.add(new AttributeField("parties", "冲突方", FieldType.TAG_LIST, null));
        conflictFields.add(new AttributeField("coreIssue", "核心问题", FieldType.TEXT_MULTI, null));
        conflictFields.add(new AttributeField("intensity", "激烈程度", FieldType.SLIDER, null));
        conflictFields.add(new AttributeField("history", "冲突历史", FieldType.TEXT_MULTI, null));
        conflictFields.add(new AttributeField("currentStatus", "现状", FieldType.TEXT_MULTI, null));
        conflictFields.add(new AttributeField("potentialResolution", "可能结局", FieldType.TEXT_MULTI, null));
        conflictFields.add(new AttributeField("impactOnPlot", "对剧情影响", FieldType.TEXT_MULTI, null));
        SUB_CATEGORY_FIELDS_MAP.put("矛盾冲突", conflictFields);
        
        // 剧情 - 时间线
        SUB_CATEGORY_DISPLAY_MAP.put("时间线", "时间");
        List<AttributeField> timelineFields = new ArrayList<>();
        timelineFields.add(new AttributeField("eventName", "事件名称", FieldType.TEXT, null));
        timelineFields.add(new AttributeField("eventType", "事件类型", FieldType.SELECT, 
            new String[]{"历史", "当前", "未来", "预测"}));
        timelineFields.add(new AttributeField("absoluteTime", "绝对时间", FieldType.TEXT, null));
        timelineFields.add(new AttributeField("relativeTime", "相对时间", FieldType.TEXT, null));
        timelineFields.add(new AttributeField("location", "发生地点", FieldType.TEXT, null));
        timelineFields.add(new AttributeField("participants", "参与者", FieldType.TAG_LIST, null));
        timelineFields.add(new AttributeField("causes", "起因", FieldType.TAG_LIST, null));
        timelineFields.add(new AttributeField("process", "过程", FieldType.TEXT_MULTI, null));
        timelineFields.add(new AttributeField("results", "结果", FieldType.TAG_LIST, null));
        timelineFields.add(new AttributeField("connections", "关联事件", FieldType.TAG_LIST, null));
        SUB_CATEGORY_FIELDS_MAP.put("时间线", timelineFields);
    }
    
    // ========== 规则体系分类 ==========
    private static void initRuleSystemFields() {
        // 规则体系 - 力量体系
        SUB_CATEGORY_DISPLAY_MAP.put("力量体系", "力量");
        List<AttributeField> powerFields = new ArrayList<>();
        powerFields.add(new AttributeField("systemName", "体系名称", FieldType.TEXT, null));
        powerFields.add(new AttributeField("systemType", "体系类型", FieldType.SELECT, 
            new String[]{"修真", "魔法", "异能", "武技", "科技", "混合"}));
        powerFields.add(new AttributeField("progressionPath", "晋升路径", FieldType.STRUCTURED_LIST, null, "等级阶段", "\n", ":"));
        powerFields.add(new AttributeField("requirements", "晋升条件", FieldType.TAG_LIST, null));
        powerFields.add(new AttributeField("coreAbilities", "核心能力", FieldType.TAG_LIST, null));
        powerFields.add(new AttributeField("limitations", "限制条件", FieldType.TEXT_MULTI, null));
        powerFields.add(new AttributeField("sideEffects", "副作用", FieldType.TEXT_MULTI, null));
        powerFields.add(new AttributeField("famousPractitioners", "知名修炼者", FieldType.TAG_LIST, null));
        powerFields.add(new AttributeField("cultivationResources", "修炼资源", FieldType.TAG_LIST, null));
        powerFields.add(new AttributeField("battleStyle", "战斗风格", FieldType.TEXT_MULTI, null));
        SUB_CATEGORY_FIELDS_MAP.put("力量体系", powerFields);
        
        // 规则体系 - 魔法或超能力
        SUB_CATEGORY_DISPLAY_MAP.put("魔法或超能力", "能力");
        List<AttributeField> magicFields = new ArrayList<>();
        magicFields.add(new AttributeField("powerName", "能力名称", FieldType.TEXT, null));
        magicFields.add(new AttributeField("powerType", "能力类型", FieldType.SELECT, 
            new String[]{"元素", "精神", "物质", "时空", "召唤", "辅助", "混合"}));
        magicFields.add(new AttributeField("source", "能量来源", FieldType.SELECT, 
            new String[]{"天赋", "修炼", "道具", "契约", "血脉", "科技"}));
        magicFields.add(new AttributeField("castingMethod", "施展方式", FieldType.TEXT_MULTI, null));
        magicFields.add(new AttributeField("effects", "效果", FieldType.TAG_LIST, null));
        magicFields.add(new AttributeField("limitations", "限制", FieldType.TAG_LIST, null));
        magicFields.add(new AttributeField("requirements", "前置条件", FieldType.TAG_LIST, null));
        magicFields.add(new AttributeField("sideEffects", "副作用", FieldType.TAG_LIST, null));
        magicFields.add(new AttributeField("countermeasures", "克制方法", FieldType.TAG_LIST, null));
        magicFields.add(new AttributeField("famousUsers", "知名使用者", FieldType.TAG_LIST, null));
        SUB_CATEGORY_FIELDS_MAP.put("魔法或超能力", magicFields);
        
        // 规则体系 - 战斗系统
        SUB_CATEGORY_DISPLAY_MAP.put("战斗系统", "战斗");
        List<AttributeField> battleFields = new ArrayList<>();
        battleFields.add(new AttributeField("systemName", "系统名称", FieldType.TEXT, null));
        battleFields.add(new AttributeField("battleRules", "战斗规则", FieldType.TAG_LIST, null));
        battleFields.add(new AttributeField("victoryConditions", "胜利条件", FieldType.TAG_LIST, null));
        battleFields.add(new AttributeField("attackTypes", "攻击类型", FieldType.TAG_LIST, null));
        battleFields.add(new AttributeField("defenseTypes", "防御类型", FieldType.TAG_LIST, null));
        battleFields.add(new AttributeField("supportTypes", "辅助类型", FieldType.TAG_LIST, null));
        battleFields.add(new AttributeField("specialMechanics", "特殊机制", FieldType.TAG_LIST, null));
        battleFields.add(new AttributeField("balanceFactors", "平衡因素", FieldType.TEXT_MULTI, null));
        SUB_CATEGORY_FIELDS_MAP.put("战斗系统", battleFields);
        
        // 规则体系 - 经济体系
        SUB_CATEGORY_DISPLAY_MAP.put("经济体系", "经济");
        List<AttributeField> economyFields = new ArrayList<>();
        economyFields.add(new AttributeField("currencyName", "货币名称", FieldType.TEXT, null));
        economyFields.add(new AttributeField("exchangeRate", "汇率", FieldType.TEXT_MULTI, null));
        economyFields.add(new AttributeField("wealthDistribution", "财富分布", FieldType.TEXT_MULTI, null));
        economyFields.add(new AttributeField("economicCycle", "经济周期", FieldType.SELECT, 
            new String[]{"稳定", "波动", "通胀", "通缩", "危机"}));
        economyFields.add(new AttributeField("mainIndustries", "主要产业", FieldType.TAG_LIST, null));
        economyFields.add(new AttributeField("tradeRoutes", "贸易路线", FieldType.TAG_LIST, null));
        economyFields.add(new AttributeField("economicFactors", "经济因素", FieldType.TAG_LIST, null));
        economyFields.add(new AttributeField("wealthGap", "贫富差距", FieldType.SLIDER, null));
        economyFields.add(new AttributeField("economicStability", "经济稳定性", FieldType.SLIDER, null));
        SUB_CATEGORY_FIELDS_MAP.put("经济体系", economyFields);
        
        // 规则体系 - 时间规则
        SUB_CATEGORY_DISPLAY_MAP.put("时间规则", "时间");
        List<AttributeField> timeRuleFields = new ArrayList<>();
        timeRuleFields.add(new AttributeField("timeSystemName", "时间系统名称", FieldType.TEXT, null));
        timeRuleFields.add(new AttributeField("timeFlow", "时间流速", FieldType.SELECT, 
            new String[]{"正常", "加速", "减缓", "静止", "倒流", "混乱"}));
        timeRuleFields.add(new AttributeField("timeMeasurement", "时间计量", FieldType.TEXT_MULTI, null));
        timeRuleFields.add(new AttributeField("timeManipulation", "时间操控", FieldType.TEXT_MULTI, null));
        timeRuleFields.add(new AttributeField("paradoxHandling", "悖论处理", FieldType.TEXT_MULTI, null));
        timeRuleFields.add(new AttributeField("timelineBranches", "时间线分支", FieldType.TAG_LIST, null));
        timeRuleFields.add(new AttributeField("keyTemporalEvents", "关键时间事件", FieldType.TAG_LIST, null));
        timeRuleFields.add(new AttributeField("rules", "时间规则", FieldType.TAG_LIST, null));
        SUB_CATEGORY_FIELDS_MAP.put("时间规则", timeRuleFields);
        
        // 规则体系 - 限制条件
        SUB_CATEGORY_DISPLAY_MAP.put("限制条件", "限制");
        List<AttributeField> restrictionFields = new ArrayList<>();
        restrictionFields.add(new AttributeField("ruleName", "规则名称", FieldType.TEXT, null));
        restrictionFields.add(new AttributeField("ruleType", "规则类型", FieldType.SELECT, 
            new String[]{"物理", "魔法", "社会", "道德", "宇宙"}));
        restrictionFields.add(new AttributeField("affectedEntities", "适用对象", FieldType.TAG_LIST, null));
        restrictionFields.add(new AttributeField("restriction", "限制内容", FieldType.TEXT_MULTI, null));
        restrictionFields.add(new AttributeField("punishment", "违规惩罚", FieldType.TEXT_MULTI, null));
        restrictionFields.add(new AttributeField("exceptions", "例外情况", FieldType.TAG_LIST, null));
        restrictionFields.add(new AttributeField("reason", "设立原因", FieldType.TEXT_MULTI, null));
        restrictionFields.add(new AttributeField("enforcement", "执行机制", FieldType.TEXT_MULTI, null));
        SUB_CATEGORY_FIELDS_MAP.put("限制条件", restrictionFields);
    }
    
    // ========== 创作控制分类 ==========
    private static void initCreativeControlFields() {
        // 创作控制 - 主题内核
        SUB_CATEGORY_DISPLAY_MAP.put("主题内核", "主题");
        List<AttributeField> themeFields = new ArrayList<>();
        themeFields.add(new AttributeField("themeName", "主题名称", FieldType.TEXT, null));
        themeFields.add(new AttributeField("themeCategory", "主题类别", FieldType.SELECT, 
            new String[]{"love:爱情", "justice:正义", "growth:成长", "sacrifice:牺牲", 
                "freedom:自由", "power:权力", "identity:身份", "revenge:复仇", "other:其他"}));
        themeFields.add(new AttributeField("coreMessage", "核心信息", FieldType.TEXT_MULTI, null));
        themeFields.add(new AttributeField("symbolicElements", "象征元素", FieldType.TAG_LIST, null));
        themeFields.add(new AttributeField("philosophicalQuestions", "哲学问题", FieldType.TAG_LIST, null));
        themeFields.add(new AttributeField("moralDilemmas", "道德困境", FieldType.TAG_LIST, null));
        themeFields.add(new AttributeField("explorationDepth", "探索深度", FieldType.SLIDER, null));
        themeFields.add(new AttributeField("presentationBalance", "呈现平衡", FieldType.SLIDER, null));
        SUB_CATEGORY_FIELDS_MAP.put("主题内核", themeFields);
        
        // 创作控制 - 语言风格
        SUB_CATEGORY_DISPLAY_MAP.put("语言风格", "风格");
        List<AttributeField> styleFields = new ArrayList<>();
        styleFields.add(new AttributeField("writingStyle", "写作风格", FieldType.SELECT, 
            new String[]{"简洁", "华丽", "诗意", "写实", "古典", "现代", "幽默", "严肃"}));
        styleFields.add(new AttributeField("languageFeatures", "语言特色", FieldType.TAG_LIST, null));
        styleFields.add(new AttributeField("sentenceStructure", "句式特点", FieldType.TEXT_MULTI, null));
        styleFields.add(new AttributeField("rhetoricalDevices", "修辞手法", FieldType.TAG_LIST, null));
        styleFields.add(new AttributeField("dialogueStyle", "对话风格", FieldType.TEXT_MULTI, null));
        styleFields.add(new AttributeField("narrationType", "叙述类型", FieldType.SELECT, 
            new String[]{"第一人称", "第三人称全知", "第三人称限知", "多视角"}));
        styleFields.add(new AttributeField("referenceWorks", "参考作品", FieldType.TAG_LIST, null));
        styleFields.add(new AttributeField("avoidElements", "避免元素", FieldType.TAG_LIST, null));
        SUB_CATEGORY_FIELDS_MAP.put("语言风格", styleFields);
        
        // 创作控制 - 情感基调
        SUB_CATEGORY_DISPLAY_MAP.put("情感基调", "基调");
        List<AttributeField> toneFields = new ArrayList<>();
        toneFields.add(new AttributeField("overallTone", "整体基调", FieldType.SELECT, 
            new String[]{"serious:严肃", "humorous:幽默", "dark:黑暗", "romantic:浪漫", 
                "tragic:悲情", "comedic:喜剧", "melancholic:忧郁", "epic:史诗"}));
        toneFields.add(new AttributeField("emotionalRange", "情感范围", FieldType.TAG_LIST, null));
        toneFields.add(new AttributeField("pacing", "节奏", FieldType.SELECT, 
            new String[]{"fast:快", "medium:中", "slow:慢", "variable:起伏"}));
        toneFields.add(new AttributeField("atmosphere", "整体氛围", FieldType.TEXT_MULTI, null));
        toneFields.add(new AttributeField("emotionalPeaks", "情感高峰", FieldType.TAG_LIST, null));
        toneFields.add(new AttributeField("emotionalValleys", "情感低谷", FieldType.TAG_LIST, null));
        toneFields.add(new AttributeField("climacticMoments", "高潮时刻", FieldType.TAG_LIST, null));
        toneFields.add(new AttributeField("resolutionMood", "结局情绪", FieldType.TEXT_MULTI, null));
        SUB_CATEGORY_FIELDS_MAP.put("情感基调", toneFields);
        
        // 创作控制 - 叙事视角
        SUB_CATEGORY_DISPLAY_MAP.put("叙事视角", "视角");
        List<AttributeField> narrativeFields = new ArrayList<>();
        narrativeFields.add(new AttributeField("narrativePerspective", "叙事视角", FieldType.SELECT, 
            new String[]{"第一人称", "第三人称全知", "第三人称限知", "第二人称"}));
        narrativeFields.add(new AttributeField("povCharacter", "视角人物", FieldType.TEXT, null));
        narrativeFields.add(new AttributeField("reliability", "可靠性", FieldType.SELECT, 
            new String[]{"完全可靠", "部分可靠", "不可靠"}));
        narrativeFields.add(new AttributeField("omniscience", "全知程度", FieldType.SELECT, 
            new String[]{"全知", "受限全知", "限制视角"}));
        narrativeFields.add(new AttributeField("limitedKnowledge", "已知限制", FieldType.TEXT_MULTI, null));
        narrativeFields.add(new AttributeField("povAdvantages", "视角优势", FieldType.TEXT_MULTI, null));
        narrativeFields.add(new AttributeField("povLimitations", "视角局限", FieldType.TEXT_MULTI, null));
        narrativeFields.add(new AttributeField("multiplePovs", "多视角人物", FieldType.TAG_LIST, null));
        SUB_CATEGORY_FIELDS_MAP.put("叙事视角", narrativeFields);
        
        // 创作控制 - 节奏控制
        SUB_CATEGORY_DISPLAY_MAP.put("节奏控制", "节奏");
        List<AttributeField> pacingFields = new ArrayList<>();
        pacingFields.add(new AttributeField("overallPacing", "整体节奏", FieldType.SELECT, 
            new String[]{"slow:慢热", "gradual:渐入佳境", "steady:平稳推进", "fast:快节奏"}));
        pacingFields.add(new AttributeField("chapterLength", "章节长度", FieldType.SELECT, 
            new String[]{"短", "中", "长", "不定"}));
        pacingFields.add(new AttributeField("sceneLength", "场景长度", FieldType.SELECT, 
            new String[]{"短", "中", "长"}));
        pacingFields.add(new AttributeField("dialogueRatio", "对话占比", FieldType.SELECT, 
            new String[]{"少", "中", "多"}));
        pacingFields.add(new AttributeField("actionRatio", "动作占比", FieldType.SELECT, 
            new String[]{"少", "中", "多"}));
        pacingFields.add(new AttributeField("descriptionRatio", "描写占比", FieldType.SELECT, 
            new String[]{"简洁", "适中", "详细"}));
        pacingFields.add(new AttributeField("climaxFrequency", "高潮频率", FieldType.SELECT, 
            new String[]{"稀少", "适中", "频繁"}));
        pacingFields.add(new AttributeField("tensionMaintained", "张力维持", FieldType.SLIDER, null));
        SUB_CATEGORY_FIELDS_MAP.put("节奏控制", pacingFields);
    }
    
    // ========== 核心方法 ==========
    
    /**
     * 解析专属属性JSON
     */
    public Map<String, Object> parseSpecificAttributes(StorySetting setting) {
        if (setting == null || TextUtils.isEmpty(setting.getSpecificAttributes())) {
            return null;
        }
        
        try {
            JsonObject jsonObj = JsonParser.parseString(setting.getSpecificAttributes()).getAsJsonObject();
            Map<String, Object> map = new HashMap<>();
            for (Map.Entry<String, com.google.gson.JsonElement> entry : jsonObj.entrySet()) {
                map.put(entry.getKey(), jsonToObject(entry.getValue()));
            }
            return map;
        } catch (Exception e) {
            Log.e(TAG, "解析专属属性失败: " + e.getMessage());
            return null;
        }
    }
    
    private Object jsonToObject(com.google.gson.JsonElement element) {
        if (element.isJsonNull()) return null;
        if (element.isJsonPrimitive()) {
            com.google.gson.JsonPrimitive primitive = element.getAsJsonPrimitive();
            if (primitive.isNumber()) {
                // 数值类型转为字符串，便于统一处理
                Number num = primitive.getAsNumber();
                if (num instanceof Float || num instanceof Double) {
                    return String.valueOf(num.doubleValue());
                }
                return String.valueOf(num.longValue());
            }
            return primitive.getAsString();
        }
        if (element.isJsonArray()) {
            com.google.gson.JsonArray jsonArray = element.getAsJsonArray();
            // 检查是否是嵌套数组（结构化列表）
            if (jsonArray.size() > 0 && jsonArray.get(0).isJsonArray()) {
                // 嵌套数组：List<List<String>>
                List<List<String>> nestedList = new ArrayList<>();
                for (com.google.gson.JsonElement item : jsonArray) {
                    com.google.gson.JsonArray subArray = item.getAsJsonArray();
                    List<String> subList = new ArrayList<>();
                    for (com.google.gson.JsonElement subItem : subArray) {
                        subList.add(jsonElementToString(subItem));
                    }
                    nestedList.add(subList);
                }
                return nestedList;
            } else {
                // 普通数组：List<String>
                List<String> list = new ArrayList<>();
                for (com.google.gson.JsonElement item : jsonArray) {
                    list.add(jsonElementToString(item));
                }
                return list;
            }
        } else if (element.isJsonObject()) {
            return gson.fromJson(element, new TypeToken<Map<String, Object>>(){}.getType());
        }
        return null;
    }
    
    /**
     * 将JsonElement转换为字符串
     */
    private String jsonElementToString(com.google.gson.JsonElement element) {
        if (element.isJsonNull()) return "";
        if (element.isJsonPrimitive()) {
            com.google.gson.JsonPrimitive primitive = element.getAsJsonPrimitive();
            if (primitive.isNumber()) {
                return String.valueOf(primitive.getAsNumber());
            }
            return primitive.getAsString();
        }
        return element.toString();
    }
    
    /**
     * 获取子分类对应的属性字段定义
     */
    public List<AttributeField> getFieldsForSubCategory(String subCategory) {
        List<AttributeField> fields = SUB_CATEGORY_FIELDS_MAP.get(subCategory);
        return fields != null ? fields : new ArrayList<>();
    }
    
    /**
     * 检查子分类是否有专属属性定义
     */
    public boolean hasSpecificFields(String subCategory) {
        return SUB_CATEGORY_FIELDS_MAP.containsKey(subCategory);
    }
    
    /**
     * 获取子分类的中文显示名称
     */
    public String getDisplayName(String subCategory) {
        String display = SUB_CATEGORY_DISPLAY_MAP.get(subCategory);
        return display != null ? display : subCategory;
    }
    
    /**
     * 获取所有有专属属性定义的子分类
     */
    public List<String> getAllSubCategoriesWithFields() {
        return new ArrayList<>(SUB_CATEGORY_FIELDS_MAP.keySet());
    }
    
    /**
     * 获取所有子分类的显示名称映射
     */
    public Map<String, String> getAllDisplayNames() {
        return new HashMap<>(SUB_CATEGORY_DISPLAY_MAP);
    }
    
    /**
     * 将Object转换为Map
     */
    public Map<String, Object> objectToMap(Object obj) {
        if (obj == null) {
            return new HashMap<>();
        }
        
        try {
            JsonObject jsonObj = gson.toJsonTree(obj).getAsJsonObject();
            Map<String, Object> map = new HashMap<>();
            for (Map.Entry<String, com.google.gson.JsonElement> entry : jsonObj.entrySet()) {
                map.put(entry.getKey(), jsonToObject(entry.getValue()));
            }
            return map;
        } catch (Exception e) {
            Log.e(TAG, "对象转Map失败: " + e.getMessage());
            return new HashMap<>();
        }
    }
    
    // ========== 内部类：属性字段定义 ==========
    public static class AttributeField {
        private String key;
        private String label;
        private FieldType type;
        private String[] options;
        // 结构化列表的输入模板，格式为逗号分隔的字段描述
        // 例如："角色名,关系,描述" 或 "章节,事件,重要性"
        private String inputTemplate;
        // 结构化列表的字段分隔符
        private String itemDelimiter;
        // 结构化列表的字段分隔符（内部字段用冒号分隔）
        private String fieldDelimiter;
        
        public AttributeField(String key, String label, FieldType type, String[] options) {
            this(key, label, type, options, null);
        }
        
        public AttributeField(String key, String label, FieldType type, String[] options, String inputTemplate) {
            this.key = key;
            this.label = label;
            this.type = type;
            this.options = options;
            this.inputTemplate = inputTemplate;
            this.itemDelimiter = "\n";  // 默认每行一条记录
            this.fieldDelimiter = ":";  // 默认冒号分隔
        }
        
        public AttributeField(String key, String label, FieldType type, String[] options, String inputTemplate, 
                                 String itemDelimiter, String fieldDelimiter) {
            this.key = key;
            this.label = label;
            this.type = type;
            this.options = options;
            this.inputTemplate = inputTemplate;
            this.itemDelimiter = itemDelimiter;
            this.fieldDelimiter = fieldDelimiter;
        }
        
        public String getKey() { return key; }
        public String getLabel() { return label; }
        public FieldType getType() { return type; }
        public String[] getOptions() { return options; }
        public String getInputTemplate() { return inputTemplate; }
        public String getItemDelimiter() { return itemDelimiter; }
        public String getFieldDelimiter() { return fieldDelimiter; }
    }
    
    public enum FieldType {
        TEXT,           // 单行文本
        TEXT_MULTI,     // 多行文本
        NUMBER,         // 数字
        SLIDER,         // 滑块（1-10）
        SELECT,          // 下拉选择
        TAG_LIST,        // 标签列表
        STRUCTURED_LIST  // 结构化列表输入（如关系网、转折点等有固定格式的列表）
    }
}
