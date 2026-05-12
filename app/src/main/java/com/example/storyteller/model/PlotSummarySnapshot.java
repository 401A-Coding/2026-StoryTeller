package com.example.storyteller.model;

import java.util.ArrayList;
import java.util.List;

public class PlotSummarySnapshot {
    private int schemaVersion;
    private String model;
    private String detailLevel;
    private long generatedAt;
    private String generationPath;
    private String overviewSource;
    private String diagnosticsSummary;
    private int chunkCount;
    private int aiChapterCount;
    private int tolerantChapterCount;
    private int fallbackChapterCount;
    private String characterContext;
    private PlotOverviewSummary overview;
    private List<PlotChapterSummary> chapterSummaries;

    public PlotSummarySnapshot() {
        this.schemaVersion = 3;
        this.chapterSummaries = new ArrayList<>();
    }

    public int getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(int schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getDetailLevel() {
        return detailLevel;
    }

    public void setDetailLevel(String detailLevel) {
        this.detailLevel = detailLevel;
    }

    public long getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(long generatedAt) {
        this.generatedAt = generatedAt;
    }

    public String getGenerationPath() {
        return generationPath;
    }

    public void setGenerationPath(String generationPath) {
        this.generationPath = generationPath;
    }

    public String getOverviewSource() {
        return overviewSource;
    }

    public void setOverviewSource(String overviewSource) {
        this.overviewSource = overviewSource;
    }

    public String getDiagnosticsSummary() {
        return diagnosticsSummary;
    }

    public void setDiagnosticsSummary(String diagnosticsSummary) {
        this.diagnosticsSummary = diagnosticsSummary;
    }

    public int getChunkCount() {
        return chunkCount;
    }

    public void setChunkCount(int chunkCount) {
        this.chunkCount = chunkCount;
    }

    public int getAiChapterCount() {
        return aiChapterCount;
    }

    public void setAiChapterCount(int aiChapterCount) {
        this.aiChapterCount = aiChapterCount;
    }

    public int getTolerantChapterCount() {
        return tolerantChapterCount;
    }

    public void setTolerantChapterCount(int tolerantChapterCount) {
        this.tolerantChapterCount = tolerantChapterCount;
    }

    public int getFallbackChapterCount() {
        return fallbackChapterCount;
    }

    public void setFallbackChapterCount(int fallbackChapterCount) {
        this.fallbackChapterCount = fallbackChapterCount;
    }

    public String getCharacterContext() {
        return characterContext;
    }

    public void setCharacterContext(String characterContext) {
        this.characterContext = characterContext;
    }

    public PlotOverviewSummary getOverview() {
        return overview;
    }

    public void setOverview(PlotOverviewSummary overview) {
        this.overview = overview;
    }

    public List<PlotChapterSummary> getChapterSummaries() {
        return chapterSummaries;
    }

    public void setChapterSummaries(List<PlotChapterSummary> chapterSummaries) {
        this.chapterSummaries = chapterSummaries == null ? new ArrayList<>() : chapterSummaries;
    }
}

