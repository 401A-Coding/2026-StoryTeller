package com.example.storyteller.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * 预设模板清单项（来自 assets/presets/_index.json）
 *
 * <p>仅包含模板的元信息，用于在"模板中心"列表中展示，
 * 不含具体素材内容；具体内容需通过 {@code PresetTemplateManager.loadTemplate(id)} 按需加载。</p>
 */
public class PresetTemplateIndex {

    @SerializedName("id")
    private String id;

    @SerializedName("name")
    private String name;

    @SerializedName("description")
    private String description;

    @SerializedName("featured")
    private boolean featured;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isFeatured() {
        return featured;
    }

    public void setFeatured(boolean featured) {
        this.featured = featured;
    }

    /**
     * 索引文件根结构
     */
    public static class IndexFile {
        @SerializedName("templates")
        private List<PresetTemplateIndex> templates;

        public List<PresetTemplateIndex> getTemplates() {
            return templates;
        }

        public void setTemplates(List<PresetTemplateIndex> templates) {
            this.templates = templates;
        }
    }
}
