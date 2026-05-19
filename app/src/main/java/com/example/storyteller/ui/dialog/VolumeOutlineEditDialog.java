package com.example.storyteller.ui.dialog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.storyteller.R;
import com.example.storyteller.model.Volume;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

/**
 * 卷纲编辑BottomSheet Dialog
 */
public class VolumeOutlineEditDialog extends BottomSheetDialogFragment {

    private static final String ARG_VOLUME_INDEX = "arg_volume_index";
    private static final String ARG_VOLUME_TITLE = "arg_volume_title";
    private static final String ARG_SUMMARY = "arg_summary";
    private static final String ARG_TARGET_WORDS = "arg_target_words";
    private static final String ARG_TARGET_CHAPTERS = "arg_target_chapters";

    private int volumeIndex;
    private Volume volume;
    private OnVolumeOutlineSaveListener listener;

    // UI Components
    private TextView tvVolumeTitle;
    private EditText etSummary;
    private EditText etTargetWordCount;
    private EditText etTargetChapterCount;
    private Button btnSave;
    private Button btnDecreaseWords;
    private Button btnIncreaseWords;
    private Button btnDecreaseChapters;
    private Button btnIncreaseChapters;

    public interface OnVolumeOutlineSaveListener {
        void onVolumeOutlineSaved(int volumeIndex, Volume updatedVolume);
    }

    public static VolumeOutlineEditDialog newInstance(int volumeIndex, Volume volume) {
        VolumeOutlineEditDialog dialog = new VolumeOutlineEditDialog();
        Bundle args = new Bundle();
        args.putInt(ARG_VOLUME_INDEX, volumeIndex);
        args.putString(ARG_VOLUME_TITLE, volume.getTitle());
        args.putString(ARG_SUMMARY, volume.getSummary());
        args.putInt(ARG_TARGET_WORDS, volume.getTargetWordCount());
        args.putInt(ARG_TARGET_CHAPTERS, volume.getTargetChapterCount());
        dialog.setArguments(args);
        // 保存volume引用用于后续更新
        dialog.volume = volume;
        return dialog;
    }

    public void setOnSaveListener(OnVolumeOutlineSaveListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_volume_outline, container, false);
        
        initView(view);
        loadData();
        setupListeners();
        
        return view;
    }

    private void initView(View view) {
        tvVolumeTitle = view.findViewById(R.id.tv_volume_title);
        etSummary = view.findViewById(R.id.et_volume_summary);
        etTargetWordCount = view.findViewById(R.id.et_target_word_count);
        etTargetChapterCount = view.findViewById(R.id.et_target_chapter_count);
        btnSave = view.findViewById(R.id.btn_save_volume);
        btnDecreaseWords = view.findViewById(R.id.btn_decrease_words);
        btnIncreaseWords = view.findViewById(R.id.btn_increase_words);
        btnDecreaseChapters = view.findViewById(R.id.btn_decrease_chapters);
        btnIncreaseChapters = view.findViewById(R.id.btn_increase_chapters);
    }

    private void loadData() {
        if (getArguments() != null) {
            volumeIndex = getArguments().getInt(ARG_VOLUME_INDEX);
            String title = getArguments().getString(ARG_VOLUME_TITLE, "");
            String summary = getArguments().getString(ARG_SUMMARY, "");
            int targetWords = getArguments().getInt(ARG_TARGET_WORDS, 0);
            int targetChapters = getArguments().getInt(ARG_TARGET_CHAPTERS, 0);

            tvVolumeTitle.setText(title);
            etSummary.setText(summary);
            etTargetWordCount.setText(targetWords > 0 ? String.valueOf(targetWords) : "");
            etTargetChapterCount.setText(targetChapters > 0 ? String.valueOf(targetChapters) : "");
        }
    }

    private void setupListeners() {
        // 保存按钮
        btnSave.setOnClickListener(v -> saveAndDismiss());

        // 防止卷摘要EditText的滚动事件冒泡到父容器（BottomSheet）
        etSummary.setOnTouchListener((v, event) -> {
            // 让EditText自己处理滚动事件，阻止事件向上传播
            v.getParent().requestDisallowInterceptTouchEvent(true);
            return false;
        });

        // 字数增减按钮
        btnDecreaseWords.setOnClickListener(v -> {
            int current = parseInt(etTargetWordCount.getText().toString());
            if (current > 0) {
                etTargetWordCount.setText(String.valueOf(current - 1000));
            }
        });

        btnIncreaseWords.setOnClickListener(v -> {
            int current = parseInt(etTargetWordCount.getText().toString());
            etTargetWordCount.setText(String.valueOf(current + 1000));
        });

        // 章节数增减按钮
        btnDecreaseChapters.setOnClickListener(v -> {
            int current = parseInt(etTargetChapterCount.getText().toString());
            if (current > 0) {
                etTargetChapterCount.setText(String.valueOf(current - 1));
            }
        });

        btnIncreaseChapters.setOnClickListener(v -> {
            int current = parseInt(etTargetChapterCount.getText().toString());
            etTargetChapterCount.setText(String.valueOf(current + 1));
        });
    }

    private void saveAndDismiss() {
        if (listener != null) {
            // 更新现有Volume对象的字段，而不是创建新对象
            // 这样可以保留chapters列表等其他字段
            volume.setSummary(etSummary.getText().toString().trim());
            volume.setTargetWordCount(parseInt(etTargetWordCount.getText().toString()));
            volume.setTargetChapterCount(parseInt(etTargetChapterCount.getText().toString()));

            listener.onVolumeOutlineSaved(volumeIndex, volume);
        }
        dismiss();
    }

    private int parseInt(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
