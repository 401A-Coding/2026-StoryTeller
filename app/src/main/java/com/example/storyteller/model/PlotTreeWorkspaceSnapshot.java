package com.example.storyteller.model;
import java.util.ArrayList;
import java.util.List;
public class PlotTreeWorkspaceSnapshot {
    private int schemaVersion;
    private int activeBranchId;
    private int nextBranchId;
    private int nextEventId;
    private long updateTime;
    private List<PlotTreeBranch> branches;
    public PlotTreeWorkspaceSnapshot() {
        schemaVersion = 1;
        activeBranchId = 1;
        nextBranchId = 2;
        nextEventId = 1;
        updateTime = System.currentTimeMillis();
        branches = new ArrayList<>();
    }
    public int getSchemaVersion() { return schemaVersion; }
    public void setSchemaVersion(int schemaVersion) { this.schemaVersion = schemaVersion; }
    public int getActiveBranchId() { return activeBranchId; }
    public void setActiveBranchId(int activeBranchId) { this.activeBranchId = activeBranchId; }
    public int getNextBranchId() { return nextBranchId; }
    public void setNextBranchId(int nextBranchId) { this.nextBranchId = nextBranchId; }
    public int getNextEventId() { return nextEventId; }
    public void setNextEventId(int nextEventId) { this.nextEventId = nextEventId; }
    public long getUpdateTime() { return updateTime; }
    public void setUpdateTime(long updateTime) { this.updateTime = updateTime; }
    public List<PlotTreeBranch> getBranches() { return branches; }
    public void setBranches(List<PlotTreeBranch> branches) { this.branches = branches == null ? new ArrayList<>() : branches; }
}
