package com.example.storyteller.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.storyteller.R;
import com.example.storyteller.model.StoryDocument;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 文档列表适配器
 */
public class DocumentAdapter extends RecyclerView.Adapter<DocumentAdapter.DocumentViewHolder> {

    private List<StoryDocument> documents = new ArrayList<>();
    private OnDocumentClickListener listener;
    private OnDocumentLongClickListener longListener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    public interface OnDocumentClickListener {
        void onDocumentClick(StoryDocument document);
    }

    public interface OnDocumentLongClickListener {
        void onDocumentLongClick(StoryDocument document, int position);
    }

    public void setOnDocumentClickListener(OnDocumentClickListener listener) {
        this.listener = listener;
    }

    public void setOnDocumentLongClickListener(OnDocumentLongClickListener listener) {
        this.longListener = listener;
    }

    public void setDocuments(List<StoryDocument> documents) {
        this.documents = documents != null ? documents : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DocumentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_document, parent, false);
        return new DocumentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DocumentViewHolder holder, int position) {
        StoryDocument doc = documents.get(position);
        holder.bind(doc);
    }

    @Override
    public int getItemCount() {
        return documents.size();
    }

    public StoryDocument getItem(int position) {
        if (position >= 0 && position < documents.size()) {
            return documents.get(position);
        }
        return null;
    }

    class DocumentViewHolder extends RecyclerView.ViewHolder {
        private TextView tvTitle;
        private TextView tvCategory;
        private TextView tvUpdateTime;
        private TextView tvPreview;

        public DocumentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_document_title);
            tvCategory = itemView.findViewById(R.id.tv_document_category);
            tvUpdateTime = itemView.findViewById(R.id.tv_document_update_time);
            tvPreview = itemView.findViewById(R.id.tv_document_preview);
        }

        public void bind(StoryDocument doc) {
            // 标题
            tvTitle.setText(doc.getTitle());

            // 分类标签
            String categoryDisplay = doc.getCategoryDisplayName();
            tvCategory.setText(categoryDisplay);

            // 更新时间
            String timeText = dateFormat.format(new Date(doc.getUpdateTime()));
            tvUpdateTime.setText(timeText);

            // 内容预览（取前50个字符）
            String content = doc.getContent();
            if (content != null && !content.isEmpty()) {
                // 移除Markdown标记，只显示纯文本预览
                String plainText = content.replaceAll("#+\\s*", "")
                        .replaceAll("\\*\\*(.+?)\\*\\*", "$1")
                        .replaceAll("\\*(.+?)\\*", "$1")
                        .replaceAll("`(.+?)`", "$1");
                
                String preview = plainText.length() > 50 
                        ? plainText.substring(0, 50) + "..." 
                        : plainText;
                tvPreview.setText(preview);
                tvPreview.setVisibility(View.VISIBLE);
            } else {
                tvPreview.setVisibility(View.GONE);
            }

            // 点击事件
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDocumentClick(doc);
                }
            });

            // 长按事件
            itemView.setOnLongClickListener(v -> {
                if (longListener != null) {
                    longListener.onDocumentLongClick(doc, getAdapterPosition());
                }
                return true;
            });
        }
    }
}
