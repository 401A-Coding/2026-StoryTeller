package com.example.storyteller.ui.activity;

import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.storyteller.R;
import com.example.storyteller.base.BaseActivity;
import com.example.storyteller.data.local.db.CharacterDao;
import com.example.storyteller.data.local.db.StoryDao;
import com.example.storyteller.data.local.prefs.PrefsUtils;
import com.example.storyteller.data.remote.ApiClient;
import com.example.storyteller.model.Character;
import com.example.storyteller.model.Story;
import com.example.storyteller.ui.adapter.CharacterAdapter;
import com.example.storyteller.ui.adapter.StoryAdapter;
import com.example.storyteller.ui.dialog.CharacterRegenerateBottomSheetDialogFragment;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CharacterActivity extends BaseActivity {

    public static final String EXTRA_STORY_ID = StoryAdapter.EXTRA_STORY_ID;

    private TextView tvCurrentStoryTitle;
    private ProgressBar pbLoading;
    private TextView tvStatus;
    private Button btnRegenerate;
    private CharacterAdapter adapter;
    private StoryDao storyDao;
    private CharacterDao characterDao;
    private final Gson gson = new Gson();

    private int generationToken = 0;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_character;
    }

    @Override
    protected void initView() {
        // 刘海屏适配：为根布局设置系统栏内边距
        applySystemWindowInsets(findViewById(android.R.id.content));
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        RecyclerView rvCharacterList = findViewById(R.id.rv_character_list);
        tvCurrentStoryTitle = findViewById(R.id.tv_current_story_title);
        pbLoading = findViewById(R.id.pb_character_loading);
        tvStatus = findViewById(R.id.tv_character_status);
        btnRegenerate = findViewById(R.id.btn_regenerate_character);

        rvCharacterList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CharacterAdapter(this, new ArrayList<>());
        rvCharacterList.setAdapter(adapter);

        adapter.setListener((character, position) -> {
            Story story = resolveSelectedStory();
            if (story == null) {
                Toast.makeText(this, "还没有可分析的小说", Toast.LENGTH_SHORT).show();
                return;
            }

            CharacterRegenerateBottomSheetDialogFragment dialog =
                    CharacterRegenerateBottomSheetDialogFragment.newInstance(story.getTitle(), character.getName());
            dialog.setListener(extraDemand -> regenerateSingleCharacter(story, character, position, extraDemand));
            dialog.show(getSupportFragmentManager(), "character_regenerate_one");
        });

        findViewById(R.id.btn_back_home).setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra(MainActivity.EXTRA_TARGET_TAB, MainActivity.TAB_HOME);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });

        btnRegenerate.setOnClickListener(v -> {
            Story story = resolveSelectedStory();
            if (story == null) {
                Toast.makeText(this, "还没有可分析的小说", Toast.LENGTH_SHORT).show();
                return;
            }

            CharacterRegenerateBottomSheetDialogFragment dialog =
                    CharacterRegenerateBottomSheetDialogFragment.newInstance(story.getTitle());
            dialog.setListener(extraDemand -> loadCharactersForSelectedStory(true, extraDemand));
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
                tvStatus.setText(String.format(Locale.CHINA, "已加载《%s》的 %d 位人物画像（本地缓存）", story.getTitle(), cached.size()));
                adapter.setData(cached);
                return;
            }
        }

        btnRegenerate.setEnabled(false);
        int token = ++generationToken;

        if (forceRefresh) {
            tvStatus.setText(String.format(Locale.CHINA, "正在重新生成《%s》的人物画像...", story.getTitle()));
        } else {
            tvStatus.setText(String.format(Locale.CHINA, "正在分析：《%s》", story.getTitle()));
        }

        String prompt = buildCharacterPrompt(story, extraDemand);

        ApiClient.getInstance().generateStory(prompt, this, new ApiClient.Callback() {
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
                            showMessage("重新生成结果不可用：没有识别到主要人物。你可以再补充更明确的要求后重试。", false);
                        } else {
                            showEmpty("没有识别到主要人物，请换一篇更长或人物更明确的故事");
                        }
                        return;
                    }
                    pbLoading.setVisibility(View.GONE);
                    tvStatus.setText(String.format(Locale.CHINA, "已识别到 %d 位主要人物", characters.size()));
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
        tvStatus.setText(String.format(Locale.CHINA, "正在重新生成「%s」的人物画像...", target.getName()));

        String prompt = buildSingleCharacterPrompt(story, target, extraDemand);
        ApiClient.getInstance().generateStory(prompt, this, new ApiClient.Callback() {
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
        String content = story.getContent() == null ? "" : story.getContent().trim();
        int maxLength = 5000;
        if (content.length() > maxLength) {
            content = content.substring(0, maxLength);
        }

        StringBuilder builder = new StringBuilder();
        builder.append("你是小说人物分析助手。请根据下面这篇小说，找出 3 到 5 个主要人物，并为每个人物总结基础形象。\n")
                .append("要求：\n")
                .append("1. 只输出严格 JSON，不要 Markdown，不要解释。\n")
                .append("2. JSON 格式如下：\n")
                .append("{\"characters\":[{\"name\":\"人物名\",\"summary\":\"主人公 / 淳朴带腹黑 / 穿越者\",\"detail\":\"详细介绍\"}]}\n")
                .append("3. summary 必须是短标签风格，2到4个短词，用“ / ”分隔，不要长句。\n")
                .append("4. detail 要写成完整介绍，包含身份、性格、动机、关系、成长弧线。\n")
                .append("5. 若把握不足，summary 仍给出最确定的身份与特征词。\n")
                .append("\n小说标题：")
                .append(story.getTitle())
                .append("\n")
                .append("小说内容：\n")
                .append(content);

        if (!TextUtils.isEmpty(extraDemand)) {
            builder.append("\n\n用户补充的生成需求（请尽量满足）：\n")
                    .append(extraDemand)
                    .append("\n\n请根据用户补充需求优化人物画像，但依然只输出 JSON。\n");
        }
        return builder.toString();
    }

    private String buildSingleCharacterPrompt(Story story, Character target, String extraDemand) {
        String content = story.getContent() == null ? "" : story.getContent().trim();
        int maxLength = 5000;
        if (content.length() > maxLength) {
            content = content.substring(0, maxLength);
        }

        StringBuilder builder = new StringBuilder();
        builder.append("你是小说人物分析助手。请根据下面这篇小说，只针对指定人物生成/优化人物画像。\n")
                .append("要求：\n")
                .append("1. 只输出严格 JSON，不要 Markdown，不要解释。\n")
                .append("2. 只输出一个人物，且人物名必须与指定人物名完全一致。\n")
                .append("3. JSON 格式如下：\n")
                .append("{\"character\":{\"name\":\"人物名\",\"summary\":\"主人公 / 淳朴带腹黑 / 穿越者\",\"detail\":\"详细介绍\"}}\n")
                .append("4. summary 必须是短标签风格，2到4个短词，用“ / ”分隔，不要长句。\n")
                .append("5. detail 要写成完整介绍，包含身份、性格、动机、关系、成长弧线。\n")
                .append("\n指定人物名：")
                .append(target.getName())
                .append("\n")
                .append("当前已有人物画像（供你优化，可参考但不要照抄）：\n")
                .append("summary：")
                .append(target.getProfile() == null ? "" : target.getProfile())
                .append("\n")
                .append("detail：")
                .append(target.getDetail() == null ? "" : target.getDetail())
                .append("\n")
                .append("\n小说标题：")
                .append(story.getTitle())
                .append("\n")
                .append("小说内容：\n")
                .append(content);

        if (!TextUtils.isEmpty(extraDemand)) {
            builder.append("\n\n用户补充的生成需求（请尽量满足）：\n")
                    .append(extraDemand)
                    .append("\n\n请根据用户补充需求优化该人物画像，但依然只输出 JSON。\n");
        }
        return builder.toString();
    }

    private List<Character> parseCharacters(Story story, String responseText) {
        List<Character> result = new ArrayList<>();
        String jsonText = extractJson(responseText);
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
                String name = obj.has("name") && !obj.get("name").isJsonNull() ? obj.get("name").getAsString() : "未命名人物";
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
                result.add(new Character(story.getId(), name, summary, detail, 0));
            }
        } catch (Exception e) {
            Toast.makeText(this, "解析人物画像失败，请重试", Toast.LENGTH_SHORT).show();
        }
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

            String name = safeGetString(obj, "name", original.getName());
            String summary = safeGetString(obj, "summary", safeGetString(obj, "profile", original.getProfile()));
            String detail = safeGetString(obj, "detail", original.getDetail());
            if (TextUtils.isEmpty(summary)) {
                summary = "人物待补充 / 画像待生成";
            }
            if (TextUtils.isEmpty(detail)) {
                detail = summary;
            }

            Character updated = new Character(story.getId(), name, summary, detail, original.getAvatarResId());
            updated.setId(original.getId());
            return updated;
        } catch (Exception e) {
            return null;
        }
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

