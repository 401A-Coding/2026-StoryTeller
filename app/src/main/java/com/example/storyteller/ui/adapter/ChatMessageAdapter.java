package com.example.storyteller.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

    public ChatMessageAdapter(Context context, List<ChatMessage> messages) {
        this.context = context;
        this.messages = messages;
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
        holder.tvMessage.setText(message.getContent());
        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) holder.tvMessage.getLayoutParams();
        if (message.isFromUser()) {
            params.startToStart = ConstraintLayout.LayoutParams.UNSET;
            params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
            holder.tvMessage.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END);
        } else {
            params.endToEnd = ConstraintLayout.LayoutParams.UNSET;
            params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
            holder.tvMessage.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        }
        holder.tvMessage.setLayoutParams(params);
    }

    @Override
    public int getItemCount() {
        return messages == null ? 0 : messages.size();
    }

    public static class ChatMessageViewHolder extends RecyclerView.ViewHolder {
        final TextView tvMessage;

        public ChatMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tv_message);
        }
    }
}
