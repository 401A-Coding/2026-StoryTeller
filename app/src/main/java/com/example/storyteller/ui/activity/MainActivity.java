package com.example.storyteller.ui.activity;

import android.content.Intent;
import com.example.storyteller.R;
import com.example.storyteller.base.BaseActivity;
import com.example.storyteller.ui.fragment.BookshelfFragment;
import com.example.storyteller.ui.fragment.HomeFragment;
import com.example.storyteller.ui.fragment.MineFragment;
import com.example.storyteller.utils.DatabaseMigrationUtils;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.fragment.app.Fragment;

public class MainActivity extends BaseActivity {

    public static final String EXTRA_TARGET_TAB = "target_tab";
    public static final String TAB_HOME = "home";
    public static final String TAB_BOOKSHELF = "bookshelf";
    public static final String TAB_MINE = "mine";

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
            } else if (itemId == R.id.menu_bookshelf) {
                fragment = new BookshelfFragment();
            } else {
                fragment = new MineFragment();
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
        // 初始化故事字数统计（仅在首次启动或数据库升级后执行）
        DatabaseMigrationUtils.initializeWordCounts(this);
        
        String targetTab = getIntent() != null ? getIntent().getStringExtra(EXTRA_TARGET_TAB) : null;
        bottomNav.setSelectedItemId(getMenuIdForTab(targetTab));
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        String targetTab = intent != null ? intent.getStringExtra(EXTRA_TARGET_TAB) : null;
        bottomNav.setSelectedItemId(getMenuIdForTab(targetTab));
    }

    private int getMenuIdForTab(String tab) {
        if (TAB_BOOKSHELF.equals(tab)) {
            return R.id.menu_bookshelf;
        }
        if (TAB_MINE.equals(tab)) {
            return R.id.menu_mine;
        }
        return R.id.menu_home;
    }
}
