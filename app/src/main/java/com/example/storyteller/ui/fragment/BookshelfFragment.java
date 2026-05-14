package com.example.storyteller.ui.fragment;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.storyteller.R;
import com.example.storyteller.base.BaseFragment;
import com.example.storyteller.data.local.db.StoryDao;
import com.example.storyteller.data.local.prefs.PrefsUtils;
import com.example.storyteller.model.Story;
import com.example.storyteller.ui.adapter.StoryAdapter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.Collator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BookshelfFragment extends BaseFragment {
    private StoryDao storyDao;
    private StoryAdapter adapter;
    private TextView tvStoryCount;
    private TextView tvEmptyHint;
    private RecyclerView recyclerView;
    private EditText etSearch;
    private TextView btnSort;
    private TextView btnFilter;

    // 当前正在设置封面的故事ID
    private int pendingCoverStoryId = -1;

    // 搜索关键词
    private String searchKeyword = "";

    // 当前排序方式：0=创建时间降序, 1=创建时间升序, 2=标题升序, 3=字数降序
    private int currentSortType = 0;

    // 筛选条件
    private boolean filterOnlyCollected = false; // 仅显示收藏
    private String filterByStatus = null; // 按状态筛选：null=全部, "创作中", "已完成"

    // 图片选择启动器
    private final ActivityResultLauncher<String> pickImageLauncher =
        registerForActivityResult(new ActivityResultContracts.GetContent(), this::onImageSelected);

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_bookshelf;
    }

    @Override
    protected void initView(View view) {
        recyclerView = view.findViewById(R.id.rv_story_list);
        tvStoryCount = view.findViewById(R.id.tv_story_count);
        tvEmptyHint = view.findViewById(R.id.tv_empty_hint);
        etSearch = view.findViewById(R.id.et_search);
        btnSort = view.findViewById(R.id.btn_sort);
        btnFilter = view.findViewById(R.id.btn_filter);

        // 使用网格布局，每行2列
        recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 2));

        storyDao = new StoryDao(requireContext());
        adapter = new StoryAdapter(requireContext(), getFilteredStories());

        // 设置删除监听
        adapter.setOnStoryDeleteListener(storyId -> refreshStories());

        // 设置分类变更监听
        adapter.setOnStoryCategoryChangeListener((storyId, newCategory) -> refreshStories());

        // 设置封面变更监听
        adapter.setOnStoryCoverChangeListener((storyId, newCoverColor) -> refreshStories());

        // 设置上传封面图片监听
        adapter.setOnPickCoverImageListener(storyId -> {
            pendingCoverStoryId = storyId;
            pickImageLauncher.launch("image/*");
        });

        recyclerView.setAdapter(adapter);

        // 搜索框文本变化监听
        etSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(android.text.Editable s) {
                searchKeyword = s.toString().trim();
                refreshStories();
            }
        });

        // 排序按钮点击
        btnSort.setOnClickListener(v -> showSortMenu());

        // 筛选按钮点击
        btnFilter.setOnClickListener(v -> showFilterMenu());

        // 创建故事按钮
        Button btnCreateStory = view.findViewById(R.id.btn_create_story);
        btnCreateStory.setOnClickListener(v -> showCreateStoryDialog());
    }

    @Override
    protected void initData() {
        updateSortButtonText();
        updateFilterButtonText();
        refreshStories();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshStories();
    }

    /**
     * 处理选择的封面图片
     */
    private void onImageSelected(Uri imageUri) {
        if (imageUri == null || pendingCoverStoryId < 0) {
            pendingCoverStoryId = -1;
            return;
        }

        try {
            // 将图片复制到应用内部存储
            String fileName = "cover_" + pendingCoverStoryId + "_" + System.currentTimeMillis() + ".jpg";
            File coverDir = new File(requireContext().getFilesDir(), "covers");
            if (!coverDir.exists()) {
                coverDir.mkdirs();
            }
            File coverFile = new File(coverDir, fileName);

            // 复制图片文件
            try (InputStream inputStream = requireContext().getContentResolver().openInputStream(imageUri);
                 FileOutputStream outputStream = new FileOutputStream(coverFile)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }

            // 更新数据库中的封面路径
            String coverPath = coverFile.getAbsolutePath();
            storyDao.updateStoryCoverPath(pendingCoverStoryId, coverPath);

            Toast.makeText(requireContext(), "封面已更新", Toast.LENGTH_SHORT).show();
            refreshStories();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "设置封面失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        pendingCoverStoryId = -1;
    }

    private void refreshStories() {
        if (storyDao == null || adapter == null) {
            return;
        }
        List<Story> filteredStories = getFilteredStories();
        adapter.setData(filteredStories);

        // 更新作品数量
        tvStoryCount.setText(getString(R.string.bookshelf_story_count_format, filteredStories.size()));

        // 更新空状态
        if (filteredStories.isEmpty()) {
            tvEmptyHint.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvEmptyHint.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 获取过滤后的故事列表
     */
    private List<Story> getFilteredStories() {
        List<Story> allStories = storyDao.getAllStories();
        List<Story> filteredStories = new ArrayList<>(allStories);

        // 应用筛选条件
        if (filterOnlyCollected) {
            filteredStories = getCollectedStories(filteredStories);
        }
        
        if (filterByStatus != null) {
            filteredStories = filterByCategory(filteredStories, filterByStatus);
        }

        // 应用搜索过滤
        if (!TextUtils.isEmpty(searchKeyword)) {
            filteredStories = filterBySearch(filteredStories, searchKeyword);
        }

        return sortStoriesForBookshelf(filteredStories);
    }

    /**
     * 按分类过滤
     */
    private List<Story> filterByCategory(List<Story> stories, String category) {
        List<Story> result = new ArrayList<>();
        for (Story story : stories) {
            String storyCategory = story.getCategory();
            if (TextUtils.isEmpty(storyCategory)) {
                storyCategory = "创作中";
            }
            // 兼容“已完成”和“已完结”
            boolean matched = category.equals(storyCategory);
            if (!matched && "已完成".equals(category) && "已完结".equals(storyCategory)) {
                matched = true;
            }
            if (!matched && "已完结".equals(category) && "已完成".equals(storyCategory)) {
                matched = true;
            }
            if (matched) {
                result.add(story);
            }
        }
        return result;
    }

    /**
     * 获取已收藏的故事
     */
    private List<Story> getCollectedStories(List<Story> stories) {
        List<Story> result = new ArrayList<>();
        for (Story story : stories) {
            if (story.isCollected()) {
                result.add(story);
            }
        }
        return result;
    }

    /**
     * 按搜索关键词过滤
     */
    private List<Story> filterBySearch(List<Story> stories, String keyword) {
        if (TextUtils.isEmpty(keyword)) {
            return stories;
        }
        
        String lowerKeyword = keyword.toLowerCase();
        List<Story> result = new ArrayList<>();
        
        for (Story story : stories) {
            boolean matched = false;
            
            // 搜索标题
            String title = story.getTitle();
            if (!TextUtils.isEmpty(title) && title.toLowerCase().contains(lowerKeyword)) {
                matched = true;
            }
            
            // 搜索简介
            if (!matched) {
                String description = story.getDescription();
                if (!TextUtils.isEmpty(description) && description.toLowerCase().contains(lowerKeyword)) {
                    matched = true;
                }
            }
            
            // 搜索系列名
            if (!matched) {
                String seriesName = story.getSeriesName();
                if (!TextUtils.isEmpty(seriesName) && seriesName.toLowerCase().contains(lowerKeyword)) {
                    matched = true;
                }
            }
            
            if (matched) {
                result.add(story);
            }
        }
        
        return result;
    }

    /**
     * 根据当前排序方式排序
     */
    private List<Story> sortStoriesForBookshelf(List<Story> source) {
        if (source == null || source.isEmpty()) {
            return source;
        }

        List<Story> sortedStories = new ArrayList<>(source);
        Collator collator = Collator.getInstance(Locale.CHINA);

        switch (currentSortType) {
            case 0: // 创建时间降序（最新在前）
                sortedStories.sort((left, right) -> 
                    Long.compare(right.getCreateTime(), left.getCreateTime()));
                break;
            
            case 1: // 创建时间升序（最早在前）
                sortedStories.sort((left, right) -> 
                    Long.compare(left.getCreateTime(), right.getCreateTime()));
                break;
            
            case 2: // 标题升序（A-Z）
                sortedStories.sort((left, right) -> {
                    String leftTitle = left.getTitle() == null ? "" : left.getTitle().trim();
                    String rightTitle = right.getTitle() == null ? "" : right.getTitle().trim();
                    return collator.compare(leftTitle, rightTitle);
                });
                break;
            
            case 3: // 字数降序（多到少）
                sortedStories.sort((left, right) -> 
                    Integer.compare(right.getWordCount(), left.getWordCount()));
                break;
        }

        return sortedStories;
    }

    /**
     * 显示排序菜单
     */
    private void showSortMenu() {
        String[] sortOptions = {
            "创建时间（最新在前）",
            "创建时间（最早在前）",
            "标题（A-Z）",
            "字数（多到少）"
        };
        
        new AlertDialog.Builder(requireContext())
            .setTitle("选择排序方式")
            .setSingleChoiceItems(sortOptions, currentSortType, (dialog, which) -> {
                currentSortType = which;
                refreshStories();
                dialog.dismiss();
                
                // 更新按钮文本
                updateSortButtonText();
            })
            .show();
    }

    /**
     * 更新排序按钮文本
     */
    private void updateSortButtonText() {
        String[] sortLabels = {
            "⇅ 最新",
            "⇅ 最早",
            "⇅ 标题",
            "⇅ 字数"
        };
        btnSort.setText(sortLabels[currentSortType]);
    }

    /**
     * 显示筛选菜单
     */
    private void showFilterMenu() {
        // 构建筛选项列表
        List<String> filterOptions = new ArrayList<>();
        filterOptions.add("仅显示收藏" + (filterOnlyCollected ? " ✓" : ""));
        filterOptions.add("创作中" + ("创作中".equals(filterByStatus) ? " ✓" : ""));
        filterOptions.add("已完成" + ("已完成".equals(filterByStatus) ? " ✓" : ""));
        
        // 如果有筛选条件，添加重置选项
        boolean hasFilter = filterOnlyCollected || filterByStatus != null;
        if (hasFilter) {
            filterOptions.add("🔄 重置筛选");
        }
        
        new AlertDialog.Builder(requireContext())
            .setTitle("选择筛选条件")
            .setItems(filterOptions.toArray(new String[0]), (dialog, which) -> {
                if (which == 0) {
                    // 切换“仅显示收藏”
                    filterOnlyCollected = !filterOnlyCollected;
                    refreshStories();
                    updateFilterButtonText();
                } else if (which == 1) {
                    // 切换“创作中”
                    if ("创作中".equals(filterByStatus)) {
                        filterByStatus = null; // 取消筛选
                    } else {
                        filterByStatus = "创作中";
                    }
                    refreshStories();
                    updateFilterButtonText();
                } else if (which == 2) {
                    // 切换“已完成”
                    if ("已完成".equals(filterByStatus)) {
                        filterByStatus = null; // 取消筛选
                    } else {
                        filterByStatus = "已完成";
                    }
                    refreshStories();
                    updateFilterButtonText();
                } else if (which == 3 && hasFilter) {
                    // 重置筛选
                    filterOnlyCollected = false;
                    filterByStatus = null;
                    refreshStories();
                    updateFilterButtonText();
                }
                dialog.dismiss();
            })
            .show();
    }

    /**
     * 更新筛选按钮文本
     */
    private void updateFilterButtonText() {
        boolean hasFilter = filterOnlyCollected || filterByStatus != null;
        if (hasFilter) {
            btnFilter.setText("☰ 筛选 ·");
        } else {
            btnFilter.setText("☰ 筛选");
        }
    }

    /**
     * 显示创建小说弹窗
     */
    private void showCreateStoryDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_create_story, null);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create();

        EditText etTitle = dialogView.findViewById(R.id.et_story_title);
        EditText etSeriesName = dialogView.findViewById(R.id.et_series_name);
        EditText etDescription = dialogView.findViewById(R.id.et_story_description);

        dialogView.findViewById(R.id.btn_cancel).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.btn_create).setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            if (TextUtils.isEmpty(title)) {
                Toast.makeText(requireContext(), "请输入小说标题", Toast.LENGTH_SHORT).show();
                return;
            }

            String seriesName = etSeriesName.getText().toString().trim();
            String description = etDescription.getText().toString().trim();

            // Create empty story
            Story newStory = new Story(title, "", "创作", System.currentTimeMillis());
            if (!TextUtils.isEmpty(description)) {
                newStory.setDescription(description);
            }
            if (!TextUtils.isEmpty(seriesName)) {
                newStory.setSeriesName(seriesName);
            }

            // Initialize with one volume and one chapter
            java.util.List<com.example.storyteller.model.Volume> volumes = new java.util.ArrayList<>();
            com.example.storyteller.model.Volume volume = new com.example.storyteller.model.Volume(1, "新卷名");
            com.example.storyteller.model.Chapter chapter = new com.example.storyteller.model.Chapter(1, "新章节", "");
            volume.addChapter(chapter);
            volumes.add(volume);

            // Save structure as JSON
            String structureJson = com.example.storyteller.utils.JsonUtils.toJson(volumes);
            newStory.setStructure(structureJson);

            long id = storyDao.insertStory(newStory);

            if (id > 0) {
                Toast.makeText(requireContext(), "创建成功", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                refreshStories();

                // Set as current selected story
                PrefsUtils.getInstance(requireContext()).putString(StoryAdapter.PREF_SELECTED_STORY_ID, String.valueOf(id));
                PrefsUtils.getInstance(requireContext()).putString(StoryAdapter.PREF_SELECTED_STORY_TITLE, title);

                // Navigate directly to workspace page
                Intent intent = new Intent(requireContext(), com.example.storyteller.ui.activity.StoryWorkspaceActivity.class);
                intent.putExtra(com.example.storyteller.ui.activity.StoryWorkspaceActivity.EXTRA_STORY_ID, (int) id);
                startActivity(intent);
            } else {
                Toast.makeText(requireContext(), "创建失败", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }
}
