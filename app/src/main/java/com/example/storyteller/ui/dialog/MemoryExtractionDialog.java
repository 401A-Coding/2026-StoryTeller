package com.example.storyteller.ui.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.storyteller.R;
import com.example.storyteller.model.AiMemory;
import com.example.storyteller.ui.adapter.MemoryExtractionAdapter;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 记忆提取结果对话框
 * 展示AI提取的记忆，允许用户勾选需要保存的，然后批量保存
 */
public class MemoryExtractionDialog extends DialogFragment {
    
    private static final String ARG_STORY_ID = "story_id";
    private static final String ARG_MEMORIES = "memories";
    
    private int storyId;
    private List<AiMemory> extractedMemories;
    
    // UI组件
    private TextView tvMemoryCount;
    private LinearLayout layoutPlot;
    private LinearLayout layoutPersonality;
    private LinearLayout layoutWorld;
    private LinearLayout layoutOther;
    private TextView tvEmpty;
    
    private MemoryExtractionAdapter adapterPlot;
    private MemoryExtractionAdapter adapterPersonality;
    private MemoryExtractionAdapter adapterWorld;
    private MemoryExtractionAdapter adapterOther;
    
    // 底部按钮
    private com.google.android.material.button.MaterialButton btnDeselectAll;
    private com.google.android.material.button.MaterialButton btnCancel;
    private com.google.android.material.button.MaterialButton btnConfirm;
    
    public interface OnMemoriesSavedListener {
        void onMemoriesSaved(int count);
    }
    
    private OnMemoriesSavedListener savedListener;
    
    public void setOnMemoriesSavedListener(OnMemoriesSavedListener listener) {
        this.savedListener = listener;
    }
    
    public static MemoryExtractionDialog newInstance(int storyId, List<AiMemory> memories) {
        MemoryExtractionDialog dialog = new MemoryExtractionDialog();
        Bundle args = new Bundle();
        args.putInt(ARG_STORY_ID, storyId);
        args.putSerializable(ARG_MEMORIES, new ArrayList<>(memories));
        dialog.setArguments(args);
        return dialog;
    }
    
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        
        if (getArguments() != null) {
            storyId = getArguments().getInt(ARG_STORY_ID, -1);
            extractedMemories = (List<AiMemory>) getArguments().getSerializable(ARG_MEMORIES);
            if (extractedMemories == null) {
                extractedMemories = new ArrayList<>();
            }
        }
    }
    
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View contentView = getLayoutInflater().inflate(R.layout.dialog_memory_extraction, null);
        builder.setView(contentView);
        
        builder.setTitle("AI记忆提取结果");
        
        AlertDialog dialog = builder.create();
        
        initViews(contentView);
        loadMemories();
        setupButtons();
        
        return dialog;
    }
    
    private void initViews(View contentView) {
        tvMemoryCount = contentView.findViewById(R.id.tv_memory_count);
        layoutPlot = contentView.findViewById(R.id.layout_plot);
        layoutPersonality = contentView.findViewById(R.id.layout_personality);
        layoutWorld = contentView.findViewById(R.id.layout_world);
        layoutOther = contentView.findViewById(R.id.layout_other);
        tvEmpty = contentView.findViewById(R.id.tv_empty);
        
        // 初始化适配器
        adapterPlot = new MemoryExtractionAdapter();
        adapterPersonality = new MemoryExtractionAdapter();
        adapterWorld = new MemoryExtractionAdapter();
        adapterOther = new MemoryExtractionAdapter();
        
        // 设置RecyclerView
        RecyclerView rvPlot = contentView.findViewById(R.id.rv_plot);
        RecyclerView rvPersonality = contentView.findViewById(R.id.rv_personality);
        RecyclerView rvWorld = contentView.findViewById(R.id.rv_world);
        RecyclerView rvOther = contentView.findViewById(R.id.rv_other);
        
        rvPlot.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvPlot.setAdapter(adapterPlot);
        
        rvPersonality.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvPersonality.setAdapter(adapterPersonality);
        
        rvWorld.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvWorld.setAdapter(adapterWorld);
        
        rvOther.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvOther.setAdapter(adapterOther);
        
        // 底部按钮
        btnDeselectAll = contentView.findViewById(R.id.btn_deselect_all);
        btnCancel = contentView.findViewById(R.id.btn_cancel);
        btnConfirm = contentView.findViewById(R.id.btn_confirm);
    }
    
    private void setupButtons() {
        // 取消全选
        btnDeselectAll.setOnClickListener(v -> {
            adapterPlot.deselectAll();
            adapterPersonality.deselectAll();
            adapterWorld.deselectAll();
            adapterOther.deselectAll();
            updateUI();
        });
        
        // 取消
        btnCancel.setOnClickListener(v -> dismiss());
        
        // 确认
        btnConfirm.setOnClickListener(v -> saveMemories());
    }
    
    private void loadMemories() {
        // 按类型分组
        Map<String, List<AiMemory>> grouped = new HashMap<>();
        for (AiMemory memory : extractedMemories) {
            String type = memory.getMemoryType();
            if (!grouped.containsKey(type)) {
                grouped.put(type, new ArrayList<>());
            }
            grouped.get(type).add(memory);
        }
        
        int totalCount = 0;
        
        // 剧情类
        List<AiMemory> plotMemories = grouped.get(AiMemory.TYPE_PLOT);
        if (plotMemories != null && !plotMemories.isEmpty()) {
            layoutPlot.setVisibility(View.VISIBLE);
            adapterPlot.setMemories(plotMemories);
            totalCount += plotMemories.size();
        } else {
            layoutPlot.setVisibility(View.GONE);
        }
        
        // 人设类
        List<AiMemory> personalityMemories = grouped.get(AiMemory.TYPE_PERSONALITY);
        if (personalityMemories != null && !personalityMemories.isEmpty()) {
            layoutPersonality.setVisibility(View.VISIBLE);
            adapterPersonality.setMemories(personalityMemories);
            totalCount += personalityMemories.size();
        } else {
            layoutPersonality.setVisibility(View.GONE);
        }
        
        // 世界观类
        List<AiMemory> worldMemories = grouped.get(AiMemory.TYPE_WORLD);
        if (worldMemories != null && !worldMemories.isEmpty()) {
            layoutWorld.setVisibility(View.VISIBLE);
            adapterWorld.setMemories(worldMemories);
            totalCount += worldMemories.size();
        } else {
            layoutWorld.setVisibility(View.GONE);
        }
        
        // 其他类
        List<AiMemory> otherMemories = grouped.get(AiMemory.TYPE_OTHER);
        if (otherMemories != null && !otherMemories.isEmpty()) {
            layoutOther.setVisibility(View.VISIBLE);
            adapterOther.setMemories(otherMemories);
            totalCount += otherMemories.size();
        } else {
            layoutOther.setVisibility(View.GONE);
        }
        
        updateUI();
    }
    
    private void updateUI() {
        int selectedCount = adapterPlot.getSelectedCount() + 
                           adapterPersonality.getSelectedCount() + 
                           adapterWorld.getSelectedCount() + 
                           adapterOther.getSelectedCount();
        
        int totalCount = adapterPlot.getItemCount() + 
                        adapterPersonality.getItemCount() + 
                        adapterWorld.getItemCount() + 
                        adapterOther.getItemCount();
        
        tvMemoryCount.setText("共 " + totalCount + " 条记忆，已选 " + selectedCount + " 条");
        
        // 空状态
        if (totalCount == 0) {
            tvEmpty.setVisibility(View.VISIBLE);
        } else {
            tvEmpty.setVisibility(View.GONE);
        }
        
        // 更新确认按钮状态
        btnConfirm.setEnabled(selectedCount > 0);
    }
    
    private void saveMemories() {
        // 获取所有选中的记忆
        List<AiMemory> toSave = new ArrayList<>();
        toSave.addAll(adapterPlot.getSelectedMemories());
        toSave.addAll(adapterPersonality.getSelectedMemories());
        toSave.addAll(adapterWorld.getSelectedMemories());
        toSave.addAll(adapterOther.getSelectedMemories());
        
        if (toSave.isEmpty()) {
            Toast.makeText(requireContext(), "请至少选择一条记忆", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 保存到数据库
        com.example.storyteller.utils.AiMemoryManager memoryManager = 
            com.example.storyteller.utils.AiMemoryManager.getInstance(requireContext());
        
        int savedCount = 0;
        for (AiMemory memory : toSave) {
            memory.setStoryId(storyId);
            long id = memoryManager.addMemory(memory);
            if (id > 0) {
                savedCount++;
            }
        }
        
        Toast.makeText(requireContext(), "已保存 " + savedCount + " 条记忆", Toast.LENGTH_SHORT).show();
        
        if (savedListener != null) {
            savedListener.onMemoriesSaved(savedCount);
        }
        
        dismiss();
    }
}
