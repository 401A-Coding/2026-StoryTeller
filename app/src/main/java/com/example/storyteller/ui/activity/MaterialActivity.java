package com.example.storyteller.ui.activity;

import android.content.Intent;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

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

    private Button btnImport;
    private EditText etSearch;
    private Button btnFilterAll;
    private Button btnFilterPersona;
    private Button btnFilterPlot;
    private Button btnFilterTheme;
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
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        btnImport = findViewById(R.id.btn_import);
        etSearch = findViewById(R.id.et_material_search);
        btnFilterAll = findViewById(R.id.btn_filter_all);
        btnFilterPersona = findViewById(R.id.btn_filter_persona);
        btnFilterPlot = findViewById(R.id.btn_filter_plot);
        btnFilterTheme = findViewById(R.id.btn_filter_theme);
        tvCrawlStatus = findViewById(R.id.tv_crawl_status);
        rvMaterial = findViewById(R.id.rv_material);

        rvMaterial.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.btn_back_home).setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra(MainActivity.EXTRA_TARGET_TAB, MainActivity.TAB_HOME);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });

        btnImport.setOnClickListener(v -> showImportUrlDialog());

        // 搜索实时过滤
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (adapter != null) adapter.search(s == null ? null : s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnFilterAll.setOnClickListener(v -> {
            if (adapter != null) adapter.filterByType(null);
        });
        btnFilterPersona.setOnClickListener(v -> {
            if (adapter != null) adapter.filterByType(MaterialCandidateExtractor.TYPE_PERSONA);
        });
        btnFilterPlot.setOnClickListener(v -> {
            if (adapter != null) adapter.filterByType(MaterialCandidateExtractor.TYPE_PLOT);
        });
        btnFilterTheme.setOnClickListener(v -> {
            if (adapter != null) adapter.filterByType(MaterialCandidateExtractor.TYPE_THEME);
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
        // 禁用导入按钮，避免重复提交
        if (btnImport != null) btnImport.setEnabled(false);
        tvCrawlStatus.setVisibility(TextView.VISIBLE);
        tvCrawlStatus.setText(R.string.status_material_crawling_ai);

        novelCrawler.crawlAndExtract(url, this, selectedTypes, new NovelCrawler.ExtractCallback() {
            @Override
            public void onSuccess(NovelSummary summary, List<Material> materials, String rawJson) {
                runOnUiThread(() -> {
                    if (btnImport != null) btnImport.setEnabled(true);

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
                    if (btnImport != null) btnImport.setEnabled(true);
                    tvCrawlStatus.setText(getString(R.string.status_material_crawl_failed, e.getMessage()));
                    Toast.makeText(MaterialActivity.this,
                            getString(R.string.status_material_crawl_failed, e.getMessage()),
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    /**
     * 显示导入 URL 的输入对话，用户输入后选择导入的素材类型
     */
    private void showImportUrlDialog() {
        final EditText input = new EditText(this);
        input.setHint("请输入素材来源 URL");
        input.setInputType(InputType.TYPE_TEXT_VARIATION_URI);

        new AlertDialog.Builder(this)
                .setTitle("导入素材")
                .setView(input)
                .setPositiveButton("下一步", (dialog, which) -> {
                    String url = input.getText() == null ? "" : input.getText().toString().trim();
                    if (url.isEmpty()) {
                        Toast.makeText(this, "请输入 URL", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // 显示素材类型多选对话
                    showTypeSelectionDialog(url);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showTypeSelectionDialog(String url) {
        final String[] types = new String[]{
                getString(R.string.material_type_persona),
                getString(R.string.material_type_plot),
                getString(R.string.material_type_theme)
        };
        final boolean[] checked = new boolean[]{true, true, true};
        new AlertDialog.Builder(this)
                .setTitle("选择要导入的素材类型")
                .setMultiChoiceItems(types, checked, (dialog, which, isChecked) -> checked[which] = isChecked)
                .setPositiveButton("导入", (dialog, which) -> {
                    List<String> selected = new ArrayList<>();
                    if (checked[0]) selected.add(MaterialCandidateExtractor.TYPE_PERSONA);
                    if (checked[1]) selected.add(MaterialCandidateExtractor.TYPE_PLOT);
                    if (checked[2]) selected.add(MaterialCandidateExtractor.TYPE_THEME);
                    if (selected.isEmpty()) {
                        Toast.makeText(this, "请至少选择一种素材类型", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    startCrawling(url, selected);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
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
