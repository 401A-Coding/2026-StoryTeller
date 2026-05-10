package com.example.storyteller.model;

public class Character {
    private int id;
    // 所属故事ID
    private int storyId;
    // 人物名称
    private String name;
    // 人物设定/简介
    private String profile;
    // 人物详细介绍（点击后查看）
    private String detail;
    // 人物图片（后续可存drawable ID或本地路径）
    private int avatarResId;

    public Character(int storyId, String name, String profile, int avatarResId) {
        this.storyId = storyId;
        this.name = name;
        this.profile = profile;
        this.detail = "";
        this.avatarResId = avatarResId;
    }

    public Character(int storyId, String name, String profile, String detail, int avatarResId) {
        this.storyId = storyId;
        this.name = name;
        this.profile = profile;
        this.detail = detail;
        this.avatarResId = avatarResId;
    }

    // Getter & Setter（自动生成即可）
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getStoryId() { return storyId; }
    public void setStoryId(int storyId) { this.storyId = storyId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getProfile() { return profile; }
    public void setProfile(String profile) { this.profile = profile; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public int getAvatarResId() { return avatarResId; }
    public void setAvatarResId(int avatarResId) { this.avatarResId = avatarResId; }
}