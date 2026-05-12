package com.example.storyteller.ui.activity;

import android.content.Intent;
import android.text.TextUtils;
import android.widget.TextView;
import com.example.storyteller.R;
import com.example.storyteller.base.BaseActivity;
import com.example.storyteller.data.local.db.StoryDao;
import com.example.storyteller.data.local.prefs.PrefsUtils;
import com.example.storyteller.model.Story;
import com.example.storyteller.ui.adapter.StoryAdapter;

public class StoryDetailActivity extends BaseActivity {

    public static final String EXTRA_STORY_ID = StoryAdapter.EXTRA_STORY_ID;
    public static final String EXTRA_STORY_TITLE = StoryAdapter.EXTRA_STORY_TITLE;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_story_detail;
    }

    @Override
    protected void initView() {
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        TextView tvTitle = findViewById(R.id.tv_story_preview_title);
        TextView tvSeries = findViewById(R.id.tv_story_series);
        TextView tvDescription = findViewById(R.id.tv_story_description);
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
            storyId = story.getId();
        } else {
            tvTitle.setText(TextUtils.isEmpty(storyTitle) ? "未找到故事" : storyTitle);
            tvSeries.setText(getString(R.string.story_series_format, getString(R.string.story_series_default)));
            tvDescription.setText(getString(R.string.story_description_format, getString(R.string.story_description_default)));
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
            Intent editIntent = new Intent(this, StoryGenerateActivity.class);
            if (selectedStoryId > 0) {
                editIntent.putExtra("story_id", selectedStoryId);
            }
            startActivity(editIntent);
        });
    }

    @Override
    protected void initData() {
        // 占位
    }
}


