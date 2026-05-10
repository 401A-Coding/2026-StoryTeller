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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.cardview.widget.CardView;
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
    private CardView panelAi;
    private ImageView btnCloseAi;
    private RecyclerView rvChat;
    private EditText etMessage;
    private Button btnSend;
    private ProgressBar progressBar;
    private LinearLayout layoutContent;
    private Button btnAddVolume;

    // AI Chat
    private final List<ChatMessage> messages = new ArrayList<>();
    private ChatMessageAdapter adapter;

    // Data
    private final List<Volume> volumes = new ArrayList<>();
    private int volumeCount = 0;
    private int chapterCount = 0;

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

        // AI Panel
        panelAi = findViewById(R.id.panel_ai);
        btnCloseAi = findViewById(R.id.btn_close_ai);
        rvChat = findViewById(R.id.rv_chat);
        etMessage = findViewById(R.id.et_message);
        btnSend = findViewById(R.id.btn_send);

        // Content
        layoutContent = findViewById(R.id.layout_content);
        btnAddVolume = findViewById(R.id.btn_add_volume);
        progressBar = findViewById(R.id.progress_bar);

        // Setup back button
        btnBack.setOnClickListener(v -> finish());

        // Setup AI panel toggle
        btnAi.setOnClickListener(v -> toggleAiPanel());
        btnCloseAi.setOnClickListener(v -> toggleAiPanel());

        // Setup save button (placeholder)
        btnSave.setOnClickListener(v -> {
            Toast.makeText(this, "保存功能暂未开放", Toast.LENGTH_SHORT).show();
        });

        // Setup AI chat
        adapter = new ChatMessageAdapter(this, messages);
        rvChat.setLayoutManager(new LinearLayoutManager(this));
        rvChat.setAdapter(adapter);
        btnSend.setOnClickListener(v -> sendMessage());

        // Setup add volume button
        btnAddVolume.setOnClickListener(v -> addNewVolume());

        // Add initial volume and chapter
        addNewVolume();
    }

    @Override
    protected void initData() {
        // Data will be initialized in initView
    }

    /**
     * 切换AI助手面板显示/隐藏
     */
    private void toggleAiPanel() {
        if (panelAi.getVisibility() == View.VISIBLE) {
            panelAi.setVisibility(View.GONE);
        } else {
            panelAi.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 添加新卷
     */
    private void addNewVolume() {
        volumeCount++;
        chapterCount = 0;

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
        chapterCount++;

        Chapter chapter = new Chapter(chapterCount, "新章节", "");
        volume.addChapter(chapter);

        // Inflate chapter layout
        View chapterView = LayoutInflater.from(this).inflate(R.layout.item_chapter, chapterContainer, false);

        // Setup chapter prefix (e.g., "第1章 · ")
        TextView tvChapterPrefix = chapterView.findViewById(R.id.tv_chapter_prefix);
        tvChapterPrefix.setText("第" + chapterCount + "章 · ");

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

        // Call AI to generate story
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

    /**
     * 添加消息到聊天列表
     */
    private void appendMessage(ChatMessage message) {
        messages.add(message);
        adapter.notifyItemInserted(messages.size() - 1);
        rvChat.scrollToPosition(messages.size() - 1);
    }

    private void saveGeneratedStory(String prompt, String storyContent) {
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
}