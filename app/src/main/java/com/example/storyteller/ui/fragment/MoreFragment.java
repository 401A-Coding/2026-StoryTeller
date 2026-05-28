package com.example.storyteller.ui.fragment;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.example.storyteller.R;
import com.example.storyteller.base.BaseFragment;
import com.example.storyteller.data.local.db.StoryDao;
import com.example.storyteller.model.Chapter;
import com.example.storyteller.model.Story;
import com.example.storyteller.model.Volume;
import com.example.storyteller.utils.JsonUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 更多功能Fragment
 * 收纳作品管理、设置、帮助等次要功能
 */
public class MoreFragment extends BaseFragment {

    private static final String ARG_STORY_ID = "arg_story_id";

    private int storyId;
    private StoryDao storyDao;
    private Story currentStory;

    // 文件选择器Launcher
    private final ActivityResultLauncher<String> createDocumentLauncher =
            registerForActivityResult(new ActivityResultContracts.CreateDocument("text/plain"), uri -> {
                if (uri != null) {
                    saveExportedFile(uri);
                }
            });

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
     * 导出作品为TXT
     */
    private void exportStory() {
        // 从数据库获取作品
        currentStory = storyDao.getStoryById(storyId);
        if (currentStory == null) {
            Toast.makeText(requireContext(), "作品不存在", Toast.LENGTH_SHORT).show();
            return;
        }

        // 使用系统文件选择器让用户选择保存位置
        String fileName = sanitizeFileName(currentStory.getTitle()) + ".txt";
        createDocumentLauncher.launch(fileName);
    }

    /**
     * 保存导出文件
     */
    private void saveExportedFile(Uri uri) {
        try {
            String txtContent = buildTxtContent();

            // 写入文件
            java.io.OutputStream outputStream = requireContext().getContentResolver().openOutputStream(uri);
            if (outputStream != null) {
                outputStream.write(txtContent.getBytes("UTF-8"));
                outputStream.close();
                Toast.makeText(requireContext(), "导出成功！", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "导出失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 构建TXT内容
     */
    private String buildTxtContent() {
        StringBuilder sb = new StringBuilder();

        // 标题
        sb.append(currentStory.getTitle()).append("\n\n");

        // 简介
        String description = currentStory.getDescription();
        if (description != null && !description.isEmpty()) {
            sb.append("【简介】\n").append(description).append("\n\n");
        }

        // 卷章节内容
        String structure = currentStory.getStructure();
        if (structure != null && !structure.isEmpty()) {
            try {
                // 使用Gson解析为Volume列表（Story的结构）
                java.util.List<Volume> volumes = JsonUtils.fromJson(structure,
                    new com.google.gson.reflect.TypeToken<java.util.List<Volume>>(){}.getType());
                if (volumes != null) {
                    for (int i = 0; i < volumes.size(); i++) {
                        Volume volume = volumes.get(i);
                        String volumeTitle = volume.getTitle();
                        if (volumeTitle == null || volumeTitle.isEmpty()) {
                            volumeTitle = "第" + (i + 1) + "卷";
                        }
                        // 卷标题
                        sb.append("\n").append("=".repeat(24)).append("\n");
                        sb.append(volumeTitle).append("\n");
                        sb.append("=".repeat(24)).append("\n\n");

                        // 章节内容
                        java.util.List<Chapter> chapters = volume.getChapters();
                        if (chapters != null) {
                            for (int j = 0; j < chapters.size(); j++) {
                                Chapter chapter = chapters.get(j);
                                String chapterTitle = chapter.getTitle();
                                String chapterContent = chapter.getContent();

                                // 章标题
                                if (chapterTitle != null && !chapterTitle.isEmpty()) {
                                    sb.append("第" + (j + 1) + "章：" + chapterTitle).append("\n\n");
                                }
                                // 章正文
                                if (chapterContent != null && !chapterContent.isEmpty()) {
                                    sb.append(chapterContent);
                                } else {
                                    sb.append("（暂无正文）");
                                }
                                sb.append("\n\n");
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                sb.append("\n（卷章节解析失败）\n");
            }
        } else {
            sb.append("\n（暂无卷章节结构）\n");
        }

        // 添加导出信息
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        sb.append("\n\n---\n");
        sb.append("导出时间：").append(sdf.format(new Date())).append("\n");
        sb.append("导出工具：StoryTeller\n");

        return sb.toString();
    }

    /**
     * 清理文件名中的非法字符
     */
    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "未命名作品";
        }
        // 替换Windows文件名非法字符
        return fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    /**
     * 分享作品
     */
    private void shareStory() {
        Toast.makeText(requireContext(), "分享功能（开发中）", Toast.LENGTH_SHORT).show();
        // TODO: 实现分享功能
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
