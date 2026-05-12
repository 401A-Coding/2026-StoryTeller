package com.example.storyteller.data.remote;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.MediaType;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import java.io.IOException;
import java.util.List;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import android.content.Context;

public class ApiClient {
    // 单例模式
    private static ApiClient instance;
    private final OkHttpClient okHttpClient;
    private final Gson gson = new Gson();

    private ApiClient() {
        // 初始化OkHttpClient，设置超时时间
        okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)  // AI响应可能较慢，增加超时
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public static synchronized ApiClient getInstance() {
        if (instance == null) {
            instance = new ApiClient();
        }
        return instance;
    }

    // 获取OkHttpClient实例
    public OkHttpClient getOkHttpClient() {
        return okHttpClient;
    }

    // DeepSeek API请求模型
    public static class DeepSeekRequest {
        public String model = "deepseek-v4-flash";  // 根据文档调整模型名称
        public List<Message> messages;
        public int max_tokens = 1000;  // 控制故事长度
        public double temperature = 0.7;  // 创意度，0-1之间
    }

    public static class Message {
        public String role;  // "user" 或 "system"
        public String content;

        public Message(String s, String s1) {
            this.role = s;
            this.content = s1;
        }
    }

    // DeepSeek API响应模型（简化版）
    public static class DeepSeekResponse {
        public List<Choice> choices;

        public static class Choice {
            public Message message;
        }
    }

    // 生成故事的方法（默认使用 flash 模型）
    public void generateStory(String prompt, Context context, Callback callback) {
        generateStory(prompt, "flash", context, callback);
    }

    /**
     * 生成故事的方法（支持模型选择）
     * @param prompt 提示词
     * @param model 模型类型："flash" 或 "pro"
     * @param context Android Context
     * @param callback 回调
     */
    public void generateStory(String prompt, String model, Context context, Callback callback) {
        String apiKey = ApiKeyManager.getApiKey(context);
        if (apiKey.isEmpty()) {
            callback.onFailure(new Exception("API key not set"));
            return;
        }
        // 构建请求体
        DeepSeekRequest request = new DeepSeekRequest();
        // 根据选择的模型设置不同的模型名称
        if ("pro".equals(model)) {
            request.model = "deepseek-v4-pro";  // Pro 模型
        } else {
            request.model = "deepseek-v4-flash";  // Flash 模型（默认）
        }
        request.messages = List.of(new Message("user", prompt));  // 用户提示作为消息

        String json = gson.toJson(request);
        RequestBody body = RequestBody.create(json, MediaType.get("application/json; charset=utf-8"));

        Request req = new Request.Builder()
                .url("https://api.deepseek.com/chat/completions")
                .addHeader("Authorization", "Bearer " + apiKey)  // 替换为你的API密钥
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        // 异步执行请求
        new Thread(() -> {
            try (Response response = okHttpClient.newCall(req).execute()) {
                if (response.isSuccessful()) {
                    String responseJson = response.body().string();
                    DeepSeekResponse apiResponse = gson.fromJson(responseJson, DeepSeekResponse.class);
                    if (apiResponse.choices != null && !apiResponse.choices.isEmpty()) {
                        String story = apiResponse.choices.get(0).message.content;
                        callback.onSuccess(story);
                    } else {
                        callback.onFailure(new Exception("No story generated"));
                    }
                } else {
                    callback.onFailure(new Exception("API Error: " + response.code() + " - " + response.message()));
                }
            } catch (IOException e) {
                callback.onFailure(e);
            }
        }).start();
    }

    // 回调接口
    public interface Callback {
        void onSuccess(String story);
        void onFailure(Exception e);
    }

    // ==================== 智能体功能 ====================

    /**
     * 智能体模式：分析用户意图并返回结构化命令（默认使用 flash 模型）
     * @param userMessage 用户消息
     * @param currentStoryContext 当前小说上下文（卷章结构、最近内容等）
     * @param context Android Context
     * @param callback 回调
     */
    public void processAgentCommand(String userMessage, String currentStoryContext, 
                                     Context context, AgentCallback callback) {
        processAgentCommand(userMessage, currentStoryContext, "flash", context, callback);
    }

    /**
     * 智能体模式：分析用户意图并返回结构化命令（支持模型选择）
     * @param userMessage 用户消息
     * @param currentStoryContext 当前小说上下文（卷章结构、最近内容等）
     * @param model 模型类型："flash" 或 "pro"
     * @param context Android Context
     * @param callback 回调
     */
    public void processAgentCommand(String userMessage, String currentStoryContext, String model,
                                     Context context, AgentCallback callback) {
        String apiKey = ApiKeyManager.getApiKey(context);
        if (apiKey.isEmpty()) {
            callback.onFailure(new Exception("API key not set"));
            return;
        }

        // 构建系统提示，告诉 AI 如何返回结构化命令
        String systemPrompt = "你是一个小说编辑助手。请分析用户的意图，并以 JSON 格式返回要执行的操作。\n" +
                "可用的操作类型：\n" +
                "1. add_volume: 添加新卷\n" +
                "2. add_chapter: 添加新章节\n" +
                "3. edit_chapter: 编辑章节内容（支持重写、续写、修改）\n" +
                "4. delete_chapter: 删除章节\n" +
                "5. delete_volume: 删除卷（至少保留一个卷）\n" +
                "6. move_chapter: 移动章节到新位置（可在同卷内或跨卷）\n" +
                "7. merge_chapters: 合并多个连续章节为一个\n" +
                "8. generate_plot: 生成情节建议\n" +
                "9. create_character: 创建角色\n" +
                "10. answer_question: 回答问题（不执行操作）\n\n" +
                "重要说明：\n" +
                "- volume_id 和 chapter_id 从1开始计数\n" +
                "- 如果用户没有指定具体章节，默认编辑最后一章（最后一个卷的最后一章）\n" +
                "- 编辑章节时必须提供 new_content（AI生成的新内容）\n" +
                "- ⚠️ new_content 必须是纯小说正文，不要包含任何说明性文字！\n" +
                "- ⚠️ 不要在 new_content 中写'请AI生成...'、'以下是...'等提示语\n" +
                "- ⚠️ new_content 应该直接是小说的内容，就像你在写小说一样\n" +
                "- ⚠️ 添加章节时**必须**提供 chapter_title（章节标题），根据内容生成一个简洁有力的标题\n" +
                "- ⚠️ 章节标题应该概括本章主旨，长度控制在 2-8 个字\n" +
                "- 💡 标题示例：'初遇'、'阴谋浮现'、'决战前夕'、'真相大白'\n\n" +
                "返回格式示例：\n" +
                "添加新卷（默认追加到末尾）：\n" +
                "{\n" +
                "  \"action\": \"add_volume\",\n" +
                "  \"parameters\": {\n" +
                "    \"volume_title\": \"新的卷标题\"\n" +
                "  },\n" +
                "  \"reasoning\": \"用户想要添加一个新卷\"\n" +
                "}\n\n" +
                "在指定位置插入卷：\n" +
                "{\n" +
                "  \"action\": \"add_volume\",\n" +
                "  \"parameters\": {\n" +
                "    \"volume_title\": \"番外篇\",\n" +
                "    \"position\": 2,           // 在第几卷附近插入\n" +
                "    \"insert_after\": true     // true=在该卷之后，false=在该卷之前\n" +
                "  },\n" +
                "  \"reasoning\": \"用户想在第2卷后插入新卷\"\n" +
                "}\n\n" +
                "添加章节（默认追加到末尾）：\n" +
                "{\n" +
                "  \"action\": \"add_chapter\",\n" +
                "  \"parameters\": {\n" +
                "    \"volume_id\": 1,\n" +
                "    \"chapter_title\": \"新的章节标题\",\n" +
                "    \"chapter_content\": \"那年夏天，阳光洒在操场上...\"\n" +
                "  },\n" +
                "  \"reasoning\": \"用户想要添加一个新章节\"\n" +
                "}\n\n" +
                "在指定位置插入章节：\n" +
                "{\n" +
                "  \"action\": \"add_chapter\",\n" +
                "  \"parameters\": {\n" +
                "    \"volume_id\": 1,\n" +
                "    \"chapter_title\": \"回忆\",\n" +
                "    \"chapter_content\": \"十年前，那是一个寒冷的冬天...\",\n" +
                "    \"position\": 3,           // 在第几章附近插入\n" +
                "    \"insert_after\": true     // true=在该章之后，false=在该章之前\n" +
                "  },\n" +
                "  \"reasoning\": \"用户想在第3章后插入新章节\"\n" +
                "}\n\n" +
                "编辑章节（重写）：\n" +
                "{\n" +
                "  \"action\": \"edit_chapter\",\n" +
                "  \"parameters\": {\n" +
                "    \"volume_id\": 1,\n" +
                "    \"chapter_id\": 1,\n" +
                "    \"edit_type\": \"rewrite\",\n" +
                "    \"new_content\": \"夜幕降临，森林中传来诡异的声音...\",\n" +
                "    \"new_title\": \"诡异的森林\"  // 可选：同时修改标题\n" +
                "  },\n" +
                "  \"reasoning\": \"用户想要重写第一章\"\n" +
                "}\n\n" +
                "编辑章节（续写）：\n" +
                "{\n" +
                "  \"action\": \"edit_chapter\",\n" +
                "  \"parameters\": {\n" +
                "    \"volume_id\": 1,\n" +
                "    \"chapter_id\": 1,\n" +
                "    \"edit_type\": \"append\",\n" +
                "    \"new_content\": \"他小心翼翼地向前走去，突然听到身后传来脚步声...\",\n" +
                "    \"new_title\": \"新的章节标题\"  // 可选：同时修改标题\n" +
                "  },\n" +
                "  \"reasoning\": \"用户想要续写第一章\"\n" +
                "}\n\n" +
                "删除章节：\n" +
                "{\n" +
                "  \"action\": \"delete_chapter\",\n" +
                "  \"parameters\": {\n" +
                "    \"volume_id\": 1,\n" +
                "    \"chapter_id\": 3\n" +
                "  },\n" +
                "  \"reasoning\": \"用户想要删除第1卷的第3章\"\n" +
                "}\n\n" +
                "删除卷：\n" +
                "{\n" +
                "  \"action\": \"delete_volume\",\n" +
                "  \"parameters\": {\n" +
                "    \"volume_id\": 2\n" +
                "  },\n" +
                "  \"reasoning\": \"用户想要删除第2卷\"\n" +
                "}\n\n" +
                "移动章节（同卷内）：\n" +
                "{\n" +
                "  \"action\": \"move_chapter\",\n" +
                "  \"parameters\": {\n" +
                "    \"from_volume_id\": 1,\n" +
                "    \"from_chapter_id\": 3,\n" +
                "    \"to_volume_id\": 1,\n" +
                "    \"to_position\": 5,\n" +
                "    \"insert_after\": true\n" +
                "  },\n" +
                "  \"reasoning\": \"用户想把第3章移到第5章后面\"\n" +
                "}\n\n" +
                "移动章节（跨卷）：\n" +
                "{\n" +
                "  \"action\": \"move_chapter\",\n" +
                "  \"parameters\": {\n" +
                "    \"from_volume_id\": 1,\n" +
                "    \"from_chapter_id\": 2,\n" +
                "    \"to_volume_id\": 2,\n" +
                "    \"to_position\": 1,\n" +
                "    \"insert_after\": false\n" +
                "  },\n" +
                "  \"reasoning\": \"用户想把第1卷第2章移到第2卷开头\"\n" +
                "}\n\n" +
                "合并章节：\n" +
                "{\n" +
                "  \"action\": \"merge_chapters\",\n" +
                "  \"parameters\": {\n" +
                "    \"volume_id\": 1,\n" +
                "    \"chapter_ids\": [3, 4, 5],\n" +
                "    \"new_title\": \"合并后的新标题\",\n" +
                "    \"merge_strategy\": \"concatenate\"\n" +
                "  },\n" +
                "  \"reasoning\": \"用户想合并第3、4、5章\"\n" +
                "}\n\n" +
                "❌ 错误的 new_content 示例（不要这样写）：\n" +
                "- \"请AI生成续写内容，延续第一章的叙事...\"  ← 这是指令，不是小说内容\n" +
                "- \"以下是续写的内容：xxx\"  ← 不要加说明性文字\n" +
                "- \"根据用户要求，我生成了以下内容...\"  ← 不要解释\n\n" +
                "✅ 正确的 new_content 示例：\n" +
                "- \"他推开门，发现房间里空无一人...\"  ← 直接是小说内容\n" +
                "- \"阳光透过窗户洒进来，照亮了 dusty 的书桌...\"  ← 纯正文\n\n" +
                "如果不需要执行操作（只是聊天），返回：\n" +
                "{\n" +
                "  \"action\": \"answer_question\",\n" +
                "  \"parameters\": {\n" +
                "    \"response\": \"回答内容...\"\n" +
                "  }\n" +
                "}";

        // 构建请求
        DeepSeekRequest request = new DeepSeekRequest();
        // 根据选择的模型设置不同的模型名称
        if ("pro".equals(model)) {
            request.model = "deepseek-v4-pro";  // Pro 模型
        } else {
            request.model = "deepseek-v4-flash";  // Flash 模型（默认）
        }
        request.messages = Arrays.asList(
                new Message("system", systemPrompt),
                new Message("user", "当前小说上下文：\n" + currentStoryContext + "\n\n用户消息：" + userMessage)
        );
        request.max_tokens = 2000;  // 增加到 2000，避免 JSON 被截断
        request.temperature = 0.3;  // 降低温度，提高结构化输出的准确性

        String json = gson.toJson(request);
        RequestBody body = RequestBody.create(json, MediaType.get("application/json; charset=utf-8"));

        Request req = new Request.Builder()
                .url("https://api.deepseek.com/chat/completions")
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        // 异步执行
        new Thread(() -> {
            try (Response response = okHttpClient.newCall(req).execute()) {
                if (response.isSuccessful()) {
                    String responseJson = response.body().string();
                    DeepSeekResponse apiResponse = gson.fromJson(responseJson, DeepSeekResponse.class);
                    if (apiResponse.choices != null && !apiResponse.choices.isEmpty()) {
                        String aiResponse = apiResponse.choices.get(0).message.content;
                        
                        // 尝试解析为 JSON 命令
                        try {
                            AgentCommand command = gson.fromJson(aiResponse, AgentCommand.class);
                            callback.onCommandReady(command);
                        } catch (Exception e) {
                            // 如果解析失败，当作普通聊天处理
                            AgentCommand fallback = new AgentCommand();
                            fallback.action = "answer_question";
                            fallback.parameters = new java.util.HashMap<>();
                            fallback.parameters.put("response", aiResponse);
                            callback.onCommandReady(fallback);
                        }
                    } else {
                        callback.onFailure(new Exception("No response from AI"));
                    }
                } else {
                    callback.onFailure(new Exception("API Error: " + response.code()));
                }
            } catch (IOException e) {
                callback.onFailure(e);
            }
        }).start();
    }

    // 智能体命令模型
    public static class AgentCommand {
        public String action;
        public java.util.Map<String, Object> parameters;
        public String reasoning;
    }

    // 智能体回调接口
    public interface AgentCallback {
        void onCommandReady(AgentCommand command);
        void onFailure(Exception e);
    }
}
