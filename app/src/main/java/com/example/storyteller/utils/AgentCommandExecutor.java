package com.example.storyteller.utils;

import android.content.Context;
import android.text.TextUtils;
import com.example.storyteller.data.local.db.StoryDocumentDao;
import com.example.storyteller.data.local.db.StorySettingDao;
import com.example.storyteller.data.remote.ApiClient;
import com.example.storyteller.data.repository.StoryRepository;
import com.example.storyteller.model.Chapter;
import com.example.storyteller.model.Story;
import com.example.storyteller.model.StoryDocument;
import com.example.storyteller.model.StorySetting;
import com.example.storyteller.model.Volume;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 智能体命令执行器
 * 负责解析和执行 AI 返回的结构化命令
 * 采用命令模式，将业务逻辑与 UI 层解耦
 */
public class AgentCommandExecutor {

    private final StoryRepository repository;
    private final Context context;

    public AgentCommandExecutor(StoryRepository repository) {
        this.repository = repository;
        this.context = null; // 兼容旧构造函数
    }
    
    public AgentCommandExecutor(StoryRepository repository, Context context) {
        this.repository = repository;
        this.context = context;
    }

    /**
     * 执行智能体命令
     * @param command AI 返回的命令
     * @param currentStoryId 当前编辑的小说ID
     * @return 执行结果消息
     */
    public CommandResult executeCommand(ApiClient.AgentCommand command, int currentStoryId) {
        if (command == null || command.action == null) {
            return CommandResult.error("无效的命令");
        }
        
        // 安全检查：确保有有效的小说ID
        if (currentStoryId <= 0) {
            return CommandResult.error("错误：未选择小说，请先打开一本小说进行编辑");
        }

        try {
            switch (command.action) {
                case "add_volume":
                    return handleAddVolume(command.parameters, currentStoryId);
                
                case "add_chapter":
                    return handleAddChapter(command.parameters, currentStoryId);
                
                case "edit_chapter":
                    return handleEditChapter(command.parameters, currentStoryId);
                
                case "delete_chapter":
                    return handleDeleteChapter(command.parameters, currentStoryId);
                
                case "delete_volume":
                    return handleDeleteVolume(command.parameters, currentStoryId);
                
                case "move_chapter":
                    return handleMoveChapter(command.parameters, currentStoryId);
                
                case "merge_chapters":
                    return handleMergeChapters(command.parameters, currentStoryId);
                
                case "answer_question":
                    // 只是回答问题，不执行操作
                    if (command.parameters != null && command.parameters.containsKey("response")) {
                        return CommandResult.success((String) command.parameters.get("response"));
                    }
                    return CommandResult.success("");
                
                // === 设定模式 ===
                case "create_setting":
                    return handleCreateSetting(command.parameters, currentStoryId);
                
                case "batch_create_settings":
                    return handleBatchCreateSettings(command.parameters, currentStoryId);
                
                case "update_setting":
                    return handleUpdateSetting(command.parameters, currentStoryId);
                
                case "delete_setting":
                    return handleDeleteSetting(command.parameters, currentStoryId);
                
                // === 大纲模式 ===
                case "update_global_outline":
                    return handleUpdateGlobalOutline(command.parameters, currentStoryId);
                
                case "update_volume_outline":
                    return handleUpdateVolumeOutline(command.parameters, currentStoryId);
                
                case "update_chapter_outline":
                    return handleUpdateChapterOutline(command.parameters, currentStoryId);
                
                // AI生成大纲命令（自动创作并保存）
                case "generate_global_outline":
                    return handleGenerateGlobalOutline(command.parameters, currentStoryId);
                
                case "generate_volume_outline":
                    return handleGenerateVolumeOutline(command.parameters, currentStoryId);
                
                case "generate_chapter_outline":
                    return handleGenerateChapterOutline(command.parameters, currentStoryId);
                
                // === 文档模式 ===
                case "create_document":
                    return handleCreateDocument(command.parameters, currentStoryId);
                
                case "update_document":
                    return handleUpdateDocument(command.parameters, currentStoryId);
                
                case "delete_document":
                    return handleDeleteDocument(command.parameters, currentStoryId);
                
                case "extract_materials_from_document":
                    return handleExtractMaterials(command.parameters, currentStoryId);
                
                // === 审核模式 ===
                case "review_report":
                    return handleReviewReport(command.parameters, currentStoryId);
                
                case "review_aspect":
                    return handleReviewAspect(command.parameters, currentStoryId);
                
                default:
                    return CommandResult.error("未知操作：" + command.action);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return CommandResult.error("执行失败：" + e.getMessage());
        }
    }

    /**
     * 命令执行结果
     */
    public static class CommandResult {
        public final boolean success;
        public final String message;
        public final String action;
        public final Object data;
            
        private CommandResult(boolean success, String message, String action, Object data) {
            this.success = success;
            this.message = message;
            this.action = action;
            this.data = data;
        }
            
        public static CommandResult success(String message) {
            return new CommandResult(true, message, null, null);
        }
            
        public static CommandResult success(String message, String action, Object data) {
            return new CommandResult(true, message, action, data);
        }
            
        public static CommandResult error(String message) {
            return new CommandResult(false, message, null, null);
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
        public String newTitle; // 可选：新章节标题
        public String instruction;  // 编辑指令，如"让它更悬疑"
    }

    /**
     * 解析添加章节命令的参数
     */
    public static AddChapterParams parseAddChapterParams(Map<String, Object> params) {
        AddChapterParams result = new AddChapterParams();
        // 安全解析 volume_id，默认为 1
        if (params.containsKey("volume_id") && params.get("volume_id") != null) {
            result.volumeId = ((Number) params.get("volume_id")).intValue();
        } else {
            result.volumeId = 1;
        }
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
        // 安全解析 volume_id，默认为 1
        if (params.containsKey("volume_id") && params.get("volume_id") != null) {
            result.volumeId = ((Number) params.get("volume_id")).intValue();
        } else {
            result.volumeId = 1;
        }
        // 安全解析 chapter_id，默认为 1
        if (params.containsKey("chapter_id") && params.get("chapter_id") != null) {
            result.chapterId = ((Number) params.get("chapter_id")).intValue();
        } else {
            result.chapterId = 1;
        }
        result.editType = params.containsKey("edit_type") ? 
            (String) params.get("edit_type") : "rewrite";
        result.newContent = params.containsKey("new_content") ? 
            (String) params.get("new_content") : "";
        result.newTitle = params.containsKey("new_title") ?
            (String) params.get("new_title") : "";
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
     * 处理添加卷命令
     */
    private CommandResult handleAddVolume(Map<String, Object> params, int storyId) {
        Story story = repository.getStoryById(storyId);
        if (story == null) {
            return CommandResult.error("错误：小说不存在或已被删除");
        }

        AddVolumeParams volumeParams = parseAddVolumeParams(params);
        
        // 解析现有结构
        List<Volume> volumes = parseVolumesFromStory(story);
        
        // 检查是否指定了插入位置
        if (params.containsKey("position") && params.get("position") != null) {
            int position = ((Number) params.get("position")).intValue();
            boolean insertAfter = !params.containsKey("insert_after") || 
                                 ((Boolean) params.get("insert_after"));
            
            // 验证位置（可以在第1卷之前到最后一卷之后）
            if (position < 1 || position > volumes.size() + 1) {
                return CommandResult.error("❌ 错误：卷位置超出范围（当前有 " + volumes.size() + " 卷）");
            }
            
            // 计算插入索引
            int insertIndex = insertAfter ? position : position - 1;
            
            // 创建并插入新卷
            Volume newVolume = new Volume(insertIndex + 1, volumeParams.volumeTitle);
            volumes.add(insertIndex, newVolume);
            
            // 重新编号后续卷
            for (int i = insertIndex; i < volumes.size(); i++) {
                volumes.get(i).setId(i + 1);
            }
            
            // 保存更新后的结构
            saveVolumesToStory(story, volumes);
            
            String refVolumeTitle = position <= volumes.size() - 1 ? 
                volumes.get(position).getTitle() : "末尾";
            String positionDesc = insertAfter ? "之后" : "之前";
            
            return CommandResult.success(
                "✅ 已在第" + position + "卷《" + refVolumeTitle + "》" + positionDesc + "添加新卷：《" + volumeParams.volumeTitle + "》",
                "add_volume",
                newVolume
            );
        } else {
            // 默认追加到末尾
            Volume newVolume = new Volume(volumes.size() + 1, volumeParams.volumeTitle);
            volumes.add(newVolume);
            
            // 保存更新后的结构
            saveVolumesToStory(story, volumes);
            
            return CommandResult.success(
                "✅ 已成功在末尾添加卷：《" + volumeParams.volumeTitle + "》",
                "add_volume",
                newVolume
            );
        }
    }

    /**
     * 处理添加章节命令
     */
    private CommandResult handleAddChapter(Map<String, Object> params, int storyId) {
        Story story = repository.getStoryById(storyId);
        if (story == null) {
            return CommandResult.error("错误：小说不存在或已被删除");
        }

        AddChapterParams chapterParams = parseAddChapterParams(params);
        
        // 解析现有结构
        List<Volume> volumes = parseVolumesFromStory(story);
        
        if (volumes.isEmpty()) {
            return CommandResult.error("错误：小说中没有卷，请先添加卷");
        }
        
        // 获取目标卷（默认使用最后一个卷）
        int targetVolumeIndex = Math.max(0, chapterParams.volumeId - 1);
        if (targetVolumeIndex >= volumes.size()) {
            targetVolumeIndex = volumes.size() - 1;
        }
        
        Volume targetVolume = volumes.get(targetVolumeIndex);
        
        // 检查是否指定了插入位置
        if (params.containsKey("position") && params.get("position") != null) {
            int position = ((Number) params.get("position")).intValue();
            boolean insertAfter = !params.containsKey("insert_after") || 
                                 ((Boolean) params.get("insert_after"));
            
            // 如果卷中没有章节，忽略位置参数，直接添加到末尾
            if (targetVolume.getChapters().isEmpty()) {
                // 创建并添加新章节到空卷
                Chapter newChapter = new Chapter(1, chapterParams.chapterTitle, chapterParams.chapterContent);
                targetVolume.addChapter(newChapter);
                
                // 保存更新后的结构
                saveVolumesToStory(story, volumes);
                
                return CommandResult.success(
                    "✅ 已在第" + targetVolume.getId() + "卷添加新章节：《" + chapterParams.chapterTitle + "》",
                    "add_chapter",
                    newChapter
                );
            }
            
            // 验证位置
            if (position < 1 || position > targetVolume.getChapters().size()) {
                return CommandResult.error("❌ 错误：章节位置超出范围（当前有 " + 
                    targetVolume.getChapters().size() + " 章）");
            }
            
            // 计算插入索引
            int insertIndex = insertAfter ? position : position - 1;
            
            // 创建并插入新章节
            Chapter newChapter = new Chapter(insertIndex + 1, chapterParams.chapterTitle, chapterParams.chapterContent);
            targetVolume.getChapters().add(insertIndex, newChapter);
            
            // 重新编号后续章节
            for (int i = insertIndex; i < targetVolume.getChapters().size(); i++) {
                targetVolume.getChapters().get(i).setId(i + 1);
            }
            
            // 保存更新后的结构
            saveVolumesToStory(story, volumes);
            
            String refChapterTitle = targetVolume.getChapters().get(position - 1).getTitle();
            String positionDesc = insertAfter ? "之后" : "之前";
            return CommandResult.success(
                "✅ 已在第" + position + "章《" + refChapterTitle + "》" + positionDesc + "添加新章节：《" + chapterParams.chapterTitle + "》",
                "add_chapter",
                newChapter
            );
        } else {
            // 默认追加到末尾
            int newChapterId = targetVolume.getChapters().size() + 1;
            Chapter newChapter = new Chapter(newChapterId, chapterParams.chapterTitle, chapterParams.chapterContent);
            targetVolume.addChapter(newChapter);
            
            // 保存更新后的结构
            saveVolumesToStory(story, volumes);
            
            return CommandResult.success(
                "✅ 已在第" + targetVolume.getId() + "卷末尾添加章节：《" + chapterParams.chapterTitle + "》",
                "add_chapter",
                newChapter
            );
        }
    }

    /**
     * 处理编辑章节命令
     */
    private CommandResult handleEditChapter(Map<String, Object> params, int storyId) {
        Story story = repository.getStoryById(storyId);
        if (story == null) {
            return CommandResult.error("错误：小说不存在或已被删除");
        }

        EditChapterParams editParams = parseEditChapterParams(params);
        
        // 解析现有结构
        List<Volume> volumes = parseVolumesFromStory(story);
        
        // 验证卷ID
        if (editParams.volumeId < 1 || editParams.volumeId > volumes.size()) {
            return CommandResult.error("❌ 错误：无效的卷ID");
        }
        
        Volume targetVolume = volumes.get(editParams.volumeId - 1);
        
        // 如果卷中没有章节，先自动创建一个章节
        if (targetVolume.getChapters().isEmpty()) {
            Chapter newChapter = new Chapter(1, "新章节", "");
            targetVolume.addChapter(newChapter);
            
            // 保存更新后的结构
            saveVolumesToStory(story, volumes);
            
            // 重新获取目标章节（现在是第1章）
            editParams.chapterId = 1;
            targetVolume = volumes.get(editParams.volumeId - 1);
        }
        
        // 验证章节ID
        if (editParams.chapterId < 1 || editParams.chapterId > targetVolume.getChapters().size()) {
            return CommandResult.error("❌ 错误：无效的章节ID");
        }
        
        Chapter targetChapter = targetVolume.getChapters().get(editParams.chapterId - 1);
        
        // 验证新内容
        if (TextUtils.isEmpty(editParams.newContent)) {
            return CommandResult.error("❌ 错误：AI 没有生成新内容，请重试");
        }
        
        // 根据编辑类型执行不同操作
        switch (editParams.editType) {
            case "rewrite":
                // 重写：完全替换内容
                targetChapter.setContent(editParams.newContent);
                break;
                
            case "append":
                // 续写：追加到末尾
                String currentContent = targetChapter.getContent();
                if (TextUtils.isEmpty(currentContent)) {
                    targetChapter.setContent(editParams.newContent);
                } else {
                    targetChapter.setContent(currentContent + "\n\n" + editParams.newContent);
                }
                break;
                
            case "modify":
                // 修改：暂时当作重写处理（未来可以实现更智能的局部修改）
                targetChapter.setContent(editParams.newContent);
                break;
                
            default:
                return CommandResult.error("❌ 错误：未知的编辑类型");
        }
        
        // 如果提供了新标题，同时更新标题
        if (!TextUtils.isEmpty(editParams.newTitle)) {
            targetChapter.setTitle(editParams.newTitle);
        }
        
        // 保存更新后的结构
        saveVolumesToStory(story, volumes);
        
        String editTypeDesc = getEditTypeDescription(editParams.editType);
        return CommandResult.success(
            "✅ 已成功" + editTypeDesc + "第" + editParams.volumeId + "卷第" + editParams.chapterId + "章",
            "edit_chapter",
            targetChapter
        );
    }

    /**
     * 处理删除章节命令
     */
    private CommandResult handleDeleteChapter(Map<String, Object> params, int storyId) {
        Story story = repository.getStoryById(storyId);
        if (story == null) {
            return CommandResult.error("错误：小说不存在或已被删除");
        }

        // 解析参数
        int volumeId = 1;
        if (params.containsKey("volume_id") && params.get("volume_id") != null) {
            volumeId = ((Number) params.get("volume_id")).intValue();
        }
        int chapterId = 1;
        if (params.containsKey("chapter_id") && params.get("chapter_id") != null) {
            chapterId = ((Number) params.get("chapter_id")).intValue();
        }
        
        // 解析现有结构
        List<Volume> volumes = parseVolumesFromStory(story);
        
        // 验证卷ID
        if (volumeId < 1 || volumeId > volumes.size()) {
            return CommandResult.error("❌ 错误：无效的卷ID");
        }
        
        Volume targetVolume = volumes.get(volumeId - 1);
        
        // 验证章节ID
        if (chapterId < 1 || chapterId > targetVolume.getChapters().size()) {
            return CommandResult.error("❌ 错误：无效的章节ID");
        }
        
        // 获取要删除的章节名称（用于提示）
        String chapterTitle = targetVolume.getChapters().get(chapterId - 1).getTitle();
        
        // 删除章节
        targetVolume.removeChapter(chapterId - 1);
        
        // 如果卷为空，可以选择删除卷或保留空卷
        // 这里选择保留空卷，让用户自己决定是否删除
        
        // 重新编号章节ID
        for (int i = 0; i < targetVolume.getChapters().size(); i++) {
            targetVolume.getChapters().get(i).setId(i + 1);
        }
        
        // 保存更新后的结构
        saveVolumesToStory(story, volumes);
        
        return CommandResult.success(
            "✅ 已删除章节：《" + chapterTitle + "》",
            "delete_chapter",
            null
        );
    }

    /**
     * 处理删除卷命令
     */
    private CommandResult handleDeleteVolume(Map<String, Object> params, int storyId) {
        Story story = repository.getStoryById(storyId);
        if (story == null) {
            return CommandResult.error("错误：小说不存在或已被删除");
        }

        // 解析参数
        int volumeId = 1;
        if (params.containsKey("volume_id") && params.get("volume_id") != null) {
            volumeId = ((Number) params.get("volume_id")).intValue();
        }
        
        // 解析现有结构
        List<Volume> volumes = parseVolumesFromStory(story);
        
        // 验证卷ID
        if (volumeId < 1 || volumeId > volumes.size()) {
            return CommandResult.error("❌ 错误：无效的卷ID");
        }
        
        // 至少保留一个卷
        if (volumes.size() <= 1) {
            return CommandResult.error("❌ 错误：至少需要保留一个卷");
        }
        
        // 获取要删除的卷名称（用于提示）
        String volumeTitle = volumes.get(volumeId - 1).getTitle();
        int chapterCount = volumes.get(volumeId - 1).getChapters().size();
        
        // 删除卷
        volumes.remove(volumeId - 1);
        
        // 重新编号卷ID
        for (int i = 0; i < volumes.size(); i++) {
            volumes.get(i).setId(i + 1);
        }
        
        // 保存更新后的结构
        saveVolumesToStory(story, volumes);
        
        String message = "✅ 已删除卷：《" + volumeTitle + "》（包含 " + chapterCount + " 章）";
        return CommandResult.success(
            message,
            "delete_volume",
            null
        );
    }


    /**
     * 处理移动章节命令
     */
    private CommandResult handleMoveChapter(Map<String, Object> params, int storyId) {
        Story story = repository.getStoryById(storyId);
        if (story == null) {
            return CommandResult.error("错误：小说不存在或已被删除");
        }

        // 解析参数
        int fromVolumeId = 1;
        if (params.containsKey("from_volume_id") && params.get("from_volume_id") != null) {
            fromVolumeId = ((Number) params.get("from_volume_id")).intValue();
        }
        int fromChapterId = 1;
        if (params.containsKey("from_chapter_id") && params.get("from_chapter_id") != null) {
            fromChapterId = ((Number) params.get("from_chapter_id")).intValue();
        }
        int toVolumeId = fromVolumeId;
        if (params.containsKey("to_volume_id") && params.get("to_volume_id") != null) {
            toVolumeId = ((Number) params.get("to_volume_id")).intValue();
        }
        int toPosition = 1;
        if (params.containsKey("to_position") && params.get("to_position") != null) {
            toPosition = ((Number) params.get("to_position")).intValue();
        }
        boolean insertAfter = !params.containsKey("insert_after") || 
                             ((Boolean) params.get("insert_after"));
        
        // 解析现有结构
        List<Volume> volumes = parseVolumesFromStory(story);
        
        // 验证源卷ID
        if (fromVolumeId < 1 || fromVolumeId > volumes.size()) {
            return CommandResult.error("❌ 错误：无效的源卷ID");
        }
        
        // 验证目标卷ID
        if (toVolumeId < 1 || toVolumeId > volumes.size()) {
            return CommandResult.error("❌ 错误：无效的目标卷ID");
        }
        
        Volume fromVolume = volumes.get(fromVolumeId - 1);
        Volume toVolume = volumes.get(toVolumeId - 1);
        
        // 验证源章节ID
        if (fromChapterId < 1 || fromChapterId > fromVolume.getChapters().size()) {
            return CommandResult.error("❌ 错误：无效的源章节ID");
        }
        
        // 获取要移动的章节
        Chapter movingChapter = fromVolume.getChapters().get(fromChapterId - 1);
        String chapterTitle = movingChapter.getTitle();
        
        // 如果是同卷内移动，需要特殊处理
        if (fromVolumeId == toVolumeId) {
            // 验证目标位置
            int maxPosition = fromVolume.getChapters().size();
            if (toPosition < 1 || toPosition > maxPosition) {
                return CommandResult.error("❌ 错误：目标位置超出范围（当前有 " + maxPosition + " 章）");
            }
            
            // 如果目标位置就是当前位置，无需移动
            if (fromChapterId == toPosition || fromChapterId == toPosition + 1) {
                return CommandResult.success("✅ 章节《" + chapterTitle + "》已在目标位置，无需移动");
            }
            
            // 移除原章节
            fromVolume.removeChapter(fromChapterId - 1);
            
            // 计算插入位置
            int insertIndex = insertAfter ? toPosition : toPosition - 1;
            // 如果原章节在目标位置之前，需要调整插入索引
            if (fromChapterId < toPosition) {
                insertIndex--;
            }
            
            // 插入到目标位置
            movingChapter.setId(insertIndex + 1);
            fromVolume.getChapters().add(insertIndex, movingChapter);
            
            // 重新编号所有章节
            for (int i = 0; i < fromVolume.getChapters().size(); i++) {
                fromVolume.getChapters().get(i).setId(i + 1);
            }
        } else {
            // 跨卷移动
            // 验证目标位置
            int maxPosition = toVolume.getChapters().size() + 1;
            if (toPosition < 1 || toPosition > maxPosition) {
                return CommandResult.error("❌ 错误：目标位置超出范围（目标卷当前有 " + (maxPosition - 1) + " 章）");
            }
            
            // 从源卷移除章节
            fromVolume.removeChapter(fromChapterId - 1);
            
            // 重新编号源卷的后续章节
            for (int i = fromChapterId - 1; i < fromVolume.getChapters().size(); i++) {
                fromVolume.getChapters().get(i).setId(i + 1);
            }
            
            // 计算插入位置
            int insertIndex = insertAfter ? toPosition : toPosition - 1;
            if (insertIndex > toVolume.getChapters().size()) {
                insertIndex = toVolume.getChapters().size();
            }
            
            // 插入到目标卷
            movingChapter.setId(insertIndex + 1);
            toVolume.getChapters().add(insertIndex, movingChapter);
            
            // 重新编号目标卷的后续章节
            for (int i = insertIndex; i < toVolume.getChapters().size(); i++) {
                toVolume.getChapters().get(i).setId(i + 1);
            }
        }
        
        // 保存更新后的结构
        saveVolumesToStory(story, volumes);
        
        String sourceDesc = "第" + fromVolumeId + "卷";
        String targetDesc = "第" + toVolumeId + "卷";
        String positionDesc = insertAfter ? "之后" : "之前";
        
        if (fromVolumeId == toVolumeId) {
            return CommandResult.success(
                "✅ 已将章节《" + chapterTitle + "》移动到第" + toPosition + "章" + positionDesc,
                "move_chapter",
                movingChapter
            );
        } else {
            return CommandResult.success(
                "✅ 已将章节《" + chapterTitle + "》从" + sourceDesc + "移动到" + targetDesc + "的第" + toPosition + "章" + positionDesc,
                "move_chapter",
                movingChapter
            );
        }
    }

    /**
     * 处理合并章节命令
     */
    private CommandResult handleMergeChapters(Map<String, Object> params, int storyId) {
        Story story = repository.getStoryById(storyId);
        if (story == null) {
            return CommandResult.error("错误：小说不存在或已被删除");
        }

        // 解析参数
        int volumeId = 1;
        if (params.containsKey("volume_id") && params.get("volume_id") != null) {
            volumeId = ((Number) params.get("volume_id")).intValue();
        }
        
        // 解析章节ID列表
        @SuppressWarnings("unchecked")
        List<Number> chapterIdsRaw = (List<Number>) params.get("chapter_ids");
        if (chapterIdsRaw == null || chapterIdsRaw.isEmpty()) {
            return CommandResult.error("❌ 错误：未指定要合并的章节");
        }
        
        List<Integer> chapterIds = new java.util.ArrayList<>();
        for (Number num : chapterIdsRaw) {
            chapterIds.add(num.intValue());
        }
        
        // 排序章节ID
        java.util.Collections.sort(chapterIds);
        
        String newTitle = params.containsKey("new_title") ? 
            (String) params.get("new_title") : "";
        String mergeStrategy = params.containsKey("merge_strategy") ? 
            (String) params.get("merge_strategy") : "concatenate";
        
        // 解析现有结构
        List<Volume> volumes = parseVolumesFromStory(story);
        
        // 验证卷ID
        if (volumeId < 1 || volumeId > volumes.size()) {
            return CommandResult.error("❌ 错误：无效的卷ID");
        }
        
        Volume targetVolume = volumes.get(volumeId - 1);
        
        // 验证章节ID有效性
        for (int chapterId : chapterIds) {
            if (chapterId < 1 || chapterId > targetVolume.getChapters().size()) {
                return CommandResult.error("❌ 错误：无效的章节ID " + chapterId);
            }
        }
        
        // 验证章节是否连续
        for (int i = 1; i < chapterIds.size(); i++) {
            if (chapterIds.get(i) != chapterIds.get(i - 1) + 1) {
                return CommandResult.error("❌ 错误：只能合并连续的章节");
            }
        }
        
        // 至少需要两个章节才能合并
        if (chapterIds.size() < 2) {
            return CommandResult.error("❌ 错误：至少需要两个章节才能合并");
        }
        
        // 获取所有章节内容并合并
        StringBuilder mergedContent = new StringBuilder();
        List<String> chapterTitles = new java.util.ArrayList<>();
        
        for (int i = 0; i < chapterIds.size(); i++) {
            Chapter chapter = targetVolume.getChapters().get(chapterIds.get(i) - 1);
            chapterTitles.add(chapter.getTitle());
            
            if (!TextUtils.isEmpty(chapter.getContent())) {
                if (i > 0) {
                    mergedContent.append("\n\n");
                }
                mergedContent.append(chapter.getContent());
            }
        }
        
        // 如果没有提供新标题，使用第一个章节的标题或生成一个
        if (TextUtils.isEmpty(newTitle)) {
            if (chapterTitles.size() > 0) {
                newTitle = chapterTitles.get(0) + "（合并）";
            } else {
                newTitle = "合并章节";
            }
        }
        
        // 根据策略处理内容（目前只实现直接拼接）
        String finalContent = mergedContent.toString();
        
        // 删除原章节（从后往前删，避免索引变化）
        for (int i = chapterIds.size() - 1; i >= 0; i--) {
            targetVolume.removeChapter(chapterIds.get(i) - 1);
        }
        
        // 在第一个位置插入合并后的新章节
        int insertIndex = chapterIds.get(0) - 1;
        Chapter mergedChapter = new Chapter(insertIndex + 1, newTitle, finalContent);
        targetVolume.getChapters().add(insertIndex, mergedChapter);
        
        // 重新编号后续章节
        for (int i = insertIndex; i < targetVolume.getChapters().size(); i++) {
            targetVolume.getChapters().get(i).setId(i + 1);
        }
        
        // 保存更新后的结构
        saveVolumesToStory(story, volumes);
        
        return CommandResult.success(
            "✅ 已成功合并 " + chapterIds.size() + " 个章节为《" + newTitle + "》",
            "merge_chapters",
            mergedChapter
        );
    }

    /**
     * 获取编辑类型的中文描述
     */
    private String getEditTypeDescription(String editType) {
        switch (editType) {
            case "rewrite":
                return "重写";
            case "append":
                return "续写";
            case "modify":
                return "修改";
            default:
                return "编辑";
        }
    }

    /**
     * 从故事中解析卷章结构
     */
    private List<Volume> parseVolumesFromStory(Story story) {
        String structureJson = story.getStructure();
        if (!TextUtils.isEmpty(structureJson)) {
            try {
                return com.example.storyteller.utils.JsonUtils.fromJson(structureJson,
                    new com.google.gson.reflect.TypeToken<List<Volume>>(){}.getType());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // 如果解析失败，返回空列表
        return new java.util.ArrayList<>();
    }

    /**
     * 保存卷章结构到故事
     */
    private void saveVolumesToStory(Story story, List<Volume> volumes) {
        String structureJson = com.example.storyteller.utils.JsonUtils.toJson(volumes);
        story.setStructure(structureJson);
        
        // 同时更新故事内容（从所有章节构建完整内容）
        StringBuilder fullContent = new StringBuilder();
        int totalWordCount = 0; // 计算总字数
        for (int volIdx = 0; volIdx < volumes.size(); volIdx++) {
            Volume volume = volumes.get(volIdx);
            // 添加卷标题
            if (!TextUtils.isEmpty(volume.getTitle())) {
                fullContent.append("# 第").append(volIdx + 1).append("卷 ").append(volume.getTitle()).append("\n\n");
            }
            for (int chapIdx = 0; chapIdx < volume.getChapters().size(); chapIdx++) {
                Chapter chapter = volume.getChapters().get(chapIdx);
                if (!TextUtils.isEmpty(chapter.getTitle())) {
                    fullContent.append("## 第").append(chapIdx + 1).append("章 ").append(chapter.getTitle()).append("\n\n");
                }
                if (!TextUtils.isEmpty(chapter.getContent())) {
                    fullContent.append(chapter.getContent()).append("\n\n");
                    totalWordCount += chapter.getContent().length(); // 累加字数
                }
            }
        }
        story.setContent(fullContent.toString().trim());
        story.setWordCount(totalWordCount); // 更新字数统计
        story.setPlotSummaryJson(null);

        // 保存到数据库
        repository.updateStory(story);
    }

    /**
     * 构建当前小说的上下文信息（用于发送给 AI）
     * 包含：基本信息、全局大纲、卷章结构、最近内容
     */
    public static String buildStoryContext(Story story, List<Volume> volumes) {
        StringBuilder context = new StringBuilder();
        
        if (story != null) {
            context.append("小说标题：").append(story.getTitle()).append("\n");
            context.append("小说类型：").append(story.getGenre()).append("\n");
            
            // 添加小说简介（如果有）
            if (!TextUtils.isEmpty(story.getDescription())) {
                context.append("小说简介：").append(story.getDescription()).append("\n");
            }
            
            // 添加全局大纲（如果有）
            if (!TextUtils.isEmpty(story.getGlobalOutline())) {
                context.append("\n全局大纲：\n").append(story.getGlobalOutline()).append("\n");
            }
            
            context.append("\n");
        }

        if (volumes != null && !volumes.isEmpty()) {
            context.append("卷章结构与大纲（注意：volume_id 和 chapter_id 从1开始）：\n");
            for (int i = 0; i < volumes.size(); i++) {
                Volume volume = volumes.get(i);
                int volumeId = i + 1;
                context.append("- 第").append(volumeId).append("卷：").append(volume.getTitle()).append("\n");
                
                // 添加卷大纲信息
                if (!TextUtils.isEmpty(volume.getSummary())) {
                    context.append("  [卷摘要：").append(volume.getSummary()).append("]\n");
                }
                if (volume.getTargetWordCount() > 0) {
                    context.append("  [目标字数：").append(volume.getTargetWordCount()).append("]\n");
                }
                if (volume.getTargetChapterCount() > 0) {
                    context.append("  [目标章节数：").append(volume.getTargetChapterCount()).append("]\n");
                }
                
                for (int j = 0; j < volume.getChapters().size(); j++) {
                    Chapter chapter = volume.getChapters().get(j);
                    int chapterId = j + 1;
                    context.append("  - 第").append(chapterId).append("章：").append(chapter.getTitle());
                    
                    // 添加章节大纲信息
                    List<String> chapterInfo = new ArrayList<>();
                    if (!TextUtils.isEmpty(chapter.getChapterRole())) {
                        chapterInfo.add("作用：" + chapter.getChapterRole());
                    }
                    if (!TextUtils.isEmpty(chapter.getChapterSummary())) {
                        chapterInfo.add("摘要：" + chapter.getChapterSummary());
                    }
                    if (chapter.getSuspenseLevel() > 0) {
                        chapterInfo.add("悬念：" + chapter.getSuspenseLevel() + "/10");
                    }
                    if (chapter.getTwistLevel() > 0) {
                        chapterInfo.add("转折：" + chapter.getTwistLevel() + "/5");
                    }
                    
                    if (!chapterInfo.isEmpty()) {
                        context.append(" [" + String.join(", ", chapterInfo) + "]");
                    }
                    
                    // 添加章节内容预览（前100字符）
                    if (!TextUtils.isEmpty(chapter.getContent())) {
                        String preview = chapter.getContent();
                        if (preview.length() > 100) {
                            preview = preview.substring(0, 100) + "...";
                        }
                        context.append("\n    [内容预览：").append(preview).append("]");
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
    
    // ==================== 设定模式命令处理 ====================
    
    /**
     * 处理创建设定命令
     */
    private CommandResult handleCreateSetting(Map<String, Object> params, int storyId) {
        if (context == null) {
            return CommandResult.error("错误：未初始化Context，无法创建设定");
        }
        
        try {
            StorySettingDao settingDao = new StorySettingDao(context);
            
            StorySetting setting = new StorySetting();
            setting.setStoryId(storyId);
            setting.setCategory((String) params.get("category"));
            setting.setSubCategory((String) params.get("subCategory"));
            setting.setTitle((String) params.get("title"));
            setting.setSummary((String) params.get("summary"));
            setting.setDetail((String) params.get("detail"));
            
            // 处理tags（JSON数组）
            if (params.containsKey("tags") && params.get("tags") != null) {
                setting.setTags(JsonUtils.toJson(params.get("tags")));
            }
            
            // 处理aliases（JSON数组）
            if (params.containsKey("aliases") && params.get("aliases") != null) {
                setting.setAliases(JsonUtils.toJson(params.get("aliases")));
            }
            
            // 处理specificAttributes（JSON字符串）
            if (params.containsKey("specificAttributes") && params.get("specificAttributes") != null) {
                setting.setSpecificAttributes((String) params.get("specificAttributes"));
            }
            
            long id = settingDao.insert(setting);
            if (id > 0) {
                return CommandResult.success(
                    "✅ 已成功创建设定：《" + setting.getTitle() + "》",
                    "create_setting",
                    setting
                );
            } else {
                return CommandResult.error("❌ 创建设定失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return CommandResult.error("❌ 创建设定时出错：" + e.getMessage());
        }
    }
    
    /**
     * 处理批量创建设定命令
     */
    @SuppressWarnings("unchecked")
    private CommandResult handleBatchCreateSettings(Map<String, Object> params, int storyId) {
        if (context == null) {
            return CommandResult.error("错误：未初始化Context，无法创建设定");
        }
        
        try {
            StorySettingDao settingDao = new StorySettingDao(context);
            
            // 获取settings列表
            List<Map<String, Object>> settingsList = (List<Map<String, Object>>) params.get("settings");
            
            if (settingsList == null || settingsList.isEmpty()) {
                return CommandResult.error("❌ 错误：没有提供要创建的设定");
            }
            
            // 限制最多10个
            if (settingsList.size() > 10) {
                return CommandResult.error("❌ 错误：批量创建最多支持10个设定");
            }
            
            int successCount = 0;
            int failCount = 0;
            List<String> successTitles = new ArrayList<>();
            List<String> failTitles = new ArrayList<>();
            
            // 逐个创建设定
            for (Map<String, Object> settingParams : settingsList) {
                try {
                    StorySetting setting = new StorySetting();
                    setting.setStoryId(storyId);
                    setting.setCategory((String) settingParams.get("category"));
                    setting.setSubCategory((String) settingParams.get("subCategory"));
                    setting.setTitle((String) settingParams.get("title"));
                    setting.setSummary((String) settingParams.get("summary"));
                    setting.setDetail((String) settingParams.get("detail"));
                    
                    // 处理tags
                    if (settingParams.containsKey("tags") && settingParams.get("tags") != null) {
                        setting.setTags(JsonUtils.toJson(settingParams.get("tags")));
                    }
                    
                    // 处理aliases
                    if (settingParams.containsKey("aliases") && settingParams.get("aliases") != null) {
                        setting.setAliases(JsonUtils.toJson(settingParams.get("aliases")));
                    }
                    
                    // 处理specificAttributes
                    if (settingParams.containsKey("specificAttributes") && settingParams.get("specificAttributes") != null) {
                        setting.setSpecificAttributes((String) settingParams.get("specificAttributes"));
                    }
                    
                    long id = settingDao.insert(setting);
                    if (id > 0) {
                        successCount++;
                        successTitles.add(setting.getTitle());
                    } else {
                        failCount++;
                        failTitles.add(setting.getTitle());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    failCount++;
                    String title = (String) settingParams.get("title");
                    failTitles.add(title != null ? title : "未知设定");
                }
            }
            
            // 构建结果消息
            StringBuilder message = new StringBuilder();
            message.append("✅ 批量创建完成：成功").append(successCount).append("个");
            if (failCount > 0) {
                message.append("，失败").append(failCount).append("个");
            }
            
            if (!successTitles.isEmpty()) {
                message.append("\n成功：").append(String.join("、", successTitles));
            }
            if (!failTitles.isEmpty()) {
                message.append("\n失败：").append(String.join("、", failTitles));
            }
            
            return CommandResult.success(message.toString(), "batch_create_settings", null);
            
        } catch (Exception e) {
            e.printStackTrace();
            return CommandResult.error("❌ 批量创建设定时出错：" + e.getMessage());
        }
    }
    
    /**
     * 处理更新设定命令
     */
    private CommandResult handleUpdateSetting(Map<String, Object> params, int storyId) {
        if (context == null) {
            return CommandResult.error("错误：未初始化Context，无法更新设定");
        }
        
        try {
            StorySettingDao settingDao = new StorySettingDao(context);
            
            int settingId = ((Number) params.get("settingId")).intValue();
            StorySetting setting = settingDao.getById(settingId);
            
            if (setting == null) {
                return CommandResult.error("❌ 错误：设定不存在（ID: " + settingId + "）");
            }
            
            // 更新字段
            @SuppressWarnings("unchecked")
            Map<String, Object> fields = (Map<String, Object>) params.get("fields");
            if (fields != null) {
                if (fields.containsKey("summary")) {
                    setting.setSummary((String) fields.get("summary"));
                }
                if (fields.containsKey("detail")) {
                    setting.setDetail((String) fields.get("detail"));
                }
                if (fields.containsKey("tags")) {
                    setting.setTags(JsonUtils.toJson(fields.get("tags")));
                }
                if (fields.containsKey("aliases")) {
                    setting.setAliases(JsonUtils.toJson(fields.get("aliases")));
                }
                if (fields.containsKey("specificAttributes")) {
                    setting.setSpecificAttributes((String) fields.get("specificAttributes"));
                }
            }
            
            int result = settingDao.update(setting);
            if (result > 0) {
                return CommandResult.success(
                    "✅ 已成功更新设定：《" + setting.getTitle() + "》",
                    "update_setting",
                    setting
                );
            } else {
                return CommandResult.error("❌ 更新设定失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return CommandResult.error("❌ 更新设定时出错：" + e.getMessage());
        }
    }
    
    /**
     * 处理删除设定命令
     */
    private CommandResult handleDeleteSetting(Map<String, Object> params, int storyId) {
        if (context == null) {
            return CommandResult.error("错误：未初始化Context，无法删除设定");
        }
        
        try {
            StorySettingDao settingDao = new StorySettingDao(context);
            
            int settingId = ((Number) params.get("settingId")).intValue();
            StorySetting setting = settingDao.getById(settingId);
            
            if (setting == null) {
                return CommandResult.error("❌ 错误：设定不存在（ID: " + settingId + "）");
            }
            
            String title = setting.getTitle();
            int result = settingDao.delete(settingId);
            
            if (result > 0) {
                return CommandResult.success(
                    "✅ 已删除设定：《" + title + "》",
                    "delete_setting",
                    null
                );
            } else {
                return CommandResult.error("❌ 删除设定失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return CommandResult.error("❌ 删除设定时出错：" + e.getMessage());
        }
    }
    
    // ==================== 大纲模式命令处理 ====================
    
    /**
     * 处理更新全局大纲命令
     */
    private CommandResult handleUpdateGlobalOutline(Map<String, Object> params, int storyId) {
        try {
            String globalOutline = (String) params.get("globalOutline");
            
            int result = repository.updateStoryGlobalOutline(storyId, globalOutline);
            
            if (result > 0) {
                return CommandResult.success(
                    "✅ 已成功更新全局大纲",
                    "update_global_outline",
                    globalOutline
                );
            } else {
                return CommandResult.error("❌ 更新全局大纲失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return CommandResult.error("❌ 更新全局大纲时出错：" + e.getMessage());
        }
    }
    
    /**
     * 处理更新卷纲命令
     */
    private CommandResult handleUpdateVolumeOutline(Map<String, Object> params, int storyId) {
        try {
            int volumeIndex = ((Number) params.get("volumeIndex")).intValue();
            
            Story story = repository.getStoryById(storyId);
            if (story == null) {
                return CommandResult.error("错误：小说不存在");
            }
            
            // 解析outlineData
            List<Volume> volumes = parseVolumesFromOutlineData(story);
            
            if (volumeIndex < 0 || volumeIndex >= volumes.size()) {
                return CommandResult.error("❌ 错误：卷索引超出范围（当前有 " + volumes.size() + " 卷）");
            }
            
            Volume volume = volumes.get(volumeIndex);
            
            // 更新字段
            boolean titleChanged = false;
            if (params.containsKey("title")) {
                volume.setTitle((String) params.get("title"));
                titleChanged = true;
            }
            if (params.containsKey("summary")) {
                volume.setSummary((String) params.get("summary"));
            }
            if (params.containsKey("targetWordCount")) {
                volume.setTargetWordCount(((Number) params.get("targetWordCount")).intValue());
            }
            if (params.containsKey("targetChapterCount")) {
                volume.setTargetChapterCount(((Number) params.get("targetChapterCount")).intValue());
            }
            
            // 保存outlineData
            String updatedJson = JsonUtils.toJson(volumes);
            repository.updateStoryOutline(storyId, updatedJson);
            
            // 如果标题发生变化，需要同步更新structure
            if (titleChanged) {
                syncVolumeTitleToStructure(storyId, volumeIndex, volume.getTitle());
            }
            
            return CommandResult.success(
                "✅ 已成功更新第" + (volumeIndex + 1) + "卷《" + volume.getTitle() + "》的大纲",
                "update_volume_outline",
                volume
            );
        } catch (Exception e) {
            e.printStackTrace();
            return CommandResult.error("❌ 更新卷纲时出错：" + e.getMessage());
        }
    }
    
    /**
     * 处理更新章纲命令
     */
    private CommandResult handleUpdateChapterOutline(Map<String, Object> params, int storyId) {
        try {
            int volumeIndex = ((Number) params.get("volumeIndex")).intValue();
            int chapterIndex = ((Number) params.get("chapterIndex")).intValue();
            
            Story story = repository.getStoryById(storyId);
            if (story == null) {
                return CommandResult.error("错误：小说不存在");
            }
            
            // 解析outlineData
            List<Volume> volumes = parseVolumesFromOutlineData(story);
            
            if (volumeIndex < 0 || volumeIndex >= volumes.size()) {
                return CommandResult.error("❌ 错误：卷索引超出范围");
            }
            
            Volume volume = volumes.get(volumeIndex);
            List<Chapter> chapters = volume.getChapters();
            
            if (chapterIndex < 0 || chapterIndex >= chapters.size()) {
                return CommandResult.error("❌ 错误：章节索引超出范围（当前有 " + chapters.size() + " 章）");
            }
            
            Chapter chapter = chapters.get(chapterIndex);
            
            // 更新字段
            @SuppressWarnings("unchecked")
            Map<String, Object> fields = (Map<String, Object>) params.get("fields");
            boolean titleChanged = false;
            if (fields != null) {
                // 安全地获取字符串字段
                if (fields.containsKey("title")) {
                    Object titleValue = fields.get("title");
                    if (titleValue instanceof String) {
                        chapter.setTitle((String) titleValue);
                        titleChanged = true;
                    } else {
                        android.util.Log.w("AgentCommandExecutor", "title字段类型错误，期望String，实际: " + 
                            (titleValue != null ? titleValue.getClass().getSimpleName() : "null"));
                    }
                }
                if (fields.containsKey("chapterRole")) {
                    Object value = fields.get("chapterRole");
                    if (value instanceof String) {
                        chapter.setChapterRole((String) value);
                    }
                }
                if (fields.containsKey("chapterSummary")) {
                    Object value = fields.get("chapterSummary");
                    if (value instanceof String) {
                        chapter.setChapterSummary((String) value);
                    }
                }
                if (fields.containsKey("chapterPurpose")) {
                    Object value = fields.get("chapterPurpose");
                    if (value instanceof String) {
                        chapter.setChapterPurpose((String) value);
                    }
                }
                if (fields.containsKey("suspenseLevel")) {
                    Object value = fields.get("suspenseLevel");
                    if (value instanceof Number) {
                        chapter.setSuspenseLevel(((Number) value).floatValue());
                    }
                }
                if (fields.containsKey("foreshadowing")) {
                    Object value = fields.get("foreshadowing");
                    if (value instanceof String) {
                        chapter.setForeshadowing((String) value);
                    }
                }
                if (fields.containsKey("twistLevel")) {
                    Object value = fields.get("twistLevel");
                    if (value instanceof Number) {
                        chapter.setTwistLevel(((Number) value).floatValue());
                    }
                }
                if (fields.containsKey("involvedCharacters")) {
                    Object value = fields.get("involvedCharacters");
                    if (value instanceof List) {
                        chapter.setInvolvedCharacters((List<String>) value);
                    }
                }
                if (fields.containsKey("keyItems")) {
                    Object value = fields.get("keyItems");
                    if (value instanceof List) {
                        chapter.setKeyItems((List<String>) value);
                    }
                }
                if (fields.containsKey("sceneLocations")) {
                    Object value = fields.get("sceneLocations");
                    if (value instanceof List) {
                        chapter.setSceneLocations((List<String>) value);
                    }
                }
                if (fields.containsKey("timeConstraint")) {
                    Object value = fields.get("timeConstraint");
                    if (value instanceof String) {
                        chapter.setTimeConstraint((String) value);
                    }
                }
            }
            
            // 保存outlineData
            String updatedJson = JsonUtils.toJson(volumes);
            repository.updateStoryOutline(storyId, updatedJson);
            
            // 如果章节标题发生变化，需要同步更新structure
            if (titleChanged) {
                syncChapterTitleToStructure(storyId, volumeIndex, chapterIndex, chapter.getTitle());
            }
            
            return CommandResult.success(
                "✅ 已成功更新第" + (volumeIndex + 1) + "卷第" + (chapterIndex + 1) + "章《" + chapter.getTitle() + "》的大纲",
                "update_chapter_outline",
                chapter
            );
        } catch (Exception e) {
            e.printStackTrace();
            return CommandResult.error("❌ 更新章纲时出错：" + e.getMessage());
        }
    }
    
    /**
     * 处理生成全局大纲命令（AI自动创作）
     * 与update_global_outline逻辑相同，语义上强调是AI创作
     */
    private CommandResult handleGenerateGlobalOutline(Map<String, Object> params, int storyId) {
        return handleUpdateGlobalOutline(params, storyId);
    }
    
    /**
     * 处理生成卷纲命令（AI自动创作）
     * 与update_volume_outline逻辑相同
     */
    private CommandResult handleGenerateVolumeOutline(Map<String, Object> params, int storyId) {
        return handleUpdateVolumeOutline(params, storyId);
    }
    
    /**
     * 处理生成章纲命令（AI自动创作）
     * 与update_chapter_outline逻辑相同
     */
    private CommandResult handleGenerateChapterOutline(Map<String, Object> params, int storyId) {
        return handleUpdateChapterOutline(params, storyId);
    }
    
    // ==================== 文档模式命令处理 ====================
    
    /**
     * 处理创建文档命令
     */
    private CommandResult handleCreateDocument(Map<String, Object> params, int storyId) {
        if (context == null) {
            return CommandResult.error("错误：未初始化Context，无法创建文档");
        }
        
        try {
            StoryDocumentDao documentDao = new StoryDocumentDao(context);
            
            StoryDocument doc = new StoryDocument();
            doc.setStoryId(storyId);
            doc.setTitle((String) params.get("title"));
            doc.setContent((String) params.get("content"));
            doc.setCategory((String) params.get("category"));
            
            long id = documentDao.insertDocument(doc);
            if (id > 0) {
                doc.setId((int) id);
                return CommandResult.success(
                    "✅ 已成功创建文档：《" + doc.getTitle() + "》",
                    "create_document",
                    doc
                );
            } else {
                return CommandResult.error("❌ 创建文档失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return CommandResult.error("❌ 创建文档时出错：" + e.getMessage());
        }
    }
    
    /**
     * 处理更新文档命令
     */
    private CommandResult handleUpdateDocument(Map<String, Object> params, int storyId) {
        if (context == null) {
            return CommandResult.error("错误：未初始化Context，无法更新文档");
        }
        
        try {
            StoryDocumentDao documentDao = new StoryDocumentDao(context);
            
            int documentId = ((Number) params.get("documentId")).intValue();
            StoryDocument doc = documentDao.getDocumentById(documentId);
            
            if (doc == null) {
                return CommandResult.error("❌ 错误：文档不存在（ID: " + documentId + "）");
            }
            
            // 更新字段
            @SuppressWarnings("unchecked")
            Map<String, Object> fields = (Map<String, Object>) params.get("fields");
            if (fields != null) {
                if (fields.containsKey("title")) {
                    doc.setTitle((String) fields.get("title"));
                }
                if (fields.containsKey("content")) {
                    doc.setContent((String) fields.get("content"));
                }
                if (fields.containsKey("category")) {
                    doc.setCategory((String) fields.get("category"));
                }
            }
            
            int result = documentDao.updateDocument(doc);
            if (result > 0) {
                return CommandResult.success(
                    "✅ 已成功更新文档：《" + doc.getTitle() + "》",
                    "update_document",
                    doc
                );
            } else {
                return CommandResult.error("❌ 更新文档失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return CommandResult.error("❌ 更新文档时出错：" + e.getMessage());
        }
    }
    
    /**
     * 处理删除文档命令
     */
    private CommandResult handleDeleteDocument(Map<String, Object> params, int storyId) {
        if (context == null) {
            return CommandResult.error("错误：未初始化Context，无法删除文档");
        }
        
        try {
            StoryDocumentDao documentDao = new StoryDocumentDao(context);
            
            int documentId = ((Number) params.get("documentId")).intValue();
            StoryDocument doc = documentDao.getDocumentById(documentId);
            
            if (doc == null) {
                return CommandResult.error("❌ 错误：文档不存在（ID: " + documentId + "）");
            }
            
            String title = doc.getTitle();
            int result = documentDao.deleteDocument(documentId);
            
            if (result > 0) {
                return CommandResult.success(
                    "✅ 已删除文档：《" + title + "》",
                    "delete_document",
                    null
                );
            } else {
                return CommandResult.error("❌ 删除文档失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return CommandResult.error("❌ 删除文档时出错：" + e.getMessage());
        }
    }
    
    /**
     * 处理从文档提取素材命令
     */
    private CommandResult handleExtractMaterials(Map<String, Object> params, int storyId) {
        // TODO: 实现从文档提取素材并创建设定的逻辑
        // 这需要：
        // 1. 读取指定文档的内容
        // 2. 调用专用任务Prompt进行智能提取
        // 3. 根据extractType创建对应的StorySetting条目
        return CommandResult.error("⚠️ 从文档提取素材功能开发中...");
    }
    
    // ==================== 辅助方法 ====================
    
    /**
     * 从 outlineData字段解析卷章结构
     */
    private List<Volume> parseVolumesFromOutlineData(Story story) {
        String outlineJson = story.getOutlineData();
        if (!TextUtils.isEmpty(outlineJson)) {
            try {
                return JsonUtils.fromJson(outlineJson,
                    new com.google.gson.reflect.TypeToken<List<Volume>>(){}.getType());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // 如果解析失败，返回空列表
        return new ArrayList<>();
    }
    
    /**
     * 同步卷标题到structure字段
     * 确保目录和写作区显示的标题与大纲一致
     */
    private void syncVolumeTitleToStructure(int storyId, int volumeIndex, String newTitle) {
        try {
            Story story = repository.getStoryById(storyId);
            if (story == null) {
                android.util.Log.e("AgentCommandExecutor", "同步标题失败：小说不存在");
                return;
            }
            
            // 解析structure
            String structureJson = story.getStructure();
            if (TextUtils.isEmpty(structureJson)) {
                android.util.Log.w("AgentCommandExecutor", "structure为空，无法同步标题");
                return;
            }
            
            List<Volume> volumes = JsonUtils.fromJson(structureJson,
                new com.google.gson.reflect.TypeToken<List<Volume>>(){}.getType());
            
            if (volumeIndex < 0 || volumeIndex >= volumes.size()) {
                android.util.Log.e("AgentCommandExecutor", "同步标题失败：卷索引超出范围");
                return;
            }
            
            // 更新标题
            Volume volume = volumes.get(volumeIndex);
            volume.setTitle(newTitle);
            
            // 保存structure
            String updatedStructure = JsonUtils.toJson(volumes);
            repository.updateStoryStructure(storyId, updatedStructure);
            
            android.util.Log.d("AgentCommandExecutor", "已同步卷标题到structure：第" + (volumeIndex + 1) + "卷 -> " + newTitle);
        } catch (Exception e) {
            e.printStackTrace();
            android.util.Log.e("AgentCommandExecutor", "同步标题时出错：" + e.getMessage());
        }
    }
    
    /**
     * 同步章节标题到structure字段
     * 确保目录和写作区显示的章节标题与大纲一致
     */
    private void syncChapterTitleToStructure(int storyId, int volumeIndex, int chapterIndex, String newTitle) {
        try {
            Story story = repository.getStoryById(storyId);
            if (story == null) {
                android.util.Log.e("AgentCommandExecutor", "同步章节标题失败：小说不存在");
                return;
            }
            
            // 解析structure
            String structureJson = story.getStructure();
            if (TextUtils.isEmpty(structureJson)) {
                android.util.Log.w("AgentCommandExecutor", "structure为空，无法同步章节标题");
                return;
            }
            
            List<Volume> volumes = JsonUtils.fromJson(structureJson,
                new com.google.gson.reflect.TypeToken<List<Volume>>(){}.getType());
            
            if (volumeIndex < 0 || volumeIndex >= volumes.size()) {
                android.util.Log.e("AgentCommandExecutor", "同步章节标题失败：卷索引超出范围");
                return;
            }
            
            Volume volume = volumes.get(volumeIndex);
            List<Chapter> chapters = volume.getChapters();
            
            if (chapterIndex < 0 || chapterIndex >= chapters.size()) {
                android.util.Log.e("AgentCommandExecutor", "同步章节标题失败：章节索引超出范围");
                return;
            }
            
            // 更新标题
            Chapter chapter = chapters.get(chapterIndex);
            chapter.setTitle(newTitle);
            
            // 保存structure
            String updatedStructure = JsonUtils.toJson(volumes);
            repository.updateStoryStructure(storyId, updatedStructure);
            
            android.util.Log.d("AgentCommandExecutor", "已同步章节标题到structure：第" + (volumeIndex + 1) + "卷第" + (chapterIndex + 1) + "章 -> " + newTitle);
        } catch (Exception e) {
            e.printStackTrace();
            android.util.Log.e("AgentCommandExecutor", "同步章节标题时出错：" + e.getMessage());
        }
    }
    
    /**
     * 处理审核报告命令
     * 将AI返回的审核结果格式化为可读的报告
     */
    private CommandResult handleReviewReport(Map<String, Object> params, int storyId) {
        try {
            if (params == null) {
                return CommandResult.error("❌ 审核结果为空");
            }
            
            // 打印原始 JSON
            com.google.gson.Gson gson = new com.google.gson.Gson();
            String rawJson = gson.toJson(params);
            android.util.Log.d("AgentCommandExecutor", "=== RAW JSON review_report ===");
            android.util.Log.d("AgentCommandExecutor", rawJson);
            
            // 提取评分
            Object overallScoreObj = params.get("overall_score");
            int score = 0;
            if (overallScoreObj instanceof Number) {
                score = ((Number) overallScoreObj).intValue();
            }
            
            // 构建报告
            StringBuilder report = new StringBuilder();
            report.append("📊 **小说审核报告**\n\n");
            
            // 总体评分
            String scoreLevel = getScoreLevel(score);
            report.append("【总体评分】").append(score).append("/100 - ").append(scoreLevel).append("\n\n");
            
            // 维度评分（动态遍历所有维度）
            Object dimensionScoresObj = params.get("dimension_scores");
            if (dimensionScoresObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> dimensionScores = (Map<String, Object>) dimensionScoresObj;
                if (!dimensionScores.isEmpty()) {
                    report.append("【维度评分】\n");
                    for (java.util.Map.Entry<String, Object> entry : dimensionScores.entrySet()) {
                        String dimension = entry.getKey();
                        Object scoreObj = entry.getValue();
                        String scoreStr = "-";
                        if (scoreObj instanceof Number) {
                            scoreStr = String.valueOf(((Number) scoreObj).intValue());
                        }
                        String displayName = getDimensionDisplayName(dimension);
                        report.append("- ").append(displayName).append(": ").append(scoreStr).append("/100\n");
                    }
                    report.append("\n");
                }
            }
            
            // 关键问题
            Object issuesObj = params.get("critical_issues");
            if (issuesObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> issuesList = (List<Object>) issuesObj;
                if (!issuesList.isEmpty()) {
                    report.append("⚠️ 【发现的问题】共").append(issuesList.size()).append("个\n\n");
                    for (int i = 0; i < issuesList.size(); i++) {
                        Object issueObj = issuesList.get(i);
                        
                        String severity = "";
                        String description = "";
                        String location = "";
                        
                        if (issueObj instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> issue = (Map<String, Object>) issueObj;
                            severity = objectToString(issue.get("severity"));
                            description = objectToString(issue.get("description"));
                            location = objectToString(issue.get("location"));
                        } else if (issueObj != null) {
                            description = issueObj.toString();
                        }
                        
                        String severityIcon = "🔴";
                        if ("medium".equals(severity) || "major".equals(severity)) {
                            severityIcon = "🟡";
                        } else if ("low".equals(severity) || "minor".equals(severity)) {
                            severityIcon = "🟢";
                        }
                        
                        if (!description.isEmpty()) {
                            report.append(severityIcon).append(" **问题").append(i + 1).append("**");
                            if (!location.isEmpty()) {
                                report.append("（").append(location).append("）");
                            }
                            report.append("\n");
                            report.append(description).append("\n\n");
                        }
                    }
                }
            } else {
                report.append("✅ **未发现明显问题**\n\n");
            }
            
            // 改进建议
            Object suggestionsObj = params.get("suggestions");
            if (suggestionsObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> suggestionsList = (List<Object>) suggestionsObj;
                if (!suggestionsList.isEmpty()) {
                    report.append("💡 【改进建议】\n");
                    for (int i = 0; i < suggestionsList.size(); i++) {
                        Object item = suggestionsList.get(i);
                        if (item instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> suggestion = (Map<String, Object>) item;
                            String action = objectToString(suggestion.get("action"));
                            String description = objectToString(suggestion.get("description"));
                            String location = objectToString(suggestion.get("location"));
                            
                            report.append((i + 1)).append(". ");
                            if (!action.isEmpty()) report.append("[").append(action).append("] ");
                            if (!location.isEmpty()) report.append("位置：").append(location).append("\n");
                            if (!description.isEmpty()) report.append("   ").append(description).append("\n");
                        } else if (item != null) {
                            report.append((i + 1)).append(". ").append(item.toString()).append("\n");
                        }
                    }
                    report.append("\n");
                }
            }
            
            // 优点
            Object strengthsObj = params.get("strengths");
            if (strengthsObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> strengthsList = (List<Object>) strengthsObj;
                if (!strengthsList.isEmpty()) {
                    report.append("✨ 【优点】\n\n");
                    for (int i = 0; i < strengthsList.size(); i++) {
                        Object item = strengthsList.get(i);
                        report.append("- ").append(item != null ? item.toString() : "").append("\n");
                    }
                    report.append("\n");
                }
            }
            
            return CommandResult.success(report.toString(), "review_report", params);
            
        } catch (Exception e) {
            e.printStackTrace();
            return CommandResult.error("❌ 生成审核报告失败：" + e.getMessage());
        }
    }
    
    /**
     * 处理定向审核命令
     * 针对特定方面进行分析，范围由AI自行判断
     */
    @SuppressWarnings("unchecked")
    private CommandResult handleReviewAspect(Map<String, Object> params, int storyId) {
        try {
            if (params == null) {
                return CommandResult.error("❌ 定向审核参数为空");
            }
            
            // 打印原始 JSON（未处理的完整参数）
            com.google.gson.Gson gson = new com.google.gson.Gson();
            String rawJson = gson.toJson(params);
            android.util.Log.d("AgentCommandExecutor", "=== RAW JSON ===");
            android.util.Log.d("AgentCommandExecutor", rawJson);
            
            // 提取基本参数（aspects 必需，target_scope 可选）
            String targetScope = objectToString(params.get("target_scope"));
            List<String> aspects = new ArrayList<>();
            
            if (params.containsKey("aspects") && params.get("aspects") != null) {
                Object aspectsObj = params.get("aspects");
                if (aspectsObj instanceof List) {
                    // 安全遍历 aspects，处理可能的 LinkedTreeMap 元素
                    for (Object item : (List<?>) aspectsObj) {
                        if (item instanceof String) {
                            aspects.add((String) item);
                        } else if (item != null) {
                            // 处理对象类型，提取第一个字段作为 aspect
                            if (item instanceof Map) {
                                Map<?, ?> map = (Map<?, ?>) item;
                                if (!map.isEmpty()) {
                                    Object firstValue = map.values().iterator().next();
                                    if (firstValue != null) {
                                        aspects.add(firstValue.toString());
                                    }
                                }
                            } else {
                                aspects.add(item.toString());
                            }
                        }
                    }
                }
            }
            
            // 验证参数
            if (aspects.isEmpty()) {
                return CommandResult.error("❌ 请指定要审核的方面（如 pacing、conflict_strength）");
            }
            
            // 构建报告
            StringBuilder report = new StringBuilder();
            report.append("📋 **定向审核报告**\n\n");
            if (!targetScope.isEmpty()) {
                report.append("📖 ").append(targetScope).append("\n\n");
            }
            report.append("🎯 审核维度：").append(String.join("、", aspects)).append("\n\n");
            
            // 评分
            Map<String, Object> scores = (Map<String, Object>) params.get("scores");
            if (scores != null && !scores.isEmpty()) {
                report.append("【维度评分】\n\n");
                for (String aspect : aspects) {
                    // 安全获取评分（处理 LinkedTreeMap key 的情况）
                    Object scoreValue = scores.get(aspect);
                    if (scoreValue != null) {
                        int score = 0;
                        try {
                            score = ((Number) scoreValue).intValue();
                        } catch (Exception e) {
                            // 忽略转换错误
                        }
                        String aspectName = getAspectDisplayName(aspect);
                        String scoreEmoji = getScoreEmoji(score);
                        report.append(scoreEmoji).append(" ")
                              .append(aspectName).append(": ")
                              .append(score).append("/100\n");
                    }
                }
                report.append("\n");
            }
            
            // 分析详情
            Map<String, Object> analysis = (Map<String, Object>) params.get("analysis");
            if (analysis != null && !analysis.isEmpty()) {
                report.append("【分析详情】\n\n");
                for (String aspect : aspects) {
                    Object analysisObj = analysis.get(aspect);
                    if (analysisObj != null) {
                        String analysisText = analysisObj.toString();
                        String aspectName = getAspectDisplayName(aspect);
                        report.append("▶ ").append(aspectName).append("：\n");
                        report.append("  ").append(analysisText).append("\n\n");
                    }
                }
            }
            
            // 调试日志：打印原始 parameters
            android.util.Log.d("AgentCommandExecutor", "=== handleReviewAspect params ===");
            android.util.Log.d("AgentCommandExecutor", "aspects: " + aspects);
            
            // 详细发现
            Object findingsObj = params.get("detailed_findings");
            android.util.Log.d("AgentCommandExecutor", "detailed_findings type: " + (findingsObj != null ? findingsObj.getClass().getName() : "null"));
            android.util.Log.d("AgentCommandExecutor", "detailed_findings value: " + findingsObj);
            
            if (findingsObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> findingsList = (List<Object>) findingsObj;
                
                android.util.Log.d("AgentCommandExecutor", "findingsList size: " + findingsList.size());
                
                // 先统计有内容的问题数量
                int validFindingsCount = 0;
                for (int i = 0; i < findingsList.size(); i++) {
                    Object findingObj = findingsList.get(i);
                    android.util.Log.d("AgentCommandExecutor", "finding[" + i + "] type: " + (findingObj != null ? findingObj.getClass().getName() : "null"));
                    android.util.Log.d("AgentCommandExecutor", "finding[" + i + "] value: " + findingObj);
                    
                    if (findingObj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> finding = (Map<String, Object>) findingObj;
                        // AI 返回的字段是 "finding"，不是 "issue"
                        Object findingTextObj = finding.get("finding");
                        Object dimensionObj = finding.get("dimension");
                        String findingText = objectToString(findingTextObj);
                        String dimension = objectToString(dimensionObj);
                        android.util.Log.d("AgentCommandExecutor", "finding[" + i + "] dimension: '" + dimension + "', finding: '" + findingText + "'");
                        if (!findingText.isEmpty()) {
                            validFindingsCount++;
                        }
                    } else if (findingObj != null) {
                        String issue = findingObj.toString();
                        android.util.Log.d("AgentCommandExecutor", "finding[" + i + "] issue (non-Map): '" + issue + "'");
                        if (!issue.isEmpty()) {
                            validFindingsCount++;
                        }
                    }
                }
                
                // 只有存在有效问题时才显示标题和内容
                if (validFindingsCount > 0) {
                    report.append("🔍 【具体发现】\n\n");
                    int problemNum = 0;
                    for (int i = 0; i < findingsList.size(); i++) {
                        Object findingObj = findingsList.get(i);
                                        
                        String dimension = "";
                        String findingText = "";
                                        
                        if (findingObj instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> finding = (Map<String, Object>) findingObj;
                            Object dimensionObj = finding.get("dimension");
                            Object findingTextObj = finding.get("finding");
                                            
                            dimension = objectToString(dimensionObj);
                            findingText = objectToString(findingTextObj);
                        } else if (findingObj != null) {
                            findingText = findingObj.toString();
                        }
                                        
                        // 只显示有内容的问题
                        if (!findingText.isEmpty()) {
                            problemNum++;
                            report.append("问题").append(problemNum).append("：").append(findingText).append("\n\n");
                            if (!dimension.isEmpty()) {
                                report.append("  📌 维度：").append(dimension).append("\n");
                            }
                            report.append("\n");
                        }
                    }
                }
            }
            
            // 改进建议
            Object suggestionsObj = params.get("suggestions");
            android.util.Log.d("AgentCommandExecutor", "suggestions type: " + (suggestionsObj != null ? suggestionsObj.getClass().getName() : "null"));
            android.util.Log.d("AgentCommandExecutor", "suggestions value: " + suggestionsObj);
            
            if (suggestionsObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> suggestions = (List<Object>) suggestionsObj;
                android.util.Log.d("AgentCommandExecutor", "suggestions size: " + suggestions.size());
                
                if (!suggestions.isEmpty()) {
                    report.append("💡 【改进建议】\n");
                    for (int i = 0; i < suggestions.size(); i++) {
                        Object item = suggestions.get(i);
                        android.util.Log.d("AgentCommandExecutor", "suggestion[" + i + "] type: " + (item != null ? item.getClass().getName() : "null"));
                        android.util.Log.d("AgentCommandExecutor", "suggestion[" + i + "] value: " + item);
                        
                        if (item instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> suggestion = (Map<String, Object>) item;
                            Object action = suggestion.get("action");
                            Object location = suggestion.get("location");
                            Object description = suggestion.get("description");
                            Object expectedResult = suggestion.get("expected_result");
                            
                            android.util.Log.d("AgentCommandExecutor", "suggestion[" + i + "] action: " + action + ", location: " + location);
                            
                            report.append((i + 1)).append(". ");
                            if (action != null) report.append("[").append(action).append("] ");
                            if (location != null) report.append("位置：").append(location).append("\n");
                            if (description != null) report.append("描述：").append(description).append("\n");
                            if (expectedResult != null) report.append("预期：").append(expectedResult).append("\n\n");
                        } else if (item != null) {
                            report.append((i + 1)).append(". ").append(item.toString()).append("\n");
                        }
                    }
                    report.append("\n");
                }
            }
            
            return CommandResult.success(report.toString(), "review_aspect", params);
            
        } catch (Exception e) {
            e.printStackTrace();
            return CommandResult.error("❌ 生成定向审核报告失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取维度显示名称
     */
    private String getAspectDisplayName(String aspect) {
        switch (aspect) {
            case "pacing": return "节奏";
            case "conflict_strength": return "冲突强度";
            case "emotional_impact": return "情感冲击";
            case "suspense": return "悬念";
            case "consistency": return "一致性";
            case "dialogue": return "对话质量";
            case "description": return "描写质量";
            case "character": return "人设一致性";
            case "foreshadowing": return "伏笔处理";
            case "foreshadowing_handling": return "伏笔处理";
            case "character_arc": return "角色弧线";
            default: return aspect;
        }
    }
    
    /**
     * 获取维度显示名称（用于 review_report 的 dimension_scores）
     */
    private String getDimensionDisplayName(String dimension) {
        switch (dimension) {
            case "consistency": return "一致性";
            case "emotional_impact": return "情感冲击";
            case "conflict_strength": return "冲突强度";
            case "suspense": return "悬念设置";
            case "pacing": return "节奏把控";
            case "foreshadowing_handling": return "伏笔处理";
            case "character_arc": return "角色弧线";
            default: return dimension;
        }
    }
    
    /**
     * 获取评分表情符号
     */
    private String getScoreEmoji(int score) {
        if (score >= 85) return "🟢";
        if (score >= 70) return "🟡";
        return "🔴";
    }
    
    /**
     * 获取评分等级描述
     */
    private String getScoreLevel(int score) {
        if (score >= 90) return "优秀";
        if (score >= 80) return "良好";
        if (score >= 70) return "中等";
        if (score >= 60) return "较差";
        return "不合格";
    }
    
    /**
     * 获取维度评分
     */
    private int getDimensionScore(Map<String, Object> scores, String key) {
        Number value = (Number) scores.get(key);
        return value != null ? value.intValue() : 0;
    }

    private String objectToString(Object obj) {
        if (obj == null) return "";
        if (obj instanceof String) return (String) obj;
        if (obj instanceof Number) return obj.toString();
        if (obj instanceof Map) {
            StringBuilder sb = new StringBuilder();
            Map<?, ?> map = (Map<?, ?>) obj;
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) sb.append("；");
                sb.append(entry.getKey()).append("：").append(entry.getValue());
                first = false;
            }
            return sb.toString();
        }
        return obj.toString();
    }
}