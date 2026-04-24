package com.example.storyteller.ui.activity;

import android.os.Bundle;
import com.example.storyteller.R;
import com.example.storyteller.base.BaseActivity;

public class MainActivity extends BaseActivity {

    @Override
    protected int getLayoutId() {
        // 绑定主页面布局（后续在res/layout/activity_main.xml中实现）
        return R.layout.activity_main;
    }

    @Override
    protected void initView() {
        // 后续在这里findViewById、绑定点击事件
    }

    @Override
    protected void initData() {
        // 后续在这里加载本地数据、初始化页面逻辑
    }
}