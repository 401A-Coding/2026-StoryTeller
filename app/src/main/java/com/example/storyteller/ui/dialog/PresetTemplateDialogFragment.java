package com.example.storyteller.ui.dialog;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.storyteller.R;
import com.example.storyteller.model.PresetTemplateIndex;
import com.example.storyteller.ui.adapter.PresetTemplateAdapter;
import com.example.storyteller.utils.PresetTemplateManager;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 模板中心对话框（BottomSheet）
 *
 * <p>列出所有内置模板及其安装状态，用户可一键安装/更新/卸载。</p>
 *
 * <p>使用方式：</p>
 * <pre>{@code
 * PresetTemplateDialogFragment dialog = PresetTemplateDialogFragment.newInstance(storyId);
 * dialog.setListener(new PresetTemplateDialogFragment.Listener() {
 *     // 收到结果时由 Fragment/Activity 自行刷新列表
 * });
 * dialog.show(getChildFragmentManager(), "preset_template");
 * }</pre>
 */
public class PresetTemplateDialogFragment extends BottomSheetDialogFragment {

    public interface Listener {
        /** 用户执行了安装/更新/卸载等操作，外部应刷新素材列表 */
        void onChanged();
    }

    private static final String ARG_STORY_ID = "story_id";

    private PresetTemplateAdapter adapter;
    private PresetTemplateManager manager;
    private Listener listener;
    private int storyId = 0;

    // 后台 IO 线程：安装/卸载会读 assets + 多次 SQLite insert/delete，
    // 不能在主线程做。
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    // 防重复点击：安装/卸载进行中不允许再点下一个
    private volatile boolean busy = false;

    public static PresetTemplateDialogFragment newInstance(int storyId) {
        PresetTemplateDialogFragment f = new PresetTemplateDialogFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_STORY_ID, storyId);
        f.setArguments(args);
        return f;
    }

    public void setListener(@Nullable Listener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            storyId = getArguments().getInt(ARG_STORY_ID, 0);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_preset_template, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        manager = new PresetTemplateManager(requireContext());
        adapter = new PresetTemplateAdapter();

        TextView tvEmpty = view.findViewById(R.id.tv_preset_empty);
        RecyclerView rvList = view.findViewById(R.id.rv_preset_list);
        Button btnClose = view.findViewById(R.id.btn_preset_close);

        rvList.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvList.setAdapter(adapter);

        adapter.setOnTemplateActionListener(new PresetTemplateAdapter.OnTemplateActionListener() {
            @Override
            public void onInstall(PresetTemplateManager.TemplateInstallState state,
                                  PresetTemplateIndex index) {
                handleInstall(state, index);
            }

            @Override
            public void onUninstall(PresetTemplateManager.TemplateInstallState state,
                                    PresetTemplateIndex index) {
                handleUninstall(state, index);
            }

            @Override
            public void onPreview(PresetTemplateManager.TemplateInstallState state,
                                  PresetTemplateIndex index) {
                handlePreview(index);
            }
        });

        btnClose.setOnClickListener(v -> dismiss());

        refreshList(tvEmpty);
    }

    private void refreshList(TextView tvEmpty) {
        List<PresetTemplateIndex> indexes = manager.listTemplates();
        List<PresetTemplateAdapter.Item> items = new ArrayList<>();
        for (PresetTemplateIndex idx : indexes) {
            PresetTemplateManager.TemplateInstallState state =
                    manager.getInstalledState(idx.getId(), storyId);
            items.add(new PresetTemplateAdapter.Item(idx, state));
        }
        adapter.setData(items);
        tvEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
    }

    /**
     * 弹出模板预览对话框，让用户在不安装的情况下看到 6 个 setting 的内容。
     */
    private void handlePreview(PresetTemplateIndex index) {
        if (index == null || index.getId() == null) {
            return;
        }
        PresetTemplatePreviewDialogFragment dialog =
                PresetTemplatePreviewDialogFragment.newInstance(index.getId());
        dialog.show(getChildFragmentManager(), "preset_template_preview");
    }

    private void handleInstall(PresetTemplateManager.TemplateInstallState state,
                               PresetTemplateIndex index) {
        if (index == null || index.getId() == null) {
            return;
        }
        if (busy) {
            return;
        }
        busy = true;
        final String templateId = index.getId();
        final Context appContext = requireContext().getApplicationContext();
        // 读 assets + Gson 解析 + N 次 SQLite insert 都推到后台线程
        ioExecutor.execute(() -> {
            PresetTemplateManager bgManager = new PresetTemplateManager(appContext);
            com.example.storyteller.model.PresetTemplate template = bgManager.loadTemplate(templateId);
            PresetTemplateManager.InstallResult result;
            if (template == null) {
                result = new PresetTemplateManager.InstallResult();
                result.errorMessage = getString(R.string.preset_load_failed_format, templateId);
            } else {
                result = bgManager.install(template, storyId,
                        PresetTemplateManager.InstallMode.SKIP_EXISTING);
            }
            // 切回主线程刷新 UI
            mainHandler.post(() -> {
                busy = false;
                if (!isAdded()) {
                    return;
                }
                String msg = result.errorMessage != null
                        ? result.errorMessage
                        : formatInstallSummary(result);
                Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
                refreshList(requireView().findViewById(R.id.tv_preset_empty));
                if (listener != null) {
                    listener.onChanged();
                }
            });
        });
    }

    private void handleUninstall(PresetTemplateManager.TemplateInstallState state,
                                 PresetTemplateIndex index) {
        if (index == null || index.getId() == null) {
            return;
        }
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(R.string.preset_uninstall_title)
                .setMessage(getString(R.string.preset_uninstall_message_format, index.getName()))
                .setPositiveButton(R.string.action_uninstall, (d, w) -> doUninstall(index.getId()))
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    /**
     * 实际执行卸载——同样推到后台线程，避免 deleteByPresetTemplateId 阻塞 UI。
     */
    private void doUninstall(String templateId) {
        if (busy) {
            return;
        }
        busy = true;
        final Context appContext = requireContext().getApplicationContext();
        ioExecutor.execute(() -> {
            PresetTemplateManager bgManager = new PresetTemplateManager(appContext);
            int removed = bgManager.uninstall(templateId, storyId);
            mainHandler.post(() -> {
                busy = false;
                if (!isAdded()) {
                    return;
                }
                Toast.makeText(requireContext(),
                        getString(R.string.preset_uninstall_success_format, removed),
                        Toast.LENGTH_SHORT).show();
                refreshList(requireView().findViewById(R.id.tv_preset_empty));
                if (listener != null) {
                    listener.onChanged();
                }
            });
        });
    }

    /**
     * 把 InstallResult 格式化为用户可读的字符串（用 string 资源拼接，便于 i18n）。
     */
    private String formatInstallSummary(PresetTemplateManager.InstallResult result) {
        android.content.Context ctx = requireContext();
        StringBuilder sb = new StringBuilder();
        sb.append(ctx.getString(R.string.preset_install_summary_format,
                result.installed, result.total));
        if (result.replaced > 0) {
            sb.append(ctx.getString(R.string.preset_install_replaced_format, result.replaced));
        }
        if (result.skipped > 0) {
            sb.append(ctx.getString(R.string.preset_install_skipped_format, result.skipped));
        }
        if (result.renamed > 0) {
            sb.append(ctx.getString(R.string.preset_install_renamed_format, result.renamed));
        }
        if (result.failed > 0) {
            sb.append(ctx.getString(R.string.preset_install_failed_format, result.failed));
        }
        return sb.toString();
    }
}
