package com.example.storyteller.ui.fragment;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.storyteller.R;
import com.example.storyteller.base.BaseFragment;
import com.example.storyteller.data.repository.StoryRepository;
import com.example.storyteller.data.repository.StoryRepositoryImpl;
import com.example.storyteller.model.Chapter;
import com.example.storyteller.model.Story;
import com.example.storyteller.model.Volume;
import com.example.storyteller.ui.adapter.OutlineAdapter;
import com.example.storyteller.ui.dialog.ChapterOutlineEditDialog;
import com.example.storyteller.ui.dialog.GlobalOutlineEditDialog;
import com.example.storyteller.ui.dialog.VolumeOutlineEditDialog;

import java.util.ArrayList;
import java.util.List;

/**
 * 大纲Fragment
 * 展示和管理小说的全局大纲、卷纲和章纲
 */
public class OutlineFragment extends BaseFragment {

    private static final String ARG_STORY_ID = "arg_story_id";

    // UI Components
    private RecyclerView rvVolumes;
    private TextView tvChapterCount;
    private View cardGlobalOutline;
    private TextView tvGlobalOutlinePreview;
    private ImageButton btnToggleGlobalOutline;
    
    // 全局大纲展开状态
    private boolean isGlobalOutlineExpanded = false;

    // Data
    private int storyId;
    private Story currentStory;
    private StoryRepository storyRepository;
    private List<Volume> volumes;
    private OutlineAdapter adapter;

    public static OutlineFragment newInstance(int storyId) {
        OutlineFragment fragment = new OutlineFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_STORY_ID, storyId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_outline;
    }

    @Override
    protected void initView(View view) {
        rvVolumes = view.findViewById(R.id.rv_volumes);
        tvChapterCount = view.findViewById(R.id.tv_chapter_count);
        cardGlobalOutline = view.findViewById(R.id.card_global_outline);
        tvGlobalOutlinePreview = view.findViewById(R.id.tv_global_outline_preview);
        btnToggleGlobalOutline = view.findViewById(R.id.btn_toggle_global_outline);

        // 设置RecyclerView
        rvVolumes.setLayoutManager(new LinearLayoutManager(requireContext()));
        
        // 全局大纲卡片点击 - 打开编辑对话框
        cardGlobalOutline.setOnClickListener(v -> {
            showGlobalOutlineEditDialog();
        });
        
        // 展开/收起按钮点击
        btnToggleGlobalOutline.setOnClickListener(v -> {
            toggleGlobalOutline();
        });
    }

    @Override
    protected void initData() {
        storyRepository = new StoryRepositoryImpl(requireContext());

        if (getArguments() != null) {
            storyId = getArguments().getInt(ARG_STORY_ID, -1);
        }

        if (storyId > 0) {
            loadOutlineData();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // 每次可见时从数据库重新加载数据
        if (storyId > 0) {
            loadOutlineData();
        }
    }

    /**
     * 加载大纲数据
     */
    private void loadOutlineData() {
        android.util.Log.d("OutlineFragment", "=== 开始加载大纲数据 ===");
        currentStory = storyRepository.getStoryById(storyId);
        if (currentStory == null) {
            Toast.makeText(requireContext(), "加载作品失败", Toast.LENGTH_SHORT).show();
            return;
        }
    
        // 【分离存储】从outline_data字段读取大纲数据
        String outlineJson = currentStory.getOutlineData();
        android.util.Log.d("OutlineFragment", "outline_data长度: " + (outlineJson != null ? outlineJson.length() : 0));
            
        if (!TextUtils.isEmpty(outlineJson)) {
            try {
                volumes = com.example.storyteller.utils.JsonUtils.fromJson(outlineJson,
                    new com.google.gson.reflect.TypeToken<List<Volume>>(){}.getType());
                android.util.Log.d("OutlineFragment", "解析成功，卷数量: " + volumes.size());
                if (!volumes.isEmpty()) {
                    android.util.Log.d("OutlineFragment", "第一卷标题: " + volumes.get(0).getTitle());
                }
            } catch (Exception e) {
                e.printStackTrace();
                android.util.Log.e("OutlineFragment", "解析失败: " + e.getMessage());
                volumes = new ArrayList<>();
            }
        } else {
            volumes = new ArrayList<>();
        }
            
        // 【同步逻辑】检查是否需要从 structure同步新增的卷/章
        syncVolumesFromStructure();
    
        // 更新统计信息
        updateStatistics();
    
        // 显示全局大纲预览
        updateGlobalOutlinePreview();
    
        // 设置适配器
        adapter = new OutlineAdapter(requireContext(), volumes, this::onVolumeClick, this::onChapterClick);
        rvVolumes.setAdapter(adapter);
        android.util.Log.d("OutlineFragment", "=== 大纲数据加载完成 ===");
    }

    /**
     * 同步逻辑：从structure中同步新增的卷/章到outline_data
     */
    private void syncVolumesFromStructure() {
        try {
            // 从structure读取最新的卷章结构
            String structureJson = currentStory.getStructure();
            if (TextUtils.isEmpty(structureJson)) {
                return;
            }
            
            List<Volume> structureVolumes = com.example.storyteller.utils.JsonUtils.fromJson(structureJson,
                new com.google.gson.reflect.TypeToken<List<Volume>>(){}.getType());
            
            if (structureVolumes == null || structureVolumes.isEmpty()) {
                return;
            }
            
            boolean needSync = false;
            
            // 检查是否有新增的卷
            if (structureVolumes.size() > volumes.size()) {
                android.util.Log.d("OutlineFragment", "检测到新增的卷，需要同步");
                needSync = true;
            }
            
            // 检查是否有新增的章节
            if (!needSync) {
                for (int i = 0; i < Math.min(structureVolumes.size(), volumes.size()); i++) {
                    Volume structVol = structureVolumes.get(i);
                    Volume outlineVol = volumes.get(i);
                    
                    if (structVol.getChapters() != null && outlineVol.getChapters() != null) {
                        if (structVol.getChapters().size() != outlineVol.getChapters().size()) {
                            android.util.Log.d("OutlineFragment", "检测到新增的章节，需要同步");
                            needSync = true;
                            break;
                        }
                    }
                }
            }
            
            // 如果需要同步，从structure重建volumes
            if (needSync) {
                android.util.Log.d("OutlineFragment", "开始同步卷章结构");
                
                // 创建新的volumes列表，保留已有的大纲数据
                List<Volume> syncedVolumes = new ArrayList<>();
                
                for (int i = 0; i < structureVolumes.size(); i++) {
                    Volume structVol = structureVolumes.get(i);
                    Volume outlineVol = (i < volumes.size()) ? volumes.get(i) : null;
                    
                    if (outlineVol != null) {
                        // 已存在的卷，保留大纲数据，但更新章节列表
                        Volume mergedVol = new Volume();
                        mergedVol.setTitle(structVol.getTitle());
                        android.util.Log.d("OutlineFragment", "同步卷" + i + "标题: '" + outlineVol.getTitle() + "' -> '" + structVol.getTitle() + "'");
                        mergedVol.setSummary(outlineVol.getSummary());
                        mergedVol.setTargetWordCount(outlineVol.getTargetWordCount());
                        mergedVol.setTargetChapterCount(outlineVol.getTargetChapterCount());
                        
                        // 同步章节
                        List<Chapter> mergedChapters = syncChapters(structVol.getChapters(), outlineVol.getChapters());
                        mergedVol.setChapters(mergedChapters);
                        
                        syncedVolumes.add(mergedVol);
                    } else {
                        // 新增的卷，使用默认大纲字段
                        syncedVolumes.add(structVol);
                    }
                }
                
                volumes = syncedVolumes;
                
                // 保存同步后的数据到outline_data
                String syncedJson = com.example.storyteller.utils.JsonUtils.toJson(volumes);
                storyRepository.updateStoryOutline(currentStory.getId(), syncedJson);
                android.util.Log.d("OutlineFragment", "同步完成，已保存到outline_data");
            }
        } catch (Exception e) {
            android.util.Log.e("OutlineFragment", "同步失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 同步章节列表
     */
    private List<Chapter> syncChapters(List<Chapter> structChapters, List<Chapter> outlineChapters) {
        List<Chapter> syncedChapters = new ArrayList<>();
        
        for (int i = 0; i < structChapters.size(); i++) {
            Chapter structChapter = structChapters.get(i);
            Chapter outlineChapter = (i < outlineChapters.size()) ? outlineChapters.get(i) : null;
            
            if (outlineChapter != null) {
                // 已存在的章节，保留大纲数据
                Chapter mergedChapter = new Chapter();
                mergedChapter.setTitle(structChapter.getTitle());
                android.util.Log.d("OutlineFragment", "同步章节标题: '" + outlineChapter.getTitle() + "' -> '" + structChapter.getTitle() + "'");
                mergedChapter.setContent(structChapter.getContent());
                mergedChapter.setChapterRole(outlineChapter.getChapterRole());
                mergedChapter.setChapterSummary(outlineChapter.getChapterSummary());
                mergedChapter.setChapterPurpose(outlineChapter.getChapterPurpose());
                mergedChapter.setSuspenseLevel(outlineChapter.getSuspenseLevel());
                mergedChapter.setForeshadowing(outlineChapter.getForeshadowing());
                mergedChapter.setTwistLevel(outlineChapter.getTwistLevel());
                mergedChapter.setInvolvedCharacters(outlineChapter.getInvolvedCharacters());
                mergedChapter.setKeyItems(outlineChapter.getKeyItems());
                mergedChapter.setSceneLocations(outlineChapter.getSceneLocations());
                mergedChapter.setTimeConstraint(outlineChapter.getTimeConstraint());
                
                syncedChapters.add(mergedChapter);
            } else {
                // 新增的章节，使用默认大纲字段
                syncedChapters.add(structChapter);
            }
        }
        
        return syncedChapters;
    }

    /**
     * 更新统计信息
     */
    private void updateStatistics() {
        int totalChapters = 0;
        if (volumes != null) {
            for (Volume volume : volumes) {
                if (volume.getChapters() != null) {
                    totalChapters += volume.getChapters().size();
                }
            }
        }
        tvChapterCount.setText("共 " + totalChapters + " 个章纲");
    }

    /**
     * 切换全局大纲展开/收起状态
     */
    private void toggleGlobalOutline() {
        isGlobalOutlineExpanded = !isGlobalOutlineExpanded;
        
        // 更新按钮图标
        if (isGlobalOutlineExpanded) {
            btnToggleGlobalOutline.setImageResource(R.drawable.ic_arrow_drop_up);
        } else {
            btnToggleGlobalOutline.setImageResource(R.drawable.ic_arrow_drop_down);
        }
        
        // 重新渲染预览
        updateGlobalOutlinePreview();
    }

    /**
     * 更新全局大纲预览
     */
    private void updateGlobalOutlinePreview() {
        android.util.Log.d("OutlineFragment", "updateGlobalOutlinePreview called, isExpanded=" + isGlobalOutlineExpanded);
        
        if (currentStory == null) {
            android.util.Log.d("OutlineFragment", "currentStory is null");
            tvGlobalOutlinePreview.setText("点击编辑全局大纲...");
            return;
        }
        
        String globalOutline = currentStory.getGlobalOutline();
        android.util.Log.d("OutlineFragment", "globalOutline is empty: " + TextUtils.isEmpty(globalOutline));
        
        if (TextUtils.isEmpty(globalOutline)) {
            tvGlobalOutlinePreview.setText("点击编辑全局大纲...");
        } else {
            // 使用Markwon渲染Markdown
            try {
                io.noties.markwon.Markwon markwon = io.noties.markwon.Markwon.create(requireContext());
                
                if (isGlobalOutlineExpanded) {
                    // 展开状态：显示完整内容
                    android.util.Log.d("OutlineFragment", "展开状态，显示完整内容");
                    markwon.setMarkdown(tvGlobalOutlinePreview, globalOutline);
                } else {
                    // 收起状态：智能截断，只显示前3行非空内容
                    android.util.Log.d("OutlineFragment", "收起状态，生成预览");
                    String previewText = generateCollapsedPreview(globalOutline);
                    android.util.Log.d("OutlineFragment", "收起状态预览文本长度: " + previewText.length());
                    markwon.setMarkdown(tvGlobalOutlinePreview, previewText);
                }
            } catch (Exception e) {
                // 如果渲染失败，显示纯文本
                android.util.Log.e("OutlineFragment", "Markwon渲染失败: " + e.getMessage());
                tvGlobalOutlinePreview.setText(globalOutline);
                e.printStackTrace();
            }
        }
    }
    
    /**
     * 生成收起状态的预览文本
     * 规则：显示去掉空行后的前3行非空内容
     */
    private String generateCollapsedPreview(String markdown) {
        StringBuilder preview = new StringBuilder();
        String[] lines = markdown.split("\n");
        
        int nonEmptyLineCount = 0;
        android.util.Log.d("OutlineFragment", "=== 生成收起预览 ===");
        android.util.Log.d("OutlineFragment", "原始Markdown行数: " + lines.length);
        
        for (String line : lines) {
            String trimmedLine = line.trim();
            
            // 跳过空行
            if (trimmedLine.isEmpty()) {
                continue;
            }
            
            // 添加非空行
            preview.append(trimmedLine);
            nonEmptyLineCount++;
            android.util.Log.d("OutlineFragment", "第" + nonEmptyLineCount + "行非空: " + trimmedLine);
            
            // 如果已经取了3行，停止
            if (nonEmptyLineCount >= 3) {
                break;
            }
            
            preview.append("\n");
        }
        
        String result = preview.toString().trim();
        android.util.Log.d("OutlineFragment", "最终预览文本:\n" + result);
        android.util.Log.d("OutlineFragment", "=== 生成完成 ===");
        return result;
    }

    /**
     * 卷卡片点击回调
     */
    private void onVolumeClick(int volumeIndex) {
        // 弹出卷纲编辑BottomSheet
        Volume volume = volumes.get(volumeIndex);
        VolumeOutlineEditDialog dialog = VolumeOutlineEditDialog.newInstance(volumeIndex, volume);
        dialog.setOnSaveListener((index, updatedVolume) -> {
            // 更新volumes列表中的数据
            volumes.set(index, updatedVolume);
            // 保存数据
            saveOutlineData();
            // 刷新视图
            refreshOutlineView();
        });
        dialog.show(getChildFragmentManager(), "VolumeOutlineEdit");
    }

    /**
     * 章纲卡片点击回调
     */
    private void onChapterClick(int volumeIndex, int chapterIndex) {
        // 弹出章纲编辑BottomSheet
        Volume volume = volumes.get(volumeIndex);
        Chapter chapter = volume.getChapters().get(chapterIndex);
        
        ChapterOutlineEditDialog dialog = ChapterOutlineEditDialog.newInstance(
            volumeIndex, chapterIndex, chapter);
        dialog.setOnSaveListener((volIdx, chapIdx, updatedChapter) -> {
            // 更新volumes列表中的数据
            volumes.get(volIdx).getChapters().set(chapIdx, updatedChapter);
            // 保存数据
            saveOutlineData();
            // 刷新视图
            refreshOutlineView();
        });
        dialog.show(getChildFragmentManager(), "ChapterOutlineEdit");
    }

    /**
     * 公开方法：刷新大纲视图
     */
    public void refreshOutlineView() {
        loadOutlineData();
    }
    
    /**
     * 显示全局大纲编辑对话框
     */
    private void showGlobalOutlineEditDialog() {
        if (currentStory == null) {
            return;
        }
        
        String globalOutline = currentStory.getGlobalOutline();
        GlobalOutlineEditDialog dialog = GlobalOutlineEditDialog.newInstance(
            storyId, 
            globalOutline != null ? globalOutline : ""
        );
        
        dialog.setOnSaveListener((savedStoryId, savedGlobalOutline) -> {
            // 保存到数据库
            storyRepository.updateStoryGlobalOutline(savedStoryId, savedGlobalOutline);
            
            // 更新本地数据
            if (currentStory != null) {
                currentStory.setGlobalOutline(savedGlobalOutline);
            }
            
            // 刷新预览
            updateGlobalOutlinePreview();
        });
        
        dialog.show(getParentFragmentManager(), "GlobalOutlineEditDialog");
    }

    /**
     * 保存大纲数据
     */
    private void saveOutlineData() {
        if (currentStory == null || volumes == null) {
            android.util.Log.e("OutlineFragment", "保存失败: currentStory或volumes为null");
            return;
        }
        
        try {
            // 将volumes转换为JSON
            String json = com.example.storyteller.utils.JsonUtils.toJson(volumes);
            android.util.Log.d("OutlineFragment", "准备保存，JSON长度: " + json.length());
            android.util.Log.d("OutlineFragment", "JSON前100字符: " + json.substring(0, Math.min(100, json.length())));
            
            // 【分离存储】只更新outline_data字段，不覆盖其他字段
            int result = storyRepository.updateStoryOutline(currentStory.getId(), json);
            android.util.Log.d("OutlineFragment", "保存结果: " + result + " 行受影响");
            
            // 验证保存是否成功
            Story verifyStory = storyRepository.getStoryById(currentStory.getId());
            if (verifyStory != null && verifyStory.getOutlineData() != null) {
                android.util.Log.d("OutlineFragment", "验证成功，outline_data长度: " + verifyStory.getOutlineData().length());
            } else {
                android.util.Log.e("OutlineFragment", "验证失败: outline_data为null");
            }
        } catch (Exception e) {
            e.printStackTrace();
            android.util.Log.e("OutlineFragment", "保存异常: " + e.getMessage());
            Toast.makeText(requireContext(), "保存失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
