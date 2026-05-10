package com.example.storyteller.ui.fragment;

import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
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
import com.example.storyteller.ui.activity.MainActivity;
import com.example.storyteller.ui.activity.CharacterActivity;
import com.example.storyteller.ui.activity.MaterialActivity;
import com.example.storyteller.ui.activity.PlotTreeActivity;
import com.example.storyteller.ui.activity.StoryGenerateActivity;
import com.example.storyteller.ui.adapter.StoryAdapter;
import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends BaseFragment {

    private RecyclerView rvStory;
    private TextView tvCurrentNovel;
    private StoryDao storyDao;
    private StoryAdapter adapter;

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
                Intent intent = new Intent(requireContext(), StoryGenerateActivity.class);
                if (!TextUtils.isEmpty(selectedId)) {
                    try {
                        intent.putExtra("story_id", Integer.parseInt(selectedId));
                    } catch (NumberFormatException e) {
                        // 忽略无效的ID
                    }
                }
                startActivity(intent);
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
        rvStory = view.findViewById(R.id.rv_story);
        tvCurrentNovel = view.findViewById(R.id.tv_current_novel);

        rvStory.setLayoutManager(new LinearLayoutManager(getContext()));
    }

    @Override
    protected void initData() {
        storyDao = new StoryDao(requireContext());
        refreshList();
        refreshCurrentNovel();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshList();
        refreshCurrentNovel();
    }

    private void refreshList() {
        // Get the currently selected story ID
        String selectedId = PrefsUtils.getInstance(requireContext()).getString(StoryAdapter.PREF_SELECTED_STORY_ID, "");
        
        List<Story> stories;
        if (!TextUtils.isEmpty(selectedId)) {
            // Only show the selected story
            try {
                int storyId = Integer.parseInt(selectedId);
                Story selectedStory = storyDao.getStoryById(storyId);
                if (selectedStory != null) {
                    stories = new java.util.ArrayList<>();
                    stories.add(selectedStory);
                } else {
                    stories = new java.util.ArrayList<>();
                }
            } catch (NumberFormatException e) {
                stories = new java.util.ArrayList<>();
            }
        } else {
            // No selection, show empty list
            stories = new java.util.ArrayList<>();
        }
        
        if (adapter == null) {
            adapter = new StoryAdapter(requireContext(), stories);
            
            // Set delete listener
            adapter.setOnStoryDeleteListener(storyId -> {
                refreshList();
                refreshCurrentNovel();
            });
            
            rvStory.setAdapter(adapter);
        } else {
            adapter.setData(stories);
        }
    }

    private void refreshCurrentNovel() {
        if (tvCurrentNovel == null) {
            return;
        }
        String selectedId = PrefsUtils.getInstance(requireContext()).getString(StoryAdapter.PREF_SELECTED_STORY_ID, "");
        if (selectedId == null || selectedId.isEmpty()) {
            tvCurrentNovel.setText("当前小说：未选择");
            return;
        }
        try {
            int storyId = Integer.parseInt(selectedId);
            Story story = storyDao.getStoryById(storyId);
            tvCurrentNovel.setText(story == null ? "当前小说：未选择" : "当前小说：" + story.getTitle());
        } catch (NumberFormatException e) {
            tvCurrentNovel.setText("当前小说：未选择");
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
            refreshList();
            refreshCurrentNovel();
            
            dialog.dismiss();
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }
}
