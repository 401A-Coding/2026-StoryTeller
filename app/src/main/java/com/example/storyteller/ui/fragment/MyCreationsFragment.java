package com.example.storyteller.ui.fragment;

import android.view.View;

import com.example.storyteller.R;
import com.example.storyteller.base.BaseFragment;

/**
 * 我的创作Fragment
 * 嵌入BookshelfFragment，显示用户创作的小说列表
 */
public class MyCreationsFragment extends BaseFragment {

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_my_creations;
    }

    @Override
    protected void initView(View view) {
        // 在子FragmentManager中嵌入BookshelfFragment
        getChildFragmentManager()
            .beginTransaction()
            .replace(R.id.container_my_creations, new BookshelfFragment())
            .commit();
    }

    @Override
    protected void initData() {
        // 不需要额外初始化
    }
}
