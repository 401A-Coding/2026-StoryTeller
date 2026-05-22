package com.example.storyteller.ui.adapter;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.storyteller.R;
import com.example.storyteller.model.RelationExtractionResult;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 关系提取结果适配器（基于 MaterialCandidateReviewAdapter 模式）
 */
public class ExtractionResultAdapter extends RecyclerView.Adapter<ExtractionResultAdapter.ViewHolder> {

    private static final int TAB_CONFIRMED = 0;
    private static final int TAB_PENDING = 1;

    private final RelationExtractionResult result;
    private int currentTab = TAB_CONFIRMED;

    // 选择状态
    private final Set<Integer> selectedConfirmed = new HashSet<>();
    private final Set<Integer> selectedPending = new HashSet<>();

    public ExtractionResultAdapter(@NonNull RelationExtractionResult result) {
        this.result = result;
        // 默认全选
        if (result.getConfirmedRelations() != null) {
            for (int i = 0; i < result.getConfirmedRelations().size(); i++) {
                selectedConfirmed.add(i);
            }
        }
        if (result.getPendingEntities() != null) {
            for (int i = 0; i < result.getPendingEntities().size(); i++) {
                selectedPending.add(i);
            }
        }
    }

    public void showConfirmedTab() {
        currentTab = TAB_CONFIRMED;
    }

    public void showPendingTab() {
        currentTab = TAB_PENDING;
    }

    public void toggleAllSelection() {
        if (currentTab == TAB_CONFIRMED) {
            if (isAllSelected()) {
                selectedConfirmed.clear();
            } else {
                if (result.getConfirmedRelations() != null) {
                    for (int i = 0; i < result.getConfirmedRelations().size(); i++) {
                        selectedConfirmed.add(i);
                    }
                }
            }
        } else {
            if (isAllSelected()) {
                selectedPending.clear();
            } else {
                if (result.getPendingEntities() != null) {
                    for (int i = 0; i < result.getPendingEntities().size(); i++) {
                        selectedPending.add(i);
                    }
                }
            }
        }
    }

    public boolean isAllSelected() {
        if (currentTab == TAB_CONFIRMED) {
            if (result.getConfirmedRelations() == null || result.getConfirmedRelations().isEmpty()) {
                return false;
            }
            return selectedConfirmed.size() == result.getConfirmedRelations().size();
        } else {
            if (result.getPendingEntities() == null || result.getPendingEntities().isEmpty()) {
                return false;
            }
            return selectedPending.size() == result.getPendingEntities().size();
        }
    }

    public List<Integer> getSelectedConfirmedPositions() {
        return new ArrayList<>(selectedConfirmed);
    }

    public List<Integer> getSelectedPendingPositions() {
        return new ArrayList<>(selectedPending);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_extraction_result, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (currentTab == TAB_CONFIRMED) {
            bindConfirmedRelation(holder, position);
        } else {
            bindPendingEntity(holder, position);
        }
    }

    private void bindConfirmedRelation(ViewHolder holder, int position) {
        RelationExtractionResult.ConfirmedRelation rel = result.getConfirmedRelations().get(position);

        holder.tvTitle.setText(rel.getSourceName() + " → " + rel.getTargetName());
        holder.tvCategory.setText(getRelationTypeName(rel.getRelationshipType()));
        
        // 简介为空时隐藏摘要区域
        String description = rel.getDescription();
        if (!TextUtils.isEmpty(description)) {
            holder.tvSummary.setVisibility(View.VISIBLE);
            holder.tvSummary.setText(description);
        } else {
            holder.tvSummary.setVisibility(View.GONE);
        }
        // 隐藏标签区域（关系卡片不需要）
        holder.chipGroupTags.setVisibility(View.GONE);

        holder.cbSelected.setOnCheckedChangeListener(null);
        holder.cbSelected.setChecked(selectedConfirmed.contains(position));
        holder.cbSelected.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedConfirmed.add(position);
            } else {
                selectedConfirmed.remove(position);
            }
        });

        holder.itemView.setOnClickListener(v -> holder.cbSelected.setChecked(!holder.cbSelected.isChecked()));
    }

    private void bindPendingEntity(ViewHolder holder, int position) {
        RelationExtractionResult.PendingEntity entity = result.getPendingEntities().get(position);

        // 标题
        holder.tvTitle.setText(entity.getName());
        
        // 分类
        String categoryText = entity.getSuggestedCategory() != null ? entity.getSuggestedCategory() : "";
        if (!TextUtils.isEmpty(entity.getSuggestedSubcategory())) {
            categoryText += " · " + entity.getSuggestedSubcategory();
        }
        holder.tvCategory.setText(categoryText);
        
        // 简介
        String summary = entity.getSummary();
        if (!TextUtils.isEmpty(summary)) {
            holder.tvSummary.setVisibility(View.VISIBLE);
            if (summary.length() > 100) {
                summary = summary.substring(0, 100) + "...";
            }
            holder.tvSummary.setText(summary);
        } else {
            holder.tvSummary.setVisibility(View.GONE);
        }
        
        // 标签（最多显示3个，超出显示 +N）
        holder.chipGroupTags.setVisibility(View.VISIBLE);
        holder.chipGroupTags.removeAllViews();
        List<String> tags = entity.getTags();
        if (tags != null && !tags.isEmpty()) {
            int maxShow = 3;
            int showCount = Math.min(tags.size(), maxShow);
            for (int i = 0; i < showCount; i++) {
                Chip chip = new Chip(holder.itemView.getContext());
                chip.setText(tags.get(i));
                chip.setTextSize(10);
                chip.setChipMinHeight(20f);
                chip.setChipBackgroundColorResource(android.R.color.transparent);
                chip.setChipStrokeWidth(1f);
                chip.setChipStrokeColorResource(android.R.color.darker_gray);
                chip.setClickable(false);
                chip.setCheckable(false);
                holder.chipGroupTags.addView(chip);
            }
            if (tags.size() > maxShow) {
                Chip chip = new Chip(holder.itemView.getContext());
                chip.setText("+" + (tags.size() - maxShow));
                chip.setTextSize(10);
                chip.setChipMinHeight(20f);
                chip.setClickable(false);
                chip.setCheckable(false);
                holder.chipGroupTags.addView(chip);
            }
        }

        holder.cbSelected.setOnCheckedChangeListener(null);
        holder.cbSelected.setChecked(selectedPending.contains(position));
        holder.cbSelected.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedPending.add(position);
            } else {
                selectedPending.remove(position);
            }
        });

        holder.itemView.setOnClickListener(v -> holder.cbSelected.setChecked(!holder.cbSelected.isChecked()));
    }

    @Override
    public int getItemCount() {
        if (currentTab == TAB_CONFIRMED) {
            return result.getConfirmedRelations() != null ? result.getConfirmedRelations().size() : 0;
        } else {
            return result.getPendingEntities() != null ? result.getPendingEntities().size() : 0;
        }
    }

    private String getRelationTypeName(String type) {
        if (type == null) return "未知";
        switch (type) {
            case "BELONGS_TO": return "属于";
            case "CONTAINS": return "包含";
            case "FRIEND": return "朋友";
            case "ALLY": return "盟友";
            case "MENTOR": return "师徒";
            case "LOVER": return "恋人";
            case "PARENT": return "父母";
            case "CHILD": return "子女";
            case "ENEMY": return "敌人";
            case "LOCATED_AT": return "位于";
            case "USES": return "使用";
            case "AFFECTS": return "影响";
            default: return type;
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final CheckBox cbSelected;
        final TextView tvTitle;
        final TextView tvCategory;
        final TextView tvSummary;
        final ChipGroup chipGroupTags;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            cbSelected = itemView.findViewById(R.id.cb_selected);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvCategory = itemView.findViewById(R.id.tv_category);
            tvSummary = itemView.findViewById(R.id.tv_summary);
            chipGroupTags = itemView.findViewById(R.id.chip_group_tags);
        }
    }
}