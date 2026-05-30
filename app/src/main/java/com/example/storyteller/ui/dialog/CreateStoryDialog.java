package com.example.storyteller.ui.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.storyteller.R;

/**
 * 创建小说弹窗
 * 仅负责收集输入并通过回调把数据交给调用方处理。
 */
public class CreateStoryDialog extends DialogFragment {

    public interface OnCreateStoryListener {
        void onCreateStory(@NonNull String title, @NonNull String seriesName, @NonNull String description);
    }

    private OnCreateStoryListener listener;

    public static CreateStoryDialog newInstance() {
        return new CreateStoryDialog();
    }

    public void setOnCreateStoryListener(OnCreateStoryListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_create_story, null);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create();

        EditText etTitle = dialogView.findViewById(R.id.et_story_title);
        EditText etSeriesName = dialogView.findViewById(R.id.et_series_name);
        EditText etDescription = dialogView.findViewById(R.id.et_story_description);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        Button btnCreate = dialogView.findViewById(R.id.btn_create);

        btnCancel.setOnClickListener(v -> dismiss());
        btnCreate.setOnClickListener(v -> {
            String title = etTitle.getText() == null ? "" : etTitle.getText().toString().trim();
            if (TextUtils.isEmpty(title)) {
                Toast.makeText(requireContext(), "请输入小说标题", Toast.LENGTH_SHORT).show();
                return;
            }

            String seriesName = etSeriesName.getText() == null ? "" : etSeriesName.getText().toString().trim();
            String description = etDescription.getText() == null ? "" : etDescription.getText().toString().trim();

            if (listener != null) {
                listener.onCreateStory(title, seriesName, description);
            }
            dismiss();
        });

        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(
                (int) (getResources().getDisplayMetrics().widthPixels * 0.95),
                ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
    }
}

