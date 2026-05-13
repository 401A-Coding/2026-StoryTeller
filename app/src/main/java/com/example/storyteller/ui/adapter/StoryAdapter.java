package com.example.storyteller.ui.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
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
import com.example.storyteller.ui.activity.StoryWorkspaceActivity;
import java.io.File;
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
    private OnStoryCategoryChangeListener categoryChangeListener;
    private OnStoryCoverChangeListener coverChangeListener;
    private OnPickCoverImageListener pickCoverImageListener;

    public interface OnStoryDeleteListener {
        void onStoryDeleted(int storyId);
    }

    public interface OnStoryCategoryChangeListener {
        void onCategoryChanged(int storyId, String newCategory);
    }

    public interface OnStoryCoverChangeListener {
        void onCoverChanged(int storyId, String newCoverColor);
    }

    public interface OnPickCoverImageListener {
        void onPickCoverImage(int storyId);
    }

    public void setOnStoryDeleteListener(OnStoryDeleteListener listener) {
        this.deleteListener = listener;
    }

    public void setOnStoryCategoryChangeListener(OnStoryCategoryChangeListener listener) {
        this.categoryChangeListener = listener;
    }

    public void setOnStoryCoverChangeListener(OnStoryCoverChangeListener listener) {
        this.coverChangeListener = listener;
    }

    public void setOnPickCoverImageListener(OnPickCoverImageListener listener) {
        this.pickCoverImageListener = listener;
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

        // 设置封面背景：优先显示用户上传的图片，否则显示渐变背景
        String coverPath = story.getCoverPath();
        if (!TextUtils.isEmpty(coverPath)) {
            File coverFile = new File(coverPath);
            if (coverFile.exists()) {
                holder.ivCoverImage.setVisibility(View.VISIBLE);
                holder.ivCoverImage.setImageBitmap(BitmapFactory.decodeFile(coverPath));
                // 隐藏渐变背景
                holder.vCoverBackground.setVisibility(View.GONE);
            } else {
                // 图片文件不存在，使用渐变背景
                holder.ivCoverImage.setVisibility(View.GONE);
                holder.vCoverBackground.setVisibility(View.VISIBLE);
                setCoverGradient(holder, story);
            }
        } else {
            // 没有上传图片，使用渐变背景
            holder.ivCoverImage.setVisibility(View.GONE);
            holder.vCoverBackground.setVisibility(View.VISIBLE);
            setCoverGradient(holder, story);
        }

        // 收藏按钮 - 始终显示，根据状态切换图标
        if (story.isCollected()) {
            holder.btnFavorite.setImageResource(R.drawable.ic_star_filled);
        } else {
            holder.btnFavorite.setImageResource(R.drawable.ic_star_border);
        }
        
        // 收藏按钮点击事件
        holder.btnFavorite.setOnClickListener(v -> {
            toggleFavorite(story, holder);
        });

        // 封面标题
        holder.tvCoverTitle.setText(story.getTitle());

        // 封面状态标签
        String category = story.getCategory();
        if (TextUtils.isEmpty(category)) {
            category = "创作中";
        }
        holder.tvCoverCategory.setText(category);

        // 底部信息
        holder.tvTitle.setText(story.getTitle());
        holder.tvTime.setText(dateFormat.format(new Date(story.getCreateTime())));

        // 点击进入编辑页面
        holder.itemView.setOnClickListener(v -> {
            PrefsUtils.getInstance(context).putString(PREF_SELECTED_STORY_ID, String.valueOf(story.getId()));
            PrefsUtils.getInstance(context).putString(PREF_SELECTED_STORY_TITLE, story.getTitle());
            Intent intent = new Intent(context, StoryWorkspaceActivity.class);
            intent.putExtra(StoryWorkspaceActivity.EXTRA_STORY_ID, story.getId());
            context.startActivity(intent);
        });

        // 删除按钮
        holder.btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                .setTitle("删除故事")
                .setMessage("确定要删除《" + story.getTitle() + "》吗？")
                .setPositiveButton("删除", (dialog, which) -> {
                    StoryDao storyDao = new StoryDao(context);

                    String currentSelectedId = PrefsUtils.getInstance(context).getString(PREF_SELECTED_STORY_ID, "");
                    boolean isDeletingSelectedStory = !TextUtils.isEmpty(currentSelectedId) &&
                        currentSelectedId.equals(String.valueOf(story.getId()));

                    int result = storyDao.deleteStory(story.getId());
                    if (result > 0) {
                        if (isDeletingSelectedStory) {
                            PrefsUtils.getInstance(context).putString(PREF_SELECTED_STORY_ID, "");
                            PrefsUtils.getInstance(context).putString(PREF_SELECTED_STORY_TITLE, "");
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

    /**
     * 设置封面渐变背景
     */
    private void setCoverGradient(StoryViewHolder holder, Story story) {
        String title = story.getTitle();
        int[] colors = getGradientColors(title);
        GradientDrawable gradient = new GradientDrawable();
        gradient.setOrientation(GradientDrawable.Orientation.TL_BR);
        gradient.setColors(colors);
        holder.vCoverBackground.setBackground(gradient);
    }
    
    /**
     * 根据标题生成渐变色（与ArchitectureFragment保持一致）
     */
    private int[] getGradientColors(String title) {
        if (title == null || title.isEmpty()) {
            return new int[]{0xFF1976D2, 0xFF42A5F5};
        }
        
        // 预定义的渐变色组合
        int[][] gradients = {
            {0xFF667eea, 0xFF764ba2}, // 紫蓝渐变
            {0xFFf093fb, 0xFFf5576c}, // 粉红渐变
            {0xFF4facfe, 0xFF00f2fe}, // 蓝青渐变
            {0xFF43e97b, 0xFF38f9d7}, // 绿青渐变
            {0xFFfa709a, 0xFFfee140}, // 粉黄渐变
            {0xFF30cfd0, 0xFF330867}, // 青紫渐变
            {0xFFa8edea, 0xFFfed6e3}, // 浅蓝粉渐变
            {0xFFff9a9e, 0xFFfecfef}, // 粉色渐变
        };
        
        int index = Math.abs(title.hashCode()) % gradients.length;
        return gradients[index];
    }
    
    /**
     * 切换收藏状态
     */
    private void toggleFavorite(Story story, StoryViewHolder holder) {
        boolean newCollected = !story.isCollected();
        StoryDao storyDao = new StoryDao(context);
        storyDao.updateStoryCollected(story.getId(), newCollected);
        story.setCollected(newCollected);
        
        // 更新图标
        if (newCollected) {
            holder.btnFavorite.setImageResource(R.drawable.ic_star_filled);
        } else {
            holder.btnFavorite.setImageResource(R.drawable.ic_star_border);
        }
        
        Toast.makeText(context, newCollected ? "已收藏" : "已取消收藏", Toast.LENGTH_SHORT).show();
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
        View layoutCover;
        View vCoverBackground;
        ImageView ivCoverImage;
        ImageButton btnFavorite;
        TextView tvCoverTitle;
        TextView tvCoverCategory;
        TextView tvTitle;
        TextView tvTime;
        ImageView btnDelete;

        public StoryViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutCover = itemView.findViewById(R.id.layout_cover);
            vCoverBackground = itemView.findViewById(R.id.v_cover_background);
            ivCoverImage = itemView.findViewById(R.id.iv_cover_image);
            btnFavorite = itemView.findViewById(R.id.btn_favorite);
            tvCoverTitle = itemView.findViewById(R.id.tv_cover_title);
            tvCoverCategory = itemView.findViewById(R.id.tv_cover_category);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvTime = itemView.findViewById(R.id.tv_time);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }
}