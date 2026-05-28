package com.example.storyteller.ui.adapter;

import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.storyteller.R;
import com.example.storyteller.utils.ThemeColorUtils;
import com.google.android.material.card.MaterialCardView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 设定配图选项适配器
 */
public class SettingImageOptionAdapter extends RecyclerView.Adapter<SettingImageOptionAdapter.ViewHolder> {

    private List<String> images = new ArrayList<>();
    private String selectedImage = null;
    private OnImageSelectedListener listener;
    
    public interface OnImageSelectedListener {
        void onImageSelected(String imagePath);
    }

    public SettingImageOptionAdapter(List<String> images, OnImageSelectedListener listener) {
        this.images = images != null ? images : new ArrayList<>();
        this.listener = listener;
    }

    public void setImages(List<String> images) {
        this.images = images != null ? images : new ArrayList<>();
        notifyDataSetChanged();
    }
    
    public void addImage(String imagePath) {
        if (imagePath != null) {
            this.images.add(imagePath);
            notifyItemInserted(this.images.size() - 1);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_setting_image_option, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String imagePath = images.get(position);
        holder.bind(imagePath, imagePath.equals(selectedImage));
    }

    @Override
    public int getItemCount() {
        return images.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardImage;
        private final ImageView ivImage;
        private final View viewSelected;
        private final ImageView ivSelectedCheck;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardImage = itemView.findViewById(R.id.card_image);
            ivImage = itemView.findViewById(R.id.iv_image);
            viewSelected = itemView.findViewById(R.id.view_selected);
            ivSelectedCheck = itemView.findViewById(R.id.iv_selected_check);
        }

        public void bind(String imagePath, boolean isSelected) {
            // 加载图片
            File imageFile = new File(imagePath);
            if (imageFile.exists()) {
                ivImage.setImageBitmap(BitmapFactory.decodeFile(imagePath));
            } else {
                ivImage.setImageResource(R.drawable.ic_insert_drive_file);
            }
            
            // 选中状态
            if (isSelected) {
                cardImage.setCardBackgroundColor(Color.argb(50, 33, 150, 243));
                cardImage.setStrokeWidth(4);
                cardImage.setStrokeColor(ThemeColorUtils.getLinkColor(itemView.getContext()));
                viewSelected.setVisibility(View.GONE);
                ivSelectedCheck.setVisibility(View.GONE);
            } else {
                cardImage.setCardBackgroundColor(0x00000000);
                cardImage.setStrokeWidth(0);
                viewSelected.setVisibility(View.GONE);
                ivSelectedCheck.setVisibility(View.GONE);
            }
            
            // 点击事件
            itemView.setOnClickListener(v -> {
                selectedImage = imagePath;
                notifyDataSetChanged();
                if (listener != null) {
                    listener.onImageSelected(imagePath);
                }
            });
        }
    }
}