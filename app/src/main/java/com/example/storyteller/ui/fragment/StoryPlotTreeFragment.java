package com.example.storyteller.ui.fragment;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.storyteller.R;
import com.example.storyteller.base.BaseFragment;
import com.example.storyteller.data.local.db.CharacterDao;
import com.example.storyteller.data.local.db.SettingRelationshipDao;
import com.example.storyteller.data.local.db.StoryDao;
import com.example.storyteller.data.local.db.StoryDocumentDao;
import com.example.storyteller.data.local.db.StorySettingDao;
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
import com.example.storyteller.ui.adapter.PlotTreeEventAdapter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
public class StoryPlotTreeFragment extends BaseFragment {
    private static final String ARG_STORY_ID = "arg_story_id";
    private static final Type VOLUME_LIST_TYPE = new TypeToken<List<Volume>>() {}.getType();
    private TextView tvStoryTitle;
    private TextView tvBranchInfo;
    private TextView tvStatus;
    private TextView tvEmpty;
    private PlotTreeEventAdapter adapter;
    private final Gson gson = new Gson();
    private StoryDao storyDao;
    private CharacterDao characterDao;
    private StorySettingDao storySettingDao;
    private SettingRelationshipDao relationshipDao;
    private StoryDocumentDao documentDao;
    private Story currentStory;
    private PlotTreeWorkspaceSnapshot currentSnapshot;
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
        tvStatus = view.findViewById(R.id.tv_plot_tree_status);
        tvEmpty = view.findViewById(R.id.tv_plot_tree_empty);
        RecyclerView recyclerView = view.findViewById(R.id.rv_plot_tree_events);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new PlotTreeEventAdapter();
        adapter.setListener(this::showEventActionDialog);
        recyclerView.setAdapter(adapter);
        view.findViewById(R.id.btn_plot_tree_switch_branch).setOnClickListener(v -> showBranchSwitchDialog());
        view.findViewById(R.id.btn_plot_tree_add_event).setOnClickListener(v -> showEventEditorDialog(null, -1));
        view.findViewById(R.id.btn_plot_tree_branch_actions).setOnClickListener(v -> showBranchActionDialog());
        view.findViewById(R.id.btn_plot_tree_export).setOnClickListener(v -> showExportDialog());
    }
    @Override
    protected void initData() {
        storyDao = new StoryDao(requireContext());
        characterDao = new CharacterDao(requireContext());
        storySettingDao = new StorySettingDao(requireContext());
        relationshipDao = new SettingRelationshipDao(requireContext());
        documentDao = new StoryDocumentDao(requireContext());
        if (getArguments() != null) {
            storyId = getArguments().getInt(ARG_STORY_ID, -1);
        }
        loadStory();
    }
    @Override
    public void onResume() {
        super.onResume();
        if (storyId > 0) {
            loadStory();
        }
    }
    private void loadStory() {
        currentStory = storyDao.getStoryById(storyId);
        if (currentStory == null) {
            tvStoryTitle.setText("剧情树");
            tvBranchInfo.setText("");
            tvStatus.setText("未找到作品");
            tvEmpty.setVisibility(View.VISIBLE);
            tvEmpty.setText("请先选择一部小说，再进入剧情树。\n\n旧的 PlotTreeActivity 不参与本功能。");
            adapter.setData(new ArrayList<>(), -1);
            return;
        }
        tvStoryTitle.setText("《" + currentStory.getTitle() + "》剧情树");
        currentSnapshot = loadOrCreateSnapshot(currentStory);
        renderCurrentBranch();
    }
    private PlotTreeWorkspaceSnapshot loadOrCreateSnapshot(Story story) {
        PlotTreeWorkspaceSnapshot snapshot = null;
        if (story != null && !TextUtils.isEmpty(story.getPlotTreeJson())) {
            try {
                snapshot = gson.fromJson(story.getPlotTreeJson(), PlotTreeWorkspaceSnapshot.class);
            } catch (Exception ignored) {
                snapshot = null;
            }
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
        if (story == null) {
            return events;
        }
        if (!TextUtils.isEmpty(story.getPlotSummaryJson())) {
            try {
                PlotSummarySnapshot plotSummarySnapshot = gson.fromJson(story.getPlotSummaryJson(), PlotSummarySnapshot.class);
                if (plotSummarySnapshot != null && plotSummarySnapshot.getChapterSummaries() != null) {
                    for (PlotChapterSummary chapterSummary : plotSummarySnapshot.getChapterSummaries()) {
                        if (chapterSummary == null) {
                            continue;
                        }
                        if (chapterSummary.getKeyEvents() != null && !chapterSummary.getKeyEvents().isEmpty()) {
                            for (String eventText : chapterSummary.getKeyEvents()) {
                                if (!TextUtils.isEmpty(eventText)) {
                                    events.add(newEvent(workspace, eventText, chapterSummary.getChapterLabel() + " · " + safeText(chapterSummary.getBriefSummary())));
                                }
                            }
                        } else if (!TextUtils.isEmpty(chapterSummary.getBriefSummary())) {
                            events.add(newEvent(workspace, chapterSummary.getChapterTitle(), chapterSummary.getBriefSummary()));
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }
        if (!events.isEmpty()) {
            return events;
        }
        List<Volume> volumes = parseStoryVolumes(story);
        for (Volume volume : volumes) {
            if (volume == null || volume.getChapters() == null) {
                continue;
            }
            for (Chapter chapter : volume.getChapters()) {
                if (chapter == null) {
                    continue;
                }
                String title = TextUtils.isEmpty(chapter.getTitle()) ? "章节事件" : chapter.getTitle();
                String summary = TextUtils.isEmpty(chapter.getContent()) ? "待补充该章节的关键剧情。" : trimText(chapter.getContent(), 80);
                events.add(newEvent(workspace, title, summary));
            }
        }
        if (!events.isEmpty()) {
            return events;
        }
        if (!TextUtils.isEmpty(story.getDescription())) {
            events.add(newEvent(workspace, safeTitle(story), trimText(story.getDescription(), 80)));
        }
        return events;
    }
    private PlotTreeEvent newEvent(PlotTreeWorkspaceSnapshot workspace, String title, String summary) {
        int id = workspace.getNextEventId();
        workspace.setNextEventId(id + 1);
        PlotTreeEvent event = new PlotTreeEvent(id, title, summary);
        return event;
    }
    private void renderCurrentBranch() {
        PlotTreeBranch branch = getActiveBranch();
        if (branch == null) {
            tvBranchInfo.setText("没有可用分支");
            tvStatus.setText("请先初始化剧情树");
            tvEmpty.setVisibility(View.VISIBLE);
            tvEmpty.setText("暂无剧情事件");
            adapter.setData(new ArrayList<>(), -1);
            return;
        }
        int count = branch.getEvents() == null ? 0 : branch.getEvents().size();
        tvBranchInfo.setText("当前分支：" + safeText(branch.getName()) + " · 共 " + count + " 个事件");
        tvStatus.setText(TextUtils.isEmpty(branch.getDescription())
                ? "点击任意事件可编辑、插入新事件或从该节点分叉。"
                : branch.getDescription());
        List<PlotTreeEvent> events = branch.getEvents() == null ? new ArrayList<>() : branch.getEvents();
        adapter.setData(events, branch.getSourceEventId());
        tvEmpty.setVisibility(events.isEmpty() ? View.VISIBLE : View.GONE);
        tvEmpty.setText(events.isEmpty() ? "这个分支还没有事件，点击上方“新增事件”开始梳理。" : "");
    }
    private PlotTreeBranch getActiveBranch() {
        if (currentSnapshot == null || currentSnapshot.getBranches() == null) {
            return null;
        }
        for (PlotTreeBranch branch : currentSnapshot.getBranches()) {
            if (branch != null && branch.getId() == currentSnapshot.getActiveBranchId()) {
                return branch;
            }
        }
        return currentSnapshot.getBranches().isEmpty() ? null : currentSnapshot.getBranches().get(0);
    }
    private void showEventActionDialog(PlotTreeEvent event, int position) {
        String[] items = new String[]{"编辑事件", "在后面插入事件", "从这里创建分支", "删除该事件"};
        new AlertDialog.Builder(requireContext())
                .setTitle(safeText(event.getTitle()))
                .setItems(items, (dialog, which) -> {
                    if (which == 0) {
                        showEventEditorDialog(event, position);
                    } else if (which == 1) {
                        showEventEditorDialog(null, position + 1);
                    } else if (which == 2) {
                        showCreateBranchDialog(event, position);
                    } else if (which == 3) {
                        deleteEvent(position);
                    }
                })
                .show();
    }
    private void showEventEditorDialog(PlotTreeEvent event, int insertPosition) {
        LinearLayout layout = buildFormLayout();
        EditText etTitle = buildEditText("事件标题");
        EditText etSummary = buildEditText("事件摘要 / 主线推进");
        EditText etNote = buildEditText("补充说明（可选）");
        EditText etTags = buildEditText("标签，使用顿号/逗号分隔（可选）");
        layout.addView(etTitle);
        layout.addView(etSummary);
        layout.addView(etNote);
        layout.addView(etTags);
        if (event != null) {
            etTitle.setText(event.getTitle());
            etSummary.setText(event.getSummary());
            etNote.setText(event.getNote());
            etTags.setText(event.getTags() == null ? "" : TextUtils.join("、", event.getTags()));
        }
        new AlertDialog.Builder(requireContext())
                .setTitle(event == null ? "新增剧情事件" : "编辑剧情事件")
                .setView(layout)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", (dialog, which) -> {
                    PlotTreeBranch branch = getActiveBranch();
                    if (branch == null) {
                        return;
                    }
                    String title = trimToEmpty(etTitle.getText() == null ? null : etTitle.getText().toString());
                    String summary = trimToEmpty(etSummary.getText() == null ? null : etSummary.getText().toString());
                    if (TextUtils.isEmpty(title)) {
                        Toast.makeText(requireContext(), "请先输入事件标题", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (event == null) {
                        PlotTreeEvent newEvent = newEvent(currentSnapshot, title, summary);
                        newEvent.setNote(trimToEmpty(etNote.getText() == null ? null : etNote.getText().toString()));
                        newEvent.setTags(splitTags(etTags.getText() == null ? null : etTags.getText().toString()));
                        int targetPosition = insertPosition < 0 ? branch.getEvents().size() : Math.min(insertPosition, branch.getEvents().size());
                        branch.getEvents().add(targetPosition, newEvent);
                    } else {
                        event.setTitle(title);
                        event.setSummary(summary);
                        event.setNote(trimToEmpty(etNote.getText() == null ? null : etNote.getText().toString()));
                        event.setTags(splitTags(etTags.getText() == null ? null : etTags.getText().toString()));
                        event.setUpdateTime(System.currentTimeMillis());
                    }
                    branch.setUpdateTime(System.currentTimeMillis());
                    persistSnapshot();
                    renderCurrentBranch();
                })
                .show();
    }
    private void deleteEvent(int position) {
        PlotTreeBranch branch = getActiveBranch();
        if (branch == null || branch.getEvents() == null || position < 0 || position >= branch.getEvents().size()) {
            return;
        }
        branch.getEvents().remove(position);
        branch.setUpdateTime(System.currentTimeMillis());
        persistSnapshot();
        renderCurrentBranch();
    }
    private void showCreateBranchDialog(PlotTreeEvent event, int position) {
        LinearLayout layout = buildFormLayout();
        EditText etName = buildEditText("新分支名称");
        EditText etDescription = buildEditText("新分支走向说明");
        layout.addView(etName);
        layout.addView(etDescription);
        etName.setText(safeText(event.getTitle()) + " 分支");
        new AlertDialog.Builder(requireContext())
                .setTitle("从当前事件创建分支")
                .setView(layout)
                .setNegativeButton("取消", null)
                .setPositiveButton("创建", (dialog, which) -> {
                    PlotTreeBranch activeBranch = getActiveBranch();
                    if (activeBranch == null) {
                        return;
                    }
                    String branchName = trimToEmpty(etName.getText() == null ? null : etName.getText().toString());
                    if (TextUtils.isEmpty(branchName)) {
                        Toast.makeText(requireContext(), "请输入分支名称", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    PlotTreeBranch newBranch = new PlotTreeBranch();
                    newBranch.setId(currentSnapshot.getNextBranchId());
                    currentSnapshot.setNextBranchId(currentSnapshot.getNextBranchId() + 1);
                    newBranch.setName(branchName);
                    newBranch.setDescription(trimToEmpty(etDescription.getText() == null ? null : etDescription.getText().toString()));
                    newBranch.setMainline(false);
                    newBranch.setSourceBranchId(activeBranch.getId());
                    newBranch.setSourceEventId(event.getId());
                    List<PlotTreeEvent> copied = new ArrayList<>();
                    for (int i = 0; i <= position && i < activeBranch.getEvents().size(); i++) {
                        copied.add(copyEvent(activeBranch.getEvents().get(i)));
                    }
                    newBranch.setEvents(copied);
                    currentSnapshot.getBranches().add(newBranch);
                    currentSnapshot.setActiveBranchId(newBranch.getId());
                    persistSnapshot();
                    renderCurrentBranch();
                })
                .show();
    }
    private void showBranchSwitchDialog() {
        if (currentSnapshot == null || currentSnapshot.getBranches() == null || currentSnapshot.getBranches().isEmpty()) {
            return;
        }
        List<PlotTreeBranch> branches = currentSnapshot.getBranches();
        String[] names = new String[branches.size()];
        int checked = 0;
        for (int i = 0; i < branches.size(); i++) {
            PlotTreeBranch branch = branches.get(i);
            names[i] = branch.getName() + (branch.isMainline() ? "（主线）" : "");
            if (branch.getId() == currentSnapshot.getActiveBranchId()) {
                checked = i;
            }
        }
        new AlertDialog.Builder(requireContext())
                .setTitle("切换剧情分支")
                .setSingleChoiceItems(names, checked, (dialog, which) -> {
                    currentSnapshot.setActiveBranchId(branches.get(which).getId());
                    persistSnapshot();
                    renderCurrentBranch();
                    dialog.dismiss();
                })
                .setNegativeButton("取消", null)
                .show();
    }
    private void showBranchActionDialog() {
        PlotTreeBranch branch = getActiveBranch();
        if (branch == null) {
            return;
        }
        List<String> options = new ArrayList<>();
        options.add("编辑分支信息");
        if (!branch.isMainline()) {
            options.add("删除当前分支");
        }
        new AlertDialog.Builder(requireContext())
                .setTitle(branch.getName())
                .setItems(options.toArray(new String[0]), (dialog, which) -> {
                    String option = options.get(which);
                    if ("编辑分支信息".equals(option)) {
                        showEditBranchDialog(branch);
                    } else if ("删除当前分支".equals(option)) {
                        deleteCurrentBranch(branch);
                    }
                })
                .show();
    }
    private void showEditBranchDialog(PlotTreeBranch branch) {
        LinearLayout layout = buildFormLayout();
        EditText etName = buildEditText("分支名称");
        EditText etDescription = buildEditText("分支说明");
        etName.setText(branch.getName());
        etDescription.setText(branch.getDescription());
        layout.addView(etName);
        layout.addView(etDescription);
        new AlertDialog.Builder(requireContext())
                .setTitle("编辑分支")
                .setView(layout)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", (dialog, which) -> {
                    branch.setName(trimToEmpty(etName.getText() == null ? null : etName.getText().toString()));
                    branch.setDescription(trimToEmpty(etDescription.getText() == null ? null : etDescription.getText().toString()));
                    branch.setUpdateTime(System.currentTimeMillis());
                    persistSnapshot();
                    renderCurrentBranch();
                })
                .show();
    }
    private void deleteCurrentBranch(PlotTreeBranch branch) {
        if (branch == null || branch.isMainline() || currentSnapshot == null) {
            return;
        }
        currentSnapshot.getBranches().remove(branch);
        currentSnapshot.setActiveBranchId(1);
        persistSnapshot();
        renderCurrentBranch();
    }
    private void showExportDialog() {
        PlotTreeBranch branch = getActiveBranch();
        if (currentStory == null || branch == null) {
            return;
        }
        EditText editText = buildEditText("导出后的小说标题");
        editText.setText(currentStory.getTitle() + " - " + branch.getName());
        new AlertDialog.Builder(requireContext())
                .setTitle("导出当前分支")
                .setView(editText)
                .setNegativeButton("取消", null)
                .setPositiveButton("导出", (dialog, which) -> exportBranch(editText.getText() == null ? "" : editText.getText().toString().trim()))
                .show();
    }
    private void exportBranch(String title) {
        PlotTreeBranch branch = getActiveBranch();
        if (currentStory == null || branch == null || TextUtils.isEmpty(title)) {
            Toast.makeText(requireContext(), "导出标题不能为空", Toast.LENGTH_SHORT).show();
            return;
        }
        long newStoryId = storyDao.duplicateStory(currentStory, title);
        if (newStoryId <= 0) {
            Toast.makeText(requireContext(), "导出失败，请稍后重试", Toast.LENGTH_SHORT).show();
            return;
        }
        copyRelatedData(currentStory.getId(), (int) newStoryId);
        PlotTreeWorkspaceSnapshot exported = new PlotTreeWorkspaceSnapshot();
        PlotTreeBranch exportedBranch = copyBranch(branch);
        exportedBranch.setId(1);
        exportedBranch.setMainline(true);
        exportedBranch.setSourceBranchId(0);
        exportedBranch.setSourceEventId(0);
        exportedBranch.setName("主线版本");
        exported.getBranches().add(exportedBranch);
        exported.setActiveBranchId(1);
        exported.setNextBranchId(2);
        exported.setNextEventId(findMaxEventId(exportedBranch) + 1);
        storyDao.updatePlotTree((int) newStoryId, gson.toJson(exported));
        new AlertDialog.Builder(requireContext())
                .setTitle("导出成功")
                .setMessage("已将当前分支导出为新小说版本。是否立即打开？")
                .setNegativeButton("稍后", null)
                .setPositiveButton("打开", (dialog, which) -> {
                    Intent intent = new Intent(requireContext(), StoryWorkspaceActivity.class);
                    intent.putExtra(StoryWorkspaceActivity.EXTRA_STORY_ID, (int) newStoryId);
                    startActivity(intent);
                })
                .show();
    }
    private void copyRelatedData(int sourceStoryId, int targetStoryId) {
        characterDao.replaceCharactersForStory(targetStoryId, copyCharacters(sourceStoryId, targetStoryId));
        copyDocuments(sourceStoryId, targetStoryId);
        copySettingsAndRelations(sourceStoryId, targetStoryId);
    }
    private List<Character> copyCharacters(int sourceStoryId, int targetStoryId) {
        List<Character> sourceCharacters = characterDao.getCharactersByStoryId(sourceStoryId);
        List<Character> copied = new ArrayList<>();
        for (Character character : sourceCharacters) {
            Character clone = new Character(targetStoryId, character.getName(), character.getProfile(), character.getDetail(), character.getAvatarResId());
            copied.add(clone);
        }
        return copied;
    }
    private void copyDocuments(int sourceStoryId, int targetStoryId) {
        List<StoryDocument> documents = documentDao.getDocumentsByStory(sourceStoryId);
        for (StoryDocument document : documents) {
            StoryDocument clone = new StoryDocument(targetStoryId, document.getTitle(), document.getContent(), document.getCategory());
            documentDao.insertDocument(clone);
        }
    }
    private void copySettingsAndRelations(int sourceStoryId, int targetStoryId) {
        List<StorySetting> settings = storySettingDao.getByStoryId(sourceStoryId);
        List<SettingRelationship> relationships = relationshipDao.getByStoryId(sourceStoryId);
        java.util.Map<Integer, Integer> settingIdMap = new java.util.HashMap<>();
        for (StorySetting setting : settings) {
            StorySetting clone = copySetting(setting, targetStoryId);
            long newId = storySettingDao.insert(clone);
            if (newId > 0) {
                settingIdMap.put(setting.getId(), (int) newId);
            }
        }
        for (SettingRelationship relationship : relationships) {
            Integer newSourceId = settingIdMap.get(relationship.getSourceSettingId());
            Integer newTargetId = settingIdMap.get(relationship.getTargetSettingId());
            if (newSourceId == null || newTargetId == null) {
                continue;
            }
            SettingRelationship clone = new SettingRelationship();
            clone.setStoryId(targetStoryId);
            clone.setSourceSettingId(newSourceId);
            clone.setTargetSettingId(newTargetId);
            clone.setRelationshipType(relationship.getRelationshipType());
            clone.setDescription(relationship.getDescription());
            clone.setSourceType(relationship.getSourceType());
            clone.setConfidence(relationship.getConfidence());
            clone.setDirected(relationship.isDirected());
            clone.setCreateTime(System.currentTimeMillis());
            clone.setUpdateTime(System.currentTimeMillis());
            relationshipDao.insert(clone);
        }
    }
    private StorySetting copySetting(StorySetting source, int targetStoryId) {
        StorySetting clone = new StorySetting();
        clone.setStoryId(targetStoryId);
        clone.setCategory(source.getCategory());
        clone.setSubCategory(source.getSubCategory());
        clone.setTitle(source.getTitle());
        clone.setSummary(source.getSummary());
        clone.setDetail(source.getDetail());
        clone.setAttributes(source.getAttributes());
        clone.setTags(source.getTags());
        clone.setAliases(source.getAliases());
        clone.setSpecificAttributes(source.getSpecificAttributes());
        clone.setSourceMaterialId(source.getSourceMaterialId());
        clone.setSourceType(source.getSourceType());
        clone.setSourceUrl(source.getSourceUrl());
        clone.setSourceTitle(source.getSourceTitle());
        clone.setAiConfidence(source.getAiConfidence());
        clone.setRawJson(source.getRawJson());
        clone.setImportTime(source.getImportTime());
        clone.setLastSyncTime(source.getLastSyncTime());
        clone.setSyncEnabled(source.isSyncEnabled());
        clone.setHasUpdates(source.isHasUpdates());
        clone.setCreateTime(System.currentTimeMillis());
        clone.setUpdateTime(System.currentTimeMillis());
        clone.setFavorite(source.isFavorite());
        clone.setUsageCount(source.getUsageCount());
        clone.setImagePath(source.getImagePath());
        return clone;
    }
    private PlotTreeBranch copyBranch(PlotTreeBranch branch) {
        PlotTreeBranch clone = new PlotTreeBranch();
        clone.setName(branch.getName());
        clone.setDescription(branch.getDescription());
        clone.setMainline(branch.isMainline());
        clone.setSourceBranchId(branch.getSourceBranchId());
        clone.setSourceEventId(branch.getSourceEventId());
        List<PlotTreeEvent> copiedEvents = new ArrayList<>();
        for (PlotTreeEvent event : branch.getEvents()) {
            copiedEvents.add(copyEvent(event));
        }
        clone.setEvents(copiedEvents);
        return clone;
    }
    private PlotTreeEvent copyEvent(PlotTreeEvent source) {
        PlotTreeEvent clone = new PlotTreeEvent();
        clone.setId(source.getId());
        clone.setTitle(source.getTitle());
        clone.setSummary(source.getSummary());
        clone.setNote(source.getNote());
        clone.setTags(source.getTags() == null ? new ArrayList<>() : new ArrayList<>(source.getTags()));
        clone.setCreateTime(source.getCreateTime());
        clone.setUpdateTime(source.getUpdateTime());
        return clone;
    }
    private int findMaxEventId(PlotTreeBranch branch) {
        int max = 0;
        if (branch.getEvents() == null) {
            return max;
        }
        for (PlotTreeEvent event : branch.getEvents()) {
            max = Math.max(max, event.getId());
        }
        return max;
    }
    private void persistSnapshot() {
        if (currentStory == null || currentSnapshot == null) {
            return;
        }
        currentSnapshot.setUpdateTime(System.currentTimeMillis());
        String json = gson.toJson(currentSnapshot);
        currentStory.setPlotTreeJson(json);
        storyDao.updatePlotTree(currentStory.getId(), json);
    }
    private List<Volume> parseStoryVolumes(Story story) {
        if (story == null || TextUtils.isEmpty(story.getStructure())) {
            return new ArrayList<>();
        }
        try {
            List<Volume> volumes = gson.fromJson(story.getStructure(), VOLUME_LIST_TYPE);
            return volumes == null ? new ArrayList<>() : volumes;
        } catch (Exception ignored) {
            return new ArrayList<>();
        }
    }
    private LinearLayout buildFormLayout() {
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (16 * requireContext().getResources().getDisplayMetrics().density);
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
        if (TextUtils.isEmpty(raw)) {
            return tags;
        }
        String[] parts = raw.split("[、,，;；\\n]+");
        for (String part : parts) {
            String trimmed = trimToEmpty(part);
            if (!TextUtils.isEmpty(trimmed)) {
                tags.add(trimmed);
            }
        }
        return tags;
    }
    private String trimText(String text, int maxLength) {
        if (TextUtils.isEmpty(text)) {
            return "";
        }
        String trimmed = text.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength) + "…";
    }
    private String trimToEmpty(String text) {
        return text == null ? "" : text.trim();
    }
    private String safeText(String text) {
        return TextUtils.isEmpty(text) ? "未命名" : text.trim();
    }
    private String safeTitle(Story story) {
        return story == null || TextUtils.isEmpty(story.getTitle()) ? "故事主线" : story.getTitle().trim();
    }
}
