package com.example.storyteller.ui.fragment;

import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.storyteller.R;
import com.example.storyteller.base.BaseFragment;
import com.example.storyteller.data.remote.ApiKeyManager;

/**
 * 设置Fragment
 * 从SettingsActivity改造而来，作为底部导航的"设置"Tab
 */
public class SettingsFragment extends BaseFragment {

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_settings;
    }

    @Override
    protected void initView(View view) {
        EditText etApiKey = view.findViewById(R.id.et_api_key);
        Button btnSave = view.findViewById(R.id.btn_save_api_key);

        btnSave.setOnClickListener(v -> {
            String apiKey = etApiKey.getText().toString().trim();
            if (!apiKey.isEmpty()) {
                ApiKeyManager.saveApiKey(requireContext(), apiKey);
                Toast.makeText(requireContext(), "API Key已保存", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "请输入API Key", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void initData() {
        // 加载已保存的API Key
        View view = getView();
        if (view != null) {
            EditText etApiKey = view.findViewById(R.id.et_api_key);
            String existingKey = ApiKeyManager.getApiKey(requireContext());
            etApiKey.setText(existingKey);
        }
    }
}
