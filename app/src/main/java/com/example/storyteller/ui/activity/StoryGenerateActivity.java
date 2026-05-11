package com.example.storyteller.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.cardview.widget.CardView;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.storyteller.R;
import com.example.storyteller.base.BaseActivity;
import com.example.storyteller.data.local.db.StoryDao;
import com.example.storyteller.data.remote.ApiClient;
import com.example.storyteller.model.Chapter;
import com.example.storyteller.model.Volume;
import com.example.storyteller.model.ChatMessage;
import com.example.storyteller.model.Story;
import com.example.storyteller.ui.adapter.ChatMessageAdapter;
import com.example.storyteller.utils.AgentCommandExecutor;
import com.example.storyteller.utils.JsonUtils;
import java.util.ArrayList;
import java.util.List;

/**
 * 故事生成页面
 * 支持卷、章节的创建和编辑，以及AI辅助写作
 */
public class StoryGenerateActivity extends BaseActivity {

    // UI Components
    private ImageView btnBack;
    private ImageView btnAi;
    private ImageView btnSave;
    private DrawerLayout drawerLayout;
    private LinearLayout panelAi;
    private ImageView btnCloseAi;
    private RecyclerView rvChat;
    private EditText etMessage;
    private ImageButton btnSend;
    private Button btnAgentMode;
    private Button btnAskMode;
    private ProgressBar progressBar;
    private LinearLayout layoutContent;
    private Button btnAddVolume;

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
    private boolean isAgentMode = false; // 是否启用智能体模式

    // Storage
    private StoryDao storyDao;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_story_generate;
    }

    @Override
    protected void initView() {
        // Toolbar buttons
        btnBack = findViewById(R.id.btn_back);
        btnAi = findViewById(R.id.btn_ai);
        btnSave = findViewById(R.id.btn_save);

        // AI Panel with DrawerLayout
        drawerLayout = findViewById(R.id.drawer_layout);
        panelAi = findViewById(R.id.panel_ai);
        btnCloseAi = findViewById(R.id.btn_close_ai);
        rvChat = findViewById(R.id.rv_chat);
        etMessage = findViewById(R.id.et_message);
        btnSend = findViewById(R.id.btn_send);
        btnAgentMode = findViewById(R.id.btn_agent_mode);
        btnAskMode = findViewById(R.id.btn_ask_mode);

        // Content
        layoutContent = findViewById(R.id.layout_content);
        btnAddVolume = findViewById(R.id.btn_add_volume);
        progressBar = findViewById(R.id.progress_bar);

        // Setup back button
        btnBack.setOnClickListener(v -> finish());

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

        // Setup agent mode buttons
        updateAgentModeButtons(); // Initialize button states
        
        btnAgentMode.setOnClickListener(v -> {
            isAgentMode = true;
            updateAgentModeButtons();
            Toast.makeText(this, "已启用 Agent 模式", Toast.LENGTH_SHORT).show();
        });
        
        btnAskMode.setOnClickListener(v -> {
            isAgentMode = false;
            updateAgentModeButtons();
            Toast.makeText(this, "已切换到 Ask 模式", Toast.LENGTH_SHORT).show();
        });

        // Setup add volume button
        btnAddVolume.setOnClickListener(v -> addNewVolume());

        // Add initial volume and chapter
        addNewVolume();
    }

    /**
     * 更新 Agent 模式按钮的选中状态
     */
    private void updateAgentModeButtons() {
        if (isAgentMode) {
            // Agent 模式选中
            btnAgentMode.setTextColor(getResources().getColor(R.color.colorPrimary, null));
            btnAgentMode.setBackgroundResource(R.drawable.bg_chat_bubble_user);
            btnAskMode.setTextColor(getResources().getColor(R.color.colorGray, null));
            btnAskMode.setBackground(null);
        } else {
            // Ask 模式选中
            btnAskMode.setTextColor(getResources().getColor(R.color.colorPrimary, null));
            btnAskMode.setBackgroundResource(R.drawable.bg_chat_bubble_user);
            btnAgentMode.setTextColor(getResources().getColor(R.color.colorGray, null));
            btnAgentMode.setBackground(null);
        }
    }

    @Override
    protected void initData() {
        // Data will be initialized in initView
        // Initialize DAO for saving generated stories
        storyDao = new StoryDao(this);
        
        // Initialize Agent Command Executor
        commandExecutor = new AgentCommandExecutor(this);
        
        // Check if we're editing an existing story
        Intent intent = getIntent();
        int storyId = intent.getIntExtra("story_id", -1);
        if (storyId > 0) {
            loadExistingStory(storyId);
        }
    }

    /**
     * 加载现有故事进行编辑
     */
    private void loadExistingStory(int storyId) {
        currentStory = storyDao.getStoryById(storyId);
        if (currentStory != null) {
            isEditMode = true;
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
        
        // Save to database
        int result = storyDao.updateStory(currentStory);
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
        
        // Save to database
        long id = storyDao.insertStory(newStory);
        if (id > 0) {
            newStory.setId((int) id);
            Toast.makeText(this, "小说创建成功！", Toast.LENGTH_SHORT).show();
            
            // Set as current editing story
            currentStory = newStory;
            isEditMode = true;
            
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
     * 切换AI助手面板显示/隐藏
     */
    private void toggleAiPanel() {
        if (drawerLayout.isDrawerOpen(Gravity.END)) {
            drawerLayout.closeDrawer(Gravity.END);
        } else {
            drawerLayout.openDrawer(Gravity.END);
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

        // Add first chapter automatically
        addNewChapter(layoutChapters, volume);
    }

    /**
     * 添加新章节
     */
    private void addNewChapter(ViewGroup chapterContainer, Volume volume) {
        // Calculate the new chapter index based on existing chapters in this volume
        int newChapterIndex = volume.getChapters().size() + 1;

        Chapter chapter = new Chapter(newChapterIndex, "新章节", "");
        volume.addChapter(chapter);

        // Inflate chapter layout
        View chapterView = LayoutInflater.from(this).inflate(R.layout.item_chapter, chapterContainer, false);

        // Setup chapter prefix (e.g., "第1章 · ")
        TextView tvChapterPrefix = chapterView.findViewById(R.id.tv_chapter_prefix);
        tvChapterPrefix.setText("第" + newChapterIndex + "章 · ");

        // Setup chapter name TextView (display mode)
        TextView tvChapterName = chapterView.findViewById(R.id.tv_chapter_name);
        tvChapterName.setText(chapter.getTitle());

        // Setup chapter name EditText (edit mode)
        EditText etChapterName = chapterView.findViewById(R.id.et_chapter_name);
        etChapterName.setText(chapter.getTitle());

        // Double tap to edit chapter name inline
        setupInlineEdit(tvChapterName, etChapterName, chapter, true);

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

        if (isAgentMode && isEditMode && currentStory != null) {
            // Agent mode: process command
            String context = AgentCommandExecutor.buildStoryContext(currentStory, volumes);
            
            ApiClient.getInstance().processAgentCommand(
                content,
                context,
                this,
                new ApiClient.AgentCallback() {
                    @Override
                    public void onCommandReady(ApiClient.AgentCommand command) {
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            
                            // 检查是否是添加章节命令
                            if ("add_chapter".equals(command.action)) {
                                // 解析参数并执行添加
                                AgentCommandExecutor.AddChapterParams params = 
                                    AgentCommandExecutor.parseAddChapterParams(command.parameters);
                                
                                // 在 Activity 中执行添加逻辑
                                executeAddChapter(params);
                                
                                appendMessage(new ChatMessage("✅ 已成功添加章节：《" + params.chapterTitle + "》", false));
                            } else if ("add_volume".equals(command.action)) {
                                // 解析参数并执行添加卷
                                AgentCommandExecutor.AddVolumeParams params = 
                                    AgentCommandExecutor.parseAddVolumeParams(command.parameters);
                                
                                // 在 Activity 中执行添加卷逻辑
                                executeAddVolume(params);
                                
                                appendMessage(new ChatMessage("✅ 已成功添加卷：《" + params.volumeTitle + "》", false));
                            } else if ("edit_chapter".equals(command.action)) {
                                // 解析参数并执行编辑章节
                                AgentCommandExecutor.EditChapterParams params = 
                                    AgentCommandExecutor.parseEditChapterParams(command.parameters);
                                
                                // 在 Activity 中执行编辑逻辑
                                String result = executeEditChapter(params);
                                
                                appendMessage(new ChatMessage(result, false));
                            } else {
                                // 其他命令由 Executor 处理
                                String result = commandExecutor.executeCommand(command, currentStory.getId());
                                
                                if (!TextUtils.isEmpty(result)) {
                                    appendMessage(new ChatMessage(result, false));
                                }
                                
                                // 如果执行了操作，刷新 UI
                                if (!"answer_question".equals(command.action)) {
                                    refreshStoryView();
                                }
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
            ApiClient.getInstance().generateStory(content, this, new ApiClient.Callback() {
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
            appendMessage(new ChatMessage("💡 提示：在编辑模式下，请使用智能体模式来添加内容到当前小说。\n开启智能体模式后，可以说“帮我添加一个章节”", false));
            return;
        }
        
        if (storyDao == null || TextUtils.isEmpty(storyContent)) {
            return;
        }
        String title = buildStoryTitle(prompt, storyContent);
        Story story = new Story(title, storyContent, "AI生成", System.currentTimeMillis());
        long id = storyDao.insertStory(story);
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
            currentStory = storyDao.getStoryById(currentStory.getId());
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
     * 执行添加章节操作（由智能体命令触发）
     */
    private void executeAddChapter(AgentCommandExecutor.AddChapterParams chapterParams) {
        if (volumes.isEmpty()) {
            // 如果没有卷，先创建一个
            addNewVolume();
        }
        
        // 获取目标卷（默认使用最后一个卷）
        int targetVolumeIndex = Math.max(0, volumes.size() - 1);
        Volume targetVolume = volumes.get(targetVolumeIndex);
        
        // 找到对应的 UI 容器
        if (layoutContent.getChildCount() > targetVolumeIndex) {
            View volumeView = layoutContent.getChildAt(targetVolumeIndex);
            if (volumeView instanceof ViewGroup) {
                LinearLayout layoutChapters = volumeView.findViewById(R.id.layout_chapters_container);
                if (layoutChapters != null) {
                    // 在 UI 中添加新章节
                    addNewChapter(layoutChapters, targetVolume);
                    
                    // 设置章节标题和内容
                    Chapter newChapter = targetVolume.getChapters().get(targetVolume.getChapters().size() - 1);
                    newChapter.setTitle(chapterParams.chapterTitle);
                    newChapter.setContent(chapterParams.chapterContent);
                    
                    // 更新 UI
                    TextView tvChapterName = layoutChapters.getChildAt(layoutChapters.getChildCount() - 1)
                        .findViewById(R.id.tv_chapter_name);
                    EditText etChapterName = layoutChapters.getChildAt(layoutChapters.getChildCount() - 1)
                        .findViewById(R.id.et_chapter_name);
                    EditText etContent = layoutChapters.getChildAt(layoutChapters.getChildCount() - 1)
                        .findViewById(R.id.et_chapter_content);
                    
                    if (tvChapterName != null) {
                        tvChapterName.setText(chapterParams.chapterTitle);
                    }
                    if (etChapterName != null) {
                        etChapterName.setText(chapterParams.chapterTitle);
                    }
                    if (etContent != null) {
                        etContent.setText(chapterParams.chapterContent);
                    }
                }
            }
        }
        
        // 保存到数据库
        saveEditedStory();
    }

    /**
     * 执行添加卷操作（由智能体命令触发）
     */
    private void executeAddVolume(AgentCommandExecutor.AddVolumeParams volumeParams) {
        // 调用现有的 addNewVolume 方法创建新卷
        addNewVolume();
        
        // 获取刚创建的卷（最后一个）
        if (!volumes.isEmpty()) {
            Volume newVolume = volumes.get(volumes.size() - 1);
            
            // 设置卷标题
            newVolume.setTitle(volumeParams.volumeTitle);
            
            // 找到对应的 UI 并更新
            int volumeIndex = volumes.size() - 1;
            if (layoutContent.getChildCount() > volumeIndex) {
                View volumeView = layoutContent.getChildAt(volumeIndex);
                if (volumeView != null) {
                    TextView tvVolumeName = volumeView.findViewById(R.id.tv_volume_name);
                    EditText etVolumeName = volumeView.findViewById(R.id.et_volume_name);
                    
                    if (tvVolumeName != null) {
                        tvVolumeName.setText(volumeParams.volumeTitle);
                    }
                    if (etVolumeName != null) {
                        etVolumeName.setText(volumeParams.volumeTitle);
                    }
                }
            }
        }
        
        // 保存到数据库
        saveEditedStory();
    }

    /**
     * 执行编辑章节操作（由智能体命令触发）
     */
    private String executeEditChapter(AgentCommandExecutor.EditChapterParams params) {
        // 如果没有指定卷ID或章节ID，使用默认值（最后一个卷的最后一章）
        if (params.volumeId < 1 || params.volumeId > volumes.size()) {
            params.volumeId = volumes.size();  // 默认最后一个卷
        }
        
        if (params.volumeId < 1) {
            return "❌ 错误：小说中没有卷";
        }
        
        Volume targetVolume = volumes.get(params.volumeId - 1);
        
        if (params.chapterId < 1 || params.chapterId > targetVolume.getChapters().size()) {
            params.chapterId = targetVolume.getChapters().size();  // 默认最后一章
        }
        
        if (params.chapterId < 1) {
            return "❌ 错误：第" + params.volumeId + "卷中没有章节";
        }
        
        Chapter targetChapter = targetVolume.getChapters().get(params.chapterId - 1);
        
        // 验证新内容
        if (TextUtils.isEmpty(params.newContent)) {
            return "❌ 错误：AI 没有生成新内容，请重试";
        }
        
        // 根据编辑类型执行不同操作
        switch (params.editType) {
            case "rewrite":
                // 重写：完全替换内容
                targetChapter.setContent(params.newContent);
                break;
                
            case "append":
                // 续写：追加到末尾
                String currentContent = targetChapter.getContent();
                if (TextUtils.isEmpty(currentContent)) {
                    targetChapter.setContent(params.newContent);
                } else {
                    targetChapter.setContent(currentContent + "\n\n" + params.newContent);
                }
                break;
                
            case "modify":
                // 修改：暂时当作重写处理（未来可以实现更智能的局部修改）
                targetChapter.setContent(params.newContent);
                break;
                
            default:
                return "❌ 错误：未知的编辑类型";
        }
        
        // 更新 UI
        updateChapterUI(targetVolume, targetChapter, params.chapterId - 1);
        
        // 保存到数据库
        saveEditedStory();
        
        return "✅ 已成功" + getEditTypeDescription(params.editType) + "第" + params.volumeId + "卷第" + params.chapterId + "章";
    }

    /**
     * 更新章节 UI
     */
    private void updateChapterUI(Volume volume, Chapter chapter, int chapterIndex) {
        // 找到对应的卷视图
        int volumeIndex = volumes.indexOf(volume);
        if (volumeIndex >= 0 && volumeIndex < layoutContent.getChildCount()) {
            View volumeView = layoutContent.getChildAt(volumeIndex);
            if (volumeView instanceof ViewGroup) {
                LinearLayout layoutChapters = volumeView.findViewById(R.id.layout_chapters_container);
                if (layoutChapters != null && chapterIndex < layoutChapters.getChildCount()) {
                    View chapterView = layoutChapters.getChildAt(chapterIndex);
                    
                    // 更新内容编辑器
                    EditText etContent = chapterView.findViewById(R.id.et_chapter_content);
                    if (etContent != null) {
                        etContent.setText(chapter.getContent());
                    }
                }
            }
        }
    }

    /**
     * 获取编辑类型的中文描述
     */
    private String getEditTypeDescription(String editType) {
        switch (editType) {
            case "rewrite":
                return "重写";
            case "append":
                return "续写";
            case "modify":
                return "修改";
            default:
                return "编辑";
        }
    }
}