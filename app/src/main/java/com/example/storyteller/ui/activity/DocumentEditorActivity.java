package com.example.storyteller.ui.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.storyteller.R;
import com.example.storyteller.base.BaseActivity;
import com.example.storyteller.data.local.db.StoryDocumentDao;
import com.example.storyteller.model.StoryDocument;

import java.util.Timer;
import java.util.TimerTask;

import io.noties.markwon.Markwon;

/**
 * 文档编辑器Activity
 * 支持Markdown编辑和预览
 */
public class DocumentEditorActivity extends BaseActivity {

    private EditText etContent;
    private TextView tvPreview;
    private ScrollView scrollPreview;
    private Button btnToggleMode;
    private Button btnSave;

    private Markwon markwon;
    private StoryDocumentDao documentDao;
    private StoryDocument currentDocument;
    private boolean isEditMode = true;
    private Timer autoSaveTimer;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_document_editor;
    }

    @Override
    protected void initView() {
        // 刘海屏适配：应用系统窗口 insets
        applySystemWindowInsets(findViewById(android.R.id.content));
        
        etContent = findViewById(R.id.et_document_content);
        tvPreview = findViewById(R.id.tv_document_preview);
        scrollPreview = findViewById(R.id.scroll_preview);
        btnToggleMode = findViewById(R.id.btn_toggle_mode);
        btnSave = findViewById(R.id.btn_save);

        // 初始化Markwon
        markwon = Markwon.create(this);

        // 切换编辑/预览模式
        btnToggleMode.setOnClickListener(v -> {
            if (isEditMode) {
                // 切换到预览模式
                showPreviewMode();
            } else {
                // 切换到编辑模式
                showEditMode();
            }
        });

        // 保存按钮
        btnSave.setOnClickListener(v -> {
            saveDocumentSilently();
            android.widget.Toast.makeText(this, "已保存", android.widget.Toast.LENGTH_SHORT).show();
        });

        // 设置自动保存（每5秒）
        autoSaveTimer = new Timer();
        autoSaveTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(() -> saveDocumentSilently());
            }
        }, 5000, 5000);
    }

    @Override
    protected void initData() {
        documentDao = new StoryDocumentDao(this);

        int documentId = getIntent().getIntExtra("document_id", -1);
        if (documentId > 0) {
            loadDocument(documentId);
        } else {
            Toast.makeText(this, "文档不存在", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /**
     * 加载文档
     */
    private void loadDocument(int documentId) {
        currentDocument = documentDao.getDocumentById(documentId);
        if (currentDocument != null) {
            etContent.setText(currentDocument.getContent());
            
            // 设置标题（从ActionBar或Toolbar显示）
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(currentDocument.getTitle());
            }
        }
    }

    /**
     * 显示预览模式
     */
    private void showPreviewMode() {
        // 先保存当前内容
        saveDocumentSilently();

        // 渲染Markdown
        String markdown = etContent.getText().toString();
        markwon.setMarkdown(tvPreview, markdown);

        // 切换视图
        etContent.setVisibility(View.GONE);
        scrollPreview.setVisibility(View.VISIBLE);
        btnToggleMode.setText("编辑");
        isEditMode = false;
    }

    /**
     * 显示编辑模式
     */
    private void showEditMode() {
        etContent.setVisibility(View.VISIBLE);
        scrollPreview.setVisibility(View.GONE);
        btnToggleMode.setText("预览");
        isEditMode = true;
    }

    /**
     * 静默保存文档
     */
    private void saveDocumentSilently() {
        if (currentDocument != null && isEditMode) {
            String content = etContent.getText().toString();
            if (!content.equals(currentDocument.getContent())) {
                currentDocument.setContent(content);
                documentDao.updateDocument(currentDocument);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 暂停时保存
        saveDocumentSilently();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 取消自动保存定时器
        if (autoSaveTimer != null) {
            autoSaveTimer.cancel();
        }
        // 最终保存
        saveDocumentSilently();
    }
}
