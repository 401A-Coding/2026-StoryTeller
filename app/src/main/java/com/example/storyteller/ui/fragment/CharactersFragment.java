package com.example.storyteller.ui.fragment;

import android.os.Bundle;
import android.view.View;

import com.example.storyteller.R;
import com.example.storyteller.base.BaseFragment;

/**
 * 人物管理Fragment（占位）
 */
public class CharactersFragment extends BaseFragment {

    private static final String ARG_STORY_ID = "arg_story_id";

    private int storyId;

    public static CharactersFragment newInstance(int storyId) {
        CharactersFragment fragment = new CharactersFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_STORY_ID, storyId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_characters;
    }

    @Override
    protected void initView(View view) {
        // 占位
    }

    @Override
    protected void initData() {
        if (getArguments() != null) {
            storyId = getArguments().getInt(ARG_STORY_ID, -1);
        }
    }
}
