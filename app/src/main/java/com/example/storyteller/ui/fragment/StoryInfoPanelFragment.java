package com.example.storyteller.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import android.content.res.Resources;
import android.graphics.Typeface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.example.storyteller.R;
import com.example.storyteller.base.BaseFragment;
import com.example.storyteller.data.local.prefs.PrefsUtils;
import com.example.storyteller.data.repository.StoryRepository;
import com.example.storyteller.data.repository.StoryRepositoryImpl;
import com.example.storyteller.model.Chapter;
import com.example.storyteller.model.Story;
import com.example.storyteller.model.Volume;
import com.example.storyteller.ui.adapter.StoryAdapter;
import com.example.storyteller.ui.fragment.StorySettingsListFragment;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
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
    private LinearLayout layoutToc;
    private HorizontalScrollView horizontalPageIndicator;  // 页码指示器滚动容器
    private LinearLayout layoutPageIndicator;  // 页码指示器容器
    private View panelSetting;
    private View panelOutline;
    private View panelToc;
    private View panelDocs;

    // Data
    private Story currentStory;
    private int storyId;
    private StoryRepository storyRepository;
    private List<Volume> volumes;
    
    // 分页相关
    private int currentPagePosition = 0;
    private int pageSize = 20;  // 每页显示20章
    private List<TextView> pageIndicatorViews = new ArrayList<>();
    
    // 记录每个卷的展开状态和容器
    private List<Boolean> volumeExpandedStates = new ArrayList<>();
    private List<LinearLayout> volumeContainers = new ArrayList<>();
    
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
        layoutToc = view.findViewById(R.id.layout_toc);
        horizontalPageIndicator = view.findViewById(R.id.horizontal_page_indicator);
        layoutPageIndicator = view.findViewById(R.id.layout_page_indicator);
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

        // 设定面板已改为动态加载 Fragment

        // 大纲面板已改为动态加载 OutlineFragment

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
     * 公开方法：更新标题（用于实时同步）
     */
    public void updateTitle(String newTitle) {
        if (tvStoryTitle != null && !TextUtils.isEmpty(newTitle)) {
            tvStoryTitle.setText(newTitle);
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
                // 动态加载设定列表面板
                loadSettingsListFragment();
                break;
            case 1: // 大纲
                panelOutline.setVisibility(View.VISIBLE);
                // 动态加载大纲面板（每次都刷新）
                refreshOutlineData();
                break;
            case 2: // 目录
                panelToc.setVisibility(View.VISIBLE);
                // 刷新目录视图
                refreshTocView();
                break;
            case 3: // 文档
                panelDocs.setVisibility(View.VISIBLE);
                // 动态加载文档列表面板
                loadDocumentsFragment();
                break;
        }
    }
    
    /**
     * 加载设定列表面板
     */
    private void loadSettingsListFragment() {
        if (storyId <= 0) return;
        
        // 检查是否已经添加过
        if (getChildFragmentManager().findFragmentById(R.id.panel_setting) == null) {
            StorySettingsListFragment settingsFragment = StorySettingsListFragment.newInstance(storyId);
            getChildFragmentManager()
                .beginTransaction()
                .replace(R.id.panel_setting, settingsFragment)
                .commit();
        }
    }
    
    /**
     * 加载大纲面板
     */
    private void loadOutlineFragment() {
        if (storyId <= 0) return;
        
        // 每次都重新创建OutlineFragment，确保数据最新
        com.example.storyteller.ui.fragment.OutlineFragment outlineFragment = 
            com.example.storyteller.ui.fragment.OutlineFragment.newInstance(storyId);
        getChildFragmentManager()
            .beginTransaction()
            .replace(R.id.panel_outline, outlineFragment)
            .commit();
    }
    
    /**
     * 刷新大纲数据（公开方法）
     */
    public void refreshOutlineData() {
        loadOutlineFragment();
    }
    
    /**
     * 刷新设定列表面板（公开方法）
     */
    public void refreshSettingsList() {
        // 重新创建SettingsFragment
        if (storyId > 0) {
            StorySettingsListFragment settingsFragment = StorySettingsListFragment.newInstance(storyId);
            getChildFragmentManager()
                .beginTransaction()
                .replace(R.id.panel_setting, settingsFragment)
                .commit();
        }
    }
    
    /**
     * 刷新文档列表面板（公开方法）
     */
    public void refreshDocumentsList() {
        // 重新创建DocumentsFragment
        if (storyId > 0) {
            com.example.storyteller.ui.fragment.DocumentsFragment documentsFragment = 
                com.example.storyteller.ui.fragment.DocumentsFragment.newInstance(storyId);
            getChildFragmentManager()
                .beginTransaction()
                .replace(R.id.panel_docs, documentsFragment)
                .commit();
        }
    }
    
    /**
     * 加载文档列表面板
     */
    private void loadDocumentsFragment() {
        if (storyId <= 0) return;
        
        // 检查是否已经添加过
        if (getChildFragmentManager().findFragmentById(R.id.panel_docs) == null) {
            com.example.storyteller.ui.fragment.DocumentsFragment documentsFragment = 
                com.example.storyteller.ui.fragment.DocumentsFragment.newInstance(storyId);
            getChildFragmentManager()
                .beginTransaction()
                .replace(R.id.panel_docs, documentsFragment)
                .commit();
        }
    }

    /**
     * 获取主题颜色
     */
    private int getThemeColor(int colorResId) {
        return ContextCompat.getColor(requireContext(), colorResId);
    }
    
    /**
     * 刷新目录视图（公开方法，供外部调用）
     */
    public void refreshTocView() {
        android.util.Log.d("StoryInfoPanel", "refreshTocView: 开始刷新目录");
        // 重新从数据库加载最新数据
        if (storyId > 0) {
            Story latestStory = storyRepository.getStoryById(storyId);
            if (latestStory != null) {
                currentStory = latestStory;
                String structureJson = currentStory.getStructure();
                android.util.Log.d("StoryInfoPanel", "structureJson长度: " + (structureJson != null ? structureJson.length() : 0));
                if (!TextUtils.isEmpty(structureJson)) {
                    try {
                        volumes = com.example.storyteller.utils.JsonUtils.fromJson(structureJson,
                            new com.google.gson.reflect.TypeToken<List<Volume>>(){}.getType());
                        android.util.Log.d("StoryInfoPanel", "解析成功，卷数量: " + volumes.size());
                    } catch (Exception e) {
                        e.printStackTrace();
                        android.util.Log.e("StoryInfoPanel", "解析失败: " + e.getMessage());
                        volumes = new ArrayList<>();
                    }
                } else {
                    android.util.Log.d("StoryInfoPanel", "structureJson为空");
                    volumes = new ArrayList<>();
                }
            }
        }
        
        if (layoutToc == null) {
            return;
        }
        
        // 如果volumes为null，初始化为空列表
        if (volumes == null) {
            volumes = new ArrayList<>();
        }

        layoutToc.removeAllViews();
        
        // 清除页码指示器（不再需要分页）
        if (layoutPageIndicator != null) {
            layoutPageIndicator.removeAllViews();
            pageIndicatorViews.clear();
        }
        
        // 初始化展开状态
        volumeExpandedStates.clear();
        volumeContainers.clear();
        
        // 如果没有卷，显示提示
        if (volumes.isEmpty()) {
            TextView noVolumes = new TextView(requireContext());
            noVolumes.setText("暂无内容");
            noVolumes.setTextSize(14);
            noVolumes.setTextColor(getThemeColor(R.color.text_hint));
            noVolumes.setPadding(16, 16, 0, 16);
            layoutToc.addView(noVolumes);
            return;
        }

        // 遍历所有卷
        for (int i = 0; i < volumes.size(); i++) {
            Volume volume = volumes.get(i);
            List<Chapter> chapters = volume.getChapters();
            
            // 默认全部展开
            boolean isExpanded = true;
            volumeExpandedStates.add(isExpanded);
            
            // 创建卷容器
            LinearLayout volumeContainer = new LinearLayout(requireContext());
            volumeContainer.setOrientation(LinearLayout.VERTICAL);
            volumeContainer.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
            ((LinearLayout.LayoutParams) volumeContainer.getLayoutParams()).topMargin = 8;
            volumeContainers.add(volumeContainer);
            
            // 创建卷标题行（可点击展开/收起）
            LinearLayout volumeHeader = new LinearLayout(requireContext());
            volumeHeader.setOrientation(LinearLayout.HORIZONTAL);
            volumeHeader.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
            volumeHeader.setPadding(0, 12, 0, 8);
            volumeHeader.setClickable(true);
            volumeHeader.setFocusable(true);
            
            // 设置点击背景效果
            try {
                android.content.res.TypedArray typedArray = requireContext().obtainStyledAttributes(
                    new int[]{android.R.attr.selectableItemBackground});
                volumeHeader.setForeground(typedArray.getDrawable(0));
                typedArray.recycle();
            } catch (Exception ignored) {
            }
            
            // 展开/收起图标
            TextView tvExpandIcon = new TextView(requireContext());
            tvExpandIcon.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
            tvExpandIcon.setText(isExpanded ? "▼" : "▶");
            tvExpandIcon.setTextSize(12);
            tvExpandIcon.setTextColor(getThemeColor(R.color.colorPrimary));
            tvExpandIcon.setMinWidth(24);
            tvExpandIcon.setGravity(android.view.Gravity.CENTER);
            
            // 卷标题
            TextView volumeTitle = new TextView(requireContext());
            volumeTitle.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1));
            volumeTitle.setText("第" + (i + 1) + "卷：" + volume.getTitle());
            volumeTitle.setTextSize(15);
            volumeTitle.setTypeface(null, android.graphics.Typeface.BOLD);
            volumeTitle.setTextColor(getThemeColor(R.color.colorPrimary));
            volumeTitle.setPadding(4, 0, 0, 0);
            
            // 章节数量
            TextView chapterCount = new TextView(requireContext());
            chapterCount.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
            int count = chapters != null ? chapters.size() : 0;
            chapterCount.setText(count + "章");
            chapterCount.setTextSize(12);
            chapterCount.setTextColor(getThemeColor(R.color.text_hint));
            chapterCount.setPadding(8, 0, 0, 0);
            
            volumeHeader.addView(tvExpandIcon);
            volumeHeader.addView(volumeTitle);
            volumeHeader.addView(chapterCount);
            
            // 点击卷标题展开/收起
            final int volumeIndex = i;
            final TextView expandIcon = tvExpandIcon;
            final LinearLayout chaptersContainer = new LinearLayout(requireContext());
            chaptersContainer.setOrientation(LinearLayout.VERTICAL);
            
            volumeHeader.setOnClickListener(v -> {
                boolean expanded = volumeExpandedStates.get(volumeIndex);
                volumeExpandedStates.set(volumeIndex, !expanded);
                expandIcon.setText(expanded ? "▶" : "▼");
                chaptersContainer.setVisibility(expanded ? View.GONE : View.VISIBLE);
            });
            
            volumeContainer.addView(volumeHeader);
            
            // 章节列表容器
            chaptersContainer.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
            
            if (chapters != null && !chapters.isEmpty()) {
                for (int j = 0; j < chapters.size(); j++) {
                    Chapter chapter = chapters.get(j);
                    
                    // 创建章节行容器
                    LinearLayout chapterRow = new LinearLayout(requireContext());
                    chapterRow.setOrientation(LinearLayout.HORIZONTAL);
                    chapterRow.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));
                    chapterRow.setPadding(0, 8, 0, 8);
                    chapterRow.setClickable(true);
                    chapterRow.setFocusable(true);
                    
                    // 设置点击背景效果
                    try {
                        android.content.res.TypedArray typedArray = requireContext().obtainStyledAttributes(
                            new int[]{android.R.attr.selectableItemBackground});
                        chapterRow.setForeground(typedArray.getDrawable(0));
                        typedArray.recycle();
                    } catch (Exception ignored) {
                    }
                    
                    // 章节序号（使用卷内序号）
                    TextView tvIndex = new TextView(requireContext());
                    tvIndex.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));
                    tvIndex.setMinWidth(48);
                    tvIndex.setText(String.valueOf(j + 1));
                    tvIndex.setTextColor(getThemeColor(R.color.colorPrimary));
                    tvIndex.setTextSize(14);
                    tvIndex.setTypeface(tvIndex.getTypeface(), android.graphics.Typeface.BOLD);
                    tvIndex.setGravity(android.view.Gravity.CENTER);
                    
                    // 章节标题
                    TextView tvTitle = new TextView(requireContext());
                    tvTitle.setLayoutParams(new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                    ));
                    String chapterTitleText = TextUtils.isEmpty(chapter.getTitle()) ? 
                        "未命名章" : chapter.getTitle().trim();
                    tvTitle.setText(chapterTitleText);
                    tvTitle.setTextColor(getThemeColor(R.color.text_primary));
                    tvTitle.setTextSize(14);
                    tvTitle.setPadding(12, 0, 0, 0);
                    
                    // 箭头
                    TextView tvArrow = new TextView(requireContext());
                    tvArrow.setText(">");
                    tvArrow.setTextColor(getThemeColor(R.color.text_secondary));
                    tvArrow.setTextSize(14);
                    
                    chapterRow.addView(tvIndex);
                    chapterRow.addView(tvTitle);
                    chapterRow.addView(tvArrow);
                    
                    // 添加点击事件 - 跳转到对应位置
                    final int volIdx = volumeIndex;
                    final int chapIdx = j;
                    chapterRow.setOnClickListener(v -> {
                        android.util.Log.d("StoryInfoPanel", "点击章节: 卷" + volIdx + " 章" + chapIdx);
                        // 关闭抽屉
                        if (getActivity() != null) {
                            androidx.drawerlayout.widget.DrawerLayout drawerLayout = 
                                getActivity().findViewById(R.id.drawer_layout);
                            if (drawerLayout != null) {
                                drawerLayout.closeDrawer(androidx.core.view.GravityCompat.START);
                            }
                            
                            // 通知切换到写作Tab并定位到该章节
                            notifyNavigateToChapter(volIdx, chapIdx);
                        }
                    });
                    
                    chaptersContainer.addView(chapterRow);
                    
                    // 分割线（除了最后一个章节）
                    if (j < chapters.size() - 1) {
                        View divider = new View(requireContext());
                        divider.setLayoutParams(new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, 1
                        ));
                        divider.setBackgroundColor(getThemeColor(R.color.divider));
                        divider.setPadding(32, 0, 0, 0);
                        chaptersContainer.addView(divider);
                    }
                }
            } else {
                // 没有章节时显示提示
                TextView noChapters = new TextView(requireContext());
                noChapters.setText("暂无章节");
                noChapters.setTextSize(13);
                noChapters.setTextColor(getThemeColor(R.color.text_hint));
                noChapters.setPadding(32, 4, 0, 4);
                chaptersContainer.addView(noChapters);
            }
            
            volumeContainer.addView(chaptersContainer);
            layoutToc.addView(volumeContainer);
            
            // 卷之间的分隔线
            if (i < volumes.size() - 1) {
                View volumeDivider = new View(requireContext());
                volumeDivider.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1
                ));
                volumeDivider.setBackgroundColor(getThemeColor(R.color.divider));
                ((LinearLayout.LayoutParams) volumeDivider.getLayoutParams()).topMargin = 12;
                layoutToc.addView(volumeDivider);
            }
        }
    }
    
    /**
     * 通知导航到指定章节
     */
    private void notifyNavigateToChapter(int volumeIndex, int chapterIndex) {
        android.util.Log.d("StoryInfoPanel", "notifyNavigateToChapter: 卷" + volumeIndex + " 章" + chapterIndex);
        // 通过接口通知父Activity
        if (getActivity() instanceof com.example.storyteller.ui.activity.StoryWorkspaceActivity) {
            android.util.Log.d("StoryInfoPanel", "调用 StoryWorkspaceActivity.navigateToChapter");
            ((com.example.storyteller.ui.activity.StoryWorkspaceActivity) getActivity())
                .navigateToChapter(volumeIndex, chapterIndex);
        } else {
            android.util.Log.e("StoryInfoPanel", "getActivity 不是 StoryWorkspaceActivity 实例");
        }
    }

    /**
     * 保存面板数据（带Toast提示）
     */
    public void savePanelData() {
        savePanelDataInternal(true);
    }

    /**
     * 静默保存面板数据（不显示Toast）
     */
    public void savePanelDataSilently() {
        savePanelDataInternal(false);
    }

    /**
     * 内部保存方法
     * @param showToast 是否显示Toast提示
     */
    private void savePanelDataInternal(boolean showToast) {
        if (currentStory == null) {
            return;
        }

        // 设定面板暂时不保存（需要单独的setting字段）
        // TODO: 后续添加专门的setting字段到Story模型
        // currentStory.setSetting(etWorldSetting.getText().toString().trim());

        // 大纲面板暂时不保存（需要单独的outline字段）
        // TODO: 后续添加专门的outline字段到Story模型
        // currentStory.setOutline(etOutline.getText().toString().trim());

        // 文档面板暂时不保存
        // TODO: 后续添加专门的docs字段到Story模型

        // 注意：目前 StoryInfoPanelFragment 不负责保存任何字段
        // 避免覆盖其他 Fragment 的数据
        /* 暂时禁用，避免覆盖其他Fragment的数据
        int result = storyRepository.updateStory(currentStory);
        if (showToast) {
            if (result > 0) {
                Toast.makeText(requireContext(), "保存成功", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "保存失败", Toast.LENGTH_SHORT).show();
            }
        }
        */
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

        // 保存当前小说数据（静默保存）
        if (currentStory != null) {
            savePanelDataSilently();
            
            // 通知父Activity保存其他Fragment的数据
            if (getActivity() instanceof com.example.storyteller.ui.activity.StoryWorkspaceActivity) {
                ((com.example.storyteller.ui.activity.StoryWorkspaceActivity) getActivity()).saveCurrentWorkSilently();
            }
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
