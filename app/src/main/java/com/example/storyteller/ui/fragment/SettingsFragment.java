package com.example.storyteller.ui.fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.storyteller.R;
import com.example.storyteller.base.BaseFragment;
import com.example.storyteller.data.remote.ApiKeyManager;
import com.example.storyteller.data.remote.ModelConfig;
import com.example.storyteller.ui.adapter.ModelAdapter;
import com.example.storyteller.ui.dialog.WritingPreferenceDialog;
import com.example.storyteller.utils.ThemeManager;

import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

/**
 * 设置Fragment
 */
public class SettingsFragment extends BaseFragment {

    private RecyclerView rvModels;
    private ModelAdapter modelAdapter;
    private List<ModelConfig.Provider> addedProviders = new ArrayList<>();
    private TextView tvThemeModeValue;
    private ThemeManager themeManager;

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_settings;
    }

    @Override
    protected void initView(View view) {
        rvModels = view.findViewById(R.id.rv_models);
        
        // 设置模型列表
        modelAdapter = new ModelAdapter();
        rvModels.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvModels.setAdapter(modelAdapter);
        modelAdapter.setOnModelActionListener(new ModelAdapter.OnModelActionListener() {
            @Override
            public void onEnabledChanged(ModelConfig.Provider provider, boolean enabled) {
                ModelConfig.setProviderEnabled(requireContext(), provider, enabled);
            }
            
            @Override
            public void onDeleteProvider(ModelConfig.Provider provider) {
                showDeleteConfirmDialog(provider);
            }
        });

        // 添加模型按钮
        Button btnAddModel = view.findViewById(R.id.btn_add_model);
        btnAddModel.setOnClickListener(v -> showAddModelDialog());

        // 全局写作偏好设置入口
        Button btnGlobalPreference = view.findViewById(R.id.btn_global_writing_preference);
        btnGlobalPreference.setOnClickListener(v -> {
            WritingPreferenceDialog dialog = WritingPreferenceDialog.newInstance();
            dialog.show(getChildFragmentManager(), "global_writing_preference");
        });

        // 主题设置
        tvThemeModeValue = view.findViewById(R.id.tv_theme_mode_value);
        themeManager = ThemeManager.getInstance(requireContext());
        updateThemeModeDisplay();
        view.findViewById(R.id.tv_theme_mode_label).setOnClickListener(v -> showThemeModeDialog());
        view.findViewById(R.id.iv_theme_arrow).setOnClickListener(v -> showThemeModeDialog());

        // 使用帮助
        view.findViewById(R.id.btn_help).setOnClickListener(v -> showFragment(new HelpFragment()));

        // 关于
        view.findViewById(R.id.btn_about).setOnClickListener(v -> showFragment(new AboutFragment()));

        // 意见反馈
        view.findViewById(R.id.btn_feedback).setOnClickListener(v -> showFragment(new FeedbackFragment()));
    }

    @Override
    protected void initData() {
        loadAddedProviders();
    }

    private void loadAddedProviders() {
        addedProviders.clear();
        // 检查每个提供商是否已有 API Key
        for (ModelConfig.Provider provider : ModelConfig.getAllProviders()) {
            String apiKey = ApiKeyManager.getApiKey(requireContext(), provider);
            if (apiKey != null && !apiKey.isEmpty()) {
                addedProviders.add(provider);
            }
        }
        updateModelsUI();
    }

    private void updateModelsUI() {
        modelAdapter.setProviders(addedProviders);
    }

    private void updateThemeModeDisplay() {
        int currentMode = themeManager.getThemeMode();
        tvThemeModeValue.setText(ThemeManager.getThemeModeDisplayText(currentMode));
    }

    private void showThemeModeDialog() {
        String[] options = ThemeManager.getThemeModeOptions();
        int currentMode = themeManager.getThemeMode();

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.theme_mode)
                .setSingleChoiceItems(options, currentMode, (dialog, which) -> {
                    themeManager.setThemeMode(which);
                    updateThemeModeDisplay();
                    Toast.makeText(requireContext(),
                            getString(R.string.theme_switched, ThemeManager.getThemeModeDisplayText(which)),
                            Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void showAddModelDialog() {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_add_model, null);
        
        Spinner spinnerProvider = dialogView.findViewById(R.id.spinner_provider);
        EditText etApiKey = dialogView.findViewById(R.id.et_api_key);
        
        // 设置 Spinner 数据
        ModelConfig.Provider[] providers = ModelConfig.getAllProviders();
        String[] providerNames = new String[providers.length];
        for (int i = 0; i < providers.length; i++) {
            providerNames[i] = providers[i].getDisplayName();
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, providerNames);
        spinnerProvider.setAdapter(adapter);
        
        // 检查已添加的提供商，禁用选择
        spinnerProvider.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                ModelConfig.Provider selectedProvider = providers[position];
                String existingKey = ApiKeyManager.getApiKey(requireContext(), selectedProvider);
                if (existingKey != null && !existingKey.isEmpty()) {
                    etApiKey.setText(existingKey);
                    etApiKey.setEnabled(false);
                } else {
                    etApiKey.setText("");
                    etApiKey.setEnabled(true);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        new AlertDialog.Builder(requireContext())
                .setTitle("添加模型")
                .setView(dialogView)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_save, (dialog, which) -> {
                    ModelConfig.Provider selectedProvider = providers[spinnerProvider.getSelectedItemPosition()];
                    String apiKey = etApiKey.getText().toString().trim();
                    
                    if (apiKey.isEmpty()) {
                        Toast.makeText(requireContext(), "请输入 API Key", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    ApiKeyManager.saveApiKey(requireContext(), selectedProvider, apiKey);
                    Toast.makeText(requireContext(), selectedProvider.getDisplayName() + " API Key 已保存", Toast.LENGTH_SHORT).show();
                    loadAddedProviders();
                })
                .show();
    }

    private void showDeleteConfirmDialog(ModelConfig.Provider provider) {
        new AlertDialog.Builder(requireContext())
                .setTitle("删除模型")
                .setMessage("确认删除 " + provider.getDisplayName() + " 的 API Key 吗？")
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_delete, (dialog, which) -> {
                    ApiKeyManager.saveApiKey(requireContext(), provider, "");
                    Toast.makeText(requireContext(), provider.getDisplayName() + " 已删除", Toast.LENGTH_SHORT).show();
                    loadAddedProviders();
                })
                .show();
    }

    private void showFragment(Fragment fragment) {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }
}