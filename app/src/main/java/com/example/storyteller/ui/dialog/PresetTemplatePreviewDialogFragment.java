package com.example.storyteller.ui.dialog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.storyteller.R;
import com.example.storyteller.model.PresetSettingItem;
import com.example.storyteller.model.PresetTemplate;
import com.example.storyteller.utils.PresetTemplateManager;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.List;

/**
 * 模板预览对话框（BottomSheet）。
 *
 * <p>展示模板的元信息（名称、参考作品、简介）和 6 个 setting 的标题+摘要，
 * 让用户在安装前能看到模板内容。</p>
 */
public class PresetTemplatePreviewDialogFragment extends BottomSheetDialogFragment {

    private static final String ARG_TEMPLATE_ID = "template_id";

    public static PresetTemplatePreviewDialogFragment newInstance(String templateId) {
        PresetTemplatePreviewDialogFragment f = new PresetTemplatePreviewDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TEMPLATE_ID, templateId);
        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_preset_preview, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        String templateId = getArguments() != null
                ? getArguments().getString(ARG_TEMPLATE_ID) : null;
        if (templateId == null) {
            dismiss();
            return;
        }

        // 读模板（带缓存）。当前模板 JSON < 50KB，主线程读没问题
        // （如未来模板变大可改用 ioExecutor 推到后台）
        PresetTemplate template = new PresetTemplateManager(requireContext())
                .loadTemplate(templateId);
        if (template == null) {
            dismiss();
            return;
        }

        TextView tvTitle = view.findViewById(R.id.tv_preview_title);
        TextView tvSource = view.findViewById(R.id.tv_preview_source);
        TextView tvDescription = view.findViewById(R.id.tv_preview_description);
        RecyclerView rvSettings = view.findViewById(R.id.rv_preview_settings);
        Button btnClose = view.findViewById(R.id.btn_preview_close);

        tvTitle.setText(template.getTemplateName());
        tvDescription.setText(template.getDescription());

        // 来源：参考作品（+ 作者）
        if (template.getSource() != null && template.getSource().getTitle() != null) {
            String source = template.getSource().getTitle();
            if (template.getSource().getAuthor() != null
                    && !template.getSource().getAuthor().isEmpty()) {
                source = getString(R.string.preset_preview_source_author_format,
                        source, template.getSource().getAuthor());
            }
            tvSource.setText(getString(R.string.preset_preview_source_format, source));
        } else {
            tvSource.setVisibility(View.GONE);
        }

        rvSettings.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvSettings.setAdapter(new PresetSettingsAdapter(template.getSettings()));

        btnClose.setOnClickListener(v -> dismiss());
    }

    /**
     * 展示 6 个 setting 的简化列表（分类 + 标题 + 摘要）。
     */
    private static class PresetSettingsAdapter
            extends RecyclerView.Adapter<PresetSettingsAdapter.VH> {

        private final List<PresetSettingItem> items;

        PresetSettingsAdapter(List<PresetSettingItem> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_preset_preview_setting, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            PresetSettingItem item = items.get(position);
            holder.tvCategory.setText(item.getCategory());
            holder.tvSubCategory.setText(item.getSubCategory());
            holder.tvTitle.setText(item.getTitle());
            holder.tvSummary.setText(item.getSummary());
        }

        @Override
        public int getItemCount() {
            return items == null ? 0 : items.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            final TextView tvCategory;
            final TextView tvSubCategory;
            final TextView tvTitle;
            final TextView tvSummary;

            VH(View itemView) {
                super(itemView);
                tvCategory = itemView.findViewById(R.id.tv_preview_category);
                tvSubCategory = itemView.findViewById(R.id.tv_preview_subcategory);
                tvTitle = itemView.findViewById(R.id.tv_preview_setting_title);
                tvSummary = itemView.findViewById(R.id.tv_preview_setting_summary);
            }
        }
    }
}
