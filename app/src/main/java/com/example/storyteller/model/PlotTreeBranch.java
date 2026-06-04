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
    private int exportedStoryId;
    private List<PlotTreeEvent> events;
    private List<PlotTreeBranch> childBranches;
    private String childSummary;
    private int childStoryWordCount;
    private List<Integer> childBranchIds;

    public PlotTreeBranch() {
        long now = System.currentTimeMillis();
        createTime = now;
        updateTime = now;
        events = new ArrayList<>();
        childBranchIds = new ArrayList<>();
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
    public int getExportedStoryId() { return exportedStoryId; }
    public void setExportedStoryId(int exportedStoryId) { this.exportedStoryId = exportedStoryId; }
    public List<Integer> getChildBranchIds() {
        if (childBranchIds == null) childBranchIds = new ArrayList<>();
        return childBranchIds;
    }
    public void setChildBranchIds(List<Integer> childBranchIds) { this.childBranchIds = childBranchIds; }
    public boolean hasChildBranches() {
        return childBranchIds != null && !childBranchIds.isEmpty();
    }

    public List<PlotTreeBranch> getChildBranches() { return childBranches; }
    public void setChildBranches(List<PlotTreeBranch> childBranches) { this.childBranches = childBranches; }
    public String getChildSummary() { return childSummary; }
    public void setChildSummary(String childSummary) { this.childSummary = childSummary; }
    public int getChildStoryWordCount() { return childStoryWordCount; }
    public void setChildStoryWordCount(int childStoryWordCount) { this.childStoryWordCount = childStoryWordCount; }
    public List<PlotTreeEvent> getEvents() { return events; }
    public void setEvents(List<PlotTreeEvent> events) { this.events = events == null ? new ArrayList<>() : events; }
    public boolean hasExportedChild() { return exportedStoryId > 0; }
}