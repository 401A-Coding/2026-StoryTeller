package com.example.storyteller.ui.activity;

import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
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
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CharacterActivity extends BaseActivity {

    public static final String EXTRA_STORY_ID = StoryAdapter.EXTRA_STORY_ID;

    private ProgressBar pbLoading;
    private TextView tvStatus;
    private CharacterAdapter adapter;
    private StoryDao storyDao;
    private CharacterDao characterDao;
    private final Gson gson = new Gson();

    @Override
    protected int getLayoutId() {
        return R.layout.activity_character;
    }

    @Override
    protected void initView() {
        RecyclerView rvCharacterList = findViewById(R.id.rv_character_list);
        pbLoading = findViewById(R.id.pb_character_loading);
        tvStatus = findViewById(R.id.tv_character_status);

        rvCharacterList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CharacterAdapter(this, new ArrayList<>());
        rvCharacterList.setAdapter(adapter);

        findViewById(R.id.btn_back_home).setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra(MainActivity.EXTRA_TARGET_TAB, MainActivity.TAB_HOME);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });
    }

    @Override
    protected void initData() {
        storyDao = new StoryDao(this);
        characterDao = new CharacterDao(this);
        loadCharactersForSelectedStory();
    }

    private void loadCharactersForSelectedStory() {
        showLoading();

        Story story = resolveSelectedStory();
        if (story == null) {
            showEmpty("还没有可分析的小说，请先新增或选择一篇故事");
            return;
        }

        List<Character> cached = characterDao.getCharactersByStoryId(story.getId());
        if (cached != null && !cached.isEmpty()) {
            pbLoading.setVisibility(View.GONE);
            tvStatus.setText(String.format(Locale.CHINA, "已加载《%s》的 %d 位人物画像（本地缓存）", story.getTitle(), cached.size()));
            adapter.setData(cached);
            return;
        }

        tvStatus.setText(String.format(Locale.CHINA, "正在分析：《%s》", story.getTitle()));
        String prompt = buildCharacterPrompt(story);

        ApiClient.getInstance().generateStory(prompt, this, new ApiClient.Callback() {
            @Override
            public void onSuccess(String responseText) {
                runOnUiThread(() -> {
                    List<Character> characters = parseCharacters(story, responseText);
                    if (characters.isEmpty()) {
                        showEmpty("没有识别到主要人物，请换一篇更长或人物更明确的故事");
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
                runOnUiThread(() -> showEmpty("人物画像生成失败：" + e.getMessage()));
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

    private String buildCharacterPrompt(Story story) {
        String content = story.getContent() == null ? "" : story.getContent().trim();
        int maxLength = 5000;
        if (content.length() > maxLength) {
            content = content.substring(0, maxLength);
        }

        return "你是小说人物分析助手。请根据下面这篇小说，找出 3 到 5 个主要人物，并为每个人物总结基础形象。\n" +
                "要求：\n" +
                "1. 只输出严格 JSON，不要 Markdown，不要解释。\n" +
                "2. JSON 格式如下：\n" +
                "{\"characters\":[{\"name\":\"人物名\",\"summary\":\"主人公 / 淳朴带腹黑 / 穿越者\",\"detail\":\"详细介绍\"}]}\n" +
                "3. summary 必须是短标签风格，2到4个短词，用“ / ”分隔，不要长句。\n" +
                "4. detail 要写成完整介绍，包含身份、性格、动机、关系、成长弧线。\n" +
                "5. 若把握不足，summary 仍给出最确定的身份与特征词。\n" +
                "\n小说标题：" + story.getTitle() + "\n" +
                "小说内容：\n" + content;
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

    private void showEmpty(String message) {
        pbLoading.setVisibility(View.GONE);
        tvStatus.setVisibility(View.VISIBLE);
        tvStatus.setText(message);
        adapter.setData(new ArrayList<>());
    }
}
