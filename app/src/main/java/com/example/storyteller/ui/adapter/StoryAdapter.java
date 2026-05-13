package com.example.storyteller.ui.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
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

        // 设置封面：优先显示用户上传的图片，否则显示颜色背景
        String coverPath = story.getCoverPath();
        if (!TextUtils.isEmpty(coverPath)) {
            File coverFile = new File(coverPath);
            if (coverFile.exists()) {
                holder.ivCoverImage.setVisibility(View.VISIBLE);
                holder.ivCoverImage.setImageBitmap(BitmapFactory.decodeFile(coverPath));
                // 设置颜色背景作为图片加载时的备用
                String coverColorStr = story.getCoverColor();
                if (TextUtils.isEmpty(coverColorStr)) {
                    coverColorStr = "#1976D2";
                }
                try {
                    holder.layoutCover.setBackgroundColor(Color.parseColor(coverColorStr));
                } catch (Exception e) {
                    holder.layoutCover.setBackgroundColor(Color.parseColor("#1976D2"));
                }
            } else {
                // 图片文件不存在，使用颜色背景
                holder.ivCoverImage.setVisibility(View.GONE);
                setCoverColor(holder, story);
            }
        } else {
            // 没有上传图片，使用颜色背景
            holder.ivCoverImage.setVisibility(View.GONE);
            setCoverColor(holder, story);
        }

        // 收藏五角星标记
        if (story.isCollected()) {
            holder.ivFavoriteStar.setVisibility(View.VISIBLE);
        } else {
            holder.ivFavoriteStar.setVisibility(View.GONE);
        }

        // 封面标题
        holder.tvCoverTitle.setText(story.getTitle());

        // 封面分类标签
        String category = story.getCategory();
        if (TextUtils.isEmpty(category)) {
            category = "创作中";
        }
        holder.tvCoverCategory.setText(category);

        // 底部信息
        holder.tvTitle.setText(story.getTitle());
        holder.tvTime.setText(dateFormat.format(new Date(story.getCreateTime())));

        // 长按显示操作菜单（修改分类/更换封面/上传封面图片）
        holder.itemView.setOnLongClickListener(v -> {
            showStoryActionMenu(story);
            return true;
        });

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

    private void setCoverColor(StoryViewHolder holder, Story story) {
        String coverColorStr = story.getCoverColor();
        if (TextUtils.isEmpty(coverColorStr)) {
            coverColorStr = "#1976D2";
        }
        try {
            int coverColor = Color.parseColor(coverColorStr);
            holder.layoutCover.setBackgroundColor(coverColor);
        } catch (Exception e) {
            holder.layoutCover.setBackgroundColor(Color.parseColor("#1976D2"));
        }
    }

    /**
     * 显示故事操作菜单（修改分类/收藏/上传封面图片）
     */
    private void showStoryActionMenu(Story story) {
        String favoriteText = story.isCollected() ? "取消收藏" : "收藏";
        String[] items = {
            context.getString(R.string.bookshelf_change_category),
            favoriteText,
            "上传封面图片"
        };
        new AlertDialog.Builder(context)
            .setTitle(story.getTitle())
            .setItems(items, (dialog, which) -> {
                if (which == 0) {
                    showCategoryDialog(story);
                } else if (which == 1) {
                    toggleFavorite(story);
                } else if (which == 2) {
                    if (pickCoverImageListener != null) {
                        pickCoverImageListener.onPickCoverImage(story.getId());
                    }
                }
            })
            .show();
    }

    /**
     * 切换收藏状态
     */
    private void toggleFavorite(Story story) {
        boolean newCollected = !story.isCollected();
        StoryDao storyDao = new StoryDao(context);
        storyDao.updateStoryCollected(story.getId(), newCollected);
        story.setCollected(newCollected);
        notifyDataSetChanged();
        Toast.makeText(context, newCollected ? "已收藏" : "已取消收藏", Toast.LENGTH_SHORT).show();
    }

    /**
     * 显示修改分类对话框
     */
    private void showCategoryDialog(Story story) {
        String[] categories = {
            context.getString(R.string.bookshelf_category_writing),
            context.getString(R.string.bookshelf_category_completed)
        };
        int currentIndex = 0;
        String currentCategory = story.getCategory();
        for (int i = 0; i < categories.length; i++) {
            if (categories[i].equals(currentCategory)) {
                currentIndex = i;
                break;
            }
        }
        new AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.bookshelf_category_hint))
            .setSingleChoiceItems(categories, currentIndex, (dialog, which) -> {
                String newCategory = categories[which];
                StoryDao storyDao = new StoryDao(context);
                storyDao.updateStoryCategory(story.getId(), newCategory);
                story.setCategory(newCategory);
                notifyDataSetChanged();
                if (categoryChangeListener != null) {
                    categoryChangeListener.onCategoryChanged(story.getId(), newCategory);
                }
                dialog.dismiss();
            })
            .setNegativeButton("取消", null)
            .show();
    }

    /**
     * 显示更换封面颜色对话框
     */
    private void showCoverColorDialog(Story story) {
        String[] colorNames = {"蓝色", "绿色", "橙色", "紫色", "玫红", "青色", "棕色", "灰蓝", "深橙", "靛蓝", "青绿", "黄绿"};
        String[] colorValues = {
            "#1976D2", "#388E3C", "#F57C00", "#7B1FA2",
            "#C2185B", "#0097A7", "#5D4037", "#455A64",
            "#E64A19", "#303F9F", "#00796B", "#AFB42B"
        };
        new AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.bookshelf_cover_color_hint))
            .setItems(colorNames, (dialog, which) -> {
                String newColor = colorValues[which];
                StoryDao storyDao = new StoryDao(context);
                storyDao.updateStoryCoverColor(story.getId(), newColor);
                story.setCoverColor(newColor);
                notifyDataSetChanged();
                if (coverChangeListener != null) {
                    coverChangeListener.onCoverChanged(story.getId(), newColor);
                }
                dialog.dismiss();
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
        ImageView ivCoverImage;
        ImageView ivFavoriteStar;
        TextView tvCoverTitle;
        TextView tvCoverCategory;
        TextView tvTitle;
        TextView tvTime;
        ImageView btnDelete;

        public StoryViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutCover = itemView.findViewById(R.id.layout_cover);
            ivCoverImage = itemView.findViewById(R.id.iv_cover_image);
            ivFavoriteStar = itemView.findViewById(R.id.iv_favorite_star);
            tvCoverTitle = itemView.findViewById(R.id.tv_cover_title);
            tvCoverCategory = itemView.findViewById(R.id.tv_cover_category);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvTime = itemView.findViewById(R.id.tv_time);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }
}