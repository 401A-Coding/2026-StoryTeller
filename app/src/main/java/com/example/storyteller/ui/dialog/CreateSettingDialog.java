package com.example.storyteller.ui.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.storyteller.R;
import com.example.storyteller.data.local.db.StorySettingDao;
import com.example.storyteller.model.StorySetting;
import com.example.storyteller.utils.SettingCategoryConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * 创建/编辑设定的对话框
 */
public class CreateSettingDialog extends DialogFragment {

    public interface OnSettingCreatedListener {
        void onSettingCreated(StorySetting setting);
    }

    // 使用统一配置类
    private static final String[] CATEGORIES = SettingCategoryConfig.getAllMainCategories();

    private EditText etTitle;
    private EditText etSummary;
    private EditText etDetail;
    private EditText etTags;      // 标签输入
    private EditText etAliases;   // 别名输入
    private Spinner spCategory;
    private Spinner spSubCategory;
    private Button btnCancel;
    private Button btnConfirm;

    private StorySettingDao settingDao;
    private int storyId = 0;  // 所属小说ID
    private OnSettingCreatedListener listener;

    public static CreateSettingDialog newInstance(int storyId) {
        CreateSettingDialog dialog = new CreateSettingDialog();
        Bundle args = new Bundle();
        args.putInt("story_id", storyId);
        dialog.setArguments(args);
        return dialog;
    }

    public void setOnSettingCreatedListener(OnSettingCreatedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            storyId = getArguments().getInt("story_id", 0);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_create_setting, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 初始化视图
        etTitle = view.findViewById(R.id.et_setting_title);
        etSummary = view.findViewById(R.id.et_setting_summary);
        etDetail = view.findViewById(R.id.et_setting_detail);
        etTags = view.findViewById(R.id.et_setting_tags);
        etAliases = view.findViewById(R.id.et_setting_aliases);
        spCategory = view.findViewById(R.id.sp_category);
        spSubCategory = view.findViewById(R.id.sp_sub_category);
        btnCancel = view.findViewById(R.id.btn_cancel);
        btnConfirm = view.findViewById(R.id.btn_confirm);

        // 初始化DAO
        settingDao = new StorySettingDao(requireContext());

        // 设置分类Spinner
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            CATEGORIES
        );
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCategory.setAdapter(categoryAdapter);

        // 设置子分类Spinner（初始显示第一个分类的子分类）
        updateSubCategories(0);

        // 分类选择变化时更新子分类
        spCategory.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                updateSubCategories(position);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        // 取消按钮
        btnCancel.setOnClickListener(v -> dismiss());

        // 确认按钮
        btnConfirm.setOnClickListener(v -> {
            if (validateAndSave()) {
                dismiss();
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        // 设置对话框宽度为屏幕的95%
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(
                (int) (getResources().getDisplayMetrics().widthPixels * 0.95),
                ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
    }

    /**
     * 更新子分类列表
     */
    private void updateSubCategories(int categoryIndex) {
        String mainCategory = CATEGORIES[categoryIndex];
        String[] subCategories = SettingCategoryConfig.getSubCategories(mainCategory);
        
        ArrayAdapter<String> subCategoryAdapter = new ArrayAdapter<>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            subCategories
        );
        subCategoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spSubCategory.setAdapter(subCategoryAdapter);
    }

    /**
     * 验证并保存
     */
    private boolean validateAndSave() {
        String title = etTitle.getText() == null ? "" : etTitle.getText().toString().trim();
        String summary = etSummary.getText() == null ? "" : etSummary.getText().toString().trim();
        String detail = etDetail.getText() == null ? "" : etDetail.getText().toString().trim();
        String tagsInput = etTags.getText() == null ? "" : etTags.getText().toString().trim();
        String aliasesInput = etAliases.getText() == null ? "" : etAliases.getText().toString().trim();

        // 验证必填字段
        if (TextUtils.isEmpty(title)) {
            etTitle.setError("请输入标题");
            etTitle.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(summary)) {
            etSummary.setError("请输入摘要");
            etSummary.requestFocus();
            return false;
        }

        // 获取分类
        String category = CATEGORIES[spCategory.getSelectedItemPosition()];
        String subCategory = spSubCategory.getSelectedItem() != null ? 
            spSubCategory.getSelectedItem().toString() : "";

        // 创建设定对象
        StorySetting setting = new StorySetting(storyId, category, subCategory, title);
        setting.setSummary(summary);
        setting.setDetail(detail);
        
        // 处理标签（转换为JSON数组）
        if (!TextUtils.isEmpty(tagsInput)) {
            String[] tagsArray = tagsInput.split(",");
            List<String> tagsList = new ArrayList<>();
            for (String tag : tagsArray) {
                String trimmed = tag.trim();
                if (!trimmed.isEmpty()) {
                    tagsList.add(trimmed);
                }
            }
            if (!tagsList.isEmpty()) {
                setting.setTags(new com.google.gson.Gson().toJson(tagsList));
            }
        }
        
        // 处理别名（转换为JSON数组）
        if (!TextUtils.isEmpty(aliasesInput)) {
            String[] aliasesArray = aliasesInput.split(",");
            List<String> aliasesList = new ArrayList<>();
            for (String alias : aliasesArray) {
                String trimmed = alias.trim();
                if (!trimmed.isEmpty()) {
                    aliasesList.add(trimmed);
                }
            }
            if (!aliasesList.isEmpty()) {
                setting.setAliases(new com.google.gson.Gson().toJson(aliasesList));
            }
        }
        
        setting.setCreateTime(System.currentTimeMillis());
        setting.setUpdateTime(System.currentTimeMillis());

        // 保存到数据库
        long id = settingDao.insert(setting);
        if (id > 0) {
            Toast.makeText(requireContext(), "设定创建成功", Toast.LENGTH_SHORT).show();
            
            // 通知监听器
            if (listener != null) {
                setting.setId((int) id);
                listener.onSettingCreated(setting);
            }
            return true;
        } else {
            Toast.makeText(requireContext(), "保存失败，请重试", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return super.onCreateDialog(savedInstanceState);
    }
}
