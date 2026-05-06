package com.example.storyteller.ui.fragment;

import android.content.Intent;
import android.view.View;
import com.example.storyteller.R;
import com.example.storyteller.base.BaseFragment;
import com.example.storyteller.ui.activity.CharacterActivity;
import com.example.storyteller.ui.activity.MaterialActivity;
import com.example.storyteller.ui.activity.PlotTreeActivity;
import com.example.storyteller.ui.activity.StoryGenerateActivity;

// 首页占位Fragment
public class HomeFragment extends BaseFragment {

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_home;
    }

    @Override
    protected void initView(View view) {
        view.findViewById(R.id.card_story_generate)
            .setOnClickListener(v -> startActivity(new Intent(requireContext(), StoryGenerateActivity.class)));
        view.findViewById(R.id.card_character)
            .setOnClickListener(v -> startActivity(new Intent(requireContext(), CharacterActivity.class)));
        view.findViewById(R.id.card_plot_tree)
            .setOnClickListener(v -> startActivity(new Intent(requireContext(), PlotTreeActivity.class)));
        view.findViewById(R.id.card_material)
            .setOnClickListener(v -> startActivity(new Intent(requireContext(), MaterialActivity.class)));
    }

    @Override
    protected void initData() {
        // 后续加载数据
    }
}