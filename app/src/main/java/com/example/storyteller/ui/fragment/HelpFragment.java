package com.example.storyteller.ui.fragment;

import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.example.storyteller.R;
import com.example.storyteller.base.BaseFragment;

/**
 * 使用帮助页面
 * 介绍各个功能模块的使用方法
 */
public class HelpFragment extends BaseFragment {

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_help;
    }

    @Override
    protected void initView(View view) {
        // 书架模块帮助
        view.findViewById(R.id.card_bookshelf).setOnClickListener(v -> showBookshelfHelp());
        
        // 小说工作区帮助
        view.findViewById(R.id.card_workspace).setOnClickListener(v -> showWorkspaceHelp());
        
        // 设定与素材帮助
        view.findViewById(R.id.card_settings).setOnClickListener(v -> showSettingsHelp());
        
        // AI功能帮助
        view.findViewById(R.id.card_ai).setOnClickListener(v -> showAiHelp());
        
        // 快捷操作帮助
        view.findViewById(R.id.card_shortcuts).setOnClickListener(v -> showShortcutsHelp());
        
        // 访问GitHub
        view.findViewById(R.id.btn_github).setOnClickListener(v -> openGitHub());
    }

    @Override
    protected void initData() {
        // 不需要额外初始化
    }

    /**
     * 书架模块帮助
     */
    private void showBookshelfHelp() {
        String helpContent = "📚 书架 - 小说管理\n\n" +
                "【创建小说】\n" +
                "• 点击右上角「+ 创建」按钮\n" +
                "• 输入标题、系列名（可选）和简介\n" +
                "• 自动创建一个卷和一个章节\n\n" +
                "【查找小说】\n" +
                "• 使用搜索框按标题或系列名搜索\n" +
                "• 点击「排序」按时间/标题/字数排序\n" +
                "• 点击「筛选」按收藏/状态筛选\n\n" +
                "【管理小说】\n" +
                "• 点击卡片直接进入编辑页面\n" +
                "• 点击三点菜单：收藏/上传封面/修改分类/删除\n\n" +
                "💡 提示：点击卡片直接进入编辑，无需经过详情页！";
        
        showHelpDialog("书架使用帮助", helpContent);
    }

    /**
     * 小说工作区帮助
     */
    private void showWorkspaceHelp() {
        String helpContent = "✍️ 小说工作区\n\n" +
                "【写作Tab】\n" +
                "• 大纲：生成/查看小说整体框架\n" +
                "• 章节：编辑卷结构和章节内容\n" +
                "• AI辅助：创作建议、续写、优化\n\n" +
                "【架构Tab】\n" +
                "• 基本信息：书名、简介、标签\n" +
                "• 大纲：查看/编辑故事主线\n" +
                "• 设定：角色、物品、地点等设定\n" +
                "• 关系：管理角色间的关系\n\n" +
                "【数据安全】\n" +
                "• 自动保存，无需手动保存\n" +
                "• 随时可导出为TXT文件";
        
        showHelpDialog("工作区使用帮助", helpContent);
    }

    /**
     * 设定与素材帮助
     */
    private void showSettingsHelp() {
        String helpContent = "🗂️ 设定与素材\n\n" +
                "【设定分类】\n" +
                "• 角色：人物外貌、性格、背景\n" +
                "• 物品：装备、道具、宝物\n" +
                "• 地点：场景、地点、环境\n" +
                "• 事件：重要情节、历史\n" +
                "• 势力：组织、种族、门派\n" +
                "• 概念：规则、体系、世界观\n\n" +
                "【素材库】\n" +
                "• 全局素材库：所有小说共享的素材\n" +
                "• 单本素材：仅当前小说可用的素材\n" +
                "• 从参考书库导入：爬取网络小说提取素材\n\n" +
                "【关系管理】\n" +
                "• 为角色建立关系网\n" +
                "• 支持关系类型自定义\n" +
                "• 自动生成关系图谱";
        
        showHelpDialog("设定与素材帮助", helpContent);
    }

    /**
     * AI功能帮助
     */
    private void showAiHelp() {
        String helpContent = "🤖 AI智能助手\n\n" +
                "【智能体模式】\n" +
                "• 执行复杂创作任务\n" +
                "• 支持续写、扩写、润色\n" +
                "• 自动提取和整理素材\n\n" +
                "【生图功能】\n" +
                "• AI生成封面/配图\n" +
                "• 支持自定义提示词\n" +
                "• 可保存到本地或分享\n\n" +
                "【审校功能】\n" +
                "• 全面审核内容质量\n" +
                "• 检查逻辑、错别字\n" +
                "• 提供修改建议";
        
        showHelpDialog("AI功能帮助", helpContent);
    }

    /**
     * 快捷操作帮助
     */
    private void showShortcutsHelp() {
        String helpContent = "⚡ 快捷操作\n\n" +
                "【编辑页面】\n" +
                "• 点击设定卡片可查看预览\n" +
                "• 长按卡片可编辑/删除\n" +
                "• 分享卡片可自定义封面\n\n" +
                "【大纲编辑】\n" +
                "• 点击标题直接编辑\n" +
                "• 拖拽调整顺序\n" +
                "• 点击+添加新项\n\n" +
                "【写作偏好】\n" +
                "• 可设置全局写作风格\n" +
                "• 新建小说自动应用";
        
        showHelpDialog("快捷操作帮助", helpContent);
    }

    private void showHelpDialog(String title, String content) {
        new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setMessage(content)
                .setPositiveButton("知道了", null)
                .show();
    }

    private void openGitHub() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://github.com/401A-Coding/2026-StoryTeller"));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "无法打开链接", Toast.LENGTH_SHORT).show();
        }
    }
}