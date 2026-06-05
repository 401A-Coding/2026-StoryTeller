package com.example.storyteller.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * 预设模板完整定义（来自 assets/presets/{templateId}.json）
 *
 * <p>包含模板元信息（id/name/version/source）以及具体素材条目列表。</p>
 */
public class PresetTemplate {

    @SerializedName("templateId")
    private String templateId;

    @SerializedName("templateName")
    private String templateName;

    @SerializedName("version")
    private int version;

    @SerializedName("description")
    private String description;

    @SerializedName("source")
    private Source source;

    @SerializedName("settings")
    private List<PresetSettingItem> settings;

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Source getSource() {
        return source;
    }

    public void setSource(Source source) {
        this.source = source;
    }

    public List<PresetSettingItem> getSettings() {
        return settings;
    }

    public void setSettings(List<PresetSettingItem> settings) {
        this.settings = settings;
    }

    /**
     * 模板来源元信息
     */
    public static class Source {
        @SerializedName("type")
        private String type;

        @SerializedName("title")
        private String title;

        @SerializedName("author")
        private String author;

        @SerializedName("url")
        private String url;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getAuthor() {
            return author;
        }

        public void setAuthor(String author) {
            this.author = author;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }
}
