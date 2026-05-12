package com.example.storyteller.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;
import com.example.storyteller.R;
import com.example.storyteller.model.ChatMessage;
import java.util.List;

public class ChatMessageAdapter extends RecyclerView.Adapter<ChatMessageAdapter.ChatMessageViewHolder> {

    private final Context context;
    private final List<ChatMessage> messages;
    private OnRetryListener retryListener;
    
    // 重试监听器接口
    public interface OnRetryListener {
        void onRetry(String originalMessage);
    }

    public ChatMessageAdapter(Context context, List<ChatMessage> messages) {
        this.context = context;
        this.messages = messages;
    }
    
    public void setOnRetryListener(OnRetryListener listener) {
        this.retryListener = listener;
    }

    @NonNull
    @Override
    public ChatMessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_chat_message, parent, false);
        return new ChatMessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatMessageViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        
        // 根据消息类型显示不同内容
        if (message.getMessageType() == ChatMessage.MessageType.PROCESSING || 
            message.getMessageType() == ChatMessage.MessageType.COMPLETED) {
            // 显示执行步骤
            renderExecutionSteps(holder, message);
        } else {
            // 显示普通消息
            renderNormalMessage(holder, message);
        }
    }
    
    /**
     * 渲染普通消息
     */
    private void renderNormalMessage(ChatMessageViewHolder holder, ChatMessage message) {
        // 隐藏步骤容器
        if (holder.layoutStepsContainer != null) {
            holder.layoutStepsContainer.setVisibility(View.GONE);
        }
        holder.tvFinalResult.setVisibility(View.GONE);
        
        // 显示普通消息内容
        holder.tvMessage.setVisibility(View.VISIBLE);
        holder.tvMessage.setText(message.getDisplayContent());
        
        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) holder.tvMessage.getLayoutParams();
        if (message.isFromUser()) {
            params.startToStart = ConstraintLayout.LayoutParams.UNSET;
            params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
            holder.tvMessage.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END);
            holder.tvMessage.setBackgroundResource(R.drawable.bg_chat_bubble_user);
        } else {
            params.endToEnd = ConstraintLayout.LayoutParams.UNSET;
            params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
            holder.tvMessage.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
            holder.tvMessage.setBackgroundResource(R.drawable.bg_chat_bubble_bot);
        }
        holder.tvMessage.setLayoutParams(params);
    }
    
    /**
     * 渲染执行步骤
     */
    private void renderExecutionSteps(ChatMessageViewHolder holder, ChatMessage message) {
        // 隐藏普通消息
        holder.tvMessage.setVisibility(View.GONE);
        
        // 显示步骤容器
        if (holder.layoutStepsContainer != null) {
            holder.layoutStepsContainer.setVisibility(View.VISIBLE);
        }
        
        // 更新标题和耗时
        if (message.getMessageType() == ChatMessage.MessageType.COMPLETED) {
            holder.tvStepTitle.setText("✅ AI 执行完成");
            long duration = message.getDuration();
            holder.tvStepDuration.setText("耗时: " + duration + "ms");
        } else {
            holder.tvStepTitle.setText("🤖 AI 正在执行...");
            holder.tvStepDuration.setText("进行中...");
        }
        
        // 清空并重新渲染步骤
        holder.layoutStepsList.removeAllViews();
        for (ChatMessage.ExecutionStep step : message.getSteps()) {
            View stepView = LayoutInflater.from(context).inflate(
                R.layout.item_execution_step, holder.layoutStepsList, false);
            
            TextView tvIcon = stepView.findViewById(R.id.tv_step_icon);
            TextView tvText = stepView.findViewById(R.id.tv_step_text);
            TextView tvDetail = stepView.findViewById(R.id.tv_step_detail);
            
            // 设置图标
            String icon = getStepIcon(step.status);
            tvIcon.setText(icon);
            
            // 设置标题
            tvText.setText(step.title);
            
            // 设置详细信息
            if (step.detail != null && !step.detail.isEmpty()) {
                tvDetail.setText(step.detail);
                tvDetail.setVisibility(View.VISIBLE);
            } else {
                tvDetail.setVisibility(View.GONE);
            }
            
            holder.layoutStepsList.addView(stepView);
        }
        
        // 显示最终结果（如果有）
        if (message.getMessageType() == ChatMessage.MessageType.COMPLETED && 
            !message.getResultContent().isEmpty()) {
            holder.tvFinalResult.setVisibility(View.VISIBLE);
            // 如果启用打字机效果，使用 displayContent；否则使用完整内容
            String displayText = message.isTyping() ? message.getDisplayContent() : message.getResultContent();
            holder.tvFinalResult.setText("\n" + displayText);
        } else {
            holder.tvFinalResult.setVisibility(View.GONE);
        }
        
        // 显示重试按钮（如果有失败步骤且可以重试）
        boolean hasFailedStep = false;
        for (ChatMessage.ExecutionStep step : message.getSteps()) {
            if (step.status == ChatMessage.StepStatus.FAILED) {
                hasFailedStep = true;
                break;
            }
        }
        
        if (hasFailedStep && message.canRetry() && retryListener != null) {
            holder.btnRetry.setVisibility(View.VISIBLE);
            holder.btnRetry.setOnClickListener(v -> {
                retryListener.onRetry(message.getOriginalUserMessage());
            });
        } else {
            holder.btnRetry.setVisibility(View.GONE);
        }
    }
    
    /**
     * 根据状态获取图标
     */
    private String getStepIcon(ChatMessage.StepStatus status) {
        switch (status) {
            case PENDING:
                return "⏸️";
            case RUNNING:
                return "⏳";
            case COMPLETED:
                return "✅";
            case FAILED:
                return "❌";
            default:
                return "⏳";
        }
    }

    @Override
    public int getItemCount() {
        return messages == null ? 0 : messages.size();
    }

    public static class ChatMessageViewHolder extends RecyclerView.ViewHolder {
        final TextView tvMessage;
        final View layoutStepsContainer;      // 步骤容器根视图
        final LinearLayout layoutStepsList;   // 步骤列表容器
        final TextView tvStepTitle;           // 步骤标题
        final TextView tvStepDuration;        // 耗时
        final TextView tvFinalResult;         // 最终结果
        final android.widget.Button btnRetry; // 重试按钮

        public ChatMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tv_message);
            
            // 查找 include 的根视图
            layoutStepsContainer = itemView.findViewById(R.id.layout_execution_steps);
            
            // 从 include 布局中查找子视图
            if (layoutStepsContainer != null) {
                layoutStepsList = layoutStepsContainer.findViewById(R.id.layout_steps_container);
                tvStepTitle = layoutStepsContainer.findViewById(R.id.tv_step_title);
                tvStepDuration = layoutStepsContainer.findViewById(R.id.tv_step_duration);
                tvFinalResult = layoutStepsContainer.findViewById(R.id.tv_final_result);
                btnRetry = layoutStepsContainer.findViewById(R.id.btn_retry);
            } else {
                // 兼容旧布局
                layoutStepsList = new LinearLayout(itemView.getContext());
                tvStepTitle = new TextView(itemView.getContext());
                tvStepDuration = new TextView(itemView.getContext());
                tvFinalResult = new TextView(itemView.getContext());
                btnRetry = new android.widget.Button(itemView.getContext());
            }
        }
    }
}
