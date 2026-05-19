package com.example.storyteller.ui.dialog;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.example.storyteller.R;
import com.example.storyteller.model.Chapter;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.slider.Slider;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

/**
 * 章纲编辑Dialog（全屏）
 */
public class ChapterOutlineEditDialog extends DialogFragment {

    private static final String ARG_VOLUME_INDEX = "arg_volume_index";
    private static final String ARG_CHAPTER_INDEX = "arg_chapter_index";
    private static final String ARG_CHAPTER_TITLE = "arg_chapter_title";
    private static final String ARG_ROLE = "arg_role";
    private static final String ARG_SUMMARY = "arg_summary";
    private static final String ARG_PURPOSE = "arg_purpose";
    private static final String ARG_SUSPENSE = "arg_suspense";
    private static final String ARG_FORESHADOWING = "arg_foreshadowing";
    private static final String ARG_TWIST = "arg_twist";

    private int volumeIndex;
    private int chapterIndex;
    private OnChapterOutlineSaveListener listener;

    // 拓展信息字段
    private List<String> involvedCharacters = new ArrayList<>();
    private List<String> keyItems = new ArrayList<>();
    private List<String> sceneLocations = new ArrayList<>();
    private String timeConstraint = "";

    // UI Components - Main
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private Button btnSave;
    
    // Core Tab Views
    private TextView tvChapterTitle;
    private EditText etChapterRole;
    private EditText etChapterSummary;
    private EditText etChapterPurpose;
    private Slider sliderSuspense;
    private TextView tvSuspenseValue;
    private EditText etForeshadowing;
    private Slider sliderTwist;
    private TextView tvTwistValue;
    
    // Extended Tab Views
    private ChipGroup chipGroupCharacters;
    private ChipGroup chipGroupItems;
    private ChipGroup chipGroupLocations;
    private Button btnAddCharacter;
    private Button btnAddItem;
    private Button btnAddLocation;
    private EditText etTimeConstraint;

    public interface OnChapterOutlineSaveListener {
        void onChapterOutlineSaved(int volumeIndex, int chapterIndex, Chapter updatedChapter);
    }

    public static ChapterOutlineEditDialog newInstance(int volumeIndex, int chapterIndex, Chapter chapter) {
        ChapterOutlineEditDialog dialog = new ChapterOutlineEditDialog();
        Bundle args = new Bundle();
        args.putInt(ARG_VOLUME_INDEX, volumeIndex);
        args.putInt(ARG_CHAPTER_INDEX, chapterIndex);
        args.putString(ARG_CHAPTER_TITLE, chapter.getTitle());
        args.putString(ARG_ROLE, chapter.getChapterRole());
        args.putString(ARG_SUMMARY, chapter.getChapterSummary());
        args.putString(ARG_PURPOSE, chapter.getChapterPurpose());
        args.putFloat(ARG_SUSPENSE, chapter.getSuspenseLevel());
        args.putString(ARG_FORESHADOWING, chapter.getForeshadowing());
        args.putFloat(ARG_TWIST, chapter.getTwistLevel());
        
        // 拓展信息
        if (chapter.getInvolvedCharacters() != null) {
            args.putStringArrayList("involved_characters", new ArrayList<>(chapter.getInvolvedCharacters()));
        }
        if (chapter.getKeyItems() != null) {
            args.putStringArrayList("key_items", new ArrayList<>(chapter.getKeyItems()));
        }
        if (chapter.getSceneLocations() != null) {
            args.putStringArrayList("scene_locations", new ArrayList<>(chapter.getSceneLocations()));
        }
        args.putString("time_constraint", chapter.getTimeConstraint());
        
        dialog.setArguments(args);
        return dialog;
    }

    public void setOnSaveListener(OnChapterOutlineSaveListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_chapter_outline, container, false);
        
        // 初始化UI组件
        tabLayout = view.findViewById(R.id.tab_chapter_outline);
        viewPager = view.findViewById(R.id.view_pager_chapter);
        btnSave = view.findViewById(R.id.btn_save_chapter);
        
        // 设置ViewPager2适配器
        setupViewPager();
        
        // 设置保存按钮
        if (btnSave != null) {
            btnSave.setOnClickListener(v -> saveAndDismiss());
        }
        
        return view;
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 设置为全屏样式
        setStyle(STYLE_NORMAL, R.style.FullScreenDialog);
    }

    private void setupViewPager() {
        FragmentStateAdapter adapter = new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                if (position == 0) {
                    return CoreInfoFragment.newInstance();
                } else {
                    return ExtendedInfoFragment.newInstance();
                }
            }

            @Override
            public int getItemCount() {
                return 2;
            }
        };
        
        viewPager.setAdapter(adapter);
        
        // 设置预加载策略：保持两个Fragment都存活，避免切换时重建
        viewPager.setOffscreenPageLimit(1);
        
        // 关联TabLayout和ViewPager2
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            if (position == 0) {
                tab.setText("核心信息");
            } else {
                tab.setText("拓展信息");
            }
        }).attach();
    }

    /**
     * 核心信息Fragment
     */
    public static class CoreInfoFragment extends Fragment {
        private ChapterOutlineEditDialog parentDialog;
        
        public static CoreInfoFragment newInstance() {
            return new CoreInfoFragment();
        }
        
        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_chapter_outline_core, container, false);
            parentDialog = (ChapterOutlineEditDialog) requireParentFragment();
            parentDialog.initCoreInfoViews(view);
            
            // 视图创建完成后加载数据
            parentDialog.loadData();
            
            return view;
        }
    }
    
    /**
     * 拓展信息Fragment
     */
    public static class ExtendedInfoFragment extends Fragment {
        private ChapterOutlineEditDialog parentDialog;
        
        public static ExtendedInfoFragment newInstance() {
            return new ExtendedInfoFragment();
        }
        
        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_chapter_outline_extended, container, false);
            parentDialog = (ChapterOutlineEditDialog) requireParentFragment();
            parentDialog.initExtendedInfoViews(view);
            
            // 视图创建完成后加载拓展信息
            parentDialog.loadExtendedData();
            
            return view;
        }
    }
    
    private void initCoreInfoViews(View view) {
        tvChapterTitle = view.findViewById(R.id.tv_chapter_title);
        etChapterRole = view.findViewById(R.id.et_chapter_role);
        etChapterSummary = view.findViewById(R.id.et_chapter_summary);
        etChapterPurpose = view.findViewById(R.id.et_chapter_purpose);
        sliderSuspense = view.findViewById(R.id.slider_suspense);
        tvSuspenseValue = view.findViewById(R.id.tv_suspense_value);
        etForeshadowing = view.findViewById(R.id.et_foreshadowing);
        sliderTwist = view.findViewById(R.id.slider_twist);
        tvTwistValue = view.findViewById(R.id.tv_twist_value);
        
        // 防止EditText的滚动事件冒泡到父容器（ScrollView）
        setupEditTextScrollListener(etChapterRole);
        setupEditTextScrollListener(etChapterSummary);
        setupEditTextScrollListener(etChapterPurpose);
        setupEditTextScrollListener(etForeshadowing);
        
        // Slider监听
        if (sliderSuspense != null) {
            sliderSuspense.addOnChangeListener((slider, value, fromUser) -> {
                updateSuspenseDisplay(value);
            });
        }
        
        if (sliderTwist != null) {
            sliderTwist.addOnChangeListener((slider, value, fromUser) -> {
                updateTwistDisplay(value);
            });
        }
    }
    
    private void initExtendedInfoViews(View view) {
        chipGroupCharacters = view.findViewById(R.id.chip_group_characters);
        chipGroupItems = view.findViewById(R.id.chip_group_items);
        chipGroupLocations = view.findViewById(R.id.chip_group_locations);
        btnAddCharacter = view.findViewById(R.id.btn_add_character);
        btnAddItem = view.findViewById(R.id.btn_add_item);
        btnAddLocation = view.findViewById(R.id.btn_add_location);
        etTimeConstraint = view.findViewById(R.id.et_time_constraint);
        
        // 设置添加按钮点击事件
        if (btnAddCharacter != null) {
            btnAddCharacter.setOnClickListener(v -> showAddChipDialog("添加角色", chipGroupCharacters));
        }
        if (btnAddItem != null) {
            btnAddItem.setOnClickListener(v -> showAddChipDialog("添加物品", chipGroupItems));
        }
        if (btnAddLocation != null) {
            btnAddLocation.setOnClickListener(v -> showAddChipDialog("添加位置", chipGroupLocations));
        }
    }

    private void loadData() {
        if (getArguments() != null) {
            volumeIndex = getArguments().getInt(ARG_VOLUME_INDEX);
            chapterIndex = getArguments().getInt(ARG_CHAPTER_INDEX);
            String title = getArguments().getString(ARG_CHAPTER_TITLE, "");
            String role = getArguments().getString(ARG_ROLE, "");
            String summary = getArguments().getString(ARG_SUMMARY, "");
            String purpose = getArguments().getString(ARG_PURPOSE, "");
            float suspense = getArguments().getFloat(ARG_SUSPENSE, 0f);
            String foreshadowing = getArguments().getString(ARG_FORESHADOWING, "");
            float twist = getArguments().getFloat(ARG_TWIST, 0f);
            
            // 拓展信息
            involvedCharacters = getArguments().getStringArrayList("involved_characters");
            if (involvedCharacters == null) involvedCharacters = new ArrayList<>();
            
            keyItems = getArguments().getStringArrayList("key_items");
            if (keyItems == null) keyItems = new ArrayList<>();
            
            sceneLocations = getArguments().getStringArrayList("scene_locations");
            if (sceneLocations == null) sceneLocations = new ArrayList<>();
            
            timeConstraint = getArguments().getString("time_constraint", "");

            // 填充核心信息（如果视图已创建）
            if (tvChapterTitle != null) tvChapterTitle.setText(title);
            if (etChapterRole != null) etChapterRole.setText(role);
            if (etChapterSummary != null) etChapterSummary.setText(summary);
            if (etChapterPurpose != null) etChapterPurpose.setText(purpose);
            if (sliderSuspense != null) sliderSuspense.setValue(roundToStep(suspense, 0.5f));
            if (etForeshadowing != null) etForeshadowing.setText(foreshadowing);
            if (sliderTwist != null) sliderTwist.setValue(roundToStep(twist, 0.5f));
            
            updateSuspenseDisplay(suspense);
            updateTwistDisplay(twist);
        }
    }
    
    /**
     * 加载拓展信息数据
     */
    private void loadExtendedData() {
        // 安全检查：确保视图已经初始化
        if (chipGroupCharacters == null || chipGroupItems == null || chipGroupLocations == null) {
            android.util.Log.w("ChapterOutlineEditDialog", "loadExtendedData: 视图未初始化，跳过加载");
            return;
        }
        
        // 填充拓展信息
        chipGroupCharacters.removeAllViews();
        for (String character : involvedCharacters) {
            addChipToGroup(chipGroupCharacters, character);
        }
        
        chipGroupItems.removeAllViews();
        for (String item : keyItems) {
            addChipToGroup(chipGroupItems, item);
        }
        
        chipGroupLocations.removeAllViews();
        for (String location : sceneLocations) {
            addChipToGroup(chipGroupLocations, location);
        }
        
        if (etTimeConstraint != null) {
            etTimeConstraint.setText(timeConstraint);
        }
        
        android.util.Log.d("ChapterOutlineEditDialog", "loadExtendedData: 已加载 " + 
            involvedCharacters.size() + " 个角色, " + 
            keyItems.size() + " 个物品, " + 
            sceneLocations.size() + " 个位置");
    }


    private void updateSuspenseDisplay(float value) {
        String level = getSuspenseLevelText(value);
        tvSuspenseValue.setText(String.format("当前: %.1f/10 (%s)", value, level));
    }

    private void updateTwistDisplay(float value) {
        String level = getTwistLevelText(value);
        tvTwistValue.setText(String.format("当前: %.1f/5 (%s)", value, level));
    }

    private String getSuspenseLevelText(float value) {
        if (value == 0) return "无";
        if (value <= 3) return "低";
        if (value <= 6) return "中";
        return "高";
    }

    private String getTwistLevelText(float value) {
        if (value == 0) return "无";
        if (value <= 2) return "轻微";
        if (value <= 3) return "中等";
        return "重大";
    }
    
    /**
     * 将值四舍五入到最近的步长倍数
     * @param value 原始值
     * @param stepSize 步长
     * @return 对齐后的值
     */
    private float roundToStep(float value, float stepSize) {
        if (stepSize <= 0) return value;
        return Math.round(value / stepSize) * stepSize;
    }
    
    /**
     * 设置EditText滚动监听，防止滚动事件冒泡到父容器
     */
    private void setupEditTextScrollListener(EditText editText) {
        if (editText != null) {
            editText.setOnTouchListener((v, event) -> {
                // 让EditText自己处理滚动事件，阻止事件向上传播
                v.getParent().requestDisallowInterceptTouchEvent(true);
                return false;
            });
        }
    }

    private void saveAndDismiss() {
        if (listener != null) {
            Chapter updatedChapter = new Chapter();
            updatedChapter.setTitle(tvChapterTitle != null ? tvChapterTitle.getText().toString() : "");
            updatedChapter.setChapterRole(etChapterRole != null ? etChapterRole.getText().toString().trim() : "");
            updatedChapter.setChapterSummary(etChapterSummary != null ? etChapterSummary.getText().toString().trim() : "");
            updatedChapter.setChapterPurpose(etChapterPurpose != null ? etChapterPurpose.getText().toString().trim() : "");
            updatedChapter.setSuspenseLevel(sliderSuspense != null ? sliderSuspense.getValue() : 0f);
            updatedChapter.setForeshadowing(etForeshadowing != null ? etForeshadowing.getText().toString().trim() : "");
            updatedChapter.setTwistLevel(sliderTwist != null ? sliderTwist.getValue() : 0f);
            
            // 保存拓展信息（从ChipGroup获取）
            if (chipGroupCharacters != null) {
                updatedChapter.setInvolvedCharacters(getChipsFromGroup(chipGroupCharacters));
            }
            if (chipGroupItems != null) {
                updatedChapter.setKeyItems(getChipsFromGroup(chipGroupItems));
            }
            if (chipGroupLocations != null) {
                updatedChapter.setSceneLocations(getChipsFromGroup(chipGroupLocations));
            }
            if (etTimeConstraint != null) {
                updatedChapter.setTimeConstraint(etTimeConstraint.getText().toString().trim());
            }

            listener.onChapterOutlineSaved(volumeIndex, chapterIndex, updatedChapter);
            Toast.makeText(getContext(), "已保存章纲", Toast.LENGTH_SHORT).show();
        }
        dismiss();
    }
    
    /**
     * 向ChipGroup添加一个Chip
     */
    private void addChipToGroup(ChipGroup chipGroup, String text) {
        if (TextUtils.isEmpty(text)) return;
        
        Chip chip = new Chip(requireContext());
        chip.setText(text);
        chip.setCloseIconVisible(true);
        chip.setClickable(false);  // Chip本身不可点击，只可删除
        chip.setCheckable(false);
        
        // 设置删除监听
        chip.setOnCloseIconClickListener(v -> {
            chipGroup.removeView(chip);
        });
        
        chipGroup.addView(chip);
    }
    
    /**
     * 从ChipGroup获取所有Chip的文本
     */
    private List<String> getChipsFromGroup(ChipGroup chipGroup) {
        List<String> list = new ArrayList<>();
        int childCount = chipGroup.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = chipGroup.getChildAt(i);
            if (child instanceof Chip) {
                String text = ((Chip) child).getText().toString().trim();
                if (!TextUtils.isEmpty(text)) {
                    list.add(text);
                }
            }
        }
        return list;
    }
    
    /**
     * 显示添加Chip的对话框
     */
    private void showAddChipDialog(String title, ChipGroup chipGroup) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(title);
        
        // 创建输入框
        TextInputEditText input = new TextInputEditText(requireContext());
        input.setHint("请输入" + title.replace("添加", ""));
        input.setPadding(50, 30, 50, 30);
        
        builder.setView(input);
        
        builder.setPositiveButton("确定", (dialog, which) -> {
            String text = input.getText().toString().trim();
            if (!TextUtils.isEmpty(text)) {
                addChipToGroup(chipGroup, text);
            }
        });
        
        builder.setNegativeButton("取消", null);
        
        AlertDialog dialog = builder.create();
        dialog.show();
        
        // 自动聚焦并显示键盘
        input.postDelayed(() -> {
            input.requestFocus();
            android.view.inputmethod.InputMethodManager imm = 
                (android.view.inputmethod.InputMethodManager) requireContext()
                    .getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(input, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
            }
        }, 200);
    }
}
