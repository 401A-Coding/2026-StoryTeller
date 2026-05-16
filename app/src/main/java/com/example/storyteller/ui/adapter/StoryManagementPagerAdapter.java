package com.example.storyteller.ui.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.storyteller.ui.fragment.MaterialLibraryFragment;
import com.example.storyteller.ui.fragment.MyCreationsFragment;
import com.example.storyteller.ui.fragment.ReferenceLibraryFragment;

/**
 * 小说管理Tab的PagerAdapter
 */
public class StoryManagementPagerAdapter extends FragmentStateAdapter {

    private static final int TAB_COUNT = 3;
    private static final int TAB_MY_CREATIONS = 0;
    private static final int TAB_REFERENCE_LIBRARY = 1;
    private static final int TAB_MATERIAL_LIBRARY = 2;

    public StoryManagementPagerAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case TAB_MY_CREATIONS:
                return new MyCreationsFragment();
            case TAB_REFERENCE_LIBRARY:
                return new ReferenceLibraryFragment();
            case TAB_MATERIAL_LIBRARY:
                return new MaterialLibraryFragment();
            default:
                return new MyCreationsFragment();
        }
    }

    @Override
    public int getItemCount() {
        return TAB_COUNT;
    }

    /**
     * 获取Tab标题
     */
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case TAB_MY_CREATIONS:
                return "我的创作";
            case TAB_REFERENCE_LIBRARY:
                return "参考书库";
            case TAB_MATERIAL_LIBRARY:
                return "素材库";
            default:
                return "";
        }
    }
}
