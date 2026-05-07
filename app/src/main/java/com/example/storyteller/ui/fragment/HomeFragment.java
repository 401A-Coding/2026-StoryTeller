package com.example.storyteller.ui.fragment;

import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.storyteller.R;
import com.example.storyteller.base.BaseFragment;
import com.example.storyteller.data.local.db.StoryDao;
import com.example.storyteller.model.Story;
import com.example.storyteller.ui.activity.CharacterActivity;
import com.example.storyteller.ui.activity.MaterialActivity;
import com.example.storyteller.ui.activity.PlotTreeActivity;
import com.example.storyteller.ui.activity.StoryGenerateActivity;
import com.example.storyteller.ui.adapter.StoryAdapter;
import java.util.List;

public class HomeFragment extends BaseFragment {

    private RecyclerView rvStory;
    private Button btnAdd;
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
            .setOnClickListener(v -> startActivity(new Intent(requireContext(), StoryGenerateActivity.class)));
        view.findViewById(R.id.card_character)
            .setOnClickListener(v -> startActivity(new Intent(requireContext(), CharacterActivity.class)));
        view.findViewById(R.id.card_plot_tree)
            .setOnClickListener(v -> startActivity(new Intent(requireContext(), PlotTreeActivity.class)));
        view.findViewById(R.id.card_material)
            .setOnClickListener(v -> startActivity(new Intent(requireContext(), MaterialActivity.class)));

        // 组员3：本地数据存储演示
        rvStory = view.findViewById(R.id.rv_story);
        btnAdd = view.findViewById(R.id.btn_add_demo_story);

        rvStory.setLayoutManager(new LinearLayoutManager(getContext()));

        btnAdd.setOnClickListener(v -> {
            String title = "演示故事 " + System.currentTimeMillis();
            String content = "这是由组员3本地存储模块自动生成的演示内容。";
            String genre = "通用";
            Story story = new Story(title, content, genre, System.currentTimeMillis());
            long id = storyDao.insertStory(story);
            if (id > 0) {
                Toast.makeText(getContext(), "新增成功，ID=" + id, Toast.LENGTH_SHORT).show();
                refreshList();
            } else {
                Toast.makeText(getContext(), "新增失败", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void initData() {
        storyDao = new StoryDao(requireContext());
        refreshList();
    }

    private void refreshList() {
        List<Story> stories = storyDao.getAllStories();
        if (adapter == null) {
            adapter = new StoryAdapter(requireContext(), stories);
            rvStory.setAdapter(adapter);
        } else {
            adapter.setData(stories);
        }
    }
}
