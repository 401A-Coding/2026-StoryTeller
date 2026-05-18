package com.example.storyteller.ui.dialog;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.storyteller.R;

/**
 * 全局大纲编辑Dialog（全屏）
 */
public class GlobalOutlineEditDialog extends DialogFragment {

    private static final String ARG_STORY_ID = "arg_story_id";
    private static final String ARG_GLOBAL_OUTLINE = "arg_global_outline";

    private int storyId;
    private String globalOutline;
    private OnGlobalOutlineSaveListener listener;

    private EditText etGlobalOutline;
    private Button btnSave;

    public interface OnGlobalOutlineSaveListener {
        void onGlobalOutlineSaved(int storyId, String globalOutline);
    }

    public static GlobalOutlineEditDialog newInstance(int storyId, String globalOutline) {
        GlobalOutlineEditDialog dialog = new GlobalOutlineEditDialog();
        Bundle args = new Bundle();
        args.putInt(ARG_STORY_ID, storyId);
        args.putString(ARG_GLOBAL_OUTLINE, globalOutline);
        dialog.setArguments(args);
        return dialog;
    }

    public void setOnSaveListener(OnGlobalOutlineSaveListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_global_outline_edit, container, false);
        
        // 初始化UI组件
        etGlobalOutline = view.findViewById(R.id.et_global_outline);
        btnSave = view.findViewById(R.id.btn_save_global_outline);
        
        // 加载数据
        if (getArguments() != null) {
            storyId = getArguments().getInt(ARG_STORY_ID);
            globalOutline = getArguments().getString(ARG_GLOBAL_OUTLINE, "");
            
            if (!TextUtils.isEmpty(globalOutline)) {
                etGlobalOutline.setText(globalOutline);
            }
        }
        
        // 设置保存按钮
        if (btnSave != null) {
            btnSave.setOnClickListener(v -> saveAndDismiss());
        }
        
        return view;
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 设置为全屏样式
        setStyle(STYLE_NORMAL, R.style.FullScreenDialog);
    }

    private void saveAndDismiss() {
        String newGlobalOutline = etGlobalOutline.getText().toString().trim();
        
        if (listener != null) {
            listener.onGlobalOutlineSaved(storyId, newGlobalOutline);
            Toast.makeText(getContext(), "已保存全局大纲", Toast.LENGTH_SHORT).show();
        }
        
        dismiss();
    }
}
