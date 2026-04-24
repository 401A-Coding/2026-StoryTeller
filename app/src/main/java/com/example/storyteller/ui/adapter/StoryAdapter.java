package com.example.storyteller.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.storyteller.R;
import com.example.storyteller.model.Story;
import java.util.List;

// 故事卡片列表适配器（占位，后续完善）
public class StoryAdapter extends RecyclerView.Adapter<StoryAdapter.StoryViewHolder> {

    private final Context context;
    private List<Story> storyList;

    public StoryAdapter(Context context, List<Story> storyList) {
        this.context = context;
        this.storyList = storyList;
    }

    @NonNull
    @Override
    public StoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_story, parent, false);
        return new StoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StoryViewHolder holder, int position) {
        Story story = storyList.get(position);
        // 后续绑定数据
        // holder.tvTitle.setText(story.getTitle());
    }

    @Override
    public int getItemCount() {
        return storyList == null ? 0 : storyList.size();
    }

    // 更新数据
    public void setData(List<Story> list) {
        this.storyList = list;
        notifyDataSetChanged();
    }

    // ViewHolder 占位
    public static class StoryViewHolder extends RecyclerView.ViewHolder {
        // 示例控件，后续根据布局修改
        TextView tvTitle;
        TextView tvContent;

        public StoryViewHolder(@NonNull View itemView) {
            super(itemView);
            // tvTitle = itemView.findViewById(R.id.tv_title);
            // tvContent = itemView.findViewById(R.id.tv_content);
        }
    }
}