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

import java.util.ArrayList;
import java.util.List;

/**
 * 关系提取结果对话框（两阶段模式）
 * 第一阶段：选择要创建的实体
 * 第二阶段：选择要建立的关系
 */
public class ExtractionResultDialogFragment extends BottomSheetDialogFragment {

    public interface Listener {
        /**
         * 两阶段完成后回调
         * @param entityPositions 第一阶段选中的实体位置
         * @param relationPositions 第二阶段选中的关系位置（包括已确认关系和候选关系）
         * @param isFromPending 第二阶段选中的关系是否来自待创建实体（true=候选关系，false=已确认关系）
         */
        void onConfirm(@NonNull List<Integer> entityPositions,
                       @NonNull List<Integer> relationPositions,
                       @NonNull List<Boolean> isFromPending);
        void onCancel();
    }

    public static ExtractionResultDialogFragment newInstance() {
        return new ExtractionResultDialogFragment();
    }

    private RelationExtractionResult result;
    private Listener listener;
    private boolean confirmed;
    private ExtractionResultAdapter adapter;
    
    // 两阶段状态
    private static final int STAGE_ENTITIES = 1;  // 阶段1：选择实体
    private static final int STAGE_RELATIONS = 2; // 阶段2：选择关系
    private int currentStage = STAGE_ENTITIES;
    
    // 第一阶段选中的实体
    private List<Integer> selectedEntities = new ArrayList<>();
    
    // 第二阶段：候选关系（从待创建实体中提取）
    private List<CandidateRelation> candidateRelations = new ArrayList<>();

    // UI 组件
    private TextView tvStageHint;
    private TextView tvStats;
    private TabLayout tabLayout;
    private Button btnSelectAll;
    private Button btnCancel;
    private Button btnNext;
    private Button btnConfirm;

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
        
        // 初始化UI组件
        tvStageHint = view.findViewById(R.id.tv_stage_hint);
        tvStats = view.findViewById(R.id.tv_stats);
        tabLayout = view.findViewById(R.id.tab_layout);
        RecyclerView rvList = view.findViewById(R.id.rv_list);
        btnSelectAll = view.findViewById(R.id.btn_select_all);
        btnCancel = view.findViewById(R.id.btn_cancel);
        btnNext = view.findViewById(R.id.btn_next);
        btnConfirm = view.findViewById(R.id.btn_confirm);
        
        // 初始化适配器
        adapter = new ExtractionResultAdapter(result);
        rvList.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvList.setAdapter(adapter);
        
        // 开始阶段1
        startStage1();
        
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

        // 下一步按钮（阶段1）
        btnNext.setOnClickListener(v -> {
            selectedEntities = adapter.getSelectedPendingPositions();
            if (selectedEntities.isEmpty()) {
                com.google.android.material.snackbar.Snackbar.make(view, "请至少选择一个实体", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show();
                return;
            }
            startStage2();
        });

        // 检查是否有待创建实体
        int pendingCount = result.getPendingEntities() != null ? result.getPendingEntities().size() : 0;
        
        if (pendingCount == 0) {
            // 没有待创建实体，直接进入阶段2（选择关系）
            selectedEntities = new ArrayList<>();
            startStage2Only();
        } else {
            // 有待创建实体，进入阶段1
            startStage1();
        }

        // 确认按钮（阶段2）
        btnConfirm.setOnClickListener(v -> {
            // 收集选中的已确认关系
            List<Integer> selectedConfirmed = adapter.getSelectedConfirmedPositions();
            // 收集选中的候选关系
            List<Integer> selectedCandidates = adapter.getSelectedCandidatePositions();
            
            // 构建结果
            List<Integer> relationPositions = new ArrayList<>();
            List<Boolean> isFromPending = new ArrayList<>();
            
            relationPositions.addAll(selectedConfirmed);
            isFromPending.addAll(java.util.Collections.nCopies(selectedConfirmed.size(), false));
            
            relationPositions.addAll(selectedCandidates);
            isFromPending.addAll(java.util.Collections.nCopies(selectedCandidates.size(), true));
            
            if (listener != null) {
                listener.onConfirm(selectedEntities, relationPositions, isFromPending);
            }
            confirmed = true;
            dismiss();
        });
    }
    
    /**
     * 开始阶段1：选择要创建的实体
     */
    private void startStage1() {
        currentStage = STAGE_ENTITIES;
        
        // 显示阶段提示
        tvStageHint.setVisibility(View.VISIBLE);
        tvStageHint.setText("第一步：选择要创建的实体");
        
        // 更新统计信息
        int pendingCount = result.getPendingEntities() != null ? result.getPendingEntities().size() : 0;
        tvStats.setText(String.format("待创建实体: %d", pendingCount));
        
        // 只显示"待创建实体" Tab
        tabLayout.removeAllTabs();
        tabLayout.addTab(tabLayout.newTab().setText("待创建实体 (" + pendingCount + ")"));
        
        // 显示实体列表
        adapter.showPendingTab();
        adapter.notifyDataSetChanged();
        
        // 显示"下一步"按钮，隐藏"确认"按钮
        btnNext.setVisibility(View.VISIBLE);
        btnConfirm.setVisibility(View.GONE);
    }
    
    /**
     * 开始阶段2（只有关系，没有实体的情况）
     */
    private void startStage2Only() {
        currentStage = STAGE_RELATIONS;
        
        // 显示阶段提示
        tvStageHint.setVisibility(View.VISIBLE);
        tvStageHint.setText("选择要建立的关系");
        
        // 直接显示潜在关系，不需要提取候选
        candidateRelations = new ArrayList<>();
        
        // 更新统计信息
        int relationCount = result.getPotentialRelations() != null ? result.getPotentialRelations().size() : 0;
        tvStats.setText(String.format("潜在关系: %d", relationCount));
        
        // 只显示"潜在关系" Tab
        tabLayout.removeAllTabs();
        tabLayout.addTab(tabLayout.newTab().setText("潜在关系 (" + relationCount + ")"));
        
        // 设置候选关系为空
        adapter.setCandidateRelations(candidateRelations);
        
        // 显示潜在关系列表
        adapter.showConfirmedTab();
        adapter.notifyDataSetChanged();
        
        // 隐藏"下一步"按钮，显示"确认"按钮
        btnNext.setVisibility(View.GONE);
        btnConfirm.setVisibility(View.VISIBLE);
    }
    
    /**
     * 开始阶段2：选择要建立的关系
     */
    private void startStage2() {
        currentStage = STAGE_RELATIONS;
        
        // 显示阶段提示
        tvStageHint.setVisibility(View.VISIBLE);
        tvStageHint.setText("第二步：选择要建立的关系");
        
        // 提取候选关系（从选中的待创建实体中提取）
        candidateRelations = extractCandidateRelations(selectedEntities);
        
        // 更新统计信息
        int confirmedCount = result.getPotentialRelations() != null ? result.getPotentialRelations().size() : 0;
        int candidateCount = candidateRelations.size();
        tvStats.setText(String.format("潜在关系: %d | 候选关系: %d", confirmedCount, candidateCount));
        
        // 显示两个 Tab
        tabLayout.removeAllTabs();
        tabLayout.addTab(tabLayout.newTab().setText("潜在关系 (" + confirmedCount + ")"));
        if (candidateCount > 0) {
            tabLayout.addTab(tabLayout.newTab().setText("候选关系 (" + candidateCount + ")"));
        }
        
        // 设置候选关系到适配器
        adapter.setCandidateRelations(candidateRelations);
        
        // 默认显示已有关系
        adapter.showConfirmedTab();
        adapter.notifyDataSetChanged();
        
        // 监听 Tab 切换
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    adapter.showConfirmedTab();
                } else {
                    adapter.showCandidateTab();
                }
                adapter.notifyDataSetChanged();
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
        
        // 隐藏"下一步"按钮，显示"确认"按钮
        btnNext.setVisibility(View.GONE);
        btnConfirm.setVisibility(View.VISIBLE);
    }
    
    /**
     * 候选关系模型
     */
    public static class CandidateRelation {
        public String sourceName;
        public String targetName;
        public String relationshipType;
        public String description;
        public boolean isDirected = true;
    }
    
    /**
     * 从潜在关系中提取候选关系（用于阶段2显示）
     */
    public List<CandidateRelation> extractCandidateRelations(List<Integer> entityPositions) {
        List<CandidateRelation> candidates = new ArrayList<>();
        if (result.getPendingEntities() == null || result.getPotentialRelations() == null) return candidates;
        
        // 获取选中的实体名称
        java.util.Set<String> selectedEntityNames = new java.util.HashSet<>();
        for (int pos : entityPositions) {
            if (pos >= 0 && pos < result.getPendingEntities().size()) {
                selectedEntityNames.add(result.getPendingEntities().get(pos).getName());
            }
        }
        
        // 从潜在关系中筛选涉及选中实体的关系
        for (RelationExtractionResult.PotentialRelation rel : result.getPotentialRelations()) {
            boolean sourceInSelected = selectedEntityNames.contains(rel.getSourceName());
            boolean targetInSelected = selectedEntityNames.contains(rel.getTargetName());
            // 只要有一端在选中实体中，就可以作为候选关系
            // 另一端可能是已有实体或其他待创建实体
            if (sourceInSelected || targetInSelected) {
                CandidateRelation candidate = new CandidateRelation();
                candidate.sourceName = rel.getSourceName();
                candidate.targetName = rel.getTargetName();
                candidate.relationshipType = rel.getRelationshipType();
                candidate.description = rel.getDescription();
                candidate.isDirected = rel.isDirected();
                candidates.add(candidate);
            }
        }
        
        return candidates;
    }

    @Override
    public void onDismiss(@NonNull android.content.DialogInterface dialog) {
        super.onDismiss(dialog);
        if (!confirmed && listener != null) {
            listener.onCancel();
        }
    }
}