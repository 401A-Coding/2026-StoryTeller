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

        // 字数统计
        etDescription.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tvDescriptionCount.setText(s.length() + "/500");
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
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
        } else {
            Toast.makeText(requireContext(), "未找到作品", Toast.LENGTH_SHORT).show();
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
     * 公开方法：刷新视图（用于切换小说后强制刷新）
     */
    public void refreshView() {
        if (storyId > 0) {
            loadStoryData();
        }
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
