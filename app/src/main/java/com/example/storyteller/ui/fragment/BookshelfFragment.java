package com.example.storyteller.ui.fragment;

import android.app.AlertDialog;
import android.content.Intent;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.storyteller.R;
import com.example.storyteller.base.BaseFragment;
import com.example.storyteller.data.local.db.StoryDao;
import com.example.storyteller.data.local.prefs.PrefsUtils;
import com.example.storyteller.model.Story;
import com.example.storyteller.ui.activity.StoryGenerateActivity;
import com.example.storyteller.ui.adapter.StoryAdapter;
import com.google.android.material.tabs.TabLayout;

import java.text.Collator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BookshelfFragment extends BaseFragment {
    private StoryDao storyDao;
    private StoryAdapter adapter;
    private TabLayout tabCategory;
    private TextView tvStoryCount;
    private TextView tvEmptyHint;
    private RecyclerView recyclerView;

    // 当前选中的分类索引：0=全部, 1=创作中, 2=已完成, 3=已收藏
    private int currentCategoryIndex = 0;

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_bookshelf;
    }

    @Override
    protected void initView(View view) {
        recyclerView = view.findViewById(R.id.rv_story_list);
        tabCategory = view.findViewById(R.id.tab_category);
        tvStoryCount = view.findViewById(R.id.tv_story_count);
        tvEmptyHint = view.findViewById(R.id.tv_empty_hint);

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

        recyclerView.setAdapter(adapter);

        // 分类标签切换
        tabCategory.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentCategoryIndex = tab.getPosition();
                refreshStories();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        // 创建故事按钮
        Button btnCreateStory = view.findViewById(R.id.btn_create_story);
        btnCreateStory.setOnClickListener(v -> showCreateStoryDialog());
    }

    @Override
    protected void initData() {
        refreshStories();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshStories();
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
     * 根据当前选中的分类获取过滤后的故事列表
     */
    private List<Story> getFilteredStories() {
        List<Story> allStories = storyDao.getAllStories();
        List<Story> filteredStories;

        switch (currentCategoryIndex) {
            case 1: // 创作中
                filteredStories = filterByCategory(allStories, getString(R.string.bookshelf_category_writing));
                break;
            case 2: // 已完成
                filteredStories = filterByCategory(allStories, getString(R.string.bookshelf_category_completed));
                break;
            case 3: // 已收藏
                filteredStories = getCollectedStories(allStories);
                break;
            default: // 全部
                filteredStories = new ArrayList<>(allStories);
                break;
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
            if (category.equals(storyCategory)) {
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

    private List<Story> sortStoriesForBookshelf(List<Story> source) {
        if (source == null || source.isEmpty()) {
            return source;
        }

        List<Story> sortedStories = new ArrayList<>(source);
        String selectedStoryId = PrefsUtils.getInstance(requireContext())
            .getString(StoryAdapter.PREF_SELECTED_STORY_ID, "");
        Collator collator = Collator.getInstance(Locale.CHINA);

        sortedStories.sort((left, right) -> {
            boolean leftIsSelected = selectedStoryId.equals(String.valueOf(left.getId()));
            boolean rightIsSelected = selectedStoryId.equals(String.valueOf(right.getId()));
            if (leftIsSelected != rightIsSelected) {
                return leftIsSelected ? -1 : 1;
            }

            String leftTitle = left.getTitle() == null ? "" : left.getTitle().trim();
            String rightTitle = right.getTitle() == null ? "" : right.getTitle().trim();
            int titleCompare = collator.compare(leftTitle, rightTitle);
            if (titleCompare != 0) {
                return titleCompare;
            }

            return Long.compare(right.getCreateTime(), left.getCreateTime());
        });

        return sortedStories;
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

            // 创建新故事，默认分类为"创作中"，默认封面颜色为蓝色
            Story newStory = new Story(title, "", TextUtils.isEmpty(seriesName) ? "创作" : seriesName, System.currentTimeMillis());
            if (!TextUtils.isEmpty(description)) {
                newStory.setDescription(description);
            }
            newStory.setCategory(getString(R.string.bookshelf_category_writing));
            newStory.setCoverColor("#1976D2");

            // 初始化一卷一章
            java.util.List<com.example.storyteller.model.Volume> volumes = new java.util.ArrayList<>();
            com.example.storyteller.model.Volume volume = new com.example.storyteller.model.Volume(1, "第一卷");
            com.example.storyteller.model.Chapter chapter = new com.example.storyteller.model.Chapter(1, "第一章", "");
            volume.addChapter(chapter);
            volumes.add(volume);

            String structureJson = com.example.storyteller.utils.JsonUtils.toJson(volumes);
            newStory.setStructure(structureJson);

            long id = storyDao.insertStory(newStory);

            if (id > 0) {
                Toast.makeText(requireContext(), "创建成功", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                refreshStories();

                Intent intent = new Intent(requireContext(), StoryGenerateActivity.class);
                intent.putExtra("story_id", (int) id);
                startActivity(intent);
            } else {
                Toast.makeText(requireContext(), "创建失败", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }
}