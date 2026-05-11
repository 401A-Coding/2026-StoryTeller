package com.example.storyteller.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.storyteller.R;
import com.example.storyteller.base.BaseActivity;
import com.example.storyteller.data.local.db.MaterialDao;
import com.example.storyteller.data.remote.NovelCrawler;
import com.example.storyteller.model.Material;
import com.example.storyteller.model.NovelSummary;
import com.example.storyteller.ui.adapter.MaterialAdapter;

import java.util.List;

public class MaterialActivity extends BaseActivity {

    private EditText etNovelUrl;
    private Button btnCrawl;
    private TextView tvCrawlStatus;
    private RecyclerView rvMaterial;
    private MaterialDao materialDao;
    private MaterialAdapter adapter;
    private NovelCrawler novelCrawler;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_material;
    }

    @Override
    protected void initView() {
        etNovelUrl = findViewById(R.id.et_novel_url);
        btnCrawl = findViewById(R.id.btn_crawl);
        tvCrawlStatus = findViewById(R.id.tv_crawl_status);
        rvMaterial = findViewById(R.id.rv_material);

        rvMaterial.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.btn_back_home).setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra(MainActivity.EXTRA_TARGET_TAB, MainActivity.TAB_HOME);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });

        btnCrawl.setOnClickListener(v -> {
            String url = etNovelUrl.getText().toString().trim();
            if (url.isEmpty()) {
                Toast.makeText(this, "请输入小说URL", Toast.LENGTH_SHORT).show();
                return;
            }
            startCrawling(url);
        });
    }

    @Override
    protected void initData() {
        materialDao = new MaterialDao(this);
        novelCrawler = new NovelCrawler();
        refreshMaterialList();
    }

    /**
     * 开始爬取小说
     */
    private void startCrawling(String url) {
        btnCrawl.setEnabled(false);
        tvCrawlStatus.setVisibility(TextView.VISIBLE);
        tvCrawlStatus.setText("正在爬取中，请稍候...");

        novelCrawler.crawlAndSave(url, this, new NovelCrawler.CrawlCallback() {
            @Override
            public void onSuccess(NovelSummary summary) {
                runOnUiThread(() -> {
                    btnCrawl.setEnabled(true);
                    tvCrawlStatus.setText("✅ 爬取完成！已存入素材库\n"
                            + "标题：" + summary.getTitle() + "\n"
                            + "作者：" + summary.getAuthor() + "\n"
                            + "识别到 " + (summary.getCharacters() != null ? summary.getCharacters().size() : 0) + " 个人物");
                    refreshMaterialList();
                    Toast.makeText(MaterialActivity.this,
                            "素材《" + summary.getTitle() + "》已存入素材库",
                            Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> {
                    btnCrawl.setEnabled(true);
                    tvCrawlStatus.setText("❌ 爬取失败：" + e.getMessage());
                    Toast.makeText(MaterialActivity.this,
                            "爬取失败：" + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    /**
     * 刷新素材列表
     */
    private void refreshMaterialList() {
        List<Material> materials = materialDao.getAll();
        if (adapter == null) {
            adapter = new MaterialAdapter(materials);
            rvMaterial.setAdapter(adapter);
        } else {
            adapter.setData(materials);
        }
    }
}
