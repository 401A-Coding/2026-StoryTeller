package com.example.storyteller.ui.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.CheckBox;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.DialogFragment;
import com.example.storyteller.R;
import com.example.storyteller.model.UserWritingPreference;
import com.example.storyteller.utils.PreferenceExtractor;
import com.example.storyteller.utils.PreferenceManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * 用户写作偏好设置对话框
 * 允许用户设置全局或小说专属的写作偏好
 */
public class WritingPreferenceDialog extends DialogFragment {
    
    private static final String ARG_STORY_ID = "story_id";
    private static final String ARG_STORY_TITLE = "story_title";
    private static final String ARG_CONVERSATION_HISTORY = "conversation_history";
    
    private PreferenceManager preferenceManager;
    private OnPreferenceSavedListener listener;
    
    private RadioGroup rgWritingStyle;
    private RadioGroup rgNarrative;
    private RadioGroup rgParagraph;
    private CheckBox cbAvoidBloody;
    private CheckBox cbAvoidViolence;
    private CheckBox cbAvoidSensitive;
    private TextInputEditText etCustomStyle;
    private TextInputEditText etSpecialRequirements;
    private TextInputLayout tilCustomStyle;
    private CardView cardAiPreference;
    private TextView tvAiPreferenceResult;
    private MaterialButton btnApplyAiPreference;
    
    private Integer storyId;  // null表示全局偏好
    private String conversationHistory;  // 对话历史，用于AI学习
    private PreferenceExtractor.UserPreferences aiExtractedPreferences;  // 存储AI分析结果
    
    public interface OnPreferenceSavedListener {
        void onPreferenceSaved(UserWritingPreference preference);
    }
    
    public static WritingPreferenceDialog newInstance() {
        return new WritingPreferenceDialog();
    }
    
    public static WritingPreferenceDialog newInstance(int storyId, String storyTitle) {
        WritingPreferenceDialog dialog = new WritingPreferenceDialog();
        Bundle args = new Bundle();
        args.putInt(ARG_STORY_ID, storyId);
        args.putString(ARG_STORY_TITLE, storyTitle);
        dialog.setArguments(args);
        return dialog;
    }
    
    public static WritingPreferenceDialog newInstance(int storyId, String storyTitle, String conversationHistory) {
        WritingPreferenceDialog dialog = new WritingPreferenceDialog();
        Bundle args = new Bundle();
        args.putInt(ARG_STORY_ID, storyId);
        args.putString(ARG_STORY_TITLE, storyTitle);
        args.putString(ARG_CONVERSATION_HISTORY, conversationHistory);
        dialog.setArguments(args);
        return dialog;
    }
    
    public void setOnPreferenceSavedListener(OnPreferenceSavedListener listener) {
        this.listener = listener;
    }
    
    public void setConversationHistory(String history) {
        this.conversationHistory = history;
    }
    
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        preferenceManager = PreferenceManager.getInstance(context);
        
        if (getArguments() != null) {
            storyId = getArguments().getInt(ARG_STORY_ID, -1);
            if (storyId == -1) {
                storyId = null;
            }
            conversationHistory = getArguments().getString(ARG_CONVERSATION_HISTORY);
        }
    }
    
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View contentView = getLayoutInflater().inflate(R.layout.dialog_writing_preference, null);
        builder.setView(contentView);
        
        // 书名最大显示长度（单位：字符），超过则截断
        int maxTitleLength = 7;
        String title = storyId != null ? "本书写作偏好" : "写作偏好设置";
        if (getArguments() != null) {
            String storyTitle = getArguments().getString(ARG_STORY_TITLE);
            if (!TextUtils.isEmpty(storyTitle)) {
                // 截断过长的书名
                String displayTitle = storyTitle;
                if (storyTitle.length() > maxTitleLength) {
                    displayTitle = storyTitle.substring(0, maxTitleLength) + "...";
                }
                title = "《" + displayTitle + "》写作偏好";
            }
        }
        builder.setTitle(title);
        
        builder.setPositiveButton("保存", (dialog, which) -> savePreferenceFromView(contentView));
        builder.setNegativeButton("取消", null);
        
        AlertDialog dialog = builder.create();
        
        initViews(contentView);
        loadCurrentPreference();
        loadAiAnalyzedPreference();
        
        return dialog;
    }
    
    private void initViews(View contentView) {
        rgWritingStyle = contentView.findViewById(R.id.rg_writing_style);
        rgNarrative = contentView.findViewById(R.id.rg_narrative);
        rgParagraph = contentView.findViewById(R.id.rg_paragraph);
        cbAvoidBloody = contentView.findViewById(R.id.cb_avoid_bloody);
        cbAvoidViolence = contentView.findViewById(R.id.cb_avoid_violence);
        cbAvoidSensitive = contentView.findViewById(R.id.cb_avoid_sensitive);
        etCustomStyle = contentView.findViewById(R.id.et_custom_style);
        etSpecialRequirements = contentView.findViewById(R.id.et_special_requirements);
        tilCustomStyle = contentView.findViewById(R.id.til_custom_style);
        cardAiPreference = contentView.findViewById(R.id.card_ai_preference);
        tvAiPreferenceResult = contentView.findViewById(R.id.tv_ai_preference_result);
        btnApplyAiPreference = contentView.findViewById(R.id.btn_apply_ai_preference);
        
        rgWritingStyle.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_style_custom) {
                tilCustomStyle.setVisibility(View.VISIBLE);
            } else {
                tilCustomStyle.setVisibility(View.GONE);
            }
        });
        
        // 全局模式下隐藏整个AI分析区域（因为没有对话历史）
        View layoutAiSection = contentView.findViewById(R.id.layout_ai_section);
        if (storyId == null) {
            // 全局模式：隐藏整个AI分析区域
            layoutAiSection.setVisibility(View.GONE);
        } else {
            // 小说专属模式：显示AI分析功能
            MaterialButton btnLearnPreference = contentView.findViewById(R.id.btn_learn_preference);
            btnLearnPreference.setOnClickListener(v -> learnPreferences());
            btnApplyAiPreference.setOnClickListener(v -> applyAiPreferences());
        }
    }
    
    /**
     * AI分析偏好 - 只显示结果，不自动覆盖
     */
    private void learnPreferences() {
        if (TextUtils.isEmpty(conversationHistory)) {
            Toast.makeText(requireContext(), "当前没有对话历史，请先与AI对话", Toast.LENGTH_SHORT).show();
            return;
        }
        
        int currentStoryId = storyId != null ? storyId : -1;
        android.util.Log.d("WritingPreferenceDialog", "learnPreferences - currentStoryId: " + currentStoryId);
        android.util.Log.d("WritingPreferenceDialog", "Conversation history length: " + conversationHistory.length());
        
        Toast.makeText(requireContext(), "正在分析对话...", Toast.LENGTH_SHORT).show();
        
        // 获取历史分析结果（根据storyId获取）
        PreferenceExtractor.UserPreferences previousAnalyzed = preferenceManager.getAiAnalyzedPreference(currentStoryId);
        if (previousAnalyzed != null) {
            android.util.Log.d("WritingPreferenceDialog", "Previous analysis found:");
            android.util.Log.d("WritingPreferenceDialog", "  writing_style: " + previousAnalyzed.writing_style);
            android.util.Log.d("WritingPreferenceDialog", "  narrative_perspective: " + previousAnalyzed.narrative_perspective);
        } else {
            android.util.Log.d("WritingPreferenceDialog", "No previous analysis found");
        }
        
        PreferenceExtractor extractor = new PreferenceExtractor(requireContext());
        extractor.extractPreferences(conversationHistory, previousAnalyzed, new PreferenceExtractor.Callback() {
            
            @Override
            public void onPreferencesExtracted(PreferenceExtractor.UserPreferences preferences) {
                aiExtractedPreferences = preferences;
                
                // 保存分析结果（保存到对应的storyId或全局）
                preferenceManager.saveAiAnalyzedPreference(preferences, storyId);
                
                // 构建显示文本（只显示非null的字段）
                StringBuilder result = new StringBuilder();
                if (!TextUtils.isEmpty(preferences.writing_style)) {
                    result.append("写作风格：").append(toDisplayStyle(preferences.writing_style)).append("\n");
                }
                if (!TextUtils.isEmpty(preferences.narrative_perspective)) {
                    result.append("叙事视角：").append(toDisplayNarrative(preferences.narrative_perspective)).append("\n");
                }
                if (!TextUtils.isEmpty(preferences.paragraph_length)) {
                    result.append("段落长度：").append(toDisplayParagraph(preferences.paragraph_length)).append("\n");
                }
                if (preferences.avoid_bloody != null) {
                    result.append("避免血腥：").append(preferences.avoid_bloody ? "是" : "否").append("\n");
                }
                if (preferences.avoid_violence != null) {
                    result.append("避免暴力：").append(preferences.avoid_violence ? "是" : "否").append("\n");
                }
                if (preferences.avoid_sensitive != null) {
                    result.append("避免敏感：").append(preferences.avoid_sensitive ? "是" : "否").append("\n");
                }
                if (!TextUtils.isEmpty(preferences.special_requirements)) {
                    result.append("特殊要求：").append(preferences.special_requirements).append("\n");
                }
                
                requireActivity().runOnUiThread(() -> {
                    if (result.length() == 0) {
                        tvAiPreferenceResult.setText("【当前无分析结果】");
                        tvAiPreferenceResult.setTextColor(0xFF999999);
                    } else {
                        tvAiPreferenceResult.setText(result.toString());
                        tvAiPreferenceResult.setTextColor(0xFF333333);
                    }
                    Toast.makeText(requireContext(), "分析完成", Toast.LENGTH_SHORT).show();
                });
            }
            
            @Override
            public void onFailure(Exception e) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "分析失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    /**
     * 加载历史分析结果到显示区域
     */
    private void loadAiAnalyzedPreference() {
        int queryStoryId = storyId != null ? storyId : -1;
        
        PreferenceExtractor.UserPreferences analyzed = preferenceManager.getAiAnalyzedPreference(queryStoryId);
        if (analyzed == null) {
            return;
        }
        
        aiExtractedPreferences = analyzed;
        
        StringBuilder result = new StringBuilder();
        if (!TextUtils.isEmpty(analyzed.writing_style)) {
            result.append("写作风格：").append(toDisplayStyle(analyzed.writing_style)).append("\n");
        }
        if (!TextUtils.isEmpty(analyzed.narrative_perspective)) {
            result.append("叙事视角：").append(toDisplayNarrative(analyzed.narrative_perspective)).append("\n");
        }
        if (!TextUtils.isEmpty(analyzed.paragraph_length)) {
            result.append("段落长度：").append(toDisplayParagraph(analyzed.paragraph_length)).append("\n");
        }
        if (analyzed.avoid_bloody != null) {
            result.append("避免血腥：").append(analyzed.avoid_bloody ? "是" : "否").append("\n");
        }
        if (analyzed.avoid_violence != null) {
            result.append("避免暴力：").append(analyzed.avoid_violence ? "是" : "否").append("\n");
        }
        if (analyzed.avoid_sensitive != null) {
            result.append("避免敏感：").append(analyzed.avoid_sensitive ? "是" : "否").append("\n");
        }
        if (!TextUtils.isEmpty(analyzed.special_requirements)) {
            result.append("特殊要求：").append(analyzed.special_requirements).append("\n");
        }
        
        if (result.length() > 0) {
            tvAiPreferenceResult.setText(result.toString());
            tvAiPreferenceResult.setTextColor(0xFF333333);
        }
    }
    
    /**
     * 将内部风格值转换为显示文本
     */
    private String toDisplayStyle(String style) {
        switch (style) {
            case "simple": return "简洁直白";
            case "elegant": return "华丽抒情";
            case "humorous": return "幽默风趣";
            case "suspense": return "悬疑紧张";
            default: return style;
        }
    }
    
    /**
     * 将内部叙事视角值转换为显示文本
     */
    private String toDisplayNarrative(String narrative) {
        switch (narrative) {
            case "first": return "第一人称";
            case "third_limited": return "第三人称限知";
            case "third_omniscient": return "第三人称全知";
            default: return narrative;
        }
    }
    
    /**
     * 将内部段落长度值转换为显示文本
     */
    private String toDisplayParagraph(String length) {
        switch (length) {
            case "short": return "短段落（快节奏）";
            case "medium": return "中等";
            case "long": return "长段落（沉浸感）";
            default: return length;
        }
    }
    
    /**
     * 应用AI分析的偏好到表单
     */
    private void applyAiPreferences() {
        if (aiExtractedPreferences == null) {
            Toast.makeText(requireContext(), "请先点击\"分析偏好\"按钮", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (aiExtractedPreferences.writing_style != null) {
            setWritingStyleRadio(aiExtractedPreferences.writing_style);
        }
        if (aiExtractedPreferences.narrative_perspective != null) {
            setNarrativeRadio(aiExtractedPreferences.narrative_perspective);
        }
        if (aiExtractedPreferences.paragraph_length != null) {
            setParagraphRadio(aiExtractedPreferences.paragraph_length);
        }
        if (aiExtractedPreferences.avoid_bloody != null) {
            cbAvoidBloody.setChecked(aiExtractedPreferences.avoid_bloody);
        }
        if (aiExtractedPreferences.avoid_violence != null) {
            cbAvoidViolence.setChecked(aiExtractedPreferences.avoid_violence);
        }
        if (aiExtractedPreferences.avoid_sensitive != null) {
            cbAvoidSensitive.setChecked(aiExtractedPreferences.avoid_sensitive);
        }
        if (aiExtractedPreferences.special_requirements != null) {
            etSpecialRequirements.setText(aiExtractedPreferences.special_requirements);
        }
        
        Toast.makeText(requireContext(), "已应用AI分析的偏好，请点击\"保存\"按钮保存", Toast.LENGTH_SHORT).show();
    }
    
    private void setWritingStyleRadio(String style) {
        if (TextUtils.isEmpty(style)) return;
        // 先尝试直接匹配内部值
        switch (style) {
            case "simple":
            case "简洁直白":
                rgWritingStyle.check(R.id.rb_style_simple);
                break;
            case "elegant":
            case "华丽抒情":
                rgWritingStyle.check(R.id.rb_style_elegant);
                break;
            case "humorous":
            case "幽默风趣":
                rgWritingStyle.check(R.id.rb_style_humorous);
                break;
            case "suspense":
            case "悬疑紧张":
                rgWritingStyle.check(R.id.rb_style_suspense);
                break;
        }
    }
    
    private void setNarrativeRadio(String narrative) {
        if (TextUtils.isEmpty(narrative)) return;
        switch (narrative) {
            case "first":
            case "第一人称":
                rgNarrative.check(R.id.rb_narrative_first);
                break;
            case "third_limited":
            case "第三人称限知":
                rgNarrative.check(R.id.rb_narrative_third_limited);
                break;
            case "third_omniscient":
            case "第三人称全知":
                rgNarrative.check(R.id.rb_narrative_third_omniscient);
                break;
        }
    }
    
    private void setParagraphRadio(String length) {
        if (TextUtils.isEmpty(length)) return;
        switch (length) {
            case "short":
            case "短段落（快节奏）":
                rgParagraph.check(R.id.rb_paragraph_short);
                break;
            case "medium":
            case "中等":
                rgParagraph.check(R.id.rb_paragraph_medium);
                break;
            case "long":
            case "长段落（沉浸感）":
                rgParagraph.check(R.id.rb_paragraph_long);
                break;
        }
    }
    
    private void loadCurrentPreference() {
        UserWritingPreference preference;
        if (storyId != null) {
            preference = preferenceManager.getStoryPreference(storyId);
        } else {
            preference = preferenceManager.getGlobalPreference();
        }
        
        if (!TextUtils.isEmpty(preference.getWritingStyle())) {
            switch (preference.getWritingStyle()) {
                case UserWritingPreference.STYLE_SIMPLE:
                    rgWritingStyle.check(R.id.rb_style_simple);
                    break;
                case UserWritingPreference.STYLE_ELEGANT:
                    rgWritingStyle.check(R.id.rb_style_elegant);
                    break;
                case UserWritingPreference.STYLE_HUMOROUS:
                    rgWritingStyle.check(R.id.rb_style_humorous);
                    break;
                case UserWritingPreference.STYLE_SUSPENSE:
                    rgWritingStyle.check(R.id.rb_style_suspense);
                    break;
                case UserWritingPreference.STYLE_CUSTOM:
                    rgWritingStyle.check(R.id.rb_style_custom);
                    tilCustomStyle.setVisibility(View.VISIBLE);
                    if (!TextUtils.isEmpty(preference.getCustomStyle())) {
                        etCustomStyle.setText(preference.getCustomStyle());
                    }
                    break;
            }
        }
        
        if (!TextUtils.isEmpty(preference.getNarrativePerspective())) {
            switch (preference.getNarrativePerspective()) {
                case UserWritingPreference.NARRATIVE_FIRST:
                    rgNarrative.check(R.id.rb_narrative_first);
                    break;
                case UserWritingPreference.NARRATIVE_THIRD_LIMITED:
                    rgNarrative.check(R.id.rb_narrative_third_limited);
                    break;
                case UserWritingPreference.NARRATIVE_THIRD_OMNISCIENT:
                    rgNarrative.check(R.id.rb_narrative_third_omniscient);
                    break;
            }
        }
        
        if (!TextUtils.isEmpty(preference.getParagraphLength())) {
            switch (preference.getParagraphLength()) {
                case UserWritingPreference.PARAGRAPH_SHORT:
                    rgParagraph.check(R.id.rb_paragraph_short);
                    break;
                case UserWritingPreference.PARAGRAPH_MEDIUM:
                    rgParagraph.check(R.id.rb_paragraph_medium);
                    break;
                case UserWritingPreference.PARAGRAPH_LONG:
                    rgParagraph.check(R.id.rb_paragraph_long);
                    break;
            }
        }
        
        cbAvoidBloody.setChecked(preference.isAvoidBloody());
        cbAvoidViolence.setChecked(preference.isAvoidViolence());
        cbAvoidSensitive.setChecked(preference.isAvoidSensitive());
        
        if (!TextUtils.isEmpty(preference.getSpecialRequirements())) {
            etSpecialRequirements.setText(preference.getSpecialRequirements());
        }
    }
    
    private void savePreferenceFromView(View contentView) {
        RadioGroup rgWritingStyleFromView = contentView.findViewById(R.id.rg_writing_style);
        RadioGroup rgNarrativeFromView = contentView.findViewById(R.id.rg_narrative);
        RadioGroup rgParagraphFromView = contentView.findViewById(R.id.rg_paragraph);
        CheckBox cbAvoidBloodyFromView = contentView.findViewById(R.id.cb_avoid_bloody);
        CheckBox cbAvoidViolenceFromView = contentView.findViewById(R.id.cb_avoid_violence);
        CheckBox cbAvoidSensitiveFromView = contentView.findViewById(R.id.cb_avoid_sensitive);
        TextInputEditText etCustomStyleFromView = contentView.findViewById(R.id.et_custom_style);
        TextInputEditText etSpecialRequirementsFromView = contentView.findViewById(R.id.et_special_requirements);
        
        UserWritingPreference preference = new UserWritingPreference();
        
        int checkedStyleId = rgWritingStyleFromView.getCheckedRadioButtonId();
        if (checkedStyleId == R.id.rb_style_simple) {
            preference.setWritingStyle(UserWritingPreference.STYLE_SIMPLE);
        } else if (checkedStyleId == R.id.rb_style_elegant) {
            preference.setWritingStyle(UserWritingPreference.STYLE_ELEGANT);
        } else if (checkedStyleId == R.id.rb_style_humorous) {
            preference.setWritingStyle(UserWritingPreference.STYLE_HUMOROUS);
        } else if (checkedStyleId == R.id.rb_style_suspense) {
            preference.setWritingStyle(UserWritingPreference.STYLE_SUSPENSE);
        } else if (checkedStyleId == R.id.rb_style_custom) {
            preference.setWritingStyle(UserWritingPreference.STYLE_CUSTOM);
            String customStyle = etCustomStyleFromView.getText() != null ? etCustomStyleFromView.getText().toString().trim() : "";
            preference.setCustomStyle(customStyle);
        }
        
        int checkedNarrativeId = rgNarrativeFromView.getCheckedRadioButtonId();
        if (checkedNarrativeId == R.id.rb_narrative_first) {
            preference.setNarrativePerspective(UserWritingPreference.NARRATIVE_FIRST);
        } else if (checkedNarrativeId == R.id.rb_narrative_third_limited) {
            preference.setNarrativePerspective(UserWritingPreference.NARRATIVE_THIRD_LIMITED);
        } else if (checkedNarrativeId == R.id.rb_narrative_third_omniscient) {
            preference.setNarrativePerspective(UserWritingPreference.NARRATIVE_THIRD_OMNISCIENT);
        }
        
        int checkedParagraphId = rgParagraphFromView.getCheckedRadioButtonId();
        if (checkedParagraphId == R.id.rb_paragraph_short) {
            preference.setParagraphLength(UserWritingPreference.PARAGRAPH_SHORT);
        } else if (checkedParagraphId == R.id.rb_paragraph_medium) {
            preference.setParagraphLength(UserWritingPreference.PARAGRAPH_MEDIUM);
        } else if (checkedParagraphId == R.id.rb_paragraph_long) {
            preference.setParagraphLength(UserWritingPreference.PARAGRAPH_LONG);
        }
        
        preference.setAvoidBloody(cbAvoidBloodyFromView.isChecked());
        preference.setAvoidViolence(cbAvoidViolenceFromView.isChecked());
        preference.setAvoidSensitive(cbAvoidSensitiveFromView.isChecked());
        
        String specialRequirements = etSpecialRequirementsFromView.getText() != null ? etSpecialRequirementsFromView.getText().toString().trim() : "";
        if (!TextUtils.isEmpty(specialRequirements)) {
            preference.setSpecialRequirements(specialRequirements);
        }
        
        boolean success;
        if (storyId != null) {
            success = preferenceManager.saveStoryPreference(preference, storyId);
        } else {
            success = preferenceManager.saveGlobalPreference(preference);
        }
        
        if (success) {
            Toast.makeText(requireContext(), "偏好已保存", Toast.LENGTH_SHORT).show();
            
            if (listener != null) {
                listener.onPreferenceSaved(preference);
            }
        } else {
            Toast.makeText(requireContext(), "保存失败，请重试", Toast.LENGTH_SHORT).show();
        }
    }
}