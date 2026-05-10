package com.example.storyteller.ui.fragment;

import android.view.View;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.storyteller.R;
import com.example.storyteller.base.BaseFragment;
import com.example.storyteller.data.local.db.StoryDao;
import com.example.storyteller.model.Story;
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
        recyclerView.setAdapter(adapter);
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
}
