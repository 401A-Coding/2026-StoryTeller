package com.example.storyteller.ui.adapter;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
import com.example.storyteller.R;
import com.example.storyteller.data.local.db.StoryDao;
import com.example.storyteller.data.local.prefs.PrefsUtils;
import com.example.storyteller.model.Story;
import com.example.storyteller.ui.activity.StoryDetailActivity;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;

public class StoryAdapter extends RecyclerView.Adapter<StoryAdapter.StoryViewHolder> {

    public static final String PREF_SELECTED_STORY_ID = "selected_story_id";
    public static final String PREF_SELECTED_STORY_TITLE = "selected_story_title";
    public static final String EXTRA_STORY_ID = "extra_story_id";
    public static final String EXTRA_STORY_TITLE = "extra_story_title";

    private final Context context;
    private List<Story> storyList;
    private final DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);
    private OnStoryDeleteListener deleteListener;

    public interface OnStoryDeleteListener {
        void onStoryDeleted(int storyId);
    }

    public void setOnStoryDeleteListener(OnStoryDeleteListener listener) {
        this.deleteListener = listener;
    }

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
        String seriesName = TextUtils.isEmpty(story.getGenre()) ? "创作" : story.getGenre().trim();
        holder.tvVolumes.setText(context.getString(R.string.story_series_format, seriesName));
        holder.tvVolumes.setVisibility(View.VISIBLE);

        String description = story.getDescription();
        if (TextUtils.isEmpty(description)) {
            holder.tvChapters.setText(context.getString(R.string.story_description_empty));
        } else {
            holder.tvChapters.setText(context.getString(R.string.story_description_format, description.trim()));
        }
        holder.tvChapters.setVisibility(View.VISIBLE);

        // Check if this is the currently selected story
        String selectedId = PrefsUtils.getInstance(context).getString(PREF_SELECTED_STORY_ID, "");
        boolean isSelected = !TextUtils.isEmpty(selectedId) && selectedId.equals(String.valueOf(story.getId()));
        
        // Highlight the selected story
        if (isSelected) {
            holder.itemView.setBackgroundColor(context.getResources().getColor(R.color.colorPrimaryLight, null));
            holder.tvTitle.setTextColor(context.getResources().getColor(R.color.colorPrimary, null));
        } else {
            holder.itemView.setBackgroundColor(context.getResources().getColor(android.R.color.transparent, null));
            holder.tvTitle.setTextColor(context.getResources().getColor(android.R.color.black, null));
        }
        
        // Click to edit story directly
        holder.itemView.setOnClickListener(v -> {
            PrefsUtils.getInstance(context).putString(PREF_SELECTED_STORY_ID, String.valueOf(story.getId()));
            PrefsUtils.getInstance(context).putString(PREF_SELECTED_STORY_TITLE, story.getTitle());
            Intent intent = new Intent(context, StoryDetailActivity.class);
            intent.putExtra(EXTRA_STORY_ID, story.getId());
            intent.putExtra(EXTRA_STORY_TITLE, story.getTitle());
            context.startActivity(intent);
        });
        
        // Delete button click
        holder.btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                .setTitle("删除故事")
                .setMessage("确定要删除《" + story.getTitle() + "》吗？")
                .setPositiveButton("删除", (dialog, which) -> {
                    StoryDao storyDao = new StoryDao(context);
                    
                    // 检查是否删除的是当前选中的小说
                    String currentSelectedId = PrefsUtils.getInstance(context).getString(PREF_SELECTED_STORY_ID, "");
                    boolean isDeletingSelectedStory = !TextUtils.isEmpty(currentSelectedId) && 
                        currentSelectedId.equals(String.valueOf(story.getId()));
                    
                    int result = storyDao.deleteStory(story.getId());
                    if (result > 0) {
                        // 如果删除的是当前选中的小说，清除选择状态
                        if (isDeletingSelectedStory) {
                            PrefsUtils.getInstance(context).putString(PREF_SELECTED_STORY_ID, "");
                            PrefsUtils.getInstance(context).putString(PREF_SELECTED_STORY_TITLE, "");
                            android.util.Log.d("StoryAdapter", "删除了当前选中的小说，已清除 selectedId");
                        }
                        
                        Toast.makeText(context, "已删除", Toast.LENGTH_SHORT).show();
                        if (deleteListener != null) {
                            deleteListener.onStoryDeleted(story.getId());
                        }
                    } else {
                        Toast.makeText(context, "删除失败", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
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
        TextView tvVolumes;
        TextView tvChapters;
        ImageView btnDelete;

        public StoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvTime = itemView.findViewById(R.id.tv_time);
            tvVolumes = itemView.findViewById(R.id.tv_volumes);
            tvChapters = itemView.findViewById(R.id.tv_chapters);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }
}
