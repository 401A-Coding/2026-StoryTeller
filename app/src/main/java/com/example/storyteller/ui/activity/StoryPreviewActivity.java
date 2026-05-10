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

public class StoryPreviewActivity extends BaseActivity {

    public static final String EXTRA_STORY_ID = StoryAdapter.EXTRA_STORY_ID;
    public static final String EXTRA_STORY_TITLE = StoryAdapter.EXTRA_STORY_TITLE;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_story_preview;
    }

    @Override
    protected void initView() {
        TextView tvTitle = findViewById(R.id.tv_story_preview_title);
        TextView tvContent = findViewById(R.id.tv_story_preview_content);
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
            tvContent.setText(story.getContent());
            storyId = story.getId();
        } else {
            tvTitle.setText(TextUtils.isEmpty(storyTitle) ? "未找到故事" : storyTitle);
            tvContent.setText("当前没有可显示的小说正文，请先生成或新增一篇故事。");
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
    }

    @Override
    protected void initData() {
        // 占位
    }
}

