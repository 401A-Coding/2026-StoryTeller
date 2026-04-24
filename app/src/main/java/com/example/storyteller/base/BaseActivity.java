package com.example.storyteller.base;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
public abstract class BaseActivity extends AppCompatActivity{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 绑定布局（子类实现getLayoutId返回布局ID）
        setContentView(getLayoutId());
        // 初始化视图控件（子类实现）
        initView();
        // 初始化数据/逻辑（子类实现）
        initData();
    }
    // 子类必须实现：返回当前页面的布局ID
    protected abstract int getLayoutId();
    // 初始化视图控件（如findViewById、绑定点击事件）
    protected abstract void initView();
    // 初始化数据/业务逻辑（如加载本地数据、请求API）
    protected abstract void initData();
}
