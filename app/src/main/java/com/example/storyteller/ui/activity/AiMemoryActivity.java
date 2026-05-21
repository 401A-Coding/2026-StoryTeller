package com.example.storyteller.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.storyteller.R;
import com.example.storyteller.base.BaseActivity;
import com.example.storyteller.data.local.db.CharacterDao;
import com.example.storyteller.data.local.db.StorySettingDao;
import com.example.storyteller.data.remote.ApiClient;
import com.example.storyteller.data.repository.StoryRepository;
import com.example.storyteller.data.repository.StoryRepositoryImpl;
import com.example.storyteller.model.AiMemory;
import com.example.storyteller.model.Story;
import com.example.storyteller.ui.adapter.AiMemoryAdapter;
import com.example.storyteller.ui.dialog.MemoryExtractionDialog;
import com.example.storyteller.utils.AiMemoryManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * AI记忆管理Activity
 * 提供独立的记忆管理页面，支持查看、删除、提取记忆
 */
public class AiMemoryActivity extends BaseActivity {

    public static final String EXTRA_STORY_ID = "story_id";
    public static final String EXTRA_STORY_TITLE = "story_title";

    private static final String[] MEMORY_TYPES = {
        AiMemory.TYPE_PLOT,      // 剧情类
        AiMemory.TYPE_PERSONALITY, // 人设类
        AiMemory.TYPE_WORLD,     // 世界观类
        AiMemory.TYPE_OTHER      // 其他类
    };

    private static final String[] TYPE_LABELS = {
        "全部", "剧情类", "人设类", "世界观类", "其他类"
    };

    private TextView tvTitle;
    private TextView tvMemoryCount;
    private TextView tvEmpty;
    private RecyclerView rvMemories;
    private MaterialButton btnExtract;
    private MaterialButton btnClear;
    private View layoutBottomActions;
    private ChipGroup chipGroupMainCategory;
    private ChipGroup chipGroupSubCategory;
    private View layoutSubCategory;

    private AiMemoryManager memoryManager;
    private AiMemoryAdapter adapter;
    private ApiClient apiClient;
    private int storyId;
    private String storyTitle;
    private List<AiMemory> allMemories = new ArrayList<>();
    private String currentFilterType = null; // null表示全部
    private String currentModel = "flash"; // 默认使用flash模型

    @Override
    protected int getLayoutId() {
        return R.layout.activity_ai_memory;
    }

    @Override
    protected void initView() {
        // 刘海屏适配
        applySystemWindowInsets(findViewById(android.R.id.content));

        tvTitle = findViewById(R.id.tv_title);
        tvMemoryCount = findViewById(R.id.tv_memory_count);
        tvEmpty = findViewById(R.id.tv_empty);
        rvMemories = findViewById(R.id.rv_memories);
        btnExtract = findViewById(R.id.btn_extract);
        btnClear = findViewById(R.id.btn_clear);
        layoutBottomActions = findViewById(R.id.layout_bottom_actions);
        chipGroupMainCategory = findViewById(R.id.chip_group_main_category);
        chipGroupSubCategory = findViewById(R.id.chip_group_sub_category);
        layoutSubCategory = findViewById(R.id.layout_sub_category);

        // 返回按钮
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        // 提取对话记忆按钮
        btnExtract.setOnClickListener(v -> extractMemoriesFromConversation());

        // 清空按钮
        btnClear.setOnClickListener(v -> showClearConfirmDialog());

        // 设置RecyclerView
        rvMemories.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AiMemoryAdapter();
        adapter.setOnMemoryDeleteListener(this::deleteMemory);
        rvMemories.setAdapter(adapter);

        // 设置分类筛选器
        setupCategoryFilter();
    }

    @Override
    protected void initData() {
        memoryManager = AiMemoryManager.getInstance(this);
        apiClient = ApiClient.getInstance();

        // 获取参数
        Intent intent = getIntent();
        if (intent != null) {
            storyId = intent.getIntExtra(EXTRA_STORY_ID, -1);
            storyTitle = intent.getStringExtra(EXTRA_STORY_TITLE);
        }

        // 更新标题
        if (storyTitle != null && !storyTitle.isEmpty()) {
            tvTitle.setText("《" + storyTitle + "》AI记忆");
        }

        // 加载记忆
        loadMemories();
    }

    /**
     * 加载记忆列表
     */
    private void loadMemories() {
        Map<String, List<AiMemory>> grouped = memoryManager.getMemoriesGroupedByType(storyId);

        allMemories.clear();
        for (String type : MEMORY_TYPES) {
            List<AiMemory> memories = grouped.get(type);
            if (memories != null) {
                allMemories.addAll(memories);
            }
        }

        applyFilter();
    }

    /**
     * 应用筛选
     */
    private void applyFilter() {
        List<AiMemory> filtered;

        if (currentFilterType == null) {
            // 显示全部
            filtered = new ArrayList<>(allMemories);
        } else {
            // 按类型筛选
            filtered = new ArrayList<>();
            for (AiMemory memory : allMemories) {
                if (currentFilterType.equals(memory.getMemoryType())) {
                    filtered.add(memory);
                }
            }
        }

        adapter.setMemories(filtered);
        updateUI(filtered.size());
    }

    /**
     * 更新UI状态
     */
    private void updateUI(int count) {
        tvMemoryCount.setText("共 " + count + " 条记忆");

        if (count == 0) {
            tvEmpty.setVisibility(View.VISIBLE);
            layoutBottomActions.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            layoutBottomActions.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 设置分类筛选器
     */
    private void setupCategoryFilter() {
        // 添加主分类Chip
        for (int i = 0; i < TYPE_LABELS.length; i++) {
            addCategoryChip(TYPE_LABELS[i], i == 0); // 第一个默认选中（全部）
        }
    }

    /**
     * 添加分类Chip
     */
    private void addCategoryChip(String label, boolean isDefault) {
        Chip chip = new Chip(this);
        chip.setText(label);
        chip.setCheckable(true);
        chip.setChecked(isDefault);
        chip.setCloseIconVisible(false);

        chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // 取消其他Chip的选中状态
                for (int i = 0; i < chipGroupMainCategory.getChildCount(); i++) {
                    View child = chipGroupMainCategory.getChildAt(i);
                    if (child instanceof Chip && child != chip) {
                        ((Chip) child).setChecked(false);
                    }
                }

                // 应用筛选
                if ("全部".equals(label)) {
                    currentFilterType = null;
                } else {
                    // 根据标签找到对应的类型
                    int index = -1;
                    for (int i = 1; i < TYPE_LABELS.length; i++) {
                        if (TYPE_LABELS[i].equals(label)) {
                            index = i - 1;
                            break;
                        }
                    }
                    if (index >= 0 && index < MEMORY_TYPES.length) {
                        currentFilterType = MEMORY_TYPES[index];
                    }
                }

                applyFilter();
            }
        });

        chipGroupMainCategory.addView(chip);
    }

    /**
     * 删除记忆
     */
    private void deleteMemory(AiMemory memory) {
        new AlertDialog.Builder(this)
            .setTitle("删除记忆")
            .setMessage("确定要删除这条记忆吗？\n\n" + memory.getTitle())
            .setPositiveButton("删除", (dialog, which) -> {
                boolean success = memoryManager.deleteMemory(memory.getId());
                if (success) {
                    Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show();
                    loadMemories();
                } else {
                    Toast.makeText(this, "删除失败", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    /**
     * 显示清空确认对话框
     */
    private void showClearConfirmDialog() {
        new AlertDialog.Builder(this)
            .setTitle("清空记忆")
            .setMessage("确定要清空当前小说的所有AI记忆吗？\n全局记忆不会被删除。")
            .setPositiveButton("清空", (dialog, which) -> {
                int count = memoryManager.clearStoryMemories(storyId);
                Toast.makeText(this, "已清空 " + count + " 条记忆", Toast.LENGTH_SHORT).show();
                loadMemories();
            })
            .setNegativeButton("取消", null)
            .show();
    }

    /**
     * 从对话中提取记忆
     */
    private void extractMemoriesFromConversation() {
        if (storyId <= 0) {
            Toast.makeText(this, "无效的小说ID", Toast.LENGTH_SHORT).show();
            return;
        }

        // 显示加载对话框
        android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(this);
        progressDialog.setMessage("正在分析对话和小说信息...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // 构建完整上下文
        CharacterDao characterDao = new CharacterDao(this);
        StorySettingDao settingDao = new StorySettingDao(this);
        StoryRepository storyRepository = new StoryRepositoryImpl(this);

        String fullContext = memoryManager.buildFullContextForExtraction(
            "", // 对话历史为空，因为这是独立页面
            storyId,
            characterDao,
            settingDao,
            storyRepository
        );

        // 调用AI提取记忆
        String prompt = buildMemoryExtractionPrompt(fullContext);

        // 设置RequestOptions
        ApiClient.RequestOptions options = new ApiClient.RequestOptions()
            .setMaxTokens(3000)
            .setTemperature(0.3);

        apiClient.generateStory(prompt, currentModel, this, options, new ApiClient.Callback() {
            @Override
            public void onSuccess(String responseText) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();

                    // 解析AI返回的记忆
                    List<AiMemory> extractedMemories = parseExtractedMemories(responseText);

                    if (extractedMemories.isEmpty()) {
                        Toast.makeText(AiMemoryActivity.this, "未提取到重要记忆", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // 显示提取结果对话框
                    MemoryExtractionDialog dialog = MemoryExtractionDialog.newInstance(storyId, extractedMemories);
                    dialog.setOnMemoriesSavedListener(count -> {
                        Toast.makeText(AiMemoryActivity.this, "已保存 " + count + " 条记忆", Toast.LENGTH_SHORT).show();
                        // 刷新列表
                        loadMemories();
                    });
                    dialog.show(getSupportFragmentManager(), "memory_extraction");
                });
            }

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(AiMemoryActivity.this, "提取失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    /**
     * 构建记忆提取Prompt
     */
    private String buildMemoryExtractionPrompt(String fullContext) {
        return "请分析以下小说的全部信息，提取需要长期记住的重要内容。\n\n" +
               "【输入数据】\n" + fullContext + "\n\n" +
               "【提取要求】\n" +
               "1. 识别重要的剧情转折点或关键事件\n" +
               "2. 识别角色的重要特征或关系\n" +
               "3. 识别世界观设定的关键要素\n" +
               "4. 识别需要保持一致性的设定\n\n" +
               "【返回格式】\n" +
               "请返回JSON格式的记忆列表：\n" +
               "{\n" +
               "  \"memories\": [\n" +
               "    {\n" +
               "      \"type\": \"personality\",  // 可选: plot/personality/world/other\n" +
               "      \"title\": \"简短标题\",\n" +
               "      \"content\": \"详细内容（可为空）\",\n" +
               "      \"importance\": 3  // 1-5，3为中等重要性\n" +
               "    }\n" +
               "  ]\n" +
               "}\n\n" +
               "【注意事项】\n" +
               "- 只提取真正重要的信息，不要提取琐碎细节\n" +
               "- importance评分标准：\n" +
               "  - 5: 核心设定，绝对不能忘记\n" +
               "  - 4: 重要信息，应该记住\n" +
               "  - 3: 一般信息，可以记住\n" +
               "  - 2: 次要信息，可选择性记住\n" +
               "  - 1: 不重要，不应记住\n" +
               "- 只返回importance >= 3的记忆\n" +
               "- 直接返回JSON，不要包含其他文字";
    }

    /**
     * 解析AI返回的记忆
     */
    private List<AiMemory> parseExtractedMemories(String jsonResponse) {
        List<AiMemory> memories = new ArrayList<>();

        try {
            // 尝试解析JSON
            org.json.JSONObject jsonObject = new org.json.JSONObject(jsonResponse);
            org.json.JSONArray memoriesArray = jsonObject.getJSONArray("memories");

            for (int i = 0; i < memoriesArray.length(); i++) {
                org.json.JSONObject memoryObj = memoriesArray.getJSONObject(i);

                AiMemory memory = new AiMemory();
                memory.setMemoryType(memoryObj.optString("type", AiMemory.TYPE_OTHER));
                memory.setTitle(memoryObj.optString("title", "未命名记忆"));
                memory.setContent(memoryObj.optString("content", ""));
                memory.setImportance(memoryObj.optInt("importance", 3));

                // 只保存重要性 >= 3 的记忆
                if (memory.getImportance() >= 3) {
                    memories.add(memory);
                }
            }
        } catch (Exception e) {
            android.util.Log.e("AiMemoryActivity", "Failed to parse memories", e);
        }

        return memories;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 从其他页面返回时刷新列表
        loadMemories();
    }
}
