package com.example.storyteller.ui.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.storyteller.R;
import com.example.storyteller.model.StorySetting;
import com.example.storyteller.ui.adapter.SettingImageOptionAdapter;
import com.example.storyteller.utils.SettingCategoryConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * 设定配图选择对话框
 * 支持本地选择和AI生成两种方式
 */
public class SettingImageSelectionDialog extends Dialog {

    private final StorySetting setting;
    private final OnImageSelectedListener listener;
    
    private RecyclerView rvImageOptions;
    private Button btnPickGallery;
    private Button btnGenerate;
    private Button btnCancel;
    private Button btnConfirm;
    private ProgressBar progressBar;
    private LinearLayout layoutButtons;
    
    private SettingImageOptionAdapter adapter;
    private String selectedImagePath;
    public interface OnImageSelectedListener {
        void onImageSelected(String imagePath);
        void onGenerateRequested();
    }
    public interface OnGalleryPickListener {
        void onGalleryPickRequested();
    }
    private OnGalleryPickListener galleryPickListener;
    public void setOnGalleryPickListener(OnGalleryPickListener listener) {
        this.galleryPickListener = listener;
    }
    
    /**
     * 设置AI生成按钮的可见性（根据主分类）
     */
    public void updateAiButtonVisibility() {
        if (btnGenerate != null && setting != null) {
            String category = setting.getCategory();
            boolean supported = SettingCategoryConfig.supportsAiImageGeneration(category);
            btnGenerate.setVisibility(supported ? View.VISIBLE : View.GONE);
        }
    }

    public SettingImageSelectionDialog(@NonNull Context context, StorySetting setting, 
                                        OnImageSelectedListener listener) {
        super(context, R.style.DialogTheme);
        this.setting = setting;
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_setting_image_selection);
        
        // 禁止点击外部关闭对话框
        setCanceledOnTouchOutside(false);
        
        rvImageOptions = findViewById(R.id.rv_image_options);
        btnPickGallery = findViewById(R.id.btn_pick_gallery);
        btnGenerate = findViewById(R.id.btn_generate);
        btnCancel = findViewById(R.id.btn_cancel);
        btnConfirm = findViewById(R.id.btn_confirm);
        progressBar = findViewById(R.id.progress_bar);
        layoutButtons = findViewById(R.id.layout_buttons);
        
        if (getWindow() != null) {
            getWindow().setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            );
        }
        
        layoutButtons.setVisibility(View.VISIBLE);
        setupRecyclerView();
        setupButtons();
        updateAiButtonVisibility();
    }

    private void setupRecyclerView() {
        adapter = new SettingImageOptionAdapter(new ArrayList<>(), imagePath -> {
            selectedImagePath = imagePath;
            updateConfirmButton();
        });
        
        rvImageOptions.setLayoutManager(new GridLayoutManager(getContext(), 2));
        rvImageOptions.setAdapter(adapter);
        
        if (setting.getImagePath() != null && !setting.getImagePath().isEmpty()) {
            List<String> existingImages = new ArrayList<>();
            existingImages.add(setting.getImagePath());
            adapter.setImages(existingImages);
            selectedImagePath = setting.getImagePath();
            updateConfirmButton();
        }
    }

    private void setupButtons() {
        btnCancel.setOnClickListener(v -> dismiss());
        btnPickGallery.setOnClickListener(v -> {
            if (galleryPickListener != null) {
                galleryPickListener.onGalleryPickRequested();
            }
        });
        btnConfirm.setOnClickListener(v -> {
            if (selectedImagePath != null && listener != null) {
                listener.onImageSelected(selectedImagePath);
                dismiss();
            }
        });
        
        btnGenerate.setOnClickListener(v -> {
            if (listener != null) {
                listener.onGenerateRequested();
            }
        });
        
        updateConfirmButton();
    }

    private void updateConfirmButton() {
        btnConfirm.setEnabled(selectedImagePath != null);
    }

    public void addGeneratedImage(String imagePath) {
        if (adapter != null) {
            adapter.addImage(imagePath);
            selectedImagePath = imagePath;
            updateConfirmButton();
        }
    }

    public void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (btnGenerate != null) {
            btnGenerate.setEnabled(!show);
        }
    }
}