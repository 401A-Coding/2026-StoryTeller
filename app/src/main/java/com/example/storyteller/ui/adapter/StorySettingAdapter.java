package com.example.storyteller.ui.adapter;

import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.storyteller.R;
import com.example.storyteller.model.StorySetting;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 小说设定列表适配器
 */
public class StorySettingAdapter extends RecyclerView.Adapter<StorySettingAdapter.ViewHolder> {

    public interface OnSettingClickListener {
        void onSettingClick(StorySetting setting);
    }

    public interface OnSettingDeleteListener {
        void onSettingDelete(StorySetting setting, int position);
    }
    
    public interface OnSelectionModeChangeListener {
        void onSelectionModeChange(boolean isInSelectionMode, int selectedCount);
    }
    
    public interface OnImportListener {
        void onImportToStory(StorySetting setting);
    }
    
    public interface OnExportListener {
        void onExportToGlobal(StorySetting setting);
    }

    private List<StorySetting> allSettings;      // 原始数据
    private List<StorySetting> displaySettings;   // 显示数据（可能被过滤）
    private String currentCategoryFilter = null;  // 当前分类过滤
    private String currentSubCategoryFilter = null; // 当前子分类过滤
    private String currentQuery = null;           // 搜索关键词
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private OnSettingClickListener listener;
    private OnSettingDeleteListener deleteListener;
    private OnSelectionModeChangeListener selectionModeListener;
    private OnImportListener importListener;
    private OnExportListener exportListener;
    
    // 多选模式
    private boolean isSelectionMode = false;
    private java.util.Set<Integer> selectedPositions = new java.util.HashSet<>();

    public StorySettingAdapter(List<StorySetting> settings) {
        this.allSettings = settings != null ? settings : new ArrayList<>();
        this.displaySettings = this.allSettings;
    }

    public void setOnSettingClickListener(OnSettingClickListener listener) {
        this.listener = listener;
    }

    public void setOnSettingDeleteListener(OnSettingDeleteListener listener) {
        this.deleteListener = listener;
    }
    
    public void setOnSelectionModeChangeListener(OnSelectionModeChangeListener listener) {
        this.selectionModeListener = listener;
    }
    
    public void setOnImportListener(OnImportListener listener) {
        this.importListener = listener;
    }
    
    public void setOnExportListener(OnExportListener listener) {
        this.exportListener = listener;
    }
    
    /**
     * 进入/退出多选模式
     */
    public void setSelectionMode(boolean enabled) {
        this.isSelectionMode = enabled;
        if (!enabled) {
            selectedPositions.clear();
        }
        notifyDataSetChanged();
        
        if (selectionModeListener != null) {
            selectionModeListener.onSelectionModeChange(enabled, selectedPositions.size());
        }
    }
    
    /**
     * 是否处于多选模式
     */
    public boolean isSelectionMode() {
        return isSelectionMode;
    }
    
    /**
     * 切换选中状态
     */
    public void toggleSelection(int position) {
        if (selectedPositions.contains(position)) {
            selectedPositions.remove(position);
        } else {
            selectedPositions.add(position);
        }
        
        // 只更新当前项，避免触发Checkbox的点击事件
        notifyItemChanged(position);
        
        if (selectionModeListener != null) {
            selectionModeListener.onSelectionModeChange(true, selectedPositions.size());
        }
    }
    
    /**
     * 获取选中的设定列表
     */
    public List<StorySetting> getSelectedSettings() {
        List<StorySetting> selected = new ArrayList<>();
        
        for (int position : selectedPositions) {
            if (position >= 0 && position < displaySettings.size()) {
                StorySetting setting = displaySettings.get(position);
                selected.add(setting);
            }
        }
        
        return selected;
    }
    
    /**
     * 全选
     */
    public void selectAll() {
        selectedPositions.clear();
        for (int i = 0; i < displaySettings.size(); i++) {
            selectedPositions.add(i);
        }
        notifyDataSetChanged();
        
        if (selectionModeListener != null) {
            selectionModeListener.onSelectionModeChange(true, selectedPositions.size());
        }
    }
    
    /**
     * 清空选择
     */
    public void clearSelection() {
        selectedPositions.clear();
        notifyDataSetChanged();
        
        if (selectionModeListener != null) {
            selectionModeListener.onSelectionModeChange(true, 0);
        }
    }

    /**
     * 设置数据
     */
    public void setData(List<StorySetting> settings) {
        this.allSettings = settings != null ? settings : new ArrayList<>();
        applyFilters();
    }

    /**
     * 按顶层分类过滤
     */
    public void filterByCategory(String category) {
        this.currentCategoryFilter = category;
        applyFilters();
    }

    /**
     * 按子分类过滤
     */
    public void filterBySubCategory(String subCategory) {
        this.currentSubCategoryFilter = subCategory;
        applyFilters();
    }

    /**
     * 搜索
     */
    public void search(String query) {
        if (query != null) {
            query = query.trim().toLowerCase();
            if (query.isEmpty()) query = null;
        }
        this.currentQuery = query;
        applyFilters();
    }

    /**
     * 应用过滤条件
     */
    private void applyFilters() {
        List<StorySetting> filtered = new ArrayList<>();
        
        for (StorySetting setting : allSettings) {
            // 分类过滤
            if (currentCategoryFilter != null && !currentCategoryFilter.equals(setting.getCategory())) {
                continue;
            }
            
            // 子分类过滤
            if (currentSubCategoryFilter != null && !currentSubCategoryFilter.equals(setting.getSubCategory())) {
                continue;
            }
            
            // 关键词搜索
            if (currentQuery != null) {
                boolean matched = false;
                if (setting.getTitle() != null && setting.getTitle().toLowerCase().contains(currentQuery)) {
                    matched = true;
                }
                if (!matched && setting.getSummary() != null && setting.getSummary().toLowerCase().contains(currentQuery)) {
                    matched = true;
                }
                if (!matched && setting.getDetail() != null && setting.getDetail().toLowerCase().contains(currentQuery)) {
                    matched = true;
                }
                if (!matched) continue;
            }
            
            filtered.add(setting);
        }
        
        this.displaySettings = filtered;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_story_setting, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StorySetting setting = displaySettings.get(position);
        
        // 标题
        holder.tvTitle.setText(setting.getTitle());
        
        // 分类标签
        String categoryText = setting.getCategory() + " · " + setting.getSubCategory();
        holder.tvCategory.setText(categoryText);
        
        // 摘要预览
        if (setting.getSummary() != null && !setting.getSummary().isEmpty()) {
            String preview = setting.getSummary();
            if (preview.length() > 100) {
                preview = preview.substring(0, 100) + "...";
            }
            holder.tvSummary.setText(preview);
        } else {
            holder.tvSummary.setText("暂无摘要");
        }
        
        // 时间
        holder.tvTime.setText(dateFormat.format(new Date(setting.getCreateTime())));
        
        // 溯源信息
        if (setting.getSourceMaterialId() > 0) {
            holder.tvSource.setVisibility(View.VISIBLE);
            holder.tvSource.setText("📥 源自全局素材");
        } else {
            holder.tvSource.setVisibility(View.GONE);
        }
        
        // 标签预览
        loadTagsPreview(holder.chipGroupTagsPreview, setting.getTags());
        
        // 配图预览
        if (setting.getImagePath() != null && !setting.getImagePath().isEmpty()) {
            holder.ivSettingImage.setVisibility(View.VISIBLE);
            File imageFile = new File(setting.getImagePath());
            if (imageFile.exists()) {
                holder.ivSettingImage.setImageBitmap(BitmapFactory.decodeFile(setting.getImagePath()));
            } else {
                holder.ivSettingImage.setImageResource(R.drawable.ic_insert_drive_file);
            }
        } else {
            holder.ivSettingImage.setVisibility(View.GONE);
        }
        
        // 多选模式处理
        if (isSelectionMode) {
            holder.cbSelect.setVisibility(View.VISIBLE);
            holder.btnMoreMenu.setVisibility(View.GONE);
            holder.btnDelete.setVisibility(View.GONE);
            holder.cbSelect.setChecked(selectedPositions.contains(position));
            
            // Checkbox 点击切换选中状态
            holder.cbSelect.setOnClickListener(v -> {
                toggleSelection(position);
            });
            
            // 点击卡片也切换选中状态
            holder.itemView.setOnClickListener(v -> {
                toggleSelection(position);
            });
        } else {
            holder.cbSelect.setVisibility(View.GONE);
            holder.btnMoreMenu.setVisibility(View.VISIBLE);
            holder.btnDelete.setVisibility(View.GONE);  // 删除功能放在菜单中
            
            // 点击事件 - 卡片整体点击跳转到详情
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onSettingClick(setting);
                }
            });
            
            // 三点菜单
            holder.btnMoreMenu.setOnClickListener(v -> {
                showPopupMenu(v, setting, position);
            });
        }
    }

    @Override
    public int getItemCount() {
        return displaySettings != null ? displaySettings.size() : 0;
    }
    
    /**
     * 显示三点菜单
     */
    private void showPopupMenu(View anchorView, StorySetting setting, int position) {
        android.widget.PopupMenu popup = new android.widget.PopupMenu(anchorView.getContext(), anchorView);
        popup.getMenuInflater().inflate(R.menu.menu_setting_item, popup.getMenu());
        
        // 如果是小说专属设定（storyId > 0），隐藏“导入到小说”选项，显示“导出到全局素材”
        if (setting.getStoryId() > 0) {
            popup.getMenu().findItem(R.id.menu_import_to_story).setVisible(false);
            popup.getMenu().findItem(R.id.menu_export_to_global).setVisible(true);
        } else {
            // 全局素材库：隐藏“导出到全局素材”
            popup.getMenu().findItem(R.id.menu_export_to_global).setVisible(false);
        }
        
        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.menu_view_detail) {
                // 查看详情
                if (listener != null) {
                    listener.onSettingClick(setting);
                }
                return true;
            } else if (itemId == R.id.menu_import_to_story) {
                // 导入到小说
                if (importListener != null) {
                    importListener.onImportToStory(setting);
                }
                return true;
            } else if (itemId == R.id.menu_export_to_global) {
                // 导出到全局素材
                if (exportListener != null) {
                    exportListener.onExportToGlobal(setting);
                }
                return true;
            } else if (itemId == R.id.menu_delete) {
                // 删除
                if (deleteListener != null) {
                    deleteListener.onSettingDelete(setting, position);
                }
                return true;
            }
            return false;
        });
        
        popup.show();
    }
    
    /**
     * 加载标签预览（最多显示3个）
     */
    private void loadTagsPreview(ChipGroup chipGroup, String tagsJson) {
        chipGroup.removeAllViews();
        
        if (tagsJson == null || tagsJson.isEmpty()) {
            chipGroup.setVisibility(View.GONE);
            return;
        }
        
        try {
            Type type = new TypeToken<List<String>>(){}.getType();
            List<String> tagsList = new Gson().fromJson(tagsJson, type);
            
            if (tagsList == null || tagsList.isEmpty()) {
                chipGroup.setVisibility(View.GONE);
                return;
            }
            
            // 最多显示3个标签
            int maxDisplay = Math.min(tagsList.size(), 3);
            for (int i = 0; i < maxDisplay; i++) {
                Chip chip = new Chip(chipGroup.getContext());
                chip.setText(tagsList.get(i));
                chip.setCloseIconVisible(false);
                chip.setClickable(false);
                chip.setCheckable(false);
                
                // 设置小尺寸
                chip.setTextSize(12);       // 字体大小12sp
                chip.setPaddingRelative(8, 4, 8, 4);  // 左右内边距
                
                chipGroup.addView(chip);
            }
            
            // 如果还有更多标签，显示"+N"
            if (tagsList.size() > 3) {
                Chip moreChip = new Chip(chipGroup.getContext());
                moreChip.setText("+" + (tagsList.size() - 3));
                moreChip.setCloseIconVisible(false);
                moreChip.setClickable(false);
                moreChip.setCheckable(false);
                moreChip.setTextSize(12);
                moreChip.setPaddingRelative(8, 4, 8, 4);
                chipGroup.addView(moreChip);
            }
            
            chipGroup.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            chipGroup.setVisibility(View.GONE);
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivSettingImage;  // 配图预览
        TextView tvTitle;
        TextView tvCategory;
        TextView tvSummary;
        TextView tvTime;
        TextView tvSource;
        ImageView btnMoreMenu;  // 三点菜单
        ImageView btnDelete;  // 删除按钮
        CheckBox cbSelect;  // 多选复选框
        ChipGroup chipGroupTagsPreview;  // 标签预览

        ViewHolder(View itemView) {
            super(itemView);
            ivSettingImage = itemView.findViewById(R.id.iv_setting_image);
            tvTitle = itemView.findViewById(R.id.tv_setting_title);
            tvCategory = itemView.findViewById(R.id.tv_setting_category);
            tvSummary = itemView.findViewById(R.id.tv_setting_summary);
            tvTime = itemView.findViewById(R.id.tv_setting_time);
            tvSource = itemView.findViewById(R.id.tv_setting_source);
            btnMoreMenu = itemView.findViewById(R.id.btn_more_menu);
            btnDelete = itemView.findViewById(R.id.btn_delete_setting);
            cbSelect = itemView.findViewById(R.id.cb_select);
            chipGroupTagsPreview = itemView.findViewById(R.id.chip_group_tags_preview);
        }
    }
}
