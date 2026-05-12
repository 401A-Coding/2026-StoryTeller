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
import com.example.storyteller.model.Chapter;
import com.example.storyteller.model.Character;
import com.example.storyteller.model.Story;
import com.example.storyteller.model.Volume;
import com.example.storyteller.ui.adapter.CharacterAdapter;
import com.example.storyteller.ui.adapter.StoryAdapter;
import com.example.storyteller.ui.dialog.CharacterRegenerateBottomSheetDialogFragment;
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
    private static final Type VOLUME_LIST_TYPE = new TypeToken<List<Volume>>() {}.getType();

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

        adapter.setListener(new CharacterAdapter.Listener() {
            @Override
            public void onRegenerateCharacter(@androidx.annotation.NonNull Character character, int position) {
                Story story = resolveSelectedStory();
                if (story == null) {
                    Toast.makeText(CharacterActivity.this, "还没有可分析的小说", Toast.LENGTH_SHORT).show();
                    return;
                }

                CharacterRegenerateBottomSheetDialogFragment dialog =
                        CharacterRegenerateBottomSheetDialogFragment.newInstance(story.getTitle(), character.getName());
                dialog.setListener(extraDemand -> regenerateSingleCharacter(story, character, position, extraDemand));
                dialog.show(getSupportFragmentManager(), "character_regenerate_one");
            }
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
        String storyContext = buildStoryContextForPrompt(story, MAX_PROMPT_CONTEXT_LENGTH);

        StringBuilder builder = new StringBuilder();
        builder.append("你是一名严格、谨慎的小说人物分析师。请根据下面给出的小说内容，识别真正已经在正文中出场、并对剧情有明显作用的人物。\n")
                .append("要求：\n")
                .append("1. 只输出严格 JSON，不要 Markdown，不要解释。\n")
                .append("2. 不要为了凑数量而硬编人物；如果只能确定 1 到 3 个人物，就输出 1 到 3 个。\n")
                .append("3. 严禁杜撰原文未明确出现的人名、身份、关系、经历；不确定就不要写。\n")
                .append("4. 如果人物没有明确姓名，可以直接使用正文中自然出现的称呼，例如“主角”“客栈掌柜”“班主任”；不要硬编具体姓名。\n")
                .append("5. 优先选择有反复出场、对白、行动、心理描写、或明显推动情节的人物。\n")
                .append("6. summary 必须是短标签风格，2到4个短词，用“ / ”分隔，不要长句，不要空话。\n")
                .append("7. detail 只写文本中能支持的内容，建议包含：身份/定位、性格特点、关键动机、主要关系、当前成长状态；文本没有明确给出的地方请写“文中暂未明确”。\n")
                .append("8. 每个人物补充 evidence 字段，写 1 到 2 条证据，简述他出现在哪一卷/章、做了什么；证据必须来自给定文本。\n")
                .append("9. JSON 格式如下：\n")
                .append("{\"characters\":[{\"name\":\"人物名\",\"summary\":\"冷静 / 谨慎 / 复仇者\",\"detail\":\"详细介绍\",\"evidence\":[\"第1卷第2章：……\",\"第1卷第4章：……\"]}]}\n")
                .append("\n在输出前请先自行检查：人物是否真的在正文出现、名字是否明确、描述是否有原文依据；只保留把握最高的人物。\n\n")
                .append(storyContext);

        if (!TextUtils.isEmpty(extraDemand)) {
            builder.append("\n\n用户补充的生成需求（请尽量满足）：\n")
                    .append(extraDemand)
                    .append("\n\n请根据用户补充需求优化人物画像，但依然必须遵守“不能杜撰、必须以正文为准”的原则，只输出 JSON。\n");
        }
        return builder.toString();
    }

    private String buildSingleCharacterPrompt(Story story, Character target, String extraDemand) {
        String storyContext = buildStoryContextForPrompt(story, MAX_PROMPT_CONTEXT_LENGTH);

        StringBuilder builder = new StringBuilder();
        builder.append("你是一名严格、谨慎的小说人物分析师。请根据下面这篇小说，只针对指定人物生成或优化人物画像。\n")
                .append("要求：\n")
                .append("1. 只输出严格 JSON，不要 Markdown，不要解释。\n")
                .append("2. 只输出一个人物，且人物名必须与指定人物名完全一致。\n")
                .append("3. 严禁杜撰该人物在正文中没有出现过的经历、关系和设定；不确定就写“文中暂未明确”。\n")
                .append("4. 如果指定人物没有明确姓名，可以保留当前自然称呼；如果正文信息很少，也不要强行补全，只保留能确认的事实。\n")
                .append("5. summary 必须是短标签风格，2到4个短词，用“ / ”分隔，不要长句。\n")
                .append("6. detail 只写有文本依据的介绍，优先包含身份/定位、性格特点、关键动机、主要关系、当前成长状态。\n")
                .append("7. JSON 格式如下：\n")
                .append("{\"character\":{\"name\":\"人物名\",\"summary\":\"冷静 / 谨慎 / 复仇者\",\"detail\":\"详细介绍\",\"evidence\":[\"第1卷第2章：……\"]}}\n")
                .append("\n指定人物名：")
                .append(target.getName())
                .append("\n")
                .append("当前已有人物画像（供你优化，可参考但不要照抄）：\n")
                .append("summary：")
                .append(target.getProfile() == null ? "" : target.getProfile())
                .append("\n")
                .append("detail：")
                .append(target.getDetail() == null ? "" : target.getDetail())
                .append("\n\n在输出前请先自行检查：是否确实是这个人物、是否存在原文证据、是否有未经文本支持的臆测。\n\n")
                .append(storyContext);

        if (!TextUtils.isEmpty(extraDemand)) {
            builder.append("\n\n用户补充的生成需求（请尽量满足）：\n")
                    .append(extraDemand)
                    .append("\n\n请根据用户补充需求优化该人物画像，但依然必须遵守“不能杜撰、必须以正文为准”的原则，只输出 JSON。\n");
        }
        return builder.toString();
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

