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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 最近编辑小说适配器
 */
public class RecentStoryAdapter extends RecyclerView.Adapter<RecentStoryAdapter.ViewHolder> {

    private Context context;
    private List<Story> stories;
    private OnStoryClickListener listener;

    public interface OnStoryClickListener {
        void onStoryClick(Story story);
    }

    public RecentStoryAdapter(Context context, List<Story> stories) {
        this.context = context;
        this.stories = stories != null ? stories : new ArrayList<>();
    }

    public void setOnStoryClickListener(OnStoryClickListener listener) {
        this.listener = listener;
    }

    public void setData(List<Story> stories) {
        this.stories = stories != null ? stories : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_recent_story, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Story story = stories.get(position);
        
        // 设置标题
        holder.tvTitle.setText(story.getTitle());
        
        // 格式化最后编辑时间
        holder.tvLastEditTime.setText(formatLastEditTime(story.getLastEditTime()));
        
        // 显示字数
        holder.tvWordCount.setText(story.getWordCount() + " 字");
        
        // 点击事件
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onStoryClick(story);
            }
        });
    }

    @Override
    public int getItemCount() {
        return stories.size();
    }

    /**
     * 格式化最后编辑时间
     */
    private String formatLastEditTime(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;
        
        if (diff < 60 * 1000) {
            return "刚刚";
        } else if (diff < 60 * 60 * 1000) {
            return (diff / (60 * 1000)) + "分钟前";
        } else if (diff < 24 * 60 * 60 * 1000) {
            return (diff / (60 * 60 * 1000)) + "小时前";
        } else if (diff < 7 * 24 * 60 * 60 * 1000) {
            return (diff / (24 * 60 * 60 * 1000)) + "天前";
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("MM-dd", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        TextView tvLastEditTime;
        TextView tvWordCount;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_story_title);
            tvLastEditTime = itemView.findViewById(R.id.tv_last_edit_time);
            tvWordCount = itemView.findViewById(R.id.tv_word_count);
        }
    }
}
