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
import com.example.storyteller.data.remote.ModelConfig;

import java.util.ArrayList;
import java.util.List;

public class ModelAdapter extends RecyclerView.Adapter<ModelAdapter.ViewHolder> {
    
    private List<ModelConfig.Provider> providers = new ArrayList<>();
    private OnModelActionListener listener;
    
    public interface OnModelActionListener {
        void onEnabledChanged(ModelConfig.Provider provider, boolean enabled);
        void onDeleteProvider(ModelConfig.Provider provider);
    }
    
    public void setOnModelActionListener(OnModelActionListener listener) {
        this.listener = listener;
    }
    
    public void setProviders(List<ModelConfig.Provider> providers) {
        this.providers = providers != null ? providers : new ArrayList<>();
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_model, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ModelConfig.Provider provider = providers.get(position);
        holder.tvModelName.setText(provider.getDisplayName());
        holder.tvProvider.setText(provider.getBaseUrl());
        
        // 获取启用状态
        holder.cbEnabled.setOnCheckedChangeListener(null);
        holder.cbEnabled.setChecked(ModelConfig.isProviderEnabled(holder.itemView.getContext(), provider));
        holder.cbEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (listener != null) {
                listener.onEnabledChanged(provider, isChecked);
            }
        });
        
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteProvider(provider);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return providers.size();
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        CheckBox cbEnabled;
        TextView tvModelName;
        TextView tvProvider;
        ImageButton btnDelete;
        
        ViewHolder(View itemView) {
            super(itemView);
            cbEnabled = itemView.findViewById(R.id.cb_enabled);
            tvModelName = itemView.findViewById(R.id.tv_model_name);
            tvProvider = itemView.findViewById(R.id.tv_provider);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }
}