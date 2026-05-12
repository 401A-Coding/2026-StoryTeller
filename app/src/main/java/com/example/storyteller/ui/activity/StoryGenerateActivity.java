package com.example.storyteller.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.storyteller.R;
import com.example.storyteller.base.BaseActivity;
import com.example.storyteller.data.repository.StoryRepository;
import com.example.storyteller.data.repository.StoryRepositoryImpl;
import com.example.storyteller.data.remote.ApiClient;
import com.example.storyteller.model.Chapter;
import com.example.storyteller.model.Volume;
import com.example.storyteller.model.ChatMessage;
import com.example.storyteller.model.Story;
import com.example.storyteller.ui.adapter.ChatMessageAdapter;
import com.example.storyteller.utils.AgentCommandExecutor;
import com.example.storyteller.utils.JsonUtils;
import com.google.android.material.tabs.TabLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * 故事生成页面
 * 支持卷、章节的创建和编辑，以及AI辅助写作
 */
public class StoryGenerateActivity extends BaseActivity {

    // UI Components
    private ImageView btnStoryInfo;
    private ImageView btnAi;
    private ImageView btnSave;
    private DrawerLayout drawerLayout;
    private LinearLayout panelAi;
    private ImageView btnCloseAi;
    private RecyclerView rvChat;
    private EditText etMessage;
    private ImageButton btnSend;
    private Button btnModeSelector;
    private Button btnModelSelector;
    private ProgressBar progressBar;
    private LinearLayout layoutContent;
    private Button btnAddVolume;

    // Story Info Panel (左侧悬浮栏)
    private LinearLayout panelStoryInfo;
    private ImageView btnCloseStoryInfo;
    private ImageView btnSelectStory;
    private TextView tvStoryTitle;
    private TabLayout tabStoryInfo;
    private EditText etWorldSetting;
    private EditText etOutline;
    private EditText etDocs;
    private LinearLayout layoutToc;
    private View panelSetting;
    private View panelOutline;
    private View panelToc;
    private View panelDocs;


    // AI Chat
    private final List<ChatMessage> messages = new ArrayList<>();
    private ChatMessageAdapter adapter;

    // Data
    private final List<Volume> volumes = new ArrayList<>();
    private int volumeCount = 0;
    private Story currentStory; // 当前正在编辑的故事
    private boolean isEditMode = false; // 是否为编辑模式
    
    // Agent Mode
    private AgentCommandExecutor commandExecutor;
    private String currentMode = "agent"; // 当前选择的模式: agent 或 ask
    private String currentModel = "flash"; // 当前选择的模型: flash 或 pro

    // Storage
    private StoryRepository storyRepository;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_story_generate;
    }

    @Override
    protected void initView() {
        // Toolbar buttons
        btnStoryInfo = findViewById(R.id.btn_story_info);
        btnAi = findViewById(R.id.btn_ai);
        btnSave = findViewById(R.id.btn_save);

        // AI Panel with DrawerLayout
        drawerLayout = findViewById(R.id.drawer_layout);
        panelAi = findViewById(R.id.panel_ai);
        btnCloseAi = findViewById(R.id.btn_close_ai);
        rvChat = findViewById(R.id.rv_chat);
        etMessage = findViewById(R.id.et_message);
        btnSend = findViewById(R.id.btn_send);
        btnModeSelector = findViewById(R.id.btn_mode_selector);
        btnModelSelector = findViewById(R.id.btn_model_selector);

        // Content
        layoutContent = findViewById(R.id.layout_content);
        btnAddVolume = findViewById(R.id.btn_add_volume);
        progressBar = findViewById(R.id.progress_bar);

        // Story Info Panel (左侧悬浮栏)
        panelStoryInfo = findViewById(R.id.panel_story_info);
        btnCloseStoryInfo = findViewById(R.id.btn_close_story_info);
        btnSelectStory = findViewById(R.id.btn_select_story);
        tvStoryTitle = findViewById(R.id.tv_story_title);
        tabStoryInfo = findViewById(R.id.tab_story_info);
        etWorldSetting = findViewById(R.id.et_world_setting);
        etOutline = findViewById(R.id.et_outline);
        etDocs = findViewById(R.id.et_docs);
        layoutToc = findViewById(R.id.layout_toc);
        panelSetting = findViewById(R.id.panel_setting);
        panelOutline = findViewById(R.id.panel_outline);
        panelToc = findViewById(R.id.panel_toc);
        panelDocs = findViewById(R.id.panel_docs);

        // Setup story info button (打开左侧面板)
        btnStoryInfo.setOnClickListener(v -> toggleStoryInfoPanel());

        // Setup close story info button
        btnCloseStoryInfo.setOnClickListener(v -> toggleStoryInfoPanel());

        // Setup select story button
        btnSelectStory.setOnClickListener(v -> showStorySelector());

        // Setup TabLayout for story info panel
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

        // Setup AI panel toggle
        btnAi.setOnClickListener(v -> toggleAiPanel());
        btnCloseAi.setOnClickListener(v -> toggleAiPanel());

        // Setup save button
        btnSave.setOnClickListener(v -> {
            if (isEditMode && currentStory != null) {
                // 编辑模式：更新现有小说
                saveEditedStory();
            } else {
                // 创建模式：创建新小说
                createNewStory();
            }
        });

        // Setup AI chat
        adapter = new ChatMessageAdapter(this, messages);
        rvChat.setLayoutManager(new LinearLayoutManager(this));
        rvChat.setAdapter(adapter);
        btnSend.setOnClickListener(v -> sendMessage());

        // Setup mode selector
        btnModeSelector.setOnClickListener(v -> showModeSelectorPopup());
        
        // Setup model selector
        btnModelSelector.setOnClickListener(v -> showModelSelectorPopup());

        // Setup add volume button
        btnAddVolume.setOnClickListener(v -> addNewVolume());
    }


    /**
     * 显示模式选择弹窗（Agent/Ask）
     */
    private void showModeSelectorPopup() {
        android.widget.PopupMenu popupMenu = new android.widget.PopupMenu(this, btnModeSelector);
        popupMenu.getMenu().add(0, 1, 0, getString(R.string.agent));
        popupMenu.getMenu().add(0, 2, 1, getString(R.string.ask));
        
        // 标记当前选中的模式
        if ("agent".equals(currentMode)) {
            popupMenu.getMenu().getItem(0).setChecked(true);
        } else {
            popupMenu.getMenu().getItem(1).setChecked(true);
        }
        
        popupMenu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == 1) {
                currentMode = "agent";
                btnModeSelector.setText(getString(R.string.agent));
                Toast.makeText(this, "已启用 Agent 模式", Toast.LENGTH_SHORT).show();
                return true;
            } else if (itemId == 2) {
                currentMode = "ask";
                btnModeSelector.setText(getString(R.string.ask));
                Toast.makeText(this, "已切换到 Ask 模式", Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });
        
        popupMenu.show();
    }
    
    /**
     * 显示模型选择弹窗（Flash/Pro）
     */
    private void showModelSelectorPopup() {
        android.widget.PopupMenu popupMenu = new android.widget.PopupMenu(this, btnModelSelector);
        popupMenu.getMenu().add(0, 1, 0, getString(R.string.model_flash));
        popupMenu.getMenu().add(0, 2, 1, getString(R.string.model_pro));
        
        // 标记当前选中的模型
        if ("flash".equals(currentModel)) {
            popupMenu.getMenu().getItem(0).setChecked(true);
        } else {
            popupMenu.getMenu().getItem(1).setChecked(true);
        }
        
        popupMenu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == 1) {
                currentModel = "flash";
                btnModelSelector.setText(getString(R.string.model_flash));
                Toast.makeText(this, "已切换到 Flash 模型", Toast.LENGTH_SHORT).show();
                return true;
            } else if (itemId == 2) {
                currentModel = "pro";
                btnModelSelector.setText(getString(R.string.model_pro));
                Toast.makeText(this, "已切换到 Pro 模型", Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });
        
        popupMenu.show();
    }

    @Override
    protected void initData() {
        // Initialize Repository for data access
        storyRepository = new StoryRepositoryImpl(this);
        
        // Initialize Agent Command Executor with repository
        commandExecutor = new AgentCommandExecutor(storyRepository);
        
        // Check if we're editing an existing story
        Intent intent = getIntent();
        int storyId = intent.getIntExtra("story_id", -1);
        if (storyId > 0) {
            // 编辑模式：加载已有小说
            loadExistingStory(storyId);
        } else {
            // 新建模式：设置默认标题
            tvStoryTitle.setText("新小说");
            
            // 自动创建初始卷和章节
            addNewVolume();
        }
    }

    /**
     * 加载现有故事进行编辑
     */
    private void loadExistingStory(int storyId) {
        currentStory = storyRepository.getStoryById(storyId);
        if (currentStory != null) {
            isEditMode = true;
            
            // 更新标题显示
            tvStoryTitle.setText(currentStory.getTitle());
            
            // Clear existing content
            layoutContent.removeAllViews();
            
            // First, add the "Add Volume" button
            Button btnAddVolumeNew = new Button(this);
            btnAddVolumeNew.setId(R.id.btn_add_volume);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
            params.topMargin = 32;
            params.bottomMargin = 32;
            params.gravity = android.view.Gravity.CENTER;
            btnAddVolumeNew.setLayoutParams(params);
            btnAddVolumeNew.setText(getString(R.string.btn_add_volume));
            btnAddVolumeNew.setTextColor(android.graphics.Color.parseColor("#1976D2"));
            btnAddVolumeNew.setOnClickListener(v -> addNewVolume());
            
            layoutContent.addView(btnAddVolumeNew);
            
            // Then parse and render the story structure
            String structureJson = currentStory.getStructure();
            if (!TextUtils.isEmpty(structureJson)) {
                // Load from JSON structure
                parseStoryStructure(structureJson);
            } else {
                // Fallback: parse from plain text content
                parseStoryContent(currentStory.getContent());
            }
        }
    }

    /**
     * 从JSON解析卷-章结构
     */
    private void parseStoryStructure(String structureJson) {
        try {
            volumes.clear();
            List<Volume> loadedVolumes = JsonUtils.fromJson(structureJson, 
                new com.google.gson.reflect.TypeToken<List<Volume>>(){}.getType());
            
            if (loadedVolumes != null && !loadedVolumes.isEmpty()) {
                volumes.addAll(loadedVolumes);
                
                // Update volume counter
                volumeCount = volumes.size();
                
                // Render all volumes and chapters to UI in order
                for (int i = 0; i < volumes.size(); i++) {
                    Volume volume = volumes.get(i);
                    renderVolumeToUI(volume, i + 1); // Pass the correct volume index
                }
            } else {
                // Fallback to plain text parsing
                parseStoryContent(currentStory.getContent());
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Fallback to plain text parsing
            parseStoryContent(currentStory.getContent());
        }
    }

    /**
     * 将卷渲染到UI
     * @param volume 卷对象
     * @param volumeIndex 卷的序号（从1开始）
     */
    private void renderVolumeToUI(Volume volume, int volumeIndex) {
        // Inflate volume layout
        View volumeView = LayoutInflater.from(this).inflate(R.layout.item_volume, layoutContent, false);

        // Setup volume prefix
        TextView tvVolumePrefix = volumeView.findViewById(R.id.tv_volume_prefix);
        tvVolumePrefix.setText("第" + volumeIndex + "卷 · ");

        // Setup volume name TextView (display mode)
        TextView tvVolumeName = volumeView.findViewById(R.id.tv_volume_name);
        tvVolumeName.setText(volume.getTitle());

        // Setup volume name EditText (edit mode)
        EditText etVolumeName = volumeView.findViewById(R.id.et_volume_name);
        etVolumeName.setText(volume.getTitle());

        // Double tap to edit volume name inline
        setupInlineEdit(tvVolumeName, etVolumeName, volume, false);

        // Chapter container
        LinearLayout layoutChapters = volumeView.findViewById(R.id.layout_chapters_container);

        // Setup add chapter button
        Button btnAddChapter = volumeView.findViewById(R.id.btn_add_chapter);
        btnAddChapter.setOnClickListener(v -> addNewChapter(layoutChapters, volume));
        
        // Setup more button for volume
        ImageView btnMoreVolume = volumeView.findViewById(R.id.btn_more_volume);
        final Volume finalVolume = volume;
        final int finalVolumeIndex = volumeIndex;
        btnMoreVolume.setOnClickListener(v -> showVolumeMenu(finalVolume, finalVolumeIndex, volumeView));

        // Add volume to layout - always append before the last child (btnAddVolume)
        // Find the button and insert before it
        int buttonIndex = -1;
        for (int i = 0; i < layoutContent.getChildCount(); i++) {
            if (layoutContent.getChildAt(i).getId() == R.id.btn_add_volume) {
                buttonIndex = i;
                break;
            }
        }
        
        if (buttonIndex >= 0) {
            // Insert before the button
            layoutContent.addView(volumeView, buttonIndex);
        } else {
            // Button not found, just append
            layoutContent.addView(volumeView);
        }

        // Render all chapters with correct chapter index
        for (int i = 0; i < volume.getChapters().size(); i++) {
            Chapter chapter = volume.getChapters().get(i);
            renderChapterToUI(layoutChapters, volume, chapter, i + 1); // Pass the correct chapter index
        }
    }

    /**
     * 将章节渲染到UI
     * @param chapterContainer 章节容器
     * @param volume 所属卷
     * @param chapter 章节对象
     * @param chapterIndex 章节序号（从1开始，每个卷内独立计数）
     */
    private void renderChapterToUI(ViewGroup chapterContainer, Volume volume, Chapter chapter, int chapterIndex) {
        // Inflate chapter layout
        View chapterView = LayoutInflater.from(this).inflate(R.layout.item_chapter, chapterContainer, false);

        // Setup chapter prefix
        TextView tvChapterPrefix = chapterView.findViewById(R.id.tv_chapter_prefix);
        tvChapterPrefix.setText("第" + chapterIndex + "章 · ");

        // Setup chapter name TextView (display mode)
        TextView tvChapterName = chapterView.findViewById(R.id.tv_chapter_name);
        tvChapterName.setText(chapter.getTitle());

        // Setup chapter name EditText (edit mode)
        EditText etChapterName = chapterView.findViewById(R.id.et_chapter_name);
        etChapterName.setText(chapter.getTitle());

        // Double tap to edit chapter name inline
        setupInlineEdit(tvChapterName, etChapterName, chapter, true);
        
        // Setup more button for chapter
        ImageView btnMoreChapter = chapterView.findViewById(R.id.btn_more_chapter);
        final Volume finalVolume = volume;
        final Chapter finalChapter = chapter;
        final int finalChapterIndex = chapterIndex;
        btnMoreChapter.setOnClickListener(v -> showChapterMenu(finalVolume, finalChapter, finalChapterIndex, chapterView));

        // Setup content editor
        EditText etContent = chapterView.findViewById(R.id.et_chapter_content);
        etContent.setHint("开始写作...");
        if (!TextUtils.isEmpty(chapter.getContent())) {
            etContent.setText(chapter.getContent());
        }
        etContent.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(android.text.Editable s) {
                chapter.setContent(s.toString());
            }
        });

        chapterContainer.addView(chapterView);
    }

    /**
     * 解析故事内容为卷和章节结构
     */
    private void parseStoryContent(String content) {
        // For now, we'll create a simple structure with one volume and one chapter
        // In a real implementation, you might want to parse the content more intelligently
        volumeCount = 0;
        volumes.clear();
        
        // Create a default volume and chapter with the existing content
        addNewVolume();
        
        // Set the content to the first chapter
        if (!volumes.isEmpty() && !volumes.get(0).getChapters().isEmpty()) {
            Chapter firstChapter = volumes.get(0).getChapters().get(0);
            firstChapter.setContent(content);
            
            // Update the EditText in the UI
            // Find the first volume view and its first chapter's content EditText
            if (layoutContent.getChildCount() > 0) {
                View firstVolumeView = layoutContent.getChildAt(0);
                if (firstVolumeView instanceof ViewGroup) {
                    LinearLayout layoutChapters = firstVolumeView.findViewById(R.id.layout_chapters_container);
                    if (layoutChapters != null && layoutChapters.getChildCount() > 0) {
                        View firstChapterView = layoutChapters.getChildAt(0);
                        EditText etContent = firstChapterView.findViewById(R.id.et_chapter_content);
                        if (etContent != null) {
                            etContent.setText(content);
                        }
                    }
                }
            }
        }
    }

    /**
     * 保存编辑后的故事
     */
    private void saveEditedStory() {
        if (currentStory == null) return;
        
        // Build the complete story content from all chapters
        StringBuilder fullContent = new StringBuilder();
        for (Volume volume : volumes) {
            for (Chapter chapter : volume.getChapters()) {
                if (!TextUtils.isEmpty(chapter.getTitle())) {
                    fullContent.append("## ").append(chapter.getTitle()).append("\n\n");
                }
                if (!TextUtils.isEmpty(chapter.getContent())) {
                    fullContent.append(chapter.getContent()).append("\n\n");
                }
            }
        }
        
        // Update the story object
        currentStory.setContent(fullContent.toString().trim());
        
        // Serialize volumes structure to JSON
        String structureJson = JsonUtils.toJson(volumes);
        currentStory.setStructure(structureJson);
        
        // Save to database via repository
        int result = storyRepository.updateStory(currentStory);
        if (result > 0) {
            Toast.makeText(this, "故事保存成功", Toast.LENGTH_SHORT).show();
            // Removed finish() to keep the activity open after saving
        } else {
            Toast.makeText(this, "保存失败", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 创建新小说（非编辑模式）
     */
    private void createNewStory() {
        // Build the complete story content from all chapters
        StringBuilder fullContent = new StringBuilder();
        for (Volume volume : volumes) {
            for (Chapter chapter : volume.getChapters()) {
                if (!TextUtils.isEmpty(chapter.getTitle())) {
                    fullContent.append("## ").append(chapter.getTitle()).append("\n\n");
                }
                if (!TextUtils.isEmpty(chapter.getContent())) {
                    fullContent.append(chapter.getContent()).append("\n\n");
                }
            }
        }
        
        String content = fullContent.toString().trim();
        if (TextUtils.isEmpty(content)) {
            Toast.makeText(this, "请先添加一些内容再保存", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Generate title from first volume and chapter
        String title = "新小说";
        if (!volumes.isEmpty()) {
            Volume firstVolume = volumes.get(0);
            if (!firstVolume.getChapters().isEmpty()) {
                Chapter firstChapter = firstVolume.getChapters().get(0);
                if (!TextUtils.isEmpty(firstChapter.getTitle()) && !"新章节".equals(firstChapter.getTitle())) {
                    title = firstChapter.getTitle();
                } else if (!TextUtils.isEmpty(firstVolume.getTitle()) && !"新卷名".equals(firstVolume.getTitle())) {
                    title = firstVolume.getTitle();
                }
            }
        }
        
        // Create new story
        Story newStory = new Story(title, content, "手动创建", System.currentTimeMillis());
        
        // Serialize volumes structure to JSON
        String structureJson = JsonUtils.toJson(volumes);
        newStory.setStructure(structureJson);
        
        // Save to database via repository
        long id = storyRepository.insertStory(newStory);
        if (id > 0) {
            newStory.setId((int) id);
            Toast.makeText(this, "小说创建成功！", Toast.LENGTH_SHORT).show();
            
            // Set as current editing story
            currentStory = newStory;
            isEditMode = true;
            
            // 更新标题显示
            tvStoryTitle.setText(title);
            
            // Update selection in SharedPreferences
            com.example.storyteller.data.local.prefs.PrefsUtils.getInstance(this)
                .putString(com.example.storyteller.ui.adapter.StoryAdapter.PREF_SELECTED_STORY_ID, String.valueOf(id));
            com.example.storyteller.data.local.prefs.PrefsUtils.getInstance(this)
                .putString(com.example.storyteller.ui.adapter.StoryAdapter.PREF_SELECTED_STORY_TITLE, title);
            
            // Finish and return to home
            finish();
        } else {
            Toast.makeText(this, "创建失败", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 切换小说信息面板显示/隐藏（左侧悬浮栏）
     */
    private void toggleStoryInfoPanel() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            drawerLayout.openDrawer(GravityCompat.START);
        }
    }

    /**
     * 切换小说信息面板中的Tab
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
                refreshTocView();
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
        layoutToc.removeAllViews();
        
        // 添加标题
        TextView titleView = new TextView(this);
        titleView.setText("📚 目录概览");
        titleView.setTextSize(16);
        titleView.setTypeface(null, android.graphics.Typeface.BOLD);
        titleView.setTextColor(0xFF212121);
        titleView.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT));
        ((LinearLayout.LayoutParams) titleView.getLayoutParams()).bottomMargin = 12;
        layoutToc.addView(titleView);

        // 遍历所有卷和章节
        for (int i = 0; i < volumes.size(); i++) {
            Volume volume = volumes.get(i);
            
            // 卷标题
            TextView volumeTitle = new TextView(this);
            volumeTitle.setText("第" + (i + 1) + "卷 · " + volume.getTitle());
            volumeTitle.setTextSize(14);
            volumeTitle.setTypeface(null, android.graphics.Typeface.BOLD);
            volumeTitle.setTextColor(0xFF1976D2);
            volumeTitle.setPadding(0, 8, 0, 4);
            layoutToc.addView(volumeTitle);

            // 章节列表
            for (int j = 0; j < volume.getChapters().size(); j++) {
                Chapter chapter = volume.getChapters().get(j);
                TextView chapterTitle = new TextView(this);
                chapterTitle.setText("    第" + (j + 1) + "章 · " + chapter.getTitle());
                chapterTitle.setTextSize(13);
                chapterTitle.setTextColor(0xFF666666);
                chapterTitle.setPadding(0, 2, 0, 2);
                layoutToc.addView(chapterTitle);
            }
        }
    }

    /**
     * 显示小说选择器
     */
    private void showStorySelector() {
        // 获取所有小说列表
        List<Story> allStories = storyRepository.getAllStories();
        if (allStories == null || allStories.isEmpty()) {
            Toast.makeText(this, "暂无其他小说", Toast.LENGTH_SHORT).show();
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
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
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
    
        // 保存当前小说（如果有）
        if (currentStory != null && isEditMode) {
            saveEditedStory();
        }
    
        // 更新当前小说
        currentStory = story;
        isEditMode = true;
    
        // 更新标题显示
        tvStoryTitle.setText(story.getTitle());
    
        // 重新加载内容
        layoutContent.removeAllViews();
    
        // 重新添加“添加新卷”按钮
        Button btnAddVolumeNew = new Button(this);
        btnAddVolumeNew.setId(R.id.btn_add_volume);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = 32;
        params.bottomMargin = 32;
        params.gravity = android.view.Gravity.CENTER;
        btnAddVolumeNew.setLayoutParams(params);
        btnAddVolumeNew.setText(getString(R.string.btn_add_volume));
        btnAddVolumeNew.setTextColor(android.graphics.Color.parseColor("#1976D2"));
        btnAddVolumeNew.setOnClickListener(v -> addNewVolume());
        layoutContent.addView(btnAddVolumeNew);
    
        // 解析并渲染故事结构
        String structureJson = story.getStructure();
        if (!TextUtils.isEmpty(structureJson)) {
            parseStoryStructure(structureJson);
        } else {
            parseStoryContent(story.getContent());
        }
    
        // 如果当前显示的是目录Tab，自动刷新目录视图
        if (panelToc.getVisibility() == View.VISIBLE) {
            refreshTocView();
        }
    
        Toast.makeText(this, "已切换到《" + story.getTitle() + "》", Toast.LENGTH_SHORT).show();
    }

    /**
     * 切换AI助手面板显示/隐藏
     */
    private void toggleAiPanel() {
        if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END);
        } else {
            drawerLayout.openDrawer(GravityCompat.END);
        }
    }


    /**
     * 添加新卷
     */
    private void addNewVolume() {
        volumeCount++;

        Volume volume = new Volume(volumeCount, "新卷名");
        volumes.add(volume);

        // Inflate volume layout
        View volumeView = LayoutInflater.from(this).inflate(R.layout.item_volume, layoutContent, false);

        // Setup volume prefix (e.g., "第1卷 · ")
        TextView tvVolumePrefix = volumeView.findViewById(R.id.tv_volume_prefix);
        tvVolumePrefix.setText("第" + volumeCount + "卷 · ");

        // Setup volume name TextView (display mode)
        TextView tvVolumeName = volumeView.findViewById(R.id.tv_volume_name);
        tvVolumeName.setText(volume.getTitle());

        // Setup volume name EditText (edit mode)
        EditText etVolumeName = volumeView.findViewById(R.id.et_volume_name);
        etVolumeName.setText(volume.getTitle());

        // Double tap to edit volume name inline
        setupInlineEdit(tvVolumeName, etVolumeName, volume, false);

        // Chapter container
        LinearLayout layoutChapters = volumeView.findViewById(R.id.layout_chapters_container);

        // Setup add chapter button
        Button btnAddChapter = volumeView.findViewById(R.id.btn_add_chapter);
        btnAddChapter.setOnClickListener(v -> addNewChapter(layoutChapters, volume));

        // Add volume to layout (before the add volume button)
        int volumeIndex = layoutContent.getChildCount() - 1; // Insert before btnAddVolume
        layoutContent.addView(volumeView, volumeIndex);

        // Add first chapter automatically (不显示提示)
        addNewChapterAtPosition(layoutChapters, volume, 0, false);
    }

    /**
     * 添加新章节（直接追加到末尾 - 快捷键）
     */
    private void addNewChapter(ViewGroup chapterContainer, Volume volume) {
        // 直接追加到末尾
        int insertIndex = volume.getChapters().size();
        addNewChapterAtPosition(chapterContainer, volume, insertIndex, true);
    }
    
    /**
     * 在指定位置添加新章节
     * @param chapterContainer 章节容器
     * @param volume 所属卷
     * @param insertIndex 插入位置（从 0 开始）
     * @param showToast 是否显示提示（默认true，初始化时为false）
     */
    private void addNewChapterAtPosition(ViewGroup chapterContainer, Volume volume, int insertIndex, boolean showToast) {
        // 验证插入位置
        if (insertIndex < 0 || insertIndex > volume.getChapters().size()) {
            return;
        }
        
        // 创建新章节
        Chapter newChapter = new Chapter(insertIndex + 1, "新章节", "");
        volume.getChapters().add(insertIndex, newChapter);
        
        // 重新编号后续章节
        for (int i = insertIndex; i < volume.getChapters().size(); i++) {
            volume.getChapters().get(i).setId(i + 1);
        }
        
        // 清空容器并重新渲染所有章节
        chapterContainer.removeAllViews();
        for (int i = 0; i < volume.getChapters().size(); i++) {
            Chapter chapter = volume.getChapters().get(i);
            renderChapterToUI(chapterContainer, volume, chapter, i + 1);
        }
        
        // 只在用户主动操作时显示提示
        if (showToast) {
            Toast.makeText(this, "已在第 " + (insertIndex + 1) + " 个位置添加新章节", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 设置内联编辑（双击后在原位置编辑）
     * @param textView 显示模式的 TextView
     * @param editText 编辑模式的 EditText
     * @param model 数据模型（Volume 或 Chapter）
     * @param isChapter 是否为章节标题
     */
    private void setupInlineEdit(TextView textView, EditText editText, Object model, boolean isChapter) {
        // 双击 TextView 切换到编辑模式
        textView.setOnClickListener(v -> {
            textView.setVisibility(View.GONE);
            editText.setVisibility(View.VISIBLE);
            editText.requestFocus();
            // 选中全部文本
            editText.setSelection(editText.getText().length());
            // 显示软键盘
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(editText, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
            }
        });

        // EditText 失去焦点时切换回显示模式
        editText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                finishEditing(editText, textView, model, isChapter);
            }
        });

        // 按回车键完成编辑
        editText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE ||
                actionId == android.view.inputmethod.EditorInfo.IME_NULL) {
                finishEditing(editText, textView, model, isChapter);
                return true;
            }
            return false;
        });
    }

    /**
     * 完成编辑，切换回显示模式
     */
    private void finishEditing(EditText editText, TextView textView, Object model, boolean isChapter) {
        String newName = editText.getText().toString().trim();
        if (TextUtils.isEmpty(newName)) {
            newName = isChapter ? "新章节" : "新卷名";
        }

        // 更新模型
        if (model instanceof Volume) {
            ((Volume) model).setTitle(newName);
        } else if (model instanceof Chapter) {
            ((Chapter) model).setTitle(newName);
        }

        // 切换回显示模式
        editText.setVisibility(View.GONE);
        textView.setText(newName);
        textView.setVisibility(View.VISIBLE);

        // 隐藏软键盘
        android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
        }
    }



    /**
     * 发送消息给AI
     */
    private void sendMessage() {
        String content = etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(content)) {
            return;
        }
        appendMessage(new ChatMessage(content, true));
        etMessage.setText("");

        // Show loading
        progressBar.setVisibility(View.VISIBLE);

        if ("agent".equals(currentMode) && isEditMode && currentStory != null) {
            // Agent mode: process command
            String context = AgentCommandExecutor.buildStoryContext(currentStory, volumes);
            
            ApiClient.getInstance().processAgentCommand(
                content,
                context,
                currentModel,
                this,
                new ApiClient.AgentCallback() {
                    @Override
                    public void onCommandReady(ApiClient.AgentCommand command) {
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            
                            // 使用 AgentCommandExecutor 执行命令
                            AgentCommandExecutor.CommandResult result = 
                                commandExecutor.executeCommand(command, currentStory.getId());
                            
                            // 显示结果消息
                            if (!TextUtils.isEmpty(result.message)) {
                                appendMessage(new ChatMessage(result.message, false));
                            }
                            
                            // 如果执行成功且不是问答操作，刷新 UI
                            if (result.success && !"answer_question".equals(command.action)) {
                                refreshStoryView();
                            }
                        });
                    }
                    
                    @Override
                    public void onFailure(Exception e) {
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            appendMessage(new ChatMessage("抱歉，发生了错误：" + e.getMessage(), false));
                        });
                    }
                }
            );
        } else {
            // Normal chat mode: use original generateStory method
            ApiClient.getInstance().generateStory(content, currentModel, this, new ApiClient.Callback() {
                @Override
                public void onSuccess(String story) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        appendMessage(new ChatMessage(story, false));
                        saveGeneratedStory(content, story);
                    });
                }

                @Override
                public void onFailure(Exception e) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        appendMessage(new ChatMessage("生成失败: " + e.getMessage(), false));
                    });
                }
            });
        }
    }

    /**
     * 添加消息到聊天列表
     */
    private void appendMessage(ChatMessage message) {
        messages.add(message);
        adapter.notifyItemInserted(messages.size() - 1);
        rvChat.scrollToPosition(messages.size() - 1);
    }

    private void saveGeneratedStory(String prompt, String storyContent) {
        // 只在非编辑模式下才创建新故事
        // 在编辑模式下，AI 生成的内容应该通过智能体命令来添加到当前小说
        if (isEditMode) {
            // 编辑模式：不自动创建新故事，只提示用户如何操作
            appendMessage(new ChatMessage("💡 提示：在编辑模式下，请使用智能体模式来添加内容到当前小说。\n开启智能体模式后，可以说'帮我添加一个章节'", false));
            return;
        }
            
        if (storyRepository == null || TextUtils.isEmpty(storyContent)) {
            return;
        }
        String title = buildStoryTitle(prompt, storyContent);
        Story story = new Story(title, storyContent, "AI生成", System.currentTimeMillis());
        long id = storyRepository.insertStory(story);
        if (id > 0) {
            story.setId((int) id);
            appendMessage(new ChatMessage("故事已保存到书架：" + title, false));
        }
    }

    private String buildStoryTitle(String prompt, String storyContent) {
        String base = prompt;
        if (TextUtils.isEmpty(base)) {
            base = storyContent;
        }
        base = base.replaceAll("\\s+", "").trim();
        if (base.length() > 12) {
            base = base.substring(0, 12);
        }
        if (TextUtils.isEmpty(base)) {
            base = "AI生成故事";
        }
        return base;
    }

    /**
     * 刷新故事视图（在执行智能体命令后调用）
     */
    private void refreshStoryView() {
        // Reload story data from database
        if (currentStory != null) {
            currentStory = storyRepository.getStoryById(currentStory.getId());
        }
        
        // Clear and rebuild the content view
        layoutContent.removeAllViews();
        
        // Re-add the "Add Volume" button
        Button btnAddVolumeNew = new Button(this);
        btnAddVolumeNew.setId(R.id.btn_add_volume);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = 32;
        params.bottomMargin = 32;
        params.gravity = android.view.Gravity.CENTER;
        btnAddVolumeNew.setLayoutParams(params);
        btnAddVolumeNew.setText(getString(R.string.btn_add_volume));
        btnAddVolumeNew.setTextColor(android.graphics.Color.parseColor("#1976D2"));
        btnAddVolumeNew.setOnClickListener(v -> addNewVolume());
        
        layoutContent.addView(btnAddVolumeNew);
        
        // Parse and render the story structure
        if (currentStory != null) {
            String structureJson = currentStory.getStructure();
            if (!TextUtils.isEmpty(structureJson)) {
                parseStoryStructure(structureJson);
            } else {
                parseStoryContent(currentStory.getContent());
            }
        }
        
        Toast.makeText(this, "已更新小说内容", Toast.LENGTH_SHORT).show();
    }
    
    /**
     * 显示卷的更多操作菜单
     */
    private void showVolumeMenu(Volume volume, int volumeIndex, View anchorView) {
        android.widget.PopupMenu popupMenu = new android.widget.PopupMenu(this, anchorView);
        popupMenu.getMenu().add("重命名");
        popupMenu.getMenu().add("删除");
        
        popupMenu.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();
            if ("重命名".equals(title)) {
                // 触发编辑模式
                View volumeView = findVolumeViewByIndex(volumeIndex);
                if (volumeView != null) {
                    TextView tvVolumeName = volumeView.findViewById(R.id.tv_volume_name);
                    EditText etVolumeName = volumeView.findViewById(R.id.et_volume_name);
                    if (tvVolumeName != null && etVolumeName != null) {
                        tvVolumeName.performClick();
                    }
                }
                return true;
            } else if ("删除".equals(title)) {
                // 确认删除
                new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("确认删除")
                    .setMessage("确定要删除第" + volumeIndex + "卷《" + volume.getTitle() + "》吗？\n该卷包含 " + volume.getChapters().size() + " 章。")
                    .setPositiveButton("删除", (dialog, which) -> {
                        deleteVolume(volumeIndex);
                    })
                    .setNegativeButton("取消", null)
                    .show();
                return true;
            }
            return false;
        });
        
        popupMenu.show();
    }
    
    /**
     * 显示章节的更多操作菜单
     */
    private void showChapterMenu(Volume volume, Chapter chapter, int chapterIndex, View anchorView) {
        android.widget.PopupMenu popupMenu = new android.widget.PopupMenu(this, anchorView);
        popupMenu.getMenu().add("重命名");
        popupMenu.getMenu().add("在上方添加章节");
        popupMenu.getMenu().add("在下方添加章节");
        popupMenu.getMenu().add("删除");
        
        popupMenu.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();
            if ("重命名".equals(title)) {
                // 触发编辑模式
                TextView tvChapterName = anchorView.findViewById(R.id.tv_chapter_name);
                EditText etChapterName = anchorView.findViewById(R.id.et_chapter_name);
                if (tvChapterName != null && etChapterName != null) {
                    tvChapterName.performClick();
                }
                return true;
            } else if ("在上方添加章节".equals(title)) {
                // 在当前章节上方添加
                ViewGroup chapterContainer = (ViewGroup) anchorView.getParent();
                int insertIndex = chapterIndex - 1; // chapterIndex 从 1 开始，转换为从 0 开始
                addNewChapterAtPosition(chapterContainer, volume, insertIndex, true);
                return true;
            } else if ("在下方添加章节".equals(title)) {
                // 在当前章节下方添加
                ViewGroup chapterContainer = (ViewGroup) anchorView.getParent();
                int insertIndex = chapterIndex; // 在当前章节之后
                addNewChapterAtPosition(chapterContainer, volume, insertIndex, true);
                return true;
            } else if ("删除".equals(title)) {
                // 确认删除
                new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("确认删除")
                    .setMessage("确定要删除第" + chapterIndex + "章《" + chapter.getTitle() + "》吗？")
                    .setPositiveButton("删除", (dialog, which) -> {
                        deleteChapter(volume, chapterIndex);
                    })
                    .setNegativeButton("取消", null)
                    .show();
                return true;
            }
            return false;
        });
        
        popupMenu.show();
    }
    
    /**
     * 根据索引查找卷视图
     */
    private View findVolumeViewByIndex(int volumeIndex) {
        if (volumeIndex < 0 || volumeIndex >= layoutContent.getChildCount()) {
            return null;
        }
        return layoutContent.getChildAt(volumeIndex);
    }
    
    /**
     * 删除卷
     */
    private void deleteVolume(int volumeIndex) {
        if (volumes.size() <= 1) {
            Toast.makeText(this, "至少需要保留一个卷", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // volumeIndex 是从 1 开始的显示序号，需要转换为从 0 开始的 List 索引
        int listIndex = volumeIndex - 1;
        
        if (listIndex < 0 || listIndex >= volumes.size()) {
            return;
        }
        
        Volume removedVolume = volumes.remove(listIndex);
        
        // 重新编号
        for (int i = 0; i < volumes.size(); i++) {
            volumes.get(i).setId(i + 1);
        }
        
        // 保存并刷新
        saveEditedStory();
        refreshStoryView();
        
        Toast.makeText(this, "已删除卷：《" + removedVolume.getTitle() + "》", Toast.LENGTH_SHORT).show();
    }
    
    /**
     * 删除章节
     */
    private void deleteChapter(Volume volume, int chapterIndex) {
        // chapterIndex 是从 1 开始的显示序号，需要转换为从 0 开始的 List 索引
        int listIndex = chapterIndex - 1;
        
        if (listIndex < 0 || listIndex >= volume.getChapters().size()) {
            return;
        }
        
        Chapter removedChapter = volume.getChapters().remove(listIndex);
        
        // 重新编号章节
        for (int i = 0; i < volume.getChapters().size(); i++) {
            volume.getChapters().get(i).setId(i + 1);
        }
        
        // 保存并刷新
        saveEditedStory();
        refreshStoryView();
        
        Toast.makeText(this, "已删除章节：《" + removedChapter.getTitle() + "》", Toast.LENGTH_SHORT).show();
    }
}
