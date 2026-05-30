package com.example.storyteller.ui.fragment;

import android.app.Activity;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.util.Log;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.storyteller.R;
import com.example.storyteller.base.BaseFragment;
import com.example.storyteller.data.local.db.StoryDao;
import com.example.storyteller.data.local.prefs.PrefsUtils;
import com.example.storyteller.model.Story;
import com.example.storyteller.model.Volume;
import com.example.storyteller.ui.activity.StoryWorkspaceActivity;
import com.example.storyteller.ui.adapter.RecentStoryAdapter;
import com.example.storyteller.ui.adapter.StoryAdapter;
import com.example.storyteller.ui.dialog.CreateStoryDialog;
import com.example.storyteller.utils.JsonUtils;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends BaseFragment {

    private static final String TAG = "HomeFragment";

    private TextView tvCurrentNovelTitle;
    private TextView tvCurrentNovelStats;
    private TextView tvCurrentNovelLastEdit;
    private Button btnPrimaryAction;
    private View btnSwitchNovel;
    private ImageButton btnMoreMenu;
    private ActivityResultLauncher<Intent> selectCoverLauncher;
    private TextView tvCurrentCoverCategory;
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
        selectCoverLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        saveCoverImage(imageUri);
                    }
                }
            }
        );

        btnPrimaryAction = view.findViewById(R.id.btn_continue_edit);
        btnSwitchNovel = view.findViewById(R.id.btn_switch_novel);
        btnSwitchNovel.setOnClickListener(v -> showSwitchNovelDialog());
        btnMoreMenu = view.findViewById(R.id.btn_more_menu);
        btnMoreMenu.setOnClickListener(v -> showCurrentNovelMoreMenu());

        tvCurrentNovelTitle = view.findViewById(R.id.tv_current_novel_title);
        tvCurrentNovelStats = view.findViewById(R.id.tv_current_novel_stats);
        tvCurrentNovelLastEdit = view.findViewById(R.id.tv_current_novel_last_edit);
        tvCurrentCoverCategory = view.findViewById(R.id.tv_current_cover_category);
        ivCurrentCoverImage = view.findViewById(R.id.iv_current_cover_image);
        vCurrentCoverBackground = view.findViewById(R.id.v_current_cover_background);

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
        if (storyDao == null || tvCurrentNovelTitle == null || tvCurrentNovelStats == null || tvCurrentNovelLastEdit == null || btnPrimaryAction == null || btnSwitchNovel == null || btnMoreMenu == null || tvCurrentCoverCategory == null) {
            return;
        }

        Story story = resolveCurrentStory();
        if (story == null) {
            currentStory = null;
            clearSelectedStory();
            tvCurrentNovelTitle.setText(R.string.home_current_novel_empty_title);
            tvCurrentNovelStats.setText(R.string.home_current_novel_stats_empty);
            tvCurrentNovelLastEdit.setText(R.string.home_current_novel_last_edit_empty);
            tvCurrentCoverCategory.setVisibility(View.GONE);
            configurePrimaryAction(false);
            btnSwitchNovel.setVisibility(View.GONE);
            btnMoreMenu.setVisibility(View.GONE);
            bindCurrentCover(null);
            return;
        }

        currentStory = story;
        persistSelectedStory(story);
        tvCurrentNovelTitle.setText(story.getTitle());
        tvCurrentNovelStats.setText(buildStatsText(story));
        tvCurrentNovelLastEdit.setText(getString(R.string.home_current_novel_last_edit_format, formatLastEditTime(story.getLastEditTime())));
        tvCurrentCoverCategory.setVisibility(View.VISIBLE);
        tvCurrentCoverCategory.setText(TextUtils.isEmpty(story.getCategory()) ? getString(R.string.home_story_category_default) : story.getCategory());
        configurePrimaryAction(true);
        btnSwitchNovel.setVisibility(View.VISIBLE);
        btnMoreMenu.setVisibility(View.VISIBLE);
        bindCurrentCover(story);
    }

    private Story resolveCurrentStory() {
        String selectedId = PrefsUtils.getInstance(requireContext()).getString(StoryAdapter.PREF_SELECTED_STORY_ID, "");
        Story story = null;

        if (!TextUtils.isEmpty(selectedId)) {
            try {
                story = storyDao.getStoryById(Integer.parseInt(selectedId));
            } catch (NumberFormatException ignored) {
                // fall back to latest story below
            }
        }

        if (story == null) {
            story = storyDao.getLatestStory();
        }

        return story;
    }

    private void configurePrimaryAction(boolean hasStory) {
        btnPrimaryAction.setVisibility(View.VISIBLE);
        if (hasStory) {
            btnPrimaryAction.setText(R.string.action_continue_edit);
            btnPrimaryAction.setOnClickListener(v -> continueEditingCurrentStory());
        } else {
            btnPrimaryAction.setText(R.string.action_create_story);
            btnPrimaryAction.setOnClickListener(v -> showCreateStoryDialog());
        }
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

    private void showCurrentNovelMoreMenu() {
        if (currentStory == null) {
            refreshCurrentNovel();
            if (currentStory == null) {
                Toast.makeText(requireContext(), "暂无可操作的当前小说", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        android.widget.PopupMenu popupMenu = new android.widget.PopupMenu(requireContext(), btnMoreMenu);
        popupMenu.getMenu().add("⭐ " + (currentStory.isCollected() ? "取消收藏" : "收藏"));
        popupMenu.getMenu().add("📷 上传封面");
        popupMenu.getMenu().add("📝 修改分类");
        popupMenu.getMenu().add("🗑️ 删除故事");

        popupMenu.setOnMenuItemClickListener(item -> {
            String title = String.valueOf(item.getTitle());
            if (title.contains("收藏")) {
                toggleFavoriteCurrentStory();
                return true;
            } else if (title.contains("上传封面")) {
                selectCoverImage();
                return true;
            } else if (title.contains("修改分类")) {
                showCategoryChangeDialog(currentStory);
                return true;
            } else if (title.contains("删除")) {
                showDeleteConfirmDialog(currentStory);
                return true;
            }
            return false;
        });

        popupMenu.show();
    }

    private void toggleFavoriteCurrentStory() {
        if (currentStory == null || storyDao == null) {
            return;
        }

        boolean newCollected = !currentStory.isCollected();
        storyDao.updateStoryCollected(currentStory.getId(), newCollected);
        currentStory.setCollected(newCollected);

        Toast.makeText(requireContext(), newCollected ? "已收藏" : "已取消收藏", Toast.LENGTH_SHORT).show();
        refreshCurrentNovel();
    }

    private void selectCoverImage() {
        if (currentStory == null) {
            return;
        }

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        if (selectCoverLauncher != null) {
            selectCoverLauncher.launch(Intent.createChooser(intent, "选择封面图片"));
        }
    }

    private void saveCoverImage(Uri imageUri) {
        if (currentStory == null || storyDao == null) {
            return;
        }

        try {
            InputStream inputStream = requireContext().getContentResolver().openInputStream(imageUri);
            if (inputStream == null) {
                Toast.makeText(requireContext(), "无法读取图片", Toast.LENGTH_SHORT).show();
                return;
            }

            File coverDir = new File(requireContext().getFilesDir(), "covers");
            if (!coverDir.exists() && !coverDir.mkdirs()) {
                Toast.makeText(requireContext(), "无法创建封面目录", Toast.LENGTH_SHORT).show();
                inputStream.close();
                return;
            }

            String fileName = "cover_" + currentStory.getId() + "_" + System.currentTimeMillis() + ".jpg";
            File coverFile = new File(coverDir, fileName);

            FileOutputStream outputStream = new FileOutputStream(coverFile);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();
            outputStream.close();
            inputStream.close();

            String coverPath = coverFile.getAbsolutePath();
            currentStory.setCoverPath(coverPath);
            storyDao.updateStoryCoverPath(currentStory.getId(), coverPath);

            Toast.makeText(requireContext(), "封面已更新", Toast.LENGTH_SHORT).show();
            refreshCurrentNovel();
        } catch (Exception e) {
            Log.w(TAG, "保存封面失败", e);
            Toast.makeText(requireContext(), "保存封面失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void showCategoryChangeDialog(Story story) {
        if (story == null || storyDao == null) {
            return;
        }

        String[] categories = {"创作中", "已完成"};
        new AlertDialog.Builder(requireContext())
            .setTitle("修改分类")
            .setItems(categories, (dialog, which) -> {
                String newCategory = categories[which];
                storyDao.updateStoryCategory(story.getId(), newCategory);
                story.setCategory(newCategory);

                Toast.makeText(requireContext(), "已修改为：" + newCategory, Toast.LENGTH_SHORT).show();
                refreshCurrentNovel();
            })
            .show();
    }

    private void showDeleteConfirmDialog(Story story) {
        if (story == null || storyDao == null) {
            return;
        }

        new AlertDialog.Builder(requireContext())
            .setTitle("删除故事")
            .setMessage("确定要删除《" + story.getTitle() + "》吗？")
            .setPositiveButton("删除", (dialog, which) -> {
                String currentSelectedId = PrefsUtils.getInstance(requireContext()).getString(StoryAdapter.PREF_SELECTED_STORY_ID, "");
                boolean isDeletingSelectedStory = !TextUtils.isEmpty(currentSelectedId) && currentSelectedId.equals(String.valueOf(story.getId()));

                int result = storyDao.deleteStory(story.getId());
                if (result > 0) {
                    if (isDeletingSelectedStory) {
                        Story fallbackStory = storyDao.getLatestStory();
                        if (fallbackStory != null) {
                            persistSelectedStory(fallbackStory);
                            currentStory = fallbackStory;
                        } else {
                            clearSelectedStory();
                            currentStory = null;
                        }
                    }
                    Toast.makeText(requireContext(), "已删除", Toast.LENGTH_SHORT).show();
                    refreshCurrentNovel();
                    loadRecentStories();
                } else {
                    Toast.makeText(requireContext(), "删除失败", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    private void bindCurrentCover(Story story) {
        if (ivCurrentCoverImage == null || vCurrentCoverBackground == null || tvCurrentCoverCategory == null) {
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

        if (story == null) {
            tvCurrentCoverCategory.setVisibility(View.GONE);
        } else {
            tvCurrentCoverCategory.setVisibility(View.VISIBLE);
            tvCurrentCoverCategory.setText(TextUtils.isEmpty(story.getCategory()) ? getString(R.string.home_story_category_default) : story.getCategory());
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
            } catch (Exception ignored) {
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

    private void showSwitchNovelDialog() {
        if (storyDao == null) {
            return;
        }

        List<Story> stories = storyDao.getAllStories();
        if (stories == null || stories.isEmpty()) {
            Toast.makeText(requireContext(), "暂无小说，请先创建", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] storyTitles = new String[stories.size()];
        for (int i = 0; i < stories.size(); i++) {
            storyTitles[i] = stories.get(i).getTitle();
        }

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
            } catch (NumberFormatException ignored) {
                // ignore
            }
        }

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

    private void showNoNovelSelectedDialog() {
        if (storyDao == null) {
            return;
        }

        List<Story> stories = storyDao.getAllStories();
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("提示");

        if (stories == null || stories.isEmpty()) {
            builder.setMessage("您还没有创建任何小说。\n是否要创建一个新小说？");
            builder.setPositiveButton("创建新小说", (dialog, which) -> {
                showCreateStoryDialog();
                dialog.dismiss();
            });
            builder.setNegativeButton("取消", null);
        } else {
            builder.setMessage("您还未选择当前小说。\n请选择一个小说或创建新小说。");

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
                showCreateStoryDialog();
                dialog.dismiss();
            });

            builder.setNegativeButton("取消", null);
        }

        builder.show();
    }

    private void showCreateStoryDialog() {
        CreateStoryDialog dialog = CreateStoryDialog.newInstance();
        dialog.setOnCreateStoryListener(this::createStory);
        dialog.show(getParentFragmentManager(), "create_story_dialog_home");
    }

    private void createStory(String title, String seriesName, String description) {
        if (storyDao == null) {
            return;
        }

        Story newStory = new Story(title, "", "创作", System.currentTimeMillis());
        if (!TextUtils.isEmpty(description)) {
            newStory.setDescription(description);
        }
        if (!TextUtils.isEmpty(seriesName)) {
            newStory.setSeriesName(seriesName);
        }

        List<Volume> volumes = new java.util.ArrayList<>();
        Volume volume = new Volume(1, "新卷名");
        com.example.storyteller.model.Chapter chapter = new com.example.storyteller.model.Chapter(1, "新章节", "");
        volume.addChapter(chapter);
        volumes.add(volume);

        newStory.setStructure(JsonUtils.toJson(volumes));

        long id = storyDao.insertStory(newStory);
        if (id <= 0) {
            Toast.makeText(requireContext(), "创建失败", Toast.LENGTH_SHORT).show();
            return;
        }

        com.example.storyteller.utils.PreferenceManager preferenceManager =
            com.example.storyteller.utils.PreferenceManager.getInstance(requireContext());
        preferenceManager.copyGlobalPreferenceToStory((int) id);

        newStory.setId((int) id);
        currentStory = newStory;
        persistSelectedStory(newStory);
        Toast.makeText(requireContext(), "创建成功", Toast.LENGTH_SHORT).show();
        refreshCurrentNovel();
        launchStoryWorkspace((int) id);
    }

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

    private void loadRecentStories() {
        if (storyDao == null || recentAdapter == null) {
            return;
        }

        List<Story> recentStories = storyDao.getRecentStories(3);
        List<Story> filteredRecentStories = new java.util.ArrayList<>();

        int selectedStoryId = -1;
        String selectedId = PrefsUtils.getInstance(requireContext()).getString(StoryAdapter.PREF_SELECTED_STORY_ID, "");
        if (!TextUtils.isEmpty(selectedId)) {
            try {
                selectedStoryId = Integer.parseInt(selectedId);
            } catch (NumberFormatException ignored) {
            }
        }

        if (recentStories != null) {
            for (Story story : recentStories) {
                if (story == null) {
                    continue;
                }
                if (story.getId() == selectedStoryId) {
                    continue;
                }
                filteredRecentStories.add(story);
                if (filteredRecentStories.size() >= 3) {
                    break;
                }
            }
        }

        if (filteredRecentStories.isEmpty()) {
            recentAdapter.setData(new java.util.ArrayList<>());
            tvRecentTitle.setVisibility(View.GONE);
            rvRecentStories.setVisibility(View.GONE);
        } else {
            tvRecentTitle.setVisibility(View.VISIBLE);
            rvRecentStories.setVisibility(View.VISIBLE);
            recentAdapter.setData(filteredRecentStories);
        }
    }
}
