package com.example.storyteller.data.remote;

import android.content.Context;
import android.text.TextUtils;

import com.example.storyteller.model.Material;
import com.example.storyteller.model.NovelSummary;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 素材提取器 - 新版分类体系
 * 支持5大分类：世界观、角色、剧情、风格、规则
 */
public class MaterialCandidateExtractor {

    // 新版5大分类类型标识
    public static final String TYPE_WORLDVIEW = "worldview";   // 世界观
    public static final String TYPE_CHARACTER = "character";   // 角色
    public static final String TYPE_PLOT = "plot";             // 剧情
    public static final String TYPE_STYLE = "style";           // 风格
    public static final String TYPE_RULE = "rule";             // 规则
    
    // 旧版类型别名（保持向后兼容，已废弃）
    @Deprecated
    public static final String TYPE_PERSONA = TYPE_CHARACTER;
    @Deprecated
    public static final String TYPE_THEME = TYPE_STYLE;
    
    // 对应的中文分类名（用于显示）
    public static final String CATEGORY_WORLDVIEW = "世界观";
    public static final String CATEGORY_CHARACTER = "角色";
    public static final String CATEGORY_PLOT = "剧情";
    public static final String CATEGORY_STYLE = "风格";
    public static final String CATEGORY_RULE = "规则";
    
    // 旧版分类名别名（保持向后兼容，已废弃）
    @Deprecated
    public static final String CATEGORY_PERSONA = CATEGORY_CHARACTER;
    @Deprecated
    public static final String CATEGORY_THEME = CATEGORY_STYLE;

    private final ApiClient apiClient = ApiClient.getInstance();
    private final Gson gson = new Gson();

    public interface Callback {
        void onSuccess(List<Material> materials, String rawJson);
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
                    List<Material> materials = result.materials.isEmpty()
                            ? buildFallbackMaterials(summary, normalizedTypes)
                            : result.materials;
                    callback.onSuccess(materials, result.rawJson);
                } catch (Exception e) {
                    callback.onSuccess(buildFallbackMaterials(summary, normalizedTypes), responseText);
                }
            }

            @Override
            public void onFailure(Exception e) {
                callback.onSuccess(buildFallbackMaterials(summary, normalizedTypes), "{}");
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
                + "{\"materials\":[{\"type\":\"worldview|character|plot|style|rule\",\"subCategory\":\"子分类\",\"title\":\"标题\",\"summary\":\"一句话总结\",\"detail\":\"详细说明\",\"tags\":[\"标签1\",\"标签2\"],\"confidence\":0.0}]}\n"
                + "要求：\n"
                + "1. 只输出所选类型对应的素材，不要输出其他类型。\n"
                + "2. title 要简短明确，适合素材库展示（20字以内）。\n"
                + "3. subCategory 必须从下方对应类型的子分类中选择最匹配的一个。\n"
                + "4. summary 以创作复用为目标，尽量短句（50字以内）。\n"
                + "5. detail 要说明适用场景、核心特点或使用价值（200字以内）。\n"
                + "6. tags 提供3-5个关键词标签。\n"
                + "7. confidence 取 0 到 1 之间的小数，表示素材质量评分。\n"
                + "8. type 字段与 subCategory 的对应关系：\n"
                + "   - worldview (世界观): 地理环境/历史背景/种族文化/社会制度/科技水平\n"
                + "   - character (角色): 主要角色/次要角色/反派角色/群体角色\n"
                + "   - plot (剧情): 主线任务/支线任务/悬念伏笔/关键事件\n"
                + "   - style (风格): 叙事风格/语言风格/节奏控制/情感基调\n"
                + "   - rule (规则): 魔法规则/战斗系统/经济体系/时间规则\n"
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
        if (root == null || !root.has("materials") || !root.get("materials").isJsonArray()) {
            return new ExtractionResult(new ArrayList<>(), jsonText);
        }

        JsonArray array = root.getAsJsonArray("materials");
        List<Material> materials = new ArrayList<>();
        for (JsonElement element : array) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject obj = element.getAsJsonObject();
            String type = safeGetString(obj, "type", TYPE_CHARACTER);
            String subCategory = safeGetString(obj, "subCategory", null);
            String title = safeGetString(obj, "title", "未命名素材");
            String summaryText = safeGetString(obj, "summary", title);
            String detail = safeGetString(obj, "detail", summaryText);
            double confidence = safeGetDouble(obj, "confidence", 0.5d);
            List<String> tags = readTags(obj);

            if (!requestedTypes.contains(type)) {
                continue;
            }

            String category = mapCategory(type);
            // 如果AI没有返回subCategory，根据type设置默认值
            if (subCategory == null || subCategory.isEmpty()) {
                subCategory = getDefaultSubCategory(type);
            }
            String content = buildContentWithSubCategory(category, subCategory, summaryText, detail, tags);
            Material material = new Material(category, title, content, System.currentTimeMillis(),
                    summary.getSourceUrl(), summary.getTitle(), type, confidence, obj.toString());
            materials.add(material);
        }
        return new ExtractionResult(materials, jsonText);
    }

    private List<Material> buildFallbackMaterials(NovelSummary summary, List<String> requestedTypes) {
        List<Material> materials = new ArrayList<>();
        String title = safe(summary.getTitle());
        String sourceUrl = summary.getSourceUrl();
        long now = System.currentTimeMillis();

        if (requestedTypes.contains(TYPE_WORLDVIEW)) {
            materials.add(new Material(CATEGORY_WORLDVIEW, title + " · 世界观",
                    buildContent(CATEGORY_WORLDVIEW,
                            firstNonEmpty(summary.getDescription(), "包含丰富的世界观设定"),
                            firstNonEmpty(summary.getDescription(), "可用于构建类似的世界观背景"),
                            toTags("世界观", "背景", "设定")),
                    now, sourceUrl, title, TYPE_WORLDVIEW, 0.2d, null));
        }

        if (requestedTypes.contains(TYPE_CHARACTER)) {
            materials.add(new Material(CATEGORY_CHARACTER, title + " · 角色群像",
                    buildContent(CATEGORY_CHARACTER,
                            firstNonEmpty(summary.getCharacterProfiles(), "适合继续分析的人物群像"),
                            firstNonEmpty(summary.getCharacterProfiles(), safe(summary.getDescription())),
                            toTags("人物", "角色", "关系")),
                    now, sourceUrl, title, TYPE_CHARACTER, 0.2d, null));
        }

        if (requestedTypes.contains(TYPE_PLOT)) {
            materials.add(new Material(CATEGORY_PLOT, title + " · 情节脉络",
                    buildContent(CATEGORY_PLOT,
                            firstNonEmpty(summary.getOutline(), "可用于提炼经典情节模板"),
                            firstNonEmpty(summary.getOutline(), safe(summary.getSummary())),
                            toTags("情节", "冲突", "转折")),
                    now, sourceUrl, title, TYPE_PLOT, 0.2d, null));
        }

        if (requestedTypes.contains(TYPE_STYLE)) {
            materials.add(new Material(CATEGORY_STYLE, title + " · 风格特色",
                    buildContent(CATEGORY_STYLE,
                            firstNonEmpty(summary.getSummary(), "具有独特的叙事风格"),
                            firstNonEmpty(summary.getDescription(), safe(summary.getSummary())),
                            toTags("风格", "叙事", "特色")),
                    now, sourceUrl, title, TYPE_STYLE, 0.2d, null));
        }

        if (requestedTypes.contains(TYPE_RULE)) {
            materials.add(new Material(CATEGORY_RULE, title + " · 规则体系",
                    buildContent(CATEGORY_RULE,
                            firstNonEmpty(summary.getSummary(), "包含特定的规则或体系"),
                            firstNonEmpty(summary.getDescription(), "可参考其规则设计"),
                            toTags("规则", "体系", "设定")),
                    now, sourceUrl, title, TYPE_RULE, 0.2d, null));
        }
        
        return materials;
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

    private String getDefaultSubCategory(String type) {
        // 为每种类型提供默认的子分类
        if (TYPE_WORLDVIEW.equalsIgnoreCase(type)) {
            return "地理环境";
        } else if (TYPE_CHARACTER.equalsIgnoreCase(type)) {
            return "主要角色";
        } else if (TYPE_PLOT.equalsIgnoreCase(type)) {
            return "关键事件";
        } else if (TYPE_STYLE.equalsIgnoreCase(type)) {
            return "叙事风格";
        } else if (TYPE_RULE.equalsIgnoreCase(type)) {
            return "魔法规则";
        }
        return "其他";
    }

    private String mapCategory(String type) {
        if (TYPE_WORLDVIEW.equalsIgnoreCase(type)) {
            return CATEGORY_WORLDVIEW;
        }
        if (TYPE_CHARACTER.equalsIgnoreCase(type)) {
            return CATEGORY_CHARACTER;
        }
        if (TYPE_PLOT.equalsIgnoreCase(type)) {
            return CATEGORY_PLOT;
        }
        if (TYPE_STYLE.equalsIgnoreCase(type)) {
            return CATEGORY_STYLE;
        }
        if (TYPE_RULE.equalsIgnoreCase(type)) {
            return CATEGORY_RULE;
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
            if (TYPE_WORLDVIEW.equals(type)) {
                labels.add("世界观");
            } else if (TYPE_CHARACTER.equals(type)) {
                labels.add("角色");
            } else if (TYPE_PLOT.equals(type)) {
                labels.add("剧情");
            } else if (TYPE_STYLE.equals(type)) {
                labels.add("风格");
            } else if (TYPE_RULE.equals(type)) {
                labels.add("规则");
            }
        }
        return String.join("、", labels);
    }

    private List<String> normalizeRequestedTypes(List<String> requestedTypes) {
        List<String> types = new ArrayList<>();
        if (requestedTypes == null || requestedTypes.isEmpty()) {
            // 默认全部类型
            types.add(TYPE_WORLDVIEW);
            types.add(TYPE_CHARACTER);
            types.add(TYPE_PLOT);
            types.add(TYPE_STYLE);
            types.add(TYPE_RULE);
            return types;
        }
        for (String type : requestedTypes) {
            if (TYPE_WORLDVIEW.equals(type) || TYPE_CHARACTER.equals(type) || 
                TYPE_PLOT.equals(type) || TYPE_STYLE.equals(type) || TYPE_RULE.equals(type)) {
                if (!types.contains(type)) {
                    types.add(type);
                }
            }
        }
        if (types.isEmpty()) {
            types.add(TYPE_WORLDVIEW);
            types.add(TYPE_CHARACTER);
            types.add(TYPE_PLOT);
            types.add(TYPE_STYLE);
            types.add(TYPE_RULE);
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
        List<String> tags = new ArrayList<>();
        for (String value : values) {
            tags.add(value);
        }
        return tags;
    }

    private static class ExtractionResult {
        final List<Material> materials;
        final String rawJson;

        ExtractionResult(List<Material> materials, String rawJson) {
            this.materials = materials;
            this.rawJson = rawJson;
        }
    }
}


