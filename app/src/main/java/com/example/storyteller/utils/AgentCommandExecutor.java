package com.example.storyteller.utils;

import android.text.TextUtils;
import com.example.storyteller.data.remote.ApiClient;
import com.example.storyteller.data.repository.StoryRepository;
import com.example.storyteller.model.Chapter;
import com.example.storyteller.model.Story;
import com.example.storyteller.model.Volume;
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

    public AgentCommandExecutor(StoryRepository repository) {
        this.repository = repository;
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
                
                case "generate_plot":
                    return handleGeneratePlot(command.parameters);
                
                case "create_character":
                    return handleCreateCharacter(command.parameters);
                
                case "answer_question":
                    // 只是回答问题，不执行操作
                    if (command.parameters != null && command.parameters.containsKey("response")) {
                        return CommandResult.success((String) command.parameters.get("response"));
                    }
                    return CommandResult.success("");
                
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
        result.volumeId = params.containsKey("volume_id") ? 
            ((Number) Objects.requireNonNull(params.get("volume_id"))).intValue() : 1;
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
        
        // 创建新卷
        Volume newVolume = new Volume(volumes.size() + 1, volumeParams.volumeTitle);
        volumes.add(newVolume);
        
        // 保存更新后的结构
        saveVolumesToStory(story, volumes);
        
        return CommandResult.success(
            "✅ 已成功添加卷：《" + volumeParams.volumeTitle + "》",
            "add_volume",
            newVolume
        );
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
        if (params.containsKey("position")) {
            int position = ((Number) params.get("position")).intValue();
            boolean insertAfter = !params.containsKey("insert_after") || 
                                 ((Boolean) params.get("insert_after"));
            
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
        int volumeId = params.containsKey("volume_id") ? 
            ((Number) params.get("volume_id")).intValue() : 1;
        int chapterId = params.containsKey("chapter_id") ? 
            ((Number) params.get("chapter_id")).intValue() : 1;
        
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
        int volumeId = params.containsKey("volume_id") ? 
            ((Number) params.get("volume_id")).intValue() : 1;
        
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
     * 处理生成情节建议
     */
    private CommandResult handleGeneratePlot(Map<String, Object> params) {
        if (params != null && params.containsKey("response")) {
            return CommandResult.success("情节建议：\n" + params.get("response"));
        }
        return CommandResult.error("无法生成情节建议");
    }

    /**
     * 处理创建角色
     */
    private CommandResult handleCreateCharacter(Map<String, Object> params) {
        // TODO: 实现角色创建逻辑
        return CommandResult.error("角色创建功能开发中...");
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
        int fromVolumeId = params.containsKey("from_volume_id") ? 
            ((Number) params.get("from_volume_id")).intValue() : 1;
        int fromChapterId = params.containsKey("from_chapter_id") ? 
            ((Number) params.get("from_chapter_id")).intValue() : 1;
        int toVolumeId = params.containsKey("to_volume_id") ? 
            ((Number) params.get("to_volume_id")).intValue() : fromVolumeId;
        int toPosition = params.containsKey("to_position") ? 
            ((Number) params.get("to_position")).intValue() : 1;
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
        int volumeId = params.containsKey("volume_id") ? 
            ((Number) params.get("volume_id")).intValue() : 1;
        
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
        for (Volume volume : volumes) {
            for (Chapter chapter : volume.getChapters()) {
                if (!TextUtils.isEmpty(chapter.getTitle())) {
                    fullContent.append("## ").append(chapter.getTitle()).append("\n\n");
                }
                if (!TextUtils.isEmpty(chapter.getContent())) {
                    fullContent.append(chapter.getContent()).append("\n\n");
                }
            }
        }
        story.setContent(fullContent.toString().trim());
        
        // 保存到数据库
        repository.updateStory(story);
    }

    /**
     * 构建当前小说的上下文信息（用于发送给 AI）
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
            
            context.append("\n");
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
