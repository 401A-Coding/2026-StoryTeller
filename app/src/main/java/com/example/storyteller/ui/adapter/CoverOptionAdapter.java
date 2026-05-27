package com.example.storyteller.ui.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.storyteller.R;
import com.example.storyteller.data.remote.ApiClient;
import com.google.android.material.card.MaterialCardView;

import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * 封面选项适配器 - 用于展示多张AI生成的封面供用户选择
 */
public class CoverOptionAdapter extends RecyclerView.Adapter<CoverOptionAdapter.ViewHolder> {

    private final List<String> imageUrls;
    private final ApiClient apiClient;
    private final ExecutorService executor;
    private final Context context;
    private OnCoverSelectedListener listener;
    private int selectedPosition = -1;

    public interface OnCoverSelectedListener {
        void onCoverSelected(String imageUrl);
    }

    public CoverOptionAdapter(List<String> imageUrls, ApiClient apiClient, 
                              ExecutorService executor, Context context) {
        this.imageUrls = imageUrls;
        this.apiClient = apiClient;
        this.executor = executor;
        this.context = context;
    }

    public void setOnCoverSelectedListener(OnCoverSelectedListener listener) {
        this.listener = listener;
    }

    public String getSelectedUrl() {
        if (selectedPosition >= 0 && selectedPosition < imageUrls.size()) {
            return imageUrls.get(selectedPosition);
        }
        return null;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cover_option, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String imageUrl = imageUrls.get(position);
        holder.bind(imageUrl, position == selectedPosition);
    }

    @Override
    public int getItemCount() {
        return imageUrls.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivCover;
        private final ProgressBar progressBar;
        private final MaterialCardView cardCover;

        ViewHolder(View itemView) {
            super(itemView);
            ivCover = itemView.findViewById(R.id.iv_cover);
            progressBar = itemView.findViewById(R.id.progress_bar);
            cardCover = itemView.findViewById(R.id.card_cover);
            
            cardCover.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    int oldPosition = selectedPosition;
                    selectedPosition = position;
                    
                    // 刷新旧选中项
                    if (oldPosition >= 0) {
                        notifyItemChanged(oldPosition);
                    }
                    // 刷新新选中项
                    notifyItemChanged(selectedPosition);
                    
                    // 回调
                    if (listener != null) {
                        listener.onCoverSelected(imageUrls.get(selectedPosition));
                    }
                }
            });
        }

        void bind(String imageUrl, boolean isSelected) {
            // 更新选中状态（边框+半透明背景色）
            if (isSelected) {
                // 半透明背景色高亮
                cardCover.setCardBackgroundColor(0x332196F3); // #2196F3 的 20% 透明度
                // 加粗边框
                cardCover.setStrokeWidth(8);
                cardCover.setStrokeColor(0xFF2196F3);
            } else {
                // 恢复正常
                cardCover.setCardBackgroundColor(0x00000000); // 透明背景
                cardCover.setCardElevation(2f);
                cardCover.setStrokeWidth(0);
            }
            
            // 显示加载状态
            progressBar.setVisibility(View.VISIBLE);
            ivCover.setImageBitmap(null);
            
            // 下载并显示图片
            apiClient.downloadImageAsBitmap(imageUrl, context, executor,
                bitmap -> {
                    if (context instanceof android.app.Activity) {
                        ((android.app.Activity) context).runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            ivCover.setImageBitmap(bitmap);
                        });
                    }
                },
                e -> {
                    if (context instanceof android.app.Activity) {
                        ((android.app.Activity) context).runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            ivCover.setImageResource(R.drawable.ic_insert_drive_file);
                        });
                    }
                }
            );
        }
    }
}