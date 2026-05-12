package com.example.storyteller.ui.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.storyteller.ui.fragment.ArchitectureFragment;
import com.example.storyteller.ui.fragment.WritingFragment;

/**
 * 作品详情页面适配器
 * 管理写作和架构两个Tab的Fragment
 */
public class StoryDetailPagerAdapter extends FragmentStateAdapter {

    private final int storyId;
    private static final int TAB_COUNT = 2;
    private static final int TAB_WRITING = 0;
    private static final int TAB_ARCHITECTURE = 1;

    public StoryDetailPagerAdapter(@NonNull FragmentActivity fragmentActivity, int storyId) {
        super(fragmentActivity);
        this.storyId = storyId;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case TAB_WRITING:
                return WritingFragment.newInstance(storyId);
            case TAB_ARCHITECTURE:
                return ArchitectureFragment.newInstance(storyId);
            default:
                return WritingFragment.newInstance(storyId);
        }
    }

    @Override
    public int getItemCount() {
        return TAB_COUNT;
    }

    /**
     * 获取Tab标题
     */
    public String getPageTitle(int position) {
        switch (position) {
            case TAB_WRITING:
                return "写作";
            case TAB_ARCHITECTURE:
                return "小说架构";
            default:
                return "";
        }
    }
}
