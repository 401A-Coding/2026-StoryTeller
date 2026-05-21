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
import com.example.storyteller.ui.adapter.AiMemoryAdapter;
import com.example.storyteller.utils.AiMemoryManager;
import com.google.android.material.button.MaterialButton;
import java.util.List;
import java.util.Map;

/**
 * AI记忆管理对话框
 * 允许用户查看和删除AI记忆
 */
public class AiMemoryDialog extends DialogFragment {
    
    private static final String ARG_STORY_ID = "story_id";
    private static final String ARG_STORY_TITLE = "story_title";
    
    private AiMemoryManager memoryManager;
    private int storyId;
    private String storyTitle;
    
    // UI组件
    private TextView tvMemoryCount;
    private LinearLayout layoutPlot;
    private LinearLayout layoutPersonality;
    private LinearLayout layoutWorld;
    private LinearLayout layoutOther;
    private TextView tvEmpty;
    private MaterialButton btnClear;
    
    private AiMemoryAdapter adapterPlot;
    private AiMemoryAdapter adapterPersonality;
    private AiMemoryAdapter adapterWorld;
    private AiMemoryAdapter adapterOther;
    
    public static AiMemoryDialog newInstance(int storyId, String storyTitle) {
        AiMemoryDialog dialog = new AiMemoryDialog();
        Bundle args = new Bundle();
        args.putInt(ARG_STORY_ID, storyId);
        args.putString(ARG_STORY_TITLE, storyTitle);
        dialog.setArguments(args);
        return dialog;
    }
    
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        memoryManager = AiMemoryManager.getInstance(context);
        
        if (getArguments() != null) {
            storyId = getArguments().getInt(ARG_STORY_ID, -1);
            storyTitle = getArguments().getString(ARG_STORY_TITLE);
        }
    }
    
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View contentView = getLayoutInflater().inflate(R.layout.dialog_ai_memory, null);
        builder.setView(contentView);
        
        // 设置标题
        String title = "AI记忆管理";
        if (storyTitle != null && !storyTitle.isEmpty()) {
            title = "《" + storyTitle + "》AI记忆";
        }
        builder.setTitle(title);
        
        // 设置按钮
        builder.setPositiveButton("关闭", null);
        
        AlertDialog dialog = builder.create();
        
        initViews(contentView);
        loadMemories();
        
        return dialog;
    }
    
    private void initViews(View contentView) {
        tvMemoryCount = contentView.findViewById(R.id.tv_memory_count);
        layoutPlot = contentView.findViewById(R.id.layout_plot);
        layoutPersonality = contentView.findViewById(R.id.layout_personality);
        layoutWorld = contentView.findViewById(R.id.layout_world);
        layoutOther = contentView.findViewById(R.id.layout_other);
        tvEmpty = contentView.findViewById(R.id.tv_empty);
        btnClear = contentView.findViewById(R.id.btn_clear);
        
        // 初始化适配器
        adapterPlot = new AiMemoryAdapter();
        adapterPersonality = new AiMemoryAdapter();
        adapterWorld = new AiMemoryAdapter();
        adapterOther = new AiMemoryAdapter();
        
        // 设置删除监听
        adapterPlot.setOnMemoryDeleteListener(this::deleteMemory);
        adapterPersonality.setOnMemoryDeleteListener(this::deleteMemory);
        adapterWorld.setOnMemoryDeleteListener(this::deleteMemory);
        adapterOther.setOnMemoryDeleteListener(this::deleteMemory);
        
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
        
        // 清空按钮
        btnClear.setOnClickListener(v -> showClearConfirmDialog());
    }
    
    private void loadMemories() {
        Map<String, List<AiMemory>> grouped = memoryManager.getMemoriesGroupedByType(storyId);
        
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
        
        // 更新统计
        tvMemoryCount.setText("共 " + totalCount + " 条记忆");
        
        // 空状态
        if (totalCount == 0) {
            tvEmpty.setVisibility(View.VISIBLE);
            btnClear.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            btnClear.setVisibility(View.VISIBLE);
        }
    }
    
    private void deleteMemory(AiMemory memory) {
        new AlertDialog.Builder(requireContext())
            .setTitle("删除记忆")
            .setMessage("确定要删除这条记忆吗？\n\n" + memory.getTitle())
            .setPositiveButton("删除", (dialog, which) -> {
                boolean success = memoryManager.deleteMemory(memory.getId());
                if (success) {
                    Toast.makeText(requireContext(), "已删除", Toast.LENGTH_SHORT).show();
                    loadMemories();
                } else {
                    Toast.makeText(requireContext(), "删除失败", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }
    
    private void showClearConfirmDialog() {
        new AlertDialog.Builder(requireContext())
            .setTitle("清空记忆")
            .setMessage("确定要清空当前小说的所有AI记忆吗？\n全局记忆不会被删除。")
            .setPositiveButton("清空", (dialog, which) -> {
                int count = memoryManager.clearStoryMemories(storyId);
                Toast.makeText(requireContext(), "已清空 " + count + " 条记忆", Toast.LENGTH_SHORT).show();
                loadMemories();
            })
            .setNegativeButton("取消", null)
            .show();
    }
}