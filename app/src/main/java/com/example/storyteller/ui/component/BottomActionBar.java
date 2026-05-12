package com.example.storyteller.ui.component;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.google.android.material.card.MaterialCardView;

/**
 * 底部操作栏管理器
 * 用于动态配置和更新底部快捷操作按钮
 */
public class BottomActionBar {

    private final Context context;
    private final LinearLayout layoutActions;
    private final MaterialCardView cardView;

    public BottomActionBar(Context context, MaterialCardView cardView, LinearLayout layoutActions) {
        this.context = context;
        this.cardView = cardView;
        this.layoutActions = layoutActions;
    }

    /**
     * 清除所有按钮
     */
    public void clear() {
        layoutActions.removeAllViews();
    }

    /**
     * 添加按钮
     *
     * @param text     按钮文本
     * @param listener 点击监听器
     */
    public void addButton(String text, View.OnClickListener listener) {
        Button button = new Button(context);
        button.setText(text);
        button.setTextSize(14);
        button.setAllCaps(false);
        button.setPadding(32, 16, 32, 16);

        // 设置边距
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(8, 0, 8, 0);
        button.setLayoutParams(params);

        button.setOnClickListener(listener);
        layoutActions.addView(button);
    }

    /**
     * 添加占位按钮（无功能）
     *
     * @param text 按钮文本
     */
    public void addPlaceholderButton(String text) {
        addButton(text, v -> {
            // 占位功能，暂不实现
            android.widget.Toast.makeText(context, "功能开发中...", android.widget.Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * 显示操作栏
     */
    public void show() {
        cardView.setVisibility(View.VISIBLE);
    }

    /**
     * 隐藏操作栏
     */
    public void hide() {
        cardView.setVisibility(View.GONE);
    }

    /**
     * 配置写作Tab的底部按钮
     */
    public void setupWritingActions(Runnable onAddVolume, Runnable onAddChapter, 
                                     Runnable onAIContinue, Runnable onStats) {
        clear();
        addButton("+ 卷", v -> { if (onAddVolume != null) onAddVolume.run(); });
        addButton("+ 章", v -> { if (onAddChapter != null) onAddChapter.run(); });
        addButton("🤖 AI续写", v -> { if (onAIContinue != null) onAIContinue.run(); });
        addButton("📊 统计", v -> { if (onStats != null) onStats.run(); });
        show();
    }

    /**
     * 配置架构Tab的底部按钮
     */
    public void setupArchitectureActions(Runnable onSave, Runnable onAIOptimize, Runnable onPreview) {
        clear();
        addButton("💾 保存", v -> { if (onSave != null) onSave.run(); });
        addButton("✨ AI优化", v -> { if (onAIOptimize != null) onAIOptimize.run(); });
        addButton("👁 预览", v -> { if (onPreview != null) onPreview.run(); });
        show();
    }

    /**
     * 配置人物Tab的底部按钮（占位）
     */
    public void setupCharactersActions() {
        clear();
        addPlaceholderButton("+ 角色");
        addPlaceholderButton("🤖 生成");
        addPlaceholderButton("🕸 关系图");
        show();
    }

    /**
     * 配置素材Tab的底部按钮（占位）
     */
    public void setupMaterialsActions() {
        clear();
        addPlaceholderButton("+ 素材");
        addPlaceholderButton("批量导入");
        addPlaceholderButton("分类管理");
        show();
    }

    /**
     * 配置更多Tab的底部按钮（占位）
     */
    public void setupMoreActions() {
        clear();
        addPlaceholderButton("⚙️ 设置");
        addPlaceholderButton("📤 导出");
        addPlaceholderButton("🗑 删除");
        show();
    }
}
