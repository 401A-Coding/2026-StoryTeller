package com.example.storyteller.ui.activity;

import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;
import com.example.storyteller.R;
import com.example.storyteller.base.BaseActivity;
import com.example.storyteller.data.local.db.StoryDao;
import com.example.storyteller.data.local.prefs.PrefsUtils;
import com.example.storyteller.model.Chapter;
import com.example.storyteller.model.Story;
import com.example.storyteller.model.Volume;
import com.example.storyteller.ui.adapter.StoryAdapter;
import com.example.storyteller.utils.JsonUtils;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.List;

public class StoryDetailActivity extends BaseActivity {

    public static final String EXTRA_STORY_ID = StoryAdapter.EXTRA_STORY_ID;
    public static final String EXTRA_STORY_TITLE = StoryAdapter.EXTRA_STORY_TITLE;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_story_detail;
    }

    @Override
    protected void initView() {
        // 刘海屏适配：为根布局设置系统栏内边距
        applySystemWindowInsets(findViewById(android.R.id.content));
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        TextView tvTitle = findViewById(R.id.tv_story_preview_title);
        TextView tvSeries = findViewById(R.id.tv_story_series);
        TextView tvDescription = findViewById(R.id.tv_story_description);
        TextView tvWordCount = findViewById(R.id.tv_story_word_count);
        TextView tvCatalog = findViewById(R.id.tv_story_catalog);
        Intent intent = getIntent();
        int storyId = intent.getIntExtra(EXTRA_STORY_ID, -1);
        String storyTitle = intent.getStringExtra(EXTRA_STORY_TITLE);
        StoryDao storyDao = new StoryDao(this);

        Story story = null;
        if (storyId > 0) {
            story = storyDao.getStoryById(storyId);
        }
        if (story == null) {
            story = storyDao.getLatestStory();
        }

        if (story != null) {
            tvTitle.setText(TextUtils.isEmpty(storyTitle) ? story.getTitle() : storyTitle);
            tvSeries.setText(getString(
                R.string.story_series_format,
                TextUtils.isEmpty(story.getGenre()) ? getString(R.string.story_series_default) : story.getGenre()
            ));
            tvDescription.setText(getString(
                R.string.story_description_format,
                TextUtils.isEmpty(story.getDescription()) ? getString(R.string.story_description_default) : story.getDescription()
            ));
            tvWordCount.setText(getString(R.string.story_detail_word_count_format, calculateWordCount(story.getContent())));
            tvCatalog.setText(buildCatalogText(story.getStructure()));
            storyId = story.getId();
        } else {
            tvTitle.setText(TextUtils.isEmpty(storyTitle) ? "未找到故事" : storyTitle);
            tvSeries.setText(getString(R.string.story_series_format, getString(R.string.story_series_default)));
            tvDescription.setText(getString(R.string.story_description_format, getString(R.string.story_description_default)));
            tvWordCount.setText(getString(R.string.story_detail_word_count_format, 0));
            tvCatalog.setText(getString(R.string.story_detail_catalog_empty));
        }

        final int selectedStoryId = storyId;
        if (selectedStoryId > 0) {
            PrefsUtils.getInstance(this).putString(StoryAdapter.PREF_SELECTED_STORY_ID, String.valueOf(selectedStoryId));
            PrefsUtils.getInstance(this).putString(StoryAdapter.PREF_SELECTED_STORY_TITLE, tvTitle.getText().toString());
        }

        findViewById(R.id.btn_back_home).setOnClickListener(v -> {
            Intent homeIntent = new Intent(this, MainActivity.class);
            homeIntent.putExtra(MainActivity.EXTRA_TARGET_TAB, MainActivity.TAB_HOME);
            homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(homeIntent);
        });

        findViewById(R.id.btn_character_analysis).setOnClickListener(v -> {
            Intent characterIntent = new Intent(this, CharacterActivity.class);
            if (selectedStoryId > 0) {
                characterIntent.putExtra(CharacterActivity.EXTRA_STORY_ID, selectedStoryId);
            }
            startActivity(characterIntent);
        });

        findViewById(R.id.btn_plot_tree).setOnClickListener(v -> {
            Intent plotIntent = new Intent(this, PlotTreeActivity.class);
            startActivity(plotIntent);
        });

        findViewById(R.id.btn_edit_story).setOnClickListener(v -> {
            startEditStory(selectedStoryId);
        });

        // 点击故事章节/目录也可进入编辑
        findViewById(R.id.tv_story_catalog).setOnClickListener(v -> {
            startEditStory(selectedStoryId);
        });
    }

    private void startEditStory(int storyId) {
        Intent editIntent = new Intent(this, StoryGenerateActivity.class);
        if (storyId > 0) {
            editIntent.putExtra("story_id", storyId);
        }
        startActivity(editIntent);
    }

    @Override
    protected void initData() {
        // 占位
    }

    private int calculateWordCount(String content) {
        if (TextUtils.isEmpty(content)) {
            return 0;
        }
        return content.replaceAll("\\s+", "").length();
    }

    private String buildCatalogText(String structureJson) {
        if (TextUtils.isEmpty(structureJson)) {
            return getString(R.string.story_detail_catalog_empty);
        }

        try {
            Type type = new TypeToken<List<Volume>>() {}.getType();
            List<Volume> volumes = JsonUtils.fromJson(structureJson, type);
            if (volumes == null || volumes.isEmpty()) {
                return getString(R.string.story_detail_catalog_empty);
            }

            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < volumes.size(); i++) {
                Volume volume = volumes.get(i);
                String volumeTitle = TextUtils.isEmpty(volume.getTitle()) ? "未命名卷" : volume.getTitle().trim();
                if (builder.length() > 0) {
                    builder.append("\n");
                }
                builder.append("第").append(i + 1).append("卷 · ").append(volumeTitle);

                List<Chapter> chapters = volume.getChapters();
                if (chapters == null || chapters.isEmpty()) {
                    builder.append("\n  暂无章节");
                    continue;
                }

                for (int j = 0; j < chapters.size(); j++) {
                    Chapter chapter = chapters.get(j);
                    String chapterTitle = TextUtils.isEmpty(chapter.getTitle()) ? "未命名章" : chapter.getTitle().trim();
                    builder.append("\n  ").append(j + 1).append(". ").append(chapterTitle);
                }
            }
            return builder.toString();
        } catch (Exception e) {
            Log.w("StoryDetailActivity", "Failed to parse story catalog", e);
            return getString(R.string.story_detail_catalog_empty);
        }
    }
}


