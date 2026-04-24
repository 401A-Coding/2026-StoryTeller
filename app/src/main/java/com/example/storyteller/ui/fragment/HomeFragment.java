package com.example.storyteller.ui.fragment;

import android.view.View;
import com.example.storyteller.R;
import com.example.storyteller.base.BaseFragment;

// 首页占位Fragment
public class HomeFragment extends BaseFragment {

    @Override
    protected int getLayoutId() {
        // 后续创建 fragment_home.xml 即可
        return R.layout.fragment_home;
    }

    @Override
    protected void initView(View view) {
        // 后续绑定控件
    }

    @Override
    protected void initData() {
        // 后续加载数据
    }
}