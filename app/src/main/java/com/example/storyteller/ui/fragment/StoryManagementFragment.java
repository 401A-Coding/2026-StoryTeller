package com.example.storyteller.ui.fragment;

import android.view.View;

import com.example.storyteller.R;
import com.example.storyteller.base.BaseFragment;
import com.example.storyteller.ui.adapter.StoryManagementPagerAdapter;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import androidx.viewpager2.widget.ViewPager2;

/**
 * 小说管理Fragment
 * 包含两个子Tab：
 * 1. 我的创作 - 显示用户创作的小说列表（复用BookshelfFragment）
 * 2. 参考书库 - 显示导入的小说（待实现）
 */
public class StoryManagementFragment extends BaseFragment {

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private StoryManagementPagerAdapter pagerAdapter;

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_story_management;
    }

    @Override
    protected void initView(View view) {
        tabLayout = view.findViewById(R.id.tab_layout);
        viewPager = view.findViewById(R.id.view_pager);

        // 设置ViewPager2适配器
        pagerAdapter = new StoryManagementPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        // 关联TabLayout和ViewPager2
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            tab.setText(pagerAdapter.getPageTitle(position));
        }).attach();
    }

    @Override
    protected void initData() {
        // 不需要额外初始化
    }
}
