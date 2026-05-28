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
import androidx.core.widget.NestedScrollView;

import com.example.storyteller.R;
import com.example.storyteller.base.BaseFragment;
import com.example.storyteller.data.local.db.CharacterDao;
import com.example.storyteller.data.repository.StoryRepository;
import com.example.storyteller.data.repository.StoryRepositoryImpl;
import com.example.storyteller.model.Chapter;
import com.example.storyteller.model.Character;
import com.example.storyteller.model.PlotChapterSummary;
import com.example.storyteller.model.PlotOverviewSummary;
import com.example.storyteller.model.PlotSummarySnapshot;
import com.example.storyteller.model.Story;
import com.example.storyteller.model.Volume;
import com.example.storyteller.utils.JsonUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 写作Fragment - 卷章编辑器
 * 负责显示和编辑小说的卷章结构
 */
public class WritingFragment extends BaseFragment {

    private static final String ARG_STORY_ID = "arg_story_id";
    private static final int MAX_REFERENCE_TEXT_LENGTH = 180;
    private static final int MAX_CHARACTER_REFERENCE_COUNT = 4;

    // UI Components
    private LinearLayout layoutContent;
    private Button btnAddVolume;
    private TextView tvEmptyHint;
    private View cardWritingReference;
    private TextView tvWritingReferenceStatus;
    private TextView tvWritingReferenceOverview;
    private TextView tvWritingReferenceMainLine;

    // Data
    private List<Volume> volumes = new ArrayList<>();
    private Story currentStory;
    private int storyId;
    private StoryRepository storyRepository;
    private CharacterDao characterDao;
    private int volumeCount = 0;
    private final List<Character> storyCharacters = new ArrayList<>();
    private final Map<String, Character> characterIndex = new LinkedHashMap<>();
    private PlotSummarySnapshot currentPlotSnapshot;

    // 标记是否有正在编辑的EditText（用于防止切换Tab时自动保存）
    private EditText currentEditingEditText = null;

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
        cardWritingReference = view.findViewById(R.id.card_writing_reference);
        tvWritingReferenceStatus = view.findViewById(R.id.tv_writing_reference_status);
        tvWritingReferenceOverview = view.findViewById(R.id.tv_writing_reference_overview);
        tvWritingReferenceMainLine = view.findViewById(R.id.tv_writing_reference_main_line);

        // 添加卷按钮
        btnAddVolume.setOnClickListener(v -> addNewVolume());
    }

    @Override
    protected void initData() {
        storyRepository = new StoryRepositoryImpl(requireContext());
        characterDao = new CharacterDao(requireContext());

        if (getArguments() != null) {
            storyId = getArguments().getInt(ARG_STORY_ID, -1);
        }

        if (storyId > 0) {
            loadStoryData();
        } else {
            Toast.makeText(requireContext(), "未找到作品", Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    public void onPause() {
        super.onPause();
        // Fragment暂停时，清除编辑状态，防止切换Tab时自动保存
        currentEditingEditText = null;
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // 每次可见时从数据库重新加载最新数据，确保不会覆盖OutlineFragment的修改
        if (storyId > 0) {
            volumes.clear();
            loadStoryData();
        }
    }
    
    @Override
    public void onDestroyView() {
        // View销毁时，先保存数据，防止切换Tab导致数据丢失
        saveStructureSilently();
        // 清除编辑状态
        currentEditingEditText = null;
        super.onDestroyView();
    }

    /**
     * 加载作品数据
     */
    private void loadStoryData() {
        // 重置UI状态，防止残留视图干扰
        resetUIState();
        
        currentStory = storyRepository.getStoryById(storyId);
        if (currentStory == null) {
            storyCharacters.clear();
            characterIndex.clear();
            currentPlotSnapshot = null;
            bindWritingReferenceCard();
            Toast.makeText(requireContext(), "加载作品失败", Toast.LENGTH_SHORT).show();
            return;
        }

        loadWritingReferenceData();

        // 解析卷章结构
        String structureJson = currentStory.getStructure();
        
        if (!TextUtils.isEmpty(structureJson)) {
            try {
                volumes = JsonUtils.fromJson(structureJson, 
                    new com.google.gson.reflect.TypeToken<List<Volume>>(){}.getType());
            } catch (Exception e) {
                android.util.Log.e("WritingFragment", "解析卷章结构失败: " + e.getMessage());
                e.printStackTrace();
                volumes = new ArrayList<>();
            }
        } else {
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
     * 重置UI状态，确保不会有残留视图或数据
     */
    private void resetUIState() {
        if (layoutContent != null) {
            // 清除所有子视图（保留永久视图）
            List<View> viewsToRemove = new ArrayList<>();
            for (int i = 0; i < layoutContent.getChildCount(); i++) {
                View child = layoutContent.getChildAt(i);
                int childId = child.getId();
                if (childId != R.id.btn_add_volume
                        && childId != R.id.tv_empty_hint
                        && childId != R.id.card_writing_reference) {
                    viewsToRemove.add(child);
                }
            }
            for (View view : viewsToRemove) {
                layoutContent.removeView(view);
            }
        }
        
        // 清空数据列表，防止残留数据
        volumes.clear();
        
        // 显示加载提示
        tvEmptyHint.setVisibility(View.GONE);
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
     * 渲染所有卷
     */
    private void renderVolumes() {
        // 清除除btn_add_volume之外的所有视图
        List<View> viewsToRemove = new ArrayList<>();
        
        for (int i = 0; i < layoutContent.getChildCount(); i++) {
            View child = layoutContent.getChildAt(i);
            if (child.getId() != R.id.btn_add_volume
                    && child.getId() != R.id.tv_empty_hint
                    && child.getId() != R.id.card_writing_reference) {
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
            .inflate(R.layout.item_chapter, layoutContent, false);

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

        bindChapterReference(chapterView, volume, chapter, chapterIndex);

        // 内容编辑器
        EditText etContent = chapterView.findViewById(R.id.et_chapter_content);
        etContent.setHint("开始写作...");
        
        // 先添加 TextWatcher，再设置文本内容
        etContent.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(android.text.Editable s) {
                // 忽略setText触发的回调
                chapter.setContent(s.toString());
                // 实时静默保存正文内容
                saveStructureSilently();
            }
        });
        
        // 设置初始内容
        if (!TextUtils.isEmpty(chapter.getContent())) {
            etContent.setText(chapter.getContent());
        }

        return chapterView;
    }

    private void loadWritingReferenceData() {
        storyCharacters.clear();
        characterIndex.clear();
        currentPlotSnapshot = null;

        if (currentStory == null) {
            bindWritingReferenceCard();
            return;
        }

        List<Character> cachedCharacters = characterDao == null
                ? null
                : characterDao.getCharactersByStoryId(currentStory.getId());
        if (cachedCharacters != null) {
            storyCharacters.addAll(cachedCharacters);
        }

        for (Character character : storyCharacters) {
            if (character == null) {
                continue;
            }
            String normalizedName = normalizeName(character.getName());
            if (!TextUtils.isEmpty(normalizedName) && !characterIndex.containsKey(normalizedName)) {
                characterIndex.put(normalizedName, character);
            }
        }

        if (!TextUtils.isEmpty(currentStory.getPlotSummaryJson())) {
            try {
                currentPlotSnapshot = JsonUtils.fromJson(currentStory.getPlotSummaryJson(), PlotSummarySnapshot.class);
            } catch (Exception ignored) {
                currentPlotSnapshot = null;
            }
        }

        bindWritingReferenceCard();
    }

    private void bindWritingReferenceCard() {
        if (cardWritingReference == null || tvWritingReferenceStatus == null) {
            return;
        }

        cardWritingReference.setVisibility(View.VISIBLE);

        int characterCount = storyCharacters.size();
        int plotCount = 0;
        PlotOverviewSummary overview = null;
        if (currentPlotSnapshot != null) {
            if (currentPlotSnapshot.getChapterSummaries() != null) {
                plotCount = currentPlotSnapshot.getChapterSummaries().size();
            }
            overview = currentPlotSnapshot.getOverview();
        }

        if (characterCount > 0 && plotCount > 0) {
            tvWritingReferenceStatus.setText(getString(R.string.writing_reference_status_format, characterCount, plotCount));
        } else if (characterCount > 0) {
            tvWritingReferenceStatus.setText(getString(R.string.writing_reference_status_character_only, characterCount));
        } else if (plotCount > 0) {
            tvWritingReferenceStatus.setText(getString(R.string.writing_reference_status_plot_only, plotCount));
        } else {
            tvWritingReferenceStatus.setText(getString(R.string.writing_reference_empty));
        }

        String overallSummary = overview == null ? "" : safeTrim(overview.getOverallSummary());
        if (!TextUtils.isEmpty(overallSummary)) {
            tvWritingReferenceOverview.setVisibility(View.VISIBLE);
            tvWritingReferenceOverview.setText(getString(
                    R.string.writing_reference_overview_format,
                    truncateText(overallSummary, MAX_REFERENCE_TEXT_LENGTH)));
        } else {
            tvWritingReferenceOverview.setVisibility(View.GONE);
            tvWritingReferenceOverview.setText("");
        }

        List<String> mainLine = overview == null ? null : overview.getMainLine();
        if (mainLine != null && !mainLine.isEmpty()) {
            tvWritingReferenceMainLine.setVisibility(View.VISIBLE);
            tvWritingReferenceMainLine.setText(getString(
                    R.string.writing_reference_main_line_format,
                    truncateText(joinNonEmpty(mainLine, "；"), MAX_REFERENCE_TEXT_LENGTH)));
        } else {
            tvWritingReferenceMainLine.setVisibility(View.GONE);
            tvWritingReferenceMainLine.setText("");
        }
    }

    private void bindChapterReference(View chapterView, Volume volume, Chapter chapter, int chapterIndex) {
        View cardReference = chapterView.findViewById(R.id.card_chapter_reference);
        TextView tvPlot = chapterView.findViewById(R.id.tv_chapter_reference_plot);
        TextView tvCharacters = chapterView.findViewById(R.id.tv_chapter_reference_characters);
        if (cardReference == null || tvPlot == null || tvCharacters == null) {
            return;
        }

        PlotChapterSummary chapterSummary = findChapterSummary(volume, chapterIndex, chapter == null ? "" : chapter.getTitle());
        String plotReference = buildChapterPlotReference(chapterSummary);
        String characterReference = buildChapterCharacterReference(chapterSummary);

        if (TextUtils.isEmpty(plotReference) && TextUtils.isEmpty(characterReference)) {
            cardReference.setVisibility(View.GONE);
            tvPlot.setVisibility(View.GONE);
            tvCharacters.setVisibility(View.GONE);
            return;
        }

        cardReference.setVisibility(View.VISIBLE);
        if (!TextUtils.isEmpty(plotReference)) {
            tvPlot.setVisibility(View.VISIBLE);
            tvPlot.setText(getString(R.string.writing_reference_chapter_plot_format, plotReference));
        } else {
            tvPlot.setVisibility(View.GONE);
            tvPlot.setText("");
        }

        if (!TextUtils.isEmpty(characterReference)) {
            tvCharacters.setVisibility(View.VISIBLE);
            tvCharacters.setText(getString(R.string.writing_reference_chapter_characters_format, characterReference));
        } else {
            tvCharacters.setVisibility(View.GONE);
            tvCharacters.setText("");
        }
    }

    private PlotChapterSummary findChapterSummary(Volume volume, int chapterIndex, String chapterTitle) {
        if (currentPlotSnapshot == null || currentPlotSnapshot.getChapterSummaries() == null) {
            return null;
        }

        int volumeIndex = resolveVolumeIndex(volume);
        for (PlotChapterSummary summary : currentPlotSnapshot.getChapterSummaries()) {
            if (summary == null) {
                continue;
            }
            if (summary.getVolumeIndex() == volumeIndex && summary.getChapterIndex() == chapterIndex) {
                return summary;
            }
        }

        String normalizedTitle = normalizeName(chapterTitle);
        if (TextUtils.isEmpty(normalizedTitle)) {
            return null;
        }
        for (PlotChapterSummary summary : currentPlotSnapshot.getChapterSummaries()) {
            if (summary == null) {
                continue;
            }
            if (normalizedTitle.equals(normalizeName(summary.getChapterTitle()))) {
                return summary;
            }
        }
        return null;
    }

    private int resolveVolumeIndex(Volume volume) {
        int index = volumes.indexOf(volume);
        if (index >= 0) {
            return index + 1;
        }
        if (volume != null && volume.getId() > 0) {
            return volume.getId();
        }
        return 1;
    }

    private String buildChapterPlotReference(PlotChapterSummary summary) {
        if (summary == null) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        String brief = safeTrim(summary.getBriefSummary());
        if (!TextUtils.isEmpty(brief)) {
            builder.append(brief);
        }

        String detail = safeTrim(summary.getDetailSummary());
        if (!TextUtils.isEmpty(detail) && !detail.equals(brief)) {
            if (builder.length() > 0) {
                builder.append("\n");
            }
            builder.append(truncateText(detail, MAX_REFERENCE_TEXT_LENGTH));
        }

        if (summary.getKeyEvents() != null && !summary.getKeyEvents().isEmpty()) {
            if (builder.length() > 0) {
                builder.append("\n");
            }
            builder.append("关键事件：")
                    .append(truncateText(joinNonEmpty(summary.getKeyEvents(), "；"), MAX_REFERENCE_TEXT_LENGTH));
        }
        return builder.toString().trim();
    }

    private String buildChapterCharacterReference(PlotChapterSummary summary) {
        if (summary == null || summary.getCharacters() == null || summary.getCharacters().isEmpty()) {
            return "";
        }

        List<String> lines = new ArrayList<>();
        int count = 0;
        for (String rawName : summary.getCharacters()) {
            String name = safeTrim(rawName);
            if (TextUtils.isEmpty(name)) {
                continue;
            }
            Character character = characterIndex.get(normalizeName(name));
            if (character != null) {
                String profile = safeTrim(character.getProfile());
                if (TextUtils.isEmpty(profile)) {
                    profile = truncateText(safeTrim(character.getDetail()), 60);
                }
                if (!TextUtils.isEmpty(profile)) {
                    lines.add("• " + character.getName() + "：" + profile);
                } else {
                    lines.add("• " + character.getName());
                }
            } else {
                lines.add("• " + name);
            }
            count++;
            if (count >= MAX_CHARACTER_REFERENCE_COUNT) {
                break;
            }
        }

        if (lines.isEmpty()) {
            return "";
        }
        if (summary.getCharacters().size() > count) {
            lines.add("• 其余人物可在人物画像中查看");
        }
        return truncateText(joinNonEmpty(lines, "\n"), MAX_REFERENCE_TEXT_LENGTH * 2);
    }

    private String joinNonEmpty(List<String> values, String delimiter) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            String trimmed = safeTrim(value);
            if (TextUtils.isEmpty(trimmed)) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(delimiter);
            }
            builder.append(trimmed);
        }
        return builder.toString();
    }

    private String truncateText(String text, int maxLength) {
        String trimmed = safeTrim(text);
        if (TextUtils.isEmpty(trimmed) || trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, Math.max(0, maxLength - 1)) + "…";
    }

    private String safeTrim(String text) {
        if (TextUtils.isEmpty(text)) {
            return "";
        }
        return text.trim();
    }

    private String normalizeName(String rawName) {
        if (TextUtils.isEmpty(rawName)) {
            return "";
        }
        return rawName.trim()
                .replace("“", "")
                .replace("”", "")
                .replace("'", "")
                .replace("\"", "")
                .replace("《", "")
                .replace("》", "")
                .replace("：", "")
                .replace(":", "")
                .trim();
    }

    /**
     * 设置内联编辑（单击切换编辑模式）
     */
    private void setupInlineEdit(TextView textView, EditText editText, Object model, boolean isChapter) {
        // 使用Tag存储关联信息
        // Tag结构: [textView, model, isChapter]
        Object[] tagData = new Object[]{textView, model, isChapter};
        editText.setTag(tagData);
        textView.setTag(tagData);  // TextView也存储同样的Tag
        
        // 清除旧的监听器（如果有）
        textView.setOnClickListener(null);
        editText.setOnFocusChangeListener(null);
        editText.setOnEditorActionListener(null);
        
        // 添加实时保存的TextWatcher
        editText.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(android.text.Editable s) {
                // 实时静默保存标题修改
                String newName = s.toString().trim();
                if (!TextUtils.isEmpty(newName)) {
                    // 更新模型
                    if (model instanceof Volume) {
                        ((Volume) model).setTitle(newName);
                    } else if (model instanceof Chapter) {
                        ((Chapter) model).setTitle(newName);
                    }
                    // 静默保存
                    saveStructureSilently();
                    // 通知目录刷新
                    notifyTocRefresh();
                }
            }
        });
        
        // 单击 TextView 切换到编辑模式
        textView.setOnClickListener(v -> {
            // 从TextView的Tag中获取数据
            Object[] tag = (Object[]) v.getTag();
            if (tag == null || tag.length != 3) {
                return;
            }
            
            TextView tv = (TextView) tag[0];
            
            // 找到对应的EditText（通过遍历父布局）
            android.view.ViewParent parent = tv.getParent();
            if (parent instanceof android.view.ViewGroup) {
                android.view.ViewGroup vg = (android.view.ViewGroup) parent;
                for (int i = 0; i < vg.getChildCount(); i++) {
                    android.view.View child = vg.getChildAt(i);
                    if (child instanceof EditText && child.getVisibility() == View.GONE) {
                        EditText et = (EditText) child;
                        Object[] etTag = (Object[]) et.getTag();
                        if (etTag != null && etTag.length == 3 && etTag[0] == tv) {
                            // 找到了对应的EditText
                            tv.setVisibility(View.GONE);
                            et.setVisibility(View.VISIBLE);
                            et.requestFocus();
                            et.setSelection(et.getText().length());
                            
                            android.view.inputmethod.InputMethodManager imm = 
                                (android.view.inputmethod.InputMethodManager) requireActivity()
                                    .getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                            if (imm != null) {
                                imm.showSoftInput(et, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
                            }
                            currentEditingEditText = et;
                            break;
                        }
                    }
                }
            }
        });

        // EditText 失去焦点时切换回显示模式
        editText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus && currentEditingEditText == v) {
                // 从EditText的Tag中获取数据
                Object[] tag = (Object[]) v.getTag();
                if (tag != null && tag.length == 3) {
                    TextView tv = (TextView) tag[0];
                    Object m = tag[1];
                    Boolean isChap = (Boolean) tag[2];
                    finishEditing((EditText) v, tv, m, isChap);
                }
                currentEditingEditText = null;
            }
        });

        // 按回车键完成编辑
        editText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE ||
                actionId == android.view.inputmethod.EditorInfo.IME_NULL) {
                // 从EditText的Tag中获取数据
                Object[] tag = (Object[]) v.getTag();
                if (tag != null && tag.length == 3) {
                    TextView tv = (TextView) tag[0];
                    Object m = tag[1];
                    Boolean isChap = (Boolean) tag[2];
                    finishEditing((EditText) v, tv, m, isChap);
                }
                currentEditingEditText = null;
                return true;
            }
            return false;
        });
    }

    /**
     * 完成编辑，切换回显示模式
     */
    private void finishEditing(EditText editText, TextView textView, Object model, boolean isChapter) {
        // 安全检查：确保editText和textView仍然 attached to window
        if (!editText.isAttachedToWindow() || !textView.isAttachedToWindow()) {
            // View已经销毁，不执行保存
            return;
        }
        
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
        
        // 静默保存（因为TextWatcher已经实时保存了，这里只是确保最终状态一致）
        saveStructureSilently();
        
        // 通知目录刷新
        notifyTocRefresh();
    }

    /**
     * 添加新卷
     */
    private void addNewVolume() {
        volumeCount++;
        Volume newVolume = new Volume(volumeCount, "新卷名");
        volumes.add(newVolume);
        renderVolumes();
        
        // 先静默保存
        saveStructureSilently();
        
        // 通知目录刷新
        notifyTocRefresh();
        
        // 再显示操作结果
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
        
        // 先保存并刷新UI
        saveStructureSilently();
        renderVolumes();
        
        // 通知目录刷新
        notifyTocRefresh();
        
        // 再显示操作结果
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

        // 先静默保存
        saveStructureSilently();
        
        // 通知目录刷新
        notifyTocRefresh();
        
        // 再显示操作结果
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
        
        // 先保存并刷新UI
        saveStructureSilently();
        renderVolumes();
        
        // 通知目录刷新
        notifyTocRefresh();
        
        // 再显示操作结果（只在用户主动操作时显示）
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
                
                // 先静默保存
                saveStructureSilently();
                
                // 通知目录刷新
                notifyTocRefresh();
                
                // 再显示操作结果
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
        
        // 计算删除的字数
        int deletedWordCount = chapter.getContent() != null ? chapter.getContent().length() : 0;
        
        Chapter removedChapter = volume.getChapters().remove(listIndex);
        
        // 重新编号章节
        for (int i = 0; i < volume.getChapters().size(); i++) {
            volume.getChapters().get(i).setId(i + 1);
        }
        
        // 先静默保存并刷新UI
        saveStructureSilently();
        
        renderVolumes();
        
        // 通知目录刷新
        notifyTocRefresh();
        
        // 再显示操作结果
        Toast.makeText(requireContext(), "已删除章节：《" + removedChapter.getTitle() + "》", Toast.LENGTH_SHORT).show();
    }
    
    /**
     * 通知目录和大纲刷新
     */
    private void notifyTocRefresh() {
        android.util.Log.d("WritingFragment", "notifyTocRefresh: 通知目录和大纲刷新");
        if (getActivity() instanceof com.example.storyteller.ui.activity.StoryWorkspaceActivity) {
            com.example.storyteller.ui.activity.StoryWorkspaceActivity activity = 
                (com.example.storyteller.ui.activity.StoryWorkspaceActivity) getActivity();
            // 刷新目录
            android.util.Log.d("WritingFragment", "调用 StoryWorkspaceActivity.refreshTocView");
            activity.refreshTocView();
            // 刷新大纲
            android.util.Log.d("WritingFragment", "调用 StoryWorkspaceActivity.refreshOutlineView");
            activity.refreshOutlineView();
        } else {
            android.util.Log.e("WritingFragment", "getActivity 不是 StoryWorkspaceActivity 实例");
        }
    }

    /**
     * 保存卷章结构（公开方法，带Toast提示）
     */
    public void saveStructure() {
        saveStructureInternal(true);
    }

    /**
     * 静默保存卷章结构（不显示Toast）
     */
    public void saveStructureSilently() {
        saveStructureInternal(false);
    }

    /**
     * 内部保存方法
     * @param showToast 是否显示Toast提示
     */
    private void saveStructureInternal(boolean showToast) {
        android.util.Log.d("WritingFragment", "[SAVE] saveStructureInternal 开始, showToast=" + showToast + ", volumes.size=" + volumes.size());
        
        if (currentStory == null) {
            android.util.Log.e("WritingFragment", "[SAVE] 保存失败: currentStory为null");
            return;
        }
        
        // 【修复】移除isAdded检查，因为在onDestroyView时isAdded=false但仍需要保存数据
        // 检查Activity是否处于有效状态（允许在Fragment未attached时保存，只要Activity有效）
        boolean activityValid = getActivity() != null && !getActivity().isFinishing() && !getActivity().isDestroyed();
        android.util.Log.d("WritingFragment", "[SAVE] Activity有效性检查: activityValid=" + activityValid + ", isAdded=" + isAdded());
        
        // 如果Activity已销毁，则不保存（数据会丢失，但这是无法避免的）
        // 如果Activity有效，即使Fragment已detached，也可以尝试保存
        if (!activityValid) {
            android.util.Log.w("WritingFragment", "[SAVE] 保存跳过: Activity已销毁");
            return;
        }

        // 计算总字数
        int totalWordCount = 0;
        android.util.Log.d("WritingFragment", "[SAVE] 开始遍历章节计算字数");
        for (int i = 0; i < volumes.size(); i++) {
            Volume volume = volumes.get(i);
            android.util.Log.d("WritingFragment", "[SAVE] 卷" + (i+1) + "《" + volume.getTitle() + "》有" + volume.getChapters().size() + "章");
            for (int j = 0; j < volume.getChapters().size(); j++) {
                Chapter chapter = volume.getChapters().get(j);
                if (chapter.getContent() != null) {
                    int contentLen = chapter.getContent().length();
                    totalWordCount += contentLen;
                    android.util.Log.d("WritingFragment", "[SAVE]   章" + (j+1) + "《" + chapter.getTitle() + "》字数=" + contentLen);
                } else {
                    android.util.Log.d("WritingFragment", "[SAVE]   章" + (j+1) + "《" + chapter.getTitle() + "》字数=0(null)");
                }
            }
        }
        android.util.Log.d("WritingFragment", "[SAVE] 总字数=" + totalWordCount);
        
        // 【分离存储】不再需要合并大纲数据，因为大纲和写作内容已分离存储
        
        // 更新卷章结构
        String structureJson = JsonUtils.toJson(volumes);
        android.util.Log.d("WritingFragment", "[SAVE] 序列化volumes为JSON, JSON长度=" + (structureJson != null ? structureJson.length() : 0));
        
        currentStory.setStructure(structureJson);
        currentStory.setWordCount(totalWordCount);
        
        // 同时更新完整内容（从所有章节构建）
        StringBuilder fullContent = new StringBuilder();
        for (Volume volume : volumes) {
            for (Chapter chapter : volume.getChapters()) {
                // 添加章节标题
                if (!TextUtils.isEmpty(chapter.getTitle())) {
                    fullContent.append("## ").append(chapter.getTitle()).append("\n\n");
                }
                // 添加章节内容
                if (!TextUtils.isEmpty(chapter.getContent())) {
                    fullContent.append(chapter.getContent()).append("\n\n");
                }
            }
        }
        currentStory.setContent(fullContent.toString().trim());
        android.util.Log.d("WritingFragment", "[SAVE] 构建完整内容完成, content长度=" + currentStory.getContent().length());

        // 只更新写作相关字段，不覆盖架构信息
        android.util.Log.d("WritingFragment", "[SAVE] 调用storyRepository.updateStoryWriting");
        int result = storyRepository.updateStoryWriting(
            currentStory.getId(),
            structureJson,
            totalWordCount,
            currentStory.getContent()
        );
        android.util.Log.d("WritingFragment", "[SAVE] 数据库更新结果=" + result);
        
        if (result > 0) {
            // 【同步】同步标题到outline_data
            syncTitlesToOutlineData();
            
            // 通知Activity刷新大纲视图（如果当前在大纲Tab）
            // 安全检查：确保Activity仍然有效且未处于状态保存后
            if (isAdded() && getActivity() != null && !getActivity().isFinishing() && !getActivity().isDestroyed()) {
                if (getActivity() instanceof com.example.storyteller.ui.activity.StoryWorkspaceActivity) {
                    ((com.example.storyteller.ui.activity.StoryWorkspaceActivity) getActivity()).refreshOutlineView();
                }
            }
            
            if (showToast) {
                Toast.makeText(requireContext(), "保存成功", Toast.LENGTH_SHORT).show();
            }
        } else {
            if (showToast) {
                Toast.makeText(requireContext(), "保存失败", Toast.LENGTH_SHORT).show();
            }
        }
        android.util.Log.d("WritingFragment", "[SAVE] saveStructureInternal 结束");
    }
    
    /**
     * 同步标题到outline_data
     * 
     * 【设计说明】
     * 虽然采用了分离存储（structure vs outline_data），但标题作为基础元数据，
     * 需要在两个地方保持一致。这里采用单向同步策略：
     * - WritingFragment修改标题后，自动同步到outline_data
     * - OutlineFragment不应修改标题，只修改大纲字段（摘要、作用等）
     * 
     * TODO: 未来可以考虑将标题从outline_data中移除，改用volumeId引用，
     * 实现真正的职责分离。但这需要重构OutlineFragment的数据加载逻辑。
     */
    private void syncTitlesToOutlineData() {
        try {
            // 读取现有的outline_data
            Story story = storyRepository.getStoryById(currentStory.getId());
            if (story == null) {
                return;
            }
            
            String outlineJson = story.getOutlineData();
            List<Volume> outlineVolumes;
            
            if (!TextUtils.isEmpty(outlineJson)) {
                outlineVolumes = JsonUtils.fromJson(outlineJson,
                    new com.google.gson.reflect.TypeToken<List<Volume>>(){}.getType());
            } else {
                outlineVolumes = new ArrayList<>();
            }
            
            boolean needUpdate = false;
            
            // 同步卷和章节的标题
            for (int i = 0; i < volumes.size(); i++) {
                Volume writingVol = volumes.get(i);
                Volume outlineVol = (i < outlineVolumes.size()) ? outlineVolumes.get(i) : null;
                
                if (outlineVol != null) {
                    // 检查卷标题是否变化
                    if (!writingVol.getTitle().equals(outlineVol.getTitle())) {
                        outlineVol.setTitle(writingVol.getTitle());
                        needUpdate = true;
                        android.util.Log.d("WritingFragment", "同步卷标题: '" + outlineVol.getTitle() + "' -> '" + writingVol.getTitle() + "'");
                    }
                    
                    // 同步章节标题
                    List<Chapter> writingChapters = writingVol.getChapters();
                    List<Chapter> outlineChapters = outlineVol.getChapters();
                    
                    if (writingChapters != null && outlineChapters != null) {
                        for (int j = 0; j < writingChapters.size(); j++) {
                            Chapter writingChapter = writingChapters.get(j);
                            Chapter outlineChapter = (j < outlineChapters.size()) ? outlineChapters.get(j) : null;
                            
                            if (outlineChapter != null && !writingChapter.getTitle().equals(outlineChapter.getTitle())) {
                                outlineChapter.setTitle(writingChapter.getTitle());
                                needUpdate = true;
                                android.util.Log.d("WritingFragment", "同步章节标题: '" + outlineChapter.getTitle() + "' -> '" + writingChapter.getTitle() + "'");
                            }
                        }
                    }
                }
            }
            
            // 如果有变化，保存到outline_data
            if (needUpdate) {
                String updatedOutlineJson = JsonUtils.toJson(outlineVolumes);
                storyRepository.updateStoryOutline(currentStory.getId(), updatedOutlineJson);
                android.util.Log.d("WritingFragment", "已同步标题到outline_data");
            }
        } catch (Exception e) {
            android.util.Log.e("WritingFragment", "同步标题失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    /**
     * 获取当前卷章数据
     */
    public List<Volume> getVolumes() {
        return volumes;
    }
    
    /**
     * 导航到指定章节
     * @param volumeIndex 卷索引
     * @param chapterIndex 章索引
     */
    public void navigateToChapter(int volumeIndex, int chapterIndex) {
        android.util.Log.d("WritingFragment", "navigateToChapter: 卷" + volumeIndex + " 章" + chapterIndex);
        android.util.Log.d("WritingFragment", "当前volumes数量: " + (volumes != null ? volumes.size() : 0));
        
        if (volumeIndex < 0 || volumeIndex >= volumes.size()) {
            android.util.Log.e("WritingFragment", "无效的卷索引: " + volumeIndex);
            Toast.makeText(requireContext(), "无效的卷", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Volume volume = volumes.get(volumeIndex);
        List<Chapter> chapters = volume.getChapters();
        
        android.util.Log.d("WritingFragment", "该卷章节数量: " + (chapters != null ? chapters.size() : 0));
        
        if (chapters == null || chapterIndex < 0 || chapterIndex >= chapters.size()) {
            android.util.Log.e("WritingFragment", "无效的章节索引: " + chapterIndex);
            Toast.makeText(requireContext(), "无效的章节", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 滚动到对应的章节
        scrollToChapter(volumeIndex, chapterIndex);
    }
    
    /**
     * 滚动到指定章节
     */
    private void scrollToChapter(int volumeIndex, int chapterIndex) {
        if (layoutContent == null) {
            android.util.Log.e("WritingFragment", "layoutContent 为 null");
            return;
        }
        
        // 查找目标章节的 EditText（内容编辑器）
        View targetView = findChapterContentView(volumeIndex, chapterIndex);
        
        if (targetView != null) {
            android.util.Log.d("WritingFragment", "找到目标章节视图，开始滚动");
            
            // 查找父布局中的 NestedScrollView 或 ScrollView
            android.view.ViewParent parent = layoutContent.getParent();
            NestedScrollView nestedScrollView = null;
            android.widget.ScrollView scrollView = null;
            
            while (parent != null) {
                if (parent instanceof NestedScrollView) {
                    nestedScrollView = (NestedScrollView) parent;
                    break;
                } else if (parent instanceof android.widget.ScrollView) {
                    scrollView = (android.widget.ScrollView) parent;
                    break;
                }
                parent = parent.getParent();
            }
            
            if (nestedScrollView != null) {
                android.util.Log.d("WritingFragment", "找到 NestedScrollView");
                // 计算滚动位置
                int[] location = new int[2];
                targetView.getLocationInWindow(location);
                int[] scrollViewLocation = new int[2];
                nestedScrollView.getLocationInWindow(scrollViewLocation);
                int scrollY = location[1] - scrollViewLocation[1] - 200; // 留一些顶部空间
                
                // 执行平滑滚动
                nestedScrollView.smoothScrollTo(0, scrollY);
                android.util.Log.d("WritingFragment", "滚动到位置: " + scrollY);
                
                // 延迟聚焦到编辑框
                finalizeChapterNavigation(targetView, volumeIndex, chapterIndex);
                
            } else if (scrollView != null) {
                android.util.Log.d("WritingFragment", "找到 ScrollView");
                // 计算滚动位置
                int[] location = new int[2];
                targetView.getLocationInWindow(location);
                int scrollY = location[1] - scrollView.getTop() - 200; // 留一些顶部空间
                
                // 执行滚动
                scrollView.smoothScrollTo(0, scrollY);
                android.util.Log.d("WritingFragment", "滚动到位置: " + scrollY);
                
                // 延迟聚焦到编辑框
                finalizeChapterNavigation(targetView, volumeIndex, chapterIndex);
                
            } else {
                android.util.Log.e("WritingFragment", "未找到 ScrollView 或 NestedScrollView");
                Toast.makeText(requireContext(), "无法定位到章节", Toast.LENGTH_SHORT).show();
            }
        } else {
            android.util.Log.e("WritingFragment", "未找到目标章节视图");
            Toast.makeText(requireContext(), "未找到对应章节", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 完成章节导航的最后步骤（聚焦和显示键盘）
     */
    private void finalizeChapterNavigation(View targetView, int volumeIndex, int chapterIndex) {
        targetView.postDelayed(() -> {
            if (targetView instanceof EditText) {
                EditText etContent = (EditText) targetView;
                etContent.requestFocus();
                // 显示光标在末尾
                etContent.setSelection(etContent.getText().length());
                
                // 显示软键盘
                android.view.inputmethod.InputMethodManager imm = 
                    (android.view.inputmethod.InputMethodManager) requireActivity()
                        .getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(etContent, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
                }
                
                android.util.Log.d("WritingFragment", "已聚焦到章节编辑框");
            }
            
            // 显示提示信息
            Chapter chapter = volumes.get(volumeIndex).getChapters().get(chapterIndex);
            String message = "第" + (volumeIndex + 1) + "卷 - " + 
                            (TextUtils.isEmpty(chapter.getTitle()) ? "第" + (chapterIndex + 1) + "章" : chapter.getTitle());
            Toast.makeText(requireContext(), "已定位到：" + message, Toast.LENGTH_SHORT).show();
        }, 500);
    }
    
    /**
     * 查找指定章节的内容编辑框
     */
    private View findChapterContentView(int volumeIndex, int chapterIndex) {
        if (layoutContent == null) {
            return null;
        }
        
        // 遍历所有子视图，找到对应的卷和章节
        int currentVolumeIdx = -1;
        int currentChapterIdx = -1;
        
        for (int i = 0; i < layoutContent.getChildCount(); i++) {
            View child = layoutContent.getChildAt(i);
            
            // 检查是否是卷视图（item_volume）
            if (child.getTag() != null && child.getTag() instanceof Integer) {
                int tagValue = (Integer) child.getTag();
                // 假设我们给卷视图设置了Tag
            }
            
            // 通过findViewById查找卷内的章节容器
            LinearLayout layoutChapters = child.findViewById(R.id.layout_chapters_container);
            if (layoutChapters != null) {
                currentVolumeIdx++;
                
                if (currentVolumeIdx == volumeIndex) {
                    // 找到了目标卷，现在查找目标章节
                    if (chapterIndex < layoutChapters.getChildCount()) {
                        View chapterView = layoutChapters.getChildAt(chapterIndex);
                        if (chapterView != null) {
                            // 找到章节的内容编辑框
                            EditText etContent = chapterView.findViewById(R.id.et_chapter_content);
                            if (etContent != null) {
                                android.util.Log.d("WritingFragment", 
                                    "找到章节编辑框: 卷" + volumeIndex + " 章" + chapterIndex);
                                return etContent;
                            }
                        }
                    }
                    break;
                }
            }
        }
        
        return null;
    }
}
