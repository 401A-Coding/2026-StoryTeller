package com.example.storyteller.ui.fragment;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.storyteller.R;
import com.example.storyteller.base.BaseFragment;
import com.example.storyteller.data.repository.StoryRepository;
import com.example.storyteller.data.repository.StoryRepositoryImpl;
import com.example.storyteller.model.Chapter;
import com.example.storyteller.model.Story;
import com.example.storyteller.model.Volume;
import com.example.storyteller.utils.JsonUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 写作Fragment - 卷章编辑器
 * 负责显示和编辑小说的卷章结构
 */
public class WritingFragment extends BaseFragment {

    private static final String ARG_STORY_ID = "arg_story_id";

    // UI Components
    private LinearLayout layoutContent;
    private Button btnAddVolume;
    private TextView tvEmptyHint;

    // Data
    private List<Volume> volumes = new ArrayList<>();
    private Story currentStory;
    private int storyId;
    private StoryRepository storyRepository;
    private int volumeCount = 0;

    public static WritingFragment newInstance(int storyId) {
        WritingFragment fragment = new WritingFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_STORY_ID, storyId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_writing;
    }

    @Override
    protected void initView(View view) {
        layoutContent = view.findViewById(R.id.layout_content);
        btnAddVolume = view.findViewById(R.id.btn_add_volume);
        tvEmptyHint = view.findViewById(R.id.tv_empty_hint);

        // 添加卷按钮
        btnAddVolume.setOnClickListener(v -> addNewVolume());
    }

    @Override
    protected void initData() {
        storyRepository = new StoryRepositoryImpl(requireContext());

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
        currentStory = storyRepository.getStoryById(storyId);
        if (currentStory == null) {
            Toast.makeText(requireContext(), "加载作品失败", Toast.LENGTH_SHORT).show();
            return;
        }

        // 解析卷章结构
        String structureJson = currentStory.getStructure();
        if (!TextUtils.isEmpty(structureJson)) {
            try {
                volumes = JsonUtils.fromJson(structureJson, 
                    new com.google.gson.reflect.TypeToken<List<Volume>>(){}.getType());
            } catch (Exception e) {
                e.printStackTrace();
                volumes = new ArrayList<>();
            }
        }

        if (volumes == null) {
            volumes = new ArrayList<>();
        }

        // 更新卷计数
        if (!volumes.isEmpty()) {
            volumeCount = volumes.size();
        }

        // 渲染卷章
        renderVolumes();
    }

    /**
     * 渲染所有卷
     */
    private void renderVolumes() {
        // 清除除btn_add_volume之外的所有视图
        List<View> viewsToRemove = new ArrayList<>();
        for (int i = 0; i < layoutContent.getChildCount(); i++) {
            View child = layoutContent.getChildAt(i);
            if (child.getId() != R.id.btn_add_volume && child.getId() != R.id.tv_empty_hint) {
                viewsToRemove.add(child);
            }
        }
        for (View view : viewsToRemove) {
            layoutContent.removeView(view);
        }

        if (volumes.isEmpty()) {
            tvEmptyHint.setVisibility(View.VISIBLE);
        } else {
            tvEmptyHint.setVisibility(View.GONE);
            
            // 渲染每个卷（在btn_add_volume之前插入）
            int buttonIndex = -1;
            for (int i = 0; i < layoutContent.getChildCount(); i++) {
                if (layoutContent.getChildAt(i).getId() == R.id.btn_add_volume) {
                    buttonIndex = i;
                    break;
                }
            }
            
            for (int i = 0; i < volumes.size(); i++) {
                Volume volume = volumes.get(i);
                View volumeView = createVolumeView(volume, i);
                if (buttonIndex >= 0) {
                    layoutContent.addView(volumeView, buttonIndex + i);
                } else {
                    layoutContent.addView(volumeView);
                }
            }
        }
    }

    /**
     * 创建卷视图
     */
    private View createVolumeView(Volume volume, int volumeIndex) {
        View volumeView = LayoutInflater.from(requireContext())
            .inflate(R.layout.item_volume, layoutContent, false);

        // 设置卷标题
        TextView tvVolumePrefix = volumeView.findViewById(R.id.tv_volume_prefix);
        TextView tvVolumeName = volumeView.findViewById(R.id.tv_volume_name);
        EditText etVolumeName = volumeView.findViewById(R.id.et_volume_name);

        tvVolumePrefix.setText("第" + (volumeIndex + 1) + "卷 · ");
        tvVolumeName.setText(volume.getTitle());
        etVolumeName.setText(volume.getTitle());

        // 双击编辑卷名
        setupInlineEdit(tvVolumeName, etVolumeName, volume, false);

        // 章节容器
        LinearLayout layoutChapters = volumeView.findViewById(R.id.layout_chapters_container);

        // 添加章节按钮
        Button btnAddChapter = volumeView.findViewById(R.id.btn_add_chapter);
        btnAddChapter.setOnClickListener(v -> addNewChapter(layoutChapters, volume));

        // 更多操作按钮
        ImageView btnMoreVolume = volumeView.findViewById(R.id.btn_more_volume);
        btnMoreVolume.setOnClickListener(v -> showVolumeMenu(volume, volumeIndex, volumeView));

        // 渲染所有章节
        for (int i = 0; i < volume.getChapters().size(); i++) {
            Chapter chapter = volume.getChapters().get(i);
            View chapterView = createChapterView(chapter, volume, i + 1);
            layoutChapters.addView(chapterView);
        }

        return volumeView;
    }

    /**
     * 创建章节视图
     */
    private View createChapterView(Chapter chapter, Volume volume, int chapterIndex) {
        View chapterView = LayoutInflater.from(requireContext())
            .inflate(R.layout.item_chapter, null, false);

        // 设置章节前缀
        TextView tvChapterPrefix = chapterView.findViewById(R.id.tv_chapter_prefix);
        tvChapterPrefix.setText("第" + chapterIndex + "章 · ");

        // 设置章节名称
        TextView tvChapterName = chapterView.findViewById(R.id.tv_chapter_name);
        EditText etChapterName = chapterView.findViewById(R.id.et_chapter_name);
        tvChapterName.setText(chapter.getTitle());
        etChapterName.setText(chapter.getTitle());

        // 双击编辑章节名
        setupInlineEdit(tvChapterName, etChapterName, chapter, true);

        // 更多内容按钮
        ImageView btnMoreChapter = chapterView.findViewById(R.id.btn_more_chapter);
        btnMoreChapter.setOnClickListener(v -> showChapterMenu(chapter, volume, chapterView));

        // 内容编辑器
        EditText etContent = chapterView.findViewById(R.id.et_chapter_content);
        etContent.setHint("开始写作...");
        if (!TextUtils.isEmpty(chapter.getContent())) {
            etContent.setText(chapter.getContent());
        }
        
        // 监听内容变化
        etContent.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(android.text.Editable s) {
                chapter.setContent(s.toString());
            }
        });

        return chapterView;
    }

    /**
     * 设置内联编辑（单击切换编辑模式）
     */
    private void setupInlineEdit(TextView textView, EditText editText, Object model, boolean isChapter) {
        // 单击 TextView 切换到编辑模式
        textView.setOnClickListener(v -> {
            textView.setVisibility(View.GONE);
            editText.setVisibility(View.VISIBLE);
            editText.requestFocus();
            // 选中全部文本
            editText.setSelection(editText.getText().length());
            // 显示软键盘
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) requireActivity().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(editText, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
            }
        });

        // EditText 失去焦点时切换回显示模式
        editText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                finishEditing(editText, textView, model, isChapter);
            }
        });

        // 按回车键完成编辑
        editText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE ||
                actionId == android.view.inputmethod.EditorInfo.IME_NULL) {
                finishEditing(editText, textView, model, isChapter);
                return true;
            }
            return false;
        });
    }

    /**
     * 完成编辑，切换回显示模式
     */
    private void finishEditing(EditText editText, TextView textView, Object model, boolean isChapter) {
        String newName = editText.getText().toString().trim();
        if (TextUtils.isEmpty(newName)) {
            newName = isChapter ? "新章节" : "新卷名";
        }

        // 更新模型
        if (model instanceof Volume) {
            ((Volume) model).setTitle(newName);
        } else if (model instanceof Chapter) {
            ((Chapter) model).setTitle(newName);
        }

        // 切换回显示模式
        editText.setVisibility(View.GONE);
        textView.setText(newName);
        textView.setVisibility(View.VISIBLE);

        // 隐藏软键盘
        android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) requireActivity().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
        }
        
        // 保存修改
        saveStructure();
    }

    /**
     * 添加新卷
     */
    private void addNewVolume() {
        volumeCount++;
        Volume newVolume = new Volume(volumeCount, "新卷名");
        volumes.add(newVolume);
        renderVolumes();
        
        // 保存
        saveStructure();
        Toast.makeText(requireContext(), "已添加新卷", Toast.LENGTH_SHORT).show();
    }
    
    /**
     * 在指定位置添加新卷
     * @param insertIndex 插入位置（从 0 开始）
     */
    private void addNewVolumeAtPosition(int insertIndex) {
        // 验证插入位置
        if (insertIndex < 0 || insertIndex > volumes.size()) {
            return;
        }
        
        volumeCount++;
        Volume newVolume = new Volume(volumeCount, "新卷名");
        volumes.add(insertIndex, newVolume);
        
        // 重新编号后续卷
        for (int i = insertIndex; i < volumes.size(); i++) {
            volumes.get(i).setId(i + 1);
        }
        
        // 保存并刷新UI
        saveStructure();
        renderVolumes();
        
        Toast.makeText(requireContext(), "已在第 " + (insertIndex + 1) + " 个位置添加新卷", Toast.LENGTH_SHORT).show();
    }

    /**
     * 添加新章节
     */
    private void addNewChapter(LinearLayout chapterContainer, Volume volume) {
        int newChapterId = volume.getChapters().size() + 1;
        Chapter newChapter = new Chapter(newChapterId, "新章节", "");
        volume.getChapters().add(newChapter);

        View chapterView = createChapterView(newChapter, volume, newChapterId);
        chapterContainer.addView(chapterView);

        // 保存并刷新
        saveStructure();
        Toast.makeText(requireContext(), "已添加新章节", Toast.LENGTH_SHORT).show();
    }
    
    /**
     * 在指定位置添加新章节
     * @param volume 所属卷
     * @param insertIndex 插入位置（从 0 开始）
     * @param showToast 是否显示提示
     */
    private void addNewChapterAtPosition(Volume volume, int insertIndex, boolean showToast) {
        // 验证插入位置
        if (insertIndex < 0 || insertIndex > volume.getChapters().size()) {
            return;
        }
        
        // 创建新章节
        Chapter newChapter = new Chapter(insertIndex + 1, "新章节", "");
        volume.getChapters().add(insertIndex, newChapter);
        
        // 重新编号后续章节
        for (int i = insertIndex; i < volume.getChapters().size(); i++) {
            volume.getChapters().get(i).setId(i + 1);
        }
        
        // 保存并刷新UI
        saveStructure();
        renderVolumes();
        
        // 只在用户主动操作时显示提示
        if (showToast) {
            Toast.makeText(requireContext(), "已在第 " + (insertIndex + 1) + " 个位置添加新章节", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 显示卷菜单
     */
    private void showVolumeMenu(Volume volume, int volumeIndex, View volumeView) {
        android.widget.PopupMenu popupMenu = new android.widget.PopupMenu(requireContext(), volumeView);
        popupMenu.getMenu().add("重命名");
        popupMenu.getMenu().add("在上方添加卷");
        popupMenu.getMenu().add("在下方添加卷");
        popupMenu.getMenu().add("删除卷");

        popupMenu.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();
            if ("重命名".equals(title)) {
                // 触发编辑模式
                TextView tvVolumeName = volumeView.findViewById(R.id.tv_volume_name);
                EditText etVolumeName = volumeView.findViewById(R.id.et_volume_name);
                if (tvVolumeName != null && etVolumeName != null) {
                    tvVolumeName.performClick();
                }
                return true;
            } else if ("在上方添加卷".equals(title)) {
                // 在当前卷上方添加
                addNewVolumeAtPosition(volumeIndex);
                return true;
            } else if ("在下方添加卷".equals(title)) {
                // 在当前卷下方添加
                addNewVolumeAtPosition(volumeIndex + 1);
                return true;
            } else if ("删除卷".equals(title)) {
                deleteVolume(volume, volumeIndex);
                return true;
            }
            return false;
        });

        popupMenu.show();
    }

    /**
     * 显示章节菜单
     */
    private void showChapterMenu(Chapter chapter, Volume volume, View chapterView) {
        android.widget.PopupMenu popupMenu = new android.widget.PopupMenu(requireContext(), chapterView);
        popupMenu.getMenu().add("重命名");
        popupMenu.getMenu().add("在上方添加章节");
        popupMenu.getMenu().add("在下方添加章节");
        popupMenu.getMenu().add("删除章节");

        popupMenu.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();
            if ("重命名".equals(title)) {
                // 触发编辑模式
                TextView tvChapterName = chapterView.findViewById(R.id.tv_chapter_name);
                EditText etChapterName = chapterView.findViewById(R.id.et_chapter_name);
                if (tvChapterName != null && etChapterName != null) {
                    tvChapterName.performClick();
                }
                return true;
            } else if ("在上方添加章节".equals(title)) {
                // 在当前章节上方添加
                int chapterIndex = volume.getChapters().indexOf(chapter);
                addNewChapterAtPosition(volume, chapterIndex, true);
                return true;
            } else if ("在下方添加章节".equals(title)) {
                // 在当前章节下方添加
                int chapterIndex = volume.getChapters().indexOf(chapter);
                addNewChapterAtPosition(volume, chapterIndex + 1, true);
                return true;
            } else if ("删除章节".equals(title)) {
                // 确认删除
                int chapterIndex = volume.getChapters().indexOf(chapter) + 1;
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("确认删除")
                    .setMessage("确定要删除第" + chapterIndex + "章《" + chapter.getTitle() + "》吗？")
                    .setPositiveButton("删除", (dialog, which) -> {
                        deleteChapter(volume, chapter);
                    })
                    .setNegativeButton("取消", null)
                    .show();
                return true;
            }
            return false;
        });

        popupMenu.show();
    }

    /**
     * 删除卷
     */
    private void deleteVolume(Volume volume, int volumeIndex) {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("确认删除")
            .setMessage("确定要删除《" + volume.getTitle() + "》吗？该卷下的所有章节都将被删除。")
            .setPositiveButton("删除", (dialog, which) -> {
                volumes.remove(volumeIndex);
                
                // 重新编号
                for (int i = 0; i < volumes.size(); i++) {
                    volumes.get(i).setId(i + 1);
                }
                
                volumeCount = volumes.size();
                renderVolumes();
                
                // 保存
                saveStructure();
                Toast.makeText(requireContext(), "已删除", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("取消", null)
            .show();
    }
    
    /**
     * 删除章节
     */
    private void deleteChapter(Volume volume, Chapter chapter) {
        int listIndex = volume.getChapters().indexOf(chapter);
        
        if (listIndex < 0 || listIndex >= volume.getChapters().size()) {
            return;
        }
        
        Chapter removedChapter = volume.getChapters().remove(listIndex);
        
        // 重新编号章节
        for (int i = 0; i < volume.getChapters().size(); i++) {
            volume.getChapters().get(i).setId(i + 1);
        }
        
        // 保存并刷新UI
        saveStructure();
        renderVolumes();
        
        Toast.makeText(requireContext(), "已删除章节：《" + removedChapter.getTitle() + "》", Toast.LENGTH_SHORT).show();
    }

    /**
     * 保存卷章结构
     */
    public void saveStructure() {
        if (currentStory == null) {
            return;
        }

        String structureJson = JsonUtils.toJson(volumes);
        currentStory.setStructure(structureJson);

        int result = storyRepository.updateStory(currentStory);
        if (result > 0) {
            Toast.makeText(requireContext(), "保存成功", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(requireContext(), "保存失败", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 获取当前卷章数据
     */
    public List<Volume> getVolumes() {
        return volumes;
    }
    
    /**
     * 刷新视图（从数据库重新加载数据）
     */
    public void refreshView() {
        loadStoryData();
    }
}
