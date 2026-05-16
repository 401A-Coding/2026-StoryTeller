package com.example.storyteller.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.storyteller.R;
import com.example.storyteller.base.BaseFragment;
import com.example.storyteller.data.local.db.StorySettingDao;
import com.example.storyteller.model.StorySetting;
import com.example.storyteller.ui.activity.SettingDetailActivity;
import com.example.storyteller.ui.adapter.StorySettingAdapter;
import com.example.storyteller.utils.SettingCategoryConfig;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.List;

/**
 * 小说设定列表面板
 * 显示当前小说的所有设定，支持创建、查看、编辑、删除
 */
public class StorySettingsListFragment extends BaseFragment {

    private static final String ARG_STORY_ID = "story_id";

    private RecyclerView rvSettings;
    private LinearLayout layoutEmptyHint;  // 空状态容器
    private Button btnCreateSetting;
    private EditText etSearch;  // 搜索框
    private ChipGroup chipGroupMainCategory;  // 主分类筛选器
    private ChipGroup chipGroupSubCategory;  // 子分类筛选器
    private View layoutSubCategory;  // 子分类容器（用于控制显示/隐藏）

    private StorySettingDao settingDao;
    private StorySettingAdapter adapter;
    private int storyId;

    /**
     * 创建实例
     * @param storyId 小说ID
     */
    public static StorySettingsListFragment newInstance(int storyId) {
        StorySettingsListFragment fragment = new StorySettingsListFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_STORY_ID, storyId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_story_settings_list;
    }

    @Override
    protected void initView(View view) {
        rvSettings = view.findViewById(R.id.rv_settings);
        layoutEmptyHint = view.findViewById(R.id.tv_empty_hint);
        btnCreateSetting = view.findViewById(R.id.btn_create_setting);
        etSearch = view.findViewById(R.id.et_search);
        chipGroupMainCategory = view.findViewById(R.id.chip_group_main_category);
        chipGroupSubCategory = view.findViewById(R.id.chip_group_sub_category);
        layoutSubCategory = view.findViewById(R.id.layout_sub_category);

        // 设置RecyclerView
        rvSettings.setLayoutManager(new LinearLayoutManager(requireContext()));

        // 创建按钮
        btnCreateSetting.setOnClickListener(v -> showCreateSettingDialog());
        
        // 搜索功能
        setupSearch();
        
        // 分类筛选
        setupCategoryFilter();
    }

    @Override
    protected void initData() {
        if (getArguments() != null) {
            storyId = getArguments().getInt(ARG_STORY_ID, -1);
        }

        if (storyId <= 0) {
            // 找到空状态容器中的第一个TextView并设置文本
            TextView tvMessage = layoutEmptyHint.findViewById(android.R.id.text1);
            if (tvMessage == null) {
                // 如果找不到，尝试获取第一个子View
                if (layoutEmptyHint.getChildCount() > 0) {
                    View firstChild = layoutEmptyHint.getChildAt(0);
                    if (firstChild instanceof TextView) {
                        ((TextView) firstChild).setText("无效的小说ID");
                    }
                }
            } else {
                tvMessage.setText("无效的小说ID");
            }
            return;
        }

        settingDao = new StorySettingDao(requireContext());
        refreshSettingsList();
    }

    @Override
    public void onResume() {
        super.onResume();
        // 从详情页返回时刷新列表（支持新建和编辑）
        if (settingDao != null && storyId > 0) {
            refreshSettingsList();
        }
    }

    /**
     * 刷新设定列表
     */
    private void refreshSettingsList() {
        List<StorySetting> settings = settingDao.getByStoryId(storyId);

        if (adapter == null) {
            adapter = new StorySettingAdapter(settings);
            
            // 点击卡片跳转到详情
            adapter.setOnSettingClickListener(setting -> {
                Intent intent = new Intent(requireContext(), SettingDetailActivity.class);
                intent.putExtra(SettingDetailActivity.EXTRA_SETTING_ID, setting.getId());
                intent.putExtra(SettingDetailActivity.EXTRA_STORY_ID, storyId);
                startActivity(intent);
            });
            
            // 删除按钮回调
            adapter.setOnSettingDeleteListener((setting, position) -> {
                showDeleteConfirmDialog(setting, position);
            });
            
            // 导出回调（单条）
            adapter.setOnExportListener(setting -> {
                exportSettingToGlobal(setting);
            });
            
            rvSettings.setAdapter(adapter);
        } else {
            adapter.setData(settings);
        }

        updateEmptyHint();
    }
    
    /**
     * 显示删除确认对话框
     */
    private void showDeleteConfirmDialog(StorySetting setting, int position) {
        new AlertDialog.Builder(requireContext())
            .setTitle("删除设定")
            .setMessage("确定要删除「" + setting.getTitle() + "」吗？\n此操作不可恢复。")
            .setPositiveButton("删除", (dialog, which) -> {
                deleteSetting(setting, position);
            })
            .setNegativeButton("取消", null)
            .show();
    }
    
    /**
     * 删除设定
     */
    private void deleteSetting(StorySetting setting, int position) {
        int result = settingDao.delete(setting.getId());
        if (result > 0) {
            android.widget.Toast.makeText(requireContext(), "已删除", android.widget.Toast.LENGTH_SHORT).show();
            refreshSettingsList();  // 刷新列表
        } else {
            android.widget.Toast.makeText(requireContext(), "删除失败", android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 更新空状态提示
     */
    private void updateEmptyHint() {
        if (adapter == null || adapter.getItemCount() == 0) {
            layoutEmptyHint.setVisibility(View.VISIBLE);
        } else {
            layoutEmptyHint.setVisibility(View.GONE);
        }
    }

    /**
     * 显示创建设定对话框
     */
    private void showCreateSettingDialog() {
        // 直接跳转到 SettingDetailActivity，传入 storyId，settingId = -1 表示新建
        Intent intent = new Intent(requireContext(), SettingDetailActivity.class);
        intent.putExtra(SettingDetailActivity.EXTRA_SETTING_ID, -1);  // -1 表示新建
        intent.putExtra(SettingDetailActivity.EXTRA_STORY_ID, storyId);
        startActivity(intent);
    }
    
    /**
     * 导出单个设定到全局素材库
     */
    private void exportSettingToGlobal(StorySetting novelSetting) {
        try {
            StorySettingDao globalDao = new StorySettingDao(requireContext());
            
            // 1. 检查全局素材库是否已存在同名设定
            List<StorySetting> existing = globalDao.getByStoryId(0);  // storyId=0 表示全局素材库
            boolean nameExists = false;
            for (StorySetting s : existing) {
                if (s.getTitle().equals(novelSetting.getTitle())) {
                    nameExists = true;
                    break;
                }
            }
            
            // 2. 创建副本
            StorySetting copy = new StorySetting();
            copy.setStoryId(0);  // 设置为全局素材库
            copy.setTitle(nameExists ? novelSetting.getTitle() + " (副本)" : novelSetting.getTitle());
            copy.setCategory(novelSetting.getCategory());
            copy.setSubCategory(novelSetting.getSubCategory());
            copy.setSummary(novelSetting.getSummary());
            copy.setDetail(novelSetting.getDetail());
            copy.setTags(novelSetting.getTags());
            copy.setAliases(novelSetting.getAliases());
            copy.setFavorite(false);  // 不继承收藏状态
            copy.setSourceMaterialId(0);  // 导出的素材不再记录溯源（独立素材）
            copy.setCreateTime(System.currentTimeMillis());
            
            // 3. 插入数据库
            long newId = globalDao.insert(copy);
            
            if (newId > 0) {
                android.widget.Toast.makeText(requireContext(), 
                    "导出成功" + (nameExists ? "（已重命名）" : ""), 
                    android.widget.Toast.LENGTH_SHORT).show();
            } else {
                android.widget.Toast.makeText(requireContext(), "导出失败", android.widget.Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            android.widget.Toast.makeText(requireContext(), "导出失败：" + e.getMessage(), android.widget.Toast.LENGTH_LONG).show();
        }
    }
    
    /**
     * 设置搜索功能
     */
    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (adapter != null) {
                    adapter.search(s.toString());
                }
            }
        });
    }
    
    /**
     * 设置分类筛选器
     */
    private void setupCategoryFilter() {
        // 添加主分类
        addMainCategoryChip("全部", true);
        
        String[] categories = SettingCategoryConfig.getAllMainCategories();
        for (String category : categories) {
            addMainCategoryChip(category, false);
        }
    }
    
    /**
     * 添加主分类Chip
     */
    private void addMainCategoryChip(String category, boolean isDefault) {
        Chip chip = new Chip(requireContext());
        chip.setText(category);
        chip.setCheckable(true);
        chip.setChecked(isDefault);
        chip.setCloseIconVisible(false);
        
        chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // 取消其他主分类Chip的选中状态
                for (int i = 0; i < chipGroupMainCategory.getChildCount(); i++) {
                    View child = chipGroupMainCategory.getChildAt(i);
                    if (child instanceof Chip && child != chip) {
                        ((Chip) child).setChecked(false);
                    }
                }
                
                // 应用筛选
                if (adapter != null) {
                    if ("全部".equals(category)) {
                        adapter.filterByCategory(null);
                        adapter.filterBySubCategory(null);
                        layoutSubCategory.setVisibility(View.GONE);  // 隐藏子分类
                    } else {
                        adapter.filterByCategory(category);
                        adapter.filterBySubCategory(null);
                        showSubCategories(category);  // 显示对应的子分类
                    }
                }
            }
        });
        
        chipGroupMainCategory.addView(chip);
    }
    
    /**
     * 显示子分类
     */
    private void showSubCategories(String mainCategory) {
        // 清空子分类
        chipGroupSubCategory.removeAllViews();
        
        // 添加“全部”选项
        addSubCategoryChip(mainCategory, "全部", true);
        
        // 添加所有子分类
        String[] subCategories = SettingCategoryConfig.getSubCategories(mainCategory);
        for (String subCategory : subCategories) {
            addSubCategoryChip(mainCategory, subCategory, false);
        }
        
        // 显示子分类容器
        layoutSubCategory.setVisibility(View.VISIBLE);
    }
    
    /**
     * 添加子分类Chip
     */
    private void addSubCategoryChip(String mainCategory, String subCategory, boolean isDefault) {
        Chip chip = new Chip(requireContext());
        chip.setText(subCategory);
        chip.setCheckable(true);
        chip.setChecked(isDefault);
        chip.setCloseIconVisible(false);
        
        chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // 取消其他子分类Chip的选中状态
                for (int i = 0; i < chipGroupSubCategory.getChildCount(); i++) {
                    View child = chipGroupSubCategory.getChildAt(i);
                    if (child instanceof Chip && child != chip) {
                        ((Chip) child).setChecked(false);
                    }
                }
                
                // 应用筛选
                if (adapter != null) {
                    if ("全部".equals(subCategory)) {
                        adapter.filterByCategory(mainCategory);
                        adapter.filterBySubCategory(null);
                    } else {
                        adapter.filterByCategory(mainCategory);
                        adapter.filterBySubCategory(subCategory);
                    }
                }
            }
        });
        
        chipGroupSubCategory.addView(chip);
    }
}
