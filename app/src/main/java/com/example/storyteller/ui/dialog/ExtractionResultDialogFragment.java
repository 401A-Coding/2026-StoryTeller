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
import com.example.storyteller.model.RelationExtractionResult;
import com.example.storyteller.ui.adapter.ExtractionResultAdapter;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.tabs.TabLayout;

import java.util.List;

/**
 * 关系提取结果对话框（基于 MaterialCandidateReviewDialogFragment 模式）
 */
public class ExtractionResultDialogFragment extends BottomSheetDialogFragment {

    public interface Listener {
        void onConfirm(@NonNull RelationExtractionResult result, 
                       @NonNull List<Integer> selectedConfirmedPositions,
                       @NonNull List<Integer> selectedPendingPositions);
        void onCancel();
    }

    public static ExtractionResultDialogFragment newInstance() {
        return new ExtractionResultDialogFragment();
    }

    private RelationExtractionResult result;
    private Listener listener;
    private boolean confirmed;
    private ExtractionResultAdapter adapter;

    public void setData(@NonNull RelationExtractionResult result) {
        this.result = result;
    }

    public void setListener(@Nullable Listener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_extraction_result, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView tvStats = view.findViewById(R.id.tv_stats);
        TabLayout tabLayout = view.findViewById(R.id.tab_layout);
        RecyclerView rvList = view.findViewById(R.id.rv_list);
        Button btnSelectAll = view.findViewById(R.id.btn_select_all);
        Button btnCancel = view.findViewById(R.id.btn_cancel);
        Button btnConfirm = view.findViewById(R.id.btn_confirm);

        // 初始化统计信息
        int confirmedCount = result.getConfirmedRelations() != null ? result.getConfirmedRelations().size() : 0;
        int pendingCount = result.getPendingEntities() != null ? result.getPendingEntities().size() : 0;
        tvStats.setText(String.format("已确认关系: %d | 待创建实体: %d", confirmedCount, pendingCount));

        // 初始化适配器
        adapter = new ExtractionResultAdapter(result);
        rvList.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvList.setAdapter(adapter);

        // 创建 Tab
        tabLayout.removeAllTabs();
        tabLayout.addTab(tabLayout.newTab().setText("已确认关系 (" + confirmedCount + ")"));
        if (pendingCount > 0) {
            tabLayout.addTab(tabLayout.newTab().setText("待创建实体 (" + pendingCount + ")"));
        }

        // 默认显示已确认关系 Tab
        adapter.showConfirmedTab();
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    adapter.showConfirmedTab();
                } else {
                    adapter.showPendingTab();
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        // 全选按钮
        btnSelectAll.setOnClickListener(v -> {
            adapter.toggleAllSelection();
            btnSelectAll.setText(adapter.isAllSelected() ? "取消全选" : "全选");
        });

        // 取消按钮
        btnCancel.setOnClickListener(v -> {
            confirmed = false;
            dismiss();
        });

        // 确认按钮
        btnConfirm.setOnClickListener(v -> {
            if (listener != null) {
                List<Integer> selectedConfirmed = adapter.getSelectedConfirmedPositions();
                List<Integer> selectedPending = adapter.getSelectedPendingPositions();
                listener.onConfirm(result, selectedConfirmed, selectedPending);
            }
            confirmed = true;
            dismiss();
        });
    }

    @Override
    public void onDismiss(@NonNull android.content.DialogInterface dialog) {
        super.onDismiss(dialog);
        if (!confirmed && listener != null) {
            listener.onCancel();
        }
    }
}