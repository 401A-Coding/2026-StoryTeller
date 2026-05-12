package com.example.storyteller.ui.fragment;

import android.app.AlertDialog;
import android.content.Intent;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.storyteller.R;
import com.example.storyteller.base.BaseFragment;
import com.example.storyteller.data.local.db.StoryDao;
import com.example.storyteller.data.local.prefs.PrefsUtils;
import com.example.storyteller.model.Story;
import com.example.storyteller.ui.adapter.StoryAdapter;
import java.text.Collator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BookshelfFragment extends BaseFragment {
    private StoryDao storyDao;
    private StoryAdapter adapter;

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_bookshelf;
    }

    @Override
    protected void initView(View view) {
        RecyclerView recyclerView = view.findViewById(R.id.rv_story_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        storyDao = new StoryDao(requireContext());
        adapter = new StoryAdapter(requireContext(), sortStoriesForBookshelf(storyDao.getAllStories()));

        // Set delete listener
        adapter.setOnStoryDeleteListener(storyId -> {
            refreshStories();
        });

        recyclerView.setAdapter(adapter);

        // Create story button
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
        List<Story> stories = storyDao.getAllStories();
        adapter.setData(sortStoriesForBookshelf(stories));
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

            // Create empty story
            Story newStory = new Story(title, "", TextUtils.isEmpty(seriesName) ? "创作" : seriesName, System.currentTimeMillis());
            if (!TextUtils.isEmpty(description)) {
                newStory.setDescription(description);
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
