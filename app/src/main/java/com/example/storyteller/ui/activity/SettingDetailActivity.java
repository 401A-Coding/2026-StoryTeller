package com.example.storyteller.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.storyteller.R;
import com.example.storyteller.data.local.db.StorySettingDao;
import com.example.storyteller.model.StorySetting;
import com.example.storyteller.utils.SettingCategoryConfig;
import com.example.storyteller.utils.SpecificAttributesParser;
import com.example.storyteller.utils.SpecificAttributesParser.AttributeField;
import com.example.storyteller.utils.SpecificAttributesParser.FieldType;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.gson.Gson;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    
    // 专属属性解析器
    private SpecificAttributesParser attrParser;
    // 存储编辑时的专属属性数据（TEXT、TEXT_MULTI类型）
    private Map<String, View> editSpecificAttrFields;
    private Map<String, ChipGroup> editSpecificAttrChipGroups;
    private Map<String, SeekBar> editSpecificAttrSliders;
    // 存储Spinner的选项值数组
    private Map<String, String[]> editSpecificAttrSpinnerOptions;

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
        attrParser = new SpecificAttributesParser();
        editSpecificAttrFields = new HashMap<>();
        editSpecificAttrChipGroups = new HashMap<>();
        editSpecificAttrSliders = new HashMap<>();
        editSpecificAttrSpinnerOptions = new HashMap<>();
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
        
        // 专属属性（结构化展示）
        displaySpecificAttributes();
        
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
        // 编辑模式下禁用子分类修改
        spEditSubCategory.setEnabled(false);
        
        // 填充标签和别名（如果有的话）
        fillTagsAndAliases();
        
        // 填充专属属性编辑控件
        fillSpecificAttrEditFields();
        
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
            spEditCategory.getSelectedItem().toString() : "世界";
        
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
        // 恢复子分类Spinner的可用状态
        spEditSubCategory.setEnabled(true);
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
        
        // 获取专属属性
        String specificAttrsJson = collectSpecificAttributes();
        currentSetting.setSpecificAttributes(specificAttrsJson);
        
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
    
    /**
     * 结构化显示专属属性
     */
    private void displaySpecificAttributes() {
        View layoutSpecificAttrs = findViewById(R.id.layout_specific_attrs);
        View layoutRawJson = findViewById(R.id.layout_raw_json);
        LinearLayout layoutContent = findViewById(R.id.layout_specific_attrs_content);
        TextView tvTitle = findViewById(R.id.tv_specific_attrs_title);
        
        String subCategory = currentSetting.getSubCategory();
        
        if (TextUtils.isEmpty(currentSetting.getSpecificAttributes())) {
            // 没有专属属性数据
            layoutSpecificAttrs.setVisibility(View.GONE);
            layoutRawJson.setVisibility(View.GONE);
            return;
        }
        
        // 检查是否有结构化定义
        if (attrParser.hasSpecificFields(subCategory)) {
            // 有结构化定义，尝试解析并显示
            layoutContent.removeAllViews();
            
            Object attrObj = attrParser.parseSpecificAttributes(currentSetting);
            Map<String, Object> attrMap;
            
            if (attrObj != null) {
                attrMap = attrParser.objectToMap(attrObj);
            } else {
                // 解析失败，回退到原始JSON显示
                try {
                    com.google.gson.JsonObject jsonObj = JsonParser.parseString(
                        currentSetting.getSpecificAttributes()).getAsJsonObject();
                    attrMap = new HashMap<>();
                    for (Map.Entry<String, com.google.gson.JsonElement> entry : jsonObj.entrySet()) {
                        attrMap.put(entry.getKey(), jsonToDisplayValue(entry.getValue()));
                    }
                } catch (Exception e) {
                    attrMap = null;
                }
            }
            
            if (attrMap != null && !attrMap.isEmpty()) {
                // 显示标题
                String displayName = attrParser.getDisplayName(subCategory);
                tvTitle.setText(displayName + "专属属性");
                
                // 添加属性项
                List<AttributeField> fields = attrParser.getFieldsForSubCategory(subCategory);
                for (AttributeField field : fields) {
                    Object value = attrMap.get(field.getKey());
                    if (value != null && !value.toString().isEmpty() && !value.toString().equals("null")) {
                        addSpecificAttrView(layoutContent, field.getLabel(), formatAttrValue(field, value));
                    }
                }
                
                // 检查是否有未定义的字段
                for (Map.Entry<String, Object> entry : attrMap.entrySet()) {
                    boolean found = false;
                    for (AttributeField field : fields) {
                        if (field.getKey().equals(entry.getKey())) {
                            found = true;
                            break;
                        }
                    }
                    if (!found && entry.getValue() != null && !entry.getValue().toString().isEmpty()) {
                        addSpecificAttrView(layoutContent, entry.getKey(), formatValue(entry.getValue()));
                    }
                }
                
                layoutSpecificAttrs.setVisibility(View.VISIBLE);
                layoutRawJson.setVisibility(View.GONE);
            } else {
                // 无法解析，显示原始JSON
                showRawJson(layoutRawJson, layoutSpecificAttrs);
            }
        } else {
            // 没有结构化定义，显示原始JSON
            showRawJson(layoutRawJson, layoutSpecificAttrs);
        }
    }
    
    /**
     * 显示原始JSON
     */
    private void showRawJson(View layoutRawJson, View layoutSpecificAttrs) {
        layoutSpecificAttrs.setVisibility(View.GONE);
        TextView tvRawJson = findViewById(R.id.tv_detail_specific_attrs);
        try {
            String formatted = JsonParser.parseString(currentSetting.getSpecificAttributes()).toString();
            tvRawJson.setText(formatted);
        } catch (Exception e) {
            tvRawJson.setText(currentSetting.getSpecificAttributes());
        }
        layoutRawJson.setVisibility(View.VISIBLE);
    }
    
    /**
     * JSON值转可显示值
     */
    private Object jsonToDisplayValue(com.google.gson.JsonElement element) {
        if (element.isJsonNull()) return null;
        if (element.isJsonPrimitive()) return element.getAsString();
        if (element.isJsonArray()) {
            List<String> list = new ArrayList<>();
            for (com.google.gson.JsonElement e : element.getAsJsonArray()) {
                list.add(e.getAsString());
            }
            return list;
        }
        return element.toString();
    }
    
    /**
     * 格式化属性值
     */
    private String formatAttrValue(AttributeField field, Object value) {
        if (value == null) return "";
        
        switch (field.getType()) {
            case SELECT:
                // 转换选项值
                if (field.getOptions() != null) {
                    for (String option : field.getOptions()) {
                        if (option.startsWith(value.toString() + ":")) {
                            return option.substring(option.indexOf(":") + 1);
                        }
                    }
                }
                return value.toString();
                
            case SLIDER:
                return value.toString() + "/10";
                
            case TAG_LIST:
                if (value instanceof List) {
                    List<String> strList = new ArrayList<>();
                    for (Object item : (List<?>) value) {
                        if (item != null) strList.add(item.toString());
                    }
                    return String.join("、", strList);
                }
                return value.toString();
                
            default:
                return formatValue(value);
        }
    }
    
    /**
     * 格式化通用值
     */
    private String formatValue(Object value) {
        if (value == null) return "";
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            if (list.isEmpty()) return "";
            StringBuilder sb = new StringBuilder();
            for (Object item : list) {
                if (sb.length() > 0) sb.append("、");
                sb.append(item.toString());
            }
            return sb.toString();
        }
        return value.toString();
    }
    
    /**
     * 添加专属属性显示项
     */
    private void addSpecificAttrView(LinearLayout container, String label, String value) {
        if (TextUtils.isEmpty(value) || value.equals("null")) return;
        
        LinearLayout itemLayout = new LinearLayout(this);
        itemLayout.setOrientation(LinearLayout.HORIZONTAL);
        itemLayout.setPadding(0, 8, 0, 8);
        
        TextView labelView = new TextView(this);
        labelView.setText(label + "：");
        labelView.setTextSize(14);
        labelView.setTextColor(getResources().getColor(android.R.color.darker_gray));
        labelView.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT));
        
        TextView valueView = new TextView(this);
        valueView.setText(value);
        valueView.setTextSize(14);
        valueView.setTextColor(getResources().getColor(android.R.color.black));
        valueView.setLayoutParams(new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        
        itemLayout.addView(labelView);
        itemLayout.addView(valueView);
        container.addView(itemLayout);
    }
    
    /**
     * 填充专属属性编辑控件
     */
    private void fillSpecificAttrEditFields() {
        TextView tvTitle = findViewById(R.id.tv_edit_specific_attrs_title);
        LinearLayout layoutEditAttrs = findViewById(R.id.layout_edit_specific_attrs);
        
        // 清空现有控件
        layoutEditAttrs.removeAllViews();
        editSpecificAttrFields.clear();
        editSpecificAttrChipGroups.clear();
        editSpecificAttrSliders.clear();
        
        String subCategory = currentSetting.getSubCategory();
        
        // 检查是否有结构化定义
        if (!attrParser.hasSpecificFields(subCategory)) {
            tvTitle.setVisibility(View.GONE);
            layoutEditAttrs.setVisibility(View.GONE);
            return;
        }
        
        // 显示标题
        String displayName = attrParser.getDisplayName(subCategory);
        tvTitle.setText(displayName + "专属属性");
        tvTitle.setVisibility(View.VISIBLE);
        layoutEditAttrs.setVisibility(View.VISIBLE);
        
        // 获取字段定义
        List<AttributeField> fields = attrParser.getFieldsForSubCategory(subCategory);
        
        // 解析现有数据
        Map<String, Object> existingData = null;
        if (!TextUtils.isEmpty(currentSetting.getSpecificAttributes())) {
            Object attrObj = attrParser.parseSpecificAttributes(currentSetting);
            if (attrObj != null) {
                existingData = attrParser.objectToMap(attrObj);
            } else {
                // 尝试直接解析JSON
                try {
                    com.google.gson.JsonObject jsonObj = JsonParser.parseString(
                        currentSetting.getSpecificAttributes()).getAsJsonObject();
                    existingData = new HashMap<>();
                    for (Map.Entry<String, com.google.gson.JsonElement> entry : jsonObj.entrySet()) {
                        existingData.put(entry.getKey(), jsonToDisplayValue(entry.getValue()));
                    }
                } catch (Exception e) {
                    existingData = new HashMap<>();
                }
            }
        }
        if (existingData == null) {
            existingData = new HashMap<>();
        }
        
        // 为每个字段生成编辑控件
        for (AttributeField field : fields) {
            Object value = existingData.get(field.getKey());
            createEditField(layoutEditAttrs, field, value);
        }
    }
    
    /**
     * 创建编辑控件
     */
    private void createEditField(LinearLayout container, AttributeField field, Object value) {
        switch (field.getType()) {
            case TEXT:
                createTextField(container, field, value);
                break;
            case TEXT_MULTI:
                createMultiTextField(container, field, value);
                break;
            case NUMBER:
                createNumberField(container, field, value);
                break;
            case SLIDER:
                createSliderField(container, field, value);
                break;
            case SELECT:
                createSelectField(container, field, value);
                break;
            case TAG_LIST:
                createTagListField(container, field, value);
                break;
        }
    }
    
    /**
     * 创建单行文本输入
     */
    private void createTextField(LinearLayout container, AttributeField field, Object value) {
        addFieldLabel(container, field.getLabel());
        
        EditText editText = new EditText(this);
        editText.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT));
        editText.setHint(field.getLabel());
        editText.setTextSize(14);
        editText.setPadding(16, 12, 16, 12);
        editText.setBackgroundResource(R.drawable.bg_outline_edittext);
        if (value != null) {
            editText.setText(value.toString());
        }
        editText.setTag(field.getKey());
        
        LinearLayout.MarginLayoutParams params = (LinearLayout.MarginLayoutParams) editText.getLayoutParams();
        params.bottomMargin = 16;
        editText.setLayoutParams(params);
        
        container.addView(editText);
        editSpecificAttrFields.put(field.getKey(), editText);
    }
    
    /**
     * 创建多行文本输入
     */
    private void createMultiTextField(LinearLayout container, AttributeField field, Object value) {
        addFieldLabel(container, field.getLabel());
        
        EditText editText = new EditText(this);
        editText.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT));
        editText.setHint(field.getLabel());
        editText.setTextSize(14);
        editText.setPadding(16, 12, 16, 12);
        editText.setBackgroundResource(R.drawable.bg_outline_edittext);
        editText.setMinLines(3);
        editText.setGravity(Gravity.TOP | Gravity.START);
        editText.setInputType(android.text.InputType.TYPE_CLASS_TEXT | 
            android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        if (value != null) {
            editText.setText(value.toString());
        }
        editText.setTag(field.getKey());
        
        LinearLayout.MarginLayoutParams params = (LinearLayout.MarginLayoutParams) editText.getLayoutParams();
        params.bottomMargin = 16;
        editText.setLayoutParams(params);
        
        container.addView(editText);
        editSpecificAttrFields.put(field.getKey(), editText);
    }
    
    /**
     * 创建数字输入
     */
    private void createNumberField(LinearLayout container, AttributeField field, Object value) {
        addFieldLabel(container, field.getLabel());
        
        EditText editText = new EditText(this);
        editText.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT));
        editText.setHint(field.getLabel());
        editText.setTextSize(14);
        editText.setPadding(16, 12, 16, 12);
        editText.setBackgroundResource(R.drawable.bg_outline_edittext);
        editText.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        if (value != null) {
            editText.setText(value.toString());
        }
        editText.setTag(field.getKey());
        
        LinearLayout.MarginLayoutParams params = (LinearLayout.MarginLayoutParams) editText.getLayoutParams();
        params.bottomMargin = 16;
        editText.setLayoutParams(params);
        
        container.addView(editText);
        editSpecificAttrFields.put(field.getKey(), editText);
    }
    
    /**
     * 创建滑块输入
     */
    private void createSliderField(LinearLayout container, AttributeField field, Object value) {
        addFieldLabel(container, field.getLabel());
        
        LinearLayout sliderLayout = new LinearLayout(this);
        sliderLayout.setOrientation(LinearLayout.VERTICAL);
        sliderLayout.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT));
        
        SeekBar seekBar = new SeekBar(this);
        seekBar.setMax(10);
        seekBar.setProgress(0);
        seekBar.setTag(field.getKey());
        
        if (value != null) {
            try {
                int progress = Integer.parseInt(value.toString());
                seekBar.setProgress(Math.min(progress, 10));
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        
        // 添加当前值显示
        TextView valueText = new TextView(this);
        int currentValue = seekBar.getProgress();
        valueText.setText("当前值: " + currentValue + "/10");
        valueText.setTextSize(12);
        valueText.setTextColor(getResources().getColor(android.R.color.darker_gray));
        
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                valueText.setText("当前值: " + progress + "/10");
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        sliderLayout.addView(seekBar);
        sliderLayout.addView(valueText);
        
        LinearLayout.MarginLayoutParams params = new LinearLayout.MarginLayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = 16;
        sliderLayout.setLayoutParams(params);
        
        container.addView(sliderLayout);
        editSpecificAttrSliders.put(field.getKey(), seekBar);
    }
    
    /**
     * 创建下拉选择
     */
    private void createSelectField(LinearLayout container, AttributeField field, Object value) {
        addFieldLabel(container, field.getLabel());
        
        Spinner spinner = new Spinner(this, Spinner.MODE_DROPDOWN);
        spinner.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT));
        spinner.setTag(field.getKey());
        
        // 构建选项
        String[] options = field.getOptions();
        String[] displayOptions = new String[options.length + 1];
        displayOptions[0] = "请选择";
        String[] optionValues = new String[options.length];
        for (int i = 0; i < options.length; i++) {
            int colonIndex = options[i].indexOf(':');
            if (colonIndex > 0) {
                optionValues[i] = options[i].substring(0, colonIndex);
                displayOptions[i + 1] = options[i].substring(colonIndex + 1);
            } else {
                optionValues[i] = options[i];
                displayOptions[i + 1] = options[i];
            }
        }
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            this, android.R.layout.simple_spinner_item, displayOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        
        // 设置当前值
        if (value != null) {
            for (int i = 0; i < optionValues.length; i++) {
                if (optionValues[i].equals(value.toString())) {
                    spinner.setSelection(i + 1);
                    break;
                }
            }
        }
        
        LinearLayout.MarginLayoutParams params = new LinearLayout.MarginLayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = 16;
        spinner.setLayoutParams(params);
        
        container.addView(spinner);
        
        // 存储Spinner引用和选项值
        editSpecificAttrFields.put(field.getKey(), spinner);
        editSpecificAttrSpinnerOptions.put(field.getKey(), optionValues);
    }
    
    /**
     * 创建标签列表输入
     */
    private void createTagListField(LinearLayout container, AttributeField field, Object value) {
        addFieldLabel(container, field.getLabel());
        
        ChipGroup chipGroup = new ChipGroup(this);
        chipGroup.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT));
        chipGroup.setTag(field.getKey());
        
        // 填充现有值
        if (value != null && value instanceof List) {
            List<?> list = (List<?>) value;
            for (Object item : list) {
                if (item != null) {
                    addTagChipToGroup(chipGroup, item.toString(), true);
                }
            }
        }
        
        // 添加"+ 添加"按钮
        addAddButtonChipToGroup(chipGroup, field.getKey(), true);
        
        // 设置双击添加
        setupChipGroupDoubleClickForEdit(chipGroup, field.getKey(), true);
        
        LinearLayout.MarginLayoutParams params = new LinearLayout.MarginLayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = 16;
        chipGroup.setLayoutParams(params);
        
        container.addView(chipGroup);
        editSpecificAttrChipGroups.put(field.getKey(), chipGroup);
    }
    
    /**
     * 添加字段标签
     */
    private void addFieldLabel(LinearLayout container, String text) {
        TextView label = new TextView(this);
        label.setText(text);
        label.setTextSize(14);
        label.setTextColor(getResources().getColor(android.R.color.darker_gray));
        label.setPadding(0, 8, 0, 4);
        container.addView(label);
    }
    
    /**
     * 添加Tag Chip到ChipGroup
     */
    private void addTagChipToGroup(ChipGroup chipGroup, String text, boolean removable) {
        Chip chip = new Chip(this);
        chip.setText(text);
        chip.setCloseIconVisible(removable);
        chip.setClickable(true);
        chip.setCheckable(false);
        
        if (removable) {
            chip.setOnCloseIconClickListener(v -> {
                chipGroup.removeView(chip);
            });
        }
        
        // 移除"+ 添加"按钮后再添加
        removeAddButtonChipFromGroup(chipGroup);
        chipGroup.addView(chip);
        
        // 重新添加"+ 添加"按钮
        Object tag = chipGroup.getTag();
        if (tag != null) {
            addAddButtonChipToGroup(chipGroup, tag.toString(), true);
        }
    }
    
    /**
     * 添加"+ 添加"按钮到ChipGroup
     */
    private void addAddButtonChipToGroup(ChipGroup chipGroup, String fieldKey, boolean isTag) {
        Chip addChip = new Chip(this);
        addChip.setText("+ 添加");
        addChip.setCloseIconVisible(false);
        addChip.setClickable(true);
        addChip.setCheckable(false);
        addChip.setChipBackgroundColorResource(android.R.color.transparent);
        addChip.setOnClickListener(v -> {
            showAddChipDialogForEdit(chipGroup, fieldKey, isTag);
        });
        
        chipGroup.addView(addChip);
    }
    
    /**
     * 移除"+ 添加"按钮
     */
    private void removeAddButtonChipFromGroup(ChipGroup chipGroup) {
        int childCount = chipGroup.getChildCount();
        for (int i = childCount - 1; i >= 0; i--) {
            View child = chipGroup.getChildAt(i);
            if (child instanceof Chip) {
                String text = ((Chip) child).getText().toString();
                if (text.equals("+ 添加")) {
                    chipGroup.removeViewAt(i);
                    break;
                }
            }
        }
    }
    
    /**
     * 设置ChipGroup双击添加
     */
    private void setupChipGroupDoubleClickForEdit(ChipGroup chipGroup, String fieldKey, boolean isTag) {
        chipGroup.setOnClickListener(v -> {
            // 双击空白区域添加
        });
    }
    
    /**
     * 显示添加Chip对话框
     */
    private void showAddChipDialogForEdit(ChipGroup chipGroup, String fieldKey, boolean isTag) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("添加" + (isTag ? "标签" : "项"));
        
        final EditText input = new EditText(this);
        input.setHint("请输入");
        builder.setView(input);
        
        builder.setPositiveButton("添加", (dialog, which) -> {
            String text = input.getText().toString().trim();
            if (!TextUtils.isEmpty(text)) {
                addTagChipToGroup(chipGroup, text, true);
            }
        });
        builder.setNegativeButton("取消", null);
        
        builder.show();
    }
    
    /**
     * 收集编辑的专属属性
     */
    private String collectSpecificAttributes() {
        String subCategory = currentSetting.getSubCategory();
        
        if (!attrParser.hasSpecificFields(subCategory)) {
            return null;
        }
        
        Map<String, Object> data = new HashMap<>();
        
        List<AttributeField> fields = attrParser.getFieldsForSubCategory(subCategory);
        
        for (AttributeField field : fields) {
            Object value = null;
            
            switch (field.getType()) {
                case TEXT:
                case TEXT_MULTI:
                case NUMBER:
                    View view = editSpecificAttrFields.get(field.getKey());
                    if (view instanceof EditText) {
                        EditText editText = (EditText) view;
                        if (!TextUtils.isEmpty(editText.getText())) {
                            String text = editText.getText().toString().trim();
                            if (field.getType() == FieldType.NUMBER) {
                                try {
                                    value = Integer.parseInt(text);
                                } catch (NumberFormatException e) {
                                    // 忽略无效数字
                                }
                            } else {
                                value = text;
                            }
                        }
                    }
                    break;
                    
                case SLIDER:
                    SeekBar seekBar = editSpecificAttrSliders.get(field.getKey());
                    if (seekBar != null) {
                        value = seekBar.getProgress();
                    }
                    break;
                    
                case SELECT:
                    Spinner spinner = (Spinner) editSpecificAttrFields.get(field.getKey());
                    if (spinner != null) {
                        int selectedPos = spinner.getSelectedItemPosition();
                        if (selectedPos > 0) {
                            // 从Map获取选项值数组
                            String[] optionValues = editSpecificAttrSpinnerOptions.get(field.getKey());
                            if (optionValues != null && selectedPos - 1 < optionValues.length) {
                                value = optionValues[selectedPos - 1];
                            }
                        }
                    }
                    break;
                    
                case TAG_LIST:
                    ChipGroup chipGroup = editSpecificAttrChipGroups.get(field.getKey());
                    if (chipGroup != null) {
                        List<String> tags = new ArrayList<>();
                        for (int i = 0; i < chipGroup.getChildCount(); i++) {
                            View child = chipGroup.getChildAt(i);
                            if (child instanceof Chip) {
                                String text = ((Chip) child).getText().toString();
                                if (!text.equals("+ 添加")) {
                                    tags.add(text);
                                }
                            }
                        }
                        if (!tags.isEmpty()) {
                            value = tags;
                        }
                    }
                    break;
            }
            
            if (value != null) {
                data.put(field.getKey(), value);
            }
        }
        
        if (data.isEmpty()) {
            return null;
        }
        
        return new Gson().toJson(data);
    }
}
