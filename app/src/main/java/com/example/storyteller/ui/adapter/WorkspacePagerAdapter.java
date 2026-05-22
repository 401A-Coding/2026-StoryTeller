package com.example.storyteller.ui.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.storyteller.ui.fragment.ArchitectureFragment;
import com.example.storyteller.ui.fragment.CharactersFragment;
import com.example.storyteller.ui.fragment.PlotGraphFragment;
import com.example.storyteller.ui.fragment.MoreFragment;
import com.example.storyteller.ui.fragment.WritingFragment;

/**
 * 作品工作区页面适配器
 * 管理所有Tab的Fragment
 */
public class WorkspacePagerAdapter extends FragmentStateAdapter {

    private final int storyId;
    private final long adapterId;  // 用于强制刷新Fragment
    
    // Tab定义
    public static final int TAB_WRITING = 0;
    public static final int TAB_ARCHITECTURE = 1;
    public static final int TAB_CHARACTERS = 2;
    public static final int TAB_GRAPH = 3;
    public static final int TAB_MORE = 4;
    
    public static final int TAB_COUNT = 5;

    public WorkspacePagerAdapter(@NonNull FragmentActivity fragmentActivity, int storyId) {
        super(fragmentActivity);
        this.storyId = storyId;
        this.adapterId = System.currentTimeMillis();  // 每次创建都使用不同的ID
    }
    
    /**
     * 重写getItemId，确保每次切换小说后Fragment都会被重新创建
     */
    @Override
    public long getItemId(int position) {
        // 结合position和adapterId，确保ID唯一
        return adapterId * 10 + position;
    }
    
    /**
     * 重写containsItem，确保Fragment不会被复用
     */
    @Override
    public boolean containsItem(long itemId) {
        return true;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case TAB_WRITING:
                return WritingFragment.newInstance(storyId);
            case TAB_ARCHITECTURE:
                return ArchitectureFragment.newInstance(storyId);
            case TAB_CHARACTERS:
                return CharactersFragment.newInstance(storyId);
            case TAB_GRAPH:
                return PlotGraphFragment.newInstance(storyId);
            case TAB_MORE:
                return MoreFragment.newInstance(storyId);
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
                return "架构";
            case TAB_CHARACTERS:
                return "人物";
            case TAB_GRAPH:
                return "关系图";
            case TAB_MORE:
                return "更多 ⋯";
            default:
                return "";
        }
    }
}
