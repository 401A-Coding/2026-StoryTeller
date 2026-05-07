package com.example.storyteller.ui.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.storyteller.R;
import com.example.storyteller.model.Story;
import com.example.storyteller.ui.activity.StoryPreviewActivity;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;

public class StoryAdapter extends RecyclerView.Adapter<StoryAdapter.StoryViewHolder> {

    private final Context context;
    private List<Story> storyList;
    private final DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);

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
        holder.tvTitle.setText(story.getTitle());
        holder.tvTime.setText(dateFormat.format(new Date(story.getCreateTime())));
        holder.tvContent.setText(story.getContent());
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, StoryPreviewActivity.class);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return storyList == null ? 0 : storyList.size();
    }

    public void setData(List<Story> list) {
        this.storyList = list;
        notifyDataSetChanged();
    }

    public static class StoryViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        TextView tvTime;
        TextView tvContent;

        public StoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvTime = itemView.findViewById(R.id.tv_time);
            tvContent = itemView.findViewById(R.id.tv_content);
        }
    }
}
