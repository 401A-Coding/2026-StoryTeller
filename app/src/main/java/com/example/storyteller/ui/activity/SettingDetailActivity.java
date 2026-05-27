package com.example.storyteller.ui.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.WindowInsetsCompat;

import com.example.storyteller.R;
import com.example.storyteller.data.local.db.SettingRelationshipDao;
import com.example.storyteller.data.local.db.StorySettingDao;
import com.example.storyteller.model.SettingRelationship;
import com.example.storyteller.model.StorySetting;
import com.example.storyteller.ui.dialog.SettingImageSelectionDialog;
import com.example.storyteller.utils.SettingCategoryConfig;
import com.example.storyteller.utils.SpecificAttributesParser;
import com.example.storyteller.utils.SpecificAttributesParser.AttributeField;
import com.example.storyteller.utils.SpecificAttributesParser.FieldType;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.gson.Gson;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
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
    private ImageView ivSettingImage;    // 配图
    private Button btnChangeImage;       // 更换配图按钮
    private Button btnDeleteImage;       // 删除配图按钮
    
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
    private SettingRelationshipDao relationshipDao;
    private StorySetting currentSetting;
    private int storyId;
    
    private boolean isEditMode = false;
    
    // 关联关系列表（缓存）
    private List<SettingRelationship> relationsList = new ArrayList<>();
    
    // 专属属性解析器
    private SpecificAttributesParser attrParser;
    // 存储编辑时的专属属性数据（TEXT、TEXT_MULTI类型）
    private Map<String, View> editSpecificAttrFields;
    private Map<String, ChipGroup> editSpecificAttrChipGroups;
    private Map<String, SeekBar> editSpecificAttrSliders;
    // 存储Spinner的选项值数组
    private Map<String, String[]> editSpecificAttrSpinnerOptions;
    // 存储自定义属性编辑控件
    private Map<String, EditText> editCustomAttrFields;
    // 存储结构化列表的编辑数据（每项是一个包含各字段的列表）
    private Map<String, List<List<String>>> editStructuredListItems;

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
        relationshipDao = new SettingRelationshipDao(this);
        attrParser = new SpecificAttributesParser();
        editSpecificAttrFields = new HashMap<>();
        editSpecificAttrChipGroups = new HashMap<>();
        editSpecificAttrSliders = new HashMap<>();
        editSpecificAttrSpinnerOptions = new HashMap<>();
        editCustomAttrFields = new HashMap<>();
        editStructuredListItems = new HashMap<>();
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
        // 刘海屏适配
        View rootView = findViewById(android.R.id.content);
        rootView.setOnApplyWindowInsetsListener((v, insets) -> {
            androidx.core.graphics.Insets systemBars = androidx.core.view.WindowInsetsCompat.toWindowInsetsCompat(insets, v)
                .getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, 0);
            return insets;
        });
        rootView.requestApplyInsets();
        
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
        ivSettingImage = findViewById(R.id.iv_setting_image);
        btnChangeImage = findViewById(R.id.btn_change_image);
        btnDeleteImage = findViewById(R.id.btn_delete_image);
        
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
        
        // 编辑模式关联设定按钮
        Button btnEditAddRelation = findViewById(R.id.btn_edit_add_relation);
        if (btnEditAddRelation != null) {
            btnEditAddRelation.setOnClickListener(v -> showAddRelationDialog());
        }
        
        // 关系图按钮
        Button btnViewGraph = findViewById(R.id.btn_view_graph);
        if (btnViewGraph != null) {
            btnViewGraph.setOnClickListener(v -> {
                Intent intent = new Intent(SettingDetailActivity.this, PlotGraphActivity.class);
                intent.putExtra(PlotGraphActivity.EXTRA_SETTING_ID, currentSetting.getId());
                intent.putExtra(PlotGraphActivity.EXTRA_STORY_ID, storyId);
                startActivity(intent);
            });
        }
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
        
        // 新建模式下隐藏配图卡片
        View cardSettingImage = findViewById(R.id.card_setting_image);
        if (cardSettingImage != null) {
            cardSettingImage.setVisibility(View.GONE);
        }
        
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
        
        // 自定义属性（查看模式）
        displayCustomAttributes();
        
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
        
        // 配图
        displaySettingImage();
        
        // 填充查看模式的标签和别名
        fillViewModeTagsAndAliases();
        
        // 显示关联设定
        displayRelations();
        
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
        
        // 显示编辑模式的关联设定
        displayEditModeRelations();
        
        // 显示配图卡片
        displaySettingImage();
        
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
        
        // 获取自定义属性
        String customAttrsJson = collectCustomAttributes();
        currentSetting.setAttributes(customAttrsJson);
        
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
            // 保存关系描述的修改
            saveRelations();
            
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
     * 保存所有关系的修改（描述等）
     */
    private void saveRelations() {
        if (relationsList == null || relationsList.isEmpty()) {
            return;
        }
        for (SettingRelationship rel : relationsList) {
            relationshipDao.update(rel);
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
        editStructuredListItems.clear();
        
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
        
        // 填充自定义属性编辑控件
        fillCustomAttrEditFields();
    }
    
    /**
     * 填充自定义属性编辑控件
     */
    private void fillCustomAttrEditFields() {
        LinearLayout layoutEditCustomAttrs = findViewById(R.id.layout_edit_custom_attrs);
        layoutEditCustomAttrs.removeAllViews();
        editCustomAttrFields.clear();
        
        // 解析现有数据
        Map<String, Object> existingData = new HashMap<>();
        if (!TextUtils.isEmpty(currentSetting.getAttributes())) {
            try {
                com.google.gson.JsonObject jsonObj = JsonParser.parseString(
                    currentSetting.getAttributes()).getAsJsonObject();
                for (Map.Entry<String, com.google.gson.JsonElement> entry : jsonObj.entrySet()) {
                    existingData.put(entry.getKey(), jsonToDisplayValue(entry.getValue()));
                }
            } catch (Exception e) {
                // 解析失败，使用空数据
            }
        }
        
        // 添加"+ 添加属性"按钮
        addCustomAttrAddButton(layoutEditCustomAttrs);
        
        // 如果有现有数据，生成编辑控件
        for (Map.Entry<String, Object> entry : existingData.entrySet()) {
            addCustomAttrEditRow(layoutEditCustomAttrs, entry.getKey(), entry.getValue());
        }
    }
    
    /**
     * 添加自定义属性编辑行
     */
    private void addCustomAttrEditRow(LinearLayout container, String key, Object value) {
        // 键值对容器
        LinearLayout rowLayout = new LinearLayout(this);
        rowLayout.setOrientation(LinearLayout.HORIZONTAL);
        rowLayout.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT));
        rowLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);
        
        // 键输入框
        EditText etKey = new EditText(this);
        etKey.setLayoutParams(new LinearLayout.LayoutParams(0,
            LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        etKey.setHint("属性名");
        etKey.setTextSize(14);
        etKey.setPadding(12, 10, 12, 10);
        etKey.setBackgroundResource(R.drawable.bg_outline_edittext);
        etKey.setText(key);
        etKey.setGravity(android.view.Gravity.CENTER_VERTICAL);
        
        // 值输入框
        EditText etValue = new EditText(this);
        etValue.setLayoutParams(new LinearLayout.LayoutParams(0,
            LinearLayout.LayoutParams.WRAP_CONTENT, 2f));
        etValue.setHint("属性值");
        etValue.setTextSize(14);
        etValue.setPadding(12, 10, 12, 10);
        etValue.setBackgroundResource(R.drawable.bg_outline_edittext);
        etValue.setText(value != null ? value.toString() : "");
        etValue.setGravity(android.view.Gravity.CENTER_VERTICAL);
        
        // 删除按钮（使用Chip风格）
        Chip chipDelete = new Chip(this);
        chipDelete.setChipIconResource(android.R.drawable.ic_menu_close_clear_cancel);
        chipDelete.setChipIconVisible(true);
        chipDelete.setClickable(true);
        chipDelete.setCheckable(false);
        chipDelete.setOnClickListener(v -> {
            container.removeView(rowLayout);
        });
        
        rowLayout.addView(etKey);
        rowLayout.addView(etValue);
        rowLayout.addView(chipDelete);
        
        LinearLayout.MarginLayoutParams params = new LinearLayout.MarginLayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = 8;
        rowLayout.setLayoutParams(params);
        
        // 在添加按钮之前插入
        int addButtonIndex = container.getChildCount() - 1;
        if (addButtonIndex >= 0) {
            container.addView(rowLayout, addButtonIndex);
        } else {
            container.addView(rowLayout);
        }
        
        // 存储引用
        String uniqueKey = key + "_" + System.currentTimeMillis();
        editCustomAttrFields.put(uniqueKey, etKey);
        editCustomAttrFields.put(uniqueKey + "_value", etValue);
    }
    
    /**
     * 添加自定义属性添加按钮
     */
    private void addCustomAttrAddButton(LinearLayout container) {
        Chip chipAdd = new Chip(this);
        chipAdd.setText("添加属性");
        chipAdd.setChipIconResource(android.R.drawable.ic_menu_add);
        chipAdd.setChipIconVisible(true);
        chipAdd.setClickable(true);
        chipAdd.setCheckable(false);
        chipAdd.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT));
        chipAdd.setOnClickListener(v -> {
            addCustomAttrEditRow(container, "", "");
        });
        container.addView(chipAdd);
    }
    
    /**
     * 收集自定义属性
     */
    private String collectCustomAttributes() {
        Map<String, String> data = new HashMap<>();
        
        LinearLayout container = findViewById(R.id.layout_edit_custom_attrs);
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            if (child instanceof LinearLayout) {
                LinearLayout row = (LinearLayout) child;
                if (row.getChildCount() >= 2) {
                    View keyView = row.getChildAt(0);
                    View valueView = row.getChildAt(1);
                    if (keyView instanceof EditText && valueView instanceof EditText) {
                        String key = ((EditText) keyView).getText().toString().trim();
                        String value = ((EditText) valueView).getText().toString().trim();
                        if (!TextUtils.isEmpty(key) && !TextUtils.isEmpty(value)) {
                            data.put(key, value);
                        }
                    }
                }
            }
        }
        
        if (data.isEmpty()) {
            return null;
        }
        return new Gson().toJson(data);
    }
    
    /**
     * 结构化显示自定义属性
     */
    private void displayCustomAttributes() {
        View layoutCustomAttrs = findViewById(R.id.layout_custom_attrs);
        LinearLayout layoutContent = findViewById(R.id.layout_custom_attrs_content);
        
        if (TextUtils.isEmpty(currentSetting.getAttributes())) {
            layoutCustomAttrs.setVisibility(View.GONE);
            return;
        }
        
        try {
            layoutContent.removeAllViews();
            com.google.gson.JsonObject jsonObj = JsonParser.parseString(
                currentSetting.getAttributes()).getAsJsonObject();
            
            for (Map.Entry<String, com.google.gson.JsonElement> entry : jsonObj.entrySet()) {
                String key = entry.getKey();
                String value = jsonToDisplayValue(entry.getValue()).toString();
                addSpecificAttrView(layoutContent, key, value);
            }
            
            if (jsonObj.size() > 0) {
                layoutCustomAttrs.setVisibility(View.VISIBLE);
            } else {
                layoutCustomAttrs.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            layoutCustomAttrs.setVisibility(View.GONE);
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
            case STRUCTURED_LIST:
                createStructuredListField(container, field, value);
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
     * 创建结构化列表输入控件
     * 每条记录包含多个字段，按指定格式分隔
     */
    private void createStructuredListField(LinearLayout container, AttributeField field, Object value) {
        addFieldLabel(container, field.getLabel());
        
        // 获取模板字段
        String template = field.getInputTemplate();
        String fieldDelimiter = field.getFieldDelimiter();
        String[] fieldNames = template != null ? template.split(",") : new String[]{"值"};
        
        // 创建容器存储所有行的数据
        List<List<String>> allItems = new ArrayList<>();
        editStructuredListItems.put(field.getKey(), allItems);
        
        // 主容器
        LinearLayout mainContainer = new LinearLayout(this);
        mainContainer.setOrientation(LinearLayout.VERTICAL);
        mainContainer.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT));
        
        // 添加格式提示
        TextView formatHint = new TextView(this);
        formatHint.setText("格式：" + template.replace(",", fieldDelimiter) + "（回车分隔多条）");
        formatHint.setTextSize(12);
        formatHint.setTextColor(getResources().getColor(android.R.color.darker_gray));
        formatHint.setPadding(0, 4, 0, 8);
        mainContainer.addView(formatHint);
        
        // 解析现有数据
        List<List<String>> existingItems = parseStructuredListValue(value, fieldDelimiter);
        
        // 添加现有项
        for (List<String> item : existingItems) {
            addStructuredListRow(mainContainer, field, fieldNames, fieldDelimiter, allItems, item);
        }
        
        // 添加按钮
        Chip addChip = new Chip(this);
        addChip.setText("+ 添加");
        addChip.setCloseIconVisible(false);
        addChip.setClickable(true);
        addChip.setCheckable(false);
        addChip.setChipBackgroundColorResource(android.R.color.transparent);
        addChip.setOnClickListener(v -> {
            // 添加空行
            List<String> newItem = new ArrayList<>();
            for (String name : fieldNames) {
                newItem.add("");
            }
            addStructuredListRow(mainContainer, field, fieldNames, fieldDelimiter, allItems, newItem);
        });
        mainContainer.addView(addChip);
        
        LinearLayout.MarginLayoutParams params = new LinearLayout.MarginLayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = 16;
        mainContainer.setLayoutParams(params);
        
        container.addView(mainContainer);
    }
    
    /**
     * 添加结构化列表的一行
     */
    private void addStructuredListRow(LinearLayout container, AttributeField field, String[] fieldNames, 
                                      String fieldDelimiter, List<List<String>> allItems, List<String> itemData) {
        // 每行是一个水平布局
        LinearLayout rowLayout = new LinearLayout(this);
        rowLayout.setOrientation(LinearLayout.HORIZONTAL);
        rowLayout.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT));
        rowLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);
        
        // 初始化空数据
        if (itemData == null || itemData.isEmpty()) {
            itemData = new ArrayList<>();
            for (String name : fieldNames) {
                itemData.add("");
            }
        }
        
        // 存储此行的引用
        final int rowIndex = allItems.size();
        allItems.add(itemData);
        
        // 添加每个字段的输入框
        final List<EditText> rowEditTexts = new ArrayList<>();
        for (int i = 0; i < fieldNames.length; i++) {
            EditText editText = new EditText(this);
            editText.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            editText.setHint(fieldNames[i]);
            editText.setTextSize(14);
            editText.setPadding(8, 8, 8, 8);
            editText.setBackgroundResource(R.drawable.bg_outline_edittext);
            if (i < itemData.size() && itemData.get(i) != null) {
                editText.setText(itemData.get(i));
            }
            editText.setGravity(android.view.Gravity.CENTER);
            
            // 监听文本变化
            final int fieldIndex = i;
            editText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                
                @Override
                public void afterTextChanged(Editable s) {
                    // 更新数据
                    List<List<String>> items = editStructuredListItems.get(field.getKey());
                    if (items != null && rowIndex < items.size()) {
                        List<String> row = items.get(rowIndex);
                        if (fieldIndex < row.size()) {
                            row.set(fieldIndex, s.toString());
                        }
                    }
                }
            });
            
            rowEditTexts.add(editText);
            rowLayout.addView(editText);
            
            // 添加分隔符标签（除了最后一个字段）
            if (i < fieldNames.length - 1) {
                TextView delimiterLabel = new TextView(this);
                delimiterLabel.setText(fieldDelimiter);
                delimiterLabel.setTextSize(14);
                delimiterLabel.setTextColor(getResources().getColor(android.R.color.darker_gray));
                rowLayout.addView(delimiterLabel);
            }
        }
        
        // 添加删除按钮
        Chip deleteChip = new Chip(this);
        deleteChip.setChipIconResource(android.R.drawable.ic_menu_close_clear_cancel);
        deleteChip.setChipIconVisible(true);
        deleteChip.setClickable(true);
        deleteChip.setCheckable(false);
        deleteChip.setOnClickListener(v -> {
            container.removeView(rowLayout);
            // 从数据中移除
            allItems.remove(rowIndex);
        });
        rowLayout.addView(deleteChip);
        
        // 设置行间距
        LinearLayout.MarginLayoutParams rowParams = new LinearLayout.MarginLayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        rowParams.bottomMargin = 8;
        rowLayout.setLayoutParams(rowParams);
        
        // 在添加按钮之前插入
        int addButtonIndex = container.getChildCount() - 1;
        if (addButtonIndex >= 0) {
            container.addView(rowLayout, addButtonIndex);
        } else {
            container.addView(rowLayout);
        }
    }
    
    /**
     * 解析结构化列表值
     */
    private List<List<String>> parseStructuredListValue(Object value, String fieldDelimiter) {
        List<List<String>> result = new ArrayList<>();
        if (value == null) {
            return result;
        }
        
        if (value instanceof List) {
            // 如果是嵌套列表
            List<?> listValue = (List<?>) value;
            for (Object item : listValue) {
                if (item instanceof List) {
                    List<?> subList = (List<?>) item;
                    List<String> row = new ArrayList<>();
                    for (Object subItem : subList) {
                        row.add(subItem != null ? subItem.toString() : "");
                    }
                    result.add(row);
                } else if (item != null) {
                    // 如果是字符串，按分隔符解析
                    List<String> row = new ArrayList<>();
                    String[] parts = item.toString().split(fieldDelimiter);
                    for (String part : parts) {
                        row.add(part.trim());
                    }
                    result.add(row);
                }
            }
        } else if (value instanceof String) {
            // 如果是字符串，按换行和分隔符解析
            String[] lines = value.toString().split("\n");
            for (String line : lines) {
                if (!line.trim().isEmpty()) {
                    List<String> row = new ArrayList<>();
                    String[] parts = line.split(fieldDelimiter);
                    for (String part : parts) {
                        row.add(part.trim());
                    }
                    result.add(row);
                }
            }
        }
        return result;
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
                    
                case STRUCTURED_LIST:
                    List<List<String>> structuredList = editStructuredListItems.get(field.getKey());
                    if (structuredList != null && !structuredList.isEmpty()) {
                        value = structuredList;
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
    
    // ==================== 关联设定功能 ====================
    
    /**
     * 显示关联设定区块
     */
    private void displayRelations() {
        if (currentSetting.getId() == 0) {
            // 新建设定时不显示关联区块
            return;
        }
        
        if (storyId == 0) {
            // 全局素材不显示关联设定
            findViewById(R.id.layout_relations).setVisibility(View.GONE);
            return;
        }
        
        // 获取关联关系
        relationsList = relationshipDao.getBySettingId(currentSetting.getId());
        
        LinearLayout layoutRelations = findViewById(R.id.layout_relations);
        LinearLayout layoutRelationsContent = findViewById(R.id.layout_relations_content);
        TextView tvRelationsCount = findViewById(R.id.tv_relations_count);
        
        if (relationsList == null || relationsList.isEmpty()) {
            layoutRelations.setVisibility(View.VISIBLE);
            layoutRelationsContent.removeAllViews();
            tvRelationsCount.setText(" (0)");
            
            // 添加空状态提示
            TextView emptyHint = new TextView(this);
            emptyHint.setText("暂无关联设定");
            emptyHint.setTextSize(14);
            emptyHint.setTextColor(getResources().getColor(android.R.color.darker_gray));
            layoutRelationsContent.addView(emptyHint);
        } else {
            layoutRelations.setVisibility(View.VISIBLE);
            layoutRelationsContent.removeAllViews();
            tvRelationsCount.setText(" (" + relationsList.size() + ")");
            
            // 添加每个关联项
            for (SettingRelationship rel : relationsList) {
                addViewModeRelationItemView(layoutRelationsContent, rel);
            }
        }
    }
    
    /**
     * 显示设定配图
     */
    private void displaySettingImage() {
        View cardSettingImage = findViewById(R.id.card_setting_image);
        if (cardSettingImage == null) return;
        
        String imagePath = currentSetting != null ? currentSetting.getImagePath() : null;
        boolean hasImage = imagePath != null && !imagePath.isEmpty();
        
        // 编辑模式始终显示卡片；查看模式仅在有配图时显示
        // 但新建模式下（id==0）不显示配图卡片
        if (isEditMode && (currentSetting == null || currentSetting.getId() != 0)) {
            cardSettingImage.setVisibility(View.VISIBLE);
            
            if (hasImage && ivSettingImage != null) {
                File imageFile = new File(imagePath);
                if (imageFile.exists()) {
                    Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
                    if (bitmap != null) {
                        ivSettingImage.setImageBitmap(bitmap);
                    }
                } else {
                    ivSettingImage.setImageResource(R.drawable.ic_insert_drive_file);
                }
                // 编辑模式显示"更换配图"按钮
                if (btnChangeImage != null) {
                    btnChangeImage.setText("更换配图");
                    btnChangeImage.setOnClickListener(v -> showImageSelectionDialog());
                }
                // 有配图时显示删除按钮
                if (btnDeleteImage != null) {
                    btnDeleteImage.setVisibility(View.VISIBLE);
                    btnDeleteImage.setOnClickListener(v -> showDeleteImageConfirmDialog());
                }
            } else if (ivSettingImage != null) {
                // 无配图时显示添加图标和提示
                ivSettingImage.setImageResource(R.drawable.ic_insert_drive_file);
                ivSettingImage.setScaleType(ImageView.ScaleType.CENTER);
                // 编辑模式无配图时显示"添加配图"按钮
                if (btnChangeImage != null) {
                    btnChangeImage.setText("添加配图");
                    btnChangeImage.setOnClickListener(v -> showImageSelectionDialog());
                }
                // 无配图时隐藏删除按钮
                if (btnDeleteImage != null) {
                    btnDeleteImage.setVisibility(View.GONE);
                }
            }
        } else {
            // 查看模式：使用独立的配图卡片
            View cardViewMode = findViewById(R.id.card_setting_image_view);
            ImageView ivViewMode = findViewById(R.id.iv_setting_image_view);
            if (hasImage && ivViewMode != null) {
                cardViewMode.setVisibility(View.VISIBLE);
                File imageFile = new File(imagePath);
                if (imageFile.exists()) {
                    Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
                    if (bitmap != null) {
                        ivViewMode.setImageBitmap(bitmap);
                    }
                }
            } else if (cardViewMode != null) {
                cardViewMode.setVisibility(View.GONE);
            }
        }
    }
    
    /**
     * 显示配图选择对话框
     */
    private SettingImageSelectionDialog currentImageDialog;
    
    private void showImageSelectionDialog() {
        currentImageDialog = new SettingImageSelectionDialog(this, currentSetting, new SettingImageSelectionDialog.OnImageSelectedListener() {
            @Override
            public void onImageSelected(String imagePath) {
                settingDao.updateSettingImage(currentSetting.getId(), imagePath);
                currentSetting.setImagePath(imagePath);
                displaySettingImage();
            }
            
            @Override
            public void onGenerateRequested() {
                Toast.makeText(SettingDetailActivity.this, "AI生成配图功能待实现", Toast.LENGTH_SHORT).show();
            }
        });
        
        // 设置相册选择监听器
        currentImageDialog.setOnGalleryPickListener(() -> pickImageFromGallery());
        
        currentImageDialog.show();
    }
    
    private static final int REQUEST_PICK_IMAGE = 1001;
    
    /**
     * 显示删除配图确认对话框
     */
    private void showDeleteImageConfirmDialog() {
        new AlertDialog.Builder(this)
            .setTitle("删除配图")
            .setMessage("确定要删除这张配图吗？")
            .setPositiveButton("删除", (dialog, which) -> deleteSettingImage())
            .setNegativeButton("取消", null)
            .show();
    }
    
    /**
     * 删除配图
     */
    private void deleteSettingImage() {
        String imagePath = currentSetting.getImagePath();
        if (imagePath != null && !imagePath.isEmpty()) {
            // 删除本地文件
            File file = new File(imagePath);
            if (file.exists()) {
                file.delete();
            }
            // 更新数据库
            settingDao.updateSettingImage(currentSetting.getId(), null);
            currentSetting.setImagePath(null);
            // 刷新显示
            displaySettingImage();
        }
    }
    
    /**
     * 从相册选择图片
     */
    private void pickImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_PICK_IMAGE);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri selectedImageUri = data.getData();
            if (selectedImageUri != null && currentImageDialog != null) {
                // 将URI转换为文件路径并保存
                String imagePath = copyImageToAppStorage(selectedImageUri);
                if (imagePath != null) {
                    currentImageDialog.addGeneratedImage(imagePath);
                }
            }
        }
    }
    
    /**
     * 将图片复制到应用存储目录
     */
    private String copyImageToAppStorage(Uri sourceUri) {
        try {
            String fileName = "setting_" + System.currentTimeMillis() + ".jpg";
            File outputDir = new File(getFilesDir(), "setting_images");
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }
            File outputFile = new File(outputDir, fileName);
            
            java.io.InputStream inputStream = getContentResolver().openInputStream(sourceUri);
            java.io.FileOutputStream outputStream = new java.io.FileOutputStream(outputFile);
            
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            
            inputStream.close();
            outputStream.close();
            
            return outputFile.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "保存图片失败", Toast.LENGTH_SHORT).show();
            return null;
        }
    }
    
    /**
     * 添加关联项视图
     */
    private void addViewModeRelationItemView(LinearLayout container, SettingRelationship rel) {
        // 主行：A → [关系] → B
        LinearLayout itemLayout = new LinearLayout(this);
        itemLayout.setOrientation(LinearLayout.HORIZONTAL);
        itemLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);
        itemLayout.setPadding(0, 4, 0, 4);
        
        // 源设定名称
        TextView tvSource = new TextView(this);
        tvSource.setText(rel.getSourceSettingTitle() != null ? rel.getSourceSettingTitle() : "设定" + rel.getSourceSettingId());
        tvSource.setTextSize(14);
        if (rel.isSourceSettingDeleted()) {
            // 已删除的设定显示为灰色，不可点击
            tvSource.setTextColor(getResources().getColor(android.R.color.darker_gray));
            tvSource.setText(tvSource.getText() + " [已删除]");
        } else {
            tvSource.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
            // 点击事件
            View.OnClickListener sourceClickListener = v -> {
                Intent intent = new Intent(SettingDetailActivity.this, SettingDetailActivity.class);
                intent.putExtra(SettingDetailActivity.EXTRA_SETTING_ID, rel.getSourceSettingId());
                intent.putExtra(SettingDetailActivity.EXTRA_STORY_ID, storyId);
                startActivity(intent);
            };
            tvSource.setOnClickListener(sourceClickListener);
        }
        tvSource.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT));
        
        // 箭头和关系类型（灰色）
        TextView tvArrow = new TextView(this);
        String arrow = rel.isDirected() ? " → " : " ↔ ";
        tvArrow.setText(arrow + rel.getTypeDisplayName() + arrow);
        tvArrow.setTextSize(12);
        tvArrow.setTextColor(getResources().getColor(android.R.color.darker_gray));
        tvArrow.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT));
        
        // 目标设定名称
        TextView tvTarget = new TextView(this);
        tvTarget.setText(rel.getTargetSettingTitle() != null ? rel.getTargetSettingTitle() : "设定" + rel.getTargetSettingId());
        tvTarget.setTextSize(14);
        if (rel.isTargetSettingDeleted()) {
            // 已删除的设定显示为灰色，不可点击
            tvTarget.setTextColor(getResources().getColor(android.R.color.darker_gray));
            tvTarget.setText(tvTarget.getText() + " [已删除]");
        } else {
            tvTarget.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
            // 点击事件
            View.OnClickListener targetClickListener = v -> {
                Intent intent = new Intent(SettingDetailActivity.this, SettingDetailActivity.class);
                intent.putExtra(SettingDetailActivity.EXTRA_SETTING_ID, rel.getTargetSettingId());
                intent.putExtra(SettingDetailActivity.EXTRA_STORY_ID, storyId);
                startActivity(intent);
            };
            tvTarget.setOnClickListener(targetClickListener);
        }
        tvTarget.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        
        itemLayout.addView(tvSource);
        itemLayout.addView(tvArrow);
        itemLayout.addView(tvTarget);
        
        container.addView(itemLayout);
        
        // 如果有描述，添加描述行
        if (rel.getDescription() != null && !rel.getDescription().isEmpty()) {
            TextView tvDesc = new TextView(this);
            tvDesc.setText(rel.getDescription());
            tvDesc.setTextSize(12);
            tvDesc.setTextColor(getResources().getColor(android.R.color.darker_gray));
            tvDesc.setPadding(0, 0, 0, 8);
            container.addView(tvDesc);
        }
    }
    
    /**
     * 添加编辑模式的关联项视图（有删除按钮，可编辑描述）
     */
    private void addEditModeRelationItemView(LinearLayout container, SettingRelationship rel) {
        // 使用布局文件
        View view = getLayoutInflater().inflate(R.layout.item_relation_edit, container, false);
        
        // 获取控件
        TextView tvSource = view.findViewById(R.id.tv_source);
        TextView tvArrow = view.findViewById(R.id.tv_arrow);
        TextView tvTarget = view.findViewById(R.id.tv_target);
        Button btnToggleDirection = view.findViewById(R.id.btn_toggle_direction);
        Button btnDelete = view.findViewById(R.id.btn_delete);
        EditText etDesc = view.findViewById(R.id.et_description);
        
        // 设置内容
        String sourceName = rel.getSourceSettingTitle() != null ? rel.getSourceSettingTitle() : "设定" + rel.getSourceSettingId();
        String targetName = rel.getTargetSettingTitle() != null ? rel.getTargetSettingTitle() : "设定" + rel.getTargetSettingId();
        
        // 源设定
        if (rel.isSourceSettingDeleted()) {
            tvSource.setText(sourceName + " [已删除]");
            tvSource.setTextColor(getResources().getColor(android.R.color.darker_gray));
        } else {
            tvSource.setText(sourceName);
            tvSource.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
            tvSource.setOnClickListener(v -> {
                Intent intent = new Intent(SettingDetailActivity.this, SettingDetailActivity.class);
                intent.putExtra(SettingDetailActivity.EXTRA_SETTING_ID, rel.getSourceSettingId());
                intent.putExtra(SettingDetailActivity.EXTRA_STORY_ID, storyId);
                startActivity(intent);
            });
        }
        
        // 箭头和关系类型
        String arrow = rel.isDirected() ? " → " : " ↔ ";
        tvArrow.setText(arrow + rel.getTypeDisplayName() + arrow);
        
        // 目标设定
        if (rel.isTargetSettingDeleted()) {
            tvTarget.setText(targetName + " [已删除]");
            tvTarget.setTextColor(getResources().getColor(android.R.color.darker_gray));
        } else {
            tvTarget.setText(targetName);
            tvTarget.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
            tvTarget.setOnClickListener(v -> {
                Intent intent = new Intent(SettingDetailActivity.this, SettingDetailActivity.class);
                intent.putExtra(SettingDetailActivity.EXTRA_SETTING_ID, rel.getTargetSettingId());
                intent.putExtra(SettingDetailActivity.EXTRA_STORY_ID, storyId);
                startActivity(intent);
            });
        }
        
        // 单/双向切换按钮
        btnToggleDirection.setText(rel.isDirected() ? "→" : "↔");
        if (rel.isSourceSettingDeleted() || rel.isTargetSettingDeleted()) {
            btnToggleDirection.setEnabled(false);
            btnToggleDirection.setTextColor(getResources().getColor(android.R.color.darker_gray));
        } else {
            btnToggleDirection.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
            btnToggleDirection.setOnClickListener(v -> {
                rel.setDirected(!rel.isDirected());
                String newArrow = rel.isDirected() ? " → " : " ↔ ";
                tvArrow.setText(newArrow + rel.getTypeDisplayName() + newArrow);
                btnToggleDirection.setText(rel.isDirected() ? "→" : "↔");
                relationshipDao.update(rel);
                Toast.makeText(this, rel.isDirected() ? "已切换为单向" : "已切换为双向", Toast.LENGTH_SHORT).show();
            });
        }
        
        // 删除按钮
        if (!rel.isSourceSettingDeleted() && !rel.isTargetSettingDeleted()) {
            btnDelete.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
            btnDelete.setOnClickListener(v -> showDeleteRelationConfirmDialog(rel));
        } else {
            btnDelete.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            btnDelete.setOnClickListener(v -> showDeleteRelationConfirmDialog(rel));
        }
        
        // 描述输入框
        etDesc.setText(rel.getDescription() != null ? rel.getDescription() : "");
        if (rel.isSourceSettingDeleted() || rel.isTargetSettingDeleted()) {
            etDesc.setEnabled(false);
            etDesc.setHint("已删除设定关联");
        } else {
            final String originalText = rel.getDescription() != null ? rel.getDescription() : "";
            etDesc.addTextChangedListener(new android.text.TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(android.text.Editable s) {
                    String newText = s.toString();
                    if (!newText.equals(originalText)) {
                        rel.setDescription(newText.isEmpty() ? null : newText);
                    }
                }
            });
        }
        
        container.addView(view);
    }
    
    /**
     * 显示添加关联对话框
     */
    private void showAddRelationDialog() {
        if (currentSetting.getId() == 0) {
            Toast.makeText(this, "请先保存设定后再添加关联", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 获取可关联的设定列表（同一小说或全局素材库）
        List<StorySetting> availableSettings = settingDao.getByStoryId(storyId);
        // 过滤掉当前设定
        List<StorySetting> selectableSettings = new ArrayList<>();
        for (StorySetting s : availableSettings) {
            if (s.getId() != currentSetting.getId()) {
                selectableSettings.add(s);
            }
        }
        
        if (selectableSettings.isEmpty()) {
            Toast.makeText(this, "没有其他设定可以关联", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 构建设定选择对话框
        String[] settingNames = new String[selectableSettings.size()];
        for (int i = 0; i < selectableSettings.size(); i++) {
            settingNames[i] = selectableSettings.get(i).getTitle() + 
                " (" + selectableSettings.get(i).getCategory() + " · " + selectableSettings.get(i).getSubCategory() + ")";
        }
        
        new AlertDialog.Builder(this)
            .setTitle("选择关联的设定")
            .setItems(settingNames, (dialog, which) -> {
                StorySetting targetSetting = selectableSettings.get(which);
                showRelationTypeDialog(targetSetting);
            })
            .setNegativeButton("取消", null)
            .show();
    }
    
    /**
     * 显示关系类型输入对话框
     */
    private void showRelationTypeDialog(StorySetting targetSetting) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("输入关系类型");
        
        // 创建包含提示文本和输入框的布局
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(48, 16, 48, 0);
        
        TextView tvHint = new TextView(this);
        tvHint.setText("输入关系类型（如：师徒、位于、属于、朋友等）");
        tvHint.setTextSize(13);
        tvHint.setTextColor(getResources().getColor(android.R.color.darker_gray));
        container.addView(tvHint);
        
        final EditText input = new EditText(this);
        input.setHint("输入关系类型");
        input.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT));
        input.setPadding(24, 16, 24, 16);
        container.addView(input);
        
        builder.setView(container);
        builder.setPositiveButton("下一步", (dialog, which) -> {
            String relationType = input.getText().toString().trim();
            if (!relationType.isEmpty()) {
                showRelationDescriptionDialog(targetSetting, relationType);
            } else {
                Toast.makeText(this, "请输入关系类型", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }
    
    /**
     * 显示关系描述输入对话框（可选）
     */
    private void showRelationDescriptionDialog(StorySetting targetSetting, String relationType) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("添加描述（可选）");
        
        // 创建包含提示文本和输入框的布局
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(48, 16, 48, 0);
        
        TextView tvHint = new TextView(this);
        tvHint.setText("可为关系添加说明（如：军事统治、经济控制）\n留空则不显示描述");
        tvHint.setTextSize(13);
        tvHint.setTextColor(getResources().getColor(android.R.color.darker_gray));
        container.addView(tvHint);
        
        final EditText input = new EditText(this);
        input.setHint("输入描述（可选）");
        input.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT));
        input.setPadding(24, 16, 24, 16);
        container.addView(input);
        
        builder.setView(container);
        builder.setPositiveButton("确定", (dialog, which) -> {
            String description = input.getText().toString().trim();
            createRelation(targetSetting, relationType, TextUtils.isEmpty(description) ? null : description);
        });
        builder.setNegativeButton("跳过", null);
        builder.show();
    }
    
    /**
     * 创建关联关系
     */
    private void createRelation(StorySetting targetSetting, String relationType, String description) {
        // 检查是否已存在关系
        if (relationshipDao.exists(currentSetting.getId(), targetSetting.getId(), relationType)) {
            Toast.makeText(this, "该关系已存在", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 创建关系对象
        SettingRelationship relationship = new SettingRelationship();
        relationship.setStoryId(storyId);
        relationship.setSourceSettingId(currentSetting.getId());
        relationship.setTargetSettingId(targetSetting.getId());
        relationship.setRelationshipType(relationType);
        relationship.setDescription(description);
        relationship.setSourceType(SettingRelationship.SOURCE_TYPE_MANUAL);
        relationship.setConfidence(1.0);
        relationship.setCreateTime(System.currentTimeMillis());
        relationship.setUpdateTime(System.currentTimeMillis());
        
        // 插入数据库
        long id = relationshipDao.insert(relationship);
        if (id > 0) {
            Toast.makeText(this, "已添加关联", Toast.LENGTH_SHORT).show();
            // 刷新关联显示
            if (isEditMode) {
                displayEditModeRelations();
            } else {
                displayRelations();
            }
        } else {
            Toast.makeText(this, "添加关联失败", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 显示删除关联确认对话框
     */
    private void showDeleteRelationConfirmDialog(SettingRelationship rel) {
        String targetTitle = rel.getTargetSettingId() == currentSetting.getId() 
            ? rel.getSourceSettingTitle() 
            : rel.getTargetSettingTitle();
        
        new AlertDialog.Builder(this)
            .setTitle("删除关联")
            .setMessage("确定要删除与「" + targetTitle + "」的关联吗？")
            .setPositiveButton("删除", (dialog, which) -> {
                int result = relationshipDao.delete(rel.getId());
                if (result > 0) {
                    Toast.makeText(this, "已删除关联", Toast.LENGTH_SHORT).show();
                    if (isEditMode) {
                        displayEditModeRelations();
                    } else {
                        displayRelations();
                    }
                } else {
                    Toast.makeText(this, "删除失败", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }
    
    /**
     * 显示编辑模式的关联设定
     */
    private void displayEditModeRelations() {
        if (currentSetting.getId() == 0) {
            return;
        }
        
        if (storyId == 0) {
            // 全局素材不显示关联设定
            com.google.android.material.card.MaterialCardView cardEditRelations = findViewById(R.id.card_edit_relations);
            cardEditRelations.setVisibility(View.GONE);
            return;
        }
        
        relationsList = relationshipDao.getBySettingId(currentSetting.getId());
        
        LinearLayout layoutEditRelationsContent = findViewById(R.id.layout_edit_relations_content);
        TextView tvEditRelationsCount = findViewById(R.id.tv_edit_relations_count);
        com.google.android.material.card.MaterialCardView cardEditRelations = findViewById(R.id.card_edit_relations);
        
        if (relationsList == null || relationsList.isEmpty()) {
            cardEditRelations.setVisibility(View.VISIBLE);
            layoutEditRelationsContent.removeAllViews();
            tvEditRelationsCount.setText(" (0)");
            
            TextView emptyHint = new TextView(this);
            emptyHint.setText("暂无关联设定");
            emptyHint.setTextSize(14);
            emptyHint.setTextColor(getResources().getColor(android.R.color.darker_gray));
            layoutEditRelationsContent.addView(emptyHint);
        } else {
            cardEditRelations.setVisibility(View.VISIBLE);
            layoutEditRelationsContent.removeAllViews();
            tvEditRelationsCount.setText(" (" + relationsList.size() + ")");
            
            for (SettingRelationship rel : relationsList) {
                addEditModeRelationItemView(layoutEditRelationsContent, rel);
            }
        }
    }
}
