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
 * AI记忆列表适配器
 */
public class AiMemoryAdapter extends RecyclerView.Adapter<AiMemoryAdapter.ViewHolder> {
    
    private final List<AiMemory> memories = new ArrayList<>();
    private OnMemoryDeleteListener deleteListener;
    private OnMemoryClickListener clickListener;
    private boolean isMultiSelectMode = false; // 多选模式
    private final java.util.Set<Integer> selectedPositions = new java.util.HashSet<>();
    
    public interface OnMemoryDeleteListener {
        void onDelete(AiMemory memory);
    }
    
    public interface OnMemoryClickListener {
        void onClick(AiMemory memory);
    }
    
    public void setOnMemoryDeleteListener(OnMemoryDeleteListener listener) {
        this.deleteListener = listener;
    }
    
    public void setOnMemoryClickListener(OnMemoryClickListener listener) {
        this.clickListener = listener;
    }
    
    /**
     * 设置多选模式
     */
    public void setMultiSelectMode(boolean enabled) {
        this.isMultiSelectMode = enabled;
        if (!enabled) {
            selectedPositions.clear();
        }
        notifyDataSetChanged();
    }
    
    /**
     * 是否处于多选模式
     */
    public boolean isMultiSelectMode() {
        return isMultiSelectMode;
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
    
    /**
     * 获取选中的记忆
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
     * 获取选中数量
     */
    public int getSelectedCount() {
        return selectedPositions.size();
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
        holder.bind(memory, position);
    }
    
    @Override
    public int getItemCount() {
        return memories.size();
    }
    
    class ViewHolder extends RecyclerView.ViewHolder {
        private final CheckBox cbSelect;
        private final TextView tvTitle;
        private final TextView tvContent;
        private final TextView tvMemoryType;
        private final TextView tvImportance;
        private final TextView tvTime;
        private final ImageButton btnDelete;
        
        ViewHolder(View itemView) {
            super(itemView);
            cbSelect = itemView.findViewById(R.id.cb_select);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvContent = itemView.findViewById(R.id.tv_content);
            tvMemoryType = itemView.findViewById(R.id.tv_memory_type);
            tvImportance = itemView.findViewById(R.id.tv_importance);
            tvTime = itemView.findViewById(R.id.tv_time);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
        
        void bind(AiMemory memory, int position) {
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
            
            // 多选模式
            if (isMultiSelectMode) {
                cbSelect.setVisibility(View.VISIBLE);
                btnDelete.setVisibility(View.GONE);
                
                boolean isSelected = selectedPositions.contains(position);
                cbSelect.setChecked(isSelected);
                
                // 点击CheckBox
                cbSelect.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (buttonView.isPressed()) {
                        if (isChecked) {
                            selectedPositions.add(position);
                        } else {
                            selectedPositions.remove(position);
                        }
                    }
                });
                
                // 点击卡片也可以切换选中状态
                itemView.setOnClickListener(v -> {
                    if (selectedPositions.contains(position)) {
                        selectedPositions.remove(position);
                    } else {
                        selectedPositions.add(position);
                    }
                    cbSelect.setChecked(selectedPositions.contains(position));
                });
            } else {
                cbSelect.setVisibility(View.GONE);
                btnDelete.setVisibility(View.VISIBLE);
                
                // 点击卡片展开/收起详情或进入编辑
                itemView.setOnClickListener(v -> {
                    // 优先触发编辑页面
                    if (clickListener != null) {
                        clickListener.onClick(memory);
                    } else {
                        // 如果没有设置点击监听，则展开/收起
                        if (tvContent.getVisibility() == View.VISIBLE) {
                            tvContent.setVisibility(View.GONE);
                        } else {
                            tvContent.setVisibility(View.VISIBLE);
                        }
                    }
                });
                
                btnDelete.setOnClickListener(v -> {
                    if (deleteListener != null) {
                        deleteListener.onDelete(memory);
                    }
                });
            }
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