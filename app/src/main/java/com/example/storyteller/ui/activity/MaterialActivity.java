package com.example.storyteller.ui.activity;

import android.content.Intent;
import android.widget.CheckBox;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.storyteller.R;
import com.example.storyteller.base.BaseActivity;
import com.example.storyteller.data.local.db.MaterialDao;
import com.example.storyteller.data.remote.NovelCrawler;
import com.example.storyteller.data.remote.MaterialCandidateExtractor;
import com.example.storyteller.model.Material;
import com.example.storyteller.model.NovelSummary;
import com.example.storyteller.ui.adapter.MaterialAdapter;
import com.example.storyteller.ui.dialog.MaterialCandidateReviewDialogFragment;

import java.util.ArrayList;
import java.util.List;

public class MaterialActivity extends BaseActivity {

    private EditText etNovelUrl;
    private CheckBox cbPersona;
    private CheckBox cbPlot;
    private CheckBox cbTheme;
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
        cbPersona = findViewById(R.id.cb_material_persona);
        cbPlot = findViewById(R.id.cb_material_plot);
        cbTheme = findViewById(R.id.cb_material_theme);
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
            List<String> selectedTypes = getSelectedTypes();
            if (selectedTypes.isEmpty()) {
                Toast.makeText(this, "请至少选择一种素材类型", Toast.LENGTH_SHORT).show();
                return;
            }
            startCrawling(url, selectedTypes);
        });
    }

    @Override
    protected void initData() {
        materialDao = new MaterialDao(this);
        novelCrawler = new NovelCrawler();
        refreshMaterialList();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (materialDao != null) {
            refreshMaterialList();
        }
    }

    /**
     * 开始爬取小说
     */
    private void startCrawling(String url, List<String> selectedTypes) {
        btnCrawl.setEnabled(false);
        tvCrawlStatus.setVisibility(TextView.VISIBLE);
        tvCrawlStatus.setText(R.string.status_material_crawling_ai);

        novelCrawler.crawlAndExtract(url, this, selectedTypes, new NovelCrawler.ExtractCallback() {
            @Override
            public void onSuccess(NovelSummary summary, List<Material> materials, String rawJson) {
                runOnUiThread(() -> {
                    btnCrawl.setEnabled(true);

                    MaterialCandidateReviewDialogFragment dialog = MaterialCandidateReviewDialogFragment.newInstance();
                    dialog.setData(summary, materials == null ? new ArrayList<>() : materials, rawJson);
                    dialog.setListener(new MaterialCandidateReviewDialogFragment.Listener() {
                        @Override
                        public void onConfirm(@NonNull NovelSummary s, @NonNull List<Material> selectedMaterials, String rj) {
                            long lastId = materialDao.replaceBySource(s.getSourceUrl(), selectedMaterials);
                            if (lastId > 0) {
                                tvCrawlStatus.setText(getString(R.string.status_material_crawl_success,
                                        s.getTitle(), s.getAuthor(), selectedMaterials.size()));
                                refreshMaterialList();
                                Toast.makeText(MaterialActivity.this,
                                        getString(R.string.material_review_saved),
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(MaterialActivity.this,
                                        getString(R.string.material_detail_save_failed),
                                        Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onCancel() {
                            tvCrawlStatus.setText(R.string.status_material_crawl_cancelled);
                        }
                    });
                    tvCrawlStatus.setText(R.string.status_material_candidates_ready);
                    dialog.show(getSupportFragmentManager(), "material_candidate_review");
                });
            }

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> {
                    btnCrawl.setEnabled(true);
                    tvCrawlStatus.setText(getString(R.string.status_material_crawl_failed, e.getMessage()));
                    Toast.makeText(MaterialActivity.this,
                            getString(R.string.status_material_crawl_failed, e.getMessage()),
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private List<String> getSelectedTypes() {
        List<String> types = new ArrayList<>();
        if (cbPersona != null && cbPersona.isChecked()) {
            types.add(MaterialCandidateExtractor.TYPE_PERSONA);
        }
        if (cbPlot != null && cbPlot.isChecked()) {
            types.add(MaterialCandidateExtractor.TYPE_PLOT);
        }
        if (cbTheme != null && cbTheme.isChecked()) {
            types.add(MaterialCandidateExtractor.TYPE_THEME);
        }
        return types;
    }

    /**
     * 刷新素材列表
     */
    private void refreshMaterialList() {
        List<Material> materials = materialDao.getAll();
        if (adapter == null) {
            adapter = new MaterialAdapter(materials);
            adapter.setListener(material -> {
                Intent intent = new Intent(this, MaterialDetailActivity.class);
                intent.putExtra(MaterialDetailActivity.EXTRA_MATERIAL_ID, material.getId());
                startActivity(intent);
            });
            rvMaterial.setAdapter(adapter);
        } else {
            adapter.setData(materials);
        }
    }
}
