package com.example.storyteller.ui.activity;

import android.content.Intent;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.storyteller.R;
import com.example.storyteller.base.BaseActivity;
import com.example.storyteller.data.local.db.CharacterDao;
import com.example.storyteller.data.local.db.StoryDao;
import com.example.storyteller.data.local.prefs.PrefsUtils;
import com.example.storyteller.data.remote.ApiClient;
import com.example.storyteller.data.remote.ModelConfig;
import com.example.storyteller.model.Chapter;
import com.example.storyteller.model.Character;
import com.example.storyteller.model.PlotChapterSummary;
import com.example.storyteller.model.PlotOverviewSummary;
import com.example.storyteller.model.PlotSummarySnapshot;
import com.example.storyteller.model.Story;
import com.example.storyteller.model.Volume;
import com.example.storyteller.ui.adapter.CharacterAdapter;
import com.example.storyteller.ui.adapter.StoryAdapter;
import com.example.storyteller.ui.dialog.CharacterRegenerateBottomSheetDialogFragment;
import com.example.storyteller.utils.PromptManager;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CharacterActivity extends BaseActivity {

    private static final int MAX_PROMPT_CONTEXT_LENGTH = 9000;
    private static final int MAX_CHAPTER_EXCERPT_LENGTH = 600;
    private static final int MAX_PLAIN_CONTENT_LENGTH = 8000;
    private static final int MAX_PLOT_CACHE_CONTEXT_LENGTH = 4200;
    private static final Type VOLUME_LIST_TYPE = new TypeToken<List<Volume>>() {}.getType();
    private static final String PREF_CHARACTER_MODEL = "pref_character_model";

    public static final String EXTRA_STORY_ID = StoryAdapter.EXTRA_STORY_ID;

    private TextView tvCurrentStoryTitle;
    private ProgressBar pbLoading;
    private TextView tvStatus;
    private Button btnRegenerate;
    private CharacterAdapter adapter;
    private StoryDao storyDao;
    private CharacterDao characterDao;
    private PromptManager promptManager;
    private final Gson gson = new Gson();
    private String currentModel = ModelConfig.DEFAULT_MODEL;

    private int generationToken = 0;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_character;
    }

    @Override
    protected void initView() {
        // 刘海屏适配：为根布局设置系统栏内边距
        applySystemWindowInsets(findViewById(android.R.id.content));
        
        // 初始化 PromptManager
        promptManager = new PromptManager(this);
        
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        RecyclerView rvCharacterList = findViewById(R.id.rv_character_list);
        tvCurrentStoryTitle = findViewById(R.id.tv_current_story_title);
        pbLoading = findViewById(R.id.pb_character_loading);
        tvStatus = findViewById(R.id.tv_character_status);
        btnRegenerate = findViewById(R.id.btn_regenerate_character);

        currentModel = PrefsUtils.getInstance(this).getString(PREF_CHARACTER_MODEL, ModelConfig.DEFAULT_MODEL);

        rvCharacterList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CharacterAdapter(this, new ArrayList<>());
        rvCharacterList.setAdapter(adapter);

        adapter.setListener(new CharacterAdapter.Listener() {
            @Override
            public void onRegenerateCharacter(@androidx.annotation.NonNull Character character, int position) {
                if (isCharacterGenerationInProgress()) {
                    Toast.makeText(CharacterActivity.this, R.string.character_manual_edit_disabled_loading, Toast.LENGTH_SHORT).show();
                    return;
                }
                Story story = resolveSelectedStory();
                if (story == null) {
                    Toast.makeText(CharacterActivity.this, "还没有可分析的小说", Toast.LENGTH_SHORT).show();
                    return;
                }

                CharacterRegenerateBottomSheetDialogFragment dialog =
                        CharacterRegenerateBottomSheetDialogFragment.newInstance(story.getTitle(), character.getName());
                dialog.setListener(extraDemand -> promptModelThenRun(() -> regenerateSingleCharacter(story, character, position, extraDemand)));
                dialog.show(getSupportFragmentManager(), "character_regenerate_one");
            }

            @Override
            public void onEditCharacter(@androidx.annotation.NonNull Character character, int position) {
                if (isCharacterGenerationInProgress()) {
                    Toast.makeText(CharacterActivity.this, R.string.character_manual_edit_disabled_loading, Toast.LENGTH_SHORT).show();
                    return;
                }
                showEditCharacterDialog(character, position);
            }

            @Override
            public void onDeleteCharacter(@androidx.annotation.NonNull Character character, int position) {
                if (isCharacterGenerationInProgress()) {
                    Toast.makeText(CharacterActivity.this, R.string.character_manual_edit_disabled_loading, Toast.LENGTH_SHORT).show();
                    return;
                }
                showDeleteCharacterDialog(character, position);
            }
        });

        btnRegenerate.setOnClickListener(v -> {
            Story story = resolveSelectedStory();
            if (story == null) {
                Toast.makeText(this, "还没有可分析的小说", Toast.LENGTH_SHORT).show();
                return;
            }

            CharacterRegenerateBottomSheetDialogFragment dialog =
                    CharacterRegenerateBottomSheetDialogFragment.newInstance(story.getTitle());
            dialog.setListener(extraDemand -> promptModelThenRun(() -> loadCharactersForSelectedStory(true, extraDemand)));
            dialog.show(getSupportFragmentManager(), "character_regenerate");
        });
    }

    @Override
    protected void initData() {
        storyDao = new StoryDao(this);
        characterDao = new CharacterDao(this);
        loadCharactersForSelectedStory(false, "");
    }

    private void loadCharactersForSelectedStory(boolean forceRefresh, String extraDemand) {
        Story story = resolveSelectedStory();
        if (story == null) {
            tvCurrentStoryTitle.setText("未找到故事");
            showEmpty("还没有可分析的小说，请先新增或选择一篇故事");
            return;
        }

        tvCurrentStoryTitle.setText(story.getTitle());

        boolean hasExisting = adapter != null && adapter.getItemCount() > 0;
        showLoading();

        if (!forceRefresh) {
            List<Character> cached = characterDao.getCharactersByStoryId(story.getId());
            if (cached != null && !cached.isEmpty()) {
                pbLoading.setVisibility(View.GONE);
                tvStatus.setText(String.format(Locale.CHINA, "已加载《%s》的 %d 位人物画像（本地缓存，按重要度排序）", story.getTitle(), cached.size()));
                adapter.setData(cached);
                return;
            }
        }

        btnRegenerate.setEnabled(false);
        int token = ++generationToken;

        if (forceRefresh) {
            tvStatus.setText(String.format(Locale.CHINA, "正在使用 %s 重新生成《%s》的人物画像...", getModelDisplayName(), story.getTitle()));
        } else {
            tvStatus.setText(String.format(Locale.CHINA, "正在使用 %s 分析《%s》的人物画像...", getModelDisplayName(), story.getTitle()));
        }

        String prompt = buildCharacterPrompt(story, extraDemand);

        ApiClient.getInstance().generateStory(prompt, currentModel, this, new ApiClient.Callback() {
            @Override
            public void onSuccess(String responseText) {
                runOnUiThread(() -> {
                    if (token != generationToken) {
                        return;
                    }
                    btnRegenerate.setEnabled(true);
                    List<Character> characters = parseCharacters(story, responseText);
                    if (characters.isEmpty()) {
                        if (hasExisting) {
                            showMessage("重新生成结果不可用：没有识别到人物。你可以切换到 Pro 模型或补充更明确的要求后重试。", false);
                        } else {
                            showEmpty("没有识别到人物，请尝试切换到 Pro 模型，或换一篇人物描写更明确的故事");
                        }
                        return;
                    }
                    pbLoading.setVisibility(View.GONE);
                    tvStatus.setText(String.format(Locale.CHINA, "已识别到 %d 位人物（按重要度排序）", characters.size()));
                    adapter.setData(characters);
                    characterDao.replaceCharactersForStory(story.getId(), characters);
                });
            }

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> {
                    if (token != generationToken) {
                        return;
                    }
                    btnRegenerate.setEnabled(true);
                    String msg = "人物画像生成失败：" + (e == null ? "未知错误" : e.getMessage());
                    if (hasExisting) {
                        showMessage(msg, false);
                    } else {
                        showEmpty(msg);
                    }
                });
            }
        });
    }

    private void regenerateSingleCharacter(Story story, Character target, int position, String extraDemand) {
        boolean hasExisting = adapter != null && adapter.getItemCount() > 0;
        showLoading();

        btnRegenerate.setEnabled(false);
        int token = ++generationToken;
        tvStatus.setText(String.format(Locale.CHINA, "正在使用 %s 重新生成「%s」的人物画像...", getModelDisplayName(), target.getName()));

        String prompt = buildSingleCharacterPrompt(story, target, extraDemand);
        ApiClient.getInstance().generateStory(prompt, currentModel, this, new ApiClient.Callback() {
            @Override
            public void onSuccess(String responseText) {
                runOnUiThread(() -> {
                    if (token != generationToken) {
                        return;
                    }
                    btnRegenerate.setEnabled(true);

                    Character regenerated = parseSingleCharacter(story, target, responseText);
                    if (regenerated == null) {
                        if (hasExisting) {
                            showMessage("重新生成结果不可用：解析失败。你可以再补充更明确的要求后重试。", false);
                        } else {
                            showEmpty("重新生成失败：解析人物画像失败");
                        }
                        return;
                    }

                    pbLoading.setVisibility(View.GONE);
                    tvStatus.setText(String.format(Locale.CHINA, "已更新「%s」的人物画像", regenerated.getName()));
                    adapter.updateItem(position, regenerated);

                    // 为了简单起见，直接用当前列表快照覆盖缓存。
                    characterDao.replaceCharactersForStory(story.getId(), adapter.getDataSnapshot());
                });
            }

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> {
                    if (token != generationToken) {
                        return;
                    }
                    btnRegenerate.setEnabled(true);
                    String msg = "重新生成失败：" + (e == null ? "未知错误" : e.getMessage());
                    if (hasExisting) {
                        showMessage(msg, false);
                    } else {
                        showEmpty(msg);
                    }
                });
            }
        });
    }

    private boolean isCharacterGenerationInProgress() {
        return btnRegenerate != null && !btnRegenerate.isEnabled();
    }

    private void promptModelThenRun(Runnable action) {
        if (action == null || isCharacterGenerationInProgress()) {
            return;
        }
        java.util.List<ModelConfig.ModelInfo> allModels = ModelConfig.getAllModels();
        java.util.List<ModelConfig.ModelInfo> enabledModels = new java.util.ArrayList<>();
        for (ModelConfig.ModelInfo model : allModels) {
            if (ModelConfig.isProviderEnabled(this, model.provider)) {
                enabledModels.add(model);
            }
        }
        String[] modelLabels = new String[enabledModels.size()];
        for (int i = 0; i < enabledModels.size(); i++) {
            modelLabels[i] = enabledModels.get(i).fullName;
        }
        int checkedItem = 0;
        for (int i = 0; i < enabledModels.size(); i++) {
            if (enabledModels.get(i).modelId.equals(currentModel)) {
                checkedItem = i;
                break;
            }
        }
        final int[] selectedItem = {checkedItem};

        new AlertDialog.Builder(this)
                .setTitle(R.string.character_model_dialog_title)
                .setSingleChoiceItems(modelLabels, checkedItem, (dialog, which) -> selectedItem[0] = which)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(btnRegenerate.getText(), (dialog, which) -> {
                    currentModel = enabledModels.get(selectedItem[0]).modelId;
                    PrefsUtils.getInstance(this).putString(PREF_CHARACTER_MODEL, currentModel);
                    ModelConfig.ModelInfo model = ModelConfig.getModelInfo(currentModel);
                    Toast.makeText(this,
                            getString(R.string.character_model_switched, model != null ? model.displayName : currentModel),
                            Toast.LENGTH_SHORT).show();
                    action.run();
                })
                .show();
    }

    private void showEditCharacterDialog(Character character, int position) {
        Story story = resolveSelectedStory();
        if (story == null || character == null) {
            Toast.makeText(this, R.string.character_manual_edit_no_data, Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_character, null);
        EditText etName = dialogView.findViewById(R.id.et_character_edit_name);
        EditText etSummary = dialogView.findViewById(R.id.et_character_edit_summary);
        EditText etDetail = dialogView.findViewById(R.id.et_character_edit_detail);
        etName.setText(character.getName());
        etSummary.setText(character.getProfile());
        etDetail.setText(character.getDetail());

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.title_edit_character, character.getName()))
                .setView(dialogView)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_save, null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = textOf(etName);
            String summary = textOf(etSummary);
            String detail = textOf(etDetail);

            if (TextUtils.isEmpty(name)) {
                etName.setError(getString(R.string.character_edit_name_required));
                return;
            }
            if (TextUtils.isEmpty(summary)) {
                etSummary.setError(getString(R.string.character_edit_summary_required));
                return;
            }
            if (TextUtils.isEmpty(detail)) {
                detail = summary;
            }

            Character updated = new Character(story.getId(), name, summary, detail, character.getAvatarResId());
            updated.setId(character.getId());
            adapter.updateItem(position, updated);
            characterDao.replaceCharactersForStory(story.getId(), adapter.getDataSnapshot());
            pbLoading.setVisibility(View.GONE);
            tvStatus.setVisibility(View.VISIBLE);
            tvStatus.setText(getString(R.string.character_manual_edit_saved, updated.getName()));
            Toast.makeText(this, R.string.character_manual_edit_saved_toast, Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        }));
        dialog.show();
    }

    private void showDeleteCharacterDialog(Character character, int position) {
        Story story = resolveSelectedStory();
        if (story == null || character == null) {
            Toast.makeText(this, R.string.character_manual_edit_no_data, Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.character_delete_title)
                .setMessage(getString(R.string.character_delete_message, character.getName()))
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_delete, (dialog, which) -> {
                    adapter.removeItem(position);
                    characterDao.replaceCharactersForStory(story.getId(), adapter.getDataSnapshot());
                    pbLoading.setVisibility(View.GONE);
                    if (adapter.getItemCount() == 0) {
                        showEmpty(getString(R.string.character_delete_all_removed));
                    } else {
                        tvStatus.setVisibility(View.VISIBLE);
                        tvStatus.setText(getString(R.string.character_deleted, character.getName()));
                    }
                    Toast.makeText(this, R.string.character_deleted_toast, Toast.LENGTH_SHORT).show();
                })
                .show();
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

    private String buildCharacterPrompt(Story story, String extraDemand) {
        String storyContext = buildCharacterPromptContext(story);
        
        // 使用 PromptManager 加载模板
        java.util.Map<String, Object> variables = new java.util.HashMap<>();
        variables.put("story_context", storyContext);
        if (!TextUtils.isEmpty(extraDemand)) {
            variables.put("extra_demand", extraDemand);
        } else {
            variables.put("extra_demand", "");
        }
        
        return promptManager.getTaskPrompt(
            com.example.storyteller.utils.TaskType.EXTRACT_CHARACTERS.getCode(),
            variables
        );
    }

    private String buildSingleCharacterPrompt(Story story, Character target, String extraDemand) {
        String storyContext = buildCharacterPromptContext(story);
        
        // 使用 PromptManager 加载模板
        java.util.Map<String, Object> variables = new java.util.HashMap<>();
        variables.put("character_name", target.getName());
        variables.put("current_summary", target.getProfile() == null ? "" : target.getProfile());
        variables.put("current_detail", target.getDetail() == null ? "" : target.getDetail());
        variables.put("story_context", storyContext);
        if (!TextUtils.isEmpty(extraDemand)) {
            variables.put("extra_demand", extraDemand);
        } else {
            variables.put("extra_demand", "");
        }
        
        return promptManager.getTaskPrompt(
            com.example.storyteller.utils.TaskType.OPTIMIZE_CHARACTER.getCode(),
            variables
        );
    }


    private String getModelDisplayName() {
        ModelConfig.ModelInfo model = ModelConfig.getModelInfo(currentModel);
        return model != null ? model.displayName : currentModel;
    }

    private List<Character> parseCharacters(Story story, String responseText) {
        List<Character> result = new ArrayList<>();
        String jsonText = extractJson(responseText);
        Map<String, Character> deduplicated = new LinkedHashMap<>();
        try {
            JsonObject root = gson.fromJson(jsonText, JsonObject.class);
            if (root == null || !root.has("characters") || !root.get("characters").isJsonArray()) {
                return result;
            }

            JsonArray array = root.getAsJsonArray("characters");
            for (JsonElement element : array) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject obj = element.getAsJsonObject();
                String rawName = safeGetString(obj, "name", "");
                String name = normalizeCharacterName(rawName);
                if (TextUtils.isEmpty(name)) {
                    name = "未命名人物";
                }
                String summary = "";
                if (obj.has("summary") && !obj.get("summary").isJsonNull()) {
                    summary = obj.get("summary").getAsString();
                } else if (obj.has("profile") && !obj.get("profile").isJsonNull()) {
                    summary = obj.get("profile").getAsString();
                }
                String detail = obj.has("detail") && !obj.get("detail").isJsonNull()
                        ? obj.get("detail").getAsString()
                        : summary;

                if (TextUtils.isEmpty(summary)) {
                    summary = "人物待补充 / 画像待生成";
                }

                String key = name.toLowerCase(Locale.ROOT);
                Character existing = deduplicated.get(key);
                Character candidate = new Character(story.getId(), name, summary, detail, 0);
                if (existing == null || scoreCharacter(candidate) > scoreCharacter(existing)) {
                    deduplicated.put(key, candidate);
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "解析人物画像失败，请重试", Toast.LENGTH_SHORT).show();
        }
        result.addAll(deduplicated.values());
        return result;
    }

    private Character parseSingleCharacter(Story story, Character original, String responseText) {
        String jsonText = extractJson(responseText);
        try {
            JsonObject root = gson.fromJson(jsonText, JsonObject.class);
            if (root == null) {
                return null;
            }

            JsonObject obj = null;
            if (root.has("character") && root.get("character").isJsonObject()) {
                obj = root.getAsJsonObject("character");
            } else if (root.has("name")) {
                obj = root;
            } else if (root.has("characters") && root.get("characters").isJsonArray()) {
                JsonArray array = root.getAsJsonArray("characters");
                if (!array.isEmpty() && array.get(0).isJsonObject()) {
                    obj = array.get(0).getAsJsonObject();
                }
            }

            if (obj == null) {
                return null;
            }

            String name = normalizeCharacterName(safeGetString(obj, "name", original.getName()));
            String summary = safeGetString(obj, "summary", safeGetString(obj, "profile", original.getProfile()));
            String detail = safeGetString(obj, "detail", original.getDetail());
            if (TextUtils.isEmpty(summary)) {
                summary = "人物待补充 / 画像待生成";
            }
            if (TextUtils.isEmpty(detail)) {
                detail = summary;
            }

            if (!TextUtils.equals(name, original.getName())) {
                name = original.getName();
            }

            Character updated = new Character(story.getId(), name, summary, detail, original.getAvatarResId());
            updated.setId(original.getId());
            return updated;
        } catch (Exception e) {
            return null;
        }
    }


    private String buildStoryContextForPrompt(Story story, int maxLength) {
        StringBuilder builder = new StringBuilder();
        builder.append("小说标题：")
                .append(TextUtils.isEmpty(story.getTitle()) ? "未命名小说" : story.getTitle())
                .append("\n");

        if (!TextUtils.isEmpty(story.getGenre())) {
            builder.append("小说类型：")
                    .append(story.getGenre().trim())
                    .append("\n");
        }
        if (!TextUtils.isEmpty(story.getDescription())) {
            builder.append("小说简介：")
                    .append(story.getDescription().trim())
                    .append("\n");
        }

        List<Volume> volumes = parseStoryVolumes(story);
        if (!volumes.isEmpty()) {
            builder.append("小说正文（按卷章整理）：\n");
            for (int i = 0; i < volumes.size(); i++) {
                Volume volume = volumes.get(i);
                String volumeTitle = safeTrim(volume.getTitle(), "未命名卷");
                appendWithLimit(builder, "\n第" + (i + 1) + "卷：" + volumeTitle + "\n", maxLength);
                List<Chapter> chapters = volume.getChapters();
                if (chapters == null || chapters.isEmpty()) {
                    appendWithLimit(builder, "（本卷暂无章节内容）\n", maxLength);
                    continue;
                }
                for (int j = 0; j < chapters.size(); j++) {
                    Chapter chapter = chapters.get(j);
                    String chapterTitle = safeTrim(chapter.getTitle(), "未命名章");
                    String chapterContent = trimContent(chapter.getContent(), MAX_CHAPTER_EXCERPT_LENGTH);
                    appendWithLimit(builder, "第" + (j + 1) + "章：" + chapterTitle + "\n", maxLength);
                    if (!TextUtils.isEmpty(chapterContent)) {
                        appendWithLimit(builder, chapterContent + "\n\n", maxLength);
                    }
                    if (builder.length() >= maxLength) {
                        break;
                    }
                }
                if (builder.length() >= maxLength) {
                    break;
                }
            }
        } else {
            builder.append("小说正文：\n")
                    .append(trimContent(story.getContent(), MAX_PLAIN_CONTENT_LENGTH));
        }

        if (builder.length() > maxLength) {
            return builder.substring(0, maxLength) + "\n（以下内容因长度限制已省略）";
        }
        return builder.toString();
    }

    private String buildCharacterPromptContext(Story story) {
        PlotSummarySnapshot snapshot = readPlotSummarySnapshot(story);
        String cachedContext = buildCharacterContextFromSnapshot(story, snapshot);
        if (!TextUtils.isEmpty(cachedContext)) {
            return cachedContext;
        }
        return buildStoryContextForPrompt(story, MAX_PROMPT_CONTEXT_LENGTH);
    }

    private PlotSummarySnapshot readPlotSummarySnapshot(Story story) {
        if (story == null || TextUtils.isEmpty(story.getPlotSummaryJson())) {
            return null;
        }
        try {
            return gson.fromJson(story.getPlotSummaryJson(), PlotSummarySnapshot.class);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String buildCharacterContextFromSnapshot(Story story, PlotSummarySnapshot snapshot) {
        if (snapshot == null) {
            return "";
        }
        if (!TextUtils.isEmpty(snapshot.getCharacterContext())) {
            return snapshot.getCharacterContext();
        }
        List<PlotChapterSummary> chapterSummaries = snapshot.getChapterSummaries();
        if (chapterSummaries == null || chapterSummaries.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        appendWithLimit(builder, "小说标题：" + safeTrim(story.getTitle(), "未命名小说") + "\n", MAX_PLOT_CACHE_CONTEXT_LENGTH);
        if (!TextUtils.isEmpty(story.getDescription())) {
            appendWithLimit(builder, "小说简介：" + story.getDescription().trim() + "\n", MAX_PLOT_CACHE_CONTEXT_LENGTH);
        }

        PlotOverviewSummary overview = snapshot.getOverview();
        if (overview != null) {
            if (!TextUtils.isEmpty(overview.getOverallSummary())) {
                appendWithLimit(builder, "全书梳理：" + overview.getOverallSummary() + "\n", MAX_PLOT_CACHE_CONTEXT_LENGTH);
            }
            if (overview.getMainLine() != null && !overview.getMainLine().isEmpty()) {
                appendWithLimit(builder, "主线：" + TextUtils.join("；", overview.getMainLine()) + "\n", MAX_PLOT_CACHE_CONTEXT_LENGTH);
            }
        }

        appendWithLimit(builder, "章节梳理：\n", MAX_PLOT_CACHE_CONTEXT_LENGTH);
        for (PlotChapterSummary chapter : chapterSummaries) {
            appendWithLimit(builder,
                    chapter.getChapterLabel() + " " + safeTrim(chapter.getChapterTitle(), "未命名章") + "\n",
                    MAX_PLOT_CACHE_CONTEXT_LENGTH);
            if (!TextUtils.isEmpty(chapter.getBriefSummary())) {
                appendWithLimit(builder, "概述：" + chapter.getBriefSummary() + "\n", MAX_PLOT_CACHE_CONTEXT_LENGTH);
            }
            if (chapter.getKeyEvents() != null && !chapter.getKeyEvents().isEmpty()) {
                appendWithLimit(builder, "事件：" + TextUtils.join("；", chapter.getKeyEvents()) + "\n", MAX_PLOT_CACHE_CONTEXT_LENGTH);
            }
            if (chapter.getCharacters() != null && !chapter.getCharacters().isEmpty()) {
                appendWithLimit(builder, "人物：" + TextUtils.join("、", chapter.getCharacters()) + "\n", MAX_PLOT_CACHE_CONTEXT_LENGTH);
            }
            appendWithLimit(builder, "\n", MAX_PLOT_CACHE_CONTEXT_LENGTH);
            if (builder.length() >= MAX_PLOT_CACHE_CONTEXT_LENGTH) {
                break;
            }
        }
        return builder.toString().trim();
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


    private String normalizeCharacterName(String rawName) {
        if (TextUtils.isEmpty(rawName)) {
            return "";
        }
        String normalized = rawName.trim()
                .replace("“", "")
                .replace("”", "")
                .replace("'", "")
                .replace("\"", "")
                .replace("《", "")
                .replace("》", "")
                .replace("：", "")
                .replace(":", "");
        return normalized.trim();
    }

    private int scoreCharacter(Character character) {
        int score = 0;
        if (character == null) {
            return score;
        }
        if (!TextUtils.isEmpty(character.getProfile())) {
            score += character.getProfile().length();
        }
        if (!TextUtils.isEmpty(character.getDetail())) {
            score += character.getDetail().length() * 2;
        }
        return score;
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

    private String safeTrim(String text, String fallback) {
        if (TextUtils.isEmpty(text)) {
            return fallback;
        }
        String trimmed = text.trim();
        return TextUtils.isEmpty(trimmed) ? fallback : trimmed;
    }

    private String safeGetString(JsonObject obj, String key, String fallback) {
        if (obj == null || !obj.has(key)) {
            return fallback;
        }
        JsonElement element = obj.get(key);
        if (element == null || element.isJsonNull()) {
            return fallback;
        }
        if (element.isJsonPrimitive()) {
            JsonPrimitive primitive = element.getAsJsonPrimitive();
            if (primitive.isString()) {
                return primitive.getAsString();
            }
        }
        return fallback;
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

    private String textOf(EditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }

    private void showLoading() {
        pbLoading.setVisibility(View.VISIBLE);
        tvStatus.setVisibility(View.VISIBLE);
        tvStatus.setText("正在分析小说人物画像...");
    }

    private void showMessage(String message, boolean clearList) {
        pbLoading.setVisibility(View.GONE);
        tvStatus.setVisibility(View.VISIBLE);
        tvStatus.setText(message);
        if (clearList) {
            adapter.setData(new ArrayList<>());
        }
    }

    private void showEmpty(String message) {
        showMessage(message, true);
    }
}

