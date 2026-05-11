package com.example.storyteller.ui.activity;

import android.content.Intent;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.example.storyteller.R;
import com.example.storyteller.base.BaseActivity;
import com.example.storyteller.data.local.db.MaterialDao;
import com.example.storyteller.model.Material;

public class MaterialDetailActivity extends BaseActivity {

    public static final String EXTRA_MATERIAL_ID = "extra_material_id";

    private MaterialDao materialDao;
    private Material material;

    private TextView tvMetaSourceUrl;
    private TextView tvMetaSourceTitle;
    private TextView tvMetaSourceType;
    private TextView tvMetaAiScore;
    private EditText etTitle;
    private EditText etCategory;
    private EditText etContent;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_material_detail;
    }

    @Override
    protected void initView() {
        tvMetaSourceUrl = findViewById(R.id.tv_material_detail_source_url);
        tvMetaSourceTitle = findViewById(R.id.tv_material_detail_source_title);
        tvMetaSourceType = findViewById(R.id.tv_material_detail_source_type);
        tvMetaAiScore = findViewById(R.id.tv_material_detail_ai_score);
        etTitle = findViewById(R.id.et_material_detail_title);
        etCategory = findViewById(R.id.et_material_detail_category);
        etContent = findViewById(R.id.et_material_detail_content);
        Button btnSave = findViewById(R.id.btn_material_detail_save);
        Button btnDelete = findViewById(R.id.btn_material_detail_delete);
        Button btnBack = findViewById(R.id.btn_material_detail_back);

        btnBack.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> saveMaterial());
        btnDelete.setOnClickListener(v -> confirmDelete());
    }

    @Override
    protected void initData() {
        materialDao = new MaterialDao(this);
        int materialId = getIntent().getIntExtra(EXTRA_MATERIAL_ID, -1);
        if (materialId <= 0) {
            Toast.makeText(this, getString(R.string.material_detail_not_found), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        material = materialDao.getById(materialId);
        if (material == null) {
            Toast.makeText(this, getString(R.string.material_detail_not_found), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        bindMaterial();
    }

    private void bindMaterial() {
        setTitle(material.getTitle());
        etTitle.setText(material.getTitle());
        etCategory.setText(material.getCategory());
        etContent.setText(material.getContent());

        tvMetaSourceUrl.setText(formatNullable(material.getSourceUrl()));
        tvMetaSourceTitle.setText(formatNullable(material.getSourceTitle()));
        tvMetaSourceType.setText(formatNullable(material.getSourceType()));
        tvMetaAiScore.setText(String.valueOf(material.getAiScore()));
    }

    private void saveMaterial() {
        if (material == null) {
            return;
        }

        String title = textOf(etTitle);
        String category = textOf(etCategory);
        String content = textOf(etContent);
        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(category) || TextUtils.isEmpty(content)) {
            Toast.makeText(this, getString(R.string.material_detail_save_failed), Toast.LENGTH_SHORT).show();
            return;
        }

        material.setTitle(title);
        material.setCategory(category);
        material.setContent(content);
        int updated = materialDao.update(material);
        if (updated > 0) {
            Toast.makeText(this, getString(R.string.material_detail_saved), Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK, new Intent().putExtra(EXTRA_MATERIAL_ID, material.getId()));
            finish();
        } else {
            Toast.makeText(this, getString(R.string.material_detail_save_failed), Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmDelete() {
        if (material == null) {
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.material_detail_delete_confirm_title)
                .setMessage(R.string.material_detail_delete_confirm_message)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> deleteMaterial())
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void deleteMaterial() {
        if (material == null) {
            return;
        }
        int deleted = materialDao.delete(material.getId());
        if (deleted > 0) {
            Toast.makeText(this, getString(R.string.material_detail_deleted), Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK, new Intent().putExtra(EXTRA_MATERIAL_ID, material.getId()));
            finish();
        } else {
            Toast.makeText(this, getString(R.string.material_detail_delete_failed), Toast.LENGTH_SHORT).show();
        }
    }

    private String textOf(EditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }

    private String formatNullable(String value) {
        return TextUtils.isEmpty(value) ? getString(R.string.material_detail_empty_value) : value;
    }
}

