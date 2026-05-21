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
 * AI记忆列表适配器
 */
public class AiMemoryAdapter extends RecyclerView.Adapter<AiMemoryAdapter.ViewHolder> {
    
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
                .inflate(R.layout.item_ai_memory, parent, false);
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
        private final TextView tvMemoryType;
        private final TextView tvImportance;
        private final TextView tvTime;
        private final ImageButton btnDelete;
        
        ViewHolder(View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvContent = itemView.findViewById(R.id.tv_content);
            tvMemoryType = itemView.findViewById(R.id.tv_memory_type);
            tvImportance = itemView.findViewById(R.id.tv_importance);
            tvTime = itemView.findViewById(R.id.tv_time);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
        
        void bind(AiMemory memory) {
            tvTitle.setText(memory.getTitle());
            
            // 设置记忆类型标签
            String typeLabel = getTypeLabel(memory.getMemoryType());
            tvMemoryType.setText(typeLabel);
            
            // 设置重要性指示
            String importanceStars = getImportanceStars(memory.getImportance());
            tvImportance.setText(importanceStars);
            
            // 设置时间（如果有）
            if (memory.getCreatedAt() > 0) {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.CHINA);
                tvTime.setText(sdf.format(new java.util.Date(memory.getCreatedAt())));
            } else {
                tvTime.setText("");
            }
            
            // 设置内容
            if (memory.getContent() != null && !memory.getContent().isEmpty()) {
                tvContent.setVisibility(View.VISIBLE);
                tvContent.setText(memory.getContent());
            } else {
                tvContent.setVisibility(View.GONE);
            }
            
            // 点击卡片展开/收起详情
            itemView.setOnClickListener(v -> {
                if (tvContent.getVisibility() == View.VISIBLE) {
                    tvContent.setVisibility(View.GONE);
                } else {
                    tvContent.setVisibility(View.VISIBLE);
                }
            });
            
            btnDelete.setOnClickListener(v -> {
                if (deleteListener != null) {
                    deleteListener.onDelete(memory);
                }
            });
        }
        
        private String getTypeLabel(String type) {
            switch (type) {
                case com.example.storyteller.model.AiMemory.TYPE_PLOT:
                    return "剧情类";
                case com.example.storyteller.model.AiMemory.TYPE_PERSONALITY:
                    return "人设类";
                case com.example.storyteller.model.AiMemory.TYPE_WORLD:
                    return "世界观类";
                default:
                    return "其他类";
            }
        }
        
        private String getImportanceStars(int importance) {
            StringBuilder stars = new StringBuilder();
            for (int i = 0; i < importance; i++) {
                stars.append("⭐");
            }
            return stars.toString();
        }
    }
}