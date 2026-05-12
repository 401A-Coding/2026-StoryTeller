package com.example.storyteller.ui.fragment;

import android.content.Intent;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import com.example.storyteller.R;
import com.example.storyteller.base.BaseFragment;
import com.example.storyteller.data.local.db.StoryDao;
import com.example.storyteller.data.local.prefs.PrefsUtils;
import com.example.storyteller.model.Story;
import com.example.storyteller.ui.activity.CharacterActivity;
import com.example.storyteller.ui.activity.MaterialActivity;
import com.example.storyteller.ui.activity.PlotTreeActivity;
import com.example.storyteller.ui.activity.StoryWorkspaceActivity;
import com.example.storyteller.ui.adapter.StoryAdapter;
import java.util.List;

public class HomeFragment extends BaseFragment {

    private TextView tvCurrentNovel;
    private StoryDao storyDao;

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_home;
    }

    @Override
    protected void initView(View view) {
        // 功能卡片点击
        view.findViewById(R.id.card_story_generate)
            .setOnClickListener(v -> {
                // 获取当前选中的小说ID
                String selectedId = PrefsUtils.getInstance(requireContext()).getString(StoryAdapter.PREF_SELECTED_STORY_ID, "");
                
                android.util.Log.d("HomeFragment", "点击故事生成，selectedId: '" + selectedId + "'");
                android.util.Log.d("HomeFragment", "isEmpty: " + TextUtils.isEmpty(selectedId));
                
                if (TextUtils.isEmpty(selectedId)) {
                    // 未选择小说，弹出对话框让用户选择
                    android.util.Log.d("HomeFragment", "显示未选择对话框");
                    showNoNovelSelectedDialog();
                } else {
                    // 已选择小说，验证小说是否存在
                    try {
                        int storyId = Integer.parseInt(selectedId);
                        Story story = storyDao.getStoryById(storyId);
                        
                        if (story == null) {
                            // 小说不存在，清除选择状态并提示
                            android.util.Log.w("HomeFragment", "小说不存在，ID: " + storyId + "，清除选择状态");
                            PrefsUtils.getInstance(requireContext()).putString(StoryAdapter.PREF_SELECTED_STORY_ID, "");
                            PrefsUtils.getInstance(requireContext()).putString(StoryAdapter.PREF_SELECTED_STORY_TITLE, "");
                            
                            Toast.makeText(requireContext(), "所选小说不存在，请重新选择", Toast.LENGTH_SHORT).show();
                            refreshCurrentNovel();
                            showNoNovelSelectedDialog();
                        } else {
                            // 小说存在，正常进入编辑页面
                            android.util.Log.d("HomeFragment", "进入编辑页面，story_id: " + storyId);
                            Intent intent = new Intent(requireContext(), StoryWorkspaceActivity.class);
                            intent.putExtra(StoryWorkspaceActivity.EXTRA_STORY_ID, storyId);
                            startActivity(intent);
                        }
                    } catch (NumberFormatException e) {
                        // ID 格式错误，清除选择状态
                        android.util.Log.e("HomeFragment", "无效的 story_id: " + selectedId, e);
                        PrefsUtils.getInstance(requireContext()).putString(StoryAdapter.PREF_SELECTED_STORY_ID, "");
                        PrefsUtils.getInstance(requireContext()).putString(StoryAdapter.PREF_SELECTED_STORY_TITLE, "");
                        
                        Toast.makeText(requireContext(), "数据异常，请重新选择小说", Toast.LENGTH_SHORT).show();
                        refreshCurrentNovel();
                        showNoNovelSelectedDialog();
                    }
                }
            });
        view.findViewById(R.id.card_character)
            .setOnClickListener(v -> startActivity(new Intent(requireContext(), CharacterActivity.class)));
        view.findViewById(R.id.card_plot_tree)
            .setOnClickListener(v -> startActivity(new Intent(requireContext(), PlotTreeActivity.class)));
        view.findViewById(R.id.card_material)
            .setOnClickListener(v -> startActivity(new Intent(requireContext(), MaterialActivity.class)));

        view.findViewById(R.id.btn_switch_novel).setOnClickListener(v -> {
            showSwitchNovelDialog();
        });

        // 本地数据存储演示（直接写入测试故事，不走 AI）
        tvCurrentNovel = view.findViewById(R.id.tv_current_novel);
    }

    @Override
    protected void initData() {
        storyDao = new StoryDao(requireContext());
        
        // 调试：打印当前选择状态
        String selectedId = PrefsUtils.getInstance(requireContext()).getString(StoryAdapter.PREF_SELECTED_STORY_ID, "");
        android.util.Log.d("HomeFragment", "initData - 当前 selectedId: '" + selectedId + "'");
        List<Story> allStories = storyDao.getAllStories();
        android.util.Log.d("HomeFragment", "initData - 数据库中的小说数量: " + (allStories != null ? allStories.size() : 0));

        refreshCurrentNovel();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshCurrentNovel();
    }

    private void refreshCurrentNovel() {
        if (tvCurrentNovel == null) {
            return;
        }
        String selectedId = PrefsUtils.getInstance(requireContext()).getString(StoryAdapter.PREF_SELECTED_STORY_ID, "");
        if (selectedId == null || selectedId.isEmpty()) {
            tvCurrentNovel.setText(getString(R.string.home_current_novel_empty));
            return;
        }
        try {
            int storyId = Integer.parseInt(selectedId);
            Story story = storyDao.getStoryById(storyId);

            if (story == null) {
                tvCurrentNovel.setText(getString(R.string.home_current_novel_empty));
                return;
            }

            tvCurrentNovel.setText(getString(R.string.home_current_novel_format, story.getTitle()));
        } catch (NumberFormatException e) {
            tvCurrentNovel.setText(getString(R.string.home_current_novel_empty));
        }
    }

    /**
     * 显示切换小说对话框
     */
    private void showSwitchNovelDialog() {
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
            
            // Update selection
            PrefsUtils.getInstance(requireContext()).putString(StoryAdapter.PREF_SELECTED_STORY_ID, String.valueOf(selectedStory.getId()));
            PrefsUtils.getInstance(requireContext()).putString(StoryAdapter.PREF_SELECTED_STORY_TITLE, selectedStory.getTitle());
            
            // Refresh UI
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
                
                // 更新选择
                PrefsUtils.getInstance(requireContext()).putString(StoryAdapter.PREF_SELECTED_STORY_ID, String.valueOf(selectedStory.getId()));
                PrefsUtils.getInstance(requireContext()).putString(StoryAdapter.PREF_SELECTED_STORY_TITLE, selectedStory.getTitle());
                
                // 刷新 UI
                refreshCurrentNovel();
                
                // 进入编辑页面
                Intent intent = new Intent(requireContext(), StoryWorkspaceActivity.class);
                intent.putExtra(StoryWorkspaceActivity.EXTRA_STORY_ID, selectedStory.getId());
                startActivity(intent);
                
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
                Toast.makeText(requireContext(), "创建成功", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                refreshCurrentNovel();

                // Navigate directly to edit page
                Intent intent = new Intent(requireContext(), StoryWorkspaceActivity.class);
                intent.putExtra(StoryWorkspaceActivity.EXTRA_STORY_ID, (int) id);
                startActivity(intent);
            } else {
                Toast.makeText(requireContext(), "创建失败", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }
}
