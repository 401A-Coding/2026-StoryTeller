package com.example.storyteller.ui.fragment;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.example.storyteller.R;
import com.example.storyteller.base.BaseFragment;
import com.example.storyteller.data.local.db.StoryDao;

/**
 * 更多功能Fragment
 * 收纳作品管理、设置、帮助等次要功能
 */
public class MoreFragment extends BaseFragment {

    private static final String ARG_STORY_ID = "arg_story_id";

    private int storyId;
    private StoryDao storyDao;

    public static MoreFragment newInstance(int storyId) {
        MoreFragment fragment = new MoreFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_STORY_ID, storyId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_more;
    }

    @Override
    protected void initView(View view) {
        storyDao = new StoryDao(requireContext());

        // 作品管理
        view.findViewById(R.id.btn_export).setOnClickListener(v -> exportStory());
        view.findViewById(R.id.btn_share).setOnClickListener(v -> shareStory());
        view.findViewById(R.id.btn_delete).setOnClickListener(v -> deleteStory());

        // 设置
        view.findViewById(R.id.btn_settings).setOnClickListener(v -> openSettings());
        view.findViewById(R.id.btn_about).setOnClickListener(v -> showAbout());

        // 帮助与反馈
        view.findViewById(R.id.btn_help).setOnClickListener(v -> showHelp());
        view.findViewById(R.id.btn_feedback).setOnClickListener(v -> showFeedback());
    }

    @Override
    protected void initData() {
        if (getArguments() != null) {
            storyId = getArguments().getInt(ARG_STORY_ID, -1);
        }
    }

    /**
     * 导出作品
     */
    private void exportStory() {
        Toast.makeText(requireContext(), "导出功能（开发中）", Toast.LENGTH_SHORT).show();
        // TODO: 实现导出为TXT/JSON等功能
    }

    /**
     * 分享作品
     */
    private void shareStory() {
        Toast.makeText(requireContext(), "分享功能（开发中）", Toast.LENGTH_SHORT).show();
        // TODO: 实现分享功能
    }

    /**
     * 删除作品
     */
    private void deleteStory() {
        new AlertDialog.Builder(requireContext())
            .setTitle("确认删除")
            .setMessage("确定要删除这部作品吗？此操作不可恢复！")
            .setPositiveButton("删除", (dialog, which) -> {
                int result = storyDao.deleteStory(storyId);
                if (result > 0) {
                    Toast.makeText(requireContext(), "已删除", Toast.LENGTH_SHORT).show();
                    requireActivity().finish();
                } else {
                    Toast.makeText(requireContext(), "删除失败", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    /**
     * 打开设置
     */
    private void openSettings() {
        // TODO: 切换到MainActivity的设置Tab
        Toast.makeText(requireContext(), "请切换到底部导航的“设置”Tab", Toast.LENGTH_SHORT).show();
    }

    /**
     * 显示关于
     */
    private void showAbout() {
        new AlertDialog.Builder(requireContext())
            .setTitle("关于 StoryTeller")
            .setMessage("版本：1.0.0\n\n一款AI辅助的小说创作工具\n帮助你更好地构思和写作故事。")
            .setPositiveButton("确定", null)
            .show();
    }

    /**
     * 显示帮助
     */
    private void showHelp() {
        new AlertDialog.Builder(requireContext())
            .setTitle("使用帮助")
            .setMessage(
                "📝 写作Tab：编辑小说的卷章结构\n\n" +
                "🏗️ 架构Tab：编辑小说基本信息\n\n" +
                "👥 人物Tab：管理角色设定（开发中）\n\n" +
                "📚 素材Tab：管理写作素材（开发中）\n\n" +
                "💡 点击标题可打开左侧信息面板\n\n" +
                "🤖 右下角按钮唤起AI助手（开发中）"
            )
            .setPositiveButton("确定", null)
            .show();
    }

    /**
     * 显示反馈
     */
    private void showFeedback() {
        Toast.makeText(requireContext(), "意见反馈功能（开发中）", Toast.LENGTH_SHORT).show();
        // TODO: 实现反馈功能
    }
}
