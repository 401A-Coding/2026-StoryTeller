package com.example.storyteller.data.remote;

import android.content.Context;
import android.text.TextUtils;

import com.example.storyteller.model.StorySetting;
import com.example.storyteller.model.NovelSummary;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 素材提取器 - 新版分类体系
 * 支持6大分类：世界、角色、地点、剧情、规则体系、创作控制
 */
public class MaterialCandidateExtractor {

    // 新版7大分类类型标识
    public static final String TYPE_WORLD = "world";           // 世界
    public static final String TYPE_CHARACTER = "character";   // 角色
    public static final String TYPE_LOCATION = "location";     // 地点
    public static final String TYPE_PLOT = "plot";             // 剧情
    public static final String TYPE_SYSTEM = "system";         // 规则体系
    public static final String TYPE_CREATIVE_CONTROL = "creative_control"; // 创作控制
    public static final String TYPE_META = "meta";             // 元信息（可选，用于内部追踪）

    // 旧版类型别名（保持向后兼容，已废弃）
    @Deprecated
    public static final String TYPE_PERSONA = TYPE_CHARACTER;
    @Deprecated
    public static final String TYPE_THEME = TYPE_CREATIVE_CONTROL;
    @Deprecated
    public static final String TYPE_STYLE = TYPE_CREATIVE_CONTROL;
    @Deprecated
    public static final String TYPE_RULE = TYPE_SYSTEM;
    
    // 对应的中文分类名（用于显示）
    public static final String CATEGORY_WORLD = "世界";
    public static final String CATEGORY_CHARACTER = "角色";
    public static final String CATEGORY_LOCATION = "地点";
    public static final String CATEGORY_PLOT = "剧情";
    public static final String CATEGORY_SYSTEM = "规则体系";
    public static final String CATEGORY_CREATIVE_CONTROL = "创作控制";

    // 旧版分类名别名（保持向后兼容，已废弃）
    @Deprecated
    public static final String CATEGORY_WORLDVIEW = CATEGORY_WORLD;  // "世界观" -> "世界"
    @Deprecated
    public static final String CATEGORY_PERSONA = CATEGORY_CHARACTER;
    @Deprecated
    public static final String CATEGORY_RULE = CATEGORY_SYSTEM;
    @Deprecated
    public static final String CATEGORY_STYLE = CATEGORY_CREATIVE_CONTROL;
    @Deprecated
    public static final String CATEGORY_THEME = CATEGORY_CREATIVE_CONTROL;

    private final ApiClient apiClient = ApiClient.getInstance();
    private final Gson gson = new Gson();

    public interface Callback {
        void onSuccess(List<StorySetting> settings, String rawJson);
        void onFailure(Exception e);
    }

    public void extract(NovelSummary summary, Context context, List<String> requestedTypes, Callback callback) {
        if (summary == null) {
            callback.onFailure(new IllegalArgumentException("summary is null"));
            return;
        }

        List<String> normalizedTypes = normalizeRequestedTypes(requestedTypes);
        String prompt = buildPrompt(summary, normalizedTypes);
        apiClient.generateStory(prompt, context, new ApiClient.Callback() {
            @Override
            public void onSuccess(String responseText) {
                try {
                    ExtractionResult result = parseResponse(summary, responseText, normalizedTypes);
                    List<StorySetting> settings = result.settings.isEmpty()
                            ? buildFallbackSettings(summary, normalizedTypes)
                            : result.settings;
                    callback.onSuccess(settings, result.rawJson);
                } catch (Exception e) {
                    callback.onSuccess(buildFallbackSettings(summary, normalizedTypes), responseText);
                }
            }

            @Override
            public void onFailure(Exception e) {
                callback.onSuccess(buildFallbackSettings(summary, normalizedTypes), "{}");
            }
        });
    }

    public void extract(NovelSummary summary, Context context, Callback callback) {
        extract(summary, context, null, callback);
    }

    private String buildPrompt(NovelSummary summary, List<String> requestedTypes) {
        String requestedTypeText = buildRequestedTypeText(requestedTypes);
        return "你是小说素材抽取助手。请基于下面的内容，提炼出适合写作复用的素材候选，严格只输出 JSON，不要 Markdown，不要解释。\n"
                + "必须只抽取以下类型：" + requestedTypeText + "。\n"
                + "输出格式：\n"
                + "{\"settings\":[{\"category\":\"世界|角色|地点|剧情|规则体系|创作控制\",\"subCategory\":\"子分类\",\"title\":\"标题\",\"summary\":\"一句话总结\",\"detail\":\"详细说明\",\"tags\":[\"标签1\",\"标签2\"],\"confidence\":0.0}]}\n"
                + "要求：\n"
                + "1. 只输出所选类型对应的素材，不要输出其他类型。\n"
                + "2. title 要简短明确，适合素材库展示（20字以内）。\n"
                + "3. subCategory 必须从下方对应类型的子分类中选择最匹配的一个。\n"
                + "4. summary 以创作复用为目标，尽量短句（50字以内）。\n"
                + "5. detail 要说明适用场景、核心特点或使用价值（200字以内）。\n"
                + "6. tags 提供3-5个关键词标签。\n"
                + "7. confidence 取 0 到 1 之间的小数，表示素材质量评分。\n"
                + "8. category 与 subCategory 的对应关系：\n"
                + "   - 世界: 地理环境/时代背景/历史背景/文明种族/社会文化/政治势力/科技发展/物品资源\n"
                + "   - 角色: 主要角色/次要角色/反派角色/组织阵营\n"
                + "   - 地点: 国家地区/城市/村庄/自然景观/关键场景/建筑设施/特殊空间\n"
                + "   - 剧情: 主线剧情/支线剧情/关键事件/悬念伏笔/章节规划/矛盾冲突/时间线\n"
                + "   - 规则体系: 力量体系/魔法或超能力/战斗系统/经济体系/时间规则/限制条件\n"
                + "   - 创作控制: 主题内核/语言风格/情感基调/叙事视角/节奏控制\n"
                + "\n内容信息：\n"
                + "标题：" + safe(summary.getTitle()) + "\n"
                + "作者：" + safe(summary.getAuthor()) + "\n"
                + "简介：" + safe(summary.getDescription()) + "\n"
                + "大纲：" + safe(summary.getOutline()) + "\n"
                + "总结：" + safe(summary.getSummary()) + "\n"
                + "人物：" + (summary.getCharacters() == null ? "" : String.join("、", summary.getCharacters())) + "\n"
                + "人物画像：" + safe(summary.getCharacterProfiles()) + "\n";
    }

    private ExtractionResult parseResponse(NovelSummary summary, String responseText, List<String> requestedTypes) {
        String jsonText = extractJson(responseText);
        JsonObject root = gson.fromJson(jsonText, JsonObject.class);
        if (root == null || !root.has("settings") || !root.get("settings").isJsonArray()) {
            return new ExtractionResult(new ArrayList<>(), jsonText);
        }

        JsonArray array = root.getAsJsonArray("settings");
        List<StorySetting> settings = new ArrayList<>();
        for (JsonElement element : array) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject obj = element.getAsJsonObject();
            String category = safeGetString(obj, "category", "角色");
            String subCategory = safeGetString(obj, "subCategory", null);
            String title = safeGetString(obj, "title", "未命名素材");
            String summaryText = safeGetString(obj, "summary", title);
            String detail = safeGetString(obj, "detail", summaryText);
            double confidence = safeGetDouble(obj, "confidence", 0.5d);
            List<String> tags = readTags(obj);

            // 验证category是否有效
            if (!isValidCategory(category)) {
                continue;
            }

            // 如果AI没有返回subCategory，根据category设置默认值
            if (subCategory == null || subCategory.isEmpty()) {
                subCategory = getDefaultSubCategory(category);
            }

            // 创建StorySetting对象
            StorySetting setting = new StorySetting(0, category, subCategory, title);
            setting.setSummary(summaryText);
            setting.setDetail(detail);
            
            // 设置tags为JSON数组字符串
            if (tags != null && !tags.isEmpty()) {
                setting.setTags(gson.toJson(tags));
            }
            
            // 设置来源信息
            setting.setSourceUrl(summary.getSourceUrl());
            setting.setSourceTitle(summary.getTitle());
            setting.setSourceType("ai_generated");
            setting.setAiConfidence(confidence);
            setting.setRawJson(obj.toString());
            
            settings.add(setting);
        }
        return new ExtractionResult(settings, jsonText);
    }

    private List<StorySetting> buildFallbackSettings(NovelSummary summary, List<String> requestedTypes) {
        List<StorySetting> settings = new ArrayList<>();
        String title = safe(summary.getTitle());
        String sourceUrl = summary.getSourceUrl();
        long now = System.currentTimeMillis();

        if (requestedTypes.contains(CATEGORY_WORLD)) {
            StorySetting setting = new StorySetting(0, CATEGORY_WORLD, "地理环境", title + " · 世界设定");
            setting.setSummary(firstNonEmpty(summary.getDescription(), "包含丰富的世界设定"));
            setting.setDetail(firstNonEmpty(summary.getDescription(), "可用于构建类似的世界背景"));
            setting.setTags(gson.toJson(toTags("世界", "背景", "设定")));
            setting.setSourceUrl(sourceUrl);
            setting.setSourceTitle(title);
            setting.setSourceType("ai_generated");
            setting.setAiConfidence(0.2d);
            settings.add(setting);
        }

        if (requestedTypes.contains(CATEGORY_CHARACTER)) {
            StorySetting setting = new StorySetting(0, CATEGORY_CHARACTER, "主要角色", title + " · 角色群像");
            setting.setSummary(firstNonEmpty(summary.getCharacterProfiles(), "适合继续分析的人物群像"));
            setting.setDetail(firstNonEmpty(summary.getCharacterProfiles(), safe(summary.getDescription())));
            setting.setTags(gson.toJson(toTags("人物", "角色", "关系")));
            setting.setSourceUrl(sourceUrl);
            setting.setSourceTitle(title);
            setting.setSourceType("ai_generated");
            setting.setAiConfidence(0.2d);
            settings.add(setting);
        }

        if (requestedTypes.contains(CATEGORY_LOCATION)) {
            StorySetting setting = new StorySetting(0, CATEGORY_LOCATION, "关键场景", title + " · 地点设定");
            setting.setSummary(firstNonEmpty(summary.getDescription(), "包含重要的地点或场景设定"));
            setting.setDetail(firstNonEmpty(summary.getDescription(), "可用于构建类似的地点或场景"));
            setting.setTags(gson.toJson(toTags("地点", "环境", "场景")));
            setting.setSourceUrl(sourceUrl);
            setting.setSourceTitle(title);
            setting.setSourceType("ai_generated");
            setting.setAiConfidence(0.2d);
            settings.add(setting);
        }

        if (requestedTypes.contains(CATEGORY_PLOT)) {
            StorySetting setting = new StorySetting(0, CATEGORY_PLOT, "关键事件", title + " · 情节脉络");
            setting.setSummary(firstNonEmpty(summary.getOutline(), "可用于提炼经典情节模板"));
            setting.setDetail(firstNonEmpty(summary.getOutline(), safe(summary.getSummary())));
            setting.setTags(gson.toJson(toTags("情节", "冲突", "转折")));
            setting.setSourceUrl(sourceUrl);
            setting.setSourceTitle(title);
            setting.setSourceType("ai_generated");
            setting.setAiConfidence(0.2d);
            settings.add(setting);
        }

        if (requestedTypes.contains(CATEGORY_SYSTEM)) {
            StorySetting setting = new StorySetting(0, CATEGORY_SYSTEM, "力量体系", title + " · 规则设定");
            setting.setSummary(firstNonEmpty(summary.getDescription(), "包含特定的规则或体系"));
            setting.setDetail(firstNonEmpty(summary.getDescription(), "可参考其规则设计"));
            setting.setTags(gson.toJson(toTags("规则", "体系", "设定")));
            setting.setSourceUrl(sourceUrl);
            setting.setSourceTitle(title);
            setting.setSourceType("ai_generated");
            setting.setAiConfidence(0.2d);
            settings.add(setting);
        }

        if (requestedTypes.contains(CATEGORY_CREATIVE_CONTROL)) {
            StorySetting setting = new StorySetting(0, CATEGORY_CREATIVE_CONTROL, "主题内核", title + " · 创作控制");
            setting.setSummary(firstNonEmpty(summary.getDescription(), "包含独特的主题或风格控制"));
            setting.setDetail(firstNonEmpty(summary.getDescription(), "可用于构建类似的主题表达"));
            setting.setTags(gson.toJson(toTags("创作控制", "主题", "风格")));
            setting.setSourceUrl(sourceUrl);
            setting.setSourceTitle(title);
            setting.setSourceType("ai_generated");
            setting.setAiConfidence(0.2d);
            settings.add(setting);
        }

        return settings;
    }

    private String buildContent(String category, String summary, String detail, List<String> tags) {
        StringBuilder builder = new StringBuilder();
        builder.append("【分类】").append(category).append("\n");
        builder.append("【一句话】").append(summary).append("\n");
        builder.append("【详细说明】").append(detail).append("\n");
        if (tags != null && !tags.isEmpty()) {
            builder.append("【标签】").append(String.join("、", tags)).append("\n");
        }
        return builder.toString();
    }

    private String buildContentWithSubCategory(String category, String subCategory, String summary, String detail, List<String> tags) {
        StringBuilder builder = new StringBuilder();
        builder.append("【分类】").append(category).append("\n");
        builder.append("【子分类】").append(subCategory).append("\n");
        builder.append("【一句话】").append(summary).append("\n");
        builder.append("【详细说明】").append(detail).append("\n");
        if (tags != null && !tags.isEmpty()) {
            builder.append("【标签】").append(String.join("、", tags)).append("\n");
        }
        return builder.toString();
    }

    private boolean isValidCategory(String category) {
        return CATEGORY_WORLD.equals(category)
                || CATEGORY_CHARACTER.equals(category)
                || CATEGORY_LOCATION.equals(category)
                || CATEGORY_PLOT.equals(category)
                || CATEGORY_SYSTEM.equals(category)
                || CATEGORY_CREATIVE_CONTROL.equals(category);
    }

    private String getDefaultSubCategory(String category) {
        // 为每种分类提供默认的子分类
        if (CATEGORY_WORLD.equals(category)) {
            return "地理环境";
        } else if (CATEGORY_CHARACTER.equals(category)) {
            return "主要角色";
        } else if (CATEGORY_LOCATION.equals(category)) {
            return "国家地区";
        } else if (CATEGORY_PLOT.equals(category)) {
            return "关键事件";
        } else if (CATEGORY_SYSTEM.equals(category)) {
            return "力量体系";
        } else if (CATEGORY_CREATIVE_CONTROL.equals(category)) {
            return "主题内核";
        }
        return "其他";
    }

    private String mapCategory(String type) {
        if (TYPE_WORLD.equalsIgnoreCase(type)) {
            return CATEGORY_WORLD;
        }
        if (TYPE_CHARACTER.equalsIgnoreCase(type)) {
            return CATEGORY_CHARACTER;
        }
        if (TYPE_LOCATION.equalsIgnoreCase(type)) {
            return CATEGORY_LOCATION;
        }
        if (TYPE_PLOT.equalsIgnoreCase(type)) {
            return CATEGORY_PLOT;
        }
        if (TYPE_SYSTEM.equalsIgnoreCase(type)) {
            return CATEGORY_SYSTEM;
        }
        if (TYPE_CREATIVE_CONTROL.equalsIgnoreCase(type)) {
            return CATEGORY_CREATIVE_CONTROL;
        }
        // 默认返回角色
        return CATEGORY_CHARACTER;
    }

    private String extractJson(String text) {
        if (TextUtils.isEmpty(text)) {
            return "{}";
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return text;
    }

    private List<String> readTags(JsonObject obj) {
        List<String> tags = new ArrayList<>();
        if (obj.has("tags") && obj.get("tags").isJsonArray()) {
            JsonArray array = obj.getAsJsonArray("tags");
            for (JsonElement element : array) {
                if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
                    String tag = element.getAsString().trim();
                    if (!TextUtils.isEmpty(tag)) {
                        tags.add(tag);
                    }
                }
            }
        }
        return tags;
    }

    private String safeGetString(JsonObject obj, String key, String defaultValue) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) {
            return defaultValue;
        }
        try {
            return obj.get(key).getAsString();
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private double safeGetDouble(JsonObject obj, String key, double defaultValue) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) {
            return defaultValue;
        }
        try {
            return obj.get(key).getAsDouble();
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private String safe(String text) {
        return text == null ? "" : text.trim();
    }

    private String buildRequestedTypeText(List<String> requestedTypes) {
        List<String> labels = new ArrayList<>();
        for (String type : requestedTypes) {
            if (TYPE_WORLD.equals(type)) {
                labels.add("世界");
            } else if (TYPE_CHARACTER.equals(type)) {
                labels.add("角色");
            } else if (TYPE_LOCATION.equals(type)) {
                labels.add("地点");
            } else if (TYPE_PLOT.equals(type)) {
                labels.add("剧情");
            } else if (TYPE_SYSTEM.equals(type)) {
                labels.add("规则体系");
            } else if (TYPE_CREATIVE_CONTROL.equals(type)) {
                labels.add("创作控制");
            }
        }
        return String.join("、", labels);
    }

    private List<String> normalizeRequestedTypes(List<String> requestedTypes) {
        List<String> types = new ArrayList<>();
        if (requestedTypes == null || requestedTypes.isEmpty()) {
            // 默认全部类型
            types.add(TYPE_WORLD);
            types.add(TYPE_CHARACTER);
            types.add(TYPE_LOCATION);
            types.add(TYPE_PLOT);
            types.add(TYPE_SYSTEM);
            types.add(TYPE_CREATIVE_CONTROL);
            return types;
        }
        for (String type : requestedTypes) {
            if (TYPE_WORLD.equals(type) || TYPE_CHARACTER.equals(type) || TYPE_LOCATION.equals(type) ||
                TYPE_PLOT.equals(type) || TYPE_SYSTEM.equals(type) || TYPE_CREATIVE_CONTROL.equals(type)) {
                if (!types.contains(type)) {
                    types.add(type);
                }
            }
        }
        if (types.isEmpty()) {
            types.add(TYPE_WORLD);
            types.add(TYPE_CHARACTER);
            types.add(TYPE_LOCATION);
            types.add(TYPE_PLOT);
            types.add(TYPE_SYSTEM);
            types.add(TYPE_CREATIVE_CONTROL);
        }
        return types;
    }

    private String firstNonEmpty(String a, String b) {
        if (!TextUtils.isEmpty(a)) {
            return a;
        }
        return b;
    }

    private List<String> toTags(String... values) {
        return new ArrayList<>(Arrays.asList(values));
    }

    private static class ExtractionResult {
        final List<StorySetting> settings;
        final String rawJson;

        ExtractionResult(List<StorySetting> settings, String rawJson) {
            this.settings = settings;
            this.rawJson = rawJson;
        }
    }
}


