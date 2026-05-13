package com.example.storyteller.ui.fragment;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.storyteller.R;
import com.example.storyteller.base.BaseFragment;
import com.example.storyteller.data.local.db.StoryDao;
import com.example.storyteller.model.Story;

/**
 * 架构Fragment - 管理作品的基本信息
 */
public class ArchitectureFragment extends BaseFragment {

    private static final String ARG_STORY_ID = "arg_story_id";

    // UI组件
    private EditText etTitle;
    private Spinner spinnerGenre;
    private EditText etDescription;
    private TextView tvDescriptionCount;

    // 数据
    private Story currentStory;
    private int storyId;
    private StoryDao storyDao;
    
    // 标记是否正在加载数据，避免加载时触发自动保存
    private boolean isLoadingData = false;
    
    // 架构变化监听器
    private OnArchitectureChangedListener listener;

    // 类型选项
    private static final String[] GENRE_OPTIONS = {
        "玄幻", "奇幻", "武侠", "仙侠", "都市", "历史", "科幻", "悬疑", "恐怖", "言情", "其他"
    };

    public static ArchitectureFragment newInstance(int storyId) {
        ArchitectureFragment fragment = new ArchitectureFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_STORY_ID, storyId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_architecture;
    }

    @Override
    protected void initView(View view) {
        etTitle = view.findViewById(R.id.et_architecture_title);
        spinnerGenre = view.findViewById(R.id.spinner_genre);
        etDescription = view.findViewById(R.id.et_architecture_description);
        tvDescriptionCount = view.findViewById(R.id.tv_description_count);

        // 设置类型下拉框
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            GENRE_OPTIONS
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGenre.setAdapter(adapter);

        // 字数统计（不触发自动保存）
        etDescription.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tvDescriptionCount.setText(s.length() + "/500");
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
                // 简介变化时自动保存（加载数据时不触发）
                if (!isLoadingData) {
                    autoSaveChanges();
                }
            }
        });

        // 标题变化时自动保存（加载数据时不触发）
        etTitle.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(android.text.Editable s) {
                if (!isLoadingData) {
                    autoSaveChanges();
                }
            }
        });

        // 类型变化时自动保存（加载数据时不触发）
        spinnerGenre.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!isLoadingData) {
                    autoSaveChanges();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    @Override
    protected void initData() {
        storyDao = new StoryDao(requireContext());

        if (getArguments() != null) {
            storyId = getArguments().getInt(ARG_STORY_ID, -1);
        }

        if (storyId > 0) {
            loadStoryData();
            
            // 加载完成后，自动设置监听器
            if (getActivity() instanceof OnArchitectureChangedListener) {
                listener = (OnArchitectureChangedListener) getActivity();
            }
        } else {
            Toast.makeText(requireContext(), "未找到作品", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 加载作品数据
     */
    private void loadStoryData() {
        // 标记正在加载，避免触发自动保存
        isLoadingData = true;
        
        currentStory = storyDao.getStoryById(storyId);
        if (currentStory == null) {
            Toast.makeText(requireContext(), "加载作品失败", Toast.LENGTH_SHORT).show();
            isLoadingData = false;
            return;
        }

        // 填充数据
        etTitle.setText(currentStory.getTitle());
        
        // 设置类型
        String genre = currentStory.getGenre();
        if (!TextUtils.isEmpty(genre)) {
            for (int i = 0; i < GENRE_OPTIONS.length; i++) {
                if (GENRE_OPTIONS[i].equals(genre)) {
                    spinnerGenre.setSelection(i);
                    break;
                }
            }
        } else {
            // 如果没有类型，默认选择第一个
            spinnerGenre.setSelection(0);
        }

        // 设置简介
        String description = currentStory.getDescription();
        if (description != null) {
            etDescription.setText(description);
            tvDescriptionCount.setText(description.length() + "/500");
        } else {
            etDescription.setText("");
            tvDescriptionCount.setText("0/500");
        }

        // 标记加载完成
        isLoadingData = false;

        // 注意：大纲和总结目前存储在NovelSummary中，这里暂时留空
        // 后续可以扩展Story模型或创建关联表来存储这些信息
    }
    
    /**
     * 公开方法：刷新视图（用于切换小说后强制刷新）
     */
    public void refreshView() {
        if (storyId > 0) {
            loadStoryData();
        }
    }

    /**
     * 设置架构变化监听器
     */
    public void setOnArchitectureChangedListener(OnArchitectureChangedListener listener) {
        this.listener = listener;
    }

    /**
     * 自动保存修改（静默保存，不显示Toast）
     */
    private void autoSaveChanges() {
        if (currentStory == null || isLoadingData) {
            return;
        }

        // 获取当前输入的值
        String title = etTitle.getText().toString().trim();
        String description = etDescription.getText().toString();
        String newGenre = (String) spinnerGenre.getSelectedItem();

        // 验证标题（标题不能为空）
        if (TextUtils.isEmpty(title)) {
            return;
        }

        // 验证简介长度
        if (description.length() > 500) {
            return;
        }

        // 检查是否有变化
        String currentTitle = currentStory.getTitle();
        String currentGenre = currentStory.getGenre();
        String currentDescription = currentStory.getDescription();

        // 如果没有任何变化，不需要保存
        boolean titleChanged = !title.equals(currentTitle);
        boolean genreChanged = !(newGenre != null && newGenre.equals(currentGenre));
        boolean descriptionChanged = !(description != null ? description.equals(currentDescription) : currentDescription == null);
        
        if (!titleChanged && !genreChanged && !descriptionChanged) {
            return;
        }

        // 更新数据
        currentStory.setTitle(title);
        currentStory.setGenre(newGenre);
        currentStory.setDescription(description);

        // 保存到数据库（静默保存）
        int result = storyDao.updateStory(currentStory);
        
        // 通知监听器数据已更新
        if (listener != null) {
            listener.onArchitectureChanged(currentStory);
        }
    }

    /**
     * 手动保存修改（显示Toast提示）
     */
    public void saveChanges() {
        saveChangesInternal(true);
    }

    /**
     * 静默保存修改（不显示Toast）
     */
    public void saveChangesSilently() {
        saveChangesInternal(false);
    }

    /**
     * 内部保存方法
     * @param showToast 是否显示Toast提示
     */
    private void saveChangesInternal(boolean showToast) {
        if (currentStory == null) {
            if (showToast) {
                Toast.makeText(requireContext(), "作品数据不存在", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        // 验证标题
        String title = etTitle.getText().toString().trim();
        if (TextUtils.isEmpty(title)) {
            if (showToast) {
                Toast.makeText(requireContext(), "请输入作品标题", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        // 获取简介（不要trim，保留原始内容）
        String description = etDescription.getText().toString();
        
        // 验证简介长度
        if (description.length() > 500) {
            if (showToast) {
                Toast.makeText(requireContext(), "简介不能超过500字", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        // 更新数据
        currentStory.setTitle(title);
        currentStory.setGenre((String) spinnerGenre.getSelectedItem());
        currentStory.setDescription(description);

        // 保存到数据库
        int result = storyDao.updateStory(currentStory);
        
        if (showToast) {
            if (result > 0) {
                Toast.makeText(requireContext(), "保存成功", Toast.LENGTH_SHORT).show();
                
                // 通知监听器数据已更新
                if (listener != null) {
                    listener.onArchitectureChanged(currentStory);
                }
            } else {
                Toast.makeText(requireContext(), "保存失败", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * 架构变化监听器接口
     */
    public interface OnArchitectureChangedListener {
        void onArchitectureChanged(Story story);
    }
}
