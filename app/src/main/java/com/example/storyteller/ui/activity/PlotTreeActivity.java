package com.example.storyteller.ui.activity;

import android.content.Intent;
import com.example.storyteller.R;
import com.example.storyteller.base.BaseActivity;

public class PlotTreeActivity extends BaseActivity {

    @Override
    protected int getLayoutId() {
        return R.layout.activity_plot_tree;
    }

    @Override
    protected void initView() {
        // 刘海屏适配：为根布局设置系统栏内边距
        applySystemWindowInsets(findViewById(android.R.id.content));
        findViewById(R.id.btn_back_home).setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra(MainActivity.EXTRA_TARGET_TAB, MainActivity.TAB_HOME);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });
    }

    @Override
    protected void initData() {
        // 占位
    }
}

