package com.example.storyteller.model;

public class BehaviorLog {
    private int id;
    private String action;
    private int targetId;
    private String extra;
    private long createTime;

    public BehaviorLog(String action, int targetId, String extra, long createTime) {
        this.action = action;
        this.targetId = targetId;
        this.extra = extra;
        this.createTime = createTime;
    }

    public BehaviorLog(int id, String action, int targetId, String extra, long createTime) {
        this.id = id;
        this.action = action;
        this.targetId = targetId;
        this.extra = extra;
        this.createTime = createTime;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public int getTargetId() { return targetId; }
    public void setTargetId(int targetId) { this.targetId = targetId; }
    public String getExtra() { return extra; }
    public void setExtra(String extra) { this.extra = extra; }
    public long getCreateTime() { return createTime; }
    public void setCreateTime(long createTime) { this.createTime = createTime; }
}
