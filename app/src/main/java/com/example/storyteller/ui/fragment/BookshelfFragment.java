package com.example.storyteller.ui.fragment;

import android.view.View;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.storyteller.R;
import com.example.storyteller.base.BaseFragment;
import com.example.storyteller.model.Story;
import com.example.storyteller.ui.adapter.StoryAdapter;
import java.util.ArrayList;
import java.util.List;

public class BookshelfFragment extends BaseFragment {

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_bookshelf;
    }

    @Override
    protected void initView(View view) {
        RecyclerView recyclerView = view.findViewById(R.id.rv_story_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        StoryAdapter adapter = new StoryAdapter(requireContext(), buildPlaceholderStories());
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void initData() {
        // 占位
    }

    private List<Story> buildPlaceholderStories() {
        List<Story> list = new ArrayList<>();
        long now = System.currentTimeMillis();
        list.add(new Story("遗落的星港", "一座漂浮的星港忽然失联，少女维修师独自追寻信号，发现它承载着一段被遗忘的文明记忆。", "科幻", now - 3600_000L));
        list.add(new Story("雾中来信", "一封没有署名的来信带出尘封往事，记者沿着线索深入迷雾小镇，真相在第四个清晨浮现。", "悬疑", now - 7200_000L));
        list.add(new Story("春日的玻璃屋", "植物学家与流浪画家在玻璃屋相遇，关于种子、光影与告别的故事悄然生长。", "治愈", now - 10_800_000L));
        list.add(new Story("北风尽头", "少年追随北风的指引踏上旅程，结识伙伴、穿越雪原，最终找到内心的归处。", "冒险", now - 14_400_000L));
        list.add(new Story("纸鸢与旧城", "旧城改造前夕，一只纸鸢牵出一段青涩友谊，记忆与现实在屋顶相遇。", "都市", now - 18_000_000L));
        return list;
    }
}
