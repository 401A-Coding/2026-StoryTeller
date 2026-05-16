package com.example.storyteller.ui.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.storyteller.R;
import com.example.storyteller.model.ImportedNovel;

import org.json.JSONArray;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ImportedNovelAdapter extends RecyclerView.Adapter<ImportedNovelAdapter.ViewHolder> {

    public interface Listener {
        void onNovelClick(@NonNull ImportedNovel novel);
        void onNovelLongClick(@NonNull ImportedNovel novel);
    }

    private List<ImportedNovel> novels = new ArrayList<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
    private Listener listener;

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setData(List<ImportedNovel> novels) {
        this.novels = novels != null ? novels : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_imported_novel, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ImportedNovel novel = novels.get(position);
        
        // 封面图片
        if (novel.getCoverUrl() != null && !novel.getCoverUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                .load(novel.getCoverUrl())
                .placeholder(R.drawable.ic_menu_book)  // 占位图
                .error(R.drawable.ic_menu_book)  // 错误图
                .transition(DrawableTransitionOptions.withCrossFade())  // 淡入效果
                .into(holder.ivCover);
        } else {
            holder.ivCover.setImageResource(R.drawable.ic_menu_book);
        }
        
        // 标题
        holder.tvTitle.setText(novel.getTitle());
        
        // 作者
        holder.tvAuthor.setText(novel.getAuthor() != null ? "作者：" + novel.getAuthor() : "作者未知");
        
        // 状态标签
        if (novel.getStatus() != null && !novel.getStatus().equals("imported")) {
            holder.tvStatus.setVisibility(View.VISIBLE);
            if (novel.getStatus().equals("analyzing")) {
                holder.tvStatus.setText("分析中");
                holder.tvStatus.setBackgroundColor(Color.parseColor("#FF9800"));
            } else if (novel.getStatus().equals("analyzed")) {
                holder.tvStatus.setText("已分析");
                holder.tvStatus.setBackgroundColor(Color.parseColor("#4CAF50"));
            }
        } else {
            holder.tvStatus.setVisibility(View.GONE);
        }
        
        // 标签
        holder.llTags.removeAllViews();
        if (novel.getTags() != null && !novel.getTags().equals("[]")) {
            try {
                JSONArray tagsArray = new JSONArray(novel.getTags());
                for (int i = 0; i < tagsArray.length(); i++) {
                    String tag = tagsArray.getString(i);
                    TextView tagView = createTagView(holder, tag);
                    holder.llTags.addView(tagView);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        // 简介预览
        if (novel.getDescription() != null && !novel.getDescription().isEmpty()) {
            holder.tvDescription.setText(novel.getDescription());
            holder.tvDescription.setVisibility(View.VISIBLE);
        } else {
            holder.tvDescription.setVisibility(View.GONE);
        }
        
        // 统计信息
        int chapterCount = novel.getTotalChapters();
        int wordCount = novel.getTotalWords();
        
        // 如果章节数为0，显示"暂无章节"
        if (chapterCount == 0) {
            holder.tvChapterCount.setText("暂无章节");
            holder.tvWordCount.setText("0 字");
        } else {
            holder.tvChapterCount.setText(chapterCount + " 章");
            holder.tvWordCount.setText(formatWordCount(wordCount));
        }
        
        holder.tvImportTime.setText(dateFormat.format(new Date(novel.getImportTime())));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onNovelClick(novel);
            }
        });
        
        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onNovelLongClick(novel);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return novels.size();
    }

    private String formatWordCount(int wordCount) {
        if (wordCount <= 0) {
            return "0 字";
        } else if (wordCount < 10000) {
            return wordCount + " 字";
        } else if (wordCount < 100000) {
            // 1万-10万：显示为 X.X 万字
            return String.format(Locale.getDefault(), "%.1f 万字", wordCount / 10000.0);
        } else {
            // 10万以上：显示为 X 万字（取整）
            return String.format(Locale.getDefault(), "%d 万字", wordCount / 10000);
        }
    }

    /**
     * 创建标签视图
     */
    private TextView createTagView(ViewHolder holder, String tag) {
        TextView tagView = new TextView(holder.itemView.getContext());
        tagView.setText(tag);
        tagView.setTextSize(11);
        tagView.setTextColor(Color.parseColor("#1976D2"));
        tagView.setPadding(12, 4, 12, 4);
        tagView.setBackgroundResource(R.drawable.bg_tag_chip);
        
        // 设置右边距
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.rightMargin = 8;
        tagView.setLayoutParams(params);
        
        return tagView;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCover;
        TextView tvTitle;
        TextView tvAuthor;
        TextView tvStatus;
        LinearLayout llTags;
        TextView tvDescription;
        TextView tvChapterCount;
        TextView tvWordCount;
        TextView tvImportTime;

        ViewHolder(View itemView) {
            super(itemView);
            ivCover = itemView.findViewById(R.id.iv_cover);
            tvTitle = itemView.findViewById(R.id.tv_novel_title);
            tvAuthor = itemView.findViewById(R.id.tv_novel_author);
            tvStatus = itemView.findViewById(R.id.tv_status);
            llTags = itemView.findViewById(R.id.ll_tags);
            tvDescription = itemView.findViewById(R.id.tv_description);
            tvChapterCount = itemView.findViewById(R.id.tv_chapter_count);
            tvWordCount = itemView.findViewById(R.id.tv_word_count);
            tvImportTime = itemView.findViewById(R.id.tv_import_time);
        }
    }
}
