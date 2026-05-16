package com.example.storyteller.ui.fragment;

import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.storyteller.R;
import com.example.storyteller.base.BaseFragment;

/**
 * 创作Fragment（占位）
 * TODO: 后续实现完整功能，作为底部导航的"创作"Tab
 * 注意：这与StoryWorkspaceActivity中的WritingFragment不同
 * 这个Fragment将作为快速进入创作的入口
 */
public class CreationFragment extends BaseFragment {

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_creation_placeholder;
    }

    @Override
    protected void initView(View view) {
        TextView tvPlaceholder = view.findViewById(R.id.tv_placeholder);
        tvPlaceholder.setText("创作工作台开发中...\n\n这里将显示：\n• 当前小说的快速编辑入口\n• AI创作助手\n• 创作统计");
        
        // 临时提示
        Toast.makeText(requireContext(), "创作Tab已添加（功能开发中）", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void initData() {
        // TODO: 初始化数据
    }
}
