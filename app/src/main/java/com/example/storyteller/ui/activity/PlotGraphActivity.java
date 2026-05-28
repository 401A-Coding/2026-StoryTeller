package com.example.storyteller.ui.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.appcompat.app.AlertDialog;

import com.example.storyteller.R;
import com.example.storyteller.data.local.db.SettingRelationshipDao;
import com.example.storyteller.data.local.db.StorySettingDao;
import com.example.storyteller.model.SettingRelationship;
import com.example.storyteller.model.StorySetting;
import com.example.storyteller.utils.ThemeColorUtils;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 设定关系图页面
 * 使用 WebView + vis.js 展示设定之间的关系网络
 */
public class PlotGraphActivity extends AppCompatActivity {

    public static final String EXTRA_SETTING_ID = "setting_id";
    public static final String EXTRA_STORY_ID = "story_id";

    private WebView webView;
    private TextView tvTitle;
    
    private StorySettingDao settingDao;
    private SettingRelationshipDao relationshipDao;
    
    private int settingId;
    private int storyId;
    private StorySetting currentSetting;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plot_graph);

        // 获取参数
        Intent intent = getIntent();
        settingId = intent.getIntExtra(EXTRA_SETTING_ID, -1);
        storyId = intent.getIntExtra(EXTRA_STORY_ID, 0);

        // 初始化
        settingDao = new StorySettingDao(this);
        relationshipDao = new SettingRelationshipDao(this);
        
        initView();
        loadData();
    }

    private void initView() {
        // 刘海屏适配
        View rootView = findViewById(android.R.id.content);
        rootView.setOnApplyWindowInsetsListener((v, insets) -> {
            androidx.core.graphics.Insets systemBars = androidx.core.view.WindowInsetsCompat.toWindowInsetsCompat(insets, v)
                .getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, 0);
            return insets;
        });
        rootView.requestApplyInsets();
        
        tvTitle = findViewById(R.id.tv_graph_title);
        webView = findViewById(R.id.webview_graph);
        
        // 返回按钮
        Button btnBack = findViewById(R.id.btn_graph_back);
        btnBack.setOnClickListener(v -> finish());
        
        // 配置 WebView
        setupWebView();
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
        
        webView.setBackgroundColor(ThemeColorUtils.getBackgroundPrimary(this));
        
        // WebViewClient
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // 设置深色模式配色
                boolean isDarkMode = (view.getContext().getResources().getConfiguration().uiMode
                        & android.content.res.Configuration.UI_MODE_NIGHT_MASK) 
                        == android.content.res.Configuration.UI_MODE_NIGHT_YES;
                String colorScheme = isDarkMode ? "dark" : "light";
                webView.evaluateJavascript("setColorScheme('" + colorScheme + "')", null);
                // 页面加载完成后，加载数据
                loadGraphData();
            }
        });
        
        // 添加 JavaScript 接口
        webView.addJavascriptInterface(new JsInterface(), "Android");
        
        // 加载 HTML
        webView.loadUrl("file:///android_asset/plot_graph.html");
    }

    private void loadData() {
        if (settingId == -1) {
            Toast.makeText(this, "设定不存在", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        currentSetting = settingDao.getById(settingId);
        if (currentSetting == null) {
            Toast.makeText(this, "设定不存在", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        tvTitle.setText(currentSetting.getTitle());
    }

    private void loadGraphData() {
        if (currentSetting == null) return;
        
        // 获取所有关系
        List<SettingRelationship> relations = relationshipDao.getBySettingId(settingId);
        
        // 构建图数据
        Map<String, Object> graphData = new HashMap<>();
        graphData.put("centerName", currentSetting.getTitle());
        
        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> edges = new ArrayList<>();
        
        // 添加中心节点
        Map<String, Object> centerNode = new HashMap<>();
        centerNode.put("id", currentSetting.getId());
        centerNode.put("label", truncateText(currentSetting.getTitle(), 10));
        centerNode.put("title", currentSetting.getTitle());
        centerNode.put("isCenter", true);
        nodes.add(centerNode);
        
        // 已添加的节点ID集合
        Map<Integer, Boolean> addedNodes = new HashMap<>();
        addedNodes.put(currentSetting.getId(), true);
        
        // 添加关联节点和边
        for (SettingRelationship rel : relations) {
            int relatedId;
            boolean isSource = rel.getSourceSettingId() == currentSetting.getId();
            
            if (isSource) {
                relatedId = rel.getTargetSettingId();
            } else {
                relatedId = rel.getSourceSettingId();
            }
            
            // 获取关联设定
            StorySetting relatedSetting = settingDao.getById(relatedId);
            if (relatedSetting == null) continue;
            
            // 如果节点尚未添加
            if (!addedNodes.containsKey(relatedId)) {
                Map<String, Object> node = new HashMap<>();
                node.put("id", relatedId);
                node.put("label", truncateText(relatedSetting.getTitle(), 10));
                node.put("title", relatedSetting.getTitle());
                node.put("isCenter", false);
                nodes.add(node);
                addedNodes.put(relatedId, true);
            }
            
            // 添加边
            Map<String, Object> edge = new HashMap<>();
            if (isSource) {
                edge.put("from", currentSetting.getId());
                edge.put("to", relatedId);
            } else {
                edge.put("from", relatedId);
                edge.put("to", currentSetting.getId());
            }
            edge.put("label", rel.getTypeDisplayName());
            edge.put("title", buildEdgeTitle(rel));
            edge.put("category", rel.getTypeCategory());
            edge.put("isDirected", rel.isDirected());
            edges.add(edge);
        }
        
        graphData.put("nodes", nodes);
        graphData.put("edges", edges);
        
        // 转换为 JSON
        String jsonData = new Gson().toJson(graphData);
        
        // 调用 JavaScript 方法
        String jsCall = "initGraph(" + jsonData + ");";
        webView.evaluateJavascript(jsCall, null);
    }

    private String truncateText(String text, int maxLength) {
        if (TextUtils.isEmpty(text) || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "…";
    }

    private String buildEdgeTitle(SettingRelationship rel) {
        StringBuilder sb = new StringBuilder();
        sb.append(rel.getTypeDisplayName());
        if (!TextUtils.isEmpty(rel.getDescription())) {
            sb.append("\n").append(rel.getDescription());
        }
        sb.append("\n类型: ").append(rel.getTypeCategoryDisplayName());
        return sb.toString();
    }

    /**
     * JavaScript 接口
     */
    private class JsInterface {
        
        @JavascriptInterface
        public void onNodeClicked(int nodeId) {
            // 跳转到设定详情页
            runOnUiThread(() -> {
                Intent intent = new Intent(PlotGraphActivity.this, SettingDetailActivity.class);
                intent.putExtra(SettingDetailActivity.EXTRA_SETTING_ID, nodeId);
                intent.putExtra(SettingDetailActivity.EXTRA_STORY_ID, storyId);
                startActivity(intent);
            });
        }
        
        @JavascriptInterface
        public void onNodeSelected(int nodeId) {
            // 可用于更新 UI 或显示详情
            // 目前先不做处理
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) {
            webView.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (webView != null) {
            webView.onPause();
        }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroy();
    }
}