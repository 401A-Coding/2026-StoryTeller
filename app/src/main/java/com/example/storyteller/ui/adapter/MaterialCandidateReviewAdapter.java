package com.example.storyteller.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.storyteller.R;
import com.example.storyteller.model.StorySetting;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 素材候选审核适配器（新版）
 * 用于AI提取素材后的用户审核和选择
 */
public class MaterialCandidateReviewAdapter extends RecyclerView.Adapter<MaterialCandidateReviewAdapter.ViewHolder> {

    private final List<StorySetting> settings = new ArrayList<>();
    private final Set<Integer> selectedPositions = new HashSet<>();

    public void setData(List<StorySetting> list) {
        settings.clear();
        selectedPositions.clear();
        if (list != null) {
            settings.addAll(list);
            // 默认全选
            for (int i = 0; i < settings.size(); i++) {
                selectedPositions.add(i);
            }
        }
        notifyDataSetChanged();
    }

    public void setAllSelected(boolean selected) {
        selectedPositions.clear();
        if (selected) {
            for (int i = 0; i < settings.size(); i++) {
                selectedPositions.add(i);
            }
        }
        notifyDataSetChanged();
    }

    public boolean isAllSelected() {
        return !settings.isEmpty() && selectedPositions.size() == settings.size();
    }

    public List<StorySetting> getSelectedSettings() {
        List<StorySetting> result = new ArrayList<>();
        for (int i = 0; i < settings.size(); i++) {
            if (selectedPositions.contains(i)) {
                result.add(settings.get(i));
            }
        }
        return result;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_material_candidate_review, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StorySetting setting = settings.get(position);
        holder.tvTitle.setText(setting.getTitle());
        
        // 显示分类信息：主分类 + 子分类
        String categoryText = setting.getCategory();
        if (setting.getSubCategory() != null && !setting.getSubCategory().isEmpty()) {
            categoryText += " · " + setting.getSubCategory();
        }
        holder.tvCategory.setText(categoryText);

        // 显示摘要预览
        String summary = setting.getSummary();
        if (summary != null && summary.length() > 120) {
            holder.tvPreview.setText(summary.substring(0, 120) + "...");
        } else {
            holder.tvPreview.setText(summary);
        }

        holder.cbSelected.setOnCheckedChangeListener(null);
        holder.cbSelected.setChecked(selectedPositions.contains(position));
        holder.cbSelected.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedPositions.add(position);
            } else {
                selectedPositions.remove(position);
            }
        });

        holder.itemView.setOnClickListener(v -> holder.cbSelected.setChecked(!holder.cbSelected.isChecked()));
    }

    @Override
    public int getItemCount() {
        return settings.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final CheckBox cbSelected;
        final TextView tvTitle;
        final TextView tvCategory;
        final TextView tvPreview;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            cbSelected = itemView.findViewById(R.id.cb_candidate_selected);
            tvTitle = itemView.findViewById(R.id.tv_candidate_title);
            tvCategory = itemView.findViewById(R.id.tv_candidate_category);
            tvPreview = itemView.findViewById(R.id.tv_candidate_preview);
        }
    }
}

