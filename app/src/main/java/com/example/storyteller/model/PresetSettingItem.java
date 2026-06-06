package com.example.storyteller.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * 预设模板中的单条素材条目。
 *
 * <p>字段与 {@link StorySetting} 的核心字段一一对应；安装时由
 * {@code PresetTemplateManager} 补齐来源、时间戳、置信度等元数据后再写入数据库。</p>
 */
public class PresetSettingItem {

    @SerializedName("category")
    private String category;

    @SerializedName("subCategory")
    private String subCategory;

    @SerializedName("title")
    private String title;

    @SerializedName("summary")
    private String summary;

    @SerializedName("detail")
    private String detail;

    @SerializedName("tags")
    private List<String> tags;

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSubCategory() {
        return subCategory;
    }

    public void setSubCategory(String subCategory) {
        this.subCategory = subCategory;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }
}
