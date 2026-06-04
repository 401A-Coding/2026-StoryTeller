package com.example.storyteller.utils;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.example.storyteller.data.local.db.StoryDao;
import com.example.storyteller.data.local.db.StorySettingDao;
import com.example.storyteller.data.remote.ApiClient;
import com.example.storyteller.data.remote.ModelConfig;
import com.example.storyteller.model.RelationExtractionResult;
import com.example.storyteller.model.Story;
import com.example.storyteller.model.StorySetting;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import com.example.storyteller.data.local.db.SettingRelationshipDao;
import com.example.storyteller.model.SettingRelationship;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * AI 关系提取器
 * 从小说的大纲、正文和现有设定中提取设定实体之间的关系
 */
public class RelationExtractor {

    private static final String TAG = "RelationExtractor";
    
    private final Context context;
    private final int storyId;
    private final StorySettingDao settingDao;
    private final SettingRelationshipDao relationshipDao;
    private final StoryDao storyDao;
    private final Gson gson;
    private final ApiClient apiClient;
    private final PromptManager promptManager;

    // 回调接口
    public interface ExtractCallback {
        void onStart();
        void onSuccess(RelationExtractionResult result);
        void onError(String error);
    }

    public RelationExtractor(Context context, int storyId) {
        this.context = context;
        this.storyId = storyId;
        this.settingDao = new StorySettingDao(context);
        this.relationshipDao = new SettingRelationshipDao(context);
        this.storyDao = new StoryDao(context);
        this.apiClient = ApiClient.getInstance();
        this.promptManager = new PromptManager(context);
        this.gson = new Gson();
    }

    /**
     * 执行关系提取
     */
    public void extract(ExtractCallback callback) {
        callback.onStart();

        // 1. 构建上下文变量
        final Map<String, Object> variables = buildContextVariables();
        
        // 2. 使用 PromptManager 加载提示词并替换变量
        final String systemPrompt = promptManager.getTaskPrompt("extract_relations", variables);
        Log.d(TAG, "加载的提示词(前500字符): " + (systemPrompt != null ? systemPrompt.substring(0, Math.min(500, systemPrompt.length())) : "null") + "...");
        
        // 3. 使用 ApiClient 发送请求（提示词已包含完整上下文）
        ApiClient.RequestOptions options = new ApiClient.RequestOptions()
            .setMaxTokens(4000)
            .setTemperature(0.3);
        
        apiClient.generateStory(
            systemPrompt,
            ModelConfig.DEFAULT_MODEL,
            context,
            options,
            new ApiClient.Callback() {
                @Override
                public void onSuccess(String content) {
                    Log.d(TAG, "AI返回内容: " + content);
                    try {
                        RelationExtractionResult result = parseAiResponse(content, storyId);
                        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                            callback.onSuccess(result);
                        });
                    } catch (Exception e) {
                        Log.e(TAG, "解析失败: " + e.getMessage(), e);
                        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                            callback.onError("解析结果失败，请查看日志");
                        });
                    }
                }
                
                @Override
                public void onFailure(Exception e) {
                    Log.e(TAG, "API请求失败: " + e.getMessage(), e);
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        callback.onError("API请求失败，请查看日志");
                    });
                }
            }
        );
    }

    /**
     * 构建上下文变量（用于 PromptManager 模板替换）
     */
    private Map<String, Object> buildContextVariables() {
        Map<String, Object> variables = new HashMap<>();
        
        // 构建 story_context
        variables.put("story_context", buildStoryContext());
        
        return variables;
    }
    
    /**
     * 构建故事上下文字符串
     */
    private String buildStoryContext() {
        StringBuilder sb = new StringBuilder();
        
        Story story = storyDao.getStoryById(storyId);
        
        // 小说信息
        sb.append("# 小说信息\n");
        sb.append("标题: ").append(story != null && story.getTitle() != null ? story.getTitle() : "").append("\n");
        sb.append("简介: ").append(story != null && story.getDescription() != null ? story.getDescription() : "").append("\n\n");
        
        // 现有设定
        sb.append("# 现有设定列表\n");
        List<StorySetting> settings = settingDao.getByStoryId(storyId);
        if (settings != null && !settings.isEmpty()) {
            for (StorySetting setting : settings) {
                sb.append("- ").append(setting.getTitle())
                  .append(" (").append(setting.getCategory()).append(")")
                  .append("\n");
            }
        } else {
            sb.append("(暂无设定)\n");
        }
        sb.append("\n");
        
        // 现有关系（用于去重）
        sb.append("# 现有关系列表\n");
        List<SettingRelationship> existingRelations = relationshipDao.getByStoryId(storyId);
        if (existingRelations != null && !existingRelations.isEmpty()) {
            Set<String> shownRelations = new HashSet<>();
            for (SettingRelationship rel : existingRelations) {
                // 使用标题展示关系
                String sourceTitle = rel.getSourceSettingTitle() != null ? rel.getSourceSettingTitle() : "ID:" + rel.getSourceSettingId();
                String targetTitle = rel.getTargetSettingTitle() != null ? rel.getTargetSettingTitle() : "ID:" + rel.getTargetSettingId();
                String key = sourceTitle + "||" + rel.getRelationshipType() + "||" + targetTitle;
                if (!shownRelations.contains(key)) {
                    String directed = rel.isDirected() ? " → " : " ↔ ";
                    sb.append("- ").append(sourceTitle).append(directed).append(targetTitle)
                      .append(" (").append(rel.getRelationshipType()).append(")")
                      .append("\n");
                    shownRelations.add(key);
                }
            }
        } else {
            sb.append("(暂无关系)\n");
        }
        sb.append("\n");
        
        // 正文
        sb.append("# 正文片段（等距采样·卷章标签）\n");
        String content = story != null ? story.getContent() : "";
        if (!TextUtils.isEmpty(content)) {
            String[] rawParagraphs = content.split("\n\n");
            // 采样配置：提升覆盖范围与单段深度，采用等距采样避免仅截取开头
            int maxParagraphs = 50;          // 从 15 提升至 50 段
            int maxCharPerParagraph = 800;  // 从 500 提升至 800 字符
            int minParagraphLength = 20;     // 过滤短段落阈值

            // 第一遍：解析卷章结构，过滤出有效正文段落，建立卷章映射
            // 每个元素：{正文, 卷标题, 章标题, 正文序号(从0开始)}
            String currentVolume = "";    // 如 "# 第1卷 楔子"
            String currentChapter = "";   // 如 "## 第1章 穿越"
            java.util.List<Object[]> contentList = new java.util.ArrayList<>();
            int contentIdx = 0;
            for (int i = 0; i < rawParagraphs.length; i++) {
                String para = rawParagraphs[i].trim();
                if (para.startsWith("# ") && !para.startsWith("## ")) {
                    // 卷标题：更新当前卷
                    currentVolume = para;
                } else if (para.startsWith("## ")) {
                    // 章标题：更新当前章
                    currentChapter = para;
                } else if (para.length() > minParagraphLength) {
                    // 有效正文段落
                    contentList.add(new Object[]{para, currentVolume, currentChapter, contentIdx});
                    contentIdx++;
                }
            }

            int validCount = contentList.size();
            if (validCount == 0) {
                sb.append("(暂无有效正文内容)\n");
            } else {
                // 决定本次输出哪些段落下标
                int[] sampleIndices;
                if (validCount <= maxParagraphs) {
                    // 有效段落数不足，全输出
                    sampleIndices = new int[validCount];
                    for (int i = 0; i < validCount; i++) sampleIndices[i] = i;
                } else {
                    // 等距采样：保证恰好采样到 maxParagraphs 个有效段落
                    int step = validCount / maxParagraphs;
                    sampleIndices = new int[maxParagraphs];
                    for (int i = 0; i < maxParagraphs; i++) sampleIndices[i] = i * step;
                }
                // 统一输出：附加卷章标签
                for (int sampleIdx : sampleIndices) {
                    Object[] item = contentList.get(sampleIdx);
                    String para = (String) item[0];
                    String vol = (String) item[1];
                    String chap = (String) item[2];
                    int idx = (int) item[3];
                    sb.append("【").append(vol).append(" / ").append(chap).append(" - 第")
                      .append(idx + 1).append("段】\n");
                    sb.append(para.substring(0, Math.min(para.length(), maxCharPerParagraph))).append("\n\n");
                }
            }
        } else {
            sb.append("(暂无正文内容)\n");
        }
        
        return sb.toString();
    }

    /**
     * 加载提示词模板
     */
    private String loadPromptTemplate() {
        return promptManager.getTaskPrompt("extract_relations", null);
    }
        
    /**
     * 默认提示词（模板加载失败时使用）
     */
    private String getDefaultPrompt() {
        return "# 角色\n" +
               "你是一个专业的小说设定关系分析专家。请从文本中提取关系三元组。\n\n" +
               "# 输出格式\n" +
               "返回JSON：\n" +
               "{\n" +
               "  \"confirmed_relations\": [\n" +
               "    {\"source_name\": \"角色A\", \"target_name\": \"角色B\", \"relation_type\": \"FRIEND\", \"description\": \"描述\"}\n" +
               "  ],\n" +
               "  \"pending_entities\": [\n" +
               "    {\"name\": \"新角色X\", \"category\": \"角色\", \"relations\": [{\"target_name\": \"角色A\", \"relation_type\": \"FRIEND\"}]}\n" +
               "  ]\n" +
               "}\n";
    }

    /**
     * 构建用户消息
     */
    private String buildUserMessage(Map<String, Object> contextData) {
        StringBuilder sb = new StringBuilder();
        
        // 小说信息
        sb.append("# 小说信息\n");
        sb.append("标题: ").append(contextData.get("novel_title")).append("\n");
        sb.append("简介: ").append(contextData.get("novel_summary")).append("\n\n");
        
        // 现有设定
        sb.append("# 现有设定列表\n");
        @SuppressWarnings("unchecked")
        List<Map<String, String>> settings = (List<Map<String, String>>) contextData.get("existing_settings");
        if (settings != null && !settings.isEmpty()) {
            for (Map<String, String> setting : settings) {
                sb.append("- ").append(setting.get("title"))
                  .append(" (").append(setting.get("category")).append(")")
                  .append("\n");
            }
        } else {
            sb.append("(暂无设定)\n");
        }
        sb.append("\n");
        
        // 大纲/结构
        String structure = (String) contextData.get("structure");
        if (!TextUtils.isEmpty(structure)) {
            sb.append("# 章节结构\n");
            try {
                JsonObject structureJson = JsonParser.parseString(structure).getAsJsonObject();
                JsonArray volumes = structureJson.has("volumes") ? structureJson.getAsJsonArray("volumes") : null;
                if (volumes != null) {
                    for (JsonElement volElem : volumes) {
                        JsonObject volume = volElem.getAsJsonObject();
                        String volTitle = volume.has("title") ? volume.get("title").getAsString() : "未命名卷";
                        sb.append("【").append(volTitle).append("】\n");
                        
                        JsonArray chapters = volume.has("chapters") ? volume.getAsJsonArray("chapters") : null;
                        if (chapters != null) {
                            for (JsonElement chElem : chapters) {
                                JsonObject chapter = chElem.getAsJsonObject();
                                String chTitle = chapter.has("title") ? chapter.get("title").getAsString() : "未命名章";
                                sb.append("- ").append(chTitle).append("\n");
                            }
                        }
                    }
                }
            } catch (Exception e) {
                sb.append(structure).append("\n");
            }
        }
        sb.append("\n");
        
        // 正文
        sb.append("# 正文片段（摘录）\n");
        @SuppressWarnings("unchecked")
        List<Map<String, String>> paragraphs = (List<Map<String, String>>) contextData.get("document_content");
        if (paragraphs != null && !paragraphs.isEmpty()) {
            for (Map<String, String> para : paragraphs) {
                sb.append("【").append(para.get("chapter")).append("】\n");
                sb.append(para.get("content")).append("\n\n");
            }
        } else {
            sb.append("(暂无正文内容)\n");
        }
        
        sb.append("\n请分析上述内容，提取设定之间的关系。\n");
        sb.append("注意：只从现有设定列表中的实体建立关系，不要创建列表中不存在的实体名称。\n");
        
        return sb.toString();
    }

    /**
     * 创建消息对象
     */
    private Map<String, String> createMessage(String role, String content) {
        Map<String, String> message = new HashMap<>();
        message.put("role", role);
        message.put("content", content);
        return message;
    }

    /**
     * 解析 AI 响应
     */
    private RelationExtractionResult parseAiResponse(String aiResponse, int storyId) throws Exception {
        RelationExtractionResult result = new RelationExtractionResult();

        // 提取 JSON 部分
        String jsonStr = extractJsonFromResponse(aiResponse);
        Log.d(TAG, "提取后的JSON: " + jsonStr);
        
        // 验证是否为有效 JSON
        if (!isValidJson(jsonStr)) {
            String preview = jsonStr.length() > 200 ? jsonStr.substring(0, 200) + "..." : jsonStr;
            throw new Exception("AI返回内容不是有效的JSON格式，请重试。\n内容预览：" + preview);
        }
        
        JsonObject json = gson.fromJson(jsonStr, JsonObject.class);

        // 获取所有设定
        List<StorySetting> allSettings = settingDao.getByStoryId(storyId);
        Map<String, StorySetting> nameToSetting = new HashMap<>();
        for (StorySetting setting : allSettings) {
            nameToSetting.put(setting.getTitle(), setting);
        }

        // 解析待定实体（新格式：实体本身不包含关系）
        List<RelationExtractionResult.PendingEntity> pendingEntities = new ArrayList<>();
        // 支持新格式 "待定实体" 和旧格式 "pending_entities"
        JsonArray pendingArray = null;
        if (json.has("待定实体")) {
            pendingArray = json.getAsJsonArray("待定实体");
        } else if (json.has("pending_entities")) {
            pendingArray = json.getAsJsonArray("pending_entities");
        }
        
        if (pendingArray != null) {
            for (JsonElement element : pendingArray) {
                JsonObject entity = element.getAsJsonObject();
                String name = getJsonString(entity, "name");
                // AI可能返回 suggested_category 或 category
                String category = getJsonString(entity, "suggested_category");
                if (category.isEmpty()) {
                    category = getJsonString(entity, "category");
                }
                // 解析子分类
                String subCategory = getJsonString(entity, "suggested_subcategory");
                if (subCategory.isEmpty()) {
                    subCategory = getJsonString(entity, "subcategory");
                }
                // 解析简介
                String summary = getJsonString(entity, "summary");
                // 解析别名列表
                List<String> aliases = new ArrayList<>();
                if (entity.has("aliases") && entity.get("aliases").isJsonArray()) {
                    for (JsonElement aliasElem : entity.getAsJsonArray("aliases")) {
                        aliases.add(aliasElem.getAsString());
                    }
                }
                // 解析标签列表
                List<String> tags = new ArrayList<>();
                if (entity.has("tags") && entity.get("tags").isJsonArray()) {
                    for (JsonElement tagElem : entity.getAsJsonArray("tags")) {
                        tags.add(tagElem.getAsString());
                    }
                }

                RelationExtractionResult.PendingEntity entityObj = new RelationExtractionResult.PendingEntity();
                entityObj.setName(name);
                entityObj.setSuggestedCategory(category);
                entityObj.setSuggestedSubcategory(subCategory);
                entityObj.setSummary(summary);
                entityObj.setAliases(aliases);
                entityObj.setTags(tags);
                // 注意：新格式中实体不存储关系
                pendingEntities.add(entityObj);
            }
        }

        // 解析潜在关系（新格式）或兼容旧格式的 confirmed_relations
        List<RelationExtractionResult.PotentialRelation> potentialRelations = new ArrayList<>();
        
        // 先尝试新格式 "潜在关系"
        if (json.has("潜在关系")) {
            JsonArray relArray = json.getAsJsonArray("潜在关系");
            for (JsonElement element : relArray) {
                JsonObject rel = element.getAsJsonObject();
                String sourceName = getJsonString(rel, "source_name");
                String targetName = getJsonString(rel, "target_name");
                String relationType = getJsonString(rel, "relationship_type");
                if (relationType.isEmpty()) {
                    relationType = getJsonString(rel, "relation_type");
                }
                boolean isDirected = true;
                if (rel.has("is_directed") && !rel.get("is_directed").isJsonNull()) {
                    isDirected = rel.get("is_directed").getAsBoolean();
                }
                String description = getJsonString(rel, "description");

                RelationExtractionResult.PotentialRelation relObj = new RelationExtractionResult.PotentialRelation();
                relObj.setSourceName(sourceName);
                relObj.setTargetName(targetName);
                relObj.setRelationshipType(relationType);
                relObj.setDirected(isDirected);
                relObj.setDescription(description);
                potentialRelations.add(relObj);
            }
        }
        // 兼容旧格式 confirmed_relations
        else if (json.has("confirmed_relations")) {
            JsonArray confirmedArray = json.getAsJsonArray("confirmed_relations");
            for (JsonElement element : confirmedArray) {
                JsonObject rel = element.getAsJsonObject();
                String sourceName = getJsonString(rel, "source_name");
                String targetName = getJsonString(rel, "target_name");
                String relationType = getJsonString(rel, "relationship_type");
                boolean isDirected = true;
                if (rel.has("is_directed") && !rel.get("is_directed").isJsonNull()) {
                    isDirected = rel.get("is_directed").getAsBoolean();
                }
                String description = getJsonString(rel, "description");

                RelationExtractionResult.PotentialRelation relObj = new RelationExtractionResult.PotentialRelation();
                relObj.setSourceName(sourceName);
                relObj.setTargetName(targetName);
                relObj.setRelationshipType(relationType);
                relObj.setDirected(isDirected);
                relObj.setDescription(description);
                potentialRelations.add(relObj);
            }
        }

        result.setPendingEntities(pendingEntities);
        result.setPotentialRelations(potentialRelations);
        
        return result;
    }

    /**
     * 从AI响应中提取JSON
     * 注：ApiClient.cleanMarkdownCodeBlock 已清理了 ```json ``` 标记，这里只处理括号匹配
     */
    private String extractJsonFromResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            return "{}";
        }
        
        String content = response.trim();
        
        // 找到第一个 { 或 [，然后正确解析到匹配的 } 或 ]
        int start = -1;
        char startChar = 0;
        
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '{' || c == '[') {
                start = i;
                startChar = c;
                break;
            }
        }
        
        if (start == -1) {
            return content;
        }
        
        // 从 start 开始计数括号，找到匹配的结束位置
        int end = findMatchingBracket(content, start, startChar);
        if (end > start) {
            return content.substring(start, end + 1);
        }
        
        return content;
    }
    
    /**
     * 从指定位置开始找到匹配的括号位置
     */
    private int findMatchingBracket(String content, int start, char openBracket) {
        char closeBracket = openBracket == '{' ? '}' : ']';
        int count = 1;
        boolean inString = false;
        char prevChar = 0;
        
        for (int i = start + 1; i < content.length(); i++) {
            char c = content.charAt(i);
            
            if (c == '\\' && i > 0) {
                prevChar = c;
                i++;
                continue;
            }
            
            if (c == '"' && prevChar != '\\') {
                inString = !inString;
            }
            
            if (!inString) {
                if (c == openBracket) count++;
                else if (c == closeBracket) count--;
                
                if (count == 0) {
                    return i;
                }
            }
            
            prevChar = c;
        }
        
        return -1;
    }
    
    /**
     * 验证是否为有效JSON
     */
    private boolean isValidJson(String str) {
        if (str == null || str.trim().isEmpty()) {
            return false;
        }
        str = str.trim();
        if (!str.startsWith("{") && !str.startsWith("[")) {
            return false;
        }
        try {
            JsonParser.parseString(str);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 安全获取JSON字符串
     */
    private String getJsonString(JsonObject obj, String key) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            return obj.get(key).getAsString();
        }
        return "";
    }
}