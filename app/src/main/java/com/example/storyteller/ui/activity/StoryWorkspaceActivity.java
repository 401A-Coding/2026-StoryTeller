package com.example.storyteller.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.example.storyteller.R;
import com.example.storyteller.base.BaseActivity;
import com.example.storyteller.data.local.db.StoryDao;
import com.example.storyteller.data.remote.ApiKeyManager;
import com.example.storyteller.data.remote.ModelConfig;
import com.example.storyteller.model.Story;
import com.example.storyteller.ui.adapter.WorkspacePagerAdapter;
import com.example.storyteller.ui.dialog.ModelProviderSettingsDialogHelper;
import com.example.storyteller.ui.fragment.AIPanelFragment;
import com.example.storyteller.ui.fragment.ArchitectureFragment;
import com.example.storyteller.ui.fragment.StoryInfoPanelFragment;
import com.example.storyteller.ui.fragment.WritingFragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.List;

/**
 * 作品工作区Activity
 * 整合写作、架构、人物、素材等所有功能模块
 */
public class StoryWorkspaceActivity extends BaseActivity implements ArchitectureFragment.OnArchitectureChangedListener {

    public static final String EXTRA_STORY_ID = "extra_story_id";
    /** 从剧情树方向卡片"应用到正文"传入的下一章方向信息 */
    public static final String EXTRA_NEXT_CHAPTER_DIRECTION = "extra_next_chapter_direction";

    // UI组件
    private DrawerLayout drawerLayout;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private WorkspacePagerAdapter pagerAdapter;
    private FloatingActionButton fabAI;
    private TextView tvStoryTitle;
    private StoryInfoPanelFragment storyInfoPanelFragment;
    private AIPanelFragment aiPanelFragment;

    // 数据
    private StoryDao storyDao;
    private Story currentStory;
    private int storyId;
    private String pendingDirection; // 从剧情树传入的下一章方向

    @Override
    protected int getLayoutId() {
        return R.layout.activity_story_workspace;
    }

    @Override
    protected void initView() {
        // 刘海屏适配
        applySystemWindowInsets(findViewById(android.R.id.content));

        // 初始化DrawerLayout
        drawerLayout = findViewById(R.id.drawer_layout);
        
        // 设置 DrawerLayout 遮罩层颜色（半透明黑色）
        drawerLayout.setScrimColor(0x80000000); // 50% 透明度的黑色
        
        // 添加抽屉监听器，确保抽屉打开时主内容区域不可交互
        drawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {
                // 抽屉滑动时的回调
            }

            @Override
            public void onDrawerOpened(@NonNull View drawerView) {
                // 抽屉打开时，禁用主内容区域的点击
                View mainContent = findViewById(R.id.coordinator_layout);
                if (mainContent != null) {
                    mainContent.setClickable(false);
                    mainContent.setFocusable(false);
                }
            }

            @Override
            public void onDrawerClosed(@NonNull View drawerView) {
                // 抽屉关闭时，恢复主内容区域的点击
                View mainContent = findViewById(R.id.coordinator_layout);
                if (mainContent != null) {
                    mainContent.setClickable(true);
                    mainContent.setFocusable(true);
                }
            }

            @Override
            public void onDrawerStateChanged(int newState) {
                // 抽屉状态改变时的回调
            }
        });
        
        // 禁用左侧抽屉的手势滑动，只允许通过按钮打开
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, GravityCompat.START);
        
        // 禁用右侧抽屉的手势滑动，只允许通过按钮关闭
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, GravityCompat.END);

        // 初始化UI组件
        tabLayout = findViewById(R.id.tab_layout);
        viewPager = findViewById(R.id.view_pager);
        tvStoryTitle = findViewById(R.id.tv_story_title);
        fabAI = findViewById(R.id.fab_ai);

        // 返回按钮 - 静默保存并退出
        findViewById(R.id.btn_back).setOnClickListener(v -> {
            saveCurrentWorkSilently();
            finish();
        });
        
        // 点击标题区域打开左侧抽屉（整个容器都可点击）
        View titleContainer = findViewById(R.id.layout_title_container);
        if (titleContainer != null) {
            titleContainer.setOnClickListener(v -> openStoryInfoPanel());
            // 添加长按提示
            titleContainer.setOnLongClickListener(v -> {
                Toast.makeText(this, "点击查看小说信息", Toast.LENGTH_SHORT).show();
                return true;
            });
        }

        // 保存按钮
        findViewById(R.id.btn_save).setOnClickListener(v -> saveCurrentWork());

        // AI助手按钮 - 打开右侧AI面板
        fabAI.setOnClickListener(v -> {
            if (!hasAnyProviderApiKey()) {
                ModelProviderSettingsDialogHelper.showApiKeyRequiredDialog(this, "使用 AI 助手");
                return;
            }
            if (!hasAnyProviderEnabled()) {
                ModelProviderSettingsDialogHelper.showProviderDisabledDialog(this, "使用 AI 助手");
                return;
            }
            openAIPanel();
        });
    }

    private boolean hasAnyProviderEnabled() {
        for (ModelConfig.Provider provider : ModelConfig.Provider.values()) {
            if (ModelConfig.isProviderEnabled(this, provider)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasAnyProviderApiKey() {
        for (ModelConfig.Provider provider : ModelConfig.Provider.values()) {
            String apiKey = ApiKeyManager.getApiKey(this, provider);
            if (!TextUtils.isEmpty(apiKey)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void initData() {
        storyDao = new StoryDao(this);

        // 设置返回按钮处理 - 静默保存并退出
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                saveCurrentWorkSilently();
                finish();
            }
        });

        // 获取作品ID和下一章方向信息
        Intent intent = getIntent();
        storyId = intent.getIntExtra(EXTRA_STORY_ID, -1);
        String nextChapterDirection = intent.getStringExtra(EXTRA_NEXT_CHAPTER_DIRECTION);
        if (!TextUtils.isEmpty(nextChapterDirection)) {
            pendingDirection = nextChapterDirection;
        }

        if (storyId > 0) {
            currentStory = storyDao.getStoryById(storyId);
        }

        if (currentStory == null) {
            currentStory = storyDao.getLatestStory();
        }

        if (currentStory != null) {
            storyId = currentStory.getId();
            
            // 更新标题
            tvStoryTitle.setText(currentStory.getTitle());
            
            // 初始化左侧信息面板
            initStoryInfoPanel();
            
            // 初始化右侧AI面板（传入方向）
            initAIPanel();
            
            // 设置ViewPager适配器
            pagerAdapter = new WorkspacePagerAdapter(this, storyId);
            viewPager.setAdapter(pagerAdapter);

            // 如果有下一章方向信息，传递给WritingFragment和AI面板
            if (!TextUtils.isEmpty(nextChapterDirection)) {
                viewPager.postDelayed(() -> {
                    passDirectionToWritingAgent(nextChapterDirection);
                    // 自动打开AI面板并填充方向
                    passDirectionToAIPanel(nextChapterDirection);
                }, 500);
            }

            // 关联TabLayout和ViewPager2
            new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
                tab.setText(pagerAdapter.getPageTitle(position));
            }).attach();

            // 设置Tab切换监听器
            viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    super.onPageSelected(position);
                }
            });
        } else {
            Toast.makeText(this, "未找到作品", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    // ========== Toolbar按钮功能 ==========
    
    private void saveCurrentWork() {
        saveCurrentWorkInternal(true);
    }

    /**
     * 静默保存当前工作（不显示Toast）
     */
    public void saveCurrentWorkSilently() {
        saveCurrentWorkInternal(false);
    }

    /**
     * 内部保存方法
     * @param showToast 是否显示Toast提示
     */
    private void saveCurrentWorkInternal(boolean showToast) {
        // 保存左侧信息面板数据（静默保存）
        if (storyInfoPanelFragment != null) {
            storyInfoPanelFragment.savePanelDataSilently();
        }
        
        // 遍历所有Fragment，保存 WritingFragment 和 ArchitectureFragment
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        
        for (Fragment fragment : fragments) {
            if (fragment instanceof WritingFragment) {
                ((WritingFragment) fragment).saveStructureSilently();
            } else if (fragment instanceof ArchitectureFragment) {
                ((ArchitectureFragment) fragment).saveChangesSilently();
            }
        }
        
        // 显示统一的保存提示
        if (showToast) {
            Toast.makeText(this, "已保存所有更新", Toast.LENGTH_SHORT).show();
        }
    }

    private void openAIPanel() {
        if (!drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.openDrawer(GravityCompat.END);
        } else {
            drawerLayout.closeDrawer(GravityCompat.END);
        }
    }

    /**
     * 初始化AI助手面板
     */
    private void initAIPanel() {
        if (storyId > 0) {
            aiPanelFragment = AIPanelFragment.newInstance(storyId);
            // 设置关闭监听器
            aiPanelFragment.setOnCloseListener(() -> {
                drawerLayout.closeDrawer(GravityCompat.END);
            });
            // 设置命令执行监听器
            aiPanelFragment.setOnCommandExecutedListener(() -> {
                // 命令执行成功后，刷新写作页面的UI
                refreshWritingFragment();
                // 同时刷新目录视图
                refreshTocView();
                // 同时刷新大纲视图
                refreshOutlineView();
                // 刷新设定视图
                refreshSettingsView();
                // 刷新文档视图
                refreshDocumentsView();
            });
            getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.panel_ai, aiPanelFragment)
                .commit();
        }
    }

    /**
     * 初始化故事信息面板
     */
    private void initStoryInfoPanel() {
        if (storyId > 0) {
            storyInfoPanelFragment = StoryInfoPanelFragment.newInstance(storyId);
            // 设置小说切换监听器
            storyInfoPanelFragment.setOnStoryChangedListener(this::reloadStory);
            getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.panel_story_info, storyInfoPanelFragment)
                .commit();
        }
    }

    /**
     * 打开故事信息面板
     */
    private void openStoryInfoPanel() {
        if (!drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.openDrawer(GravityCompat.START);
        } else {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
    }
    
    /**
     * 刷新写作Fragment的UI
     */
    private void refreshWritingFragment() {
        // 遍历所有Fragment来查找WritingFragment
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        
        for (Fragment fragment : fragments) {
            if (fragment instanceof WritingFragment) {
                ((WritingFragment) fragment).refreshView();
                return;
            }
        }
    }
    
    /**
     * 刷新目录视图（公开方法，供WritingFragment调用）
     */
    public void refreshTocView() {
        android.util.Log.d("StoryWorkspace", "refreshTocView: 刷新目录视图");
        if (storyInfoPanelFragment != null) {
            android.util.Log.d("StoryWorkspace", "调用 storyInfoPanelFragment.refreshTocView");
            storyInfoPanelFragment.refreshTocView();
        } else {
            android.util.Log.e("StoryWorkspace", "storyInfoPanelFragment 为 null");
        }
    }
    
    /**
     * 刷新大纲视图（公开方法，供WritingFragment调用）
     */
    public void refreshOutlineView() {
        android.util.Log.d("StoryWorkspace", "refreshOutlineView: 刷新大纲视图");
        if (storyInfoPanelFragment != null) {
            storyInfoPanelFragment.refreshOutlineData();
        }
    }
    
    /**
     * 刷新设定视图（公开方法）
     */
    public void refreshSettingsView() {
        android.util.Log.d("StoryWorkspace", "refreshSettingsView: 刷新设定视图");
        if (storyInfoPanelFragment != null) {
            storyInfoPanelFragment.refreshSettingsList();
        }
    }
    
    /**
     * 刷新文档视图（公开方法）
     */
    public void refreshDocumentsView() {
        android.util.Log.d("StoryWorkspace", "refreshDocumentsView: 刷新文档视图");
        if (storyInfoPanelFragment != null) {
            storyInfoPanelFragment.refreshDocumentsList();
        }
    }
    
    /**
     * 重新加载小说数据
     */
    private void reloadStory(int newStoryId) {
        // 重新启动Activity，但不清除任务栈，保持返回导航正常
        Intent intent = new Intent(this, StoryWorkspaceActivity.class);
        intent.putExtra(EXTRA_STORY_ID, newStoryId);
        startActivity(intent);
        finish();
    }
    
    /**
     * 传递发展方向信息到写作Agent
     */
    private void passDirectionToWritingAgent(String directionText) {
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        for (Fragment fragment : fragments) {
            if (fragment instanceof WritingFragment) {
                ((WritingFragment) fragment).setNextChapterDirection(directionText);
                return;
            }
        }
    }

    /**
     * 传递发展方向到AI面板并自动打开
     */
    private void passDirectionToAIPanel(String directionText) {
        if (aiPanelFragment != null && !TextUtils.isEmpty(directionText)) {
            aiPanelFragment.setNextChapterDirection(directionText);
            // 自动打开AI面板
            openAIPanel();
        }
    }

    /**
     * 导航到指定章节
     * @param volumeIndex 卷索引
     * @param chapterIndex 章索引
     */
    public void navigateToChapter(int volumeIndex, int chapterIndex) {
        android.util.Log.d("StoryWorkspace", "navigateToChapter: 卷" + volumeIndex + " 章" + chapterIndex);
        // 切换到写作Tab
        if (viewPager != null && pagerAdapter != null) {
            android.util.Log.d("StoryWorkspace", "切换到写作Tab");
            viewPager.setCurrentItem(WorkspacePagerAdapter.TAB_WRITING, true);
            
            // 延迟一下，等待Tab切换完成后再通知WritingFragment
            viewPager.postDelayed(() -> {
                android.util.Log.d("StoryWorkspace", "调用 refreshWritingFragmentAndNavigate");
                refreshWritingFragmentAndNavigate(volumeIndex, chapterIndex);
            }, 300);
        } else {
            android.util.Log.e("StoryWorkspace", "viewPager 或 pagerAdapter 为 null");
        }
    }
    
    /**
     * 刷新写作Fragment并导航到指定章节
     */
    private void refreshWritingFragmentAndNavigate(int volumeIndex, int chapterIndex) {
        android.util.Log.d("StoryWorkspace", "refreshWritingFragmentAndNavigate: 卷" + volumeIndex + " 章" + chapterIndex);
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        
        for (Fragment fragment : fragments) {
            if (fragment instanceof WritingFragment) {
                android.util.Log.d("StoryWorkspace", "找到 WritingFragment，调用 navigateToChapter");
                ((WritingFragment) fragment).navigateToChapter(volumeIndex, chapterIndex);
                return;
            }
        }
        android.util.Log.e("StoryWorkspace", "未找到 WritingFragment");
    }

    /**
     * ArchitectureFragment数据变化回调
     * 当架构Tab中的标题、类型、简介发生变化时调用
     */
    @Override
    public void onArchitectureChanged(Story story) {
        if (story != null) {
            String newTitle = story.getTitle();
            if (!TextUtils.isEmpty(newTitle)) {
                // 实时更新顶部标题
                if (tvStoryTitle != null) {
                    tvStoryTitle.setText(newTitle);
                }
                
                // 实时更新左侧抽屉中的标题
                if (storyInfoPanelFragment != null) {
                    storyInfoPanelFragment.updateTitle(newTitle);
                }
            }
        }
    }
}
