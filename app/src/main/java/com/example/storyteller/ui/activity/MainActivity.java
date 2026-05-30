package com.example.storyteller.ui.activity;

import android.content.Intent;
import android.content.SharedPreferences;

import com.example.storyteller.R;
import com.example.storyteller.base.BaseActivity;
import com.example.storyteller.ui.fragment.HomeFragment;
import com.example.storyteller.ui.fragment.StoryManagementFragment;
import com.example.storyteller.ui.fragment.SettingsFragment;
import com.example.storyteller.utils.DatabaseMigrationUtils;
import com.example.storyteller.utils.ThemeManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.fragment.app.Fragment;

public class MainActivity extends BaseActivity {

    public static final String EXTRA_TARGET_TAB = "target_tab";
    public static final String TAB_HOME = "home";
    public static final String TAB_STORY_MANAGEMENT = "story_management";
    public static final String TAB_SETTINGS = "settings";

    private BottomNavigationView bottomNav;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_main;
    }

    @Override
    protected void initView() {
        // 根布局只处理顶部 inset，避免全页面底部多余留白
        applySystemWindowInsets(findViewById(android.R.id.content));
        bottomNav = findViewById(R.id.bottom_nav);
        // 底部导航单独处理导航栏 inset，保证可点击区域不被遮挡
        applySystemWindowInsets(bottomNav, false, true);
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment fragment;
            int itemId = item.getItemId();
            if (itemId == R.id.menu_home) {
                fragment = new HomeFragment();
            } else if (itemId == R.id.menu_story_management) {
                fragment = new StoryManagementFragment();
            } else if (itemId == R.id.menu_settings) {
                fragment = new SettingsFragment();
            } else {
                fragment = new HomeFragment();
            }
            getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
            return true;
        });
    }

    @Override
    protected void initData() {
        // 应用保存的主题设置
        ThemeManager.getInstance(this).applySavedTheme();
        
        // 初始化故事字数统计（仅在首次启动或数据库升级后执行）
        DatabaseMigrationUtils.initializeWordCounts(this);
        
        // 检查是否有最近保存的Tab状态（5秒内）
        // 如果有，说明是配置变更（如主题切换）导致的Activity重建，恢复Tab
        // 如果没有，说明是冷启动，显示首页
        SharedPreferences prefs = getSharedPreferences("main_activity_prefs", MODE_PRIVATE);
        long savedTime = prefs.getLong("tab_save_time", 0);
        long currentTime = System.currentTimeMillis();
        
        if (currentTime - savedTime < 5000) {
            restoreSelectedTab();
        } else {
            // 清除保存的Tab状态
            prefs.edit().putInt("last_selected_tab", 0).apply();
            String targetTab = getIntent() != null ? getIntent().getStringExtra(EXTRA_TARGET_TAB) : null;
            bottomNav.setSelectedItemId(getMenuIdForTab(targetTab));
        }
    }
    
    /**
     * 保存当前选中的 Tab 到 Preference
     * 在切换主题前调用
     */
    public void saveCurrentTab() {
        int selectedId = bottomNav.getSelectedItemId();
        int tabIndex = 0;
        if (selectedId == R.id.menu_story_management) {
            tabIndex = 1;
        } else if (selectedId == R.id.menu_settings) {
            tabIndex = 2;
        }
        SharedPreferences prefs = getSharedPreferences("main_activity_prefs", MODE_PRIVATE);
        prefs.edit()
                .putInt("last_selected_tab", tabIndex)
                .putLong("tab_save_time", System.currentTimeMillis())
                .apply();
    }
    
    /**
     * 恢复之前选中的 Tab
     */
    private void restoreSelectedTab() {
        int tabIndex = getSharedPreferences("main_activity_prefs", MODE_PRIVATE)
                .getInt("last_selected_tab", 0);
        int menuId;
        switch (tabIndex) {
            case 1:
                menuId = R.id.menu_story_management;
                break;
            case 2:
                menuId = R.id.menu_settings;
                break;
            default:
                menuId = R.id.menu_home;
        }
        bottomNav.setSelectedItemId(menuId);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        String targetTab = intent != null ? intent.getStringExtra(EXTRA_TARGET_TAB) : null;
        bottomNav.setSelectedItemId(getMenuIdForTab(targetTab));
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 如果当前是小说管理Tab，通知Fragment刷新数据
        if (bottomNav.getSelectedItemId() == R.id.menu_story_management) {
            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            if (currentFragment instanceof StoryManagementFragment) {
                // 延迟一下，确保数据库事务已完成
                currentFragment.getView().postDelayed(() -> {
                    // TODO: 调用刷新方法
                    // ((StoryManagementFragment) currentFragment).refreshStoriesPublic();
                }, 50);
            }
        }
    }

    private int getMenuIdForTab(String tab) {
        if (TAB_STORY_MANAGEMENT.equals(tab)) {
            return R.id.menu_story_management;
        }
        if (TAB_SETTINGS.equals(tab)) {
            return R.id.menu_settings;
        }
        return R.id.menu_home;
    }
}
