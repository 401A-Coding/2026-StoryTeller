package com.example.storyteller.ui.fragment;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;


import com.example.storyteller.R;
import com.example.storyteller.base.BaseFragment;
import com.example.storyteller.data.local.db.CharacterDao;
import com.example.storyteller.data.local.db.SettingRelationshipDao;
import com.example.storyteller.data.local.db.StoryDao;
import com.example.storyteller.data.local.db.StoryDocumentDao;
import com.example.storyteller.data.local.db.StorySettingDao;
import com.example.storyteller.data.remote.ApiClient;
import com.example.storyteller.model.Chapter;
import com.example.storyteller.model.Character;
import com.example.storyteller.model.PlotChapterSummary;
import com.example.storyteller.model.PlotSummarySnapshot;
import com.example.storyteller.model.PlotTreeBranch;
import com.example.storyteller.model.PlotTreeEvent;
import com.example.storyteller.model.PlotTreeWorkspaceSnapshot;
import com.example.storyteller.model.SettingRelationship;
import com.example.storyteller.model.Story;
import com.example.storyteller.model.StoryDocument;
import com.example.storyteller.model.StorySetting;
import com.example.storyteller.model.Volume;
import com.example.storyteller.ui.activity.StoryWorkspaceActivity;
import com.example.storyteller.ui.adapter.PlotTreeTimelineAdapter.Cell;
import com.example.storyteller.ui.adapter.PlotTreeTimelineAdapter.ColumnHeader;
import com.example.storyteller.ui.adapter.PlotTreeTimelineAdapter.ForkTarget;
import com.example.storyteller.ui.adapter.PlotTreeTimelineAdapter.TimelineRow;
import com.example.storyteller.utils.PromptManager;
import com.example.storyteller.utils.TaskType;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONObject;
public class StoryPlotTreeFragment extends BaseFragment {
    private static final String ARG_STORY_ID = "arg_story_id";
    private static final Type VOLUME_LIST_TYPE = new TypeToken<List<Volume>>() {}.getType();
    private static final int MODE_CURRENT_BRANCH = 0;
    private static final int MODE_ALL_BRANCHES = 1;
    private static final int MAX_BRANCH_COLS = 8;

    private static final String[] BRANCH_COLORS_HEX = {
        "#2196F3", "#4CAF50", "#9C27B0", "#FF9800", "#00BCD4", "#E91E63", "#3F51B5", "#009688"
    };
    private static final int[] BRANCH_COLORS_INT = {
        0xFF2196F3, 0xFF4CAF50, 0xFF9C27B0, 0xFFFF9800,
        0xFF00BCD4, 0xFFE91E63, 0xFF3F51B5, 0xFF009688
    };

    private TextView tvStoryTitle;
    private TextView tvBranchInfo;
    private HorizontalScrollView hsvTimeline;
    private PlotTreeCanvasView canvasPlotTree;

    private final Gson gson = new Gson();
    private PromptManager promptManager;
    private StoryDao storyDao;
    private CharacterDao characterDao;
    private StorySettingDao storySettingDao;
    private SettingRelationshipDao relationshipDao;
    private StoryDocumentDao documentDao;

    private Story currentStory;
    private PlotTreeWorkspaceSnapshot currentSnapshot;
    private PlotSummarySnapshot cachedPlotSummary;
    private int displayMode = MODE_ALL_BRANCHES;
    private int summaryGenerationToken = 0;
    private int storyId;

    public static StoryPlotTreeFragment newInstance(int storyId) {
        StoryPlotTreeFragment fragment = new StoryPlotTreeFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_STORY_ID, storyId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_story_plot_tree;
    }

    @Override
    protected void initView(View view) {
        tvStoryTitle = view.findViewById(R.id.tv_plot_tree_story_title);
        tvBranchInfo = view.findViewById(R.id.tv_plot_tree_branch_info);
        hsvTimeline = view.findViewById(R.id.hsv_timeline);
        canvasPlotTree = view.findViewById(R.id.canvas_plot_tree);

        view.findViewById(R.id.btn_switch_branch).setOnClickListener(this::showBranchMenu);
        view.findViewById(R.id.btn_ai_summary).setOnClickListener(v -> showAiSummaryDialog());
        view.findViewById(R.id.fab_refresh).setOnClickListener(v -> refreshPlotTree());
        view.findViewById(R.id.btn_overflow).setOnClickListener(this::showOverflowMenu);

        canvasPlotTree.setListener(new PlotTreeCanvasView.Listener() {
            @Override
            public void onCardClick(PlotTreeEvent event, int branchColor) {
                showCardDetailDialog(event);
            }
            @Override
            public void onForkNodeClick(PlotTreeEvent sourceEvent) {
                showForkNodeCreateDialog(sourceEvent);
            }
            @Override
            public void onDirectionClick(PlotTreeEvent event, int branchId, int directionIndex) {
                showDirectionEditDialog(event, branchId, directionIndex);
            }
            @Override
            public void onDirectionAddClick(int branchId, int branchColor) {
                showDirectionCreateDialog(branchId);
            }
        });
    }

    @Override
    protected void initData() {
        promptManager = new PromptManager(requireContext());
        storyDao = new StoryDao(requireContext());
        characterDao = new CharacterDao(requireContext());
        storySettingDao = new StorySettingDao(requireContext());
        relationshipDao = new SettingRelationshipDao(requireContext());
        documentDao = new StoryDocumentDao(requireContext());
        if (getArguments() != null) { storyId = getArguments().getInt(ARG_STORY_ID, -1); }
        loadStory();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (canvasPlotTree != null) canvasPlotTree.onThemeChanged();
        if (storyId > 0) loadStory();
    }
    private void showOverflowMenu(View anchor) {
        PopupMenu popup = new PopupMenu(requireContext(), anchor, Gravity.TOP | Gravity.END);
        popup.getMenu().add(0, 2, 0, "分支操作");
        popup.getMenu().add(0, 5, 0, "导出");
        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 2: showBranchActionDialog(); return true;
                case 5: showExportDialog(); return true;
            }
            return false;
        });
        popup.show();
    }

    private void showBranchMenu(View anchor) {
        PopupMenu popup = new PopupMenu(requireContext(), anchor, Gravity.TOP | Gravity.START);
        popup.getMenu().add(0, 1, 0, "切换分支");
        popup.getMenu().add(0, 2, 0, displayMode == MODE_ALL_BRANCHES ? "返回单分支" : "查看全部走向");
        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1: showBranchSwitchDialog(); return true;
                case 2: toggleDisplayMode(); return true;
            }
            return false;
        });
        popup.show();
    }

    private void loadStory() {
        currentStory = storyDao.getStoryById(storyId);
        cachedPlotSummary = null;
        if (currentStory == null) {
            tvStoryTitle.setText("剧情树");
            tvBranchInfo.setText("");
            return;
        }
        tvStoryTitle.setText("《" + currentStory.getTitle() + "》剧情树");
        currentSnapshot = loadOrCreateSnapshot(currentStory);
        loadAllBranchOverviews();
        refreshDisplay();
    }

    private PlotTreeWorkspaceSnapshot loadOrCreateSnapshot(Story story) {
        PlotTreeWorkspaceSnapshot snapshot = null;
        if (story != null && !TextUtils.isEmpty(story.getPlotTreeJson())) {
            try { snapshot = gson.fromJson(story.getPlotTreeJson(), PlotTreeWorkspaceSnapshot.class); }
            catch (Exception ignored) { snapshot = null; }
        }
        if (snapshot == null || snapshot.getBranches() == null || snapshot.getBranches().isEmpty()) {
            snapshot = buildInitialSnapshot(story);
            currentSnapshot = snapshot;
            persistSnapshot();
        }
        return snapshot;
    }

    private PlotTreeWorkspaceSnapshot buildInitialSnapshot(Story story) {
        PlotTreeWorkspaceSnapshot snapshot = new PlotTreeWorkspaceSnapshot();
        PlotTreeBranch mainline = new PlotTreeBranch();
        mainline.setId(1);
        mainline.setName("主线版本");
        mainline.setDescription("根据当前小说内容初始化的主线时间线");
        mainline.setMainline(true);
        mainline.setEvents(buildInitialEvents(story, snapshot));
        if (mainline.getEvents().isEmpty()) {
            mainline.getEvents().add(newEvent(snapshot, safeTitle(story), "先补充第一个关键剧情事件。"));
        }
        snapshot.getBranches().add(mainline);
        snapshot.setActiveBranchId(mainline.getId());
        snapshot.setUpdateTime(System.currentTimeMillis());
        return snapshot;
    }

    private List<PlotTreeEvent> buildInitialEvents(Story story, PlotTreeWorkspaceSnapshot workspace) {
        List<PlotTreeEvent> events = new ArrayList<>();
        if (story == null) return events;
        if (!TextUtils.isEmpty(story.getPlotSummaryJson())) {
            try {
                PlotSummarySnapshot ps = gson.fromJson(story.getPlotSummaryJson(), PlotSummarySnapshot.class);
                if (ps != null && ps.getChapterSummaries() != null) {
                    for (PlotChapterSummary cs : ps.getChapterSummaries()) {
                        if (cs == null) continue;
                        String title = !TextUtils.isEmpty(cs.getChapterTitle())
                                ? cs.getChapterTitle()
                                : (!TextUtils.isEmpty(cs.getChapterLabel()) ? cs.getChapterLabel() : "章节事件");
                        String summary = !TextUtils.isEmpty(cs.getBriefSummary())
                                ? cs.getBriefSummary()
                                : (!TextUtils.isEmpty(cs.getDetailSummary())
                                    ? trimText(cs.getDetailSummary(), 50) : "");
                        List<String> tags = new ArrayList<>();
                        if (cs.getKeyEvents() != null && !cs.getKeyEvents().isEmpty()) {
                            tags.addAll(cs.getKeyEvents());
                        }
                        PlotTreeEvent ev = newEvent(workspace, title, summary);
                        if (!tags.isEmpty()) ev.setTags(tags);
                        events.add(ev);
                    }
                }
            } catch (Exception ignored) {}
        }
        if (!events.isEmpty()) return events;
        List<Volume> volumes = parseStoryVolumes(story);
        for (Volume vol : volumes) {
            if (vol == null || vol.getChapters() == null) continue;
            for (Chapter ch : vol.getChapters()) {
                if (ch == null) continue;
                String title = TextUtils.isEmpty(ch.getTitle()) ? "章节事件" : ch.getTitle();
                String summary = TextUtils.isEmpty(ch.getContent()) ? "待补充" : trimText(ch.getContent(), 50);
                events.add(newEvent(workspace, title, summary));
            }
        }
        if (!events.isEmpty()) return events;
        if (!TextUtils.isEmpty(story.getDescription()))
            events.add(newEvent(workspace, safeTitle(story), trimText(story.getDescription(), 50)));
        return events;
    }

    private PlotTreeEvent newEvent(PlotTreeWorkspaceSnapshot ws, String title, String summary) {
        int id = ws.getNextEventId();
        ws.setNextEventId(id + 1);
        return new PlotTreeEvent(id, title, summary);
    }
    private void refreshDisplay() {
        if (currentSnapshot == null) return;
        PlotTreeBranch activeBranch = getActiveBranch();
        if (activeBranch != null) {
            int count = activeBranch.getEvents() != null ? activeBranch.getEvents().size() : 0;
            tvBranchInfo.setText("当前分支：" + safeText(activeBranch.getName()) + " · 共 " + count + " 个事件");
        }
        List<ColumnHeader> headers = new ArrayList<>();
        List<TimelineRow> rows;
        if (displayMode == MODE_CURRENT_BRANCH) {
            rows = buildSingleBranchTimeline(activeBranch, headers);
        } else {
            rows = buildAllBranchesTimeline(headers);
        }
        canvasPlotTree.setData(headers, rows);
        if (displayMode == MODE_ALL_BRANCHES && currentSnapshot != null) {
            int branchCount = currentSnapshot.getBranches() != null ? currentSnapshot.getBranches().size() : 0;
            tvBranchInfo.setText("全部走向 · 共 " + branchCount + " 个本地分支");
        }
    }

    private List<TimelineRow> buildSingleBranchTimeline(PlotTreeBranch branch, List<ColumnHeader> headers) {
        int branchId = branch != null ? branch.getId() : 0;
        headers.add(new ColumnHeader(safeText(branch != null ? branch.getName() : "主线"), BRANCH_COLORS_INT[0], "", "", branchId));
        List<TimelineRow> rows = new ArrayList<>();
        if (branch == null || branch.getEvents() == null) return rows;
        for (PlotTreeEvent event : branch.getEvents()) {
            rows.add(TimelineRow.shared(new Cell(event, BRANCH_COLORS_INT[0], branchId)));
        }
        // 未导出的非主线分支：在所有事件下方显示走向说明占位卡片
        if (!branch.isMainline() && !TextUtils.isEmpty(branch.getDescription())
                && !branch.hasExportedChild()) {
            List<Cell> cells = new ArrayList<>();
            cells.add(Cell.placeholder(branch.getDescription(), BRANCH_COLORS_INT[0]));
            rows.add(TimelineRow.split(cells));
        }
        return rows;
    }

    private List<TimelineRow> buildAllBranchesTimeline(List<ColumnHeader> headers) {
        List<TimelineRow> rows = new ArrayList<>();
        PlotTreeBranch mainline = getMainlineBranch();
        if (mainline == null || mainline.getEvents() == null || mainline.getEvents().isEmpty()) {
            headers.add(new ColumnHeader("暂无事件", BRANCH_COLORS_INT[0], ""));
            return rows;
        }
        List<PlotTreeEvent> mlEvents = mainline.getEvents();
        List<PlotTreeBranch> branchList = new ArrayList<>();
        Map<Integer, Integer> forkPosMap = new LinkedHashMap<>();
        for (PlotTreeBranch b : currentSnapshot.getBranches()) {
            if (b == null || b.isMainline()) continue;
            branchList.add(b);
            int forkPos = -1;
            for (int i = 0; i < mlEvents.size(); i++) {
                if (mlEvents.get(i).getId() == b.getSourceEventId()) { forkPos = i; break; }
            }
            // Fallback: match by title when event IDs were rebuilt
            if (forkPos < 0 && b.getEvents() != null && !b.getEvents().isEmpty()) {
                String forkTitle = null;
                for (PlotTreeEvent be : b.getEvents()) {
                    if (be.getId() == b.getSourceEventId()) { forkTitle = be.getTitle(); break; }
                }
                if (forkTitle != null && !forkTitle.isEmpty()) {
                    for (int i = 0; i < mlEvents.size(); i++) {
                        if (forkTitle.equals(mlEvents.get(i).getTitle())) { forkPos = i; break; }
                    }
                }
            }
            // Second fallback: find the last branch event title that matches mainline
            // (branch copies events up to the fork point, so the last match is closest to fork)
            if (forkPos < 0 && b.getEvents() != null && !b.getEvents().isEmpty()) {
                int bestMatch = -1;
                for (int ei = 0; ei < b.getEvents().size(); ei++) {
                    String evTitle = b.getEvents().get(ei).getTitle();
                    if (evTitle == null || evTitle.isEmpty()) continue;
                    for (int mi = 0; mi < mlEvents.size(); mi++) {
                        if (evTitle.equals(mlEvents.get(mi).getTitle())) {
                            bestMatch = Math.max(bestMatch, mi);
                            break;
                        }
                    }
                }
                if (bestMatch >= 0) forkPos = bestMatch;
            }
            // Third fallback: default to first mainline event position so branch is visible
            if (forkPos < 0) {
                forkPos = 0;
            }
            forkPosMap.put(b.getId(), forkPos);
        }
        if (branchList.isEmpty()) return buildSingleBranchTimeline(mainline, headers);

        // 分叉越靠下（forkPos越大）的分支离主线越近（col越小），形成树的视觉层次
        branchList.sort((a, b) -> {
            int fpA = forkPosMap.getOrDefault(a.getId(), 0);
            int fpB = forkPosMap.getOrDefault(b.getId(), 0);
            return Integer.compare(fpB, fpA);
        });

        Map<Integer, Integer> colMap = new LinkedHashMap<>();
        colMap.put(mainline.getId(), 0);
        int nextCol = 1;
        for (PlotTreeBranch b : branchList) {
            if (nextCol >= MAX_BRANCH_COLS) break;
            colMap.put(b.getId(), nextCol);
            nextCol++;
        }
        int totalCols = nextCol;

        headers.add(new ColumnHeader("主线版本", BRANCH_COLORS_INT[0], "", "", mainline.getId()));
        int colIdx = 1;
        for (PlotTreeBranch b : branchList) {
            if (colIdx >= MAX_BRANCH_COLS) break;
            int fp = forkPosMap.getOrDefault(b.getId(), -1);
            String origin = fp >= 0 && fp < mlEvents.size()
                    ? "分叉自：" + safeText(mlEvents.get(fp).getTitle()) : "";
            headers.add(new ColumnHeader(b.getName(), BRANCH_COLORS_INT[colIdx % BRANCH_COLORS_INT.length],
                    origin, b.getDescription(), b.getId()));
            colIdx++;
        }

        Set<Integer> forkPositions = new HashSet<>();
        for (int fp : forkPosMap.values()) { if (fp >= 0 && fp < mlEvents.size()) forkPositions.add(fp); }
        // DEBUG
        android.util.Log.d("PlotTree", "=== buildAllBranchesTimeline ===");
        android.util.Log.d("PlotTree", "mlEvents count: " + mlEvents.size());
        for (int ei = 0; ei < mlEvents.size(); ei++) {
            android.util.Log.d("PlotTree", "  mlEvent[" + ei + "] id=" + mlEvents.get(ei).getId() + " title=" + mlEvents.get(ei).getTitle());
        }
        for (PlotTreeBranch dbgB : branchList) {
            int dbgFp = forkPosMap.getOrDefault(dbgB.getId(), -1);
            int dbgEc = dbgB.getEvents() != null ? dbgB.getEvents().size() : 0;
            android.util.Log.d("PlotTree", "Branch id=" + dbgB.getId() + " name=" + dbgB.getName()
                    + " srcEvId=" + dbgB.getSourceEventId() + " forkPos=" + dbgFp + " evCount=" + dbgEc
                    + " desc=[" + dbgB.getDescription() + "]"
                    + " descEmpty=" + TextUtils.isEmpty(dbgB.getDescription())
                    + " exported=" + dbgB.hasExportedChild());
            if (dbgB.getEvents() != null) {
                for (int ei2 = 0; ei2 < dbgB.getEvents().size(); ei2++) {
                    android.util.Log.d("PlotTree", "  brEv[" + ei2 + "] id=" + dbgB.getEvents().get(ei2).getId() + " title=" + dbgB.getEvents().get(ei2).getTitle());
                }
            }
        }

        int maxBranchLen = 0;
        for (PlotTreeBranch b : branchList) {
            if (b.getEvents() != null) {
                int fp = forkPosMap.getOrDefault(b.getId(), -1);
                maxBranchLen = Math.max(maxBranchLen, b.getEvents().size());
            }
        }

        int maxDepth = Math.max(mlEvents.size(), maxBranchLen);
        // 为末尾分支的占位卡片预留行数（占位在 fp+1，可能超出 mlEvents.size()）
        int depthExtendCount = 0;
        for (PlotTreeBranch b : branchList) {
            int fp = forkPosMap.getOrDefault(b.getId(), -1);
            if (fp >= 0 && !TextUtils.isEmpty(b.getDescription()) && !b.hasExportedChild()) {
                int needed = fp + 2;
                if (needed > maxDepth) {
                    android.util.Log.d("PlotTree", "DEPTH_EXTEND branch=" + b.getName() + " fp=" + fp + " needed=" + needed + " oldMax=" + maxDepth);
                    maxDepth = needed;
                    depthExtendCount++;
                } else {
                    maxDepth = Math.max(maxDepth, needed);
                }
            }
        }
        android.util.Log.d("PlotTree", "maxDepth=" + maxDepth + " (ml=" + mlEvents.size() + " br=" + maxBranchLen + " extended=" + depthExtendCount + ")");
        // 跟踪已显示占位的分支，兜底确保所有未导出分支都可见
        Set<Integer> placeholderShown = new HashSet<>();
        for (int depth = 0; depth < maxDepth; depth++) {
            if (forkPositions.contains(depth)) {
                // 检测是否有分支的占位行（fp+1）恰好被此分叉深度吞掉
                boolean placeholderConflict = false;
                for (PlotTreeBranch b : branchList) {
                    int fp = forkPosMap.getOrDefault(b.getId(), -1);
                    if (fp >= 0 && depth == fp + 1 && !TextUtils.isEmpty(b.getDescription())
                            && !b.hasExportedChild()) {
                        placeholderConflict = true;
                        break;
                    }
                }
                if (!placeholderConflict) {
                    // 无冲突：使用标准FORK行并跳过后续SPLIT逻辑
                    PlotTreeEvent forkEvent = mlEvents.get(depth);
                    Cell cell = new Cell(forkEvent, BRANCH_COLORS_INT[0], mainline.getId());
                    List<ForkTarget> targets = new ArrayList<>();
                    for (PlotTreeBranch b : branchList) {
                        int fp = forkPosMap.getOrDefault(b.getId(), -1);
                        if (fp == depth) {
                            Integer bc = colMap.get(b.getId());
                            if (bc != null) {
                                targets.add(new ForkTarget(b.getName(), BRANCH_COLORS_INT[bc % BRANCH_COLORS_INT.length]));
                            }
                        }
                    }
                    rows.add(TimelineRow.fork(cell, targets.isEmpty() ? null : targets, forkEvent.getId()));
                    continue;
                }
                // 有冲突：使用SPLIT行（主线事件 + 分支占位共存），不跳过
                android.util.Log.d("PlotTree", "FORK_CONFLICT depth=" + depth + " using SPLIT row for placeholder");
            }

            List<Cell> cells = new ArrayList<>();
            int activeCount = 0;
            for (int c = 0; c < totalCols; c++) {
                if (c == 0) {
                    if (depth < mlEvents.size()) {
                        cells.add(new Cell(mlEvents.get(depth), BRANCH_COLORS_INT[0], mainline.getId()));
                        activeCount++;
                    } else {
                        cells.add(Cell.empty());
                    }
                } else {
                    PlotTreeBranch branchForCol = null;
                    for (PlotTreeBranch b : branchList) {
                        Integer bc = colMap.get(b.getId());
                        if (bc != null && bc == c) { branchForCol = b; break; }
                    }
                    if (branchForCol != null) {
                        int fp = forkPosMap.getOrDefault(branchForCol.getId(), -1);
                        int eventIdx = depth;
                        android.util.Log.d("PlotTree", "SPLIT depth=" + depth + " col=" + c + " fp=" + fp + " eIdx=" + eventIdx + " evSize=" + (branchForCol.getEvents() != null ? branchForCol.getEvents().size() : 0));
                        // 分支第一行：无真实事件时才显示走向说明占位卡片
                        boolean realEventAtFp1 = (fp >= 0 && depth == fp + 1 && eventIdx >= 0
                                && branchForCol.getEvents() != null
                                && eventIdx < branchForCol.getEvents().size());
                        if (fp >= 0 && depth == fp + 1 && !TextUtils.isEmpty(branchForCol.getDescription())
                                && !branchForCol.hasExportedChild() && !realEventAtFp1) {
                            cells.add(Cell.placeholder(branchForCol.getDescription(),
                                    BRANCH_COLORS_INT[c % BRANCH_COLORS_INT.length]));
                            activeCount++;
                            placeholderShown.add(branchForCol.getId());
                        } else if (fp >= 0 && depth > fp && eventIdx >= 0
                                && branchForCol.getEvents() != null
                                && eventIdx < branchForCol.getEvents().size()) {
                            cells.add(new Cell(branchForCol.getEvents().get(eventIdx),
                                    BRANCH_COLORS_INT[c % BRANCH_COLORS_INT.length], branchForCol.getId()));
                            activeCount++;
                        } else {
                            if (fp >= 0 && depth == fp + 1) {
                                android.util.Log.d("PlotTree", "NO_PLACEHOLDER branch=" + branchForCol.getName()
                                        + " fp=" + fp + " depth=" + depth
                                        + " descEmpty=" + TextUtils.isEmpty(branchForCol.getDescription())
                                        + " exported=" + branchForCol.hasExportedChild());
                            }
                            cells.add(Cell.empty());
                        }
                    } else {
                        cells.add(Cell.empty());
                    }
                }
            }
            TimelineRow splitRow = TimelineRow.split(cells);
            // 若此行是分叉深度（有占位冲突），携带forkTargets以绘制分支引导线
            if (forkPositions.contains(depth)) {
                List<ForkTarget> targets = new ArrayList<>();
                for (PlotTreeBranch b : branchList) {
                    int fp = forkPosMap.getOrDefault(b.getId(), -1);
                    if (fp == depth) {
                        Integer bc = colMap.get(b.getId());
                        if (bc != null) {
                            targets.add(new ForkTarget(b.getName(), BRANCH_COLORS_INT[bc % BRANCH_COLORS_INT.length]));
                        }
                    }
                }
                if (!targets.isEmpty()) splitRow.forkTargets = targets;
            }
            rows.add(splitRow);
        }
        // 兜底：为 forkPos 未能正确匹配主线、尚未显示占位的未导出分支在最底部添加占位行
        // 但如果分支在 fp+1 已有真实事件（如方向事件），则无需兜底
        android.util.Log.d("PlotTree", "FALLBACK_CHECK placeholderShown=" + placeholderShown);
        List<Cell> fallbackCells = new ArrayList<>();
        for (int c = 0; c < totalCols; c++) fallbackCells.add(Cell.empty());
        boolean hasFallback = false;
        for (PlotTreeBranch b : branchList) {
            if (!TextUtils.isEmpty(b.getDescription()) && !b.hasExportedChild()
                    && !placeholderShown.contains(b.getId())) {
                Integer bc = colMap.get(b.getId());
                // 分支在 fp+1 已有真实事件（如方向事件），无需兜底占位
                int fp = forkPosMap.getOrDefault(b.getId(), -1);
                if (fp >= 0 && b.getEvents() != null && fp + 1 < b.getEvents().size()) {
                    android.util.Log.d("PlotTree", "FALLBACK_SKIP branch=" + b.getName()
                            + " has real event at fp+1=" + (fp + 1) + " evSize=" + b.getEvents().size());
                    continue;
                }
                android.util.Log.d("PlotTree", "FALLBACK_CANDIDATE branch=" + b.getName()
                        + " desc=[" + b.getDescription() + "] col=" + bc);
                if (bc != null && bc < totalCols) {
                    fallbackCells.set(bc, Cell.placeholder(b.getDescription(),
                            BRANCH_COLORS_INT[bc % BRANCH_COLORS_INT.length]));
                    hasFallback = true;
                }
            }
        }
        if (hasFallback) {
            android.util.Log.d("PlotTree", "FALLBACK_ADDED extra row with placeholders");
            rows.add(TimelineRow.split(fallbackCells));
        } else {
            android.util.Log.d("PlotTree", "FALLBACK_NONE no extra row needed");
        }
        return rows;
    }
    private PlotSummarySnapshot loadPlotSummarySnapshot() {
        if (cachedPlotSummary != null) return cachedPlotSummary;
        if (currentStory == null || TextUtils.isEmpty(currentStory.getPlotSummaryJson())) return null;
        try { cachedPlotSummary = gson.fromJson(currentStory.getPlotSummaryJson(), PlotSummarySnapshot.class); }
        catch (Exception e) { cachedPlotSummary = null; }
        return cachedPlotSummary;
    }

    private void toggleDisplayMode() {
        displayMode = (displayMode == MODE_CURRENT_BRANCH) ? MODE_ALL_BRANCHES : MODE_CURRENT_BRANCH;
        if (displayMode == MODE_ALL_BRANCHES) loadAllBranchOverviews();
        refreshDisplay();
    }

    private void refreshPlotTree() {
        if (storyId <= 0) { Toast.makeText(requireContext(), "刷新失败：未加载作品", Toast.LENGTH_SHORT).show(); return; }
        currentStory = storyDao.getStoryById(storyId);
        cachedPlotSummary = null;
        if (currentStory == null) { loadStory(); return; }
        currentSnapshot = null;
        if (!TextUtils.isEmpty(currentStory.getPlotTreeJson())) {
            try { currentSnapshot = gson.fromJson(currentStory.getPlotTreeJson(), PlotTreeWorkspaceSnapshot.class); }
            catch (Exception e) { e.printStackTrace(); }
        }
        if (currentSnapshot == null || currentSnapshot.getBranches() == null || currentSnapshot.getBranches().isEmpty()) {
            currentSnapshot = buildInitialSnapshot(currentStory);
            persistSnapshot();
        }
        syncStoryMainline(currentStory);
        if (!TextUtils.isEmpty(currentStory.getPlotTreeJson())) {
            try { currentSnapshot = gson.fromJson(currentStory.getPlotTreeJson(), PlotTreeWorkspaceSnapshot.class); }
            catch (Exception e) { e.printStackTrace(); }
        }
        if (currentSnapshot != null && currentSnapshot.getBranches() != null) {
            for (PlotTreeBranch b : currentSnapshot.getBranches()) {
                if (b != null && b.hasExportedChild()) {
                    Story childStory = storyDao.getStoryById(b.getExportedStoryId());
                    if (childStory != null) syncStoryMainline(childStory);
                }
            }
        }
        loadAllBranchOverviews();
        refreshDisplay();
        Toast.makeText(requireContext(), "已刷新", Toast.LENGTH_SHORT).show();
    }

    private void syncStoryMainline(Story story) {
        if (story == null || TextUtils.isEmpty(story.getPlotTreeJson())) return;
        PlotTreeWorkspaceSnapshot ws = null;
        try { ws = gson.fromJson(story.getPlotTreeJson(), PlotTreeWorkspaceSnapshot.class); }
        catch (Exception e) { return; }
        if (ws == null || ws.getBranches() == null) return;
        PlotTreeBranch mainline = null;
        for (PlotTreeBranch b : ws.getBranches()) {
            if (b != null && b.isMainline()) { mainline = b; break; }
        }
        if (mainline == null || mainline.getEvents() == null) return;
        List<Volume> volumes = parseStoryVolumes(story);
        List<Chapter> allChapters = new ArrayList<>();
        for (Volume v : volumes) {
            if (v != null && v.getChapters() != null) allChapters.addAll(v.getChapters());
        }

        List<PlotTreeEvent> events = mainline.getEvents();

        // 分离常规事件和方向事件
        List<PlotTreeEvent> regularEvents = new ArrayList<>();
        List<PlotTreeEvent> directionEvents = new ArrayList<>();
        for (PlotTreeEvent ev : events) {
            if (ev.isDirection()) directionEvents.add(ev);
            else regularEvents.add(ev);
        }

        int regularCount = regularEvents.size();
        int totalChapters = allChapters.size();

        // 没有方向事件且常规事件已足够，无需同步
        if (directionEvents.isEmpty() && regularCount >= totalChapters) return;

        PlotSummarySnapshot ps = null;
        if (!TextUtils.isEmpty(story.getPlotSummaryJson())) {
            try { ps = gson.fromJson(story.getPlotSummaryJson(), PlotSummarySnapshot.class); }
            catch (Exception ignored) {}
        }

        // 构建新事件列表：保留常规事件 + 用章节替换方向事件 + 保留多余方向事件
        List<PlotTreeEvent> newEvents = new ArrayList<>(regularEvents);

        // 为每个"新"章节创建事件（替换对应位置的方向事件）
        for (int i = regularCount; i < totalChapters; i++) {
            Chapter ch = allChapters.get(i);
            String title;
            String summary;
            List<String> tags = new ArrayList<>();
            if (ps != null && ps.getChapterSummaries() != null && i < ps.getChapterSummaries().size()) {
                PlotChapterSummary cs = ps.getChapterSummaries().get(i);
                title = !TextUtils.isEmpty(cs.getChapterTitle()) ? cs.getChapterTitle()
                        : (!TextUtils.isEmpty(cs.getChapterLabel()) ? cs.getChapterLabel() : "章节事件");
                summary = !TextUtils.isEmpty(cs.getBriefSummary()) ? cs.getBriefSummary()
                        : (!TextUtils.isEmpty(cs.getDetailSummary()) ? trimText(cs.getDetailSummary(), 50) : "");
                if (cs.getKeyEvents() != null && !cs.getKeyEvents().isEmpty()) tags.addAll(cs.getKeyEvents());
            } else {
                title = TextUtils.isEmpty(ch.getTitle()) ? "章节事件" : ch.getTitle();
                summary = TextUtils.isEmpty(ch.getContent()) ? "待补充" : trimText(ch.getContent(), 50);
            }
            PlotTreeEvent ev = new PlotTreeEvent(ws.getNextEventId(), title, summary);
            ws.setNextEventId(ws.getNextEventId() + 1);
            if (!tags.isEmpty()) ev.setTags(tags);
            newEvents.add(ev);
            android.util.Log.d("PlotTree", "SYNC_REPLACE direction at pos " + i + " with chapter: " + title);
        }

        // 保留未被替换的方向事件（方向比章节多的情况）
        int consumedDirections = totalChapters - regularCount;
        if (consumedDirections < directionEvents.size()) {
            for (int i = consumedDirections; i < directionEvents.size(); i++) {
                newEvents.add(directionEvents.get(i));
            }
            android.util.Log.d("PlotTree", "SYNC_KEEP " + (directionEvents.size() - consumedDirections) + " remaining direction events");
        }

        mainline.setEvents(newEvents);
        ws.setUpdateTime(System.currentTimeMillis());
        story.setPlotTreeJson(gson.toJson(ws));
        storyDao.updatePlotTree(story.getId(), gson.toJson(ws));
    }


    private void refreshPlotSummary() {
        if (currentStory == null) return;
        currentStory = storyDao.getStoryById(storyId);
        cachedPlotSummary = null;
        if (currentStory == null) return;
        showAiSummaryDialog();
    }

    private void loadAllBranchOverviews() {
        if (currentSnapshot == null || currentSnapshot.getBranches() == null) return;
        for (PlotTreeBranch branch : currentSnapshot.getBranches()) {
            if (branch == null) continue;
            branch.setChildBranches(null);
            branch.setChildSummary(null);
            branch.setChildStoryWordCount(0);
            if (branch.hasExportedChild()) {
                Story childStory = storyDao.getStoryById(branch.getExportedStoryId());
                if (childStory != null) {
                    branch.setChildSummary(childStory.getTitle());
                    branch.setChildStoryWordCount(childStory.getWordCount());
                    // 合并而非覆盖：保留分叉点前的事件（ID正确用于fork检测），追加导出故事的新事件
                    syncExportedBranchEvents(branch, childStory);
                    branch.setChildBranches(loadStoryBranches(childStory));
                }
            }
        }
    }

    private List<PlotTreeBranch> loadStoryBranches(Story story) {
        List<PlotTreeBranch> result = new ArrayList<>();
        if (story == null || TextUtils.isEmpty(story.getPlotTreeJson())) return result;
        try {
            PlotTreeWorkspaceSnapshot snapshot = gson.fromJson(story.getPlotTreeJson(), PlotTreeWorkspaceSnapshot.class);
            if (snapshot != null && snapshot.getBranches() != null) {
                for (PlotTreeBranch branch : snapshot.getBranches()) {
                    if (branch != null) { branch.setChildBranches(null); branch.setChildSummary(null); result.add(branch); }
                }
            }
        } catch (Exception ignored) {}
        return result;
    }

    /**
     * 将导出故事的新事件合并到分支中。
     * 保留分叉点及之前的事件（ID 正确，用于 forkPos 检测），
     * 仅从导出故事追加分叉点之后的新事件。
     */
    private void syncExportedBranchEvents(PlotTreeBranch branch, Story exportedStory) {
        if (TextUtils.isEmpty(exportedStory.getPlotTreeJson())) return;
        if (branch.getEvents() == null) branch.setEvents(new ArrayList<>());
        try {
            PlotTreeWorkspaceSnapshot es = gson.fromJson(exportedStory.getPlotTreeJson(), PlotTreeWorkspaceSnapshot.class);
            if (es == null || es.getBranches() == null) return;
            PlotTreeBranch exMainline = null;
            for (PlotTreeBranch eb : es.getBranches()) {
                if (eb != null && eb.isMainline()) { exMainline = eb; break; }
            }
            if (exMainline == null || exMainline.getEvents() == null || exMainline.getEvents().isEmpty()) return;
            List<PlotTreeEvent> exEvents = exMainline.getEvents();

            // 定位分叉点：在分支当前事件中查找 sourceEventId 对应的事件
            int forkPos = -1;
            List<PlotTreeEvent> curEvents = branch.getEvents();
            if (curEvents != null && branch.getSourceEventId() > 0) {
                for (int i = 0; i < curEvents.size(); i++) {
                    if (curEvents.get(i).getId() == branch.getSourceEventId()) {
                        forkPos = i;
                        break;
                    }
                }
            }
            // 若找不到分叉点（旧数据无 sourceEventId），保留所有现有事件不做合并
            if (forkPos < 0) return;

            // 保留 forkPos+1 个原有事件（ID 正确），其余用导出故事的新事件替换
            List<PlotTreeEvent> merged = new ArrayList<>(curEvents.subList(0, Math.min(forkPos + 1, curEvents.size())));
            for (int i = forkPos + 1; i < exEvents.size(); i++) {
                merged.add(copyEvent(exEvents.get(i)));
            }
            branch.setEvents(merged);
        } catch (Exception ignored) {}
    }

    private PlotTreeBranch getMainlineBranch() {
        if (currentSnapshot == null || currentSnapshot.getBranches() == null) return null;
        for (PlotTreeBranch b : currentSnapshot.getBranches()) { if (b != null && b.isMainline()) return b; }
        return currentSnapshot.getBranches().isEmpty() ? null : currentSnapshot.getBranches().get(0);
    }

    private PlotTreeBranch getActiveBranch() {
        if (currentSnapshot == null || currentSnapshot.getBranches() == null) return null;
        for (PlotTreeBranch branch : currentSnapshot.getBranches()) {
            if (branch != null && branch.getId() == currentSnapshot.getActiveBranchId()) return branch;
        }
        return currentSnapshot.getBranches().isEmpty() ? null : currentSnapshot.getBranches().get(0);
    }

    private PlotTreeBranch findBranchById(int branchId) {
        if (currentSnapshot == null || currentSnapshot.getBranches() == null) return null;
        for (PlotTreeBranch b : currentSnapshot.getBranches()) { if (b != null && b.getId() == branchId) return b; }
        return null;
    }

    /** Find which branch owns the given event, and its position within that branch. */
    private PlotTreeBranch findEventOwner(PlotTreeEvent event, int[] outPos) {
        if (event == null || currentSnapshot == null || currentSnapshot.getBranches() == null) return null;
        for (PlotTreeBranch b : currentSnapshot.getBranches()) {
            if (b == null || b.getEvents() == null) continue;
            for (int i = 0; i < b.getEvents().size(); i++) {
                if (b.getEvents().get(i).getId() == event.getId()) {
                    outPos[0] = i;
                    return b;
                }
            }
        }
        return null;
    }
    private void showCardDetailDialog(PlotTreeEvent event) {
        if (event == null) return;
        int[] posHolder = new int[]{-1};
        PlotTreeBranch owner = findEventOwner(event, posHolder);
        if (owner == null) return;

        LinearLayout layout = buildFormLayout();
        EditText etTitle = buildEditText("事件标题");
        EditText etSummary = buildEditText("事件摘要 / 主线推进");
        EditText etNote = buildEditText("补充说明（可选）");
        EditText etTags = buildEditText("标签，使用顿号/逗号分隔（可选）");
        layout.addView(etTitle); layout.addView(etSummary); layout.addView(etNote); layout.addView(etTags);
        etTitle.setText(event.getTitle());
        etSummary.setText(event.getSummary());
        etNote.setText(event.getNote());
        etTags.setText(event.getTags() == null ? "" : TextUtils.join("、", event.getTags()));

        PlotTreeBranch ownerBranch = owner;

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext())
                .setTitle("剧情事件详情")
                .setView(layout).setNegativeButton("取消", null)
                .setPositiveButton("保存", (dialog, which) -> {
                    String title = trimToEmpty(etTitle.getText() == null ? null : etTitle.getText().toString());
                    String summary = trimToEmpty(etSummary.getText() == null ? null : etSummary.getText().toString());
                    if (TextUtils.isEmpty(title)) {
                        Toast.makeText(requireContext(), "请先输入事件标题", Toast.LENGTH_SHORT).show(); return;
                    }
                    event.setTitle(title); event.setSummary(summary);
                    event.setNote(trimToEmpty(etNote.getText() == null ? null : etNote.getText().toString()));
                    event.setTags(splitTags(etTags.getText() == null ? null : etTags.getText().toString()));
                    event.setUpdateTime(System.currentTimeMillis());
                    if (ownerBranch != null) ownerBranch.setUpdateTime(System.currentTimeMillis());
                    persistSnapshot(); loadAllBranchOverviews(); refreshDisplay();
                });

        // 全部走向模式下，提供跳转至该事件所属分支的入口
        if (displayMode == MODE_ALL_BRANCHES) {
            builder.setNeutralButton("查看此分支：" + safeText(ownerBranch.getName()), (dialog, which) -> {
                currentSnapshot.setActiveBranchId(ownerBranch.getId());
                persistSnapshot();
                displayMode = MODE_CURRENT_BRANCH;
                refreshDisplay();
            });
        }

        builder.show();
    }

    private void showForkNodeCreateDialog(PlotTreeEvent sourceEvent) {
        if (sourceEvent == null) return;
        // Fork node is always between mainline events — find the event's position
        int[] posHolder = new int[]{-1};
        PlotTreeBranch owner = findEventOwner(sourceEvent, posHolder);
        if (owner == null || posHolder[0] < 0) return;
        showCreateBranchDialog(sourceEvent, posHolder[0]);
    }

    private void showEventActionDialog(PlotTreeEvent event, int position) {
        if (event == null) return;
        PlotTreeBranch branch = getActiveBranch();
        if (branch == null || branch.getEvents() == null) return;
        int pos = position;
        if (pos < 0) {
            for (int i = 0; i < branch.getEvents().size(); i++) {
                if (branch.getEvents().get(i).getId() == event.getId()) { pos = i; break; }
            }
        }
        int finalPos = pos;
        new AlertDialog.Builder(requireContext())
                .setTitle(safeText(event.getTitle()))
                .setItems(new String[]{"编辑事件", "在后面插入事件", "从这里创建分支", "删除该事件"}, (dialog, which) -> {
                    if (which == 0) showEventEditorDialog(event, finalPos);
                    else if (which == 1) showEventEditorDialog(null, finalPos + 1);
                    else if (which == 2) showCreateBranchDialog(event, finalPos);
                    else if (which == 3) deleteEvent(finalPos);
                }).show();
    }

    private void showEventEditorDialog(PlotTreeEvent event, int insertPosition) {
        LinearLayout layout = buildFormLayout();
        EditText etTitle = buildEditText("事件标题");
        EditText etSummary = buildEditText("事件摘要 / 主线推进");
        EditText etNote = buildEditText("补充说明（可选）");
        EditText etTags = buildEditText("标签，使用顿号/逗号分隔（可选）");
        layout.addView(etTitle); layout.addView(etSummary); layout.addView(etNote); layout.addView(etTags);
        if (event != null) {
            etTitle.setText(event.getTitle());
            etSummary.setText(event.getSummary());
            etNote.setText(event.getNote());
            etTags.setText(event.getTags() == null ? "" : TextUtils.join("、", event.getTags()));
        }
        new AlertDialog.Builder(requireContext())
                .setTitle(event == null ? "新增剧情事件" : "编辑剧情事件")
                .setView(layout).setNegativeButton("取消", null)
                .setPositiveButton("保存", (dialog, which) -> {
                    PlotTreeBranch branch = getActiveBranch();
                    if (branch == null) return;
                    String title = trimToEmpty(etTitle.getText() == null ? null : etTitle.getText().toString());
                    String summary = trimToEmpty(etSummary.getText() == null ? null : etSummary.getText().toString());
                    if (TextUtils.isEmpty(title)) { Toast.makeText(requireContext(), "请先输入事件标题", Toast.LENGTH_SHORT).show(); return; }
                    if (event == null) {
                        PlotTreeEvent newEvent = newEvent(currentSnapshot, title, summary);
                        newEvent.setNote(trimToEmpty(etNote.getText() == null ? null : etNote.getText().toString()));
                        newEvent.setTags(splitTags(etTags.getText() == null ? null : etTags.getText().toString()));
                        int tp = insertPosition < 0 ? branch.getEvents().size() : Math.min(insertPosition, branch.getEvents().size());
                        branch.getEvents().add(tp, newEvent);
                    } else {
                        event.setTitle(title); event.setSummary(summary);
                        event.setNote(trimToEmpty(etNote.getText() == null ? null : etNote.getText().toString()));
                        event.setTags(splitTags(etTags.getText() == null ? null : etTags.getText().toString()));
                        event.setUpdateTime(System.currentTimeMillis());
                    }
                    branch.setUpdateTime(System.currentTimeMillis());
                    persistSnapshot(); loadAllBranchOverviews(); refreshDisplay();
                }).show();
    }

    private void deleteEvent(int position) {
        PlotTreeBranch branch = getActiveBranch();
        if (branch == null || branch.getEvents() == null || position < 0 || position >= branch.getEvents().size()) return;
        branch.getEvents().remove(position);
        branch.setUpdateTime(System.currentTimeMillis());
        persistSnapshot(); loadAllBranchOverviews(); refreshDisplay();
    }

    private void showCreateBranchDialog(PlotTreeEvent event, int position) {
        LinearLayout layout = buildFormLayout();
        EditText etName = buildEditText("新分支名称");
        EditText etDescr = buildEditText("新分支走向说明");
        layout.addView(etName); layout.addView(etDescr);
        etName.setText(safeText(event.getTitle()) + " 分支");

        ProgressBar pbSuggest = new ProgressBar(requireContext());
        pbSuggest.setVisibility(View.GONE); pbSuggest.setPadding(0, 8, 0, 0);
        layout.addView(pbSuggest);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("从当前事件创建分支").setView(layout).setNegativeButton("取消", null)
                .setNeutralButton("AI建议", null)
                .setPositiveButton("创建", null).create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
                pbSuggest.setVisibility(View.VISIBLE);
                dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setEnabled(false);
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                requestBranchSuggestions(event, etDescr, pbSuggest, dialog);
            });
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                PlotTreeBranch activeBranch = getActiveBranch();
                if (activeBranch == null) return;
                String branchName = trimToEmpty(etName.getText() == null ? null : etName.getText().toString());
                if (TextUtils.isEmpty(branchName)) { Toast.makeText(requireContext(), "请输入分支名称", Toast.LENGTH_SHORT).show(); return; }
                PlotTreeBranch newBranch = new PlotTreeBranch();
                newBranch.setId(currentSnapshot.getNextBranchId());
                currentSnapshot.setNextBranchId(currentSnapshot.getNextBranchId() + 1);
                newBranch.setName(branchName);
                newBranch.setDescription(trimToEmpty(etDescr.getText() == null ? null : etDescr.getText().toString()));
                android.util.Log.d("PlotTree", "CREATE_BRANCH id=" + newBranch.getId()
                        + " name=" + branchName
                        + " desc=[" + newBranch.getDescription() + "]"
                        + " descEmpty=" + TextUtils.isEmpty(newBranch.getDescription())
                        + " exported=" + newBranch.hasExportedChild()
                        + " srcEvId=" + event.getId());
                newBranch.setMainline(false);
                newBranch.setSourceBranchId(activeBranch.getId());
                newBranch.setSourceEventId(event.getId());
                List<PlotTreeEvent> copied = new ArrayList<>();
                for (int i = 0; i <= position && i < activeBranch.getEvents().size(); i++)
                    copied.add(copyEvent(activeBranch.getEvents().get(i)));
                newBranch.setEvents(copied);
                // 将"走向说明"同时创建为方向事件，统一占位卡片与方向事件结构
                String branchDescr = newBranch.getDescription();
                if (!TextUtils.isEmpty(branchDescr)) {
                    int dirEventId = currentSnapshot.getNextEventId();
                    currentSnapshot.setNextEventId(dirEventId + 1);
                    String dirTitle = branchDescr.length() > 50 ? branchDescr.substring(0, 50) + "\u2026" : branchDescr;
                    PlotTreeEvent dirEvent = PlotTreeEvent.createDirection(dirEventId, dirTitle, branchDescr);
                    copied.add(dirEvent);
                }
                currentSnapshot.getBranches().add(newBranch);
                if (activeBranch.getChildBranchIds() == null) activeBranch.setChildBranchIds(new ArrayList<>());
                activeBranch.getChildBranchIds().add(newBranch.getId());
                currentSnapshot.setActiveBranchId(newBranch.getId());
                persistSnapshot(); loadAllBranchOverviews(); refreshDisplay();
                dialog.dismiss();
            });
        });
        dialog.show();
    }

    private void requestBranchSuggestions(PlotTreeEvent event, EditText etDescr,
                                          ProgressBar pb, AlertDialog dialog) {
        if (currentStory == null || event == null) return;

        // Build nearby events context
        StringBuilder nearby = new StringBuilder();
        PlotTreeBranch activeBranch = getActiveBranch();
        if (activeBranch != null && activeBranch.getEvents() != null) {
            int eventIdx = -1;
            for (int i = 0; i < activeBranch.getEvents().size(); i++) {
                if (activeBranch.getEvents().get(i).getId() == event.getId()) { eventIdx = i; break; }
            }
            if (eventIdx >= 0) {
                int start = Math.max(0, eventIdx - 2);
                int end = Math.min(activeBranch.getEvents().size(), eventIdx + 3);
                for (int i = start; i < end; i++) {
                    PlotTreeEvent ev = activeBranch.getEvents().get(i);
                    nearby.append(i == eventIdx ? ">>> 【分叉点】" : "- ");
                    nearby.append(safeText(ev.getTitle()));
                    if (!TextUtils.isEmpty(ev.getSummary())) nearby.append("：").append(ev.getSummary());
                    nearby.append("\n");
                }
            }
        }

        Map<String, Object> variables = new HashMap<>();
        variables.put("story_title", safeText(currentStory.getTitle()));
        variables.put("event_title", safeText(event.getTitle()));
        variables.put("event_summary", safeText(event.getSummary()));
        variables.put("nearby_events", nearby.toString());

        String prompt = promptManager.getTaskPrompt(TaskType.BRANCH_SUGGEST.getCode(), variables);
        if (TextUtils.isEmpty(prompt)) {
            pb.setVisibility(View.GONE);
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setEnabled(true);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
            return;
        }

        ApiClient.getInstance().generateStory(prompt, "flash", requireContext(),
                new ApiClient.RequestOptions().setMaxTokens(300), new ApiClient.Callback() {
            @Override
            public void onSuccess(String responseText) {
                requireActivity().runOnUiThread(() -> {
                    pb.setVisibility(View.GONE);
                    dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setEnabled(true);
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                    showSuggestionPicker(etDescr, responseText);
                });
            }
            @Override
            public void onFailure(Exception e) {
                requireActivity().runOnUiThread(() -> {
                    pb.setVisibility(View.GONE);
                    dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setEnabled(true);
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                    Toast.makeText(requireContext(), "AI建议获取失败", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void showSuggestionPicker(EditText etDescr, String responseText) {
        List<String> suggestions = new ArrayList<>();
        for (String line : responseText.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("-") || trimmed.startsWith("-")) {
                String s = trimmed.replaceFirst("^[-\\s]+", "").trim();
                if (!TextUtils.isEmpty(s)) suggestions.add(s);
            }
        }
        if (suggestions.isEmpty()) {
            Toast.makeText(requireContext(), "未解析到建议", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(requireContext())
                .setTitle("选择一条AI建议作为走向说明")
                .setItems(suggestions.toArray(new String[0]), (d, which) -> {
                    etDescr.setText(suggestions.get(which));
                }).setNegativeButton("取消", null).show();
    }

    // ==================== 发展方向相关 ====================

    /** 点击方向事件卡片：编辑或应用到正文 */
    private void showDirectionEditDialog(PlotTreeEvent event, int branchId, int directionIndex) {
        if (event == null) return;
        PlotTreeBranch branch = findBranchById(branchId);
        if (branch == null) return;

        LinearLayout layout = buildFormLayout();
        EditText etTitle = buildEditText("方向标题");
        EditText etSummary = buildEditText("方向脉络摘要");
        etTitle.setText(event.getTitle());
        etSummary.setText(event.getSummary());
        layout.addView(etTitle); layout.addView(etSummary);

        // 删除按钮
        Button btnDelete = new Button(requireContext());
        btnDelete.setText("删除此方向");
        btnDelete.setTextColor(Color.WHITE);
        btnDelete.setBackgroundColor(Color.parseColor("#E53935"));
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        btnParams.topMargin = (int)(12 * getResources().getDisplayMetrics().density);
        btnDelete.setLayoutParams(btnParams);
        layout.addView(btnDelete);

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext())
                .setTitle("编辑发展方向")
                .setView(layout)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", (dialog, which) -> {
                    event.setTitle(trimToEmpty(etTitle.getText() == null ? null : etTitle.getText().toString()));
                    event.setSummary(trimToEmpty(etSummary.getText() == null ? null : etSummary.getText().toString()));
                    event.setUpdateTime(System.currentTimeMillis());
                    branch.setUpdateTime(System.currentTimeMillis());
                    persistSnapshot(); loadAllBranchOverviews(); refreshDisplay();
                });

        // 始终显示"应用到正文"按钮
        builder.setNeutralButton("应用到正文", (dialog, which) -> {
            // 先保存当前编辑内容
            event.setTitle(trimToEmpty(etTitle.getText() == null ? null : etTitle.getText().toString()));
            event.setSummary(trimToEmpty(etSummary.getText() == null ? null : etSummary.getText().toString()));
            event.setUpdateTime(System.currentTimeMillis());
            branch.setUpdateTime(System.currentTimeMillis());
            persistSnapshot();

            if (branch.isMainline()) {
                // 主线：直接应用到当前作品的写作区
                applyDirectionToCurrentStory(event);
            } else if (branch.hasExportedChild()) {
                // 已导出分支：应用到该分支的导出作品
                applyDirectionToWriting(branch, event);
            } else {
                // 未导出分支：弹出导出对话框，导出后自动应用方向
                EditText etExportTitle = buildEditText("导出后的小说标题");
                etExportTitle.setText(currentStory.getTitle() + " - " + branch.getName());
                new AlertDialog.Builder(requireContext())
                        .setTitle("导出分支并应用方向")
                        .setMessage("该分支尚未导出，需要先导出为新作品才能将发展方向应用到写作。")
                        .setView(etExportTitle)
                        .setNegativeButton("取消", null)
                        .setPositiveButton("导出并应用", (diag, w) -> {
                            String exportTitle = trimToEmpty(etExportTitle.getText() == null ? "" : etExportTitle.getText().toString());
                            if (TextUtils.isEmpty(exportTitle)) {
                                Toast.makeText(requireContext(), "导出标题不能为空", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            exportBranch(branch, exportTitle);
                            // 导出后跳转到新作品的写作区并携带方向
                            if (branch.hasExportedChild()) {
                                applyDirectionToWriting(branch, event);
                            }
                        }).show();
            }
        });

        AlertDialog editDialog = builder.create();

        // 删除按钮：确认后从分支事件列表中移除该方向事件
        btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("删除发展方向")
                    .setMessage("确定要删除「" + event.getTitle() + "」吗？此操作不可撤销。")
                    .setPositiveButton("删除", (confirmDialog, w) -> {
                        if (branch.getEvents() != null) {
                            branch.getEvents().remove(event);
                        }
                        branch.setUpdateTime(System.currentTimeMillis());
                        persistSnapshot(); loadAllBranchOverviews(); refreshDisplay();
                        editDialog.dismiss();
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });

        editDialog.show();
    }

    /** 主线方向事件：直接传方向信息到当前作品 */
    private void applyDirectionToCurrentStory(PlotTreeEvent direction) {
        if (currentStory == null || direction == null) return;
        Intent intent = new Intent(requireContext(), StoryWorkspaceActivity.class);
        intent.putExtra(StoryWorkspaceActivity.EXTRA_STORY_ID, currentStory.getId());
        intent.putExtra(StoryWorkspaceActivity.EXTRA_NEXT_CHAPTER_DIRECTION,
                "【发展方向】" + direction.getTitle() + "\n" + safeText(direction.getSummary()));
        startActivity(intent);
    }

    /** 点击大"+"按钮：选择自己写还是AI建议 */
    private void showDirectionCreateDialog(int branchId) {
        PlotTreeBranch branch = findBranchById(branchId);
        if (branch == null) return;

        new AlertDialog.Builder(requireContext())
                .setTitle("添加发展方向")
                .setItems(new String[]{"自己写", "AI建议"}, (dialog, which) -> {
                    if (which == 0) {
                        showDirectionManualCreate(branch);
                    } else {
                        showDirectionAiSuggest(branch);
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /** 手动创建方向事件 */
    private void showDirectionManualCreate(PlotTreeBranch branch) {
        LinearLayout layout = buildFormLayout();
        EditText etTitle = buildEditText("方向标题（如：主角复仇线）");
        EditText etSummary = buildEditText("方向脉络摘要（该章的剧情走向）");
        layout.addView(etTitle); layout.addView(etSummary);

        new AlertDialog.Builder(requireContext())
                .setTitle("手动添加发展方向")
                .setView(layout)
                .setNegativeButton("取消", null)
                .setPositiveButton("添加", (dialog, which) -> {
                    String title = trimToEmpty(etTitle.getText() == null ? null : etTitle.getText().toString());
                    String summary = trimToEmpty(etSummary.getText() == null ? null : etSummary.getText().toString());
                    if (TextUtils.isEmpty(title)) {
                        Toast.makeText(requireContext(), "请输入方向标题", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    PlotTreeEvent dirEvent = PlotTreeEvent.createDirection(
                            currentSnapshot.getNextEventId(), title, summary);
                    currentSnapshot.setNextEventId(currentSnapshot.getNextEventId() + 1);
                    if (branch.getEvents() == null) branch.setEvents(new ArrayList<>());
                    branch.getEvents().add(dirEvent);
                    branch.setUpdateTime(System.currentTimeMillis());
                    persistSnapshot(); loadAllBranchOverviews(); refreshDisplay();
                }).show();
    }

    /** AI建议发展方向 */
    private void showDirectionAiSuggest(PlotTreeBranch branch) {
        if (currentStory == null) return;

        LinearLayout layout = buildFormLayout();
        ProgressBar pb = new ProgressBar(requireContext());
        pb.setVisibility(View.GONE); pb.setPadding(0, 8, 0, 0);
        layout.addView(pb);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("AI发展方向建议")
                .setView(layout)
                .setNegativeButton("取消", null)
                .setPositiveButton("生成", null)
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                pb.setVisibility(View.VISIBLE);
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                requestDirectionSuggestions(branch, pb, dialog, layout);
            });
        });
        dialog.show();
    }

    private void requestDirectionSuggestions(PlotTreeBranch branch, ProgressBar pb,
                                              AlertDialog dialog, LinearLayout layout) {
        // 构建分支上下文
        StringBuilder context = new StringBuilder();
        if (branch.getEvents() != null) {
            int start = Math.max(0, branch.getEvents().size() - 4);
            for (int i = start; i < branch.getEvents().size(); i++) {
                PlotTreeEvent ev = branch.getEvents().get(i);
                if (ev.isDirection()) continue; // 跳过已有的方向事件
                context.append("- ").append(safeText(ev.getTitle()));
                if (!TextUtils.isEmpty(ev.getSummary())) context.append("：").append(ev.getSummary());
                context.append("\n");
            }
        }

        Map<String, Object> variables = new HashMap<>();
        variables.put("story_title", safeText(currentStory.getTitle()));
        variables.put("branch_name", safeText(branch.getName()));
        variables.put("branch_description", safeText(branch.getDescription()));
        variables.put("nearby_events", context.toString());

        String prompt = promptManager.getTaskPrompt(TaskType.DIRECTION_SUGGEST.getCode(), variables);
        if (TextUtils.isEmpty(prompt)) {
            pb.setVisibility(View.GONE);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
            return;
        }

        ApiClient.getInstance().generateStory(prompt, "flash", requireContext(),
                new ApiClient.RequestOptions().setMaxTokens(300), new ApiClient.Callback() {
                    @Override
                    public void onSuccess(String responseText) {
                        requireActivity().runOnUiThread(() -> {
                            pb.setVisibility(View.GONE);
                            dialog.dismiss();
                            showDirectionSuggestionPicker(branch, responseText);
                        });
                    }
                    @Override
                    public void onFailure(Exception e) {
                        requireActivity().runOnUiThread(() -> {
                            pb.setVisibility(View.GONE);
                            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                            Toast.makeText(requireContext(), "AI建议获取失败", Toast.LENGTH_SHORT).show();
                        });
                    }
                });
    }

    private void showDirectionSuggestionPicker(PlotTreeBranch branch, String responseText) {
        List<String> suggestions = new ArrayList<>();
        for (String line : responseText.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("-") || trimmed.startsWith("-")) {
                String s = trimmed.replaceFirst("^[-\\s]+", "").trim();
                if (!TextUtils.isEmpty(s)) suggestions.add(s);
            }
        }
        if (suggestions.isEmpty()) {
            Toast.makeText(requireContext(), "未解析到建议", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(requireContext())
                .setTitle("选择一条AI建议作为发展方向")
                .setItems(suggestions.toArray(new String[0]), (d, which) -> {
                    String selected = suggestions.get(which);
                    // 用建议内容作为title，生成方向事件
                    PlotTreeEvent dirEvent = PlotTreeEvent.createDirection(
                            currentSnapshot.getNextEventId(), selected, selected);
                    currentSnapshot.setNextEventId(currentSnapshot.getNextEventId() + 1);
                    if (branch.getEvents() == null) branch.setEvents(new ArrayList<>());
                    branch.getEvents().add(dirEvent);
                    branch.setUpdateTime(System.currentTimeMillis());
                    persistSnapshot(); loadAllBranchOverviews(); refreshDisplay();
                }).setNegativeButton("取消", null).show();
    }

    /** "应用到正文"：跳转到已导出作品的写作区，携带分叉点前后所有方向信息 */
    private void applyDirectionToWriting(PlotTreeBranch branch, PlotTreeEvent direction) {
        if (!branch.hasExportedChild()) {
            Toast.makeText(requireContext(), "请先导出该分支", Toast.LENGTH_SHORT).show();
            return;
        }
        int exportedStoryId = branch.getExportedStoryId();
        Intent intent = new Intent(requireContext(), StoryWorkspaceActivity.class);
        intent.putExtra(StoryWorkspaceActivity.EXTRA_STORY_ID, exportedStoryId);

        // 收集该分支中当前方向事件之前的所有方向事件
        StringBuilder directionChain = new StringBuilder();
        if (branch.getEvents() != null) {
            for (PlotTreeEvent ev : branch.getEvents()) {
                if (ev == direction) break; // 找到当前方向之前
                if (ev.isDirection() && !TextUtils.isEmpty(ev.getTitle())) {
                    if (directionChain.length() > 0) directionChain.append("\n---\n");
                    directionChain.append("【发展方向】").append(ev.getTitle());
                    if (!TextUtils.isEmpty(ev.getSummary())) {
                        directionChain.append("\n").append(ev.getSummary());
                    }
                }
            }
        }

        // 如果有源分支（即当前分支是分叉出来的），也要收集源分支的方向事件
        if (branch.getSourceBranchId() > 0 && currentSnapshot != null && currentSnapshot.getBranches() != null) {
            for (PlotTreeBranch srcBranch : currentSnapshot.getBranches()) {
                if (srcBranch != null && srcBranch.getId() == branch.getSourceBranchId()) {
                    if (srcBranch.getEvents() != null) {
                        // 只取 sourceEventId 之前的方向事件
                        for (PlotTreeEvent ev : srcBranch.getEvents()) {
                            if (ev.getId() >= branch.getSourceEventId()) break;
                            if (ev.isDirection() && !TextUtils.isEmpty(ev.getTitle())) {
                                if (directionChain.length() > 0) directionChain.insert(0, "\n---\n");
                                String part = "【分叉前 · 发展方向】" + ev.getTitle();
                                if (!TextUtils.isEmpty(ev.getSummary())) {
                                    part += "\n" + ev.getSummary();
                                }
                                directionChain.insert(0, part);
                            }
                        }
                    }
                    break;
                }
            }
        }

        // 拼接当前方向事件
        if (directionChain.length() > 0) directionChain.append("\n---\n");
        directionChain.append("【当前选定方向】").append(direction.getTitle());
        if (!TextUtils.isEmpty(direction.getSummary())) {
            directionChain.append("\n").append(direction.getSummary());
        }

        intent.putExtra(StoryWorkspaceActivity.EXTRA_NEXT_CHAPTER_DIRECTION, directionChain.toString());
        startActivity(intent);
    }

    private void showBranchSwitchDialog() {
        if (currentSnapshot == null || currentSnapshot.getBranches() == null || currentSnapshot.getBranches().isEmpty()) return;
        List<PlotTreeBranch> branches = currentSnapshot.getBranches();
        String[] names = new String[branches.size()];
        int checked = 0;
        for (int i = 0; i < branches.size(); i++) {
            PlotTreeBranch branch = branches.get(i);
            names[i] = branch.getName() + (branch.isMainline() ? "（主线）" : "");
            if (branch.hasExportedChild()) {
                Story exported = storyDao.getStoryById(branch.getExportedStoryId());
                names[i] += exported != null ? " → 导出《" + exported.getTitle() + "》" : " 已导出";
            }
            if (branch.getId() == currentSnapshot.getActiveBranchId()) checked = i;
        }
        new AlertDialog.Builder(requireContext())
                .setTitle("切换剧情分支")
                .setSingleChoiceItems(names, checked, (dialog, which) -> {
                    currentSnapshot.setActiveBranchId(branches.get(which).getId());
                    persistSnapshot();
                    displayMode = MODE_CURRENT_BRANCH;
                    refreshDisplay();
                    dialog.dismiss();
                }).setNegativeButton("取消", null).show();
    }

    private void showBranchActionDialog() {
        PlotTreeBranch branch = getActiveBranch();
        if (branch == null) return;
        List<String> options = new ArrayList<>();
        options.add("编辑分支信息");
        if (branch.hasExportedChild()) {
            Story child = storyDao.getStoryById(branch.getExportedStoryId());
            if (child != null) options.add("打开导出作品《" + child.getTitle() + "》");
        }
        if (!branch.isMainline()) options.add("删除当前分支");
        new AlertDialog.Builder(requireContext())
                .setTitle(branch.getName())
                .setItems(options.toArray(new String[0]), (dialog, which) -> {
                    String option = options.get(which);
                    if ("编辑分支信息".equals(option)) showEditBranchDialog(branch);
                    else if (option != null && option.startsWith("打开导出作品")) {
                        int childId = branch.getExportedStoryId();
                        if (childId > 0) {
                            Intent intent = new Intent(requireContext(), StoryWorkspaceActivity.class);
                            intent.putExtra(StoryWorkspaceActivity.EXTRA_STORY_ID, childId);
                            startActivity(intent);
                        }
                    } else if ("删除当前分支".equals(option)) deleteCurrentBranch(branch);
                }).show();
    }

    private void showEditBranchDialog(PlotTreeBranch branch) {
        LinearLayout layout = buildFormLayout();
        EditText etName = buildEditText("分支名称");
        EditText etDescr = buildEditText("分支说明");
        etName.setText(branch.getName()); etDescr.setText(branch.getDescription());
        layout.addView(etName); layout.addView(etDescr);
        new AlertDialog.Builder(requireContext())
                .setTitle("编辑分支").setView(layout).setNegativeButton("取消", null)
                .setPositiveButton("保存", (dialog, which) -> {
                    branch.setName(trimToEmpty(etName.getText() == null ? null : etName.getText().toString()));
                    branch.setDescription(trimToEmpty(etDescr.getText() == null ? null : etDescr.getText().toString()));
                    branch.setUpdateTime(System.currentTimeMillis());
                    persistSnapshot(); loadAllBranchOverviews(); refreshDisplay();
                }).show();
    }

    private void deleteCurrentBranch(PlotTreeBranch branch) {
        if (branch == null || branch.isMainline() || currentSnapshot == null) return;
        if (branch.getSourceBranchId() > 0) {
            for (PlotTreeBranch b : currentSnapshot.getBranches()) {
                if (b != null && b.getId() == branch.getSourceBranchId() && b.getChildBranchIds() != null) {
                    b.getChildBranchIds().remove(Integer.valueOf(branch.getId()));
                    break;
                }
            }
        }
        currentSnapshot.getBranches().remove(branch);
        currentSnapshot.setActiveBranchId(1);
        persistSnapshot(); loadAllBranchOverviews(); refreshDisplay();
    }

    private void showExportDialog() {
        PlotTreeBranch branch = getActiveBranch();
        if (currentStory == null || branch == null) return;
        EditText editText = buildEditText("导出后的小说标题");
        editText.setText(currentStory.getTitle() + " - " + branch.getName());
        new AlertDialog.Builder(requireContext())
                .setTitle("导出当前分支").setView(editText).setNegativeButton("取消", null)
                .setPositiveButton("导出", (dialog, which) ->
                        exportBranch(branch, editText.getText() == null ? "" : editText.getText().toString().trim())).show();
    }

    private void exportBranch(String title) {
        exportBranch(getActiveBranch(), title);
    }

    private void exportBranch(PlotTreeBranch branch, String title) {
        if (currentStory == null || branch == null || TextUtils.isEmpty(title)) {
            Toast.makeText(requireContext(), "导出标题不能为空", Toast.LENGTH_SHORT).show(); return;
        }
        int branchEventPos = findSourceEventPositionInMainline(branch);
        long newStoryId = storyDao.duplicateStory(currentStory, title);
        if (newStoryId <= 0) { Toast.makeText(requireContext(), "导出失败，请稍后重试", Toast.LENGTH_SHORT).show(); return; }
        int targetStoryId = (int) newStoryId;
        Story exportedStory = storyDao.getStoryById(targetStoryId);
        // 确保导出故事有系列名：优先用父故事的 seriesName，否则用父故事标题
        if (exportedStory != null) {
            String parentSeries = currentStory.getSeriesName();
            if (TextUtils.isEmpty(parentSeries)) {
                parentSeries = currentStory.getTitle();
            }
            exportedStory.setSeriesName(parentSeries);
            storyDao.updateStory(exportedStory);
        }
        if (exportedStory != null && branchEventPos >= 0) {
            String trimmed = trimStructureToEventPos(currentStory.getStructure(), branchEventPos + 1);
            // 走向说明作为新章节插入在分叉点之后
            if (!TextUtils.isEmpty(branch.getDescription())) {
                trimmed = appendDescriptionChapter(trimmed, branch.getDescription());
            }
            exportedStory.setStructure(trimmed);
            exportedStory.setWordCount(countWordsInStructure(trimmed));
            storyDao.updateStory(exportedStory);
        }
        copyRelatedData(currentStory.getId(), targetStoryId);
        PlotTreeWorkspaceSnapshot exported = new PlotTreeWorkspaceSnapshot();
        PlotTreeBranch exportedBranch = copyBranch(branch);
        exportedBranch.setId(1); exportedBranch.setMainline(true);
        exportedBranch.setSourceBranchId(0); exportedBranch.setSourceEventId(0);
        exportedBranch.setName("主线版本");
        exportedBranch.setDescription("从《" + currentStory.getTitle() + "》分支 \"" + branch.getName() + "\" 导出，继承分支点前的事件。");
        exportedBranch.setExportedStoryId(0);
        exported.getBranches().add(exportedBranch);
        exported.setActiveBranchId(1); exported.setNextBranchId(2);
        exported.setNextEventId(findMaxEventId(exportedBranch) + 1);
        storyDao.updatePlotTree(targetStoryId, gson.toJson(exported));
        branch.setExportedStoryId(targetStoryId);
        branch.setUpdateTime(System.currentTimeMillis()); persistSnapshot();
        loadAllBranchOverviews(); refreshDisplay();
        new AlertDialog.Builder(requireContext())
                .setTitle("导出成功")
                .setMessage("已将当前分支导出为新作品《" + title + "》。\n\n章节已裁剪到分支点，可前往新作品从分支点继续创作。")
                .setNegativeButton("稍后", null)
                .setPositiveButton("打开", (dialog, which) -> {
                    Intent intent = new Intent(requireContext(), StoryWorkspaceActivity.class);
                    intent.putExtra(StoryWorkspaceActivity.EXTRA_STORY_ID, targetStoryId);
                    startActivity(intent);
                }).show();
    }
    private int findSourceEventPositionInMainline(PlotTreeBranch branch) {
        if (currentSnapshot == null || branch == null) return -1;
        PlotTreeBranch mainline = getMainlineBranch();
        if (mainline == null || mainline.getEvents() == null || branch.getSourceEventId() <= 0) return -1;
        for (int i = 0; i < mainline.getEvents().size(); i++) {
            if (mainline.getEvents().get(i).getId() == branch.getSourceEventId()) return i;
        }
        return -1;
    }

    private String trimStructureToEventPos(String structureJson, int eventPos) {
        if (TextUtils.isEmpty(structureJson) || eventPos < 0) return structureJson;
        try {
            List<Volume> volumes = gson.fromJson(structureJson, VOLUME_LIST_TYPE);
            if (volumes == null || volumes.isEmpty()) return structureJson;
            int chapterCount = 0;
            List<Volume> trimmedVolumes = new ArrayList<>();
            for (Volume volume : volumes) {
                if (volume == null) continue;
                Volume trimmedVol = new Volume();
                trimmedVol.setTitle(volume.getTitle()); trimmedVol.setSummary(volume.getSummary());
                List<Chapter> trimmedChapters = new ArrayList<>();
                if (volume.getChapters() != null) {
                    for (Chapter chapter : volume.getChapters()) {
                        if (chapter == null) continue;
                        if (chapterCount < eventPos) {
                            Chapter kc = new Chapter();
                            kc.setTitle(chapter.getTitle()); kc.setContent(chapter.getContent());
                            trimmedChapters.add(kc); chapterCount++;
                        } else {
                            // 到达分叉点，不再保留后续章节（appendDescriptionChapter 会追加走向说明）
                            chapterCount++;
                            break;
                        }
                    }
                }
                trimmedVol.setChapters(trimmedChapters); trimmedVolumes.add(trimmedVol);
                if (chapterCount > eventPos) break;
            }
            return gson.toJson(trimmedVolumes);
        } catch (Exception ignored) { return structureJson; }
    }

    private String appendDescriptionChapter(String structureJson, String description) {
        if (TextUtils.isEmpty(structureJson) || TextUtils.isEmpty(description)) return structureJson;
        try {
            List<Volume> volumes = gson.fromJson(structureJson, VOLUME_LIST_TYPE);
            if (volumes == null || volumes.isEmpty()) {
                Volume newVol = new Volume();
                newVol.setTitle("走向说明");
                Chapter descCh = new Chapter();
                descCh.setTitle(description); descCh.setContent("");
                newVol.addChapter(descCh);
                volumes = new ArrayList<>(); volumes.add(newVol);
                return gson.toJson(volumes);
            }
            Volume lastVol = volumes.get(volumes.size() - 1);
            Chapter descCh = new Chapter();
            descCh.setTitle(description); descCh.setContent("");
            lastVol.addChapter(descCh);
            return gson.toJson(volumes);
        } catch (Exception ignored) { return structureJson; }
    }

    private int countWordsInStructure(String structureJson) {
        if (TextUtils.isEmpty(structureJson)) return 0;
        try {
            List<Volume> volumes = gson.fromJson(structureJson, VOLUME_LIST_TYPE);
            int total = 0;
            if (volumes != null) {
                for (Volume v : volumes) {
                    if (v.getChapters() != null) for (Chapter c : v.getChapters())
                        if (c.getContent() != null) total += c.getContent().length();
                }
            }
            return total;
        } catch (Exception e) { return 0; }
    }

    private void copyRelatedData(int sourceStoryId, int targetStoryId) {
        characterDao.replaceCharactersForStory(targetStoryId, copyCharacters(sourceStoryId, targetStoryId));
        copyDocuments(sourceStoryId, targetStoryId);
        copySettingsAndRelations(sourceStoryId, targetStoryId);
    }

    private List<Character> copyCharacters(int sourceStoryId, int targetStoryId) {
        List<Character> sourceCharacters = characterDao.getCharactersByStoryId(sourceStoryId);
        List<Character> copied = new ArrayList<>();
        for (Character c : sourceCharacters)
            copied.add(new Character(targetStoryId, c.getName(), c.getProfile(), c.getDetail(), c.getAvatarResId()));
        return copied;
    }

    private void copyDocuments(int sourceStoryId, int targetStoryId) {
        List<StoryDocument> documents = documentDao.getDocumentsByStory(sourceStoryId);
        for (StoryDocument doc : documents)
            documentDao.insertDocument(new StoryDocument(targetStoryId, doc.getTitle(), doc.getContent(), doc.getCategory()));
    }

    private void copySettingsAndRelations(int sourceStoryId, int targetStoryId) {
        List<StorySetting> settings = storySettingDao.getByStoryId(sourceStoryId);
        List<SettingRelationship> relationships = relationshipDao.getByStoryId(sourceStoryId);
        Map<Integer, Integer> idMap = new HashMap<>();
        for (StorySetting s : settings) {
            StorySetting clone = copySetting(s, targetStoryId);
            long newId = storySettingDao.insert(clone);
            if (newId > 0) idMap.put(s.getId(), (int) newId);
        }
        for (SettingRelationship r : relationships) {
            Integer ns = idMap.get(r.getSourceSettingId()), nt = idMap.get(r.getTargetSettingId());
            if (ns == null || nt == null) continue;
            SettingRelationship clone = new SettingRelationship();
            clone.setStoryId(targetStoryId); clone.setSourceSettingId(ns); clone.setTargetSettingId(nt);
            clone.setRelationshipType(r.getRelationshipType()); clone.setDescription(r.getDescription());
            clone.setSourceType(r.getSourceType()); clone.setConfidence(r.getConfidence());
            clone.setDirected(r.isDirected());
            clone.setCreateTime(System.currentTimeMillis()); clone.setUpdateTime(System.currentTimeMillis());
            relationshipDao.insert(clone);
        }
    }

    private StorySetting copySetting(StorySetting src, int targetStoryId) {
        StorySetting c = new StorySetting();
        c.setStoryId(targetStoryId); c.setCategory(src.getCategory()); c.setSubCategory(src.getSubCategory());
        c.setTitle(src.getTitle()); c.setSummary(src.getSummary()); c.setDetail(src.getDetail());
        c.setAttributes(src.getAttributes()); c.setTags(src.getTags()); c.setAliases(src.getAliases());
        c.setSpecificAttributes(src.getSpecificAttributes()); c.setSourceMaterialId(src.getSourceMaterialId());
        c.setSourceType(src.getSourceType()); c.setSourceUrl(src.getSourceUrl());
        c.setSourceTitle(src.getSourceTitle()); c.setAiConfidence(src.getAiConfidence());
        c.setRawJson(src.getRawJson()); c.setImportTime(src.getImportTime());
        c.setLastSyncTime(src.getLastSyncTime()); c.setSyncEnabled(src.isSyncEnabled());
        c.setHasUpdates(src.isHasUpdates());
        c.setCreateTime(System.currentTimeMillis()); c.setUpdateTime(System.currentTimeMillis());
        c.setFavorite(src.isFavorite()); c.setUsageCount(src.getUsageCount());
        c.setImagePath(src.getImagePath());
        return c;
    }

    private PlotTreeBranch copyBranch(PlotTreeBranch branch) {
        PlotTreeBranch clone = new PlotTreeBranch();
        clone.setName(branch.getName()); clone.setDescription(branch.getDescription());
        clone.setMainline(branch.isMainline()); clone.setSourceBranchId(branch.getSourceBranchId());
        clone.setSourceEventId(branch.getSourceEventId()); clone.setExportedStoryId(branch.getExportedStoryId());
        List<PlotTreeEvent> copiedEvents = new ArrayList<>();
        for (PlotTreeEvent e : branch.getEvents()) copiedEvents.add(copyEvent(e));
        clone.setEvents(copiedEvents);
        return clone;
    }

    private PlotTreeEvent copyEvent(PlotTreeEvent source) {
        PlotTreeEvent clone = new PlotTreeEvent();
        clone.setId(source.getId()); clone.setTitle(source.getTitle()); clone.setSummary(source.getSummary());
        clone.setNote(source.getNote());
        clone.setTags(source.getTags() == null ? new ArrayList<>() : new ArrayList<>(source.getTags()));
        clone.setCreateTime(source.getCreateTime()); clone.setUpdateTime(source.getUpdateTime());
        clone.setDirection(source.isDirection());
        return clone;
    }

    private int findMaxEventId(PlotTreeBranch branch) {
        int max = 0;
        if (branch.getEvents() == null) return max;
        for (PlotTreeEvent e : branch.getEvents()) max = Math.max(max, e.getId());
        return max;
    }

    private void persistSnapshot() {
        if (currentStory == null || currentSnapshot == null) return;
        currentSnapshot.setUpdateTime(System.currentTimeMillis());
        String json = gson.toJson(currentSnapshot);
        currentStory.setPlotTreeJson(json);
        storyDao.updatePlotTree(currentStory.getId(), json);
    }

    private List<Volume> parseStoryVolumes(Story story) {
        if (story == null || TextUtils.isEmpty(story.getStructure())) return new ArrayList<>();
        try {
            List<Volume> volumes = gson.fromJson(story.getStructure(), VOLUME_LIST_TYPE);
            return volumes == null ? new ArrayList<>() : volumes;
        } catch (Exception ignored) { return new ArrayList<>(); }
    }

    private LinearLayout buildFormLayout() {
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = (int)(16 * requireContext().getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding, padding, padding / 2);
        return layout;
    }

    private EditText buildEditText(String hint) {
        EditText editText = new EditText(requireContext());
        editText.setHint(hint);
        return editText;
    }

    private List<String> splitTags(String raw) {
        List<String> tags = new ArrayList<>();
        if (TextUtils.isEmpty(raw)) return tags;
        for (String part : raw.split("[、,，;；\\n]+")) {
            String trimmed = trimToEmpty(part);
            if (!TextUtils.isEmpty(trimmed)) tags.add(trimmed);
        }
        return tags;
    }

    private String trimText(String text, int maxLength) {
        if (TextUtils.isEmpty(text)) return "";
        String trimmed = text.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength) + "\u2026";
    }

    private String trimToEmpty(String text) { return text == null ? "" : text.trim(); }
    private String safeText(String text) { return TextUtils.isEmpty(text) ? "未命名" : text.trim(); }
    private String safeTitle(Story story) {
        return story == null || TextUtils.isEmpty(story.getTitle()) ? "故事主线" : story.getTitle().trim();
    }
    private static class ChapterContext {
        int volumeIndex; int chapterIndex; String chapterLabel; String title; String content;
        ChapterContext(int vi, int ci, String label, String t, String c) {
            volumeIndex = vi; chapterIndex = ci;
            chapterLabel = label != null ? label : "";
            title = t != null ? t : "";
            content = c != null ? c : "";
        }
    }

    private void showAiSummaryDialog() {
        if (currentStory == null) { Toast.makeText(requireContext(), "请先加载作品", Toast.LENGTH_SHORT).show(); return; }
        List<ChapterContext> allChapters = buildChapterContexts(currentStory);
        if (allChapters.isEmpty()) { Toast.makeText(requireContext(), "当前作品没有章节，请先创建内容", Toast.LENGTH_SHORT).show(); return; }

        // 先选梳理模式
        List<String> options = new ArrayList<>();
        // 检测是否有新增/更新章节
        List<ChapterContext> newChapters = collectNewChapters(allChapters);
        if (!newChapters.isEmpty()) {
            options.add("梳理更新内容（" + newChapters.size() + "章）");
        }
        options.add("选择章节范围");
        options.add("梳理全部章节（" + allChapters.size() + "章）");

        new AlertDialog.Builder(requireContext())
                .setTitle("AI 剧情梳理")
                .setItems(options.toArray(new String[0]), (dialog, which) -> {
                    String selected = options.get(which);
                    if (selected.startsWith("梳理更新内容")) {
                        if (newChapters.isEmpty()) {
                            Toast.makeText(requireContext(), "没有新增或更新的章节", Toast.LENGTH_SHORT).show();
                        } else if (newChapters.size() > 12) {
                            Toast.makeText(requireContext(), "新增章节过多（" + newChapters.size() + "章），请选择章节范围", Toast.LENGTH_SHORT).show();
                        } else {
                            showModelSelectionDialog(newChapters);
                        }
                    } else if (selected.startsWith("选择章节范围")) {
                        showRangePickerDialog(allChapters);
                    } else {
                        showModelSelectionDialog(allChapters);
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /** 收集没有已有梳理的章节（新增或标题变更） */
    private List<ChapterContext> collectNewChapters(List<ChapterContext> allChapters) {
        PlotSummarySnapshot existing = loadPlotSummarySnapshot();
        List<ChapterContext> result = new ArrayList<>();
        if (existing == null || existing.getChapterSummaries() == null) {
            // 没有任何梳理记录，全部都是新的
            result.addAll(allChapters);
        } else {
            for (ChapterContext ctx : allChapters) {
                boolean found = false;
                for (PlotChapterSummary s : existing.getChapterSummaries()) {
                    if (s == null) continue;
                    // 按卷+章位置匹配
                    if (s.getVolumeIndex() == ctx.volumeIndex && s.getChapterIndex() == ctx.chapterIndex) {
                        found = true;
                        break;
                    }
                }
                if (!found) result.add(ctx);
            }
        }
        return result;
    }

    /** 章节范围选择器 */
    private void showRangePickerDialog(List<ChapterContext> allChapters) {
        LinearLayout layout = buildFormLayout();
        // 起始章节
        TextView tvStart = new TextView(requireContext());
        tvStart.setText("起始章节（含）："); tvStart.setTextSize(14);
        layout.addView(tvStart);
        NumberPicker npStart = new NumberPicker(requireContext());
        npStart.setMinValue(1); npStart.setMaxValue(allChapters.size());
        npStart.setValue(1);
        layout.addView(npStart);
        // 结束章节
        TextView tvEnd = new TextView(requireContext());
        tvEnd.setText("结束章节（含）："); tvEnd.setTextSize(14); tvEnd.setPadding(0, 12, 0, 0);
        layout.addView(tvEnd);
        NumberPicker npEnd = new NumberPicker(requireContext());
        npEnd.setMinValue(1); npEnd.setMaxValue(allChapters.size());
        npEnd.setValue(Math.min(allChapters.size(), 12));
        layout.addView(npEnd);
        // 联动：结束不能小于起始
        npStart.setOnValueChangedListener((p, oldVal, newVal) -> {
            if (newVal > npEnd.getValue()) npEnd.setValue(newVal);
        });
        npEnd.setOnValueChangedListener((p, oldVal, newVal) -> {
            if (newVal < npStart.getValue()) npStart.setValue(newVal);
        });

        new AlertDialog.Builder(requireContext())
                .setTitle("选择梳理章节范围")
                .setView(layout)
                .setNegativeButton("取消", null)
                .setPositiveButton("确定", (d, w) -> {
                    int start = npStart.getValue() - 1;
                    int end = npEnd.getValue() - 1;
                    int count = end - start + 1;
                    if (count > 12) {
                        Toast.makeText(requireContext(), "一次最多梳理12章，当前选了" + count + "章", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    List<ChapterContext> rangeChapters = new ArrayList<>();
                    for (int i = start; i <= end && i < allChapters.size(); i++) {
                        rangeChapters.add(allChapters.get(i));
                    }
                    showModelSelectionDialog(rangeChapters);
                }).show();
    }

    /** 模型选择弹窗，选定后开始生成 */
    private void showModelSelectionDialog(List<ChapterContext> chapters) {
        if (chapters.isEmpty()) {
            Toast.makeText(requireContext(), "没有需要梳理的章节", Toast.LENGTH_SHORT).show();
            return;
        }
        if (chapters.size() > 12) {
            Toast.makeText(requireContext(), "章节过多（" + chapters.size() + "章），请选择章节范围", Toast.LENGTH_SHORT).show();
            return;
        }
        LinearLayout layout = buildFormLayout();
        TextView tvHint = new TextView(requireContext());
        tvHint.setText("共 " + chapters.size() + " 章，选择模型后开始生成");
        tvHint.setTextSize(14); tvHint.setPadding(0, 0, 0, 16);
        layout.addView(tvHint);

        RadioGroup rgModel = new RadioGroup(requireContext());
        rgModel.setOrientation(LinearLayout.HORIZONTAL);
        String[] modelIds = {"flash", "pro", "m2.5"};
        String[] modelNames = {"Flash (快速)", "Pro (深度)", "MiniMax M2.5"};
        for (int i = 0; i < modelIds.length; i++) {
            RadioButton rb = new RadioButton(requireContext());
            rb.setId(View.generateViewId()); rb.setText(modelNames[i]); rb.setTag(modelIds[i]);
            rb.setPadding(0, 0, 24, 0);
            if (i == 0) rb.setChecked(true);
            rgModel.addView(rb);
        }
        layout.addView(rgModel);

        ProgressBar progressBar = new ProgressBar(requireContext());
        progressBar.setVisibility(View.GONE); progressBar.setPadding(0, 16, 0, 0);
        layout.addView(progressBar);
        TextView tvProgress = new TextView(requireContext());
        tvProgress.setTextSize(13); tvProgress.setPadding(0, 8, 0, 0); tvProgress.setVisibility(View.GONE);
        layout.addView(tvProgress);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("AI 剧情梳理").setView(layout)
                .setNegativeButton("取消", null).setPositiveButton("开始生成", null).create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                int checkedId = rgModel.getCheckedRadioButtonId();
                RadioButton checkedRb = rgModel.findViewById(checkedId);
                String selectedModel = checkedRb != null ? (String) checkedRb.getTag() : "flash";
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(false);
                progressBar.setVisibility(View.VISIBLE); tvProgress.setVisibility(View.VISIBLE);
                tvProgress.setText("正在生成剧情梳理...");
                int token = ++summaryGenerationToken;
                String prompt = buildSinglePassPrompt(currentStory, chapters);
                ApiClient.RequestOptions opts = new ApiClient.RequestOptions().setMaxTokens(4000);
                ApiClient.getInstance().generateStory(prompt, selectedModel, requireContext(), opts, new ApiClient.Callback() {
                    @Override
                    public void onSuccess(String responseText) {
                        requireActivity().runOnUiThread(() -> {
                            if (token != summaryGenerationToken) return;
                            List<PlotChapterSummary> summaries = parseSinglePassResponse(responseText, chapters);
                            // 仅更新选中的章节，保留已有梳理
                            mergePlotSummary(summaries, chapters);
                            dialog.dismiss();
                            onAiSummarySuccess();
                        });
                    }
                    @Override
                    public void onFailure(Exception e) {
                        requireActivity().runOnUiThread(() -> {
                            if (token != summaryGenerationToken) return;
                            dialog.dismiss();
                            Toast.makeText(requireContext(), "生成失败：" + (e != null ? e.getMessage() : "未知错误"), Toast.LENGTH_LONG).show();
                        });
                    }
                });
            });
        });
        dialog.show();
    }

    /** 合并新梳理结果到已有快照：仅替换/新增选中章节，保留未选中章节 */
    private void mergePlotSummary(List<PlotChapterSummary> newSummaries, List<ChapterContext> selectedChapters) {
        PlotSummarySnapshot existing = loadPlotSummarySnapshot();
        List<PlotChapterSummary> merged = new ArrayList<>();
        if (existing != null && existing.getChapterSummaries() != null) {
            // 保留已有梳理中不在本次选择范围内的章节
            for (PlotChapterSummary old : existing.getChapterSummaries()) {
                if (old == null) continue;
                boolean isSelected = false;
                for (ChapterContext ctx : selectedChapters) {
                    if (old.getVolumeIndex() == ctx.volumeIndex && old.getChapterIndex() == ctx.chapterIndex) {
                        isSelected = true;
                        break;
                    }
                }
                if (!isSelected) merged.add(old);
            }
        }
        // 追加新的梳理结果
        merged.addAll(newSummaries);
        persistPlotSummary(merged);
    }

    private void onAiSummarySuccess() {
        cachedPlotSummary = null;
        currentStory = storyDao.getStoryById(storyId);
        if (currentStory == null) return;
        currentSnapshot = loadOrCreateSnapshot(currentStory);
        PlotTreeBranch mainline = getMainlineBranch();
        if (mainline != null) {
            List<PlotTreeEvent> newEvents = buildInitialEvents(currentStory, currentSnapshot);
            mainline.setEvents(newEvents);
            persistSnapshot();
        }
        loadAllBranchOverviews();
        refreshDisplay();
        Toast.makeText(requireContext(), "剧情梳理已更新", Toast.LENGTH_SHORT).show();
    }


    private List<ChapterContext> buildChapterContexts(Story story) {
        List<ChapterContext> result = new ArrayList<>();
        if (story == null) return result;
        List<Volume> volumes = parseStoryVolumes(story);
        for (int vi = 0; vi < volumes.size(); vi++) {
            Volume vol = volumes.get(vi);
            if (vol == null || vol.getChapters() == null) continue;
            for (int ci = 0; ci < vol.getChapters().size(); ci++) {
                Chapter ch = vol.getChapters().get(ci);
                if (ch == null) continue;
                String label = "第" + (vi + 1) + "卷 第" + (ci + 1) + "章";
                String content = ch.getContent();
                if (content != null && content.length() > 2000) content = content.substring(0, 2000);
                result.add(new ChapterContext(vi, ci, label, ch.getTitle(), content));
            }
        }
        return result;
    }

    private String buildSinglePassPrompt(Story story, List<ChapterContext> chapters) {
        StringBuilder chaptersContent = new StringBuilder();
        for (int i = 0; i < chapters.size(); i++) {
            ChapterContext ctx = chapters.get(i);
            chaptersContent.append("第").append(i + 1).append("章《").append(ctx.title).append("》\n");
            chaptersContent.append("内容：").append(ctx.content).append("\n\n");
        }

        Map<String, Object> variables = new HashMap<>();
        variables.put("story_title", story.getTitle() != null ? story.getTitle() : "");
        variables.put("chapters_content", chaptersContent.toString());

        String prompt = promptManager.getTaskPrompt(TaskType.PLOT_TREE_SUMMARY.getCode(), variables);
        return TextUtils.isEmpty(prompt) ? "" : prompt;
    }

    private List<PlotChapterSummary> parseSinglePassResponse(String responseText, List<ChapterContext> chapters) {
        List<PlotChapterSummary> result = new ArrayList<>();
        if (TextUtils.isEmpty(responseText)) return result;
        try {
            String json = responseText.trim();
            if (json.startsWith("`")) {
                int end = json.indexOf("\n```\n");
                if (end > 0) json = json.substring(end + 1);
                if (json.endsWith("`")) json = json.substring(0, json.length() - 3);
                json = json.trim();
            }
            JSONArray arr;
            try {
                arr = new JSONArray(json);
            } catch (Exception firstTry) {
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\[[\\s\\S]*\\]").matcher(json);
                if (m.find()) {
                    arr = new JSONArray(m.group());
                } else {
                    JSONObject root = new JSONObject(json);
                    String key = root.has("data") ? "data" : (root.has("result") ? "result" : (root.has("chapters") ? "chapters" : ""));
                    if (!TextUtils.isEmpty(key)) arr = root.getJSONArray(key);
                    else throw firstTry;
                }
            }
            
            // Process parsed JSON array into PlotChapterSummary list
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                PlotChapterSummary cs = new PlotChapterSummary();
                cs.setChapterTitle(obj.optString("chapterTitle", ""));
                cs.setChapterLabel(obj.optString("chapterLabel", ""));
                cs.setBriefSummary(obj.optString("briefSummary", ""));
                cs.setDetailSummary(obj.optString("detailSummary", ""));
                if (obj.has("keyEvents")) {
                    JSONArray keArr = obj.getJSONArray("keyEvents");
                    List<String> keList = new ArrayList<>();
                    for (int j = 0; j < keArr.length(); j++) keList.add(keArr.getString(j));
                    cs.setKeyEvents(keList);
                }
                if (obj.has("characters")) {
                    JSONArray chArr = obj.getJSONArray("characters");
                    List<String> chList = new ArrayList<>();
                    for (int j = 0; j < chArr.length(); j++) chList.add(chArr.getString(j));
                    cs.setCharacters(chList);
                }
                if (i < chapters.size()) {
                    ChapterContext ctx = chapters.get(i);
                    cs.setVolumeIndex(ctx.volumeIndex);
                    cs.setChapterIndex(ctx.chapterIndex);
                }
                cs.setSource("ai-summary");
                result.add(cs);
            }

        } catch (Exception e) {
            for (ChapterContext ctx : chapters) {
                PlotChapterSummary cs = new PlotChapterSummary();
                cs.setChapterTitle(ctx.title); cs.setChapterLabel(ctx.chapterLabel);
                cs.setBriefSummary(trimText(ctx.content, 50));
                cs.setVolumeIndex(ctx.volumeIndex); cs.setChapterIndex(ctx.chapterIndex);
                cs.setSource("fallback"); result.add(cs);
            }
        }
        return result;
    }

    private void persistPlotSummary(List<PlotChapterSummary> summaries) {
        if (currentStory == null || summaries == null) return;
        PlotSummarySnapshot snapshot = new PlotSummarySnapshot();
        snapshot.setSchemaVersion(3); snapshot.setModel("ai-summary"); snapshot.setDetailLevel("standard");
        snapshot.setGeneratedAt(System.currentTimeMillis()); snapshot.setChapterSummaries(summaries);
        String json = gson.toJson(snapshot);
        currentStory.setPlotSummaryJson(json);
        storyDao.updatePlotSummary(currentStory.getId(), json);
    }
}
