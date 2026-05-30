package com.example.storyteller.ui.fragment;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
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
import com.example.storyteller.ui.dialog.CreateStoryDialog;
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

    // 当前排序方式：0=创建时间降序, 1=创建时间升序, 2=标题升序, 3=字数降序, 4=最近编辑降序
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

        // 帮助按钮点击
        ImageButton btnHelp = view.findViewById(R.id.btn_help);
        btnHelp.setOnClickListener(v -> showHelpDialog());

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

        // 强制重新获取数据库连接，确保读取最新数据
        storyDao = new StoryDao(requireContext());

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
     * 公开方法：刷新书架数据（供MainActivity调用）
     */
    public void refreshStoriesPublic() {
        refreshStories();
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
            
            case 4: // 最近编辑时间降序（最近编辑在前）
                sortedStories.sort((left, right) -> 
                    Long.compare(right.getLastEditTime(), left.getLastEditTime()));
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
            "字数（多到少）",
            "最近编辑（最近在前）"
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
            "⇅ 字数",
            "⇅ 编辑"
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
     * 显示使用帮助对话框
     */
    private void showHelpDialog() {
        String helpMessage = 
            "📚 管理你的小说\n\n" +
            "快速上手指南：\n\n" +
            "1️⃣ 创建小说\n" +
            "   • 点击右上角“+ 创建”按钮\n" +
            "   • 输入标题、系列名（可选）和简介\n" +
            "   • 自动创建一个卷和一个章节\n\n" +
            "2️⃣ 查找小说\n" +
            "   • 使用搜索框按标题或系列名搜索\n" +
            "   • 点击“⇅ 排序”按时间/标题/字数排序\n" +
            "   • 点击“☰ 筛选”按收藏/状态筛选\n\n" +
            "3️⃣ 编辑小说\n" +
            "   • 点击卡片任意位置进入编辑页面\n" +
            "   • 在架构Tab修改标题、分类、标签等\n" +
            "   • 在目录Tab编写章节内容\n\n" +
            "4️⃣ 管理小说\n" +
            "   • 点击三点菜单：收藏/上传封面/修改分类/删除\n" +
            "   • 定期整理书架，保持创作节奏\n\n" +
            "💡 提示：点击卡片直接进入编辑，无需经过详情页！";
        
        new AlertDialog.Builder(requireContext())
            .setTitle("❓ 使用帮助")
            .setMessage(helpMessage)
            .setPositiveButton("知道了", null)
            .show();
    }

    /**
     * 显示创建小说弹窗
     */
    private void showCreateStoryDialog() {
        CreateStoryDialog dialog = CreateStoryDialog.newInstance();
        dialog.setOnCreateStoryListener(this::createStory);
        dialog.show(getParentFragmentManager(), "create_story_dialog");
    }

    /**
     * 处理创建小说
     */
    private void createStory(String title, String seriesName, String description) {
        Story newStory = new Story(title, "", "创作", System.currentTimeMillis());
        if (!TextUtils.isEmpty(description)) {
            newStory.setDescription(description);
        }
        if (!TextUtils.isEmpty(seriesName)) {
            newStory.setSeriesName(seriesName);
        }

        java.util.List<com.example.storyteller.model.Volume> volumes = new java.util.ArrayList<>();
        com.example.storyteller.model.Volume volume = new com.example.storyteller.model.Volume(1, "新卷名");
        com.example.storyteller.model.Chapter chapter = new com.example.storyteller.model.Chapter(1, "新章节", "");
        volume.addChapter(chapter);
        volumes.add(volume);

        String structureJson = com.example.storyteller.utils.JsonUtils.toJson(volumes);
        newStory.setStructure(structureJson);

        long id = storyDao.insertStory(newStory);

        if (id > 0) {
            com.example.storyteller.utils.PreferenceManager preferenceManager =
                com.example.storyteller.utils.PreferenceManager.getInstance(requireContext());
            preferenceManager.copyGlobalPreferenceToStory((int) id);

            Toast.makeText(requireContext(), "创建成功", Toast.LENGTH_SHORT).show();
            refreshStories();

            PrefsUtils.getInstance(requireContext()).putString(StoryAdapter.PREF_SELECTED_STORY_ID, String.valueOf(id));
            PrefsUtils.getInstance(requireContext()).putString(StoryAdapter.PREF_SELECTED_STORY_TITLE, title);

            Intent intent = new Intent(requireContext(), com.example.storyteller.ui.activity.StoryWorkspaceActivity.class);
            intent.putExtra(com.example.storyteller.ui.activity.StoryWorkspaceActivity.EXTRA_STORY_ID, (int) id);
            startActivity(intent);
        } else {
            Toast.makeText(requireContext(), "创建失败", Toast.LENGTH_SHORT).show();
        }
    }
}
