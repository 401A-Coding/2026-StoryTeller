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
        private final ImageButton btnDelete;
        
        ViewHolder(View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvContent = itemView.findViewById(R.id.tv_content);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
        
        void bind(AiMemory memory) {
            tvTitle.setText(memory.getTitle());
            
            if (memory.getContent() != null && !memory.getContent().isEmpty()) {
                tvContent.setVisibility(View.VISIBLE);
                tvContent.setText(memory.getContent());
            } else {
                tvContent.setVisibility(View.GONE);
            }
            
            btnDelete.setOnClickListener(v -> {
                if (deleteListener != null) {
                    deleteListener.onDelete(memory);
                }
            });
        }
    }
}