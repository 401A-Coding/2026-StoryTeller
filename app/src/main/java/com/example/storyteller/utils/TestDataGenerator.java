package com.example.storyteller.utils;

import android.content.Context;
import com.example.storyteller.data.local.db.StorySettingDao;
import com.example.storyteller.model.StorySetting;

/**
 * 测试数据生成器
 * 用于快速填充设定数据，方便UI测试
 */
public class TestDataGenerator {

    /**
     * 插入测试用的小说设定
     */
    public static void insertTestSettings(Context context, int storyId) {
        StorySettingDao dao = new StorySettingDao(context);
        
        // === 世界观设定 ===
        
        // 地理环境
        StorySetting geography = new StorySetting(storyId, "世界观", "地理环境", "天穹大陆");
        geography.setSummary("位于星海中央的巨大浮空大陆，面积500万平方公里");
        geography.setDetail("天穹大陆悬浮在无尽星海之上，由神秘的'星核能量'支撑。大陆分为东、西、南、北四个区域，每个区域都有独特的地貌和气候。\n\n东部是繁华的魔法都市群，西部是广袤的沙漠王国，南部是茂密的精灵森林，北部是冰封的巨龙山脉。");
        dao.insert(geography);
        
        // 历史背景
        StorySetting history = new StorySetting(storyId, "世界观", "历史背景", "星陨纪元");
        history.setSummary("三千年前的星陨之战改变了整个世界格局");
        history.setDetail("在遥远的过去，天空中有七颗星辰突然坠落，带来了强大的星核能量。这场灾难被称为'星陨之战'，导致旧世界的毁灭和新纪元的开始。\n\n如今，人们已经学会利用星核能量，建立了以魔法为核心的文明体系。");
        dao.insert(history);
        
        // === 角色设定 ===
        
        // 主角
        StorySetting protagonist = new StorySetting(storyId, "角色", "主要角色", "林星辰");
        protagonist.setSummary("18岁少年，拥有罕见的双系魔法天赋");
        String characterAttrs = "{" +
            "\"role_type\": \"protagonist\"," +
            "\"age\": 18," +
            "\"gender\": \"male\"," +
            "\"personality_traits\": [\"冷静\", \"机智\", \"坚韧\"]," +
            "\"abilities\": [\"火系魔法\", \"冰系魔法\"]," +
            "\"background\": \"孤儿，被老魔法师收养\"" +
            "}";
        protagonist.setSpecificAttributes(characterAttrs);
        protagonist.setDetail("林星辰是天穹大陆上罕见的双系魔法师，同时掌握火系和冰系魔法。他性格冷静沉着，面对困境总能找到突破口。\n\n从小在魔法学院长大，师从传奇法师梅林。虽然身世成谜，但他坚信自己肩负着某种使命。");
        dao.insert(protagonist);
        
        // 配角
        StorySetting supporting = new StorySetting(storyId, "角色", "次要角色", "艾莉娅");
        supporting.setSummary("精灵族公主，精通自然魔法");
        String supportingAttrs = "{" +
            "\"role_type\": \"supporting\"," +
            "\"age\": 120," +
            "\"race\": \"elf\"," +
            "\"personality_traits\": [\"温柔\", \"善良\", \"固执\"]" +
            "}";
        supporting.setSpecificAttributes(supportingAttrs);
        supporting.setDetail("艾莉娅是南方精灵森林的公主，虽然外表看起来只有16岁，但实际年龄已达120岁。她精通自然魔法，能够与植物和动物沟通。\n\n在一次意外中遇到了林星辰，从此成为他的忠实伙伴。");
        dao.insert(supporting);
        
        // === 剧情设定 ===
        
        // 主线任务
        StorySetting mainPlot = new StorySetting(storyId, "剧情", "主线任务", "寻找失落的星核");
        mainPlot.setSummary("集齐七块星核碎片，阻止虚空入侵");
        String plotAttrs = "{" +
            "\"plot_type\": \"main_quest\"," +
            "\"urgency\": \"high\"," +
            "\"estimated_chapters\": 50" +
            "}";
        mainPlot.setSpecificAttributes(plotAttrs);
        mainPlot.setDetail("传说七颗坠落的星辰留下了七块星核碎片，散落在大陆各处。只有集齐所有碎片，才能打开通往星界的通道，获得对抗虚空的力量。\n\n目前已知三块碎片的位置：东方魔法塔、西方沙漠遗迹、北方巨龙巢穴。");
        dao.insert(mainPlot);
        
        // 悬念伏笔
        StorySetting suspense = new StorySetting(storyId, "剧情", "悬念伏笔", "林星辰的身世之谜");
        suspense.setSummary("林星辰的真实身份可能与星陨之战有关");
        String suspenseAttrs = "{" +
            "\"plot_type\": \"suspense\"," +
            "\"suspense_status\": \"pending\"," +
            "\"suspense_intensity\": 9," +
            "\"setup_chapter_id\": 3," +
            "\"resolve_chapter_id\": -1" +
            "}";
        suspense.setSpecificAttributes(suspenseAttrs);
        suspense.setDetail("在第3章中，老魔法师临终前透露林星辰并非普通人，而是'星之子'的后裔。这个秘密一直困扰着林星辰，他怀疑自己的身世与三千年前的星陨之战有直接关系。\n\n随着故事推进，越来越多的线索指向一个惊人的真相...");
        dao.insert(suspense);
        
        // === 风格设定 ===
        
        StorySetting style = new StorySetting(storyId, "风格", "叙事风格", "史诗奇幻风格");
        style.setSummary("宏大叙事，注重世界观构建和人物成长");
        String styleAttrs = "{" +
            "\"tone\": \"epic\"," +
            "\"pacing\": \"moderate\"," +
            "\"focus_areas\": [\"world_building\", \"character_development\"]" +
            "}";
        style.setSpecificAttributes(styleAttrs);
        style.setDetail("本作采用史诗奇幻的叙事风格，注重宏大的世界观构建和人物的内心成长。情节推进节奏适中，给读者足够的时间沉浸在故事中。\n\n语言风格偏向华丽但不失简洁，对话要符合人物性格和身份。");
        dao.insert(style);
        
        // === 规则设定 ===
        
        StorySetting magicRule = new StorySetting(storyId, "规则", "魔法规则", "双系魔法限制");
        magicRule.setSummary("同时使用两种相克魔法会产生反噬");
        String ruleAttrs = "{" +
            "\"rule_type\": \"magic_system\"," +
            "\"severity\": \"critical\"," +
            "\"exceptions\": [\"星之子血脉\"]" +
            "}";
        magicRule.setSpecificAttributes(ruleAttrs);
        magicRule.setDetail("在天穹大陆的魔法体系中，同时操控两种相克的魔法元素（如火与冰）极其危险。普通魔法师强行尝试会导致魔力反噬，轻则重伤，重则死亡。\n\n然而，传说中的'星之子'血脉拥有者似乎不受此限制，这正是林星辰的特殊之处。");
        dao.insert(magicRule);
    }
    
    /**
     * 清除某小说的所有测试数据
     */
    public static void clearTestSettings(Context context, int storyId) {
        StorySettingDao dao = new StorySettingDao(context);
        dao.deleteByStoryId(storyId);
    }
}
