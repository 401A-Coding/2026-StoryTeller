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
        View view = LayoutInflater.from(context).inflate(R.layout.item_story_new, parent, false);
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

        // 封面状态标签
        String category = story.getCategory();
        if (TextUtils.isEmpty(category)) {
            category = "创作中";
        }
        holder.tvCoverCategory.setText(category);

        // 标题
        holder.tvTitle.setText(story.getTitle());

        // 简介（如果有）
        String description = story.getDescription();
        if (!TextUtils.isEmpty(description)) {
            holder.tvDescription.setVisibility(View.VISIBLE);
            holder.tvDescription.setText(description);
        } else {
            holder.tvDescription.setVisibility(View.GONE);
        }

        // 统计信息：卷/章/字数
        String statsText = buildStatsText(story);
        holder.tvStats.setText(statsText);
        
        // 时间信息
        String timeText = dateFormat.format(new Date(story.getCreateTime()));
        holder.tvTime.setText(timeText);

        // 点击进入编辑页面
        holder.itemView.setOnClickListener(v -> {
            PrefsUtils.getInstance(context).putString(PREF_SELECTED_STORY_ID, String.valueOf(story.getId()));
            PrefsUtils.getInstance(context).putString(PREF_SELECTED_STORY_TITLE, story.getTitle());
            Intent intent = new Intent(context, StoryWorkspaceActivity.class);
            intent.putExtra(StoryWorkspaceActivity.EXTRA_STORY_ID, story.getId());
            context.startActivity(intent);
        });

        // 三点菜单按钮
        holder.btnMoreMenu.setOnClickListener(v -> {
            showMoreMenu(v, story, position);
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
     * 构建统计信息文本
     */
    private String buildStatsText(Story story) {
        // 从 structure 中解析卷数和章数
        int volumeCount = 0;
        int chapterCount = 0;
        
        String structureJson = story.getStructure();
        if (!TextUtils.isEmpty(structureJson)) {
            try {
                java.util.List<com.example.storyteller.model.Volume> volumes = 
                    com.example.storyteller.utils.JsonUtils.fromJson(structureJson,
                        new com.google.gson.reflect.TypeToken<java.util.List<com.example.storyteller.model.Volume>>(){}.getType());
                
                if (volumes != null) {
                    volumeCount = volumes.size();
                    for (com.example.storyteller.model.Volume volume : volumes) {
                        if (volume.getChapters() != null) {
                            chapterCount += volume.getChapters().size();
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        int wordCount = story.getWordCount();
        String wordCountText = formatWordCount(wordCount);
        
        return "📚 " + volumeCount + "卷 · " + chapterCount + "章 · 📖 " + wordCountText;
    }
    
    /**
     * 格式化字数
     */
    private String formatWordCount(int wordCount) {
        if (wordCount < 10000) {
            return wordCount + "字";
        } else {
            double wan = wordCount / 10000.0;
            return String.format("%.1f万字", wan);
        }
    }
    
    /**
     * 显示更多操作菜单
     */
    private void showMoreMenu(View anchorView, Story story, int position) {
        android.widget.PopupMenu popupMenu = new android.widget.PopupMenu(context, anchorView);
        popupMenu.getMenu().add("⭐ " + (story.isCollected() ? "取消收藏" : "收藏"));
        popupMenu.getMenu().add("📷 上传封面");
        popupMenu.getMenu().add("📝 修改分类");
        popupMenu.getMenu().add("🗑️ 删除故事");
        
        popupMenu.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();
            if (title.contains("收藏")) {
                toggleFavorite(story);
                return true;
            } else if (title.contains("上传封面")) {
                if (pickCoverImageListener != null) {
                    pickCoverImageListener.onPickCoverImage(story.getId());
                }
                return true;
            } else if (title.contains("修改分类")) {
                showCategoryChangeDialog(story);
                return true;
            } else if (title.contains("删除")) {
                showDeleteConfirmDialog(story, position);
                return true;
            }
            return false;
        });
        
        popupMenu.show();
    }
    
    /**
     * 切换收藏状态
     */
    private void toggleFavorite(Story story) {
        boolean newCollected = !story.isCollected();
        StoryDao storyDao = new StoryDao(context);
        storyDao.updateStoryCollected(story.getId(), newCollected);
        story.setCollected(newCollected);
        
        Toast.makeText(context, newCollected ? "已收藏" : "已取消收藏", Toast.LENGTH_SHORT).show();
        
        // 通知刷新
        if (categoryChangeListener != null) {
            categoryChangeListener.onCategoryChanged(story.getId(), story.getCategory());
        }
    }
    
    /**
     * 显示修改分类对话框
     */
    private void showCategoryChangeDialog(Story story) {
        String[] categories = {"创作中", "已完成"};
        new AlertDialog.Builder(context)
            .setTitle("修改分类")
            .setItems(categories, (dialog, which) -> {
                String newCategory = categories[which];
                StoryDao storyDao = new StoryDao(context);
                storyDao.updateStoryCategory(story.getId(), newCategory);
                story.setCategory(newCategory);
                
                Toast.makeText(context, "已修改为：" + newCategory, Toast.LENGTH_SHORT).show();
                
                // 通知刷新
                if (categoryChangeListener != null) {
                    categoryChangeListener.onCategoryChanged(story.getId(), newCategory);
                }
            })
            .show();
    }
    
    /**
     * 显示删除确认对话框
     */
    private void showDeleteConfirmDialog(Story story, int position) {
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
        TextView tvCoverCategory;
        ImageButton btnMoreMenu;
        TextView tvTitle;
        TextView tvDescription;
        TextView tvStats;
        TextView tvTime;

        public StoryViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutCover = itemView.findViewById(R.id.layout_cover);
            vCoverBackground = itemView.findViewById(R.id.v_cover_background);
            ivCoverImage = itemView.findViewById(R.id.iv_cover_image);
            tvCoverCategory = itemView.findViewById(R.id.tv_cover_category);
            btnMoreMenu = itemView.findViewById(R.id.btn_more_menu);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvDescription = itemView.findViewById(R.id.tv_description);
            tvStats = itemView.findViewById(R.id.tv_stats);
            tvTime = itemView.findViewById(R.id.tv_time);
        }
    }
}