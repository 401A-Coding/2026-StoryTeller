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
import com.example.storyteller.model.Story;
import com.example.storyteller.ui.activity.StoryGenerateActivity;
import com.example.storyteller.ui.adapter.StoryAdapter;
import java.util.List;

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
        adapter = new StoryAdapter(requireContext(), storyDao.getAllStories());
        
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
        adapter.setData(stories);
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
            long id = storyDao.insertStory(newStory);
            
            if (id > 0) {
                Toast.makeText(requireContext(), "创建成功", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                refreshStories();
                
                // Navigate directly to edit page
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
