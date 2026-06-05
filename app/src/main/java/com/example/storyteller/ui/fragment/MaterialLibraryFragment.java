package com.example.storyteller.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.storyteller.R;
import com.example.storyteller.base.BaseFragment;
import com.example.storyteller.data.local.db.SettingRelationshipDao;
import com.example.storyteller.data.local.db.StorySettingDao;
import com.example.storyteller.data.remote.GenericContentExtractor;
import com.example.storyteller.data.remote.MaterialCandidateExtractor;
import com.example.storyteller.data.remote.NovelCrawler;
import com.example.storyteller.data.remote.ApiKeyManager;
import com.example.storyteller.data.remote.ModelConfig;
import com.example.storyteller.ui.dialog.ModelProviderSettingsDialogHelper;
import com.example.storyteller.model.NovelSummary;
import com.example.storyteller.model.StorySetting;
import com.example.storyteller.ui.activity.SettingDetailActivity;
import com.example.storyteller.ui.adapter.StorySettingAdapter;
import com.example.storyteller.ui.dialog.MaterialCandidateReviewDialogFragment;
import com.example.storyteller.utils.SettingCategoryConfig;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * 素材库Fragment（简化版 - Phase 1）
 * <p>
 * 当前功能：
 * - 显示小说专属设定列表
 * - 按6大分类筛选
 * - 搜索功能
 * - 点击查看详情
 * <p>
 * 后续扩展：
 * - 全局素材库模式
 * - 导入功能
 * - 创建/编辑/删除设定
 */
public class MaterialLibraryFragment extends BaseFragment {

    private static final String ARG_STORY_ID = "story_id";

    // 使用统一配置类
    private static final String[] CATEGORIES = SettingCategoryConfig.getAllMainCategories();

    /**
     * 创建实例
     *
     * @param storyId 小说ID（0表示全局素材库）
     */
    public static MaterialLibraryFragment newInstance(int storyId) {
        MaterialLibraryFragment fragment = new MaterialLibraryFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_STORY_ID, storyId);
        fragment.setArguments(args);
        return fragment;
    }

    private EditText etSearch;
    private RecyclerView rvSettings;
    private TextView tvEmptyHint;
    private Button btnCreateSetting;  // 创建按钮
    private ChipGroup chipGroupMainCategory;  // 主分类筛选器
    private ChipGroup chipGroupSubCategory;  // 子分类筛选器
    private View layoutSubCategory;  // 子分类容器（用于控制显示/隐藏）
    private CheckBox cbMultiSelect;  // 多选按钮
    private View layoutBatchActions;  // 批量操作栏
    private TextView tvSelectedCount;  // 已选择数量
    private Button btnSelectAll;  // 全选按钮
    private Button btnBatchImport;  // 批量导入
    private Button btnBatchDelete;  // 批量删除

    private StorySettingDao settingDao;
    private StorySettingAdapter adapter;
    private int currentStoryId = 0;  // 所属小说ID（0表示全局素材库）

    private NovelCrawler novelCrawler;
    private GenericContentExtractor contentExtractor;
    
    // 预存素材URL - 番茄小说《黄昏分界》
    private static final String PRESET_MATERIAL_URL = "https://fanqienovel.com/page/7631534484248087577";
    private static final String PRESET_MATERIAL_TITLE = "黄昏分界";
    private static final String PRESET_MATERIAL_AUTHOR = "黑山老鬼";
    private boolean presetInProgress = false;

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_material_library;
    }

    @Override
    protected void initView(View view) {
        etSearch = view.findViewById(R.id.et_material_search);
        rvSettings = view.findViewById(R.id.rv_material);
        tvEmptyHint = view.findViewById(R.id.tv_crawl_status);
        chipGroupMainCategory = view.findViewById(R.id.chip_group_main_category);
        chipGroupSubCategory = view.findViewById(R.id.chip_group_sub_category);
        layoutSubCategory = view.findViewById(R.id.layout_sub_category);

        // 隐藏不需要的元素
        View btnImport = view.findViewById(R.id.btn_import);
        if (btnImport != null) {
            btnImport.setVisibility(View.VISIBLE);
            btnImport.setOnClickListener(v -> {
                String apiKey = ApiKeyManager.getApiKey(requireContext(), ModelConfig.Provider.DEEPSEEK);
                if (apiKey == null || apiKey.isEmpty()) {
                    ModelProviderSettingsDialogHelper.showApiKeyRequiredDialog(requireContext(), "DeepSeek", "导入素材");
                    return;
                }
                if (!ModelConfig.isProviderEnabled(requireContext(), ModelConfig.Provider.DEEPSEEK)) {
                    ModelProviderSettingsDialogHelper.showProviderDisabledDialog(requireContext(), "DeepSeek", "导入素材");
                    return;
                }
                showImportUrlDialog();
            });
        }

        View tvCrawlSection = view.findViewById(R.id.tv_crawl_section);
        if (tvCrawlSection != null) tvCrawlSection.setVisibility(View.GONE);

        View tvCrawlTypeLabel = view.findViewById(R.id.tv_crawl_type_label);
        if (tvCrawlTypeLabel != null) tvCrawlTypeLabel.setVisibility(View.GONE);

        // 获取创建按钮引用（在 initData 中根据 storyId 决定是否显示）
        btnCreateSetting = view.findViewById(R.id.btn_create_material);
        if (btnCreateSetting != null) {
            btnCreateSetting.setOnClickListener(v -> showCreateSettingDialog());
        }

        // 设置RecyclerView
        rvSettings.setLayoutManager(new LinearLayoutManager(requireContext()));

        // 搜索功能
        etSearch.setHint("搜索设定...");
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (adapter != null) {
                    adapter.search(s.toString());
                    updateEmptyHint();
                }
            }
        });

        // 分类筛选
        setupCategoryFilter();

        // 多选功能
        setupMultiSelect(view);
    }

    @Override
    protected void initData() {
        // 从参数获取小说ID
        if (getArguments() != null) {
            currentStoryId = getArguments().getInt(ARG_STORY_ID, 0);
        }

        settingDao = new StorySettingDao(requireContext());
        novelCrawler = new NovelCrawler();
        contentExtractor = new GenericContentExtractor();
        refreshSettingsList();
        
        // 全局素材库为空时，自动预存人物素材
        if (currentStoryId == 0 && !presetInProgress) {
            checkAndPresetMaterials();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // 从详情页返回时刷新列表（支持新建和编辑）
        if (settingDao != null) {
            refreshSettingsList();
        }
    }

    /**
     * 刷新设定列表
     */
    private void refreshSettingsList() {
        List<StorySetting> settings;

        if (currentStoryId > 0) {
            // 查询某小说的专属设定
            settings = settingDao.getByStoryId(currentStoryId);
        } else {
            // 查询全局素材库（story_id = 0）
            settings = settingDao.getByStoryId(0);
        }

        if (adapter == null) {
            adapter = new StorySettingAdapter(settings);

            // 点击卡片跳转到详情
            adapter.setOnSettingClickListener(setting -> {
                    Intent intent = new Intent(requireContext(), SettingDetailActivity.class);
                    intent.putExtra(SettingDetailActivity.EXTRA_SETTING_ID, setting.getId());
                    intent.putExtra(SettingDetailActivity.EXTRA_STORY_ID, currentStoryId);
                    startActivity(intent);
            });

            // 删除按钮回调
            adapter.setOnSettingDeleteListener((setting, position) -> {
                showDeleteConfirmDialog(setting, position);
            });

            // 导入回调（单条）
            adapter.setOnImportListener(setting -> {
                showImportDialog(setting);
            });

            // 导出回调（单条）
            adapter.setOnExportListener(setting -> {
                exportSettingToGlobal(setting);
            });

            // 多选模式监听
            adapter.setOnSelectionModeChangeListener((isInSelectionMode, selectedCount) -> {
                updateSelectedCount(selectedCount);
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
        String title = currentStoryId == 0 ? "全局素材" : "小说设定";
        new AlertDialog.Builder(requireContext())
                .setTitle("删除" + title)
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
        // 先删除关联关系
        SettingRelationshipDao relationshipDao = new SettingRelationshipDao(requireContext());
        relationshipDao.deleteBySettingId(setting.getId());
        
        // 再删除设定本身
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
            tvEmptyHint.setVisibility(View.VISIBLE);
            tvEmptyHint.setText(R.string.setting_empty_hint);
        } else {
            tvEmptyHint.setVisibility(View.GONE);
        }
    }

    /**
     * 显示创建设定对话框
     */
    private void showCreateSettingDialog() {
        // 直接跳转到 SettingDetailActivity，传入 storyId，settingId = -1 表示新建
        Intent intent = new Intent(requireContext(), SettingDetailActivity.class);
        intent.putExtra(SettingDetailActivity.EXTRA_SETTING_ID, -1);  // -1 表示新建
        intent.putExtra(SettingDetailActivity.EXTRA_STORY_ID, currentStoryId);
        startActivity(intent);
    }

    /**
     * 显示导入对话框（单条）
     */
    private void showImportDialog(StorySetting globalSetting) {
        // 获取用户的所有小说
        com.example.storyteller.data.local.db.StoryDao storyDao = new com.example.storyteller.data.local.db.StoryDao(requireContext());
        List<com.example.storyteller.model.Story> stories = storyDao.getAllStories();

        if (stories == null || stories.isEmpty()) {
            android.widget.Toast.makeText(getContext(), "请先创建一部小说", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        if (stories.size() == 1) {
            // 只有一部小说，直接导入
            importSettingToStory(globalSetting, stories.get(0).getId());
        } else {
            // 多部小说，弹出选择对话框
            String[] storyNames = new String[stories.size()];
            for (int i = 0; i < stories.size(); i++) {
                storyNames[i] = stories.get(i).getTitle();
            }

            new AlertDialog.Builder(requireContext())
                    .setTitle("选择目标小说")
                    .setItems(storyNames, (dialog, which) -> {
                        int targetStoryId = stories.get(which).getId();
                        importSettingToStory(globalSetting, targetStoryId);
                    })
                    .show();
        }
    }

    /**
     * 导入单个设定到小说
     */
    private void importSettingToStory(StorySetting globalSetting, int targetStoryId) {
        try {
            // 1. 检查是否已存在同名设定
            List<StorySetting> existing = settingDao.getByStoryId(targetStoryId);
            boolean nameExists = false;
            for (StorySetting s : existing) {
                if (s.getTitle().equals(globalSetting.getTitle())) {
                    nameExists = true;
                    break;
                }
            }

            // 2. 创建副本
            StorySetting copy = new StorySetting();
            copy.setStoryId(targetStoryId);
            copy.setTitle(nameExists ? globalSetting.getTitle() + " (副本)" : globalSetting.getTitle());
            copy.setCategory(globalSetting.getCategory());
            copy.setSubCategory(globalSetting.getSubCategory());
            copy.setSummary(globalSetting.getSummary());
            copy.setDetail(globalSetting.getDetail());
            copy.setTags(globalSetting.getTags());
            copy.setAliases(globalSetting.getAliases());
            copy.setFavorite(false);  // 不继承收藏状态
            copy.setSourceMaterialId(globalSetting.getId());  // 记录来源
            copy.setCreateTime(System.currentTimeMillis());

            // 3. 插入数据库
            long newId = settingDao.insert(copy);

            if (newId > 0) {
                android.widget.Toast.makeText(getContext(),
                        "导入成功" + (nameExists ? "（已重命名）" : ""),
                        android.widget.Toast.LENGTH_SHORT).show();
            } else {
                android.widget.Toast.makeText(getContext(), "导入失败", android.widget.Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            android.widget.Toast.makeText(getContext(), "导入失败：" + e.getMessage(), android.widget.Toast.LENGTH_LONG).show();
        }
    }

    /**
     * 导出单个设定到全局素材库
     */
    private void exportSettingToGlobal(StorySetting novelSetting) {
        try {
            // 1. 检查全局素材库是否已存在同名设定
            List<StorySetting> existing = settingDao.getByStoryId(0);  // storyId=0 表示全局素材库
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
            long newId = settingDao.insert(copy);

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
                    updateEmptyHint();
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
                    updateEmptyHint();
                }
            }
        });

        chipGroupSubCategory.addView(chip);
    }

    /**
     * 设置多选功能
     */
    private void setupMultiSelect(View view) {
        cbMultiSelect = view.findViewById(R.id.cb_multi_select);
        layoutBatchActions = view.findViewById(R.id.layout_batch_actions);
        tvSelectedCount = view.findViewById(R.id.tv_selected_count);
        btnSelectAll = view.findViewById(R.id.btn_select_all);
        btnBatchImport = view.findViewById(R.id.btn_batch_import);
        btnBatchDelete = view.findViewById(R.id.btn_batch_delete);

        // 多选按钮点击
        cbMultiSelect.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (adapter != null) {
                adapter.setSelectionMode(isChecked);

                if (isChecked) {
                    // 进入多选模式，显示批量操作栏
                    layoutBatchActions.setVisibility(View.VISIBLE);
                    updateSelectedCount(0);
                } else {
                    // 退出多选模式，隐藏批量操作栏
                    layoutBatchActions.setVisibility(View.GONE);
                }
            }
        });

        // 全选按钮
        btnSelectAll.setOnClickListener(v -> {
            if (adapter != null) {
                adapter.selectAll();
                updateSelectedCount(adapter.getSelectedSettings().size());
            }
        });

        // 批量导入
        btnBatchImport.setOnClickListener(v -> {
            List<StorySetting> selected = adapter.getSelectedSettings();
            if (selected.isEmpty()) {
                android.widget.Toast.makeText(getContext(), "请先选择要导入的素材", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            showBatchImportDialog(selected);
        });

        // 批量删除
        btnBatchDelete.setOnClickListener(v -> {
            List<StorySetting> selected = adapter.getSelectedSettings();
            if (selected.isEmpty()) {
                android.widget.Toast.makeText(getContext(), "请先选择要删除的素材", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            showBatchDeleteDialog(selected);
        });
    }

    /**
     * 更新已选择数量显示
     */
    private void updateSelectedCount(int count) {
        if (tvSelectedCount != null) {
            tvSelectedCount.setText("已选择 " + count + " 项");
        }
    }

    /**
     * 显示批量导入对话框
     */
    private void showBatchImportDialog(List<StorySetting> selectedSettings) {
        // 获取用户的所有小说
        com.example.storyteller.data.local.db.StoryDao storyDao = new com.example.storyteller.data.local.db.StoryDao(requireContext());
        List<com.example.storyteller.model.Story> stories = storyDao.getAllStories();

        if (stories == null || stories.isEmpty()) {
            android.widget.Toast.makeText(getContext(), "请先创建一部小说", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        if (stories.size() == 1) {
            // 只有一部小说，直接导入
            batchImportToStory(selectedSettings, stories.get(0).getId());
        } else {
            // 多部小说，弹出选择对话框
            String[] storyNames = new String[stories.size()];
            for (int i = 0; i < stories.size(); i++) {
                storyNames[i] = stories.get(i).getTitle();
            }

            new AlertDialog.Builder(requireContext())
                    .setTitle("选择目标小说")
                    .setItems(storyNames, (dialog, which) -> {
                        int targetStoryId = stories.get(which).getId();
                        batchImportToStory(selectedSettings, targetStoryId);
                    })
                    .show();
        }
    }

    /**
     * 批量导入设定到小说
     */
    private void batchImportToStory(List<StorySetting> selectedSettings, int targetStoryId) {
        int successCount = 0;
        int renamedCount = 0;

        // 获取目标小说的现有设定（用于检查重名）
        List<StorySetting> existing = settingDao.getByStoryId(targetStoryId);
        java.util.Set<String> existingTitles = new java.util.HashSet<>();
        for (StorySetting s : existing) {
            existingTitles.add(s.getTitle());
        }

        for (StorySetting globalSetting : selectedSettings) {
            try {
                // 检查是否重名
                boolean nameExists = existingTitles.contains(globalSetting.getTitle());
                if (nameExists) {
                    renamedCount++;
                }

                // 创建副本
                StorySetting copy = new StorySetting();
                copy.setStoryId(targetStoryId);
                copy.setTitle(nameExists ? globalSetting.getTitle() + " (副本)" : globalSetting.getTitle());
                copy.setCategory(globalSetting.getCategory());
                copy.setSubCategory(globalSetting.getSubCategory());
                copy.setSummary(globalSetting.getSummary());
                copy.setDetail(globalSetting.getDetail());
                copy.setTags(globalSetting.getTags());
                copy.setAliases(globalSetting.getAliases());
                copy.setFavorite(false);
                copy.setSourceMaterialId(globalSetting.getId());
                copy.setCreateTime(System.currentTimeMillis());

                // 插入数据库
                long newId = settingDao.insert(copy);
                if (newId > 0) {
                    successCount++;
                    // 添加到已存在集合，避免后续重复检测
                    existingTitles.add(copy.getTitle());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // 退出多选模式
        cbMultiSelect.setChecked(false);

        // 刷新列表
        refreshSettingsList();

        // 显示结果
        String message = "已导入 " + successCount + " 个素材";
        if (renamedCount > 0) {
            message += "（其中 " + renamedCount + " 个已重命名）";
        }
        android.widget.Toast.makeText(getContext(), message, android.widget.Toast.LENGTH_LONG).show();
    }

    /**
     * 显示批量删除对话框
     */
    private void showBatchDeleteDialog(List<StorySetting> selectedSettings) {
        new AlertDialog.Builder(requireContext())
                .setTitle("批量删除")
                .setMessage("确定要删除选中的 " + selectedSettings.size() + " 个素材吗？\n此操作不可恢复。")
                .setPositiveButton("删除", (dialog, which) -> {
                    batchDeleteSettings(selectedSettings);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 批量删除
     */
    private void batchDeleteSettings(List<StorySetting> settings) {
        SettingRelationshipDao relationshipDao = new SettingRelationshipDao(requireContext());
        int successCount = 0;
        for (StorySetting setting : settings) {
            // 先删除关联关系
            relationshipDao.deleteBySettingId(setting.getId());
            // 再删除设定本身
            int result = settingDao.delete(setting.getId());
            if (result > 0) {
                successCount++;
            }
        }

        android.widget.Toast.makeText(getContext(),
                "已删除 " + successCount + " 个素材",
                android.widget.Toast.LENGTH_SHORT).show();

        // 退出多选模式
        cbMultiSelect.setChecked(false);

        // 刷新列表
        refreshSettingsList();
    }

    /**
     * 显示导入URL对话框
     */
    private void showImportUrlDialog() {
        android.widget.EditText input = new android.widget.EditText(requireContext());
        input.setHint("请输入素材来源 URL");
        input.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_URI);

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("导入素材")
                .setView(input)
                .setPositiveButton("下一步", (dialog, which) -> {
                    String url = input.getText() == null ? "" : input.getText().toString().trim();
                    if (url.isEmpty()) {
                        android.widget.Toast.makeText(getContext(), "请输入 URL", android.widget.Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // 显示素材类型多选对话
                    showTypeSelectionDialog(url);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /**
     * 显示素材类型选择对话框
     */
    private void showTypeSelectionDialog(String url) {
        // 使用新版5大分类体系
        final String[] typeLabels = SettingCategoryConfig.getAllMainCategories();
        final boolean[] checked = new boolean[typeLabels.length];
        // 默认全选
        for (int i = 0; i < checked.length; i++) {
            checked[i] = true;
        }

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("选择要导入的素材类型")
                .setMultiChoiceItems(typeLabels, checked, (dialog, which, isChecked) -> checked[which] = isChecked)
                .setPositiveButton("导入", (dialog, which) -> {
                    List<String> selected = new ArrayList<>();
                    for (int i = 0; i < checked.length; i++) {
                        if (checked[i]) {
                            // 将中文分类名映射为AI提取类型
                            String type = mapCategoryToExtractType(typeLabels[i]);
                            if (type != null) {
                                selected.add(type);
                            }
                        }
                    }

                    if (selected.isEmpty()) {
                        android.widget.Toast.makeText(getContext(), "请至少选择一种素材类型", android.widget.Toast.LENGTH_SHORT).show();
                        return;
                    }

                    startImporting(url, selected);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /**
     * 将新版分类名映射为AI提取类型
     */
    private String mapCategoryToExtractType(String categoryName) {
        switch (categoryName) {
            case "世界":
                return MaterialCandidateExtractor.TYPE_WORLD;
            case "角色":
                return MaterialCandidateExtractor.TYPE_CHARACTER;
            case "地点":
                return MaterialCandidateExtractor.TYPE_LOCATION;
            case "剧情":
                return MaterialCandidateExtractor.TYPE_PLOT;
            case "规则体系":
                return MaterialCandidateExtractor.TYPE_SYSTEM;
            case "创作控制":
                return MaterialCandidateExtractor.TYPE_CREATIVE_CONTROL;
            default:
                // 未知分类默认使用角色
                return MaterialCandidateExtractor.TYPE_CHARACTER;
        }
    }

    /**
     * 开始导入素材
     */
    private void startImporting(String url, List<String> selectedTypes) {
        // 显示加载状态
        tvEmptyHint.setVisibility(View.VISIBLE);
        tvEmptyHint.setText("正在提取内容...");

        // 判断是否为番茄小说链接
        if (url.contains("fanqienovel.com")) {
            // 使用番茄小说爬虫
            crawlFromFanqie(url, selectedTypes);
        } else {
            // 使用通用提取器
            extractFromGenericUrl(url, selectedTypes);
        }
    }

    /**
     * 从番茄小说爬取
     */
    private void crawlFromFanqie(String url, List<String> selectedTypes) {
        novelCrawler.crawlAndExtract(url, requireContext(), selectedTypes, new NovelCrawler.ExtractCallback() {
            @Override
            public void onSuccess(NovelSummary summary, List<StorySetting> settings, String rawJson) {
                requireActivity().runOnUiThread(() -> {
                    saveSettingsToGlobalLibrary(settings, rawJson);
                });
            }

            @Override
            public void onFailure(Exception e) {
                requireActivity().runOnUiThread(() -> {
                    tvEmptyHint.setText("提取失败: " + e.getMessage());
                    android.widget.Toast.makeText(getContext(), "提取失败: " + e.getMessage(), android.widget.Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    /**
     * 从通用URL提取
     */
    private void extractFromGenericUrl(String url, List<String> selectedTypes) {
        contentExtractor.extract(url, new GenericContentExtractor.ExtractCallback() {
            @Override
            public void onSuccess(NovelSummary summary) {
                requireActivity().runOnUiThread(() -> {
                    // 对于非番茄小说，直接使用AI提取素材
                    extractSettingsFromSummary(summary, selectedTypes);
                });
            }

            @Override
            public void onFailure(Exception e) {
                requireActivity().runOnUiThread(() -> {
                    tvEmptyHint.setText("提取失败: " + e.getMessage());
                    android.widget.Toast.makeText(getContext(), "提取失败: " + e.getMessage(), android.widget.Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    /**
     * 从摘要中使用AI提取素材
     */
    private void extractSettingsFromSummary(NovelSummary summary, List<String> selectedTypes) {
        tvEmptyHint.setText("正在AI分析...");

        MaterialCandidateExtractor extractor = new MaterialCandidateExtractor();
        extractor.extract(summary, requireContext(), selectedTypes, new MaterialCandidateExtractor.Callback() {
            @Override
            public void onSuccess(List<StorySetting> settings, String rawJson) {
                requireActivity().runOnUiThread(() -> {
                    saveSettingsToGlobalLibrary(settings, rawJson);
                });
            }

            @Override
            public void onFailure(Exception e) {
                requireActivity().runOnUiThread(() -> {
                    tvEmptyHint.setText("AI分析失败: " + e.getMessage());
                    android.widget.Toast.makeText(getContext(), "AI分析失败: " + e.getMessage(), android.widget.Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    /**
     * 保存素材到全局素材库（带审核）- AI直接返回StorySetting
     */
    private void saveSettingsToGlobalLibrary(List<StorySetting> settings, String rawJson) {
        if (settings == null || settings.isEmpty()) {
            tvEmptyHint.setText("未提取到素材");
            android.widget.Toast.makeText(getContext(), "未提取到素材", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        // 显示审核对话框
        showReviewDialog(settings);
    }

    /**
     * 检查并预存素材（全局素材库为空时自动从预设URL爬取）
     */
    private void checkAndPresetMaterials() {
        List<StorySetting> existing = settingDao.getByStoryId(0);
        if (existing != null && !existing.isEmpty()) {
            return; // 已有素材，不需要预存
        }

        presetInProgress = true;
        tvEmptyHint.setVisibility(View.VISIBLE);
        tvEmptyHint.setText("正在预存素材...");

        // 请求所有分类的素材
        List<String> types = new ArrayList<>();
        types.add(MaterialCandidateExtractor.TYPE_WORLD);
        types.add(MaterialCandidateExtractor.TYPE_CHARACTER);
        types.add(MaterialCandidateExtractor.TYPE_LOCATION);
        types.add(MaterialCandidateExtractor.TYPE_PLOT);
        types.add(MaterialCandidateExtractor.TYPE_SYSTEM);
        types.add(MaterialCandidateExtractor.TYPE_CREATIVE_CONTROL);

        novelCrawler.crawlAndExtract(PRESET_MATERIAL_URL, requireContext(), types, new NovelCrawler.ExtractCallback() {
            @Override
            public void onSuccess(NovelSummary summary, List<StorySetting> settings, String rawJson) {
                requireActivity().runOnUiThread(() -> {
                    if (settings == null || settings.isEmpty()) {
                        // AI提取失败，使用降级方案
                        saveFallbackPresetMaterials();
                        return;
                    }

                    // 直接保存到全局素材库（不弹审核）
                    int successCount = 0;
                    for (StorySetting setting : settings) {
                        try {
                            setting.setStoryId(0);
                            long id = settingDao.insert(setting);
                            if (id > 0) {
                                successCount++;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    presetInProgress = false;
                    refreshSettingsList();
                    android.widget.Toast.makeText(getContext(),
                            "已预存 " + successCount + " 个素材",
                            android.widget.Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onFailure(Exception e) {
                requireActivity().runOnUiThread(() -> {
                    // 爬取失败，使用降级方案
                    saveFallbackPresetMaterials();
                });
            }
        });
    }

    /**
     * 降级方案：手动创建预设素材（涵盖世界、角色、地点、剧情等分类）
     */
    private void saveFallbackPresetMaterials() {
        com.google.gson.Gson gson = new com.google.gson.Gson();
        long now = System.currentTimeMillis();
        int successCount = 0;

        // ===== 1. 世界 - 时代背景 =====
        StorySetting worldSetting = new StorySetting(0, "世界", "时代背景", "黄昏分界 · 克苏鲁末日世界");
        worldSetting.setSummary("克苏鲁神话体系下的末日世界设定，黄昏之后世界变得陌生，人类在疯狂与理智的边缘挣扎");
        worldSetting.setDetail("【小说】黄昏分界\n【作者】黑山老鬼\n\n" +
                "【世界背景】\n" +
                "黄昏之后，世界变得陌生。克苏鲁神话体系下的末日世界，古老的存在从沉睡中苏醒，人类文明濒临崩溃。\n\n" +
                "【时代特征】\n" +
                "- 旧日支配者复苏，世界规则被扭曲\n" +
                "- 人类在疯狂与理智的边缘挣扎\n" +
                "- 特殊能力觉醒者出现，成为对抗黑暗的希望\n" +
                "- 社会秩序崩塌，弱肉强食的丛林法则盛行\n\n" +
                "【核心冲突】\n" +
                "人类与克苏鲁神话中恐怖存在的对抗，理智与疯狂的较量\n\n" +
                "【来源】" + PRESET_MATERIAL_URL);
        worldSetting.setTags(gson.toJson(java.util.Arrays.asList("克苏鲁", "末日", "世界设定", "黄昏分界", "黑暗")));
        worldSetting.setSourceUrl(PRESET_MATERIAL_URL);
        worldSetting.setSourceTitle(PRESET_MATERIAL_TITLE);
        worldSetting.setSourceType("preset");
        worldSetting.setAiConfidence(0.5);
        worldSetting.setCreateTime(now);
        worldSetting.setUpdateTime(now);

        try {
            long id = settingDao.insert(worldSetting);
            if (id > 0) successCount++;
        } catch (Exception e) {
            e.printStackTrace();
        }

        // ===== 2. 角色 - 主要角色 =====
        StorySetting characterSetting = new StorySetting(0, "角色", "主要角色", "黄昏分界 · 角色群像");
        characterSetting.setSummary("《黄昏分界》中在末日中觉醒特殊能力的主角团队，面对克苏鲁神话中的恐怖存在");
        characterSetting.setDetail("【小说】黄昏分界\n【作者】黑山老鬼\n\n" +
                "【主要人物】\n\n" +
                "1. 主角\n" +
                "   - 定位：末日觉醒者\n" +
                "   - 能力：在末日中觉醒特殊能力，拥有对抗克苏鲁存在的力量\n" +
                "   - 性格：坚韧不拔，在绝望中寻找希望\n" +
                "   - 成长：从普通人逐渐成长为对抗黑暗的中坚力量\n\n" +
                "2. 配角群\n" +
                "   - 各具特色的幸存者团队\n" +
                "   - 每个成员都有独特的背景故事和能力\n" +
                "   - 在末日中相互扶持，共同面对恐怖存在\n\n" +
                "3. 反派\n" +
                "   - 克苏鲁神话中的恐怖存在\n" +
                "   - 不可名状的古老支配者\n" +
                "   - 扭曲现实的黑暗力量\n\n" +
                "【来源】" + PRESET_MATERIAL_URL);
        characterSetting.setTags(gson.toJson(java.util.Arrays.asList("人物", "角色", "主角", "团队", "黄昏分界")));
        characterSetting.setSourceUrl(PRESET_MATERIAL_URL);
        characterSetting.setSourceTitle(PRESET_MATERIAL_TITLE);
        characterSetting.setSourceType("preset");
        characterSetting.setAiConfidence(0.5);
        characterSetting.setCreateTime(now);
        characterSetting.setUpdateTime(now);

        try {
            long id = settingDao.insert(characterSetting);
            if (id > 0) successCount++;
        } catch (Exception e) {
            e.printStackTrace();
        }

        // ===== 3. 地点 - 关键场景 =====
        StorySetting locationSetting = new StorySetting(0, "地点", "关键场景", "黄昏分界 · 末日场景");
        locationSetting.setSummary("克苏鲁末日世界中的关键场景，包括人类最后的避难所和黑暗笼罩的禁忌之地");
        locationSetting.setDetail("【小说】黄昏分界\n【作者】黑山老鬼\n\n" +
                "【关键场景】\n\n" +
                "1. 人类避难所\n" +
                "   - 类型：幸存者聚居地\n" +
                "   - 特征：残存的人类文明最后的堡垒\n" +
                "   - 氛围：压抑中带着希望，戒备森严\n\n" +
                "2. 黑暗禁区\n" +
                "   - 类型：被克苏鲁力量扭曲的区域\n" +
                "   - 特征：空间规则异常，充满不可名状的恐怖\n" +
                "   - 危险等级：极高，进入者可能永远迷失\n\n" +
                "3. 觉醒之地\n" +
                "   - 类型：特殊能量汇聚点\n" +
                "   - 特征：能力觉醒者获得力量的地方\n" +
                "   - 重要性：主角成长的关键地点\n\n" +
                "【来源】" + PRESET_MATERIAL_URL);
        locationSetting.setTags(gson.toJson(java.util.Arrays.asList("地点", "场景", "避难所", "禁区", "黄昏分界")));
        locationSetting.setSourceUrl(PRESET_MATERIAL_URL);
        locationSetting.setSourceTitle(PRESET_MATERIAL_TITLE);
        locationSetting.setSourceType("preset");
        locationSetting.setAiConfidence(0.5);
        locationSetting.setCreateTime(now);
        locationSetting.setUpdateTime(now);

        try {
            long id = settingDao.insert(locationSetting);
            if (id > 0) successCount++;
        } catch (Exception e) {
            e.printStackTrace();
        }

        // ===== 4. 剧情 - 主线剧情 =====
        StorySetting plotSetting = new StorySetting(0, "剧情", "主线剧情", "黄昏分界 · 剧情脉络");
        plotSetting.setSummary("人类在克苏鲁末日世界中挣扎求生的主线剧情，从文明崩塌到觉醒反抗的史诗旅程");
        plotSetting.setDetail("【小说】黄昏分界\n【作者】黑山老鬼\n\n" +
                "【主线剧情】\n\n" +
                "1. 开端：黄昏降临\n" +
                "   - 世界突然陷入异变，克苏鲁存在苏醒\n" +
                "   - 主角在灾难中觉醒特殊能力\n" +
                "   - 社会秩序迅速崩塌\n\n" +
                "2. 发展：末日求生\n" +
                "   - 主角寻找其他幸存者，组建团队\n" +
                "   - 探索被扭曲的世界，了解真相\n" +
                "   - 与各种恐怖存在对抗\n\n" +
                "3. 高潮：黑暗对决\n" +
                "   - 团队力量壮大，直面克苏鲁存在\n" +
                "   - 揭示末日背后的惊天秘密\n" +
                "   - 理智与疯狂的终极较量\n\n" +
                "4. 结局：希望或毁灭\n" +
                "   - 人类命运的关键抉择\n" +
                "   - 新世界的曙光或永恒的黑暗\n\n" +
                "【来源】" + PRESET_MATERIAL_URL);
        plotSetting.setTags(gson.toJson(java.util.Arrays.asList("剧情", "主线", "末日", "克苏鲁", "黄昏分界")));
        plotSetting.setSourceUrl(PRESET_MATERIAL_URL);
        plotSetting.setSourceTitle(PRESET_MATERIAL_TITLE);
        plotSetting.setSourceType("preset");
        plotSetting.setAiConfidence(0.5);
        plotSetting.setCreateTime(now);
        plotSetting.setUpdateTime(now);

        try {
            long id = settingDao.insert(plotSetting);
            if (id > 0) successCount++;
        } catch (Exception e) {
            e.printStackTrace();
        }

        // ===== 5. 规则体系 - 力量体系 =====
        StorySetting systemSetting = new StorySetting(0, "规则体系", "力量体系", "黄昏分界 · 力量体系");
        systemSetting.setSummary("克苏鲁末日世界中的特殊能力觉醒体系，以及对抗不可名状存在的规则");
        systemSetting.setDetail("【小说】黄昏分界\n【作者】黑山老鬼\n\n" +
                "【力量体系】\n\n" +
                "1. 觉醒者能力\n" +
                "   - 来源：末日降临后部分人类觉醒的特殊能力\n" +
                "   - 类型：多样化，因人而异\n" +
                "   - 代价：使用能力会加速理智的流失\n\n" +
                "2. 克苏鲁力量\n" +
                "   - 来源：古老的外神与旧日支配者\n" +
                "   - 特征：不可名状，超越人类理解\n" +
                "   - 影响：扭曲现实，污染精神\n\n" +
                "3. 理智值系统\n" +
                "   - 设定：接触克苏鲁存在会消耗理智\n" +
                "   - 后果：理智归零将彻底疯狂或异化\n" +
                "   - 恢复：有限的恢复手段\n\n" +
                "【来源】" + PRESET_MATERIAL_URL);
        systemSetting.setTags(gson.toJson(java.util.Arrays.asList("规则", "力量体系", "觉醒", "克苏鲁", "黄昏分界")));
        systemSetting.setSourceUrl(PRESET_MATERIAL_URL);
        systemSetting.setSourceTitle(PRESET_MATERIAL_TITLE);
        systemSetting.setSourceType("preset");
        systemSetting.setAiConfidence(0.5);
        systemSetting.setCreateTime(now);
        systemSetting.setUpdateTime(now);

        try {
            long id = settingDao.insert(systemSetting);
            if (id > 0) successCount++;
        } catch (Exception e) {
            e.printStackTrace();
        }

        // ===== 6. 创作控制 - 情感基调 =====
        StorySetting creativeSetting = new StorySetting(0, "创作控制", "情感基调", "黄昏分界 · 创作风格");
        creativeSetting.setSummary("克苏鲁+末日题材的黑暗压抑风格，在绝望中寻找希望的独特情感基调");
        creativeSetting.setDetail("【小说】黄昏分界\n【作者】黑山老鬼\n\n" +
                "【创作风格】\n\n" +
                "1. 情感基调\n" +
                "   - 主调：黑暗、压抑、绝望\n" +
                "   - 辅调：在绝境中闪烁的人性光辉\n" +
                "   - 氛围：克苏鲁式的不可名状恐怖\n\n" +
                "2. 叙事视角\n" +
                "   - 采用有限视角，保持神秘感\n" +
                "   - 通过主角的认知逐步揭示世界真相\n" +
                "   - 理智值变化影响叙事可靠性\n\n" +
                "3. 语言风格\n" +
                "   - 描写细腻，营造沉浸式恐怖氛围\n" +
                "   - 对话简洁有力，突出人物性格\n" +
                "   - 环境描写充满象征意义\n\n" +
                "【来源】" + PRESET_MATERIAL_URL);
        creativeSetting.setTags(gson.toJson(java.util.Arrays.asList("创作控制", "风格", "基调", "克苏鲁", "黄昏分界")));
        creativeSetting.setSourceUrl(PRESET_MATERIAL_URL);
        creativeSetting.setSourceTitle(PRESET_MATERIAL_TITLE);
        creativeSetting.setSourceType("preset");
        creativeSetting.setAiConfidence(0.5);
        creativeSetting.setCreateTime(now);
        creativeSetting.setUpdateTime(now);

        try {
            long id = settingDao.insert(creativeSetting);
            if (id > 0) successCount++;
        } catch (Exception e) {
            e.printStackTrace();
        }

        presetInProgress = false;
        refreshSettingsList();
        android.widget.Toast.makeText(getContext(),
                "已预存 " + successCount + " 个素材（来自《黄昏分界》）",
                android.widget.Toast.LENGTH_SHORT).show();
    }

    /**
     * 显示素材审核对话框
     */
    private void showReviewDialog(List<StorySetting> settings) {
        MaterialCandidateReviewDialogFragment dialog = MaterialCandidateReviewDialogFragment.newInstance();
        dialog.setData(settings);
        dialog.setListener(new MaterialCandidateReviewDialogFragment.Listener() {
            @Override
            public void onConfirm(@NonNull List<StorySetting> selectedSettings) {
                // 用户确认保存选中的素材
                int successCount = 0;
                for (StorySetting setting : selectedSettings) {
                    try {
                        long id = settingDao.insert(setting);
                        if (id > 0) {
                            successCount++;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                // 刷新列表
                refreshSettingsList();

                // 显示结果
                tvEmptyHint.setVisibility(View.GONE);
                android.widget.Toast.makeText(getContext(),
                        "成功导入 " + successCount + " 个素材",
                        android.widget.Toast.LENGTH_LONG).show();
            }

            @Override
            public void onCancel() {
                tvEmptyHint.setText("已取消导入");
                android.widget.Toast.makeText(getContext(), "已取消导入", android.widget.Toast.LENGTH_SHORT).show();
            }
        });
        dialog.show(getChildFragmentManager(), "material_review");
    }
}
