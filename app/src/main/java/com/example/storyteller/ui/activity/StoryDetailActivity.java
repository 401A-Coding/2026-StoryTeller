package com.example.storyteller.ui.activity;

import android.content.Intent;
import android.text.TextUtils;
import android.widget.TextView;

import androidx.viewpager2.widget.ViewPager2;

import com.example.storyteller.R;
import com.example.storyteller.base.BaseActivity;
import com.example.storyteller.data.local.db.StoryDao;
import com.example.storyteller.data.local.prefs.PrefsUtils;
import com.example.storyteller.model.Story;
import com.example.storyteller.ui.adapter.StoryAdapter;
import com.example.storyteller.ui.adapter.StoryDetailPagerAdapter;
import com.example.storyteller.ui.fragment.ArchitectureFragment;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class StoryDetailActivity extends BaseActivity implements ArchitectureFragment.OnArchitectureChangedListener {

    public static final String EXTRA_STORY_ID = StoryAdapter.EXTRA_STORY_ID;
    public static final String EXTRA_STORY_TITLE = StoryAdapter.EXTRA_STORY_TITLE;

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private StoryDetailPagerAdapter pagerAdapter;
    private int storyId;
    private Story currentStory;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_story_detail;
    }

    @Override
    protected void initView() {
        // 刘海屏适配：为根布局设置系统栏内边距
        applySystemWindowInsets(findViewById(android.R.id.content));
        
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        tabLayout = findViewById(R.id.tab_layout);
        viewPager = findViewById(R.id.view_pager);
    }

    @Override
    protected void initData() {
        Intent intent = getIntent();
        storyId = intent.getIntExtra(EXTRA_STORY_ID, -1);
        String storyTitle = intent.getStringExtra(EXTRA_STORY_TITLE);
        
        StoryDao storyDao = new StoryDao(this);

        if (storyId > 0) {
            currentStory = storyDao.getStoryById(storyId);
        }
        
        if (currentStory == null) {
            currentStory = storyDao.getLatestStory();
        }

        if (currentStory != null) {
            storyId = currentStory.getId();
            
            // 保存当前选择的作品
            PrefsUtils.getInstance(this).putString(StoryAdapter.PREF_SELECTED_STORY_ID, String.valueOf(storyId));
            PrefsUtils.getInstance(this).putString(StoryAdapter.PREF_SELECTED_STORY_TITLE, currentStory.getTitle());
            
            // 更新Toolbar标题
            setTitle(currentStory.getTitle());
        } else {
            // 未找到作品，显示提示并返回
            android.widget.Toast.makeText(this, "未找到作品", android.widget.Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 设置ViewPager适配器
        pagerAdapter = new StoryDetailPagerAdapter(this, storyId);
        viewPager.setAdapter(pagerAdapter);
        
        // 关联TabLayout和ViewPager2
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            tab.setText(pagerAdapter.getPageTitle(position));
        }).attach();
    }

    /**
     * 当架构信息改变时的回调
     */
    @Override
    public void onArchitectureChanged(Story story) {
        // 更新当前作品数据
        this.currentStory = story;
        // 可以在这里刷新其他UI或通知其他Fragment
    }
}


