package com.example.storyteller.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.storyteller.R;
import com.example.storyteller.model.AiMemory;
import java.util.ArrayList;
import java.util.List;

/**
 * 记忆提取结果适配器（只读，支持删除）
 */
public class MemoryExtractionAdapter extends RecyclerView.Adapter<MemoryExtractionAdapter.ViewHolder> {
    
    private final List<AiMemory> memories = new ArrayList<>();
    private OnMemoryDeleteListener deleteListener;
    
    public interface OnMemoryDeleteListener {
        void onDelete(AiMemory memory);
    }
    
    public void setOnMemoryDeleteListener(OnMemoryDeleteListener listener) {
        this.deleteListener = listener;
    }
    
    public void setMemories(List<AiMemory> newMemories) {
        memories.clear();
        if (newMemories != null) {
            memories.addAll(newMemories);
        }
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_memory_extraction, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AiMemory memory = memories.get(position);
        holder.bind(memory);
    }
    
    @Override
    public int getItemCount() {
        return memories.size();
    }
    
    class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTitle;
        private final TextView tvContent;
        private final TextView tvImportance;
        private final ImageButton btnDelete;
        
        ViewHolder(View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvContent = itemView.findViewById(R.id.tv_content);
            tvImportance = itemView.findViewById(R.id.tv_importance);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
        
        void bind(AiMemory memory) {
            // 标题
            tvTitle.setText(memory.getTitle());
            
            // 内容
            if (memory.getContent() != null && !memory.getContent().isEmpty()) {
                tvContent.setVisibility(View.VISIBLE);
                tvContent.setText(memory.getContent());
            } else {
                tvContent.setVisibility(View.GONE);
            }
            
            // 重要性
            String stars = buildStars(memory.getImportance());
            tvImportance.setText("重要性: " + stars);
            
            // 删除按钮
            btnDelete.setOnClickListener(v -> {
                if (deleteListener != null) {
                    deleteListener.onDelete(memory);
                }
            });
        }
        
        private String buildStars(int importance) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < importance; i++) {
                sb.append("⭐");
            }
            return sb.toString();
        }
    }
}
