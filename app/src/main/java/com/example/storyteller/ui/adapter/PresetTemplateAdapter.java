package com.example.storyteller.ui.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.storyteller.R;
import com.example.storyteller.model.PresetTemplateIndex;
import com.example.storyteller.utils.PresetTemplateManager;

import java.util.ArrayList;
import java.util.List;

/**
 * 预设模板列表适配器（"模板中心"用）
 *
 * <p>展示模板名、描述、状态徽章和安装/卸载按钮。
 * 业务执行（install/uninstall）由外部传入的监听器完成。</p>
 */
public class PresetTemplateAdapter extends RecyclerView.Adapter<PresetTemplateAdapter.ViewHolder> {

    /** 状态徽章颜色 */
    private static final int COLOR_NOT_INSTALLED = Color.parseColor("#9E9E9E");
    private static final int COLOR_INSTALLED = Color.parseColor("#4CAF50");
    private static final int COLOR_HAS_UPDATE = Color.parseColor("#FF9800");
    private static final int COLOR_UNKNOWN = Color.parseColor("#BDBDBD");

    public interface OnTemplateActionListener {
        void onInstall(PresetTemplateManager.TemplateInstallState state,
                       PresetTemplateIndex index);
        void onUninstall(PresetTemplateManager.TemplateInstallState state,
                         PresetTemplateIndex index);
        void onPreview(PresetTemplateManager.TemplateInstallState state,
                       PresetTemplateIndex index);
    }

    /** UI 展示用的复合数据 */
    public static class Item {
        public final PresetTemplateIndex index;
        public final PresetTemplateManager.TemplateInstallState state;

        public Item(PresetTemplateIndex index,
                    PresetTemplateManager.TemplateInstallState state) {
            this.index = index;
            this.state = state;
        }
    }

    private final List<Item> items = new ArrayList<>();
    private OnTemplateActionListener listener;

    public void setData(List<Item> list) {
        items.clear();
        if (list != null) {
            items.addAll(list);
        }
        notifyDataSetChanged();
    }

    public void setOnTemplateActionListener(OnTemplateActionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_preset_template, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Item item = items.get(position);
        PresetTemplateIndex index = item.index;
        PresetTemplateManager.TemplateInstallState state = item.state;
        final android.content.Context ctx = holder.itemView.getContext();

        holder.tvName.setText(index.getName());

        // 描述：优先用 index 的描述，否则用 TemplateInstallState 推断
        String desc = index.getDescription();
        if (desc == null || desc.isEmpty()) {
            desc = state.templateName;
        }
        holder.tvDescription.setText(desc);

        // 状态徽章 + 操作按钮（用 string 资源，保持 i18n 友好）
        switch (state.status) {
            case NOT_INSTALLED:
                holder.tvStatus.setBackgroundColor(COLOR_NOT_INSTALLED);
                holder.tvStatus.setText(ctx.getString(R.string.preset_status_not_installed));
                holder.btnInstall.setVisibility(View.VISIBLE);
                holder.btnInstall.setText(ctx.getString(R.string.preset_action_install));
                holder.btnInstall.setEnabled(true);
                holder.btnUninstall.setVisibility(View.GONE);
                break;
            case INSTALLED:
                holder.tvStatus.setBackgroundColor(COLOR_INSTALLED);
                holder.tvStatus.setText(ctx.getString(
                        R.string.preset_status_installed_format, state.installedVersion));
                holder.btnInstall.setVisibility(View.VISIBLE);
                holder.btnInstall.setText(ctx.getString(R.string.preset_action_reinstall));
                holder.btnInstall.setEnabled(true);
                holder.btnUninstall.setVisibility(View.VISIBLE);
                break;
            case HAS_UPDATE:
                holder.tvStatus.setBackgroundColor(COLOR_HAS_UPDATE);
                holder.tvStatus.setText(ctx.getString(
                        R.string.preset_status_has_update_format,
                        state.installedVersion, state.latestVersion));
                holder.btnInstall.setVisibility(View.VISIBLE);
                holder.btnInstall.setText(ctx.getString(
                        R.string.preset_action_update_format, state.latestVersion));
                holder.btnInstall.setEnabled(true);
                holder.btnUninstall.setVisibility(View.VISIBLE);
                break;
            case UNKNOWN:
            default:
                holder.tvStatus.setBackgroundColor(COLOR_UNKNOWN);
                holder.tvStatus.setText(ctx.getString(R.string.preset_status_unknown));
                holder.btnInstall.setVisibility(View.GONE);
                holder.btnUninstall.setVisibility(View.GONE);
                break;
        }

        // 元信息
        String meta;
        if (state.installedCount > 0) {
            meta = ctx.getString(R.string.preset_meta_installed_format,
                    state.installedCount, state.latestVersion);
        } else {
            meta = ctx.getString(R.string.preset_meta_latest_format, state.latestVersion);
        }
        holder.tvMeta.setText(meta);

        // 绑定按钮事件
        holder.btnInstall.setOnClickListener(v -> {
            if (listener != null) {
                listener.onInstall(state, index);
            }
        });
        holder.btnUninstall.setOnClickListener(v -> {
            if (listener != null) {
                listener.onUninstall(state, index);
            }
        });
        holder.btnPreview.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPreview(state, index);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvName;
        final TextView tvStatus;
        final TextView tvDescription;
        final TextView tvMeta;
        final Button btnInstall;
        final Button btnUninstall;
        final Button btnPreview;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_template_name);
            tvStatus = itemView.findViewById(R.id.tv_template_status);
            tvDescription = itemView.findViewById(R.id.tv_template_description);
            tvMeta = itemView.findViewById(R.id.tv_template_meta);
            btnInstall = itemView.findViewById(R.id.btn_template_install);
            btnUninstall = itemView.findViewById(R.id.btn_template_uninstall);
            btnPreview = itemView.findViewById(R.id.btn_template_preview);
        }
    }
}
