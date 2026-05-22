package com.example.storyteller.utils;

import android.content.Context;
import com.example.storyteller.data.local.db.AiMemoryDao;
import com.example.storyteller.model.AiMemory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI记忆管理器
 * 单例模式，管理AI记忆的CRUD操作
 */
public class AiMemoryManager {
    
    private static AiMemoryManager instance;
    private final AiMemoryDao memoryDao;
    
    private AiMemoryManager(Context context) {
        this.memoryDao = new AiMemoryDao(context);
    }
    
    public static synchronized AiMemoryManager getInstance(Context context) {
        if (instance == null) {
            instance = new AiMemoryManager(context.getApplicationContext());
        }
        return instance;
    }
    
    /**
     * 添加记忆
     */
    public long addMemory(AiMemory memory) {
        memory.setCreatedAt(System.currentTimeMillis());
        memory.setUpdatedAt(System.currentTimeMillis());
        return memoryDao.insert(memory);
    }
    
    /**
     * 添加记忆（便捷方法）
     */
    public long addMemory(Integer storyId, String memoryType, String title, String content) {
        AiMemory memory = new AiMemory();
        memory.setStoryId(storyId);
        memory.setMemoryType(memoryType);
        memory.setTitle(title);
        memory.setContent(content);
        return addMemory(memory);
    }
    
    /**
     * 更新记忆
     */
    public boolean updateMemory(AiMemory memory) {
        memory.setUpdatedAt(System.currentTimeMillis());
        return memoryDao.update(memory) > 0;
    }
    
    /**
     * 删除记忆
     */
    public boolean deleteMemory(long id) {
        return memoryDao.delete(id) > 0;
    }
    
    /**
     * 获取记忆
     */
    public AiMemory getMemory(long id) {
        return memoryDao.getById(id);
    }
    
    /**
     * 根据ID获取记忆（别名方法）
     */
    public AiMemory getMemoryById(int id) {
        return memoryDao.getById((long) id);
    }
    
    /**
     * 获取某小说的所有记忆（包含全局记忆）
     */
    public List<AiMemory> getMemories(int storyId) {
        return memoryDao.getByStoryId(storyId);
    }
    
    /**
     * 获取所有全局记忆
     */
    public List<AiMemory> getGlobalMemories() {
        return memoryDao.getGlobalMemories();
    }
    
    /**
     * 获取某小说某类型的记忆
     */
    public List<AiMemory> getMemoriesByType(int storyId, String memoryType) {
        return memoryDao.getByType(storyId, memoryType);
    }
    
    /**
     * 删除某小说的所有记忆（不删除全局记忆）
     */
    public int clearStoryMemories(int storyId) {
        return memoryDao.deleteByStoryId(storyId);
    }
    
    /**
     * 删除所有记忆
     */
    public int clearAllMemories() {
        return memoryDao.deleteAll();
    }
    
    /**
     * 获取记忆数量
     */
    public int getMemoryCount(int storyId) {
        return memoryDao.getCount(storyId);
    }
    
    /**
     * 按类型分组获取记忆
     */
    public Map<String, List<AiMemory>> getMemoriesGroupedByType(int storyId) {
        List<AiMemory> memories = getMemories(storyId);
        Map<String, List<AiMemory>> grouped = new HashMap<>();
        
        for (AiMemory memory : memories) {
            String type = memory.getMemoryType();
            if (!grouped.containsKey(type)) {
                grouped.put(type, new ArrayList<>());
            }
            grouped.get(type).add(memory);
        }
        
        return grouped;
    }
    
    /**
     * 构建用于Prompt的记忆上下文
     */
    public String buildMemoryContext(int storyId) {
        List<AiMemory> memories = getMemories(storyId);
        
        if (memories.isEmpty()) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("\n【AI记忆】\n");
        
        // 按类型分组显示
        Map<String, List<AiMemory>> grouped = new HashMap<>();
        for (AiMemory memory : memories) {
            String type = memory.getMemoryType();
            if (!grouped.containsKey(type)) {
                grouped.put(type, new ArrayList<>());
            }
            grouped.get(type).add(memory);
        }
        
        // 按优先级排序输出
        if (grouped.containsKey(AiMemory.TYPE_PLOT)) {
            sb.append("📋 剧情类:\n");
            for (AiMemory m : grouped.get(AiMemory.TYPE_PLOT)) {
                sb.append("  - ").append(m.getTitle());
                if (m.getContent() != null && !m.getContent().isEmpty()) {
                    sb.append(": ").append(m.getContent());
                }
                sb.append("\n");
            }
            sb.append("\n");
        }
        
        if (grouped.containsKey(AiMemory.TYPE_PERSONALITY)) {
            sb.append("👤 人设类:\n");
            for (AiMemory m : grouped.get(AiMemory.TYPE_PERSONALITY)) {
                sb.append("  - ").append(m.getTitle());
                if (m.getContent() != null && !m.getContent().isEmpty()) {
                    sb.append(": ").append(m.getContent());
                }
                sb.append("\n");
            }
            sb.append("\n");
        }
        
        if (grouped.containsKey(AiMemory.TYPE_WORLD)) {
            sb.append("🌍 世界观类:\n");
            for (AiMemory m : grouped.get(AiMemory.TYPE_WORLD)) {
                sb.append("  - ").append(m.getTitle());
                if (m.getContent() != null && !m.getContent().isEmpty()) {
                    sb.append(": ").append(m.getContent());
                }
                sb.append("\n");
            }
            sb.append("\n");
        }
        
        if (grouped.containsKey(AiMemory.TYPE_OTHER)) {
            sb.append("📝 其他类:\n");
            for (AiMemory m : grouped.get(AiMemory.TYPE_OTHER)) {
                sb.append("  - ").append(m.getTitle());
                if (m.getContent() != null && !m.getContent().isEmpty()) {
                    sb.append(": ").append(m.getContent());
                }
                sb.append("\n");
            }
        }
        
        return sb.toString();
    }
    
    /**
     * 获取类型的显示名称
     */
    public static String getTypeDisplayName(String type) {
        switch (type) {
            case AiMemory.TYPE_PLOT:
                return "剧情类";
            case AiMemory.TYPE_PERSONALITY:
                return "人设类";
            case AiMemory.TYPE_WORLD:
                return "世界观类";
            case AiMemory.TYPE_OTHER:
            default:
                return "其他类";
        }
    }
    
    /**
     * 获取类型的图标
     */
    public static String getTypeIcon(String type) {
        switch (type) {
            case AiMemory.TYPE_PLOT:
                return "📋";
            case AiMemory.TYPE_PERSONALITY:
                return "👤";
            case AiMemory.TYPE_WORLD:
                return "🌍";
            case AiMemory.TYPE_OTHER:
            default:
                return "📝";
        }
    }
    
    /**
     * 截断文本到指定长度
     */
    private String truncateText(String text, int maxLength) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
    
    /**
     * 构建用于记忆提取的完整上下文
     * 包含：对话历史 + 角色列表 + 设定列表 + 大纲
     * 
     * @param conversationHistory 对话历史（由调用方提供）
     * @param storyId 小说ID
     * @param characterDao 角色DAO
     * @param settingDao 设定DAO
     * @param storyRepository 故事仓库
     * @return 完整的上下文字符串
     */
    public String buildFullContextForExtraction(
            String conversationHistory,
            int storyId,
            com.example.storyteller.data.local.db.CharacterDao characterDao,
            com.example.storyteller.data.local.db.StorySettingDao settingDao,
            com.example.storyteller.data.repository.StoryRepository storyRepository) {
        
        StringBuilder context = new StringBuilder();
        
        // 1. 对话历史
        if (conversationHistory != null && !conversationHistory.isEmpty()) {
            context.append("【对话历史】\n");
            context.append(conversationHistory);
            context.append("\n\n");
        } else {
            context.append("【对话历史】\n暂无对话历史\n\n");
        }
        
        // 2. 角色列表
        try {
            List<com.example.storyteller.model.Character> characters = characterDao.getCharactersByStoryId(storyId);
            context.append("【现有角色】\n");
            if (characters != null && !characters.isEmpty()) {
                for (com.example.storyteller.model.Character c : characters) {
                    context.append("- ").append(c.getName());
                    if (c.getProfile() != null && !c.getProfile().isEmpty()) {
                        String summary = truncateText(c.getProfile(), 200);
                        context.append(": ").append(summary);
                    }
                    context.append("\n");
                }
            } else {
                context.append("暂无角色\n");
            }
            context.append("\n");
        } catch (Exception e) {
            context.append("【现有角色】\n加载失败\n\n");
        }
        
        // 3. 设定列表
        try {
            List<com.example.storyteller.model.StorySetting> settings = settingDao.getByStoryId(storyId);
            context.append("【现有设定】\n");
            if (settings != null && !settings.isEmpty()) {
                for (com.example.storyteller.model.StorySetting s : settings) {
                    context.append("- [").append(s.getCategory()).append(" > ").append(s.getSubCategory()).append("] ");
                    context.append(s.getTitle());
                    if (s.getSummary() != null && !s.getSummary().isEmpty()) {
                        context.append(": ").append(truncateText(s.getSummary(), 150));
                    }
                    context.append("\n");
                }
            } else {
                context.append("暂无设定\n");
            }
            context.append("\n");
        } catch (Exception e) {
            context.append("【现有设定】\n加载失败\n\n");
        }
        
        // 4. 全局大纲
        try {
            com.example.storyteller.model.Story story = storyRepository.getStoryById(storyId);
            context.append("【故事大纲】\n");
            if (story != null && story.getGlobalOutline() != null && !story.getGlobalOutline().isEmpty()) {
                context.append(story.getGlobalOutline());
            } else {
                context.append("暂无全局大纲");
            }
            context.append("\n\n");
        } catch (Exception e) {
            context.append("【故事大纲】\n加载失败\n\n");
        }
        
        return context.toString();
    }
}