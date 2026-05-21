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
}