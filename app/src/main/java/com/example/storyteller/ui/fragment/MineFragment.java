package com.example.storyteller.ui.fragment;

import android.content.Intent;
import android.view.View;
import com.example.storyteller.R;
import com.example.storyteller.base.BaseFragment;
import com.example.storyteller.ui.activity.SettingsActivity;

public class MineFragment extends BaseFragment {

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_mine;
    }

    @Override
    protected void initView(View view) {
        view.findViewById(R.id.btn_settings)
            .setOnClickListener(v -> startActivity(new Intent(requireContext(), SettingsActivity.class)));
    }

    @Override
    protected void initData() {
        // 占位
    }
}

