package com.example.storyteller.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.storyteller.R;
import com.example.storyteller.model.AiMemory;
import com.example.storyteller.utils.AiMemoryManager;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 记忆详情页
 * 功能：查看、编辑记忆
 */
public class MemoryDetailActivity extends AppCompatActivity {

    public static final String EXTRA_MEMORY_ID = "memory_id";

    private TextView tvPageTitle;
    private TextView tvTitle;
    private TextView tvMemoryType;
    private TextView tvImportance;
    private TextView tvContent;
    private TextView tvCreateTime;
    
    private EditText etEditTitle;
    private EditText etEditContent;
    private AutoCompleteTextView spEditType;
    private AutoCompleteTextView spEditImportance;
    
    private View layoutViewMode;
    private View layoutEditMode;
    private MaterialButton btnEdit;
    private MaterialButton btnSave;
    private View btnBack;
    
    private AiMemoryManager memoryManager;
    private AiMemory currentMemory;
    
    private boolean isEditMode = false;
    
    // 类型选项
    private static final String[] TYPE_OPTIONS = {
        "剧情类", "人设类", "世界观类", "其他类"
    };
    
    private static final String[] TYPE_VALUES = {
        AiMemory.TYPE_PLOT,
        AiMemory.TYPE_PERSONALITY,
        AiMemory.TYPE_WORLD,
        AiMemory.TYPE_OTHER
    };
    
    // 重要性选项
    private static final String[] IMPORTANCE_OPTIONS = {
        "⭐ 不重要",
        "⭐⭐ 次要",
        "⭐⭐⭐ 一般",
        "⭐⭐⭐⭐ 重要",
        "⭐⭐⭐⭐⭐ 核心"
    };
    
    private static final int[] IMPORTANCE_VALUES = {1, 2, 3, 4, 5};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_memory_detail);

        // 获取参数
        Intent intent = getIntent();
        long memoryId = intent.getLongExtra(EXTRA_MEMORY_ID, -1);

        // 初始化
        memoryManager = AiMemoryManager.getInstance(this);
        initView();
        
        if (memoryId != -1) {
            loadMemory(memoryId);
        } else {
            Toast.makeText(this, "无效的记忆ID", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initView() {
        // 查看模式视图
        tvPageTitle = findViewById(R.id.tv_page_title);
        tvTitle = findViewById(R.id.tv_title);
        tvMemoryType = findViewById(R.id.tv_memory_type);
        tvImportance = findViewById(R.id.tv_importance);
        tvContent = findViewById(R.id.tv_content);
        tvCreateTime = findViewById(R.id.tv_create_time);
        
        // 编辑模式视图
        etEditTitle = findViewById(R.id.et_edit_title);
        etEditContent = findViewById(R.id.et_edit_content);
        spEditType = findViewById(R.id.sp_edit_type);
        spEditImportance = findViewById(R.id.sp_edit_importance);
        
        // 布局容器
        layoutViewMode = findViewById(R.id.layout_view_mode);
        layoutEditMode = findViewById(R.id.layout_edit_mode);
        
        // 按钮
        btnBack = findViewById(R.id.btn_back);
        btnEdit = findViewById(R.id.btn_edit);
        btnSave = findViewById(R.id.btn_save);
        
        // 返回按钮
        btnBack.setOnClickListener(v -> handleBackAction());
        
        // 编辑按钮
        btnEdit.setOnClickListener(v -> enterEditMode());
        
        // 保存按钮
        btnSave.setOnClickListener(v -> saveChanges());
        
        // 设置类型下拉框
        setupTypeSpinner();
        
        // 设置重要性下拉框
        setupImportanceSpinner();
    }

    /**
     * 处理返回/放弃更改按钮点击
     */
    private void handleBackAction() {
        if (isEditMode) {
            // 编辑模式：弹出确认对话框
            showDiscardChangesDialog();
        } else {
            // 查看模式：直接返回
            finish();
        }
    }
    
    /**
     * 显示放弃更改确认对话框
     */
    private void showDiscardChangesDialog() {
        new AlertDialog.Builder(this)
            .setTitle("放弃更改")
            .setMessage("确定要放弃当前的修改吗？")
            .setPositiveButton("放弃", (dialog, which) -> {
                exitEditMode();
                displayMemory();
            })
            .setNegativeButton("取消", null)
            .show();
    }

    /**
     * 加载记忆数据
     */
    private void loadMemory(long memoryId) {
        currentMemory = memoryManager.getMemoryById((int) memoryId);
        
        if (currentMemory == null) {
            Toast.makeText(this, "记忆不存在", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        displayMemory();
    }

    /**
     * 显示记忆数据（查看模式）
     */
    private void displayMemory() {
        if (currentMemory == null) return;
        
        // 标题
        tvTitle.setText(currentMemory.getTitle());
        
        // 类型
        String typeLabel = getTypeLabel(currentMemory.getMemoryType());
        tvMemoryType.setText(typeLabel);
        
        // 重要性
        String importanceStars = getImportanceStars(currentMemory.getImportance());
        tvImportance.setText(importanceStars);
        
        // 内容
        if (currentMemory.getContent() != null && !currentMemory.getContent().isEmpty()) {
            tvContent.setText(currentMemory.getContent());
            tvContent.setVisibility(View.VISIBLE);
        } else {
            tvContent.setVisibility(View.GONE);
        }
        
        // 创建时间
        if (currentMemory.getCreatedAt() > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);
            tvCreateTime.setText("创建时间: " + sdf.format(new Date(currentMemory.getCreatedAt())));
        } else {
            tvCreateTime.setVisibility(View.GONE);
        }
        
        // 更新页面标题
        tvPageTitle.setText("记忆详情");
    }

    /**
     * 进入编辑模式
     */
    private void enterEditMode() {
        isEditMode = true;
        
        // 填充表单
        etEditTitle.setText(currentMemory.getTitle());
        etEditContent.setText(currentMemory.getContent() != null ? currentMemory.getContent() : "");
        
        // 设置类型
        String currentType = currentMemory.getMemoryType();
        for (int i = 0; i < TYPE_VALUES.length; i++) {
            if (TYPE_VALUES[i].equals(currentType)) {
                spEditType.setText(TYPE_OPTIONS[i], false);
                break;
            }
        }
        
        // 设置重要性
        int currentImportance = currentMemory.getImportance();
        for (int i = 0; i < IMPORTANCE_VALUES.length; i++) {
            if (IMPORTANCE_VALUES[i] == currentImportance) {
                spEditImportance.setText(IMPORTANCE_OPTIONS[i], false);
                break;
            }
        }
        
        // 切换UI
        setEditMode(true);
    }

    /**
     * 退出编辑模式
     */
    private void exitEditMode() {
        isEditMode = false;
        setEditMode(false);
    }

    /**
     * 设置编辑模式UI
     */
    private void setEditMode(boolean editMode) {
        if (editMode) {
            layoutViewMode.setVisibility(View.GONE);
            layoutEditMode.setVisibility(View.VISIBLE);
            btnEdit.setVisibility(View.GONE);
            btnSave.setVisibility(View.VISIBLE);
            tvPageTitle.setText("编辑记忆");
        } else {
            layoutViewMode.setVisibility(View.VISIBLE);
            layoutEditMode.setVisibility(View.GONE);
            btnEdit.setVisibility(View.VISIBLE);
            btnSave.setVisibility(View.GONE);
            tvPageTitle.setText("记忆详情");
        }
    }

    /**
     * 保存修改
     */
    private void saveChanges() {
        // 验证标题
        String title = etEditTitle.getText().toString().trim();
        if (title.isEmpty()) {
            Toast.makeText(this, "标题不能为空", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 获取类型
        String typeText = spEditType.getText().toString();
        String type = AiMemory.TYPE_OTHER;
        for (int i = 0; i < TYPE_OPTIONS.length; i++) {
            if (TYPE_OPTIONS[i].equals(typeText)) {
                type = TYPE_VALUES[i];
                break;
            }
        }
        
        // 获取重要性
        String importanceText = spEditImportance.getText().toString();
        int importance = 3;
        for (int i = 0; i < IMPORTANCE_OPTIONS.length; i++) {
            if (IMPORTANCE_OPTIONS[i].equals(importanceText)) {
                importance = IMPORTANCE_VALUES[i];
                break;
            }
        }
        
        // 获取内容
        String content = etEditContent.getText().toString().trim();
        
        // 更新记忆对象
        currentMemory.setTitle(title);
        currentMemory.setMemoryType(type);
        currentMemory.setImportance(importance);
        currentMemory.setContent(content);
        currentMemory.setUpdatedAt(System.currentTimeMillis());
        
        // 保存到数据库
        boolean success = memoryManager.updateMemory(currentMemory);
        
        if (success) {
            Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show();
            exitEditMode();
            displayMemory();
        } else {
            Toast.makeText(this, "保存失败", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 设置类型下拉框
     */
    private void setupTypeSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
            android.R.layout.simple_dropdown_item_1line, TYPE_OPTIONS);
        spEditType.setAdapter(adapter);
        spEditType.setText(TYPE_OPTIONS[0], false); // 默认第一个
    }

    /**
     * 设置重要性下拉框
     */
    private void setupImportanceSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
            android.R.layout.simple_dropdown_item_1line, IMPORTANCE_OPTIONS);
        spEditImportance.setAdapter(adapter);
        spEditImportance.setText(IMPORTANCE_OPTIONS[2], false); // 默认中等重要性
    }

    /**
     * 获取类型标签
     */
    private String getTypeLabel(String type) {
        if (type == null) return "其他类";
        switch (type) {
            case AiMemory.TYPE_PLOT:
                return "剧情类";
            case AiMemory.TYPE_PERSONALITY:
                return "人设类";
            case AiMemory.TYPE_WORLD:
                return "世界观类";
            default:
                return "其他类";
        }
    }

    /**
     * 获取重要性星级
     */
    private String getImportanceStars(int importance) {
        StringBuilder stars = new StringBuilder();
        for (int i = 0; i < importance; i++) {
            stars.append("⭐");
        }
        return stars.toString();
    }
}
