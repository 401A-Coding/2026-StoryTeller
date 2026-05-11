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
 * 将小说摘要结构化识别为“人物 / 情节 / 主题”三类素材候选。
 */
public class MaterialCandidateExtractor {

    public static final String TYPE_PERSONA = "persona";
    public static final String TYPE_PLOT = "plot";
    public static final String TYPE_THEME = "theme";

    public static final String CATEGORY_PERSONA = "人物素材";
    public static final String CATEGORY_PLOT = "情节素材";
    public static final String CATEGORY_THEME = "主题素材";

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
        return "你是小说素材抽取助手。请基于下面的小说摘要，提炼出适合写作复用的素材候选，严格只输出 JSON，不要 Markdown，不要解释。\n"
                + "必须只抽取以下类型：" + requestedTypeText + "。\n"
                + "输出格式：\n"
                + "{\"materials\":[{\"type\":\"persona|plot|theme\",\"title\":\"标题\",\"summary\":\"一句话总结\",\"detail\":\"详细说明\",\"tags\":[\"标签1\",\"标签2\"],\"confidence\":0.0}]}\n"
                + "要求：\n"
                + "1. 只输出所选类型对应的素材，不要输出其他类型。\n"
                + "2. title 要简短明确，适合素材库展示。\n"
                + "3. summary 以创作复用为目标，尽量短句。\n"
                + "4. detail 要说明适用场景、核心冲突或使用价值。\n"
                + "5. confidence 取 0 到 1 之间的小数。\n"
                + "\n小说信息：\n"
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
            String type = safeGetString(obj, "type", TYPE_PERSONA);
            String title = safeGetString(obj, "title", "未命名素材");
            String summaryText = safeGetString(obj, "summary", title);
            String detail = safeGetString(obj, "detail", summaryText);
            double confidence = safeGetDouble(obj, "confidence", 0.5d);
            List<String> tags = readTags(obj);

            if (!requestedTypes.contains(type)) {
                continue;
            }

            String category = mapCategory(type);
            String content = buildContent(category, summaryText, detail, tags);
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

        if (requestedTypes.contains(TYPE_PERSONA)) {
            materials.add(new Material(CATEGORY_PERSONA, title + " · 人物原型",
                    buildContent(CATEGORY_PERSONA,
                            firstNonEmpty(summary.getSummary(), "适合继续分析的人物群像"),
                            firstNonEmpty(summary.getCharacterProfiles(), safe(summary.getDescription())),
                            toTags("人物", "角色", "关系")),
                    now, sourceUrl, title, TYPE_PERSONA, 0.2d, null));
        }

        if (requestedTypes.contains(TYPE_PLOT)) {
            materials.add(new Material(CATEGORY_PLOT, title + " · 情节脉络",
                    buildContent(CATEGORY_PLOT,
                            firstNonEmpty(summary.getOutline(), "可用于提炼经典情节模板"),
                            firstNonEmpty(summary.getOutline(), safe(summary.getSummary())),
                            toTags("情节", "冲突", "转折")),
                    now, sourceUrl, title, TYPE_PLOT, 0.2d, null));
        }

        if (requestedTypes.contains(TYPE_THEME)) {
            materials.add(new Material(CATEGORY_THEME, title + " · 主题母题",
                    buildContent(CATEGORY_THEME,
                            firstNonEmpty(summary.getSummary(), "可用于提炼主题表达"),
                            firstNonEmpty(summary.getDescription(), safe(summary.getSummary())),
                            toTags("主题", "情感", "价值")),
                    now, sourceUrl, title, TYPE_THEME, 0.2d, null));
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

    private String mapCategory(String type) {
        if (TYPE_PLOT.equalsIgnoreCase(type)) {
            return CATEGORY_PLOT;
        }
        if (TYPE_THEME.equalsIgnoreCase(type)) {
            return CATEGORY_THEME;
        }
        return CATEGORY_PERSONA;
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
            if (TYPE_PERSONA.equals(type)) {
                labels.add("人物形象");
            } else if (TYPE_PLOT.equals(type)) {
                labels.add("经典情节");
            } else if (TYPE_THEME.equals(type)) {
                labels.add("经典主题");
            }
        }
        return String.join("、", labels);
    }

    private List<String> normalizeRequestedTypes(List<String> requestedTypes) {
        List<String> types = new ArrayList<>();
        if (requestedTypes == null || requestedTypes.isEmpty()) {
            types.add(TYPE_PERSONA);
            types.add(TYPE_PLOT);
            types.add(TYPE_THEME);
            return types;
        }
        for (String type : requestedTypes) {
            if (TYPE_PERSONA.equals(type) || TYPE_PLOT.equals(type) || TYPE_THEME.equals(type)) {
                if (!types.contains(type)) {
                    types.add(type);
                }
            }
        }
        if (types.isEmpty()) {
            types.add(TYPE_PERSONA);
            types.add(TYPE_PLOT);
            types.add(TYPE_THEME);
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


