package com.example.storyteller.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.storyteller.R;
import com.example.storyteller.model.AiMemory;
import java.util.ArrayList;
import java.util.List;

/**
 * 记忆提取结果适配器（支持批量选择）
 */
public class MemoryExtractionAdapter extends RecyclerView.Adapter<MemoryExtractionAdapter.ViewHolder> {
    
    private final List<AiMemory> memories = new ArrayList<>();
    private final java.util.Set<Integer> selectedPositions = new java.util.HashSet<>();
    
    public void setMemories(List<AiMemory> newMemories) {
        memories.clear();
        if (newMemories != null) {
            memories.addAll(newMemories);
            // 默认全选
            for (int i = 0; i < newMemories.size(); i++) {
                selectedPositions.add(i);
            }
        }
        notifyDataSetChanged();
    }
    
    /**
     * 获取所有选中的记忆
     */
    public List<AiMemory> getSelectedMemories() {
        List<AiMemory> selected = new ArrayList<>();
        for (int position : selectedPositions) {
            if (position >= 0 && position < memories.size()) {
                selected.add(memories.get(position));
            }
        }
        return selected;
    }
    
    /**
     * 全选
     */
    public void selectAll() {
        selectedPositions.clear();
        for (int i = 0; i < memories.size(); i++) {
            selectedPositions.add(i);
        }
        notifyDataSetChanged();
    }
    
    /**
     * 取消全选
     */
    public void deselectAll() {
        selectedPositions.clear();
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
        holder.bind(memory, position);
    }
    
    @Override
    public int getItemCount() {
        return memories.size();
    }
    
    public int getSelectedCount() {
        return selectedPositions.size();
    }
    
    class ViewHolder extends RecyclerView.ViewHolder {
        private final CheckBox cbSelected;
        private final TextView tvTitle;
        private final TextView tvContent;
        private final TextView tvMemoryType;
        private final TextView tvImportance;
        private final TextView tvSource;
        
        ViewHolder(View itemView) {
            super(itemView);
            cbSelected = itemView.findViewById(R.id.cb_selected);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvContent = itemView.findViewById(R.id.tv_content);
            tvMemoryType = itemView.findViewById(R.id.tv_memory_type);
            tvImportance = itemView.findViewById(R.id.tv_importance);
            tvSource = itemView.findViewById(R.id.tv_source);
        }
        
        void bind(AiMemory memory, int position) {
            // 标题
            tvTitle.setText(memory.getTitle());
            
            // 内容
            if (memory.getContent() != null && !memory.getContent().isEmpty()) {
                tvContent.setVisibility(View.VISIBLE);
                tvContent.setText(memory.getContent());
            } else {
                tvContent.setVisibility(View.GONE);
            }
            
            // 记忆类型标签
            String typeLabel = getTypeLabel(memory.getMemoryType());
            tvMemoryType.setText(typeLabel);
            
            // 重要性
            String stars = buildStars(memory.getImportance());
            tvImportance.setText("重要性: " + stars);
            
            // 来源标识
            tvSource.setVisibility(View.VISIBLE);
            
            // CheckBox状态
            boolean isSelected = selectedPositions.contains(position);
            cbSelected.setChecked(isSelected);
            
            // 点击事件
            View.OnClickListener clickListener = v -> {
                if (selectedPositions.contains(position)) {
                    selectedPositions.remove(position);
                } else {
                    selectedPositions.add(position);
                }
                notifyItemChanged(position);
            };
            
            itemView.setOnClickListener(clickListener);
            cbSelected.setOnCheckedChangeListener((buttonView, isChecked) -> {
                // 防止RecyclerView复用导致的误触发
                if (buttonView.isPressed()) {
                    if (isChecked) {
                        selectedPositions.add(position);
                    } else {
                        selectedPositions.remove(position);
                    }
                }
            });
        }
        
        private String getTypeLabel(String type) {
            if (type == null) return "其他";
            switch (type) {
                case AiMemory.TYPE_PLOT:
                    return "剧情类";
                case AiMemory.TYPE_PERSONALITY:
                    return "人设类";
                case AiMemory.TYPE_WORLD:
                    return "世界观类";
                default:
                    return "其他类";
            }
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
