package com.example.storyteller.base;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public abstract class BaseActivity extends AppCompatActivity{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 刘海屏适配：启用边到边显示，让布局延伸到状态栏和导航栏区域
        enableEdgeToEdge();
        // 绑定布局（子类实现getLayoutId返回布局ID）
        setContentView(getLayoutId());
        // 初始化视图控件（子类实现）
        initView();
        // 初始化数据/逻辑（子类实现）
        initData();
    }

    /**
     * 启用边到边显示，适配刘海屏
     * 让布局延伸到状态栏和导航栏区域，同时通过 fitsSystemWindows 处理内边距
     */
    private void enableEdgeToEdge() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        // 让状态栏和导航栏图标为浅色（适配浅色背景时可改为 false）
        controller.setAppearanceLightStatusBars(false);
        controller.setAppearanceLightNavigationBars(false);
    }

    /**
     * 为根布局设置系统栏内边距，避免内容被状态栏/导航栏/刘海屏遮挡
     * 在子类的 initView() 中调用：applySystemWindowInsets(findViewById(android.R.id.content))
     * @param rootView 根布局
     */
    protected void applySystemWindowInsets(View rootView) {
        rootView.setOnApplyWindowInsetsListener((v, insets) -> {
            WindowInsetsCompat windowInsets = WindowInsetsCompat.toWindowInsetsCompat(insets, v);
            int statusBarHeight = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            int navigationBarHeight = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
            // 只设置顶部和底部内边距，左右保持原样
            v.setPadding(
                v.getPaddingLeft(),
                statusBarHeight,
                v.getPaddingRight(),
                navigationBarHeight
            );
            return WindowInsetsCompat.CONSUMED;
        });
    }

    // 子类必须实现：返回当前页面的布局ID
    protected abstract int getLayoutId();
    // 初始化视图控件（如findViewById、绑定点击事件）
    protected abstract void initView();
    // 初始化数据/业务逻辑（如加载本地数据、请求API）
    protected abstract void initData();
}
