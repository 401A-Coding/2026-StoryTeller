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
import com.example.storyteller.model.Material;
import com.example.storyteller.model.NovelSummary;
import com.example.storyteller.ui.adapter.MaterialCandidateReviewAdapter;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.List;

public class MaterialCandidateReviewDialogFragment extends BottomSheetDialogFragment {

    public interface Listener {
        void onConfirm(@NonNull NovelSummary summary, @NonNull List<Material> selectedMaterials, @Nullable String rawJson);
        void onCancel();
    }

    private final MaterialCandidateReviewAdapter adapter = new MaterialCandidateReviewAdapter();
    private Listener listener;
    private NovelSummary summary;
    private String rawJson;
    private boolean confirmed;

    public static MaterialCandidateReviewDialogFragment newInstance() {
        return new MaterialCandidateReviewDialogFragment();
    }

    public void setListener(@Nullable Listener listener) {
        this.listener = listener;
    }

    public void setData(@NonNull NovelSummary summary, @NonNull List<Material> materials, @Nullable String rawJson) {
        this.summary = summary;
        this.rawJson = rawJson;
        adapter.setData(materials);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_material_candidate_review, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView tvTitle = view.findViewById(R.id.tv_candidate_review_title);
        Button btnSelectAll = view.findViewById(R.id.btn_candidate_review_select_all);
        Button btnCancel = view.findViewById(R.id.btn_candidate_review_cancel);
        Button btnConfirm = view.findViewById(R.id.btn_candidate_review_confirm);
        RecyclerView rvCandidates = view.findViewById(R.id.rv_candidate_review_list);
        TextView tvEmpty = view.findViewById(R.id.tv_candidate_review_empty);

        tvTitle.setText(R.string.material_review_title);

        rvCandidates.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvCandidates.setAdapter(adapter);

        // Show empty view if there are no candidate materials
        tvEmpty.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);

        // Initialize select-all button label according to current adapter state
        btnSelectAll.setText(adapter.isAllSelected()
                ? R.string.material_review_deselect_all
                : R.string.material_review_select_all);

        btnSelectAll.setOnClickListener(v -> {
            boolean newState = !adapter.isAllSelected();
            adapter.setAllSelected(newState);
            // Update button label: when all selected show "取消全选", otherwise show "全选"
            btnSelectAll.setText(newState
                    ? R.string.material_review_deselect_all
                    : R.string.material_review_select_all);
        });

        btnCancel.setOnClickListener(v -> {
            confirmed = false;
            dismiss();
        });

        btnConfirm.setOnClickListener(v -> {
            if (summary == null) {
                dismiss();
                return;
            }
            List<Material> selected = adapter.getSelectedMaterials();
            if (selected.isEmpty()) {
                tvEmpty.setText(R.string.material_review_empty);
                tvEmpty.setVisibility(View.VISIBLE);
                return;
            }
            confirmed = true;
            if (listener != null) {
                listener.onConfirm(summary, selected, rawJson);
            }
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

