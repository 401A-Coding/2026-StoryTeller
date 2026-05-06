package com.example.storyteller.ui.fragment;

import android.content.Intent;
import android.view.View;
import com.example.storyteller.R;
import com.example.storyteller.base.BaseFragment;
import com.example.storyteller.ui.activity.StoryPreviewActivity;

public class BookshelfFragment extends BaseFragment {

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_bookshelf;
    }

    @Override
    protected void initView(View view) {
        view.findViewById(R.id.btn_story_preview)
            .setOnClickListener(v -> startActivity(new Intent(requireContext(), StoryPreviewActivity.class)));
    }

    @Override
    protected void initData() {
        // 占位
    }
}

