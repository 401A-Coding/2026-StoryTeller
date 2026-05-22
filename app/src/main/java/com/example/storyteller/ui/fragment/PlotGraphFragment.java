package com.example.storyteller.ui.fragment;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.storyteller.R;
import com.example.storyteller.base.BaseFragment;
import com.example.storyteller.data.local.db.SettingRelationshipDao;
import com.example.storyteller.data.local.db.StorySettingDao;
import com.example.storyteller.model.RelationExtractionResult;
import com.example.storyteller.model.SettingRelationship;
import com.example.storyteller.model.StorySetting;
import com.example.storyteller.ui.activity.SettingDetailActivity;
import com.example.storyteller.ui.activity.StoryWorkspaceActivity;
import com.example.storyteller.ui.dialog.ExtractionResultDialogFragment;
import com.example.storyteller.utils.RelationExtractor;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 设定关系图 Fragment
 * 展示该小说下所有设定之间的关系网络
 */
public class PlotGraphFragment extends BaseFragment {

    private static final String ARG_STORY_ID = "story_id";

    private WebView webView;
    private int storyId;
    
    private StorySettingDao settingDao;
    private SettingRelationshipDao relationshipDao;

    public PlotGraphFragment() {
        // Required empty public constructor
    }

    public static PlotGraphFragment newInstance(int storyId) {
        PlotGraphFragment fragment = new PlotGraphFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_STORY_ID, storyId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_plot_graph;
    }

    @Override
    protected void initView(View view) {
        webView = view.findViewById(R.id.webview_graph);
        
        // 刷新按钮
        com.google.android.material.floatingactionbutton.FloatingActionButton fabRefresh = 
            view.findViewById(R.id.fab_refresh);
        if (fabRefresh != null) {
            fabRefresh.setOnClickListener(v -> refreshGraph());
        }
        
        // AI分析按钮
        com.google.android.material.floatingactionbutton.FloatingActionButton fabAiAnalyze = 
            view.findViewById(R.id.fab_ai_analyze);
        if (fabAiAnalyze != null) {
            fabAiAnalyze.setOnClickListener(v -> startAiRelationExtraction());
        }
        
        // 初始化 DAO
        settingDao = new StorySettingDao(requireContext());
        relationshipDao = new SettingRelationshipDao(requireContext());
        
        // 配置 WebView
        setupWebView();
    }

    @Override
    protected void initData() {
        if (getArguments() != null) {
            storyId = getArguments().getInt(ARG_STORY_ID, -1);
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowUniversalAccessFromFileURLs(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        
        webView.setBackgroundColor(Color.WHITE);
        
        // WebViewClient
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // 页面加载完成后，加载数据
                loadGraphData();
            }
        });
        
        // 添加 JavaScript 接口
        webView.addJavascriptInterface(new JsInterface(), "Android");
        
        // 加载 HTML
        webView.loadUrl("file:///android_asset/plot_graph.html");
    }

    /**
     * 刷新关系图
     */
    private void refreshGraph() {
        if (webView != null) {
            webView.evaluateJavascript("clearGraph()", null);
            loadGraphData();
        }
    }
    
    private void loadGraphData() {
        if (storyId == -1) {
            showEmptyHint("无法加载数据");
            return;
        }
        
        // 获取该小说下的所有设定
        List<StorySetting> settings = settingDao.getByStoryId(storyId);
        
        // 获取所有设定ID
        Set<Integer> settingIds = new HashSet<>();
        for (StorySetting setting : settings) {
            settingIds.add(setting.getId());
        }
        
        // 构建ID到设定的映射
        Map<Integer, StorySetting> settingMap = new HashMap<>();
        for (StorySetting setting : settings) {
            settingMap.put(setting.getId(), setting);
        }
        
        // 获取所有关系
        List<SettingRelationship> allRelations = new ArrayList<>();
        for (Integer settingId : settingIds) {
            List<SettingRelationship> relations = relationshipDao.getBySettingId(settingId);
            for (SettingRelationship rel : relations) {
                // 只保留两端设定都在该小说内的关系
                if (settingIds.contains(rel.getSourceSettingId()) && settingIds.contains(rel.getTargetSettingId())) {
                    // 避免重复添加
                    boolean exists = false;
                    for (SettingRelationship existing : allRelations) {
                        if (existing.getId() == rel.getId()) {
                            exists = true;
                            break;
                        }
                    }
                    if (!exists) {
                        allRelations.add(rel);
                    }
                }
            }
        }
        
        // 检查是否有数据
        if (allRelations.isEmpty()) {
            // 仍然显示图，但只有孤立节点
            buildGraphWithoutEdges(settings);
            return;
        }
        
        // 构建图数据
        buildGraphData(settings, allRelations, settingMap);
    }
    
    private void buildGraphWithoutEdges(List<StorySetting> settings) {
        Map<String, Object> graphData = new HashMap<>();
        graphData.put("centerName", "所有设定");
        graphData.put("isGlobal", true);
        
        List<Map<String, Object>> nodes = new ArrayList<>();
        
        // 添加所有设定为孤立节点
        for (StorySetting setting : settings) {
            Map<String, Object> node = new HashMap<>();
            node.put("id", setting.getId());
            node.put("label", truncateText(setting.getTitle(), 10));
            node.put("title", setting.getTitle());
            node.put("isCenter", false);
            nodes.add(node);
        }
        
        graphData.put("nodes", nodes);
        graphData.put("edges", new ArrayList<>());
        
        // 发送到 HTML
        String jsonData = new Gson().toJson(graphData);
        String js = "initGraph(" + jsonData + ")";
        webView.evaluateJavascript(js, null);
    }
    
    private void buildGraphData(List<StorySetting> settings, List<SettingRelationship> relations, Map<Integer, StorySetting> settingMap) {
        Map<String, Object> graphData = new HashMap<>();
        graphData.put("centerName", "所有设定");
        graphData.put("isGlobal", true);
        
        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> edges = new ArrayList<>();
        
        // 已添加的节点ID集合
        Set<Integer> addedNodes = new HashSet<>();
        
        // 添加关系
        for (SettingRelationship rel : relations) {
            StorySetting sourceSetting = settingMap.get(rel.getSourceSettingId());
            StorySetting targetSetting = settingMap.get(rel.getTargetSettingId());
            
            boolean sourceDeleted = rel.isSourceSettingDeleted();
            boolean targetDeleted = rel.isTargetSettingDeleted();
            
            // 添加源节点
            if (sourceSetting != null && !addedNodes.contains(sourceSetting.getId())) {
                Map<String, Object> node = new HashMap<>();
                node.put("id", sourceSetting.getId());
                node.put("label", truncateText(sourceSetting.getTitle(), 10));
                node.put("title", sourceSetting.getTitle());
                node.put("isCenter", false);
                if (sourceDeleted) {
                    node.put("isDeleted", true);
                    node.put("title", sourceSetting.getTitle() + " [已删除]");
                }
                nodes.add(node);
                addedNodes.add(sourceSetting.getId());
            }
            
            // 添加目标节点
            if (targetSetting != null && !addedNodes.contains(targetSetting.getId())) {
                Map<String, Object> node = new HashMap<>();
                node.put("id", targetSetting.getId());
                node.put("label", truncateText(targetSetting.getTitle(), 10));
                node.put("title", targetSetting.getTitle());
                node.put("isCenter", false);
                if (targetDeleted) {
                    node.put("isDeleted", true);
                    node.put("title", targetSetting.getTitle() + " [已删除]");
                }
                nodes.add(node);
                addedNodes.add(targetSetting.getId());
            }
            
            // 如果两个设定都不存在（都已删除），跳过这条边
            if (sourceSetting == null && targetSetting == null) continue;
            
            // 添加边
            Map<String, Object> edge = new HashMap<>();
            edge.put("from", rel.getSourceSettingId());
            edge.put("to", rel.getTargetSettingId());
            edge.put("label", rel.getTypeDisplayName());
            edge.put("title", buildEdgeTitle(rel));
            edge.put("category", rel.getTypeCategory());
            edge.put("isDirected", rel.isDirected());
            if (sourceDeleted || targetDeleted) {
                edge.put("isDeleted", true);
            }
            edges.add(edge);
        }
        
        // 添加孤立节点（没有参与任何关系的设定）
        for (StorySetting setting : settings) {
            if (!addedNodes.contains(setting.getId())) {
                Map<String, Object> node = new HashMap<>();
                node.put("id", setting.getId());
                node.put("label", truncateText(setting.getTitle(), 10));
                node.put("title", setting.getTitle());
                node.put("isCenter", false);
                node.put("isIsolated", true);  // 标记为孤立节点
                nodes.add(node);
                addedNodes.add(setting.getId());
            }
        }
        
        graphData.put("nodes", nodes);
        graphData.put("edges", edges);
        
        // 发送到 HTML
        String jsonData = new Gson().toJson(graphData);
        String js = "initGraph(" + jsonData + ")";
        webView.evaluateJavascript(js, null);
    }
    
    private String buildEdgeTitle(SettingRelationship rel) {
        StringBuilder sb = new StringBuilder();
        sb.append(rel.getTypeDisplayName());
        if (!TextUtils.isEmpty(rel.getDescription())) {
            sb.append("\n").append(rel.getDescription());
        }
        return sb.toString();
    }
    
    private String truncateText(String text, int maxLen) {
        if (text == null) return "";
        if (text.length() <= maxLen) return text;
        return text.substring(0, maxLen - 1) + "…";
    }
    
    private void showEmptyHint(String message) {
        String js = "showEmptyHint('" + message + "')";
        webView.evaluateJavascript(js, null);
    }

    /**
     * JavaScript 接口
     */
    private class JsInterface {
        @JavascriptInterface
        public void onNodeClicked(int settingId) {
            // 双击节点，跳转到设定详情
            requireActivity().runOnUiThread(() -> {
                Intent intent = new Intent(requireContext(), SettingDetailActivity.class);
                intent.putExtra(SettingDetailActivity.EXTRA_SETTING_ID, settingId);
                intent.putExtra(SettingDetailActivity.EXTRA_STORY_ID, storyId);
                startActivity(intent);
            });
        }
        
        @JavascriptInterface
        public void onNodeSelected(int settingId) {
            // 单击节点，显示预览弹出卡片
            requireActivity().runOnUiThread(() -> {
                StorySetting setting = settingDao.getById(settingId);
                if (setting != null) {
                    int relationCount = 0;
                    List<SettingRelationship> relations = relationshipDao.getBySettingId(settingId);
                    if (relations != null) {
                        relationCount = relations.size();
                    }
                    showNodePreviewDialog(setting, relationCount);
                }
            });
        }
    }
    
    /**
     * 显示节点预览底部弹出卡片
     */
    private void showNodePreviewDialog(StorySetting setting, int relationCount) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
        View contentView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_node_preview, null);
        builder.setView(contentView);
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        
        // 使对话框从底部弹出
        if (dialog.getWindow() != null) {
            dialog.getWindow().setGravity(android.view.Gravity.BOTTOM);
            dialog.getWindow().setWindowAnimations(com.google.android.material.R.style.Animation_Design_BottomSheetDialog);
        }
        
        // 标题
        TextView tvTitle = contentView.findViewById(R.id.tv_preview_title);
        tvTitle.setText(setting.getTitle());
        
        // 分类
        TextView tvCategory = contentView.findViewById(R.id.tv_preview_category);
        String categoryText = setting.getCategory();
        if (!android.text.TextUtils.isEmpty(setting.getSubCategory())) {
            categoryText += " · " + setting.getSubCategory();
        }
        tvCategory.setText(categoryText);
        
        // 摘要
        TextView tvSummary = contentView.findViewById(R.id.tv_preview_summary);
        if (!android.text.TextUtils.isEmpty(setting.getSummary())) {
            tvSummary.setText(setting.getSummary());
        } else {
            tvSummary.setText("暂无摘要");
            tvSummary.setTextColor(getResources().getColor(android.R.color.darker_gray));
        }
        
        // 关联数量
        TextView tvRelationCount = contentView.findViewById(R.id.tv_preview_relation_count);
        tvRelationCount.setText("· " + relationCount + " 个关联");
        tvRelationCount.setVisibility(View.VISIBLE);
        
        // 标签
        LinearLayout layoutTags = contentView.findViewById(R.id.layout_preview_tags);
        if (!android.text.TextUtils.isEmpty(setting.getTags())) {
            try {
                List<String> tags = new Gson().fromJson(setting.getTags(),
                    new TypeToken<List<String>>(){}.getType());
                if (tags != null && !tags.isEmpty()) {
                    int maxShow = Math.min(tags.size(), 5);
                    for (int i = 0; i < maxShow; i++) {
                        com.google.android.material.chip.Chip chip = new com.google.android.material.chip.Chip(requireContext());
                        chip.setText(tags.get(i));
                        chip.setChipBackgroundColorResource(android.R.color.transparent);
                        chip.setChipStrokeWidth(1f);
                        chip.setChipStrokeColorResource(android.R.color.darker_gray);
                        chip.setTextSize(12);
                        chip.setClickable(false);
                        chip.setCheckable(false);
                        layoutTags.addView(chip);
                    }
                    if (tags.size() > maxShow) {
                        com.google.android.material.chip.Chip chip = new com.google.android.material.chip.Chip(requireContext());
                        chip.setText("+ " + (tags.size() - maxShow));
                        chip.setChipBackgroundColorResource(android.R.color.transparent);
                        chip.setChipStrokeWidth(1f);
                        chip.setChipStrokeColorResource(android.R.color.darker_gray);
                        chip.setTextSize(12);
                        chip.setClickable(false);
                        chip.setCheckable(false);
                        layoutTags.addView(chip);
                    }
                    layoutTags.setVisibility(View.VISIBLE);
                }
            } catch (Exception ignored) {}
        }
        
        // 关闭按钮
        contentView.findViewById(R.id.btn_preview_close).setOnClickListener(v -> dialog.dismiss());
        
        // 查看详情按钮
        contentView.findViewById(R.id.btn_preview_detail).setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(requireContext(), SettingDetailActivity.class);
            intent.putExtra(SettingDetailActivity.EXTRA_SETTING_ID, setting.getId());
            intent.putExtra(SettingDetailActivity.EXTRA_STORY_ID, storyId);
            startActivity(intent);
        });
        
        dialog.show();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (webView != null) {
            webView.onResume();
            // 刷新关系图数据（可能删除了设定）
            webView.evaluateJavascript("clearGraph()", null);
            loadGraphData();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (webView != null) {
            webView.onPause();
        }
    }

    // ==================== AI 关系提取功能 ====================
    
    private void startAiRelationExtraction() {
        FrameLayout layoutLoading = requireView().findViewById(R.id.layout_loading);
        TextView tvLoadingMessage = requireView().findViewById(R.id.tv_loading_message);
        if (layoutLoading != null) {
            if (tvLoadingMessage != null) tvLoadingMessage.setText("正在分析关系...");
            layoutLoading.setVisibility(View.VISIBLE);
        }
        
        RelationExtractor extractor = new RelationExtractor(requireContext(), storyId);
        extractor.extract(new RelationExtractor.ExtractCallback() {
            @Override
            public void onStart() {}
            
            @Override
            public void onSuccess(RelationExtractionResult result) {
                if (layoutLoading != null) layoutLoading.setVisibility(View.GONE);
                showExtractionResultDialog(result);
            }
            
            @Override
            public void onError(String error) {
                if (layoutLoading != null) layoutLoading.setVisibility(View.GONE);
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
            }
        });
    }
    
    private void showExtractionResultDialog(RelationExtractionResult result) {
        ExtractionResultDialogFragment dialog = ExtractionResultDialogFragment.newInstance();
        dialog.setData(result);
        dialog.setListener(new ExtractionResultDialogFragment.Listener() {
            @Override
            public void onConfirm(@NonNull RelationExtractionResult result,
                                 @NonNull List<Integer> selectedConfirmedPositions,
                                 @NonNull List<Integer> selectedPendingPositions) {
                applyExtractionResults(result, selectedConfirmedPositions, selectedPendingPositions);
            }

            @Override
            public void onCancel() {
                // 用户取消，无需处理
            }
        });
        dialog.show(getParentFragmentManager(), "extraction_result");
    }
    
    private void applyExtractionResults(RelationExtractionResult result,
                                          List<Integer> selectedConfirmedPositions,
                                          List<Integer> selectedPendingPositions) {
        // 保存已确认关系
        int savedRelations = 0;
        if (result.getConfirmedRelations() != null) {
            for (int pos : selectedConfirmedPositions) {
                RelationExtractionResult.ConfirmedRelation rel = result.getConfirmedRelations().get(pos);
                StorySetting source = settingDao.getByTitle(rel.getSourceName());
                StorySetting target = settingDao.getByTitle(rel.getTargetName());
                if (source != null && target != null) {
                    if (!isRelationExistsByTitle(rel.getSourceName(), rel.getTargetName(), rel.getRelationshipType())) {
                        SettingRelationship newRel = new SettingRelationship();
                        newRel.setStoryId(storyId);
                        newRel.setSourceSettingId(source.getId());
                        newRel.setTargetSettingId(target.getId());
                        newRel.setRelationshipType(rel.getRelationshipType());
                        newRel.setDescription(rel.getDescription());
                        relationshipDao.insert(newRel);
                        savedRelations++;
                    }
                }
            }
        }
        
        // 创建待创建实体
        int savedEntities = 0;
        android.util.Log.d("PlotGraphFragment", "applyExtractionResults: selectedPending=" + selectedPendingPositions);
        if (result.getPendingEntities() != null) {
            android.util.Log.d("PlotGraphFragment", "  pendingEntities.size()=" + result.getPendingEntities().size());
            for (int pos : selectedPendingPositions) {
                android.util.Log.d("PlotGraphFragment", "  处理位置: " + pos);
                RelationExtractionResult.PendingEntity entity = result.getPendingEntities().get(pos);
                android.util.Log.d("PlotGraphFragment", "  实体名: " + entity.getName() + ", category=" + entity.getSuggestedCategory());
                StorySetting newSetting = new StorySetting();
                newSetting.setStoryId(storyId);
                newSetting.setTitle(entity.getName());
                // 直接使用AI返回的分类和子分类
                String mainCategory = entity.getSuggestedCategory();
                if (mainCategory == null || mainCategory.isEmpty()) {
                    mainCategory = "角色";
                }
                String subCategory = entity.getSuggestedSubcategory();
                if (subCategory == null || subCategory.isEmpty()) {
                    subCategory = getDefaultSubCategory(mainCategory);
                }
                newSetting.setCategory(mainCategory);
                newSetting.setSubCategory(subCategory);
                // 保存简介
                newSetting.setSummary(entity.getSummary());
                // 保存别名（转换为JSON）
                if (entity.getAliases() != null && !entity.getAliases().isEmpty()) {
                    newSetting.setAliases(new com.google.gson.Gson().toJson(entity.getAliases()));
                }
                // 保存标签（转换为JSON）
                if (entity.getTags() != null && !entity.getTags().isEmpty()) {
                    newSetting.setTags(new com.google.gson.Gson().toJson(entity.getTags()));
                }
                newSetting.setCreateTime(System.currentTimeMillis());
                newSetting.setUpdateTime(System.currentTimeMillis());
                android.util.Log.d("PlotGraphFragment", "  即将插入: storyId=" + newSetting.getStoryId() + ", title=" + newSetting.getTitle() + ", category=" + newSetting.getCategory() + ", subCategory=" + newSetting.getSubCategory());
                long newSettingId = settingDao.insert(newSetting);
                android.util.Log.d("PlotGraphFragment", "  insert返回ID: " + newSettingId);
                savedEntities++;
                
                if (entity.getRelations() != null && newSettingId > 0) {
                    for (RelationExtractionResult.EntityRelation entRel : entity.getRelations()) {
                        StorySetting target = settingDao.getByTitle(entRel.getTargetName());
                        android.util.Log.d("PlotGraphFragment", "  查找目标实体: " + entRel.getTargetName() + ", 结果=" + (target != null));
                        if (target != null) {
                            SettingRelationship newRel = new SettingRelationship();
                            newRel.setStoryId(storyId);
                            newRel.setSourceSettingId((int) newSettingId);
                            newRel.setTargetSettingId(target.getId());
                            newRel.setRelationshipType(entRel.getRelationshipType());
                            newRel.setDescription(entRel.getDescription());
                            relationshipDao.insert(newRel);
                        }
                    }
                }
            }
        }
        
        String message = "";
        if (savedRelations > 0) message += "添加了 " + savedRelations + " 条关系\n";
        if (savedEntities > 0) message += "创建了 " + savedEntities + " 个新实体";
        if (message.isEmpty()) {
            Toast.makeText(requireContext(), "没有需要保存的内容", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(requireContext(), message.trim(), Toast.LENGTH_LONG).show();
            refreshGraph();
            // 同时刷新左侧面板的设定列表
            if (getActivity() instanceof StoryWorkspaceActivity) {
                ((StoryWorkspaceActivity) getActivity()).refreshSettingsView();
            }
        }
    }
    
    private boolean isRelationExistsByTitle(String sourceName, String targetName, String type) {
        StorySetting source = settingDao.getByTitle(sourceName);
        if (source == null) return false;
        
        List<SettingRelationship> relations = relationshipDao.getBySettingId(source.getId());
        if (relations != null) {
            StorySetting target = settingDao.getByTitle(targetName);
            if (target == null) return false;
            for (SettingRelationship rel : relations) {
                if (rel.getTargetSettingId() == target.getId() && rel.getRelationshipType().equals(type)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void onDestroyView() {
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
        super.onDestroyView();
    }
    
    /**
     * 获取主分类的默认子分类
     */
    private String getDefaultSubCategory(String mainCategory) {
        String[] subCategories = com.example.storyteller.utils.SettingCategoryConfig.getSubCategories(mainCategory);
        if (subCategories != null && subCategories.length > 0) {
            return subCategories[0];
        }
        return "";
    }
}