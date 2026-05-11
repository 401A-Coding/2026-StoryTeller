package com.example.storyteller.ui.dialog;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.storyteller.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * “聊天输入框”风格的底部弹窗：用于让用户补充人物画像的生成需求。
 */
public class CharacterRegenerateBottomSheetDialogFragment extends BottomSheetDialogFragment {

    private static final String ARG_STORY_TITLE = "arg_story_title";
    private static final String ARG_CHARACTER_NAME = "arg_character_name";

    public interface Listener {
        void onSubmit(@NonNull String extraDemand);
    }

    private Listener listener;

    public static CharacterRegenerateBottomSheetDialogFragment newInstance(@Nullable String storyTitle) {
        return newInstance(storyTitle, null);
    }

    public static CharacterRegenerateBottomSheetDialogFragment newInstance(@Nullable String storyTitle,
                                                                          @Nullable String characterName) {
        CharacterRegenerateBottomSheetDialogFragment fragment = new CharacterRegenerateBottomSheetDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_STORY_TITLE, storyTitle);
        args.putString(ARG_CHARACTER_NAME, characterName);
        fragment.setArguments(args);
        return fragment;
    }

    public void setListener(@Nullable Listener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_character_regenerate, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView tvTitle = view.findViewById(R.id.tv_regenerate_title);
        TextView tvSubtitle = view.findViewById(R.id.tv_regenerate_subtitle);
        TextInputLayout tilInput = view.findViewById(R.id.til_regenerate_input);
        TextInputEditText etInput = view.findViewById(R.id.et_regenerate_input);
        Button btnCancel = view.findViewById(R.id.btn_regenerate_cancel);
        Button btnSend = view.findViewById(R.id.btn_regenerate_send);

        String storyTitle = null;
        String characterName = null;
        Bundle args = getArguments();
        if (args != null) {
            storyTitle = args.getString(ARG_STORY_TITLE, null);
            characterName = args.getString(ARG_CHARACTER_NAME, null);
        }

        if (!TextUtils.isEmpty(characterName)) {
            tvTitle.setText(getString(R.string.title_character_regenerate_one, characterName));
            tilInput.setHint(getString(R.string.hint_character_regenerate_one));
        } else {
            tvTitle.setText(R.string.title_character_regenerate);
            tilInput.setHint(getString(R.string.hint_character_regenerate));
        }

        if (!TextUtils.isEmpty(storyTitle)) {
            tvSubtitle.setText(getString(R.string.subtitle_character_current_story, storyTitle));
        } else {
            tvSubtitle.setText(R.string.subtitle_character_regenerate_default);
        }

        btnCancel.setOnClickListener(v -> dismiss());

        Runnable submit = () -> {
            String demand = etInput.getText() == null ? "" : etInput.getText().toString().trim();
            if (listener != null) {
                listener.onSubmit(demand);
            }
            dismiss();
        };

        btnSend.setOnClickListener(v -> submit.run());

        etInput.setOnEditorActionListener((v, actionId, event) -> {
            boolean isSend = actionId == EditorInfo.IME_ACTION_SEND;
            boolean isEnter = event != null
                    && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                    && event.getAction() == KeyEvent.ACTION_DOWN;
            if (isSend || isEnter) {
                submit.run();
                return true;
            }
            return false;
        });
    }
}


