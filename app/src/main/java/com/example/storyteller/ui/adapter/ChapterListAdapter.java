package com.example.storyteller.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.storyteller.R;

import java.util.List;

/**
 * 章节列表适配器（简化版）
 */
public class ChapterListAdapter extends RecyclerView.Adapter<ChapterListAdapter.ViewHolder> {

    private List<String> chapterTitles;

    public ChapterListAdapter(List<String> chapterTitles) {
        this.chapterTitles = chapterTitles;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chapter_simple, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String title = chapterTitles.get(position);
        holder.tvChapterTitle.setText(title);
    }

    @Override
    public int getItemCount() {
        return chapterTitles != null ? chapterTitles.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvChapterTitle;

        ViewHolder(View itemView) {
            super(itemView);
            tvChapterTitle = itemView.findViewById(R.id.tv_chapter_title);
        }
    }
}
