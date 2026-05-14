package com.example.storyteller.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import com.example.storyteller.model.Story;
import com.example.storyteller.ui.adapter.WorkspacePagerAdapter;
import com.example.storyteller.ui.component.BottomActionBar;
import com.example.storyteller.ui.fragment.AIPanelFragment;
import com.example.storyteller.ui.fragment.ArchitectureFragment;
import com.example.storyteller.ui.fragment.StoryInfoPanelFragment;
import com.example.storyteller.ui.fragment.WritingFragment;
import com.google.android.material.card.MaterialCardView;
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

    // UI组件
    private DrawerLayout drawerLayout;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private WorkspacePagerAdapter pagerAdapter;
    private BottomActionBar bottomActionBar;
    private TextView tvStoryTitle;
    private FloatingActionButton fabAI;
    private StoryInfoPanelFragment storyInfoPanelFragment;
    private AIPanelFragment aiPanelFragment;

    // 数据
    private StoryDao storyDao;
    private Story currentStory;
    private int storyId;

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

        // 底部操作栏 - 暂时隐藏（功能开发中）
        MaterialCardView cardBottomBar = findViewById(R.id.bottom_action_bar);
        LinearLayout layoutActions = findViewById(R.id.layout_bottom_actions);
        bottomActionBar = new BottomActionBar(this, cardBottomBar, layoutActions);
        
        // 确保底部栏保持隐藏
        if (cardBottomBar != null) {
            cardBottomBar.setVisibility(View.GONE);
        }

        // 返回按钮 - 自动保存并退出
        findViewById(R.id.btn_back).setOnClickListener(v -> {
            saveCurrentWork();
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

        // 更多操作按钮
        findViewById(R.id.btn_more).setOnClickListener(v -> showMoreMenu());

        // AI助手按钮 - 打开右侧AI面板
        fabAI.setOnClickListener(v -> openAIPanel());
    }

    @Override
    protected void initData() {
        storyDao = new StoryDao(this);

        // 设置返回按钮处理 - 自动保存并退出
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                saveCurrentWork();
                finish();
            }
        });

        // 获取作品ID
        Intent intent = getIntent();
        storyId = intent.getIntExtra(EXTRA_STORY_ID, -1);

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
            
            // 初始化右侧AI面板
            initAIPanel();
            
            // 设置ViewPager适配器
            pagerAdapter = new WorkspacePagerAdapter(this, storyId);
            viewPager.setAdapter(pagerAdapter);

            // 关联TabLayout和ViewPager2
            new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
                tab.setText(pagerAdapter.getPageTitle(position));
            }).attach();

            // 设置Tab切换监听器
            viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    super.onPageSelected(position);
                    // 暂时禁用底部操作栏更新（功能开发中）
                    // updateBottomActionBar(position);
                }
            });

            // 初始化底部操作栏（默认显示写作Tab的按钮）- 暂时禁用
            // updateBottomActionBar(WorkspacePagerAdapter.TAB_WRITING);
        } else {
            Toast.makeText(this, "未找到作品", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /**
     * 根据当前Tab更新底部操作栏
     */
    private void updateBottomActionBar(int tabPosition) {
        switch (tabPosition) {
            case WorkspacePagerAdapter.TAB_WRITING:
                setupWritingBottomActions();
                break;
            case WorkspacePagerAdapter.TAB_ARCHITECTURE:
                setupArchitectureBottomActions();
                break;
            case WorkspacePagerAdapter.TAB_CHARACTERS:
                bottomActionBar.setupCharactersActions();
                break;
            case WorkspacePagerAdapter.TAB_MATERIALS:
                bottomActionBar.setupMaterialsActions();
                break;
            case WorkspacePagerAdapter.TAB_MORE:
                bottomActionBar.setupMoreActions();
                break;
        }
    }

    /**
     * 配置写作Tab的底部按钮
     */
    private void setupWritingBottomActions() {
        bottomActionBar.setupWritingActions(
            this::addVolume,      // + 卷
            this::addChapter,     // + 章
            this::aiContinue,     // AI续写
            this::showStats       // 统计
        );
    }

    /**
     * 配置架构Tab的底部按钮
     */
    private void setupArchitectureBottomActions() {
        bottomActionBar.setupArchitectureActions(
            this::saveArchitecture,   // 保存
            this::aiOptimize,         // AI优化
            this::previewArchitecture // 预览
        );
    }

    // ========== 底部按钮功能实现 ==========

    private void addVolume() {
        Toast.makeText(this, "添加新卷（功能开发中）", Toast.LENGTH_SHORT).show();
        // TODO: 切换到写作Fragment并添加卷
    }

    private void addChapter() {
        Toast.makeText(this, "添加新章节（功能开发中）", Toast.LENGTH_SHORT).show();
        // TODO: 切换到写作Fragment并添加章节
    }

    private void aiContinue() {
        openAIPanel();
        // 预填充“续写”指令
        if (aiPanelFragment != null) {
            aiPanelFragment.prefillMessage("请帮我续写下一章内容");
        }
    }

    private void showStats() {
        Toast.makeText(this, "字数统计（功能开发中）", Toast.LENGTH_SHORT).show();
        // TODO: 显示统计信息
    }

    private void saveArchitecture() {
        Toast.makeText(this, "保存架构信息（功能开发中）", Toast.LENGTH_SHORT).show();
        // TODO: 保存架构Fragment的数据
    }

    private void aiOptimize() {
        openAIPanel();
        // 预填充“优化”指令
        if (aiPanelFragment != null) {
            aiPanelFragment.prefillMessage("请帮我优化简介和大纲");
        }
    }

    private void previewArchitecture() {
        Toast.makeText(this, "预览效果（功能开发中）", Toast.LENGTH_SHORT).show();
        // TODO: 显示预览界面
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

    private void showMoreMenu() {
        // 显示更多操作菜单
        android.widget.PopupMenu popupMenu = new android.widget.PopupMenu(this, findViewById(R.id.btn_more));
        popupMenu.getMenu().add("导出作品");
        popupMenu.getMenu().add("分享");
        popupMenu.getMenu().add("删除作品");
        popupMenu.getMenu().add("设置");
        
        popupMenu.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();
            Toast.makeText(this, title + "（功能开发中）", Toast.LENGTH_SHORT).show();
            return true;
        });
        
        popupMenu.show();
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
     * 重新加载小说数据
     */
    private void reloadStory(int newStoryId) {
        // 最简单可靠的方法：重新启动Activity
        Intent intent = new Intent(this, StoryWorkspaceActivity.class);
        intent.putExtra(EXTRA_STORY_ID, newStoryId);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
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

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        // 退出前先保存所有数据（静默保存，不显示Toast）
        saveCurrentWorkSilently();
        
        // 延迟一下再调用super，确保数据库事务完成
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            StoryWorkspaceActivity.super.onBackPressed();
        }, 100);
    }
}
