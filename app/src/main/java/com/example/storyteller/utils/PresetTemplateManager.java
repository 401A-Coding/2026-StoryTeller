package com.example.storyteller.utils;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import com.example.storyteller.data.local.db.StorySettingDao;
import com.example.storyteller.model.PresetSettingItem;
import com.example.storyteller.model.PresetTemplate;
import com.example.storyteller.model.PresetTemplateIndex;
import com.example.storyteller.model.StorySetting;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 预设模板管理器。
 *
 * <p>负责：</p>
 * <ul>
 *   <li>扫描 assets/presets/ 目录列出所有可用模板</li>
 *   <li>按 ID 加载模板的完整定义</li>
 *   <li>将模板内容安装到指定 storyId 的素材库</li>
 *   <li>查询模板安装状态、版本对比</li>
 *   <li>卸载已安装的模板</li>
 *   <li>迁移旧版预存素材的模板标识</li>
 * </ul>
 */
public class PresetTemplateManager {

    private static final String TAG = "PresetTemplateManager";

    /** assets 下预设模板目录 */
    private static final String PRESETS_DIR = "presets";

    /** 索引文件名 */
    private static final String INDEX_FILE = "presets/_index.json";

    /** 预设素材 sourceType 标记，用于与爬取/AI 提取的素材区分 */
    public static final String SOURCE_TYPE_PRESET = "preset_template";

    /** 旧版预存素材的默认模板ID（用于数据迁移时回填） */
    public static final String LEGACY_DEFAULT_TEMPLATE_ID = "cosmic_horror_v1";

    private final Context context;
    private final Gson gson;
    private final StorySettingDao settingDao;

    // 模板缓存：assets 目录下的模板只读，进程内一次解析永久有效，
    // 避免每次 listInstalledStates() 重复读 5 个 JSON + Gson 解析。
    private final Map<String, PresetTemplate> templateCache = new HashMap<>();

    public PresetTemplateManager(Context context) {
        this.context = context.getApplicationContext();
        this.gson = new Gson();
        this.settingDao = new StorySettingDao(this.context);
    }

    // ===================== 模板发现 =====================

    /**
     * 列出 assets/presets/_index.json 中登记的所有模板清单。
     *
     * @return 模板清单（按 featured 优先，其次按 name 排序）；失败时返回空列表
     */
    public List<PresetTemplateIndex> listTemplates() {
        String json = readAssetFile(INDEX_FILE);
        if (json == null) {
            Log.w(TAG, "索引文件不存在: " + INDEX_FILE);
            return Collections.emptyList();
        }
        try {
            PresetTemplateIndex.IndexFile indexFile = gson.fromJson(
                    json, PresetTemplateIndex.IndexFile.class);
            if (indexFile == null || indexFile.getTemplates() == null) {
                return Collections.emptyList();
            }
            List<PresetTemplateIndex> list = new ArrayList<>(indexFile.getTemplates());
            // featured 置顶
            list.sort((a, b) -> Boolean.compare(b.isFeatured(), a.isFeatured()));
            return list;
        } catch (Exception e) {
            Log.e(TAG, "解析索引文件失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 加载指定模板的完整定义（包含所有 settings）。
     *
     * @param templateId 模板 ID（如 "cosmic_horror_v1"）
     * @return 模板对象；找不到或解析失败时返回 null
     */
    public PresetTemplate loadTemplate(String templateId) {
        if (templateId == null || templateId.isEmpty()) {
            return null;
        }
        // 优先从内存缓存读取，避免重复读 assets + Gson 解析
        PresetTemplate cached = templateCache.get(templateId);
        if (cached != null) {
            return cached;
        }
        String path = PRESETS_DIR + "/" + templateId + ".json";
        String json = readAssetFile(path);
        if (json == null) {
            Log.w(TAG, "模板文件不存在: " + path);
            return null;
        }
        try {
            PresetTemplate template = gson.fromJson(json, PresetTemplate.class);
            if (template != null) {
                templateCache.put(templateId, template);
            }
            return template;
        } catch (Exception e) {
            Log.e(TAG, "解析模板文件失败: " + path, e);
            return null;
        }
    }

    // ===================== 安装 =====================

    /**
     * 将模板内容安装到指定小说（storyId=0 表示全局素材库）。
     *
     * <p>当前固定使用 {@link InstallMode#SKIP_EXISTING}：
     * 目标小说中已存在同名素材时跳过该条目，不会创建副本，也不会覆盖。</p>
     *
     * @param template 模板对象
     * @param storyId  目标小说 ID（0 = 全局素材库）
     * @return 安装结果统计
     */
    public InstallResult install(PresetTemplate template, int storyId) {
        return install(template, storyId, InstallMode.SKIP_EXISTING);
    }

    /**
     * 将模板内容安装到指定小说（storyId=0 表示全局素材库）。
     *
     * @param template 模板对象
     * @param storyId  目标小说 ID（0 = 全局素材库）
     * @param mode     冲突处理模式
     * @return 安装结果统计
     */
    public InstallResult install(PresetTemplate template, int storyId, InstallMode mode) {
        InstallResult result = new InstallResult();
        if (template == null) {
            result.errorMessage = "模板为空";
            return result;
        }
        if (template.getSettings() == null || template.getSettings().isEmpty()) {
            result.errorMessage = "模板不包含任何素材";
            return result;
        }

        // 一次性查询目标小说现有素材标题，用于去重
        Set<String> existingTitles = new HashSet<>();
        List<StorySetting> existing = settingDao.getByStoryId(storyId);
        if (existing != null) {
            for (StorySetting s : existing) {
                if (s.getTitle() != null) {
                    existingTitles.add(s.getTitle());
                }
            }
        }

        result.templateId = template.getTemplateId();
        result.templateName = template.getTemplateName();
        result.total = template.getSettings().size();

        // OVERWRITE 模式：先清空该 templateId 在目标小说中的旧素材。
        // 注意——用户对旧素材的修改会丢失；如有顾虑可改用 SKIP_EXISTING 手动处理。
        if (mode == InstallMode.OVERWRITE) {
            result.replaced = settingDao.deleteByPresetTemplateId(
                    storyId, template.getTemplateId());
        }

        for (PresetSettingItem item : template.getSettings()) {
            try {
                String finalTitle = resolveConflictTitle(
                        item.getTitle(), existingTitles, mode, result);
                if (finalTitle == null) {
                    // 跳过模式且重名
                    result.skipped++;
                    continue;
                }

                StorySetting setting = buildSetting(template, item, finalTitle, storyId);
                long id = settingDao.insert(setting);
                if (id > 0) {
                    result.installed++;
                    existingTitles.add(finalTitle);
                } else {
                    result.failed++;
                }
            } catch (Exception e) {
                Log.e(TAG, "安装素材失败: " + item.getTitle(), e);
                result.failed++;
            }
        }

        return result;
    }

    // ===================== 状态查询 =====================

    /**
     * 查询指定小说中某模板的安装状态。
     *
     * @param templateId 模板ID；传 null 返回 {@link Status#UNKNOWN}
     * @param storyId    小说ID（0=全局素材库）
     */
    public TemplateInstallState getInstalledState(String templateId, int storyId) {
        TemplateInstallState state = new TemplateInstallState();
        state.templateId = templateId;
        state.storyId = storyId;
        if (templateId == null) {
            state.status = Status.UNKNOWN;
            return state;
        }
        PresetTemplate template = loadTemplate(templateId);
        if (template == null) {
            // 模板文件丢了，但素材仍在库中
            state.status = Status.UNKNOWN;
            return state;
        }
        state.templateName = template.getTemplateName();
        state.latestVersion = template.getVersion();
        state.installedVersion = settingDao.getMaxPresetVersion(storyId, templateId);
        state.installedCount = settingDao.getByPresetTemplateId(storyId, templateId).size();
        if (state.installedCount == 0) {
            state.status = Status.NOT_INSTALLED;
        } else if (state.installedVersion >= template.getVersion()) {
            state.status = Status.INSTALLED;
        } else {
            state.status = Status.HAS_UPDATE;
        }
        return state;
    }

    /**
     * 列出所有模板在指定小说中的安装状态（用于"模板中心"列表）。
     */
    public List<TemplateInstallState> listInstalledStates(int storyId) {
        List<TemplateInstallState> result = new ArrayList<>();
        List<PresetTemplateIndex> indexes = listTemplates();
        for (PresetTemplateIndex idx : indexes) {
            result.add(getInstalledState(idx.getId(), storyId));
        }
        return result;
    }

    // ===================== 卸载 =====================

    /**
     * 卸载指定小说中的某预设模板（删除属于该模板的全部素材）。
     *
     * @return 被删除的素材条数
     */
    public int uninstall(String templateId, int storyId) {
        if (templateId == null) {
            return 0;
        }
        return settingDao.deleteByPresetTemplateId(storyId, templateId);
    }

    // ===================== 数据迁移 =====================

    /**
     * 迁移旧版预存素材：将 {@code sourceType='preset_template'} 但
     * {@code preset_template_id} 为空的素材回填为 cosmic_horror_v1。
     *
     * <p>仅作用于全局素材库（story_id=0），避免影响其他来源素材。</p>
     *
     * @return 被修复的素材条数
     */
    public int migrateLegacyPresetMaterials() {
        List<StorySetting> globals = settingDao.getByStoryId(0);
        if (globals == null || globals.isEmpty()) {
            return 0;
        }
        int fixed = 0;
        for (StorySetting s : globals) {
            if (SOURCE_TYPE_PRESET.equals(s.getSourceType())
                    && (s.getPresetTemplateId() == null
                        || s.getPresetTemplateId().isEmpty())) {
                s.setPresetTemplateId(LEGACY_DEFAULT_TEMPLATE_ID);
                s.setPresetVersion(1);
                if (settingDao.update(s) > 0) {
                    fixed++;
                }
            }
        }
        return fixed;
    }

    /**
     * 处理标题冲突，返回最终使用的标题；返回 null 表示应跳过该条目。
     */
    private String resolveConflictTitle(String originalTitle,
                                        Set<String> existingTitles,
                                        InstallMode mode,
                                        InstallResult result) {
        if (originalTitle == null) {
            return null;
        }
        if (!existingTitles.contains(originalTitle)) {
            return originalTitle;
        }
        switch (mode) {
            case SKIP_EXISTING:
                return null;
            case RENAME:
                String renamed = originalTitle + " (副本)";
                // 极端情况下副本名也已存在，继续加 (副本) 后缀
                int suffix = 2;
                while (existingTitles.contains(renamed)) {
                    renamed = originalTitle + " (副本" + suffix + ")";
                    suffix++;
                }
                result.renamed++;
                return renamed;
            case OVERWRITE:
                // 旧素材已在 install() 入口处 deleteByPresetTemplateId 清空
                // 此处不会再有冲突，但仍返回原 title 以便后续插入
                return originalTitle;
            default:
                return originalTitle;
        }
    }

    /**
     * 将模板条目转换为可写入数据库的 StorySetting。
     */
    private StorySetting buildSetting(PresetTemplate template,
                                      PresetSettingItem item,
                                      String finalTitle,
                                      int storyId) {
        long now = System.currentTimeMillis();
        StorySetting setting = new StorySetting(storyId,
                item.getCategory(), item.getSubCategory(), finalTitle);
        setting.setSummary(item.getSummary());
        setting.setDetail(item.getDetail());
        setting.setTags(gson.toJson(item.getTags()));

        // 来源信息
        if (template.getSource() != null) {
            setting.setSourceType(SOURCE_TYPE_PRESET);
            setting.setSourceTitle(template.getTemplateName()
                    + " · " + (template.getSource().getTitle() != null
                            ? template.getSource().getTitle() : ""));
            // 仅在 URL 非空时设置，避免脏数据
            // （预设模板的 source.url 仅为“参考作品主页”，不是素材本身的出处；
            //   当前策略：未填则不写入，详情页“来源 URL”卡片会自动隐藏）
            String url = template.getSource().getUrl();
            if (url != null && !url.trim().isEmpty()) {
                setting.setSourceUrl(url.trim());
            }
        } else {
            setting.setSourceType(SOURCE_TYPE_PRESET);
            setting.setSourceTitle(template.getTemplateName());
        }

        // 预存素材不是 AI 提取，置信度置 0
        setting.setAiConfidence(0);
        // 预设模板标识（用于后续升级/卸载）
        setting.setPresetTemplateId(template.getTemplateId());
        setting.setPresetVersion(template.getVersion());
        setting.setCreateTime(now);
        setting.setUpdateTime(now);
        return setting;
    }

    // ===================== 工具方法 =====================

    /**
     * 读取 assets 下的文本文件，统一 UTF-8 编码。
     */
    private String readAssetFile(String path) {
        AssetManager am = context.getAssets();
        try (InputStream is = am.open(path);
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
            return sb.toString();
        } catch (IOException e) {
            // 静默吞掉 IOException 会让"找不到模板"这类问题排查极其困难
            // （症状就是模板中心状态显示为"未知"）
            Log.w(TAG, "读取 assets 失败: " + path, e);
            return null;
        }
    }

    // ===================== 内部类型 =====================

    /**
     * 冲突处理模式
     */
    public enum InstallMode {
        /** 目标小说中已存在同名素材时跳过 */
        SKIP_EXISTING,
        /** 目标小说中已存在同名素材时重命名为"原名 (副本)" */
        RENAME,
        /** 目标小说中已存在同名素材时覆盖（先清空该 templateId 的旧素材，再插入新素材） */
        OVERWRITE
    }

    /**
     * 安装结果统计
     */
    public static class InstallResult {
        public String templateId;
        public String templateName;
        public int total;
        public int installed;
        public int skipped;
        public int renamed;
        public int replaced;     // OVERWRITE 模式下被覆盖（删除后重新插入）的素材条数
        public int failed;
        public String errorMessage;

        public boolean isSuccess() {
            return errorMessage == null && installed > 0;
        }

        /**
         * 生成给用户看的简短摘要
         */
        public String summary() {
            if (errorMessage != null) {
                return errorMessage;
            }
            StringBuilder sb = new StringBuilder();
            sb.append("已安装 ").append(installed).append("/").append(total);
            if (replaced > 0) sb.append("，覆盖 ").append(replaced);
            if (skipped > 0) sb.append("，跳过 ").append(skipped);
            if (renamed > 0) sb.append("，重命名 ").append(renamed);
            if (failed > 0) sb.append("，失败 ").append(failed);
            return sb.toString();
        }
    }

    /**
     * 模板安装状态枚举
     */
    public enum Status {
        /** 模板不存在或无法识别 */
        UNKNOWN,
        /** 未安装 */
        NOT_INSTALLED,
        /** 已安装，且本地版本不低于模板最新版本 */
        INSTALLED,
        /** 已安装，但模板文件有更新版本 */
        HAS_UPDATE
    }

    /**
     * 模板安装状态结果，用于驱动"模板中心"列表的UI展示。
     */
    public static class TemplateInstallState {
        public String templateId;
        public String templateName;
        public int storyId;
        public int installedCount;      // 已安装素材条数
        public int installedVersion;    // 已安装的最高版本号；0=未安装
        public int latestVersion;       // 模板文件中的最新版本号
        public Status status;

        public boolean isNotInstalled() {
            return status == Status.NOT_INSTALLED;
        }

        public boolean isHasUpdate() {
            return status == Status.HAS_UPDATE;
        }

        public boolean isInstalled() {
            return status == Status.INSTALLED;
        }

        /**
         * 给UI层用的状态描述
         */
        public String getStatusLabel() {
            switch (status) {
                case NOT_INSTALLED:
                    return "未安装";
                case INSTALLED:
                    return "已安装 v" + installedVersion;
                case HAS_UPDATE:
                    return "可更新 v" + installedVersion + " → v" + latestVersion;
                case UNKNOWN:
                default:
                    return "未知";
            }
        }
    }
}
