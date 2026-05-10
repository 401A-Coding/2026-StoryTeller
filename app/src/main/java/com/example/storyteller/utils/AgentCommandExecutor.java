package com.example.storyteller.utils;

import android.content.Context;
import android.text.TextUtils;
import android.widget.Toast;
import com.example.storyteller.data.local.db.StoryDao;
import com.example.storyteller.data.remote.ApiClient;
import com.example.storyteller.model.Chapter;
import com.example.storyteller.model.Story;
import com.example.storyteller.model.Volume;
import java.util.List;
import java.util.Map;

/**
 * 智能体命令执行器
 * 负责解析和执行 AI 返回的结构化命令
 */
public class AgentCommandExecutor {

    private Context context;
    private StoryDao storyDao;

    public AgentCommandExecutor(Context context) {
        this.context = context;
        this.storyDao = new StoryDao(context);
    }

    /**
     * 执行智能体命令
     * @param command AI 返回的命令
     * @param currentStoryId 当前编辑的小说ID
     * @return 执行结果消息
     */
    public String executeCommand(ApiClient.AgentCommand command, int currentStoryId) {
        if (command == null || command.action == null) {
            return "无效的命令";
        }
        
        // 安全检查：确保有有效的小说ID
        if (currentStoryId <= 0) {
            return "错误：未选择小说，请先打开一本小说进行编辑";
        }

        try {
            switch (command.action) {
                case "add_chapter":
                    // 返回命令参数，由 Activity 处理添加逻辑
                    return handleAddChapter(command.parameters, currentStoryId);
                
                case "edit_chapter":
                    return handleEditChapter(command.parameters, currentStoryId);
                
                case "generate_plot":
                    return handleGeneratePlot(command.parameters);
                
                case "create_character":
                    return handleCreateCharacter(command.parameters);
                
                case "answer_question":
                    // 只是回答问题，不执行操作
                    if (command.parameters != null && command.parameters.containsKey("response")) {
                        return (String) command.parameters.get("response");
                    }
                    return "";
                
                default:
                    return "未知操作：" + command.action;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "执行失败：" + e.getMessage();
        }
    }

    /**
     * 获取添加章节的参数（供 Activity 调用）
     */
    public static class AddChapterParams {
        public int volumeId;
        public String chapterTitle;
        public String chapterContent;
    }

    /**
     * 获取添加卷的参数（供 Activity 调用）
     */
    public static class AddVolumeParams {
        public String volumeTitle;
    }

    /**
     * 获取编辑章节的参数（供 Activity 调用）
     */
    public static class EditChapterParams {
        public int volumeId;
        public int chapterId;
        public String editType;  // "rewrite" / "append" / "modify"
        public String newContent;
        public String instruction;  // 编辑指令，如“让它更悬疑”
    }

    /**
     * 解析添加章节命令的参数
     */
    public static AddChapterParams parseAddChapterParams(Map<String, Object> params) {
        AddChapterParams result = new AddChapterParams();
        result.volumeId = params.containsKey("volume_id") ? 
            ((Number) params.get("volume_id")).intValue() : 1;
        result.chapterTitle = params.containsKey("chapter_title") ? 
            (String) params.get("chapter_title") : "新章节";
        result.chapterContent = params.containsKey("chapter_content") ? 
            (String) params.get("chapter_content") : "";
        
        if (TextUtils.isEmpty(result.chapterTitle)) {
            result.chapterTitle = "新章节";
        }
        
        return result;
    }

    /**
     * 解析添加卷命令的参数
     */
    public static AddVolumeParams parseAddVolumeParams(Map<String, Object> params) {
        AddVolumeParams result = new AddVolumeParams();
        result.volumeTitle = params.containsKey("volume_title") ? 
            (String) params.get("volume_title") : "新卷";
        
        if (TextUtils.isEmpty(result.volumeTitle)) {
            result.volumeTitle = "新卷";
        }
        
        return result;
    }

    /**
     * 解析编辑章节命令的参数
     */
    public static EditChapterParams parseEditChapterParams(Map<String, Object> params) {
        EditChapterParams result = new EditChapterParams();
        result.volumeId = params.containsKey("volume_id") ? 
            ((Number) params.get("volume_id")).intValue() : 1;
        result.chapterId = params.containsKey("chapter_id") ? 
            ((Number) params.get("chapter_id")).intValue() : 1;
        result.editType = params.containsKey("edit_type") ? 
            (String) params.get("edit_type") : "rewrite";
        result.newContent = params.containsKey("new_content") ? 
            (String) params.get("new_content") : "";
        result.instruction = params.containsKey("instruction") ? 
            (String) params.get("instruction") : "";
        
        // 验证编辑类型
        if (!"rewrite".equals(result.editType) && 
            !"append".equals(result.editType) && 
            !"modify".equals(result.editType)) {
            result.editType = "rewrite";  // 默认重写
        }
        
        return result;
    }

    /**
     * 处理添加章节命令（仅返回参数，由 Activity 执行实际添加）
     */
    private String handleAddChapter(Map<String, Object> params, int storyId) {
        Story story = storyDao.getStoryById(storyId);
        if (story == null) {
            return "错误：小说不存在或已被删除";
        }

        AddChapterParams chapterParams = parseAddChapterParams(params);
        
        // 返回特殊标记，告诉 Activity 需要执行添加操作
        return "ADD_CHAPTER:" + chapterParams.chapterTitle + ":" + chapterParams.chapterContent;
    }

    /**
     * 处理编辑章节命令
     */
    private String handleEditChapter(Map<String, Object> params, int storyId) {
        // TODO: 实现章节编辑逻辑
        return "编辑功能开发中...";
    }

    /**
     * 处理生成情节建议
     */
    private String handleGeneratePlot(Map<String, Object> params) {
        if (params != null && params.containsKey("response")) {
            return "情节建议：\n" + params.get("response");
        }
        return "无法生成情节建议";
    }

    /**
     * 处理创建角色
     */
    private String handleCreateCharacter(Map<String, Object> params) {
        // TODO: 实现角色创建逻辑
        return "角色创建功能开发中...";
    }

    /**
     * 构建当前小说的上下文信息（用于发送给 AI）
     */
    public static String buildStoryContext(Story story, List<Volume> volumes) {
        StringBuilder context = new StringBuilder();
        
        if (story != null) {
            context.append("小说标题：").append(story.getTitle()).append("\n");
            context.append("小说类型：").append(story.getGenre()).append("\n\n");
        }

        if (volumes != null && !volumes.isEmpty()) {
            context.append("卷章结构（注意：volume_id 和 chapter_id 从1开始）：\n");
            for (int i = 0; i < volumes.size(); i++) {
                Volume volume = volumes.get(i);
                int volumeId = i + 1;
                context.append("- 第").append(volumeId).append("卷：").append(volume.getTitle()).append("\n");
                
                for (int j = 0; j < volume.getChapters().size(); j++) {
                    Chapter chapter = volume.getChapters().get(j);
                    int chapterId = j + 1;
                    context.append("  - 第").append(chapterId).append("章：").append(chapter.getTitle());
                    
                    // 添加章节内容预览（前100字符）
                    if (!TextUtils.isEmpty(chapter.getContent())) {
                        String preview = chapter.getContent();
                        if (preview.length() > 100) {
                            preview = preview.substring(0, 100) + "...";
                        }
                        context.append(" [内容预览：").append(preview).append("]");
                    }
                    context.append("\n");
                }
            }
            context.append("\n");
        }

        // 添加最近的内容片段（限制长度）
        if (story != null && story.getContent() != null) {
            String recentContent = story.getContent();
            if (recentContent.length() > 500) {
                recentContent = recentContent.substring(recentContent.length() - 500);
            }
            context.append("最近内容：\n").append(recentContent);
        }

        return context.toString();
    }
}
