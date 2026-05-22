package com.example.storyteller.ui.fragment;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.storyteller.R;
import com.example.storyteller.base.BaseFragment;
import com.example.storyteller.data.local.db.SettingRelationshipDao;
import com.example.storyteller.data.local.db.StorySettingDao;
import com.example.storyteller.model.SettingRelationship;
import com.example.storyteller.model.StorySetting;
import com.example.storyteller.ui.activity.SettingDetailActivity;
import com.google.gson.Gson;

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
        public void onNodeClick(int settingId) {
            // 点击节点，跳转到设定详情
            requireActivity().runOnUiThread(() -> {
                Intent intent = new Intent(requireContext(), SettingDetailActivity.class);
                intent.putExtra(SettingDetailActivity.EXTRA_SETTING_ID, settingId);
                intent.putExtra(SettingDetailActivity.EXTRA_STORY_ID, storyId);
                startActivity(intent);
            });
        }
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

    @Override
    public void onDestroyView() {
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
        super.onDestroyView();
    }
}