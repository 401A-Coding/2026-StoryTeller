package com.example.storyteller.ui.fragment;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.storyteller.R;
import com.example.storyteller.base.BaseFragment;
import com.example.storyteller.data.local.db.StoryDao;
import com.example.storyteller.model.Story;

/**
 * 小说架构编辑Fragment
 * 用于编辑作品的基本信息和描述信息
 */
public class ArchitectureFragment extends BaseFragment {

    private static final String ARG_STORY_ID = "arg_story_id";

    private EditText etTitle;
    private EditText etDescription;
    private EditText etOutline;
    private EditText etSummary;
    private Spinner spinnerGenre;
    private TextView tvDescriptionCount;
    private Button btnSave;

    private StoryDao storyDao;
    private Story currentStory;
    private int storyId;

    // 类型选项
    private static final String[] GENRE_OPTIONS = {
        "创作", "科幻", "奇幻", "悬疑", "言情", "历史", "武侠", "都市", "其他"
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
        etDescription = view.findViewById(R.id.et_architecture_description);
        etOutline = view.findViewById(R.id.et_architecture_outline);
        etSummary = view.findViewById(R.id.et_architecture_summary);
        spinnerGenre = view.findViewById(R.id.spinner_genre);
        tvDescriptionCount = view.findViewById(R.id.tv_description_count);
        btnSave = view.findViewById(R.id.btn_save_architecture);

        // 设置类型下拉框
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            GENRE_OPTIONS
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGenre.setAdapter(adapter);

        // 简介字数统计
        etDescription.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                int length = s.toString().length();
                tvDescriptionCount.setText(length + "/500");
                if (length > 500) {
                    tvDescriptionCount.setTextColor(getResources().getColor(android.R.color.holo_red_light));
                } else {
                    tvDescriptionCount.setTextColor(getResources().getColor(android.R.color.darker_gray));
                }
            }
        });

        // 保存按钮
        btnSave.setOnClickListener(v -> saveChanges());
    }

    @Override
    protected void initData() {
        storyDao = new StoryDao(requireContext());

        if (getArguments() != null) {
            storyId = getArguments().getInt(ARG_STORY_ID, -1);
        }

        if (storyId > 0) {
            loadStoryData();
        } else {
            Toast.makeText(requireContext(), "未找到作品信息", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 加载作品数据
     */
    private void loadStoryData() {
        currentStory = storyDao.getStoryById(storyId);
        if (currentStory == null) {
            Toast.makeText(requireContext(), "加载作品失败", Toast.LENGTH_SHORT).show();
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
        }

        // 设置简介
        String description = currentStory.getDescription();
        if (!TextUtils.isEmpty(description)) {
            etDescription.setText(description);
            tvDescriptionCount.setText(description.length() + "/500");
        }

        // 注意：大纲和总结目前存储在NovelSummary中，这里暂时留空
        // 后续可以扩展Story模型或创建关联表来存储这些信息
    }

    /**
     * 保存修改
     */
    private void saveChanges() {
        if (currentStory == null) {
            Toast.makeText(requireContext(), "作品数据不存在", Toast.LENGTH_SHORT).show();
            return;
        }

        // 验证标题
        String title = etTitle.getText().toString().trim();
        if (TextUtils.isEmpty(title)) {
            Toast.makeText(requireContext(), "请输入作品标题", Toast.LENGTH_SHORT).show();
            return;
        }

        // 验证简介长度
        String description = etDescription.getText().toString().trim();
        if (description.length() > 500) {
            Toast.makeText(requireContext(), "简介不能超过500字", Toast.LENGTH_SHORT).show();
            return;
        }

        // 更新数据
        currentStory.setTitle(title);
        currentStory.setGenre((String) spinnerGenre.getSelectedItem());
        currentStory.setDescription(description);

        // 保存到数据库
        int result = storyDao.updateStory(currentStory);
        if (result > 0) {
            Toast.makeText(requireContext(), "保存成功", Toast.LENGTH_SHORT).show();
            
            // 通知Activity数据已更新（如果需要刷新其他部分）
            if (getActivity() instanceof OnArchitectureChangedListener) {
                ((OnArchitectureChangedListener) getActivity()).onArchitectureChanged(currentStory);
            }
        } else {
            Toast.makeText(requireContext(), "保存失败", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 架构变化监听器接口
     */
    public interface OnArchitectureChangedListener {
        void onArchitectureChanged(Story story);
    }
}
