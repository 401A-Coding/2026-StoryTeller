package com.example.storyteller.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.storyteller.R;
import com.example.storyteller.base.BaseActivity;
import com.example.storyteller.data.local.db.StoryDao;
import com.example.storyteller.model.SeriesGroup;
import com.example.storyteller.model.Story;
import com.example.storyteller.ui.adapter.StoryAdapter;
import com.example.storyteller.utils.ThemeManager;

import java.util.ArrayList;
import java.util.List;

/**
 * 系列详情页：展示同一系列下的所有分支书
 */
public class SeriesDetailActivity extends AppCompatActivity {

    public static final String EXTRA_SERIES_NAME = "extra_series_name";

    private StoryDao storyDao;
    private StoryAdapter adapter;
    private RecyclerView recyclerView;
    private TextView tvTitle;
    private TextView tvStoryCount;
    private TextView tvEmptyHint;

    private String seriesName;
    private List<Story> seriesStories = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 应用主题
        ThemeManager.getInstance(this).applySavedTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_series_detail);

        seriesName = getIntent() != null ? getIntent().getStringExtra(EXTRA_SERIES_NAME) : null;
        if (seriesName == null || seriesName.isEmpty()) {
            finish();
            return;
        }

        initView();
        initData();
    }

    private void initView() {
        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        tvTitle = findViewById(R.id.tv_series_detail_title);
        tvStoryCount = findViewById(R.id.tv_series_detail_count);
        tvEmptyHint = findViewById(R.id.tv_empty_hint);
        recyclerView = findViewById(R.id.rv_series_stories);

        tvTitle.setText(seriesName);

        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));

        storyDao = new StoryDao(this);
        adapter = new StoryAdapter(this, new ArrayList<>());

        adapter.setOnStoryDeleteListener(storyId -> refreshStories());
        adapter.setOnStoryCategoryChangeListener((storyId, newCategory) -> refreshStories());
        adapter.setOnStoryCoverChangeListener((storyId, newCoverColor) -> refreshStories());

        recyclerView.setAdapter(adapter);
    }

    private void initData() {
        refreshStories();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStories();
    }

    private void refreshStories() {
        if (storyDao == null) return;

        // 强制重新获取数据库连接
        storyDao = new StoryDao(this);

        // 获取所有故事，按系列名过滤
        List<Story> allStories = storyDao.getAllStories();
        seriesStories.clear();
        for (Story story : allStories) {
            String sn = story.getSeriesName();
            if (sn != null && sn.equals(seriesName)) {
                seriesStories.add(story);
            }
        }

        adapter.setData(seriesStories);
        tvStoryCount.setText(seriesStories.size() + "部作品");

        if (seriesStories.isEmpty()) {
            tvEmptyHint.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvEmptyHint.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }
}
