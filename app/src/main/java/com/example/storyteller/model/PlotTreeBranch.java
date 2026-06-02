package com.example.storyteller.model;
import java.util.ArrayList;
import java.util.List;
public class PlotTreeBranch {
    private int id;
    private String name;
    private String description;
    private boolean mainline;
    private int sourceBranchId;
    private int sourceEventId;
    private long createTime;
    private long updateTime;
    private List<PlotTreeEvent> events;
    public PlotTreeBranch() {
        long now = System.currentTimeMillis();
        createTime = now;
        updateTime = now;
        events = new ArrayList<>();
    }
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public boolean isMainline() { return mainline; }
    public void setMainline(boolean mainline) { this.mainline = mainline; }
    public int getSourceBranchId() { return sourceBranchId; }
    public void setSourceBranchId(int sourceBranchId) { this.sourceBranchId = sourceBranchId; }
    public int getSourceEventId() { return sourceEventId; }
    public void setSourceEventId(int sourceEventId) { this.sourceEventId = sourceEventId; }
    public long getCreateTime() { return createTime; }
    public void setCreateTime(long createTime) { this.createTime = createTime; }
    public long getUpdateTime() { return updateTime; }
    public void setUpdateTime(long updateTime) { this.updateTime = updateTime; }
    public List<PlotTreeEvent> getEvents() { return events; }
    public void setEvents(List<PlotTreeEvent> events) { this.events = events == null ? new ArrayList<>() : events; }
}
