package com.example.storyteller.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.storyteller.R;
import com.example.storyteller.model.Material;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MaterialCandidateReviewAdapter extends RecyclerView.Adapter<MaterialCandidateReviewAdapter.ViewHolder> {

    private final List<Material> materials = new ArrayList<>();
    private final Set<Integer> selectedPositions = new HashSet<>();

    public void setData(List<Material> list) {
        materials.clear();
        selectedPositions.clear();
        if (list != null) {
            materials.addAll(list);
            for (int i = 0; i < materials.size(); i++) {
                selectedPositions.add(i);
            }
        }
        notifyDataSetChanged();
    }

    public void setAllSelected(boolean selected) {
        selectedPositions.clear();
        if (selected) {
            for (int i = 0; i < materials.size(); i++) {
                selectedPositions.add(i);
            }
        }
        notifyDataSetChanged();
    }

    public boolean isAllSelected() {
        return !materials.isEmpty() && selectedPositions.size() == materials.size();
    }

    public List<Material> getSelectedMaterials() {
        List<Material> result = new ArrayList<>();
        for (int i = 0; i < materials.size(); i++) {
            if (selectedPositions.contains(i)) {
                result.add(materials.get(i));
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
        Material material = materials.get(position);
        holder.tvTitle.setText(material.getTitle());
        holder.tvCategory.setText(material.getCategory());

        String content = material.getContent();
        if (content != null && content.length() > 140) {
            holder.tvPreview.setText(content.substring(0, 140));
        } else {
            holder.tvPreview.setText(content);
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
        return materials.size();
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

