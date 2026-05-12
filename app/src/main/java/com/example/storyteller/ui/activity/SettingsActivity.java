package com.example.storyteller.ui.activity;

import android.content.Intent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.example.storyteller.R;
import com.example.storyteller.base.BaseActivity;
import com.example.storyteller.data.remote.ApiKeyManager;  // 新增导入

public class SettingsActivity extends BaseActivity {

    @Override
    protected int getLayoutId() {
        return R.layout.activity_settings;
    }

    @Override
    protected void initView() {
        // 刘海屏适配：为根布局设置系统栏内边距
        applySystemWindowInsets(findViewById(android.R.id.content));
        findViewById(R.id.btn_back_home).setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra(MainActivity.EXTRA_TARGET_TAB, MainActivity.TAB_HOME);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });

        EditText etApiKey = findViewById(R.id.et_api_key);
        Button btnSave = findViewById(R.id.btn_save_api_key);

        btnSave.setOnClickListener(v -> {
            String apiKey = etApiKey.getText().toString().trim();
            if (!apiKey.isEmpty()) {
                ApiKeyManager.saveApiKey(this, apiKey);
                Toast.makeText(this, "API Key saved", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Please enter API Key", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void initData() {
        EditText etApiKey = findViewById(R.id.et_api_key);
        String existingKey = ApiKeyManager.getApiKey(this);
        etApiKey.setText(existingKey);
    }
}
