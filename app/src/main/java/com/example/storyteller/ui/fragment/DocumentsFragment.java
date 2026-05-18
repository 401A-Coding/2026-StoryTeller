package com.example.storyteller.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.storyteller.R;
import com.example.storyteller.base.BaseFragment;
import com.example.storyteller.data.local.db.StoryDocumentDao;
import com.example.storyteller.model.StoryDocument;
import com.example.storyteller.ui.activity.DocumentEditorActivity;
import com.example.storyteller.ui.adapter.DocumentAdapter;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

/**
 * 文档管理Fragment
 * 显示和管理故事的参考资料、笔记等文档
 */
public class DocumentsFragment extends BaseFragment {

    private static final String ARG_STORY_ID = "arg_story_id";

    private RecyclerView rvDocuments;
    private EditText etSearch;
    private ChipGroup chipGroupCategory;
    private LinearLayout layoutEmptyHint;
    private com.google.android.material.floatingactionbutton.FloatingActionButton fabAdd;

    private StoryDocumentDao documentDao;
    private DocumentAdapter adapter;
    private int storyId;
    private List<StoryDocument> allDocuments = new ArrayList<>();
    private String currentCategory = "all"; // all/world/character/plot/research/general

    public static DocumentsFragment newInstance(int storyId) {
        DocumentsFragment fragment = new DocumentsFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_STORY_ID, storyId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_documents;
    }

    @Override
    protected void initView(View view) {
        rvDocuments = view.findViewById(R.id.rv_documents);
        etSearch = view.findViewById(R.id.et_search);
        chipGroupCategory = view.findViewById(R.id.chip_group_category);
        layoutEmptyHint = view.findViewById(R.id.layout_empty_hint);
        fabAdd = view.findViewById(R.id.fab_add_document);

        // 设置RecyclerView
        rvDocuments.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new DocumentAdapter();
        rvDocuments.setAdapter(adapter);

        // 点击事件
        adapter.setOnDocumentClickListener(this::openDocumentEditor);
        
        // 长按事件（删除确认）
        adapter.setOnDocumentLongClickListener(this::showDeleteConfirmDialog);

        // FAB按钮点击事件
        fabAdd.setOnClickListener(v -> showCreateDocumentDialog());

        // 搜索
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterDocuments(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // 分类筛选
        setupCategoryFilter();
    }

    @Override
    protected void initData() {
        if (getArguments() != null) {
            storyId = getArguments().getInt(ARG_STORY_ID, -1);
        }

        documentDao = new StoryDocumentDao(requireContext());
        loadDocuments();
    }

    @Override
    public void onResume() {
        super.onResume();
        // 从编辑器返回时刷新列表
        loadDocuments();
    }

    /**
     * 加载文档列表
     */
    private void loadDocuments() {
        allDocuments = documentDao.getDocumentsByStory(storyId);
        updateUI();
    }

    /**
     * 更新UI显示
     */
    private void updateUI() {
        List<StoryDocument> filteredList = getFilteredList();
        adapter.setDocuments(filteredList);

        // 显示/隐藏空状态
        if (filteredList.isEmpty()) {
            layoutEmptyHint.setVisibility(View.VISIBLE);
            rvDocuments.setVisibility(View.GONE);
        } else {
            layoutEmptyHint.setVisibility(View.GONE);
            rvDocuments.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 获取过滤后的列表
     */
    private List<StoryDocument> getFilteredList() {
        String searchText = etSearch.getText().toString().trim();
        
        if (!TextUtils.isEmpty(searchText)) {
            // 搜索模式
            return documentDao.searchDocuments(storyId, searchText);
        } else if (!"all".equals(currentCategory)) {
            // 分类过滤模式
            return documentDao.getDocumentsByCategory(storyId, currentCategory);
        } else {
            // 全部
            return allDocuments;
        }
    }

    /**
     * 过滤文档（搜索）
     */
    private void filterDocuments(String keyword) {
        updateUI();
    }

    /**
     * 设置分类筛选器
     */
    private void setupCategoryFilter() {
        chipGroupCategory.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == View.NO_ID) {
                return;
            }

            Chip checkedChip = group.findViewById(checkedId);
            if (checkedChip != null) {
                String chipText = checkedChip.getText().toString();
                currentCategory = getCategoryFromText(chipText);
                updateUI();
            }
        });
    }

    /**
     * 从中文文本转换为category值
     */
    private String getCategoryFromText(String text) {
        switch (text) {
            case "全部":
                return "all";
            case "世界观":
                return "world";
            case "人物":
                return "character";
            case "剧情":
                return "plot";
            case "研究资料":
                return "research";
            case "其他":
                return "general";
            default:
                return "all";
        }
    }

    /**
     * 打开文档编辑器
     */
    private void openDocumentEditor(StoryDocument document) {
        Intent intent = new Intent(getContext(), DocumentEditorActivity.class);
        intent.putExtra("document_id", document.getId());
        startActivity(intent);
    }

    /**
     * 显示删除确认对话框
     */
    private void showDeleteConfirmDialog(StoryDocument document, int position) {
        new AlertDialog.Builder(requireContext())
                .setTitle("删除文档")
                .setMessage("确定要删除《" + document.getTitle() + "》吗？")
                .setPositiveButton("删除", (dialog, which) -> {
                    int result = documentDao.deleteDocument(document.getId());
                    if (result > 0) {
                        Toast.makeText(getContext(), "已删除", Toast.LENGTH_SHORT).show();
                        loadDocuments(); // 重新加载
                    } else {
                        Toast.makeText(getContext(), "删除失败", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 显示新建文档对话框
     */
    public void showCreateDocumentDialog() {
        View dialogView = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_create_document, null);

        EditText etTitle = dialogView.findViewById(R.id.et_document_title);
        Spinner spinnerCategory = dialogView.findViewById(R.id.spinner_category);

        // 设置分类选项
        ArrayAdapter<CharSequence> categoryAdapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.document_categories,
                android.R.layout.simple_spinner_item
        );
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(categoryAdapter);

        new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setTitle("新建文档")
                .setPositiveButton("创建", (dialog, which) -> {
                    String title = etTitle.getText().toString().trim();
                    if (TextUtils.isEmpty(title)) {
                        Toast.makeText(getContext(), "请输入标题", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String categoryText = spinnerCategory.getSelectedItem().toString();
                    String category = getCategoryFromText(categoryText);

                    StoryDocument doc = new StoryDocument();
                    doc.setStoryId(storyId);
                    doc.setTitle(title);
                    doc.setContent("");
                    doc.setCategory(category);

                    long id = documentDao.insertDocument(doc);
                    if (id > 0) {
                        Toast.makeText(getContext(), "创建成功", Toast.LENGTH_SHORT).show();
                        loadDocuments();

                        // 自动打开编辑器
                        doc.setId((int) id);
                        openDocumentEditor(doc);
                    } else {
                        Toast.makeText(getContext(), "创建失败", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 刷新文档列表（公开方法，供外部调用）
     */
    public void refreshDocuments() {
        loadDocuments();
    }
}
