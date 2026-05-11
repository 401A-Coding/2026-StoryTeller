package com.example.storyteller.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.storyteller.R;
import com.example.storyteller.model.Material;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MaterialAdapter extends RecyclerView.Adapter<MaterialAdapter.ViewHolder> {

    private List<Material> materials;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    public MaterialAdapter(List<Material> materials) {
        this.materials = materials;
    }

    public void setData(List<Material> materials) {
        this.materials = materials;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_material, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Material material = materials.get(position);
        holder.tvTitle.setText(material.getTitle());
        holder.tvCategory.setText(material.getCategory());

        // 显示内容预览（前100字）
        String content = material.getContent();
        if (content != null && content.length() > 100) {
            holder.tvPreview.setText(content.substring(0, 100) + "...");
        } else {
            holder.tvPreview.setText(content);
        }

        holder.tvTime.setText(dateFormat.format(new Date(material.getCreateTime())));
    }

    @Override
    public int getItemCount() {
        return materials != null ? materials.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        TextView tvCategory;
        TextView tvPreview;
        TextView tvTime;

        ViewHolder(View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_material_item_title);
            tvCategory = itemView.findViewById(R.id.tv_material_item_category);
            tvPreview = itemView.findViewById(R.id.tv_material_item_preview);
            tvTime = itemView.findViewById(R.id.tv_material_item_time);
        }
    }
}
