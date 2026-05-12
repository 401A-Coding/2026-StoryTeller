package com.example.storyteller.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.example.storyteller.R;
import com.example.storyteller.base.BaseFragment;
import com.example.storyteller.data.local.prefs.PrefsUtils;
import com.example.storyteller.data.repository.StoryRepository;
import com.example.storyteller.data.repository.StoryRepositoryImpl;
import com.example.storyteller.model.Chapter;
import com.example.storyteller.model.Story;
import com.example.storyteller.model.Volume;
import com.example.storyteller.ui.adapter.StoryAdapter;
import com.google.android.material.tabs.TabLayout;

import java.util.List;

/**
 * 故事信息面板Fragment
 * 整合设定、大纲、目录、文档四个面板
 */
public class StoryInfoPanelFragment extends BaseFragment {

    private static final String ARG_STORY_ID = "arg_story_id";

    // UI Components
    private TextView tvStoryTitle;
    private ImageView btnSelectStory;
    private ImageView btnClosePanel;  // 新增关闭按钮
    private TabLayout tabStoryInfo;
    private EditText etWorldSetting;
    private EditText etOutline;
    private EditText etDocs;
    private LinearLayout layoutToc;
    private View panelSetting;
    private View panelOutline;
    private View panelToc;
    private View panelDocs;

    // Data
    private Story currentStory;
    private int storyId;
    private StoryRepository storyRepository;
    private List<Volume> volumes;
    
    // 小说切换监听器
    private OnStoryChangedListener onStoryChangedListener;
    
    public interface OnStoryChangedListener {
        void onStoryChanged(int newStoryId);
    }
    
    public void setOnStoryChangedListener(OnStoryChangedListener listener) {
        this.onStoryChangedListener = listener;
    }


    public static StoryInfoPanelFragment newInstance(int storyId) {
        StoryInfoPanelFragment fragment = new StoryInfoPanelFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_STORY_ID, storyId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_story_info_panel;
    }

    @Override
    protected void initView(View view) {
        tvStoryTitle = view.findViewById(R.id.tv_story_title);
        btnSelectStory = view.findViewById(R.id.btn_select_story);
        btnClosePanel = view.findViewById(R.id.btn_close_panel);  // 初始化关闭按钮
        tabStoryInfo = view.findViewById(R.id.tab_story_info);
        etWorldSetting = view.findViewById(R.id.et_world_setting);
        etOutline = view.findViewById(R.id.et_outline);
        etDocs = view.findViewById(R.id.et_docs);
        layoutToc = view.findViewById(R.id.layout_toc);
        panelSetting = view.findViewById(R.id.panel_setting);
        panelOutline = view.findViewById(R.id.panel_outline);
        panelToc = view.findViewById(R.id.panel_toc);
        panelDocs = view.findViewById(R.id.panel_docs);

        // 切换小说按钮
        btnSelectStory.setOnClickListener(v -> showStorySelector());

        // 关闭按钮 - 关闭左侧抽屉
        btnClosePanel.setOnClickListener(v -> {
            if (getActivity() != null) {
                androidx.drawerlayout.widget.DrawerLayout drawerLayout = getActivity().findViewById(R.id.drawer_layout);
                if (drawerLayout != null) {
                    drawerLayout.closeDrawer(androidx.core.view.GravityCompat.START);
                }
            }
        });

        // 设置Tab监听
        tabStoryInfo.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switchTab(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    @Override
    protected void initData() {
        storyRepository = new StoryRepositoryImpl(requireContext());

        if (getArguments() != null) {
            storyId = getArguments().getInt(ARG_STORY_ID, -1);
        }

        if (storyId > 0) {
            loadStoryData();
        }
    }

    /**
     * 加载作品数据
     */
    private void loadStoryData() {
        currentStory = storyRepository.getStoryById(storyId);
        if (currentStory == null) {
            Toast.makeText(requireContext(), "加载作品失败", Toast.LENGTH_SHORT).show();
            return;
        }

        // 更新标题
        tvStoryTitle.setText(currentStory.getTitle());

        // 加载简介（用于设定面板）
        if (!TextUtils.isEmpty(currentStory.getDescription())) {
            etWorldSetting.setText(currentStory.getDescription());
        }

        // 加载内容（用于大纲面板 - 暂时使用content字段）
        if (!TextUtils.isEmpty(currentStory.getContent())) {
            etOutline.setText(currentStory.getContent());
        }

        // 文档面板暂时为空
        // TODO: 后续可以添加专门的docs字段到Story模型

        // 解析卷章结构用于显示目录
        String structureJson = currentStory.getStructure();
        if (!TextUtils.isEmpty(structureJson)) {
            try {
                volumes = com.example.storyteller.utils.JsonUtils.fromJson(structureJson,
                    new com.google.gson.reflect.TypeToken<List<Volume>>(){}.getType());
                refreshTocView();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // 只在第一次加载时添加Tab，避免重复添加
        if (tabStoryInfo.getTabCount() == 0) {
            setupTabs();
        }
    }

    /**
     * 设置Tab
     */
    private void setupTabs() {
        tabStoryInfo.addTab(tabStoryInfo.newTab().setText("设定"));
        tabStoryInfo.addTab(tabStoryInfo.newTab().setText("大纲"));
        tabStoryInfo.addTab(tabStoryInfo.newTab().setText("目录"));
        tabStoryInfo.addTab(tabStoryInfo.newTab().setText("文档"));
    }

    /**
     * 切换Tab
     */
    private void switchTab(int position) {
        // 隐藏所有面板
        panelSetting.setVisibility(View.GONE);
        panelOutline.setVisibility(View.GONE);
        panelToc.setVisibility(View.GONE);
        panelDocs.setVisibility(View.GONE);

        // 显示选中的面板
        switch (position) {
            case 0: // 设定
                panelSetting.setVisibility(View.VISIBLE);
                break;
            case 1: // 大纲
                panelOutline.setVisibility(View.VISIBLE);
                break;
            case 2: // 目录
                panelToc.setVisibility(View.VISIBLE);
                break;
            case 3: // 文档
                panelDocs.setVisibility(View.VISIBLE);
                break;
        }
    }

    /**
     * 刷新目录视图
     */
    private void refreshTocView() {
        if (layoutToc == null || volumes == null) {
            return;
        }

        layoutToc.removeAllViews();

        // 添加标题
        TextView titleView = new TextView(requireContext());
        titleView.setText("📚 目录概览");
        titleView.setTextSize(18);
        titleView.setTypeface(null, android.graphics.Typeface.BOLD);
        titleView.setTextColor(0xFF212121);
        titleView.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT));
        ((LinearLayout.LayoutParams) titleView.getLayoutParams()).bottomMargin = 16;
        titleView.setPadding(0, 8, 0, 8);
        layoutToc.addView(titleView);

        // 遍历所有卷和章节
        for (int i = 0; i < volumes.size(); i++) {
            Volume volume = volumes.get(i);

            // 卷标题
            TextView volumeTitle = new TextView(requireContext());
            volumeTitle.setText("第" + (i + 1) + "卷：" + volume.getTitle());
            volumeTitle.setTextSize(16);
            volumeTitle.setTypeface(null, android.graphics.Typeface.BOLD);
            volumeTitle.setTextColor(0xFF1976D2);
            volumeTitle.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
            ((LinearLayout.LayoutParams) volumeTitle.getLayoutParams()).topMargin = 12;
            volumeTitle.setPadding(0, 8, 0, 4);
            layoutToc.addView(volumeTitle);

            // 章节列表
            for (int j = 0; j < volume.getChapters().size(); j++) {
                Chapter chapter = volume.getChapters().get(j);

                TextView chapterTitle = new TextView(requireContext());
                chapterTitle.setText("  第" + (j + 1) + "章：" + chapter.getTitle());
                chapterTitle.setTextSize(14);
                chapterTitle.setTextColor(0xFF424242);
                chapterTitle.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
                ((LinearLayout.LayoutParams) chapterTitle.getLayoutParams()).topMargin = 4;
                chapterTitle.setPadding(16, 4, 0, 4);
                layoutToc.addView(chapterTitle);
            }
        }
    }

    /**
     * 保存面板数据
     */
    public void savePanelData() {
        if (currentStory == null) {
            return;
        }

        // 更新简介（设定面板）
        currentStory.setDescription(etWorldSetting.getText().toString().trim());

        // 更新内容（大纲面板 - 暂时使用content字段）
        // 注意：这里不应该覆盖content，因为content是完整的故事内容
        // TODO: 后续添加专门的outline字段到Story模型
        // currentStory.setContent(etOutline.getText().toString().trim());

        // 文档面板暂时不保存
        // TODO: 后续添加专门的docs字段到Story模型

        // 保存到数据库
        int result = storyRepository.updateStory(currentStory);
        if (result > 0) {
            Toast.makeText(requireContext(), "保存成功", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(requireContext(), "保存失败", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 显示小说选择器
     */
    private void showStorySelector() {
        // 获取所有小说列表
        List<Story> allStories = storyRepository.getAllStories();
        if (allStories == null || allStories.isEmpty()) {
            Toast.makeText(requireContext(), "暂无其他小说", Toast.LENGTH_SHORT).show();
            return;
        }

        // 构建小说标题列表
        String[] storyTitles = new String[allStories.size()];
        int selectedIndex = -1;
        for (int i = 0; i < allStories.size(); i++) {
            Story story = allStories.get(i);
            storyTitles[i] = story.getTitle();
            if (currentStory != null && story.getId() == currentStory.getId()) {
                selectedIndex = i;
            }
        }

        // 显示选择对话框
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("选择小说");
        builder.setSingleChoiceItems(storyTitles, selectedIndex, (dialog, which) -> {
            Story selectedStory = allStories.get(which);
            if (selectedStory.getId() != (currentStory != null ? currentStory.getId() : -1)) {
                // 切换到选中的小说
                switchToStory(selectedStory);
            }
            dialog.dismiss();
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    /**
     * 切换到指定小说
     */
    private void switchToStory(Story story) {
        if (story == null) return;

        android.util.Log.d("StoryInfoPanel", "开始切换小说: " + story.getTitle() + ", ID: " + story.getId());

        // 保存当前小说数据（如果有修改）
        if (currentStory != null) {
            savePanelData();
        }

        // 更新当前小说
        currentStory = story;
        storyId = story.getId();

        // 更新标题显示
        tvStoryTitle.setText(story.getTitle());

        // 更新 SharedPreferences 中的选中状态
        PrefsUtils.getInstance(requireContext())
            .putString(StoryAdapter.PREF_SELECTED_STORY_ID, String.valueOf(story.getId()));
        PrefsUtils.getInstance(requireContext())
            .putString(StoryAdapter.PREF_SELECTED_STORY_TITLE, story.getTitle());

        // 重新加载内容
        loadStoryData();

        // 通知父Activity刷新UI（使用监听器）
        if (onStoryChangedListener != null) {
            android.util.Log.d("StoryInfoPanel", "调用监听器，story_id: " + story.getId());
            onStoryChangedListener.onStoryChanged(story.getId());
        } else {
            android.util.Log.e("StoryInfoPanel", "onStoryChangedListener 为 null");
        }

        Toast.makeText(requireContext(), "已切换到《" + story.getTitle() + "》", Toast.LENGTH_SHORT).show();
    }

    /**
     * 获取当前作品
     */
    public Story getCurrentStory() {
        return currentStory;
    }
}
