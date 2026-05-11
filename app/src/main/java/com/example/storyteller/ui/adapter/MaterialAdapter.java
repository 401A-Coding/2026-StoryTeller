package com.example.storyteller.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.storyteller.R;
import com.example.storyteller.data.remote.MaterialCandidateExtractor;
import com.example.storyteller.model.Material;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MaterialAdapter extends RecyclerView.Adapter<MaterialAdapter.ViewHolder> {

    public interface Listener {
        void onMaterialClick(@NonNull Material material);
    }

    private List<Material> allMaterials; // 原始完整数据
    private List<Material> materials; // 当前展示的数据（可能被过滤/搜索）
    private String currentTypeFilter = null; // null 表示全部
    private String currentQuery = null;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
    private Listener listener;

    public MaterialAdapter(List<Material> materials) {
        this.allMaterials = materials;
        this.materials = materials;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setData(List<Material> materials) {
        this.allMaterials = materials;
        applyFilters();
    }

    public void filterByType(String type) {
        if (type != null && type.trim().isEmpty()) {
            type = null;
        }
        this.currentTypeFilter = type;
        applyFilters();
    }

    public void search(String query) {
        if (query != null) {
            query = query.trim().toLowerCase();
            if (query.isEmpty()) query = null;
        }
        this.currentQuery = query;
        applyFilters();
    }

    private void applyFilters() {
        if (allMaterials == null) {
            this.materials = null;
            notifyDataSetChanged();
            return;
        }
        // 过滤逻辑：先按类型，再按关键字
        List<Material> filtered = new java.util.ArrayList<>();
        for (Material m : allMaterials) {
            if (currentTypeFilter != null) {
                String srcType = m.getSourceType();
                String cat = m.getCategory();
                boolean matches = false;
                // match by sourceType (e.g. "persona", "plot", "theme")
                if (srcType != null && srcType.equalsIgnoreCase(currentTypeFilter)) {
                    matches = true;
                }
                // or match by category label (e.g. "人物素材"), support mapping based on extractor constants
                if (!matches && cat != null) {
                    if (MaterialCandidateExtractor.TYPE_PERSONA.equalsIgnoreCase(currentTypeFilter)
                            && MaterialCandidateExtractor.CATEGORY_PERSONA.equals(cat)) {
                        matches = true;
                    } else if (MaterialCandidateExtractor.TYPE_PLOT.equalsIgnoreCase(currentTypeFilter)
                            && MaterialCandidateExtractor.CATEGORY_PLOT.equals(cat)) {
                        matches = true;
                    } else if (MaterialCandidateExtractor.TYPE_THEME.equalsIgnoreCase(currentTypeFilter)
                            && MaterialCandidateExtractor.CATEGORY_THEME.equals(cat)) {
                        matches = true;
                    }
                }
                if (!matches) continue;
            }
            if (currentQuery != null) {
                String q = currentQuery;
                boolean matched = false;
                if (m.getTitle() != null && m.getTitle().toLowerCase().contains(q)) matched = true;
                if (!matched && m.getContent() != null && m.getContent().toLowerCase().contains(q)) matched = true;
                if (!matched) continue;
            }
            filtered.add(m);
        }
        this.materials = filtered;
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
            holder.tvPreview.setText(content.substring(0, 100));
        } else {
            holder.tvPreview.setText(content);
        }

        holder.tvTime.setText(dateFormat.format(new Date(material.getCreateTime())));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onMaterialClick(material);
            }
        });
    }

    @Override
    public int getItemCount() {
        return materials != null ? materials.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
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
