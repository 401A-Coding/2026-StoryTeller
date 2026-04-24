package com.example.storyteller.base;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public abstract class BaseFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(getLayoutId(), container, false);
        initView(view);
        initData();
        return view;
    }

    // 子类必须实现：布局ID
    protected abstract int getLayoutId();

    // 初始化控件
    protected abstract void initView(View view);

    // 初始化数据
    protected abstract void initData();
}