package com.example.storyteller.ui.fragment;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.storyteller.R;
import com.example.storyteller.base.BaseFragment;
import com.example.storyteller.data.local.db.StoryDao;
import com.example.storyteller.data.local.prefs.PrefsUtils;
import com.example.storyteller.model.Story;
import com.example.storyteller.ui.activity.StoryWorkspaceActivity;
import com.example.storyteller.ui.adapter.StoryAdapter;
import com.example.storyteller.ui.adapter.RecentStoryAdapter;
import com.example.storyteller.model.Volume;
import com.example.storyteller.utils.JsonUtils;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends BaseFragment {

    private TextView tvCurrentNovelTitle;
    private TextView tvCurrentNovelStats;
    private TextView tvCurrentNovelLastEdit;
    private View btnContinueEdit;
    private ImageView ivCurrentCoverImage;
    private View vCurrentCoverBackground;
    private TextView tvRecentTitle;
    private StoryDao storyDao;
    private RecyclerView rvRecentStories;
    private RecentStoryAdapter recentAdapter;
    private Story currentStory;

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_home;
    }

    @Override
    protected void initView(View view) {
        btnContinueEdit = view.findViewById(R.id.btn_continue_edit);
        btnContinueEdit.setOnClickListener(v -> continueEditingCurrentStory());
        view.findViewById(R.id.btn_switch_novel).setOnClickListener(v -> showSwitchNovelDialog());
        tvCurrentNovelTitle = view.findViewById(R.id.tv_current_novel_title);
        tvCurrentNovelStats = view.findViewById(R.id.tv_current_novel_stats);
        tvCurrentNovelLastEdit = view.findViewById(R.id.tv_current_novel_last_edit);
        ivCurrentCoverImage = view.findViewById(R.id.iv_current_cover_image);
        vCurrentCoverBackground = view.findViewById(R.id.v_current_cover_background);

        // 初始化最近编辑列表
        rvRecentStories = view.findViewById(R.id.rv_recent_stories);
        tvRecentTitle = view.findViewById(R.id.tv_recent_title);
        setupRecentStoriesList();
    }

    @Override
    protected void initData() {
        storyDao = new StoryDao(requireContext());
        refreshCurrentNovel();
        loadRecentStories();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshCurrentNovel();
        loadRecentStories();
    }

    private void refreshCurrentNovel() {
        if (storyDao == null || tvCurrentNovelTitle == null || tvCurrentNovelStats == null || tvCurrentNovelLastEdit == null || btnContinueEdit == null) {
            return;
        }
        String selectedId = PrefsUtils.getInstance(requireContext()).getString(StoryAdapter.PREF_SELECTED_STORY_ID, "");
        Story story = null;

        if (!TextUtils.isEmpty(selectedId)) {
            try {
                story = storyDao.getStoryById(Integer.parseInt(selectedId));
            } catch (NumberFormatException e) {
                // fall back to latest story below
            }
        }

        if (story == null) {
            story = storyDao.getLatestStory();
        }

        if (story == null) {
            currentStory = null;
            clearSelectedStory();
            tvCurrentNovelTitle.setText(R.string.home_current_novel_empty_title);
            tvCurrentNovelStats.setText(R.string.home_current_novel_stats_empty);
            tvCurrentNovelLastEdit.setText(R.string.home_current_novel_last_edit_empty);
            btnContinueEdit.setVisibility(View.GONE);
            bindCurrentCover(null);
            return;
        }

        currentStory = story;
        persistSelectedStory(story);
        tvCurrentNovelTitle.setText(story.getTitle());
        tvCurrentNovelStats.setText(buildStatsText(story));
        tvCurrentNovelLastEdit.setText(getString(R.string.home_current_novel_last_edit_format, formatLastEditTime(story.getLastEditTime())));
        btnContinueEdit.setVisibility(View.VISIBLE);
        bindCurrentCover(story);
    }

    private String formatLastEditTime(long timestamp) {
        if (timestamp <= 0) {
            return "暂无";
        }

        long now = System.currentTimeMillis();
        long diff = now - timestamp;

        if (diff < 60 * 1000) {
            return "刚刚";
        } else if (diff < 60 * 60 * 1000) {
            return (diff / (60 * 1000)) + "分钟前";
        } else if (diff < 24 * 60 * 60 * 1000) {
            return (diff / (60 * 60 * 1000)) + "小时前";
        } else if (diff < 7L * 24 * 60 * 60 * 1000) {
            return (diff / (24 * 60 * 60 * 1000)) + "天前";
        }

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MM-dd", Locale.getDefault());
        return sdf.format(new java.util.Date(timestamp));
    }

    private void continueEditingCurrentStory() {
        if (storyDao == null) {
            return;
        }

        String selectedId = PrefsUtils.getInstance(requireContext()).getString(StoryAdapter.PREF_SELECTED_STORY_ID, "");
        if (TextUtils.isEmpty(selectedId)) {
            if (currentStory != null) {
                launchStoryWorkspace(currentStory.getId());
                return;
            }
            showNoNovelSelectedDialog();
            return;
        }

        try {
            int storyId = Integer.parseInt(selectedId);
            Story story = storyDao.getStoryById(storyId);
            if (story == null) {
                clearSelectedStory();
                refreshCurrentNovel();
                Toast.makeText(requireContext(), "所选小说不存在，请重新选择", Toast.LENGTH_SHORT).show();
                showNoNovelSelectedDialog();
                return;
            }

            persistSelectedStory(story);
            currentStory = story;
            launchStoryWorkspace(story.getId());
        } catch (NumberFormatException e) {
            clearSelectedStory();
            refreshCurrentNovel();
            Toast.makeText(requireContext(), "数据异常，请重新选择小说", Toast.LENGTH_SHORT).show();
            showNoNovelSelectedDialog();
        }
    }

    private void launchStoryWorkspace(int storyId) {
        Intent intent = new Intent(requireContext(), StoryWorkspaceActivity.class);
        intent.putExtra(StoryWorkspaceActivity.EXTRA_STORY_ID, storyId);
        startActivity(intent);
    }

    private void bindCurrentCover(Story story) {
        if (ivCurrentCoverImage == null || vCurrentCoverBackground == null) {
            return;
        }

        String title = story != null ? story.getTitle() : getString(R.string.home_current_novel_empty_title);
        String coverPath = story != null ? story.getCoverPath() : null;
        boolean loadedImage = false;

        if (!TextUtils.isEmpty(coverPath)) {
            File coverFile = new File(coverPath);
            if (coverFile.exists()) {
                android.graphics.Bitmap bitmap = BitmapFactory.decodeFile(coverPath);
                if (bitmap != null) {
                    ivCurrentCoverImage.setImageBitmap(bitmap);
                    ivCurrentCoverImage.setVisibility(View.VISIBLE);
                    loadedImage = true;
                }
            }
        }

        if (!loadedImage) {
            ivCurrentCoverImage.setImageDrawable(null);
            ivCurrentCoverImage.setVisibility(View.GONE);
        }

        GradientDrawable gradient = new GradientDrawable();
        gradient.setOrientation(GradientDrawable.Orientation.TL_BR);
        gradient.setColors(getCoverGradientColors(title));
        vCurrentCoverBackground.setBackground(gradient);
    }

    private int[] getCoverGradientColors(String title) {
        if (TextUtils.isEmpty(title)) {
            return new int[]{0xFF1976D2, 0xFF42A5F5};
        }

        int[][] gradients = {
            {0xFF667eea, 0xFF764ba2},
            {0xFFf093fb, 0xFFf5576c},
            {0xFF4facfe, 0xFF00f2fe},
            {0xFF43e97b, 0xFF38f9d7},
            {0xFFfa709a, 0xFFfee140},
            {0xFF30cfd0, 0xFF330867},
            {0xFFa8edea, 0xFFfed6e3},
            {0xFFff9a9e, 0xFFfecfef}
        };

        return gradients[Math.abs(title.hashCode()) % gradients.length];
    }

    private String buildStatsText(Story story) {
        int volumeCount = 0;
        int chapterCount = 0;

        String structureJson = story.getStructure();
        if (!TextUtils.isEmpty(structureJson)) {
            try {
                Type type = new TypeToken<List<Volume>>() {}.getType();
                List<Volume> volumes = JsonUtils.fromJson(structureJson, type);
                if (volumes != null) {
                    volumeCount = volumes.size();
                    for (Volume volume : volumes) {
                        if (volume.getChapters() != null) {
                            chapterCount += volume.getChapters().size();
                        }
                    }
                }
            } catch (Exception e) {
                volumeCount = 0;
                chapterCount = 0;
            }
        }

        return String.format(Locale.CHINA, "📚 %d卷 · %d章 · 📖 %s", volumeCount, chapterCount, formatWordCount(story.getWordCount()));
    }

    private String formatWordCount(int wordCount) {
        if (wordCount < 10000) {
            return wordCount + "字";
        }
        return String.format(Locale.CHINA, "%.1f万字", wordCount / 10000.0);
    }

    private void persistSelectedStory(Story story) {
        if (story == null) {
            return;
        }
        PrefsUtils.getInstance(requireContext()).putString(StoryAdapter.PREF_SELECTED_STORY_ID, String.valueOf(story.getId()));
        PrefsUtils.getInstance(requireContext()).putString(StoryAdapter.PREF_SELECTED_STORY_TITLE, story.getTitle());
    }

    private void clearSelectedStory() {
        PrefsUtils.getInstance(requireContext()).putString(StoryAdapter.PREF_SELECTED_STORY_ID, "");
        PrefsUtils.getInstance(requireContext()).putString(StoryAdapter.PREF_SELECTED_STORY_TITLE, "");
    }

    /**
     * 显示切换小说对话框
     */
    private void showSwitchNovelDialog() {
        if (storyDao == null) {
            return;
        }
        List<Story> stories = storyDao.getAllStories();
        if (stories == null || stories.isEmpty()) {
            Toast.makeText(requireContext(), "暂无小说，请先创建", Toast.LENGTH_SHORT).show();
            return;
        }

        // Extract story titles
        String[] storyTitles = new String[stories.size()];
        for (int i = 0; i < stories.size(); i++) {
            storyTitles[i] = stories.get(i).getTitle();
        }

        // Get currently selected story index
        String selectedId = PrefsUtils.getInstance(requireContext()).getString(StoryAdapter.PREF_SELECTED_STORY_ID, "");
        int selectedIndex = -1;
        if (!TextUtils.isEmpty(selectedId)) {
            try {
                int id = Integer.parseInt(selectedId);
                for (int i = 0; i < stories.size(); i++) {
                    if (stories.get(i).getId() == id) {
                        selectedIndex = i;
                        break;
                    }
                }
            } catch (NumberFormatException e) {
                // Ignore
            }
        }

        // Show single choice dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("选择小说");
        builder.setSingleChoiceItems(storyTitles, selectedIndex, (dialog, which) -> {
            Story selectedStory = stories.get(which);

            PrefsUtils.getInstance(requireContext()).putString(StoryAdapter.PREF_SELECTED_STORY_ID, String.valueOf(selectedStory.getId()));
            PrefsUtils.getInstance(requireContext()).putString(StoryAdapter.PREF_SELECTED_STORY_TITLE, selectedStory.getTitle());
            refreshCurrentNovel();
            dialog.dismiss();
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    /**
     * 显示未选择小说的提示对话框
     */
    private void showNoNovelSelectedDialog() {
        if (storyDao == null) {
            return;
        }
        List<Story> stories = storyDao.getAllStories();
        
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("提示");
        
        if (stories == null || stories.isEmpty()) {
            // 没有任何小说，询问是否创建新小说
            builder.setMessage("您还没有创建任何小说。\n是否要创建一个新小说？");
            builder.setPositiveButton("创建新小说", (dialog, which) -> {
                // 弹出创建小说对话框
                showCreateStoryDialog();
                dialog.dismiss();
            });
            builder.setNegativeButton("取消", null);
        } else {
            // 有小说但未选择，让用户选择
            builder.setMessage("您还未选择当前小说。\n请选择一个小说或创建新小说。");
            
            // 提取小说标题
            String[] storyTitles = new String[stories.size()];
            for (int i = 0; i < stories.size(); i++) {
                storyTitles[i] = stories.get(i).getTitle();
            }
            
            builder.setSingleChoiceItems(storyTitles, -1, (dialog, which) -> {
                Story selectedStory = stories.get(which);
                PrefsUtils.getInstance(requireContext()).putString(StoryAdapter.PREF_SELECTED_STORY_ID, String.valueOf(selectedStory.getId()));
                PrefsUtils.getInstance(requireContext()).putString(StoryAdapter.PREF_SELECTED_STORY_TITLE, selectedStory.getTitle());
                refreshCurrentNovel();
                launchStoryWorkspace(selectedStory.getId());
                dialog.dismiss();
            });
            
            builder.setPositiveButton("创建新小说", (dialog, which) -> {
                // 弹出创建小说对话框
                showCreateStoryDialog();
                dialog.dismiss();
            });
            
            builder.setNegativeButton("取消", null);
        }
        
        builder.show();
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
            com.example.storyteller.model.Volume volume = new com.example.storyteller.model.Volume(1, "第一卷");
            com.example.storyteller.model.Chapter chapter = new com.example.storyteller.model.Chapter(1, "第一章", "");
            volume.addChapter(chapter);
            volumes.add(volume);

            // Save structure as JSON
            String structureJson = com.example.storyteller.utils.JsonUtils.toJson(volumes);
            newStory.setStructure(structureJson);

            long id = storyDao.insertStory(newStory);

            if (id > 0) {
                com.example.storyteller.utils.PreferenceManager preferenceManager = 
                    com.example.storyteller.utils.PreferenceManager.getInstance(requireContext());
                preferenceManager.copyGlobalPreferenceToStory((int) id);
                
                newStory.setId((int) id);
                persistSelectedStory(newStory);
                currentStory = newStory;
                Toast.makeText(requireContext(), "创建成功", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                refreshCurrentNovel();
                launchStoryWorkspace((int) id);
            } else {
                Toast.makeText(requireContext(), "创建失败", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    /**
     * 初始化最近编辑列表
     */
    private void setupRecentStoriesList() {
        recentAdapter = new RecentStoryAdapter(requireContext(), new java.util.ArrayList<>());
        rvRecentStories.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvRecentStories.setAdapter(recentAdapter);
        recentAdapter.setOnStoryClickListener(story -> {
            persistSelectedStory(story);
            currentStory = story;
            refreshCurrentNovel();
            launchStoryWorkspace(story.getId());
        });
    }

    /**
     * 加载最近编辑的小说
     */
    private void loadRecentStories() {
        if (storyDao == null || recentAdapter == null) {
            return;
        }

        List<Story> recentStories = storyDao.getRecentStories(3);
        if (recentStories == null || recentStories.isEmpty()) {
            recentAdapter.setData(new java.util.ArrayList<>());
            tvRecentTitle.setVisibility(View.GONE);
            rvRecentStories.setVisibility(View.GONE);
        } else {
            tvRecentTitle.setVisibility(View.VISIBLE);
            rvRecentStories.setVisibility(View.VISIBLE);
            recentAdapter.setData(recentStories);
        }
    }
}
