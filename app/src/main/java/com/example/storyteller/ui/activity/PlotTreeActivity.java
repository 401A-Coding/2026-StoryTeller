package com.example.storyteller.ui.activity;

import android.content.Intent;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.storyteller.R;
import com.example.storyteller.base.BaseActivity;
import com.example.storyteller.data.local.db.StoryDao;
import com.example.storyteller.data.local.prefs.PrefsUtils;
import com.example.storyteller.data.remote.ApiClient;
import com.example.storyteller.model.Chapter;
import com.example.storyteller.model.PlotChapterSummary;
import com.example.storyteller.model.PlotOverviewSummary;
import com.example.storyteller.model.PlotSummarySnapshot;
import com.example.storyteller.model.Story;
import com.example.storyteller.model.Volume;
import com.example.storyteller.ui.adapter.PlotChapterSummaryAdapter;
import com.example.storyteller.ui.adapter.StoryAdapter;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PlotTreeActivity extends BaseActivity {

    public static final String EXTRA_STORY_ID = StoryAdapter.EXTRA_STORY_ID;

    private static final String PREF_PLOT_MODEL = "pref_plot_model";
    private static final String PREF_PLOT_DETAIL = "pref_plot_detail";
    private static final Type VOLUME_LIST_TYPE = new TypeToken<List<Volume>>() {}.getType();
    private static final int MAX_CHAPTER_CONTENT_LENGTH = 2800;
    private static final int SMALL_STORY_MAX_CHAPTERS = 4;
    private static final int SMALL_STORY_MAX_TOTAL_LENGTH = 6000;
    private static final int BRIEF_CHAPTER_CONTENT_LENGTH = 900;
    private static final int BRIEF_SINGLE_PASS_MAX_CHAPTERS = 12;
    private static final int BRIEF_SINGLE_PASS_MAX_TOTAL_LENGTH = 12000;
    private static final int BRIEF_BATCH_MAX_CHAPTERS = 8;
    private static final int BRIEF_BATCH_MAX_TOTAL_LENGTH = 7200;
    private static final int MAX_CHARACTER_REUSE_CONTEXT_LENGTH = 4200;
    private static final String PATH_SINGLE_PASS = "single_pass";
    private static final String PATH_BRIEF_BATCH = "brief_batch";
    private static final String PATH_CHAPTER_LOOP = "chapter_loop";
    private static final String OVERVIEW_SOURCE_AI = "ai";
    private static final String OVERVIEW_SOURCE_LOCAL_BRIEF = "local_brief";
    private static final String OVERVIEW_SOURCE_LOCAL_FALLBACK = "local_fallback";
    private static final String CHAPTER_SOURCE_AI = "ai";
    private static final String CHAPTER_SOURCE_TOLERANT = "tolerant";
    private static final String CHAPTER_SOURCE_FALLBACK = "fallback";

    private TextView tvCurrentStoryTitle;
    private TextView tvStatus;
    private ProgressBar pbLoading;
    private Button btnDetailSelector;
    private Button btnGenerate;
    private PlotChapterSummaryAdapter adapter;
    private StoryDao storyDao;
    private final Gson gson = new Gson();
    private final GenerationDiagnostics currentDiagnostics = new GenerationDiagnostics();
    private Story currentStory;
    private PlotOverviewSummary currentOverviewSummary;
    private List<PlotChapterSummary> currentChapterSummaries = new ArrayList<>();
    private String currentOverviewSource = OVERVIEW_SOURCE_LOCAL_FALLBACK;
    private PlotSummarySnapshot currentPlotSnapshot;

    private String currentModel = "flash";
    private String currentDetail = "standard";
    private int generationToken = 0;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_plot_tree;
    }

    @Override
    protected void initView() {
        // 刘海屏适配：为根布局设置系统栏内边距
        applySystemWindowInsets(findViewById(android.R.id.content));
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        tvCurrentStoryTitle = findViewById(R.id.tv_plot_current_story_title);
        tvStatus = findViewById(R.id.tv_plot_status);
        pbLoading = findViewById(R.id.pb_plot_loading);
        btnDetailSelector = findViewById(R.id.btn_plot_detail_selector);
        btnGenerate = findViewById(R.id.btn_generate_plot_summary);

        RecyclerView rvChapterSummaries = findViewById(R.id.rv_plot_chapter_summaries);
        rvChapterSummaries.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PlotChapterSummaryAdapter(new ArrayList<>());
        adapter.setListener((summary, position) -> {
            if (isPlotGenerationInProgress()) {
                Toast.makeText(this, R.string.plot_manual_edit_disabled_loading, Toast.LENGTH_SHORT).show();
                return;
            }
            showEditChapterDialog(summary, position);
        });
        rvChapterSummaries.setAdapter(adapter);

        currentModel = PrefsUtils.getInstance(this).getString(PREF_PLOT_MODEL, "flash");
        currentDetail = PrefsUtils.getInstance(this).getString(PREF_PLOT_DETAIL, "standard");
        updateDetailButtonText();

        btnDetailSelector.setOnClickListener(v -> showDetailSelectorPopup());
        btnGenerate.setOnClickListener(v -> promptModelThenGenerate());
    }

    @Override
    protected void initData() {
        storyDao = new StoryDao(this);
        currentStory = resolveSelectedStory();
        if (currentStory == null) {
            tvCurrentStoryTitle.setText(getString(R.string.title_plot_tree));
            showEmpty(getString(R.string.plot_status_no_story));
            btnGenerate.setEnabled(false);
            return;
        }
        tvCurrentStoryTitle.setText(getString(R.string.plot_current_story_format, currentStory.getTitle()));
        tvStatus.setText(getString(R.string.plot_status_ready));
        loadCachedPlotSummary(currentStory);
    }

    private void startPlotSummaryGeneration() {
        currentStory = resolveSelectedStory();
        if (currentStory == null) {
            showEmpty(getString(R.string.plot_status_no_story));
            return;
        }

        List<ChapterContext> chapterContexts = buildChapterContexts(currentStory);
        if (chapterContexts.isEmpty()) {
            showEmpty(getString(R.string.plot_status_no_story));
            return;
        }

        currentDiagnostics.reset(chapterContexts.size());

        int token = ++generationToken;
        btnGenerate.setEnabled(false);
        btnDetailSelector.setEnabled(false);
        pbLoading.setVisibility(android.view.View.VISIBLE);
        currentPlotSnapshot = null;
        currentOverviewSummary = null;
        currentChapterSummaries = new ArrayList<>();
        currentOverviewSource = OVERVIEW_SOURCE_LOCAL_FALLBACK;
        adapter.setData(null, new ArrayList<>(), currentDetail);
        if (shouldUseBriefBatchSummary(chapterContexts)) {
            currentDiagnostics.setGenerationPath(PATH_BRIEF_BATCH);
            summarizeBriefInBatches(token, currentStory, buildBriefBatches(chapterContexts), 0, new ArrayList<>());
            return;
        }
        if (shouldUseSinglePassSummary(chapterContexts)) {
            currentDiagnostics.setGenerationPath(PATH_SINGLE_PASS);
            summarizeInSinglePass(token, currentStory, chapterContexts);
            return;
        }
        currentDiagnostics.setGenerationPath(PATH_CHAPTER_LOOP);
        summarizeChapterAt(token, currentStory, chapterContexts, 0, new ArrayList<>());
    }

    private boolean shouldUseSinglePassSummary(List<ChapterContext> chapterContexts) {
        if (chapterContexts == null || chapterContexts.isEmpty()) {
            return false;
        }
        int maxChapterCount = isBriefMode() ? BRIEF_SINGLE_PASS_MAX_CHAPTERS : SMALL_STORY_MAX_CHAPTERS;
        int maxTotalLength = isBriefMode() ? BRIEF_SINGLE_PASS_MAX_TOTAL_LENGTH : SMALL_STORY_MAX_TOTAL_LENGTH;
        if (chapterContexts.size() > maxChapterCount) {
            return false;
        }
        return calculateTotalLength(chapterContexts) <= maxTotalLength;
    }

    private boolean shouldUseBriefBatchSummary(List<ChapterContext> chapterContexts) {
        if (!isBriefMode() || chapterContexts == null || chapterContexts.size() <= 1) {
            return false;
        }
        if (shouldUseSinglePassSummary(chapterContexts)) {
            return false;
        }
        return chapterContexts.size() > BRIEF_SINGLE_PASS_MAX_CHAPTERS
                || calculateTotalLength(chapterContexts) > BRIEF_SINGLE_PASS_MAX_TOTAL_LENGTH;
    }

    private int calculateTotalLength(List<ChapterContext> chapterContexts) {
        int totalLength = 0;
        if (chapterContexts == null) {
            return totalLength;
        }
        for (ChapterContext chapter : chapterContexts) {
            if (chapter != null && !TextUtils.isEmpty(chapter.content)) {
                totalLength += chapter.content.length();
            }
        }
        return totalLength;
    }

    private List<List<ChapterContext>> buildBriefBatches(List<ChapterContext> chapterContexts) {
        List<List<ChapterContext>> batches = new ArrayList<>();
        if (chapterContexts == null || chapterContexts.isEmpty()) {
            return batches;
        }

        List<ChapterContext> currentBatch = new ArrayList<>();
        int currentLength = 0;
        for (ChapterContext chapter : chapterContexts) {
            int chapterLength = chapter == null || TextUtils.isEmpty(chapter.content) ? 0 : chapter.content.length();
            boolean shouldStartNewBatch = !currentBatch.isEmpty()
                    && (currentBatch.size() >= BRIEF_BATCH_MAX_CHAPTERS
                    || currentLength + chapterLength > BRIEF_BATCH_MAX_TOTAL_LENGTH);
            if (shouldStartNewBatch) {
                batches.add(currentBatch);
                currentBatch = new ArrayList<>();
                currentLength = 0;
            }
            currentBatch.add(chapter);
            currentLength += chapterLength;
        }
        if (!currentBatch.isEmpty()) {
            batches.add(currentBatch);
        }
        return batches;
    }

    private void summarizeInSinglePass(int token, Story story, List<ChapterContext> chapterContexts) {
        currentDiagnostics.setChunkCount(1);
        tvStatus.setText(String.format(Locale.CHINA,
                "正在使用 %s（%s）快速梳理短篇剧情…",
                getModelDisplayName(), getDetailDisplayName()));

        String prompt = buildSinglePassPrompt(story, chapterContexts);
        ApiClient.getInstance().generateStory(prompt, currentModel, this, createSinglePassRequestOptions(chapterContexts), new ApiClient.Callback() {
            @Override
            public void onSuccess(String responseText) {
                runOnUiThread(() -> {
                    if (token != generationToken) {
                        return;
                    }
                    SinglePassResult result = parseSinglePassResult(chapterContexts, responseText);
                    currentDiagnostics.recordSinglePassResult(result);
                    renderOverviewSummary(story, result.overview, result.chapterSummaries,
                            result.chapterSummaries.size(), result.overviewSource);
                });
            }

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> {
                    if (token != generationToken) {
                        return;
                    }
                    // 快速梳理失败时回退到原来的逐章模式，保证可用性
                    currentDiagnostics.markIssue("单次快路径请求失败，已回退逐章梳理");
                    currentDiagnostics.setGenerationPath(PATH_CHAPTER_LOOP);
                    summarizeChapterAt(token, story, chapterContexts, 0, new ArrayList<>());
                });
            }
        });
    }

    private void summarizeBriefInBatches(int token, Story story, List<List<ChapterContext>> batches, int batchIndex,
                                         List<PlotChapterSummary> collected) {
        if (token != generationToken) {
            return;
        }
        if (batches == null || batches.isEmpty() || batchIndex >= batches.size()) {
            summarizeOverview(token, story, collected);
            return;
        }

        List<ChapterContext> batch = batches.get(batchIndex);
        currentDiagnostics.setChunkCount(batches.size());
        tvStatus.setText(String.format(Locale.CHINA,
                "正在使用 %s（%s）分批快速梳理第 %d/%d 批…",
                getModelDisplayName(), getDetailDisplayName(), batchIndex + 1, batches.size()));

        String prompt = buildSinglePassPrompt(story, batch);
        ApiClient.getInstance().generateStory(prompt, currentModel, this, createSinglePassRequestOptions(batch), new ApiClient.Callback() {
            @Override
            public void onSuccess(String responseText) {
                runOnUiThread(() -> {
                    if (token != generationToken) {
                        return;
                    }
                    SinglePassResult result = parseSinglePassResult(batch, responseText);
                    currentDiagnostics.recordSinglePassResult(result);
                    collected.addAll(result.chapterSummaries);
                    adapter.setData(new ArrayList<>(collected), currentDetail);
                    summarizeBriefInBatches(token, story, batches, batchIndex + 1, collected);
                });
            }

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> {
                    if (token != generationToken) {
                        return;
                    }
                    SinglePassResult fallbackResult = createFallbackSinglePassResult(batch);
                    currentDiagnostics.recordSinglePassResult(fallbackResult);
                    currentDiagnostics.markIssue(String.format(Locale.CHINA,
                            "第 %d 批请求失败，已使用本地回退", batchIndex + 1));
                    collected.addAll(fallbackResult.chapterSummaries);
                    adapter.setData(new ArrayList<>(collected), currentDetail);
                    summarizeBriefInBatches(token, story, batches, batchIndex + 1, collected);
                });
            }
        });
    }

    private void summarizeChapterAt(int token, Story story, List<ChapterContext> chapters, int index,
                                    List<PlotChapterSummary> collected) {
        if (token != generationToken) {
            return;
        }
        if (index >= chapters.size()) {
            summarizeOverview(token, story, collected);
            return;
        }

        ChapterContext chapter = chapters.get(index);
        tvStatus.setText(String.format(Locale.CHINA,
                "正在使用 %s（%s）梳理第 %d/%d 章…",
                getModelDisplayName(), getDetailDisplayName(), index + 1, chapters.size()));

        String prompt = buildChapterSummaryPrompt(story, chapter);
        ApiClient.getInstance().generateStory(prompt, currentModel, this, createChapterRequestOptions(chapter), new ApiClient.Callback() {
            @Override
            public void onSuccess(String responseText) {
                runOnUiThread(() -> {
                    if (token != generationToken) {
                        return;
                    }
                    ChapterParseResult result = parseChapterSummary(chapter, responseText);
                    currentDiagnostics.recordChapterSource(result.source);
                    collected.add(result.summary);
                    adapter.setData(new ArrayList<>(collected), currentDetail);
                    summarizeChapterAt(token, story, chapters, index + 1, collected);
                });
            }

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> {
                    if (token != generationToken) {
                        return;
                    }
                    collected.add(createFallbackChapterSummary(chapter));
                    currentDiagnostics.recordChapterSource(CHAPTER_SOURCE_FALLBACK);
                    currentDiagnostics.markIssue(String.format(Locale.CHINA,
                            "第 %d 章请求失败，已使用本地回退", index + 1));
                    adapter.setData(new ArrayList<>(collected), currentDetail);
                    Toast.makeText(PlotTreeActivity.this,
                            getString(R.string.plot_status_partial_failed, index + 1),
                            Toast.LENGTH_SHORT).show();
                    summarizeChapterAt(token, story, chapters, index + 1, collected);
                });
            }
        });
    }

    private void summarizeOverview(int token, Story story, List<PlotChapterSummary> chapterSummaries) {
        if (chapterSummaries.isEmpty()) {
            finishLoading(getString(R.string.plot_no_chapter_summary), true);
            return;
        }

        if (isBriefMode()) {
            PlotOverviewSummary briefOverview = createLocalOverview(chapterSummaries);
            renderOverviewSummary(story, briefOverview, chapterSummaries, chapterSummaries.size(), OVERVIEW_SOURCE_LOCAL_BRIEF);
            return;
        }

        tvStatus.setText(String.format(Locale.CHINA, "正在使用 %s 汇总全书剧情…", getModelDisplayName()));
        String prompt = buildOverviewPrompt(story, chapterSummaries);
        ApiClient.getInstance().generateStory(prompt, currentModel, this, createOverviewRequestOptions(), new ApiClient.Callback() {
            @Override
            public void onSuccess(String responseText) {
                runOnUiThread(() -> {
                    if (token != generationToken) {
                        return;
                    }
                    PlotOverviewSummary summary = parseOverviewSummary(responseText);
                    renderOverviewSummary(story, summary, chapterSummaries, chapterSummaries.size(), OVERVIEW_SOURCE_AI);
                });
            }

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> {
                    if (token != generationToken) {
                        return;
                    }
                    PlotOverviewSummary fallback = createFallbackOverview(chapterSummaries);
                    renderOverviewSummary(story, fallback, chapterSummaries, chapterSummaries.size(), OVERVIEW_SOURCE_LOCAL_FALLBACK);
                    Toast.makeText(PlotTreeActivity.this,
                            getString(R.string.plot_status_failed, e == null ? "未知错误" : e.getMessage()),
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void renderOverviewSummary(Story story, PlotOverviewSummary summary, List<PlotChapterSummary> chapterSummaries,
                                       int chapterCount, String overviewSource) {
        currentOverviewSummary = summary;
        currentChapterSummaries = chapterSummaries == null ? new ArrayList<>() : new ArrayList<>(chapterSummaries);
        currentOverviewSource = TextUtils.isEmpty(overviewSource) ? OVERVIEW_SOURCE_LOCAL_FALLBACK : overviewSource;
        adapter.setData(summary, chapterSummaries, currentDetail);
        currentDiagnostics.setOverviewSource(overviewSource);
        persistPlotSummary(story, summary, chapterSummaries, overviewSource);
        finishLoading(buildStatusWithDiagnostics(String.format(Locale.CHINA,
                "已完成 %d 章剧情梳理（%s / %s）",
                chapterCount,
                getModelDisplayName(),
                getDetailDisplayName()), currentDiagnostics.buildSummary()), false);
        btnGenerate.setText(R.string.action_regenerate_plot_summary);
    }

    private void loadCachedPlotSummary(Story story) {
        if (story == null || TextUtils.isEmpty(story.getPlotSummaryJson())) {
            return;
        }
        try {
            PlotSummarySnapshot snapshot = gson.fromJson(story.getPlotSummaryJson(), PlotSummarySnapshot.class);
            if (snapshot == null) {
                return;
            }
            if (!TextUtils.isEmpty(snapshot.getModel())) {
                currentModel = snapshot.getModel();
            }
            if (!TextUtils.isEmpty(snapshot.getDetailLevel())) {
                currentDetail = snapshot.getDetailLevel();
                updateDetailButtonText();
            }
            if (snapshot.getChapterSummaries() != null && !snapshot.getChapterSummaries().isEmpty()) {
                if (isLowQualitySnapshot(snapshot)) {
                    tvStatus.setText(buildStatusWithDiagnostics(
                            "检测到旧版剧情缓存质量较低，请重新梳理一次剧情",
                            buildCacheDiagnosticsSummary(snapshot)));
                    btnGenerate.setText(R.string.action_regenerate_plot_summary);
                    return;
                }
                currentPlotSnapshot = snapshot;
                currentOverviewSummary = snapshot.getOverview();
                currentChapterSummaries = new ArrayList<>(snapshot.getChapterSummaries());
                currentOverviewSource = TextUtils.isEmpty(snapshot.getOverviewSource())
                        ? OVERVIEW_SOURCE_LOCAL_FALLBACK
                        : snapshot.getOverviewSource();
                adapter.setData(snapshot.getOverview(), snapshot.getChapterSummaries(), currentDetail);
                tvStatus.setText(buildStatusWithDiagnostics(String.format(Locale.CHINA,
                        "已加载本地剧情梳理结果（%d 章，%s / %s）",
                        snapshot.getChapterSummaries().size(),
                        getModelDisplayName(),
                        getDetailDisplayName()), buildCacheDiagnosticsSummary(snapshot)));
                btnGenerate.setText(R.string.action_regenerate_plot_summary);
            }
        } catch (Exception ignored) {
        }
    }

    private void persistPlotSummary(Story story, PlotOverviewSummary overview, List<PlotChapterSummary> chapterSummaries,
                                    String overviewSource) {
        if (story == null || story.getId() <= 0) {
            return;
        }
        PlotSummarySnapshot snapshot = new PlotSummarySnapshot();
        snapshot.setSchemaVersion(3);
        snapshot.setModel(currentModel);
        snapshot.setDetailLevel(currentDetail);
        snapshot.setGeneratedAt(System.currentTimeMillis());
        snapshot.setGenerationPath(currentDiagnostics.getGenerationPath());
        snapshot.setOverviewSource(overviewSource);
        snapshot.setDiagnosticsSummary(currentDiagnostics.buildSummary());
        snapshot.setChunkCount(currentDiagnostics.getChunkCount());
        snapshot.setAiChapterCount(currentDiagnostics.getAiChapterCount());
        snapshot.setTolerantChapterCount(currentDiagnostics.getTolerantChapterCount());
        snapshot.setFallbackChapterCount(currentDiagnostics.getFallbackChapterCount());
        snapshot.setCharacterContext(buildCharacterReuseContext(story, overview, chapterSummaries));
        snapshot.setOverview(overview);
        snapshot.setChapterSummaries(chapterSummaries);
        String json = gson.toJson(snapshot);
        story.setPlotSummaryJson(json);
        storyDao.updatePlotSummary(story.getId(), json);
        currentPlotSnapshot = snapshot;
    }

    private boolean isPlotGenerationInProgress() {
        return btnGenerate != null && !btnGenerate.isEnabled();
    }

    private void showEditChapterDialog(PlotChapterSummary chapter, int position) {
        if (chapter == null || currentStory == null) {
            Toast.makeText(this, R.string.plot_manual_edit_no_data, Toast.LENGTH_SHORT).show();
            return;
        }
        if (position < 0 || position >= currentChapterSummaries.size()) {
            Toast.makeText(this, R.string.plot_manual_edit_no_data, Toast.LENGTH_SHORT).show();
            return;
        }

        android.view.View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_plot_chapter, null);
        EditText etBrief = dialogView.findViewById(R.id.et_plot_edit_brief_summary);
        EditText etDetail = dialogView.findViewById(R.id.et_plot_edit_detail_summary);
        EditText etEvents = dialogView.findViewById(R.id.et_plot_edit_key_events);
        EditText etCharacters = dialogView.findViewById(R.id.et_plot_edit_characters);

        etBrief.setText(chapter.getBriefSummary());
        etDetail.setText(chapter.getDetailSummary());
        etEvents.setText(joinWithNewLine(chapter.getKeyEvents()));
        etCharacters.setText(joinWithNewLine(chapter.getCharacters()));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.title_edit_plot_chapter, chapter.getChapterLabel()))
                .setView(dialogView)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_save, null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String brief = textOf(etBrief);
            if (TextUtils.isEmpty(brief)) {
                etBrief.setError(getString(R.string.plot_manual_edit_brief_required));
                return;
            }

            PlotChapterSummary updated = copyChapterSummary(currentChapterSummaries.get(position));
            updated.setBriefSummary(brief);
            updated.setDetailSummary(textOf(etDetail));
            updated.setKeyEvents(splitToList(textOf(etEvents)));
            updated.setCharacters(splitToList(textOf(etCharacters)));

            currentChapterSummaries.set(position, updated);
            if (currentOverviewSummary == null) {
                currentOverviewSummary = createFallbackOverview(currentChapterSummaries);
            }
            adapter.setData(currentOverviewSummary, new ArrayList<>(currentChapterSummaries), currentDetail);
            persistManualPlotSummary();
            tvStatus.setText(getString(R.string.plot_manual_edit_saved, updated.getChapterLabel()));
            Toast.makeText(this, R.string.plot_manual_edit_saved_toast, Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        }));
        dialog.show();
    }

    private void persistManualPlotSummary() {
        if (currentStory == null || currentStory.getId() <= 0) {
            return;
        }

        PlotSummarySnapshot snapshot = currentPlotSnapshot == null ? new PlotSummarySnapshot() : currentPlotSnapshot;
        PlotOverviewSummary overviewToSave = currentOverviewSummary == null
                ? createFallbackOverview(currentChapterSummaries)
                : currentOverviewSummary;

        snapshot.setSchemaVersion(3);
        snapshot.setModel(currentModel);
        snapshot.setDetailLevel(currentDetail);
        snapshot.setGeneratedAt(System.currentTimeMillis());
        if (TextUtils.isEmpty(snapshot.getGenerationPath())) {
            snapshot.setGenerationPath(PATH_CHAPTER_LOOP);
        }
        if (TextUtils.isEmpty(currentOverviewSource)) {
            currentOverviewSource = firstNonEmpty(snapshot.getOverviewSource(), OVERVIEW_SOURCE_LOCAL_FALLBACK);
        }
        snapshot.setOverviewSource(currentOverviewSource);

        String diagnostics = snapshot.getDiagnosticsSummary();
        if (TextUtils.isEmpty(diagnostics)) {
            diagnostics = getString(R.string.plot_manual_edit_diagnostics);
        } else if (!diagnostics.contains(getString(R.string.plot_manual_edit_tag))) {
            diagnostics = diagnostics + " / " + getString(R.string.plot_manual_edit_tag);
        }
        snapshot.setDiagnosticsSummary(diagnostics);
        snapshot.setOverview(overviewToSave);
        snapshot.setChapterSummaries(new ArrayList<>(currentChapterSummaries));
        snapshot.setCharacterContext(buildCharacterReuseContext(currentStory, overviewToSave, currentChapterSummaries));

        String json = gson.toJson(snapshot);
        currentStory.setPlotSummaryJson(json);
        storyDao.updatePlotSummary(currentStory.getId(), json);
        currentPlotSnapshot = snapshot;
        currentOverviewSummary = overviewToSave;
    }

    private PlotChapterSummary copyChapterSummary(PlotChapterSummary original) {
        PlotChapterSummary copy = new PlotChapterSummary();
        copy.setVolumeIndex(original.getVolumeIndex());
        copy.setChapterIndex(original.getChapterIndex());
        copy.setChapterLabel(original.getChapterLabel());
        copy.setChapterTitle(original.getChapterTitle());
        copy.setBriefSummary(original.getBriefSummary());
        copy.setDetailSummary(original.getDetailSummary());
        copy.setKeyEvents(original.getKeyEvents() == null ? new ArrayList<>() : new ArrayList<>(original.getKeyEvents()));
        copy.setCharacters(original.getCharacters() == null ? new ArrayList<>() : new ArrayList<>(original.getCharacters()));
        copy.setConflict(original.getConflict());
        copy.setStoryFunction(original.getStoryFunction());
        copy.setSource(original.getSource());
        return copy;
    }

    private String joinWithNewLine(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return TextUtils.join("\n", values);
    }

    private String textOf(EditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }

    private boolean isLowQualitySnapshot(PlotSummarySnapshot snapshot) {
        if (snapshot == null || snapshot.getChapterSummaries() == null || snapshot.getChapterSummaries().isEmpty()) {
            return false;
        }
        int degradedCount = 0;
        for (PlotChapterSummary chapter : snapshot.getChapterSummaries()) {
            if (chapter == null) {
                continue;
            }
            boolean looksTruncated = !TextUtils.isEmpty(chapter.getBriefSummary())
                    && chapter.getBriefSummary().trim().endsWith("……");
            boolean lacksStructure = (chapter.getKeyEvents() == null || chapter.getKeyEvents().isEmpty())
                    && (chapter.getCharacters() == null || chapter.getCharacters().isEmpty());
            if (looksTruncated && lacksStructure) {
                degradedCount++;
            }
        }
        return degradedCount * 2 >= snapshot.getChapterSummaries().size();
    }

    private String buildStatusWithDiagnostics(String baseStatus, String diagnosticsSummary) {
        if (TextUtils.isEmpty(diagnosticsSummary)) {
            return baseStatus;
        }
        return baseStatus + "\n诊断：" + diagnosticsSummary;
    }

    private String buildCacheDiagnosticsSummary(PlotSummarySnapshot snapshot) {
        if (snapshot == null) {
            return "";
        }
        if (!TextUtils.isEmpty(snapshot.getDiagnosticsSummary())) {
            return snapshot.getDiagnosticsSummary();
        }
        List<String> parts = new ArrayList<>();
        if (!TextUtils.isEmpty(snapshot.getGenerationPath())) {
            parts.add("路径=" + displayGenerationPath(snapshot.getGenerationPath(), snapshot.getChunkCount()));
        }
        if (!TextUtils.isEmpty(snapshot.getOverviewSource())) {
            parts.add("概览=" + displayOverviewSource(snapshot.getOverviewSource()));
        }
        if (snapshot.getAiChapterCount() > 0) {
            parts.add("AI=" + snapshot.getAiChapterCount() + "章");
        }
        if (snapshot.getTolerantChapterCount() > 0) {
            parts.add("容错=" + snapshot.getTolerantChapterCount() + "章");
        }
        if (snapshot.getFallbackChapterCount() > 0) {
            parts.add("回退=" + snapshot.getFallbackChapterCount() + "章");
        }
        return parts.isEmpty() ? "" : TextUtils.join(" / ", parts);
    }

    private void finishLoading(String status, boolean clearList) {
        pbLoading.setVisibility(android.view.View.GONE);
        btnGenerate.setEnabled(true);
        btnDetailSelector.setEnabled(true);
        tvStatus.setText(status);
        if (clearList) {
            adapter.setData(null, new ArrayList<>(), currentDetail);
        }
    }

    private void showEmpty(String message) {
        finishLoading(message, true);
    }

    private void promptModelThenGenerate() {
        if (isPlotGenerationInProgress()) {
            return;
        }
        String[] modelLabels = new String[] {
                getString(R.string.model_flash),
                getString(R.string.model_pro)
        };
        int checkedItem = "pro".equals(currentModel) ? 1 : 0;
        final int[] selectedItem = {checkedItem};

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.plot_model_dialog_title)
                .setSingleChoiceItems(modelLabels, checkedItem, (d, which) -> selectedItem[0] = which)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(btnGenerate.getText(), (d, which) -> {
                    currentModel = selectedItem[0] == 1 ? "pro" : "flash";
                    PrefsUtils.getInstance(this).putString(PREF_PLOT_MODEL, currentModel);
                    Toast.makeText(this,
                            selectedItem[0] == 1 ? R.string.plot_model_switched_pro : R.string.plot_model_switched_flash,
                            Toast.LENGTH_SHORT).show();
                    startPlotSummaryGeneration();
                })
                .create();
        dialog.show();
    }

    private void showDetailSelectorPopup() {
        PopupMenu popupMenu = new PopupMenu(this, btnDetailSelector);
        popupMenu.getMenu().add(0, 1, 0, getString(R.string.plot_detail_brief));
        popupMenu.getMenu().add(0, 2, 1, getString(R.string.plot_detail_standard));
        popupMenu.getMenu().add(0, 3, 2, getString(R.string.plot_detail_detailed));
        popupMenu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                currentDetail = "brief";
                PrefsUtils.getInstance(this).putString(PREF_PLOT_DETAIL, currentDetail);
                updateDetailButtonText();
                Toast.makeText(this, R.string.plot_detail_switched_brief, Toast.LENGTH_SHORT).show();
                return true;
            } else if (item.getItemId() == 2) {
                currentDetail = "standard";
                PrefsUtils.getInstance(this).putString(PREF_PLOT_DETAIL, currentDetail);
                updateDetailButtonText();
                Toast.makeText(this, R.string.plot_detail_switched_standard, Toast.LENGTH_SHORT).show();
                return true;
            } else if (item.getItemId() == 3) {
                currentDetail = "detailed";
                PrefsUtils.getInstance(this).putString(PREF_PLOT_DETAIL, currentDetail);
                updateDetailButtonText();
                Toast.makeText(this, R.string.plot_detail_switched_detailed, Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });
        popupMenu.show();
    }


    private void updateDetailButtonText() {
        btnDetailSelector.setText(getDetailDisplayName());
    }

    private String getModelDisplayName() {
        return "flash".equals(currentModel) ? getString(R.string.model_flash) : getString(R.string.model_pro);
    }

    private String getDetailDisplayName() {
        switch (currentDetail) {
            case "brief":
                return getString(R.string.plot_detail_brief);
            case "detailed":
                return getString(R.string.plot_detail_detailed);
            default:
                return getString(R.string.plot_detail_standard);
        }
    }

    private boolean isBriefMode() {
        return "brief".equals(currentDetail);
    }

    private ApiClient.RequestOptions createSinglePassRequestOptions(List<ChapterContext> chapterContexts) {
        ApiClient.RequestOptions options = new ApiClient.RequestOptions().setTemperature(0.2);
        int chapterCount = chapterContexts == null ? 1 : Math.max(1, chapterContexts.size());
        if (isBriefMode()) {
            return options.setMaxTokens(Math.min(2200, 900 + chapterCount * 120));
        }
        if ("detailed".equals(currentDetail)) {
            return options.setMaxTokens(Math.min(3200, 1800 + chapterCount * 140));
        }
        return options.setMaxTokens(Math.min(2600, 1400 + chapterCount * 120));
    }

    private ApiClient.RequestOptions createChapterRequestOptions(ChapterContext chapter) {
        ApiClient.RequestOptions options = new ApiClient.RequestOptions().setTemperature(0.2);
        int contentLength = chapter == null || TextUtils.isEmpty(chapter.content) ? 0 : chapter.content.length();
        if (isBriefMode()) {
            return options.setMaxTokens(contentLength > 700 ? 520 : 420);
        }
        if ("detailed".equals(currentDetail)) {
            return options.setMaxTokens(contentLength > 2200 ? 1400 : 1100);
        }
        return options.setMaxTokens(contentLength > 1400 ? 900 : 720);
    }

    private ApiClient.RequestOptions createOverviewRequestOptions() {
        ApiClient.RequestOptions options = new ApiClient.RequestOptions().setTemperature(0.25);
        if ("detailed".equals(currentDetail)) {
            return options.setMaxTokens(800);
        }
        return options.setMaxTokens(500);
    }

    private Story resolveSelectedStory() {
        int storyId = -1;
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(EXTRA_STORY_ID)) {
            storyId = intent.getIntExtra(EXTRA_STORY_ID, -1);
        }
        if (storyId <= 0) {
            String savedId = PrefsUtils.getInstance(this).getString(StoryAdapter.PREF_SELECTED_STORY_ID, "");
            if (!TextUtils.isEmpty(savedId)) {
                try {
                    storyId = Integer.parseInt(savedId);
                } catch (NumberFormatException ignored) {
                    storyId = -1;
                }
            }
        }
        if (storyId > 0) {
            Story story = storyDao.getStoryById(storyId);
            if (story != null) {
                return story;
            }
        }
        return storyDao.getLatestStory();
    }

    private List<ChapterContext> buildChapterContexts(Story story) {
        List<ChapterContext> result = new ArrayList<>();
        int contentLimit = getChapterContentLimit();
        List<Volume> volumes = parseStoryVolumes(story);
        if (!volumes.isEmpty()) {
            for (int i = 0; i < volumes.size(); i++) {
                Volume volume = volumes.get(i);
                List<Chapter> chapters = volume.getChapters();
                if (chapters == null || chapters.isEmpty()) {
                    continue;
                }
                for (int j = 0; j < chapters.size(); j++) {
                    Chapter chapter = chapters.get(j);
                    ChapterContext context = new ChapterContext();
                    context.volumeIndex = i + 1;
                    context.chapterIndex = j + 1;
                    context.title = safeTrim(chapter.getTitle(), "未命名章");
                    context.content = trimContent(chapter.getContent(), contentLimit);
                    result.add(context);
                }
            }
        }

        if (result.isEmpty() && !TextUtils.isEmpty(story.getContent())) {
            ChapterContext context = new ChapterContext();
            context.volumeIndex = 1;
            context.chapterIndex = 1;
            context.title = safeTrim(story.getTitle(), "正文");
            context.content = trimContent(story.getContent(), contentLimit);
            result.add(context);
        }
        return result;
    }

    private int getChapterContentLimit() {
        switch (currentDetail) {
            case "brief":
                return BRIEF_CHAPTER_CONTENT_LENGTH;
            case "standard":
                return 1800;
            default:
                return MAX_CHAPTER_CONTENT_LENGTH;
        }
    }

    private String buildChapterSummaryPrompt(Story story, ChapterContext chapter) {
        if (isBriefMode()) {
            return new StringBuilder()
                    .append("你是一名小说剧情速记助手。请只根据给出的单章内容输出极简剧情卡片。\n")
                    .append("要求：\n")
                    .append("1. 只输出严格 JSON，不要 Markdown，不要解释。\n")
                    .append("2. 不要杜撰正文里没有的信息。\n")
                    .append("3. brief_summary 只写 1 句话，尽量控制在 20 到 40 个字。\n")
                    .append("4. key_events 最多保留 2 条，每条尽量短。\n")
                    .append("5. characters 只保留本章最关键的 1 到 3 人。\n")
                    .append("6. 不要输出 detail_summary、conflict、story_function 等额外字段。\n")
                    .append("7. JSON 格式如下：\n")
                    .append("{\"chapter_title\":\"章节标题\",\"brief_summary\":\"一句话概括\",\"key_events\":[\"事件1\"],\"characters\":[\"人物1\"]}\n\n")
                    .append("小说标题：").append(safeTrim(story.getTitle(), "未命名小说")).append("\n")
                    .append("章节位置：第").append(chapter.volumeIndex).append("卷 第").append(chapter.chapterIndex).append("章\n")
                    .append("章节标题：").append(chapter.title).append("\n")
                    .append("章节正文：\n").append(chapter.content)
                    .toString();
        }

        String detailInstruction;
        if ("detailed".equals(currentDetail)) {
            detailInstruction = "detail_summary 可以写成较完整的一段，key_events 保留 4 到 6 条，并补充章节情绪、伏笔或人物变化。";
        } else {
            detailInstruction = "detail_summary 写成 2 到 4 句的标准梳理，key_events 保留 3 到 4 条。";
        }

        return new StringBuilder()
                .append("你是一名小说剧情梳理助手。请只根据给出的单章内容输出结构化剧情摘要。\n")
                .append("要求：\n")
                .append("1. 只输出严格 JSON，不要 Markdown，不要解释。\n")
                .append("2. 不要杜撰正文里没有的信息。\n")
                .append("3. ").append(detailInstruction).append("\n")
                .append("4. JSON 格式如下：\n")
                .append("{\"chapter_title\":\"章节标题\",\"brief_summary\":\"一句话概括\",\"detail_summary\":\"章节详细梳理\",\"key_events\":[\"事件1\",\"事件2\"],\"characters\":[\"人物1\",\"人物2\"],\"conflict\":\"本章冲突\",\"story_function\":\"本章在整体剧情中的作用\"}\n\n")
                .append("小说标题：").append(safeTrim(story.getTitle(), "未命名小说")).append("\n")
                .append("章节位置：第").append(chapter.volumeIndex).append("卷 第").append(chapter.chapterIndex).append("章\n")
                .append("章节标题：").append(chapter.title).append("\n")
                .append("章节正文：\n").append(chapter.content)
                .toString();
    }

    private String buildOverviewPrompt(Story story, List<PlotChapterSummary> chapterSummaries) {
        StringBuilder chapterContext = new StringBuilder();
        for (PlotChapterSummary summary : chapterSummaries) {
            chapterContext.append(summary.getChapterLabel())
                    .append(" ")
                    .append(summary.getChapterTitle())
                    .append("\n")
                    .append("概述：")
                    .append(summary.getBriefSummary())
                    .append("\n");
            if (!summary.getKeyEvents().isEmpty()) {
                chapterContext.append("事件：").append(TextUtils.join("；", summary.getKeyEvents())).append("\n");
            }
            if (!summary.getCharacters().isEmpty()) {
                chapterContext.append("人物：").append(TextUtils.join("、", summary.getCharacters())).append("\n");
            }
            chapterContext.append("\n");
        }

        return new StringBuilder()
                .append("你是一名小说结构分析助手。请根据给出的各章梳理结果，汇总出全书级剧情梳理。\n")
                .append("要求：\n")
                .append("1. 只输出严格 JSON，不要 Markdown，不要解释。\n")
                .append("2. 不要杜撰章节梳理中没有出现的信息。\n")
                .append("3. JSON 格式如下：\n")
                .append("{\"overall_summary\":\"全书概述\",\"main_line\":[\"主线1\",\"主线2\"],\"turning_points\":[\"转折1\",\"转折2\"],\"character_threads\":[\"人物线1\",\"人物线2\"],\"rhythm\":\"节奏评价\"}\n\n")
                .append("小说标题：").append(safeTrim(story.getTitle(), "未命名小说")).append("\n")
                .append("章节梳理结果：\n")
                .append(chapterContext)
                .toString();
    }

    private String buildSinglePassPrompt(Story story, List<ChapterContext> chapterContexts) {
        if (isBriefMode()) {
            StringBuilder chapterBuilder = new StringBuilder();
            for (ChapterContext chapter : chapterContexts) {
                chapterBuilder.append(getString(R.string.plot_chapter_label_format, chapter.volumeIndex, chapter.chapterIndex))
                        .append("\n标题：")
                        .append(chapter.title)
                        .append("\n正文：\n")
                        .append(chapter.content)
                        .append("\n\n");
            }

            return new StringBuilder()
                    .append("你是一名小说剧情速记助手。请基于下面给出的章节内容，一次性完成所有章节的极简梳理。\n")
                    .append("要求：\n")
                    .append("1. 只输出严格 JSON，不要 Markdown，不要解释。\n")
                    .append("2. 不要杜撰原文中没有的信息。\n")
                    .append("3. 每章只保留一句 brief_summary、最多 2 条 key_events、1 到 3 个关键人物。\n")
                    .append("4. 不要输出 overview，由系统本地根据章节结果汇总。\n")
                    .append("5. JSON 格式如下：\n")
                    .append("{\"chapters\":[{\"chapter_label\":\"第1卷 · 第1章\",\"chapter_title\":\"章节标题\",\"brief_summary\":\"一句话概括\",\"key_events\":[\"事件1\"],\"characters\":[\"人物1\"]}]}\n\n")
                    .append("小说标题：")
                    .append(safeTrim(story.getTitle(), "未命名小说"))
                    .append("\n章节内容：\n")
                    .append(chapterBuilder)
                    .toString();
        }

        String detailInstruction;
        if ("detailed".equals(currentDetail)) {
            detailInstruction = "每章 detail_summary 可以更完整，补充冲突和章节作用，全书概述也可更详细。";
        } else {
            detailInstruction = "每章保持标准梳理深度，全书概述清晰概括主线、转折和人物线。";
        }

        StringBuilder chapterBuilder = new StringBuilder();
        for (ChapterContext chapter : chapterContexts) {
            chapterBuilder.append(getString(R.string.plot_chapter_label_format, chapter.volumeIndex, chapter.chapterIndex))
                    .append("\n标题：")
                    .append(chapter.title)
                    .append("\n正文：\n")
                    .append(chapter.content)
                    .append("\n\n");
        }

        return new StringBuilder()
                .append("你是一名小说剧情梳理助手。请基于下面给出的整段短篇小说章节内容，一次性完成每章梳理和全书概览。\n")
                .append("要求：\n")
                .append("1. 只输出严格 JSON，不要 Markdown，不要解释。\n")
                .append("2. 不要杜撰原文中没有的信息。\n")
                .append("3. ").append(detailInstruction).append("\n")
                .append("4. JSON 格式如下：\n")
                .append("{\"overview\":{\"overall_summary\":\"全书概述\",\"main_line\":[\"主线1\"],\"turning_points\":[\"转折1\"],\"character_threads\":[\"人物线1\"],\"rhythm\":\"节奏评价\"},\"chapters\":[{\"chapter_label\":\"第1卷 · 第1章\",\"chapter_title\":\"章节标题\",\"brief_summary\":\"一句话概括\",\"detail_summary\":\"章节详细梳理\",\"key_events\":[\"事件1\"],\"characters\":[\"人物1\"],\"conflict\":\"本章冲突\",\"story_function\":\"本章作用\"}]}\n\n")
                .append("小说标题：")
                .append(safeTrim(story.getTitle(), "未命名小说"))
                .append("\n")
                .append("章节内容：\n")
                .append(chapterBuilder)
                .toString();
    }

    private ChapterParseResult parseChapterSummary(ChapterContext chapter, String responseText) {
        PlotChapterSummary summary = createFallbackChapterSummary(chapter);
        ChapterParseResult result = new ChapterParseResult();
        result.summary = summary;
        result.source = CHAPTER_SOURCE_FALLBACK;
        String jsonText = extractJson(responseText);
        try {
            JsonObject root = gson.fromJson(jsonText, JsonObject.class);
            if (root == null) {
                return result;
            }
            result.source = detectChapterSource(root);
            summary.setChapterTitle(firstNonEmpty(
                    safeGetString(root, "chapter_title", ""),
                    safeGetString(root, "title", ""),
                    chapter.title));
            summary.setBriefSummary(firstNonEmpty(
                    safeGetString(root, "brief_summary", ""),
                    safeGetString(root, "summary", ""),
                    safeGetString(root, "brief", ""),
                    summary.getBriefSummary()));
            summary.setDetailSummary(firstNonEmpty(
                    safeGetString(root, "detail_summary", ""),
                    safeGetString(root, "detail", ""),
                    safeGetString(root, "summary_detail", ""),
                    summary.getDetailSummary()));
            summary.setConflict(firstNonEmpty(
                    safeGetString(root, "conflict", ""),
                    safeGetString(root, "core_conflict", ""),
                    summary.getConflict()));
            summary.setStoryFunction(firstNonEmpty(
                    safeGetString(root, "story_function", ""),
                    safeGetString(root, "chapter_function", ""),
                    safeGetString(root, "plot_function", ""),
                    summary.getStoryFunction()));
            List<String> keyEvents = firstNonEmptyList(
                    toStringList(root.get("key_events")),
                    toStringList(root.get("events")),
                    toStringList(root.get("highlights")));
            if (!keyEvents.isEmpty()) {
                summary.setKeyEvents(keyEvents);
            }
            List<String> characters = firstNonEmptyList(
                    toStringList(root.get("characters")),
                    toStringList(root.get("roles")),
                    toStringList(root.get("people")));
            if (!characters.isEmpty()) {
                summary.setCharacters(characters);
            }
            applySummaryDefaults(summary, chapter);
        } catch (Exception ignored) {
        }
        summary.setSource(result.source);
        result.summary = summary;
        return result;
    }

    private PlotOverviewSummary parseOverviewSummary(String responseText) {
        PlotOverviewSummary summary = new PlotOverviewSummary();
        String jsonText = extractJson(responseText);
        try {
            JsonObject root = gson.fromJson(jsonText, JsonObject.class);
            if (root == null) {
                return summary;
            }
            summary.setOverallSummary(safeGetString(root, "overall_summary", ""));
            summary.setMainLine(toStringList(root.get("main_line")));
            summary.setTurningPoints(toStringList(root.get("turning_points")));
            summary.setCharacterThreads(toStringList(root.get("character_threads")));
            summary.setRhythm(safeGetString(root, "rhythm", ""));
        } catch (Exception ignored) {
        }
        return summary;
    }

    private SinglePassResult parseSinglePassResult(List<ChapterContext> chapterContexts, String responseText) {
        SinglePassResult result = new SinglePassResult();
        result.overview = new PlotOverviewSummary();
        result.chapterSummaries = new ArrayList<>();
        result.overviewSource = OVERVIEW_SOURCE_AI;

        String jsonText = extractJson(responseText);
        try {
            JsonObject root = gson.fromJson(jsonText, JsonObject.class);
            if (root == null) {
                return createFallbackSinglePassResult(chapterContexts);
            }

            if (!isBriefMode() && root.has("overview") && root.get("overview").isJsonObject()) {
                JsonObject overviewObj = root.getAsJsonObject("overview");
                result.overview.setOverallSummary(safeGetString(overviewObj, "overall_summary", ""));
                result.overview.setMainLine(toStringList(overviewObj.get("main_line")));
                result.overview.setTurningPoints(toStringList(overviewObj.get("turning_points")));
                result.overview.setCharacterThreads(toStringList(overviewObj.get("character_threads")));
                result.overview.setRhythm(safeGetString(overviewObj, "rhythm", ""));
            }

            if (root.has("chapters") && root.get("chapters").isJsonArray()) {
                JsonArray array = root.getAsJsonArray("chapters");
                for (int i = 0; i < array.size(); i++) {
                    if (!array.get(i).isJsonObject()) {
                        continue;
                    }
                    ChapterContext fallbackChapter = i < chapterContexts.size() ? chapterContexts.get(i) : chapterContexts.get(chapterContexts.size() - 1);
                    ChapterParseResult chapterResult = parseChapterSummaryFromObject(fallbackChapter, array.get(i).getAsJsonObject());
                    result.chapterSummaries.add(chapterResult.summary);
                    result.recordChapterSource(chapterResult.source);
                }
            }
        } catch (Exception ignored) {
            return createFallbackSinglePassResult(chapterContexts);
        }

        if (result.chapterSummaries.isEmpty()) {
            return createFallbackSinglePassResult(chapterContexts);
        }
        if (isBriefMode()) {
            result.overview = createLocalOverview(result.chapterSummaries);
            result.overviewSource = OVERVIEW_SOURCE_LOCAL_BRIEF;
            return result;
        }
        if (TextUtils.isEmpty(result.overview.getOverallSummary())) {
            result.overview = createFallbackOverview(result.chapterSummaries);
            result.overviewSource = OVERVIEW_SOURCE_LOCAL_FALLBACK;
        }
        return result;
    }

    private ChapterParseResult parseChapterSummaryFromObject(ChapterContext chapter, JsonObject root) {
        PlotChapterSummary summary = createFallbackChapterSummary(chapter);
        ChapterParseResult result = new ChapterParseResult();
        result.source = detectChapterSource(root);
        summary.setChapterLabel(firstNonEmpty(
                safeGetString(root, "chapter_label", ""),
                safeGetString(root, "label", ""),
                summary.getChapterLabel()));
        summary.setChapterTitle(firstNonEmpty(
                safeGetString(root, "chapter_title", ""),
                safeGetString(root, "title", ""),
                chapter.title));
        summary.setBriefSummary(firstNonEmpty(
                safeGetString(root, "brief_summary", ""),
                safeGetString(root, "summary", ""),
                safeGetString(root, "brief", ""),
                summary.getBriefSummary()));
        summary.setDetailSummary(firstNonEmpty(
                safeGetString(root, "detail_summary", ""),
                safeGetString(root, "detail", ""),
                summary.getDetailSummary()));
        summary.setConflict(firstNonEmpty(
                safeGetString(root, "conflict", ""),
                safeGetString(root, "core_conflict", ""),
                summary.getConflict()));
        summary.setStoryFunction(firstNonEmpty(
                safeGetString(root, "story_function", ""),
                safeGetString(root, "chapter_function", ""),
                summary.getStoryFunction()));
        List<String> keyEvents = firstNonEmptyList(
                toStringList(root.get("key_events")),
                toStringList(root.get("events")),
                toStringList(root.get("highlights")));
        if (!keyEvents.isEmpty()) {
            summary.setKeyEvents(keyEvents);
        }
        List<String> characters = firstNonEmptyList(
                toStringList(root.get("characters")),
                toStringList(root.get("roles")),
                toStringList(root.get("people")));
        if (!characters.isEmpty()) {
            summary.setCharacters(characters);
        }
        applySummaryDefaults(summary, chapter);
        summary.setSource(result.source);
        result.summary = summary;
        return result;
    }

    private SinglePassResult createFallbackSinglePassResult(List<ChapterContext> chapterContexts) {
        SinglePassResult result = new SinglePassResult();
        result.chapterSummaries = new ArrayList<>();
        for (ChapterContext chapter : chapterContexts) {
            result.chapterSummaries.add(createFallbackChapterSummary(chapter));
        }
        result.overview = createFallbackOverview(result.chapterSummaries);
        result.overviewSource = isBriefMode() ? OVERVIEW_SOURCE_LOCAL_BRIEF : OVERVIEW_SOURCE_LOCAL_FALLBACK;
        result.fallbackChapterCount = result.chapterSummaries.size();
        return result;
    }

    private String detectChapterSource(JsonObject root) {
        if (root == null) {
            return CHAPTER_SOURCE_FALLBACK;
        }
        if (root.has("brief_summary") || root.has("detail_summary") || root.has("key_events")
                || root.has("characters") || root.has("story_function") || root.has("conflict")) {
            return CHAPTER_SOURCE_AI;
        }
        if (root.has("summary") || root.has("brief") || root.has("detail")
                || root.has("events") || root.has("highlights") || root.has("roles")
                || root.has("people") || root.has("chapter_function") || root.has("plot_function")) {
            return CHAPTER_SOURCE_TOLERANT;
        }
        return CHAPTER_SOURCE_FALLBACK;
    }

    private PlotChapterSummary createFallbackChapterSummary(ChapterContext chapter) {
        PlotChapterSummary summary = new PlotChapterSummary();
        summary.setVolumeIndex(chapter.volumeIndex);
        summary.setChapterIndex(chapter.chapterIndex);
        summary.setChapterLabel(getString(R.string.plot_chapter_label_format, chapter.volumeIndex, chapter.chapterIndex));
        summary.setChapterTitle(chapter.title);
        summary.setBriefSummary(buildFallbackBriefSummary(chapter));
        summary.setDetailSummary(isBriefMode() ? "" : buildFallbackDetailSummary(chapter));
        summary.setConflict(isBriefMode() ? "" : "文中未自动提炼出明确冲突");
        summary.setStoryFunction(isBriefMode() ? "" : "本章作用待补充");
        summary.setSource(CHAPTER_SOURCE_FALLBACK);
        return summary;
    }

    private void applySummaryDefaults(PlotChapterSummary summary, ChapterContext chapter) {
        if (summary == null || chapter == null) {
            return;
        }
        if (TextUtils.isEmpty(summary.getBriefSummary())) {
            if (!summary.getKeyEvents().isEmpty()) {
                summary.setBriefSummary(summary.getKeyEvents().get(0));
            } else if (!TextUtils.isEmpty(summary.getDetailSummary())) {
                summary.setBriefSummary(extractReadableSentence(summary.getDetailSummary(), isBriefMode() ? 42 : 64));
            } else {
                summary.setBriefSummary(buildFallbackBriefSummary(chapter));
            }
        }
        if (!isBriefMode() && TextUtils.isEmpty(summary.getDetailSummary())) {
            summary.setDetailSummary(!summary.getKeyEvents().isEmpty()
                    ? TextUtils.join("；", summary.getKeyEvents())
                    : buildFallbackDetailSummary(chapter));
        }
        if (summary.getKeyEvents().isEmpty()) {
            String event = extractReadableSentence(chapter.content, 28);
            if (!TextUtils.isEmpty(event)) {
                List<String> fallbackEvents = new ArrayList<>();
                fallbackEvents.add(event);
                summary.setKeyEvents(fallbackEvents);
            }
        }
    }

    private String buildFallbackBriefSummary(ChapterContext chapter) {
        String sentence = extractReadableSentence(chapter.content, isBriefMode() ? 42 : 56);
        if (!TextUtils.isEmpty(sentence)) {
            return sentence;
        }
        return "本章围绕「" + safeTrim(chapter.title, "未命名章") + "」展开";
    }

    private String buildFallbackDetailSummary(ChapterContext chapter) {
        String first = extractReadableSentence(chapter.content, 72);
        String second = extractSecondReadableSentence(chapter.content, 72, first);
        if (!TextUtils.isEmpty(first) && !TextUtils.isEmpty(second) && !TextUtils.equals(first, second)) {
            return first + "；" + second;
        }
        if (!TextUtils.isEmpty(first)) {
            return first;
        }
        return "本章正文已读取，但结构化梳理暂未完成。";
    }

    private PlotOverviewSummary createFallbackOverview(List<PlotChapterSummary> chapterSummaries) {
        if (isBriefMode()) {
            return createLocalOverview(chapterSummaries);
        }
        PlotOverviewSummary summary = new PlotOverviewSummary();
        if (!chapterSummaries.isEmpty()) {
            summary.setOverallSummary(chapterSummaries.get(0).getBriefSummary());
            List<String> mainLine = new ArrayList<>();
            for (PlotChapterSummary chapter : chapterSummaries) {
                if (!TextUtils.isEmpty(chapter.getBriefSummary())) {
                    mainLine.add(chapter.getBriefSummary());
                }
                if (mainLine.size() >= 3) {
                    break;
                }
            }
            summary.setMainLine(mainLine);
        }
        return summary;
    }

    private PlotOverviewSummary createLocalOverview(List<PlotChapterSummary> chapterSummaries) {
        PlotOverviewSummary summary = new PlotOverviewSummary();
        if (chapterSummaries == null || chapterSummaries.isEmpty()) {
            return summary;
        }

        List<String> mainLine = new ArrayList<>();
        List<String> turningPoints = new ArrayList<>();
        List<String> characterThreads = new ArrayList<>();
        StringBuilder overallBuilder = new StringBuilder();
        int summaryLimit = isBriefMode() ? 3 : 4;
        int overallLengthLimit = isBriefMode() ? 96 : 160;

        for (PlotChapterSummary chapter : chapterSummaries) {
            String brief = safeTrim(chapter.getBriefSummary(), "");
            if (!TextUtils.isEmpty(brief)) {
                if (mainLine.size() < summaryLimit) {
                    mainLine.add(brief);
                }
                if (overallBuilder.length() < overallLengthLimit) {
                    if (overallBuilder.length() > 0) {
                        overallBuilder.append("；");
                    }
                    overallBuilder.append(brief);
                }
            }

            if (!isBriefMode()) {
                for (String event : chapter.getKeyEvents()) {
                    String trimmedEvent = safeTrim(event, "");
                    if (!TextUtils.isEmpty(trimmedEvent) && !turningPoints.contains(trimmedEvent)) {
                        turningPoints.add(trimmedEvent);
                    }
                    if (turningPoints.size() >= 4) {
                        break;
                    }
                }
            }

            for (String character : chapter.getCharacters()) {
                String trimmedCharacter = safeTrim(character, "");
                if (!TextUtils.isEmpty(trimmedCharacter) && !characterThreads.contains(trimmedCharacter)) {
                    characterThreads.add(trimmedCharacter);
                }
                if (characterThreads.size() >= (isBriefMode() ? 4 : 6)) {
                    break;
                }
            }
        }

        summary.setOverallSummary(trimContent(overallBuilder.toString(), overallLengthLimit));
        summary.setMainLine(mainLine);
        summary.setTurningPoints(turningPoints);
        summary.setCharacterThreads(characterThreads);
        if (!isBriefMode()) {
            summary.setRhythm(chapterSummaries.size() <= 3 ? "节奏集中，推进较快" : "整体按章节逐步推进");
        }
        return summary;
    }

    private String buildCharacterReuseContext(Story story, PlotOverviewSummary overview, List<PlotChapterSummary> chapterSummaries) {
        StringBuilder builder = new StringBuilder();
        appendWithLimit(builder, "小说标题：" + safeTrim(story.getTitle(), "未命名小说") + "\n", MAX_CHARACTER_REUSE_CONTEXT_LENGTH);
        if (!TextUtils.isEmpty(story.getDescription())) {
            appendWithLimit(builder, "小说简介：" + story.getDescription().trim() + "\n", MAX_CHARACTER_REUSE_CONTEXT_LENGTH);
        }
        if (overview != null) {
            if (!TextUtils.isEmpty(overview.getOverallSummary())) {
                appendWithLimit(builder, "全书梳理：" + overview.getOverallSummary() + "\n", MAX_CHARACTER_REUSE_CONTEXT_LENGTH);
            }
            if (overview.getMainLine() != null && !overview.getMainLine().isEmpty()) {
                appendWithLimit(builder, "主线：" + TextUtils.join("；", overview.getMainLine()) + "\n", MAX_CHARACTER_REUSE_CONTEXT_LENGTH);
            }
        }
        appendWithLimit(builder, "章节梳理：\n", MAX_CHARACTER_REUSE_CONTEXT_LENGTH);
        if (chapterSummaries != null) {
            for (PlotChapterSummary chapter : chapterSummaries) {
                appendWithLimit(builder,
                        chapter.getChapterLabel() + " " + safeTrim(chapter.getChapterTitle(), "未命名章") + "\n",
                        MAX_CHARACTER_REUSE_CONTEXT_LENGTH);
                if (!TextUtils.isEmpty(chapter.getBriefSummary())) {
                    appendWithLimit(builder, "概述：" + chapter.getBriefSummary() + "\n", MAX_CHARACTER_REUSE_CONTEXT_LENGTH);
                }
                if (chapter.getKeyEvents() != null && !chapter.getKeyEvents().isEmpty()) {
                    appendWithLimit(builder, "事件：" + TextUtils.join("；", chapter.getKeyEvents()) + "\n", MAX_CHARACTER_REUSE_CONTEXT_LENGTH);
                }
                if (chapter.getCharacters() != null && !chapter.getCharacters().isEmpty()) {
                    appendWithLimit(builder, "人物：" + TextUtils.join("、", chapter.getCharacters()) + "\n", MAX_CHARACTER_REUSE_CONTEXT_LENGTH);
                }
                appendWithLimit(builder, "\n", MAX_CHARACTER_REUSE_CONTEXT_LENGTH);
                if (builder.length() >= MAX_CHARACTER_REUSE_CONTEXT_LENGTH) {
                    break;
                }
            }
        }
        return builder.toString().trim();
    }

    private void appendWithLimit(StringBuilder builder, String text, int maxLength) {
        if (builder.length() >= maxLength || TextUtils.isEmpty(text)) {
            return;
        }
        int remaining = maxLength - builder.length();
        if (text.length() <= remaining) {
            builder.append(text);
        } else {
            builder.append(text, 0, remaining);
        }
    }


    private List<Volume> parseStoryVolumes(Story story) {
        if (story == null || TextUtils.isEmpty(story.getStructure())) {
            return new ArrayList<>();
        }
        try {
            List<Volume> volumes = gson.fromJson(story.getStructure(), VOLUME_LIST_TYPE);
            return volumes == null ? new ArrayList<>() : volumes;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private List<String> toStringList(JsonElement element) {
        List<String> result = new ArrayList<>();
        if (element == null || element.isJsonNull()) {
            return result;
        }
        if (element.isJsonPrimitive()) {
            return splitToList(element.getAsString());
        }
        if (!element.isJsonArray()) {
            return result;
        }
        JsonArray array = element.getAsJsonArray();
        for (JsonElement item : array) {
            if (item != null && !item.isJsonNull()) {
                if (item.isJsonPrimitive()) {
                    String value = item.getAsString();
                    if (!TextUtils.isEmpty(value)) {
                        result.add(value.trim());
                    }
                } else if (item.isJsonObject()) {
                    String value = firstNonEmpty(
                            safeGetString(item.getAsJsonObject(), "name", ""),
                            safeGetString(item.getAsJsonObject(), "title", ""),
                            safeGetString(item.getAsJsonObject(), "summary", ""));
                    if (!TextUtils.isEmpty(value)) {
                        result.add(value.trim());
                    }
                }
            }
        }
        return result;
    }

    @SafeVarargs
    private final List<String> firstNonEmptyList(List<String>... candidates) {
        if (candidates == null) {
            return new ArrayList<>();
        }
        for (List<String> candidate : candidates) {
            if (candidate != null && !candidate.isEmpty()) {
                return candidate;
            }
        }
        return new ArrayList<>();
    }

    private String firstNonEmpty(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (!TextUtils.isEmpty(value) && !TextUtils.isEmpty(value.trim())) {
                return value.trim();
            }
        }
        return "";
    }

    private String displayGenerationPath(String generationPath, int chunkCount) {
        if (PATH_SINGLE_PASS.equals(generationPath)) {
            return "单次快路径";
        }
        if (PATH_BRIEF_BATCH.equals(generationPath)) {
            return chunkCount > 1 ? "分批快路径（" + chunkCount + "批）" : "分批快路径";
        }
        if (PATH_CHAPTER_LOOP.equals(generationPath)) {
            return "逐章梳理";
        }
        return "未知路径";
    }

    private String displayOverviewSource(String overviewSource) {
        if (OVERVIEW_SOURCE_AI.equals(overviewSource)) {
            return "AI汇总";
        }
        if (OVERVIEW_SOURCE_LOCAL_BRIEF.equals(overviewSource)) {
            return "本地极简汇总";
        }
        if (OVERVIEW_SOURCE_LOCAL_FALLBACK.equals(overviewSource)) {
            return "本地回退汇总";
        }
        return "未知来源";
    }

    private List<String> splitToList(String raw) {
        List<String> result = new ArrayList<>();
        if (TextUtils.isEmpty(raw)) {
            return result;
        }
        String[] parts = raw.split("[\n；;、,，|•·]+");
        for (String part : parts) {
            String trimmed = safeTrim(part, "");
            if (!TextUtils.isEmpty(trimmed)) {
                result.add(trimmed);
            }
        }
        return result;
    }

    private String safeGetString(JsonObject obj, String key, String fallback) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) {
            return fallback;
        }
        return obj.get(key).getAsString();
    }

    private String extractJson(String text) {
        if (TextUtils.isEmpty(text)) {
            return "{}";
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return text;
    }

    private String trimContent(String content, int maxLength) {
        if (TextUtils.isEmpty(content)) {
            return "";
        }
        String trimmed = content.trim();
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, maxLength) + "……";
    }

    private String extractReadableSentence(String content, int maxLength) {
        if (TextUtils.isEmpty(content)) {
            return "";
        }
        String normalized = content.replace("\r", "\n").trim();
        if (TextUtils.isEmpty(normalized)) {
            return "";
        }
        String[] segments = normalized.split("(?<=[。！？!?；;])|\n+");
        for (String segment : segments) {
            String cleaned = cleanFallbackSegment(segment);
            if (!TextUtils.isEmpty(cleaned)) {
                return cutSentenceWithoutEllipsis(cleaned, maxLength);
            }
        }
        return cutSentenceWithoutEllipsis(cleanFallbackSegment(normalized), maxLength);
    }

    private String extractSecondReadableSentence(String content, int maxLength, String firstSentence) {
        if (TextUtils.isEmpty(content)) {
            return "";
        }
        String normalized = content.replace("\r", "\n").trim();
        String[] segments = normalized.split("(?<=[。！？!?；;])|\n+");
        for (String segment : segments) {
            String cleaned = cleanFallbackSegment(segment);
            if (!TextUtils.isEmpty(cleaned) && !TextUtils.equals(cleaned, firstSentence)) {
                return cutSentenceWithoutEllipsis(cleaned, maxLength);
            }
        }
        return "";
    }

    private String cleanFallbackSegment(String text) {
        if (TextUtils.isEmpty(text)) {
            return "";
        }
        String cleaned = text.trim();
        while (cleaned.startsWith("#") || cleaned.startsWith("-") || cleaned.startsWith("•")) {
            cleaned = cleaned.substring(1).trim();
        }
        if (cleaned.endsWith("……")) {
            cleaned = cleaned.substring(0, cleaned.length() - 2).trim();
        }
        return cleaned;
    }

    private String cutSentenceWithoutEllipsis(String text, int maxLength) {
        if (TextUtils.isEmpty(text)) {
            return "";
        }
        String cleaned = text.trim();
        if (cleaned.length() <= maxLength) {
            return cleaned;
        }
        int punctuationIndex = Math.max(
                Math.max(cleaned.lastIndexOf('，', maxLength), cleaned.lastIndexOf('。', maxLength)),
                Math.max(cleaned.lastIndexOf('；', maxLength), cleaned.lastIndexOf('、', maxLength)));
        if (punctuationIndex >= 12) {
            return cleaned.substring(0, punctuationIndex).trim();
        }
        return cleaned.substring(0, maxLength).trim();
    }

    private String safeTrim(String text, String fallback) {
        if (TextUtils.isEmpty(text)) {
            return fallback;
        }
        String trimmed = text.trim();
        return TextUtils.isEmpty(trimmed) ? fallback : trimmed;
    }

    private static class ChapterContext {
        int volumeIndex;
        int chapterIndex;
        String title;
        String content;
    }

    private static class ChapterParseResult {
        PlotChapterSummary summary;
        String source;
    }

    private static class SinglePassResult {
        PlotOverviewSummary overview;
        List<PlotChapterSummary> chapterSummaries;
        String overviewSource;

        int aiChapterCount;
        int tolerantChapterCount;
        int fallbackChapterCount;

        void recordChapterSource(String source) {
            if (CHAPTER_SOURCE_AI.equals(source)) {
                aiChapterCount++;
            } else if (CHAPTER_SOURCE_TOLERANT.equals(source)) {
                tolerantChapterCount++;
            } else {
                fallbackChapterCount++;
            }
        }
    }

    private static class GenerationDiagnostics {
        private String generationPath = "";
        private String overviewSource = "";
        private int chunkCount;
        private int totalChapterCount;
        private int aiChapterCount;
        private int tolerantChapterCount;
        private int fallbackChapterCount;
        private String lastIssue = "";

        void reset(int totalChapterCount) {
            generationPath = "";
            overviewSource = "";
            chunkCount = 0;
            this.totalChapterCount = totalChapterCount;
            aiChapterCount = 0;
            tolerantChapterCount = 0;
            fallbackChapterCount = 0;
            lastIssue = "";
        }

        void setGenerationPath(String generationPath) {
            this.generationPath = generationPath;
        }

        String getGenerationPath() {
            return generationPath;
        }

        void setOverviewSource(String overviewSource) {
            this.overviewSource = overviewSource;
        }

        void setChunkCount(int chunkCount) {
            this.chunkCount = chunkCount;
        }

        int getChunkCount() {
            return chunkCount;
        }

        int getAiChapterCount() {
            return aiChapterCount;
        }

        int getTolerantChapterCount() {
            return tolerantChapterCount;
        }

        int getFallbackChapterCount() {
            return fallbackChapterCount;
        }

        void recordChapterSource(String source) {
            if (CHAPTER_SOURCE_AI.equals(source)) {
                aiChapterCount++;
            } else if (CHAPTER_SOURCE_TOLERANT.equals(source)) {
                tolerantChapterCount++;
            } else {
                fallbackChapterCount++;
            }
        }

        void recordSinglePassResult(SinglePassResult result) {
            if (result == null) {
                return;
            }
            aiChapterCount += result.aiChapterCount;
            tolerantChapterCount += result.tolerantChapterCount;
            fallbackChapterCount += result.fallbackChapterCount;
        }

        void markIssue(String issue) {
            if (!TextUtils.isEmpty(issue)) {
                lastIssue = issue;
            }
        }

        String buildSummary() {
            List<String> parts = new ArrayList<>();
            if (!TextUtils.isEmpty(generationPath)) {
                parts.add("路径=" + displayStaticGenerationPath(generationPath, chunkCount));
            }
            if (totalChapterCount > 0) {
                parts.add("总章数=" + totalChapterCount);
            }
            if (aiChapterCount > 0) {
                parts.add("AI=" + aiChapterCount + "章");
            }
            if (tolerantChapterCount > 0) {
                parts.add("容错=" + tolerantChapterCount + "章");
            }
            if (fallbackChapterCount > 0) {
                parts.add("回退=" + fallbackChapterCount + "章");
            }
            if (!TextUtils.isEmpty(overviewSource)) {
                parts.add("概览=" + displayStaticOverviewSource(overviewSource));
            }
            if (!TextUtils.isEmpty(lastIssue)) {
                parts.add("备注=" + lastIssue);
            }
            return parts.isEmpty() ? "" : TextUtils.join(" / ", parts);
        }

        private static String displayStaticGenerationPath(String generationPath, int chunkCount) {
            if (PATH_SINGLE_PASS.equals(generationPath)) {
                return "单次快路径";
            }
            if (PATH_BRIEF_BATCH.equals(generationPath)) {
                return chunkCount > 1 ? "分批快路径（" + chunkCount + "批）" : "分批快路径";
            }
            if (PATH_CHAPTER_LOOP.equals(generationPath)) {
                return "逐章梳理";
            }
            return "未知路径";
        }

        private static String displayStaticOverviewSource(String overviewSource) {
            if (OVERVIEW_SOURCE_AI.equals(overviewSource)) {
                return "AI汇总";
            }
            if (OVERVIEW_SOURCE_LOCAL_BRIEF.equals(overviewSource)) {
                return "本地极简汇总";
            }
            if (OVERVIEW_SOURCE_LOCAL_FALLBACK.equals(overviewSource)) {
                return "本地回退汇总";
            }
            return "未知来源";
        }
    }
}

