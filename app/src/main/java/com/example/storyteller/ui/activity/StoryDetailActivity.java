package com.example.storyteller.ui.activity;

import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
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
import com.google.android.material.card.MaterialCardView;
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
        LinearLayout catalogContainer = findViewById(R.id.layout_catalog_container);
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
            buildCatalogViews(catalogContainer, story.getStructure(), story.getId());
            storyId = story.getId();
        } else {
            tvTitle.setText(TextUtils.isEmpty(storyTitle) ? "未找到故事" : storyTitle);
            tvSeries.setText(getString(R.string.story_series_format, getString(R.string.story_series_default)));
            tvDescription.setText(getString(R.string.story_description_format, getString(R.string.story_description_default)));
            tvWordCount.setText(getString(R.string.story_detail_word_count_format, 0));
            TextView emptyCatalog = new TextView(this);
            emptyCatalog.setText(getString(R.string.story_detail_catalog_empty));
            emptyCatalog.setTextColor(0xFF333333);
            emptyCatalog.setTextSize(14);
            catalogContainer.addView(emptyCatalog);
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

    /**
     * 动态生成每卷的卡片视图，每卷和每章都可点击进入编辑
     */
    private void buildCatalogViews(LinearLayout container, String structureJson, int storyId) {
        if (TextUtils.isEmpty(structureJson)) {
            TextView emptyCatalog = new TextView(this);
            emptyCatalog.setText(getString(R.string.story_detail_catalog_empty));
            emptyCatalog.setTextColor(0xFF333333);
            emptyCatalog.setTextSize(14);
            container.addView(emptyCatalog);
            return;
        }

        try {
            Type type = new TypeToken<List<Volume>>() {}.getType();
            List<Volume> volumes = JsonUtils.fromJson(structureJson, type);
            if (volumes == null || volumes.isEmpty()) {
                TextView emptyCatalog = new TextView(this);
                emptyCatalog.setText(getString(R.string.story_detail_catalog_empty));
                emptyCatalog.setTextColor(0xFF333333);
                emptyCatalog.setTextSize(14);
                container.addView(emptyCatalog);
                return;
            }

            for (int i = 0; i < volumes.size(); i++) {
                final int volumeIndex = i;
                Volume volume = volumes.get(i);
                String volumeTitle = TextUtils.isEmpty(volume.getTitle()) ? "未命名卷" : volume.getTitle().trim();

                // 创建卷卡片
                MaterialCardView volumeCard = new MaterialCardView(this);
                LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                );
                cardParams.topMargin = 12;
                volumeCard.setLayoutParams(cardParams);
                volumeCard.setRadius(48);
                volumeCard.setCardElevation(2);
                volumeCard.setStrokeWidth(1);
                volumeCard.setStrokeColor(0xFFE6E6E6);
                volumeCard.setClickable(true);
                volumeCard.setFocusable(true);
                volumeCard.setForeground(getDrawable(android.R.attr.selectableItemBackground));

                // 卷卡片内部布局
                LinearLayout volumeContent = new LinearLayout(this);
                volumeContent.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ));
                volumeContent.setOrientation(LinearLayout.VERTICAL);
                volumeContent.setPadding(48, 48, 48, 48);

                // 卷标题
                TextView tvVolumeTitle = new TextView(this);
                tvVolumeTitle.setText("第" + (volumeIndex + 1) + "卷 · " + volumeTitle);
                tvVolumeTitle.setTextColor(0xFF2A2A2A);
                tvVolumeTitle.setTextSize(15);
                tvVolumeTitle.setTypeface(tvVolumeTitle.getTypeface(), android.graphics.Typeface.BOLD);
                volumeContent.addView(tvVolumeTitle);

                // 章节列表
                List<Chapter> chapters = volume.getChapters();
                if (chapters != null && !chapters.isEmpty()) {
                    for (int j = 0; j < chapters.size(); j++) {
                        Chapter chapter = chapters.get(j);
                        String chapterTitle = TextUtils.isEmpty(chapter.getTitle()) ? "未命名章" : chapter.getTitle().trim();

                        // 章节行
                        LinearLayout chapterRow = new LinearLayout(this);
                        chapterRow.setLayoutParams(new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ));
                        chapterRow.setOrientation(LinearLayout.HORIZONTAL);
                        chapterRow.setPadding(0, 24, 0, 24);
                        chapterRow.setClickable(true);
                        chapterRow.setFocusable(true);
                        chapterRow.setForeground(getDrawable(android.R.attr.selectableItemBackground));

                        TextView tvChapter = new TextView(this);
                        tvChapter.setLayoutParams(new LinearLayout.LayoutParams(
                            0,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            1
                        ));
                        tvChapter.setText((j + 1) + ". " + chapterTitle);
                        tvChapter.setTextColor(0xFF333333);
                        tvChapter.setTextSize(14);

                        // 编辑提示箭头
                        TextView tvArrow = new TextView(this);
                        tvArrow.setText(">");
                        tvArrow.setTextColor(0xFFBBBBBB);
                        tvArrow.setTextSize(14);

                        chapterRow.addView(tvChapter);
                        chapterRow.addView(tvArrow);

                        // 点击章节进入编辑
                        final int chapterIndex = j;
                        final int volIdxForChapter = volumeIndex;
                        chapterRow.setOnClickListener(v -> {
                            Intent editIntent = new Intent(this, StoryGenerateActivity.class);
                            editIntent.putExtra("story_id", storyId);
                            editIntent.putExtra("volume_index", volIdxForChapter);
                            editIntent.putExtra("chapter_index", chapterIndex);
                            startActivity(editIntent);
                        });

                        volumeContent.addView(chapterRow);

                        // 章节之间的分割线
                        if (j < chapters.size() - 1) {
                            View divider = new View(this);
                            divider.setLayoutParams(new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT, 1
                            ));
                            divider.setBackgroundColor(0xFFEEEEEE);
                            volumeContent.addView(divider);
                        }
                    }
                } else {
                    TextView tvEmpty = new TextView(this);
                    tvEmpty.setText("  暂无章节");
                    tvEmpty.setTextColor(0xFF999999);
                    tvEmpty.setTextSize(13);
                    tvEmpty.setPadding(0, 16, 0, 0);
                    volumeContent.addView(tvEmpty);
                }

                volumeCard.addView(volumeContent);

                // 点击整卷进入卷章节列表（分页展示）
                final int volumeIdx = volumeIndex;
                volumeCard.setOnClickListener(v -> {
                    Intent volumeIntent = new Intent(StoryDetailActivity.this, VolumeChaptersActivity.class);
                    volumeIntent.putExtra(VolumeChaptersActivity.EXTRA_STORY_ID, storyId);
                    volumeIntent.putExtra(VolumeChaptersActivity.EXTRA_VOLUME_INDEX, volumeIdx);
                    startActivity(volumeIntent);
                });

                container.addView(volumeCard);
            }
        } catch (Exception e) {
            Log.w("StoryDetailActivity", "Failed to parse story catalog", e);
            TextView errorCatalog = new TextView(this);
            errorCatalog.setText(getString(R.string.story_detail_catalog_empty));
            errorCatalog.setTextColor(0xFF333333);
            errorCatalog.setTextSize(14);
            container.addView(errorCatalog);
        }
    }
}


