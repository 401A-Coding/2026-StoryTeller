package com.example.storyteller.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.storyteller.R;
import com.example.storyteller.data.local.db.StorySettingDao;
import com.example.storyteller.model.StorySetting;
import com.example.storyteller.utils.SettingCategoryConfig;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.gson.Gson;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;

/**
 * 设定详情页
 * 功能：查看、编辑、新建设定
 */
public class SettingDetailActivity extends AppCompatActivity {

    public static final String EXTRA_SETTING_ID = "setting_id";
    public static final String EXTRA_STORY_ID = "story_id";

    private TextView tvTitle;
    private TextView tvCategory;
    private TextView tvSummary;
    private TextView tvDetail;
    private TextView tvSpecificAttrs;
    private TextView tvSourceTitle;  // 溯源信息标题
    private TextView tvSourceUrl;    // 来源URL
    private TextView tvCreateTime;
    private TextView tvUpdateTime;
    
    private EditText etEditTitle;
    private EditText etEditSummary;
    private EditText etEditDetail;
    private ChipGroup chipGroupTags;      // 标签Chip组
    private ChipGroup chipGroupAliases;   // 别名Chip组
    private ChipGroup chipGroupViewTags;      // 查看模式标签
    private ChipGroup chipGroupViewAliases;   // 查看模式别名
    private Spinner spEditCategory;      // 主分类（新建/编辑时可改）
    private Spinner spEditSubCategory;   // 子分类（可编辑）
    
    private Button btnEdit;
    private Button btnSave;
    private Button btnBack;  // 返回/放弃更改按钮
    
    private StorySettingDao settingDao;
    private StorySetting currentSetting;
    private int storyId;
    
    private boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting_detail);

        // 获取参数
        Intent intent = getIntent();
        int settingId = intent.getIntExtra(EXTRA_SETTING_ID, -1);
        storyId = intent.getIntExtra(EXTRA_STORY_ID, 0);

        // 初始化
        settingDao = new StorySettingDao(this);
        initView();
        
        if (settingId == -1) {
            // 新建模式
            enterCreateMode();
        } else {
            // 查看/编辑模式
            loadSetting(settingId);
        }
    }

    private void initView() {
        // 查看模式视图
        tvTitle = findViewById(R.id.tv_detail_title);
        tvCategory = findViewById(R.id.tv_detail_category);
        tvSummary = findViewById(R.id.tv_detail_summary);
        tvDetail = findViewById(R.id.tv_detail_content);
        tvSpecificAttrs = findViewById(R.id.tv_detail_specific_attrs);
        tvSourceTitle = findViewById(R.id.tv_source_title);  // 溯源信息标题
        tvSourceUrl = findViewById(R.id.tv_source_url);      // 来源URL
        tvCreateTime = findViewById(R.id.tv_detail_create_time);
        tvUpdateTime = findViewById(R.id.tv_detail_update_time);
        
        // 编辑模式视图
        etEditTitle = findViewById(R.id.et_edit_title);
        etEditSummary = findViewById(R.id.et_edit_summary);
        etEditDetail = findViewById(R.id.et_edit_detail);
        chipGroupTags = findViewById(R.id.chip_group_tags);
        chipGroupAliases = findViewById(R.id.chip_group_aliases);
        chipGroupViewTags = findViewById(R.id.chip_group_view_tags);
        chipGroupViewAliases = findViewById(R.id.chip_group_view_aliases);
        spEditCategory = findViewById(R.id.sp_edit_category);      // 主分类
        spEditSubCategory = findViewById(R.id.sp_edit_sub_category);  // 子分类
        
        // 按钮
        btnBack = findViewById(R.id.btn_back);
        btnEdit = findViewById(R.id.btn_edit);
        btnSave = findViewById(R.id.btn_save);
        
        // 返回/放弃更改按钮
        btnBack.setOnClickListener(v -> handleBackAction());
        
        // 编辑按钮
        btnEdit.setOnClickListener(v -> enterEditMode());
        
        // 保存按钮
        btnSave.setOnClickListener(v -> saveChanges());
    }

    /**
     * 处理返回/放弃更改按钮点击
     */
    private void handleBackAction() {
        if (isEditMode) {
            // 编辑模式：弹出确认对话框
            showDiscardChangesDialog();
        } else {
            // 查看模式：直接返回
            finish();
        }
    }
    
    /**
     * 显示放弃更改确认对话框
     */
    private void showDiscardChangesDialog() {
        new AlertDialog.Builder(this)
            .setTitle("放弃更改")
            .setMessage("确定要放弃当前的修改吗？")
            .setPositiveButton("放弃", (dialog, which) -> {
                if (currentSetting.getId() == 0) {
                    // 新建模式：直接关闭页面
                    finish();
                } else {
                    // 编辑模式：返回查看模式
                    exitEditMode();
                    displaySetting();
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    /**
     * 进入新建模式
     */
    private void enterCreateMode() {
        isEditMode = true;
        
        // 创建空对象
        currentSetting = new StorySetting();
        currentSetting.setStoryId(storyId);
        currentSetting.setCreateTime(System.currentTimeMillis());
        currentSetting.setUpdateTime(System.currentTimeMillis());
        
        // 清空表单
        etEditTitle.setText("");
        etEditSummary.setText("");
        etEditDetail.setText("");
        
        // 设置主分类Spinner
        setupCategorySpinner();
        spEditCategory.setSelection(0);  // 默认第一个分类
        
        // 设置子分类Spinner
        setupSubCategorySpinner();
        
        // 初始化标签和别名ChipGroup（添加"+ 添加"按钮）
        fillTagsAndAliases();
        
        // 切换到编辑模式UI
        setViewMode(true);
    }

    /**
     * 加载设定数据
     */
    private void loadSetting(int settingId) {
        currentSetting = settingDao.getById(settingId);
        
        if (currentSetting == null) {
            Toast.makeText(this, "设定不存在", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        displaySetting();
    }

    /**
     * 显示设定（查看模式）
     */
    private void displaySetting() {
        // 标题
        tvTitle.setText(currentSetting.getTitle());
        
        // 分类
        String categoryText = currentSetting.getCategory() + " · " + currentSetting.getSubCategory();
        tvCategory.setText(categoryText);
        
        // 摘要
        tvSummary.setText(TextUtils.isEmpty(currentSetting.getSummary()) ? "暂无摘要" : currentSetting.getSummary());
        
        // 详情
        tvDetail.setText(TextUtils.isEmpty(currentSetting.getDetail()) ? "暂无详细描述" : currentSetting.getDetail());
        
        // 专属属性（JSON格式化）
        if (!TextUtils.isEmpty(currentSetting.getSpecificAttributes())) {
            try {
                String json = currentSetting.getSpecificAttributes();
                // 格式化JSON
                String formatted = JsonParser.parseString(json).toString();
                tvSpecificAttrs.setText(formatted);
                tvSpecificAttrs.setVisibility(View.VISIBLE);
                findViewById(R.id.layout_specific_attrs).setVisibility(View.VISIBLE);
            } catch (Exception e) {
                tvSpecificAttrs.setText(currentSetting.getSpecificAttributes());
                tvSpecificAttrs.setVisibility(View.VISIBLE);
            }
        } else {
            tvSpecificAttrs.setVisibility(View.GONE);
            findViewById(R.id.layout_specific_attrs).setVisibility(View.GONE);
        }
        
        // 溯源信息
        View layoutSourceInfo = findViewById(R.id.layout_source_info);
        com.google.android.material.card.MaterialCardView cardSourceInfo = 
            findViewById(R.id.card_source_info);
        
        if (currentSetting.getSourceMaterialId() > 0) {
            // 查询全局素材的标题
            StorySetting sourceSetting = settingDao.getById(currentSetting.getSourceMaterialId());
            if (sourceSetting != null) {
                tvSourceTitle.setText(sourceSetting.getTitle());
                tvSourceTitle.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
                layoutSourceInfo.setVisibility(View.VISIBLE);
                
                // 设置点击跳转到全局素材详情
                View.OnClickListener clickListener = v -> {
                    Intent intent = new Intent(SettingDetailActivity.this, SettingDetailActivity.class);
                    intent.putExtra(SettingDetailActivity.EXTRA_SETTING_ID, sourceSetting.getId());
                    intent.putExtra(SettingDetailActivity.EXTRA_STORY_ID, 0);  // 全局素材库
                    startActivity(intent);
                };
                
                // 同时设置给 Layout 和 CardView
                layoutSourceInfo.setOnClickListener(clickListener);
                cardSourceInfo.setOnClickListener(clickListener);
            } else {
                // 原素材已被删除，显示提示
                tvSourceTitle.setText("原素材已被删除");
                tvSourceTitle.setTextColor(getResources().getColor(android.R.color.darker_gray));
                layoutSourceInfo.setVisibility(View.VISIBLE);
                layoutSourceInfo.setClickable(false);
                cardSourceInfo.setClickable(false);
            }
        } else {
            layoutSourceInfo.setVisibility(View.GONE);
        }
        
        // 来源URL（仅全局素材且有URL时显示）
        View layoutSourceUrl = findViewById(R.id.layout_source_url);
        com.google.android.material.card.MaterialCardView cardSourceUrl = 
            findViewById(R.id.card_source_url);
        
        if (storyId == 0 && !TextUtils.isEmpty(currentSetting.getSourceUrl())) {
            // 全局素材且有来源URL
            tvSourceUrl.setText(currentSetting.getSourceUrl());
            layoutSourceUrl.setVisibility(View.VISIBLE);
            
            // 设置点击打开链接
            View.OnClickListener urlClickListener = v -> {
                try {
                    android.content.Intent browserIntent = new android.content.Intent(
                        android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse(currentSetting.getSourceUrl())
                    );
                    startActivity(browserIntent);
                } catch (Exception e) {
                    Toast.makeText(this, "无法打开链接", Toast.LENGTH_SHORT).show();
                }
            };
            
            layoutSourceUrl.setOnClickListener(urlClickListener);
            cardSourceUrl.setOnClickListener(urlClickListener);
        } else {
            // 小说专属素材或无URL，隐藏
            layoutSourceUrl.setVisibility(View.GONE);
        }
        
        // 时间
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
        tvCreateTime.setText("创建时间: " + sdf.format(new java.util.Date(currentSetting.getCreateTime())));
        tvUpdateTime.setText("更新时间: " + sdf.format(new java.util.Date(currentSetting.getUpdateTime())));
        
        // 填充查看模式的标签和别名
        fillViewModeTagsAndAliases();
        
        // 切换到查看模式
        setViewMode(false);
    }

    /**
     * 进入编辑模式
     */
    private void enterEditMode() {
        isEditMode = true;
        
        // 填充编辑表单
        etEditTitle.setText(currentSetting.getTitle());
        etEditSummary.setText(currentSetting.getSummary());
        etEditDetail.setText(currentSetting.getDetail());
        
        // 设置主分类Spinner（编辑模式下禁用）
        setupCategorySpinner();
        int categoryPosition = getPositionInArray(SettingCategoryConfig.getAllMainCategories(), currentSetting.getCategory());
        if (categoryPosition >= 0) {
            spEditCategory.setSelection(categoryPosition);
        }
        // 编辑模式下禁用主分类修改
        spEditCategory.setEnabled(false);
        
        // 设置子分类Spinner
        setupSubCategorySpinner();
        
        // 填充标签和别名（如果有的话）
        fillTagsAndAliases();
        
        setViewMode(true);
    }
    
    /**
     * 设置主分类Spinner
     */
    private void setupCategorySpinner() {
        String[] categories = SettingCategoryConfig.getAllMainCategories();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            this,
            android.R.layout.simple_spinner_item,
            categories
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spEditCategory.setAdapter(adapter);
        
        // 主分类变化时更新子分类
        spEditCategory.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                setupSubCategorySpinner();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }
    
    /**
     * 填充标签和别名（编辑模式）
     */
    private void fillTagsAndAliases() {
        // 清空现有Chips
        chipGroupTags.removeAllViews();
        chipGroupAliases.removeAllViews();
        
        // 填充标签
        if (currentSetting.getTags() != null && !currentSetting.getTags().isEmpty()) {
            try {
                List<String> tagsList = new Gson().fromJson(
                    currentSetting.getTags(), 
                    new com.google.gson.reflect.TypeToken<List<String>>(){}.getType()
                );
                if (tagsList != null) {
                    for (String tag : tagsList) {
                        addTagChip(tag);
                    }
                }
            } catch (Exception e) {
                // 如果不是JSON格式，直接添加
                addTagChip(currentSetting.getTags());
            }
        }
        
        // 填充别名
        if (currentSetting.getAliases() != null && !currentSetting.getAliases().isEmpty()) {
            try {
                List<String> aliasesList = new Gson().fromJson(
                    currentSetting.getAliases(), 
                    new com.google.gson.reflect.TypeToken<List<String>>(){}.getType()
                );
                if (aliasesList != null) {
                    for (String alias : aliasesList) {
                        addAliasChip(alias);
                    }
                }
            } catch (Exception e) {
                // 如果不是JSON格式，直接添加
                addAliasChip(currentSetting.getAliases());
            }
        }
        
        // 设置双击空白处添加Chip
        setupChipGroupDoubleClick(chipGroupTags, true);
        setupChipGroupDoubleClick(chipGroupAliases, false);
    }
    
    /**
     * 填充查看模式的标签和别名
     */
    private void fillViewModeTagsAndAliases() {
        // 清空现有Chips
        chipGroupViewTags.removeAllViews();
        chipGroupViewAliases.removeAllViews();
        
        // 填充标签
        if (currentSetting.getTags() != null && !currentSetting.getTags().isEmpty()) {
            try {
                List<String> tagsList = new Gson().fromJson(
                    currentSetting.getTags(), 
                    new com.google.gson.reflect.TypeToken<List<String>>(){}.getType()
                );
                if (tagsList != null && !tagsList.isEmpty()) {
                    for (String tag : tagsList) {
                        addViewTagChip(tag);
                    }
                    findViewById(R.id.card_tags).setVisibility(View.VISIBLE);
                } else {
                    findViewById(R.id.card_tags).setVisibility(View.GONE);
                }
            } catch (Exception e) {
                addViewTagChip(currentSetting.getTags());
                findViewById(R.id.card_tags).setVisibility(View.VISIBLE);
            }
        } else {
            findViewById(R.id.card_tags).setVisibility(View.GONE);
        }
        
        // 填充别名
        if (currentSetting.getAliases() != null && !currentSetting.getAliases().isEmpty()) {
            try {
                List<String> aliasesList = new Gson().fromJson(
                    currentSetting.getAliases(), 
                    new com.google.gson.reflect.TypeToken<List<String>>(){}.getType()
                );
                if (aliasesList != null && !aliasesList.isEmpty()) {
                    for (String alias : aliasesList) {
                        addViewAliasChip(alias);
                    }
                    findViewById(R.id.card_aliases).setVisibility(View.VISIBLE);
                } else {
                    findViewById(R.id.card_aliases).setVisibility(View.GONE);
                }
            } catch (Exception e) {
                addViewAliasChip(currentSetting.getAliases());
                findViewById(R.id.card_aliases).setVisibility(View.VISIBLE);
            }
        } else {
            findViewById(R.id.card_aliases).setVisibility(View.GONE);
        }
    }
    
    /**
     * 添加标签Chip（编辑模式）
     */
    private void addTagChip(String text) {
        Chip chip = new Chip(this);
        chip.setText(text);
        chip.setCloseIconVisible(true);
        chip.setClickable(true);
        chip.setCheckable(false);
        
        // 设置关闭按钮点击事件
        chip.setOnCloseIconClickListener(v -> {
            chipGroupTags.removeView(chip);
        });
        
        chipGroupTags.addView(chip);
    }
    
    /**
     * 添加别名Chip（编辑模式）
     */
    private void addAliasChip(String text) {
        Chip chip = new Chip(this);
        chip.setText(text);
        chip.setCloseIconVisible(true);
        chip.setClickable(true);
        chip.setCheckable(false);
        
        // 设置关闭按钮点击事件
        chip.setOnCloseIconClickListener(v -> {
            chipGroupAliases.removeView(chip);
        });
        
        chipGroupAliases.addView(chip);
    }
    
    /**
     * 添加标签Chip（查看模式）
     */
    private void addViewTagChip(String text) {
        Chip chip = new Chip(this);
        chip.setText(text);
        chip.setCloseIconVisible(false);  // 查看模式不显示关闭按钮
        chip.setClickable(false);
        chip.setCheckable(false);
        
        chipGroupViewTags.addView(chip);
    }
    
    /**
     * 添加别名Chip（查看模式）
     */
    private void addViewAliasChip(String text) {
        Chip chip = new Chip(this);
        chip.setText(text);
        chip.setCloseIconVisible(false);  // 查看模式不显示关闭按钮
        chip.setClickable(false);
        chip.setCheckable(false);
        
        chipGroupViewAliases.addView(chip);
    }
    
    /**
     * 设置ChipGroup双击空白处添加Chip
     */
    private void setupChipGroupDoubleClick(ChipGroup chipGroup, boolean isTag) {
        // 添加"+ 添加"Chip
        addAddButtonChip(chipGroup, isTag);
    }
    
    /**
     * 添加"+ 添加"按钮Chip
     */
    private void addAddButtonChip(ChipGroup chipGroup, boolean isTag) {
        Chip addChip = new Chip(this);
        addChip.setText("+ 添加" + (isTag ? "标签" : "别名"));
        addChip.setCloseIconVisible(false);
        addChip.setClickable(true);
        addChip.setCheckable(false);
        
        // 设置样式为Outlined（边框样式）
        addChip.setChipBackgroundColorResource(android.R.color.transparent);
        
        // 点击事件：弹出输入对话框
        addChip.setOnClickListener(v -> {
            showAddChipDialog(chipGroup, isTag);
        });
        
        chipGroup.addView(addChip);
    }
    
    /**
     * 显示添加Chip对话框
     */
    private void showAddChipDialog(ChipGroup chipGroup, boolean isTag) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(isTag ? "添加标签" : "添加别名");
        
        final EditText input = new EditText(this);
        input.setHint("请输入" + (isTag ? "标签" : "别名"));
        builder.setView(input);
        
        builder.setPositiveButton("添加", (dialog, which) -> {
            String text = input.getText().toString().trim();
            if (!TextUtils.isEmpty(text)) {
                // 移除"+ 添加"按钮
                removeAddButtonChip(chipGroup);
                
                // 添加新Chip
                if (isTag) {
                    addTagChip(text);
                } else {
                    addAliasChip(text);
                }
                
                // 重新添加"+ 添加"按钮
                addAddButtonChip(chipGroup, isTag);
            }
        });
        builder.setNegativeButton("取消", null);
        
        builder.show();
    }
    
    /**
     * 移除"+ 添加"按钮Chip
     */
    private void removeAddButtonChip(ChipGroup chipGroup) {
        int childCount = chipGroup.getChildCount();
        if (childCount > 0) {
            View lastChild = chipGroup.getChildAt(childCount - 1);
            if (lastChild instanceof Chip) {
                String text = ((Chip) lastChild).getText().toString();
                if (text.startsWith("+ 添加")) {
                    chipGroup.removeView(lastChild);
                }
            }
        }
    }

    /**
     * 设置子分类Spinner
     */
    private void setupSubCategorySpinner() {
        // 从主分类Spinner获取当前选中的分类
        String selectedCategory = spEditCategory.getSelectedItem() != null ? 
            spEditCategory.getSelectedItem().toString() : "世界观";
        
        // 根据主分类加载对应的子分类列表
        String[] subCategories = getSubCategoriesByCategory(selectedCategory);
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            this,
            android.R.layout.simple_spinner_item,
            subCategories
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spEditSubCategory.setAdapter(adapter);
        
        // 设置当前选中的子分类（仅当有数据时）
        if (currentSetting.getSubCategory() != null && !currentSetting.getSubCategory().isEmpty()) {
            int position = getPositionInArray(subCategories, currentSetting.getSubCategory());
            if (position >= 0) {
                spEditSubCategory.setSelection(position);
            }
        }
    }
    
    /**
     * 根据主分类获取子分类列表
     */
    private String[] getSubCategoriesByCategory(String category) {
        return SettingCategoryConfig.getSubCategories(category);
    }
    
    /**
     * 在数组中查找元素的位置
     */
    private int getPositionInArray(String[] array, String target) {
        if (array == null || target == null) return -1;
        for (int i = 0; i < array.length; i++) {
            if (array[i].equals(target)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 退出编辑模式，返回查看模式
     */
    private void exitEditMode() {
        isEditMode = false;
        // 恢复主分类Spinner的可用状态
        spEditCategory.setEnabled(true);
        setViewMode(false);
    }

    /**
     * 保存修改
     */
    private void saveChanges() {
        String title = etEditTitle.getText() == null ? "" : etEditTitle.getText().toString().trim();
        String summary = etEditSummary.getText() == null ? "" : etEditSummary.getText().toString().trim();
        String detail = etEditDetail.getText() == null ? "" : etEditDetail.getText().toString().trim();
        
        // 验证
        if (TextUtils.isEmpty(title)) {
            etEditTitle.setError("请输入标题");
            return;
        }
        
        if (TextUtils.isEmpty(summary)) {
            etEditSummary.setError("请输入摘要");
            return;
        }
        
        // 从Spinner获取分类
        String newCategory = spEditCategory.getSelectedItem() != null ? 
            spEditCategory.getSelectedItem().toString() : currentSetting.getCategory();
        String newSubCategory = spEditSubCategory.getSelectedItem() != null ? 
            spEditSubCategory.getSelectedItem().toString() : currentSetting.getSubCategory();
        
        // 从ChipGroup获取标签
        List<String> tagsList = getChipsFromGroup(chipGroupTags);
        if (!tagsList.isEmpty()) {
            currentSetting.setTags(new Gson().toJson(tagsList));
        } else {
            currentSetting.setTags(null);
        }
        
        // 从ChipGroup获取别名
        List<String> aliasesList = getChipsFromGroup(chipGroupAliases);
        if (!aliasesList.isEmpty()) {
            currentSetting.setAliases(new Gson().toJson(aliasesList));
        } else {
            currentSetting.setAliases(null);
        }
        
        // 更新数据
        currentSetting.setTitle(title);
        currentSetting.setSummary(summary);
        currentSetting.setDetail(detail);
        currentSetting.setCategory(newCategory);      // 更新主分类
        currentSetting.setSubCategory(newSubCategory);  // 更新子分类
        currentSetting.setUpdateTime(System.currentTimeMillis());
        
        int result;
        if (currentSetting.getId() == 0) {
            // 新建模式：插入数据库
            currentSetting.setCreateTime(System.currentTimeMillis());
            long id = settingDao.insert(currentSetting);
            result = id > 0 ? 1 : 0;
            if (result > 0) {
                currentSetting.setId((int) id);
            }
        } else {
            // 编辑模式：更新数据库
            result = settingDao.update(currentSetting);
        }
        
        if (result > 0) {
            Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show();
            
            if (currentSetting.getId() == 0) {
                // 新建模式：保存后关闭页面，返回列表
                finish();
            } else {
                // 编辑模式：保存后返回查看模式
                exitEditMode();
                displaySetting();  // 刷新显示
            }
        } else {
            Toast.makeText(this, "保存失败", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 从ChipGroup获取所有Chip的文本（排除"+ 添加"按钮）
     */
    private List<String> getChipsFromGroup(ChipGroup chipGroup) {
        List<String> texts = new ArrayList<>();
        for (int i = 0; i < chipGroup.getChildCount(); i++) {
            View child = chipGroup.getChildAt(i);
            if (child instanceof Chip) {
                String text = ((Chip) child).getText().toString().trim();
                // 过滤掉"+ 添加"按钮
                if (!TextUtils.isEmpty(text) && !text.startsWith("+ 添加")) {
                    texts.add(text);
                }
            }
        }
        return texts;
    }

    /**
     * 切换视图模式
     */
    private void setViewMode(boolean editMode) {
        if (editMode) {
            // 编辑模式
            findViewById(R.id.layout_view_mode).setVisibility(View.GONE);
            findViewById(R.id.layout_edit_mode).setVisibility(View.VISIBLE);
            btnEdit.setVisibility(View.GONE);
            btnSave.setVisibility(View.VISIBLE);
            
            // 修改返回按钮文本为"放弃更改"
            btnBack.setText("放弃更改");
        } else {
            // 查看模式
            findViewById(R.id.layout_view_mode).setVisibility(View.VISIBLE);
            findViewById(R.id.layout_edit_mode).setVisibility(View.GONE);
            btnEdit.setVisibility(View.VISIBLE);
            btnSave.setVisibility(View.GONE);
            
            // 恢复返回按钮文本
            btnBack.setText("← 返回");
        }
    }
}
