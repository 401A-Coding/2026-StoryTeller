package com.example.storyteller.ui.fragment;

import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.example.storyteller.BuildConfig;
import com.example.storyteller.R;
import com.example.storyteller.base.BaseFragment;

/**
 * 意见反馈页面
 */
public class FeedbackFragment extends BaseFragment {

    private static final String GITHUB_ISSUES_URL = "https://github.com/401A-Coding/2026-StoryTeller/issues/new";
    private static final String GITHUB_PAGE_URL = "https://github.com/401A-Coding/2026-StoryTeller";
    private static final String FEEDBACK_EMAIL = "1750096317@qq.com";

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_feedback;
    }

    @Override
    protected void initView(View view) {
        // GitHub Issue
        view.findViewById(R.id.card_github_issue).setOnClickListener(v -> openGitHubIssue());

        // 发送邮件
        view.findViewById(R.id.card_email).setOnClickListener(v -> sendEmail());

        // 项目主页
        view.findViewById(R.id.card_project_page).setOnClickListener(v -> openProjectPage());
    }

    @Override
    protected void initData() {
        // 不需要额外初始化
    }

    private void openGitHubIssue() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_ISSUES_URL));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "无法打开链接", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendEmail() {
        try {
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
            emailIntent.setData(Uri.parse("mailto:"));
            emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{FEEDBACK_EMAIL});
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "StoryTeller意见反馈");
            emailIntent.putExtra(Intent.EXTRA_TEXT, "\n\n---\n" +
                    "版本：" + BuildConfig.VERSION_NAME + "\n" +
                    "设备：Android\n");
            if (emailIntent.resolveActivity(requireContext().getPackageManager()) != null) {
                startActivity(Intent.createChooser(emailIntent, "发送邮件"));
            } else {
                Toast.makeText(requireContext(), "未找到邮件应用", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(requireContext(), "无法发送邮件", Toast.LENGTH_SHORT).show();
        }
    }

    private void openProjectPage() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_PAGE_URL));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "无法打开链接", Toast.LENGTH_SHORT).show();
        }
    }
}