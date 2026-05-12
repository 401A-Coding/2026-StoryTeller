package com.example.storyteller.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import com.example.storyteller.R;
import com.example.storyteller.base.BaseActivity;
import com.example.storyteller.data.local.db.StoryDao;
import com.example.storyteller.model.Chapter;
import com.example.storyteller.model.Story;
import com.example.storyteller.model.Volume;
import com.example.storyteller.utils.JsonUtils;
import com.google.android.material.tabs.TabLayout;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 卷章节列表页面
 * 每10章一页，支持滑动翻页和底部页数提示
 */
public class VolumeChaptersActivity extends BaseActivity {

    public static final String EXTRA_STORY_ID = "extra_story_id";
    public static final String EXTRA_VOLUME_INDEX = "extra_volume_index";

    private ViewPager viewPager;
    private TabLayout tabPageIndicator;
    private TextView tvVolumeTitle;
    private ChaptersPagerAdapter pagerAdapter;
    private List<List<Chapter>> chapterPages = new ArrayList<>();
    private int storyId;
    private int volumeIndex;
    private String volumeTitle;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_volume_chapters;
    }

    @Override
    protected void initView() {
        applySystemWindowInsets(findViewById(android.R.id.content));

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        tvVolumeTitle = findViewById(R.id.tv_volume_title);
        viewPager = findViewById(R.id.view_pager_chapters);
        tabPageIndicator = findViewById(R.id.tab_page_indicator);

        Intent intent = getIntent();
        storyId = intent.getIntExtra(EXTRA_STORY_ID, -1);
        volumeIndex = intent.getIntExtra(EXTRA_VOLUME_INDEX, -1);

        loadVolumeData();
    }

    private void loadVolumeData() {
        if (storyId <= 0 || volumeIndex < 0) {
            tvVolumeTitle.setText("无效参数");
            return;
        }

        StoryDao storyDao = new StoryDao(this);
        Story story = storyDao.getStoryById(storyId);
        if (story == null) {
            tvVolumeTitle.setText("故事未找到");
            return;
        }

        // 获取卷列表：优先从 structure 解析，失败则从 content 解析
        List<Volume> volumes = null;
        String structureJson = story.getStructure();
        if (!TextUtils.isEmpty(structureJson)) {
            try {
                Type type = new TypeToken<List<Volume>>() {}.getType();
                volumes = JsonUtils.fromJson(structureJson, type);
            } catch (Exception e) {
                Log.w("VolumeChaptersActivity", "Failed to parse structure JSON", e);
            }
        }
        if (volumes == null || volumes.isEmpty()) {
            if (!TextUtils.isEmpty(story.getContent())) {
                volumes = parseVolumesFromContent(story.getContent());
            }
        }

        if (volumes == null || volumeIndex >= volumes.size()) {
            tvVolumeTitle.setText("卷未找到");
            return;
        }

        Volume volume = volumes.get(volumeIndex);
        volumeTitle = TextUtils.isEmpty(volume.getTitle()) ? "未命名卷" : volume.getTitle().trim();
        tvVolumeTitle.setText("第" + (volumeIndex + 1) + "卷 · " + volumeTitle);

        List<Chapter> chapters = volume.getChapters();
        if (chapters == null || chapters.isEmpty()) {
            tvVolumeTitle.setText(tvVolumeTitle.getText() + "（暂无章节）");
            return;
        }

        // 每20章一页
        chapterPages.clear();
        int pageSize = 20;
        for (int i = 0; i < chapters.size(); i += pageSize) {
            int end = Math.min(i + pageSize, chapters.size());
            chapterPages.add(new ArrayList<>(chapters.subList(i, end)));
        }

        pagerAdapter = new ChaptersPagerAdapter();
        viewPager.setAdapter(pagerAdapter);

        // 设置底部页数指示器
        tabPageIndicator.setupWithViewPager(viewPager, true);

        // 更新页数标签
        for (int i = 0; i < tabPageIndicator.getTabCount(); i++) {
            com.google.android.material.tabs.TabLayout.Tab tab = tabPageIndicator.getTabAt(i);
            if (tab != null) {
                int startChapter = i * pageSize + 1;
                int endChapter = Math.min((i + 1) * pageSize, chapters.size());
                tab.setText(startChapter + "-" + endChapter + "章");
            }
        }
    }

    @Override
    protected void initData() {
        // 占位
    }

    /**
     * 从纯文本 content 中解析出卷-章结构
     */
    private List<Volume> parseVolumesFromContent(String content) {
        List<Volume> volumes = new ArrayList<>();
        if (TextUtils.isEmpty(content)) {
            return volumes;
        }

        String[] lines = content.split("\n");
        Volume currentVolume = null;
        List<Chapter> currentChapters = new ArrayList<>();
        int volumeCount = 0;

        Pattern volumePattern = Pattern.compile("^##\\s*第[一二三四五六七八九十百千0-9]+卷[\\s　]*([^\\n]*)");
        Pattern chapterPattern = Pattern.compile("^第[一二三四五六七八九十百千0-9]+[章节][\\s　]*([^\\n]*)");

        for (String line : lines) {
            String trimmed = line.trim();
            if (TextUtils.isEmpty(trimmed)) {
                continue;
            }

            Matcher volumeMatcher = volumePattern.matcher(trimmed);
            if (volumeMatcher.find()) {
                if (currentVolume != null) {
                    currentVolume.setChapters(new ArrayList<>(currentChapters));
                    volumes.add(currentVolume);
                    currentChapters.clear();
                }
                volumeCount++;
                String volTitle = volumeMatcher.group(1).trim();
                currentVolume = new Volume(TextUtils.isEmpty(volTitle) ? "第" + volumeCount + "卷" : volTitle);
                continue;
            }

            Matcher chapterMatcher = chapterPattern.matcher(trimmed);
            if (chapterMatcher.find()) {
                String chapTitle = chapterMatcher.group(1).trim();
                Chapter chapter = new Chapter(TextUtils.isEmpty(chapTitle) ? "第" + (currentChapters.size() + 1) + "章" : chapTitle);
                currentChapters.add(chapter);
                continue;
            }

            if (currentVolume == null) {
                Matcher altChapterMatcher = Pattern.compile("^##\\s*第[一二三四五六七八九十百千0-9]+[章节][\\s　]*([^\\n]*)").matcher(trimmed);
                if (altChapterMatcher.find()) {
                    volumeCount++;
                    currentVolume = new Volume("第" + volumeCount + "卷");
                    String chapTitle = altChapterMatcher.group(1).trim();
                    Chapter chapter = new Chapter(TextUtils.isEmpty(chapTitle) ? "第" + (currentChapters.size() + 1) + "章" : chapTitle);
                    currentChapters.add(chapter);
                }
            }
        }

        if (currentVolume != null) {
            currentVolume.setChapters(new ArrayList<>(currentChapters));
            volumes.add(currentVolume);
        }

        if (volumes.isEmpty()) {
            Volume defaultVolume = new Volume("第一卷");
            defaultVolume.setChapters(new ArrayList<>(currentChapters));
            if (defaultVolume.getChapters().isEmpty()) {
                Chapter defaultChapter = new Chapter("第一章");
                List<Chapter> defaultChapters = new ArrayList<>();
                defaultChapters.add(defaultChapter);
                defaultVolume.setChapters(defaultChapters);
            }
            volumes.add(defaultVolume);
        }

        return volumes;
    }

    /**
     * 章节分页适配器
     */
    private class ChaptersPagerAdapter extends PagerAdapter {

        @Override
        public int getCount() {
            return chapterPages.size();
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return view == object;
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            LinearLayout pageLayout = new LinearLayout(VolumeChaptersActivity.this);
            pageLayout.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ));
            pageLayout.setOrientation(LinearLayout.VERTICAL);
            pageLayout.setPadding(32, 16, 32, 16);

            List<Chapter> pageChapters = chapterPages.get(position);
            int globalStartIndex = position * 20;

            for (int i = 0; i < pageChapters.size(); i++) {
                Chapter chapter = pageChapters.get(i);
                final int chapterGlobalIndex = globalStartIndex + i;
                String chapterTitle = TextUtils.isEmpty(chapter.getTitle()) ? "未命名章" : chapter.getTitle().trim();

                // 章节行
                LinearLayout chapterRow = new LinearLayout(VolumeChaptersActivity.this);
                chapterRow.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ));
                chapterRow.setOrientation(LinearLayout.HORIZONTAL);
                chapterRow.setPadding(0, 20, 0, 20);
                chapterRow.setClickable(true);
                chapterRow.setFocusable(true);
                try {
                    chapterRow.setForeground(getDrawable(android.R.attr.selectableItemBackground));
                } catch (Exception ignored) {
                }

                // 章节序号
                TextView tvIndex = new TextView(VolumeChaptersActivity.this);
                tvIndex.setLayoutParams(new LinearLayout.LayoutParams(
                    48,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ));
                tvIndex.setText(String.valueOf(chapterGlobalIndex + 1));
                tvIndex.setTextColor(0xFF9C27B0);
                tvIndex.setTextSize(16);
                tvIndex.setTypeface(tvIndex.getTypeface(), android.graphics.Typeface.BOLD);
                tvIndex.setGravity(android.view.Gravity.CENTER);

                // 章节标题
                TextView tvTitle = new TextView(VolumeChaptersActivity.this);
                tvTitle.setLayoutParams(new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1
                ));
                tvTitle.setText(chapterTitle);
                tvTitle.setTextColor(0xFF333333);
                tvTitle.setTextSize(15);
                tvTitle.setPadding(12, 0, 0, 0);

                // 箭头
                TextView tvArrow = new TextView(VolumeChaptersActivity.this);
                tvArrow.setText(">");
                tvArrow.setTextColor(0xFFBBBBBB);
                tvArrow.setTextSize(14);

                chapterRow.addView(tvIndex);
                chapterRow.addView(tvTitle);
                chapterRow.addView(tvArrow);

                // 点击进入编辑
                chapterRow.setOnClickListener(v -> {
                    Intent editIntent = new Intent(VolumeChaptersActivity.this, StoryGenerateActivity.class);
                    editIntent.putExtra("story_id", storyId);
                    editIntent.putExtra("volume_index", volumeIndex);
                    editIntent.putExtra("chapter_index", chapterGlobalIndex);
                    startActivity(editIntent);
                });

                pageLayout.addView(chapterRow);

                // 分割线
                if (i < pageChapters.size() - 1) {
                    View divider = new View(VolumeChaptersActivity.this);
                    divider.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 1
                    ));
                    divider.setBackgroundColor(0xFFEEEEEE);
                    pageLayout.addView(divider);
                }
            }

            container.addView(pageLayout);
            return pageLayout;
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            container.removeView((View) object);
        }
    }
}
